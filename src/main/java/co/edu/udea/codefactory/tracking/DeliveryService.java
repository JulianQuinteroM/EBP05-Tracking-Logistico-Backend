package co.edu.udea.codefactory.tracking;

import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {
    private final JdbcTemplate db; private final ShipmentRepository shipments;
    private final ShipmentEventRepository events; private final ShipmentAuditRepository audits;
    private final ShipmentService service; private final Clock clock;
    public DeliveryService(JdbcTemplate db,ShipmentRepository shipments,ShipmentEventRepository events,ShipmentAuditRepository audits,ShipmentService service,Clock clock) {
        this.db=db; this.shipments=shipments; this.events=events; this.audits=audits; this.service=service; this.clock=clock;
    }
    private Shipment require(Long id,Long version) {
        Shipment s=shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        if(!Objects.equals(s.version,version)) throw new ShipmentConflictException("El envío cambió; recarga");
        return s;
    }
    @Transactional public ShipmentResponses.ShipmentDetail deliver(Long id,Long version,String receiver,String type,String mime,String base64,String actor,String ip) {
        Shipment s=require(id,version);
        if(s.status!=ShipmentStatus.RUTA_ENTREGA) throw new ShipmentConflictException("La entrega requiere el estado En ruta de entrega");
        if(!Set.of("FOTO","FIRMA").contains(type)||!Set.of("image/png","image/jpeg").contains(mime)) throw new BusinessException("Usa una foto o firma PNG/JPEG");
        byte[] bytes;
        try { bytes=Base64.getDecoder().decode(base64); } catch(IllegalArgumentException e) { throw new BusinessException("Imagen inválida"); }
        if(bytes.length>1_500_000 || bytes.length<50) throw new BusinessException("La evidencia debe ocupar entre 50 bytes y 1,5 MB");
        try (var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext()) throw new BusinessException("El archivo no contiene una imagen válida");
            var reader=readers.next();
            try {
                reader.setInput(stream); String format=reader.getFormatName();
                if(!(mime.equals("image/png")&&format.equalsIgnoreCase("png")) && !(mime.equals("image/jpeg")&&format.equalsIgnoreCase("JPEG"))) throw new BusinessException("El formato no coincide con la imagen");
                if(reader.getWidth(0)>6000||reader.getHeight(0)>6000) throw new BusinessException("Imagen demasiado grande: máximo 6000 píxeles por lado");
                if(reader.read(0)==null) throw new BusinessException("No se pudo leer la imagen");
            } finally { reader.dispose(); }
        } catch(java.io.IOException e) { throw new BusinessException("Imagen dañada"); }
        OffsetDateTime now=OffsetDateTime.now(clock);
        db.update("insert into evidencia_entrega values (?,?,?,?,?,?,?)",id,receiver.strip(),type,mime,bytes,actor,now);
        s.status=ShipmentStatus.ENTREGADO; s.updatedAt=now; shipments.saveAndFlush(s);
        events.save(ShipmentEvent.statusChange(s,ShipmentStatus.RUTA_ENTREGA,actor,"Envío entregado; evidencia privada registrada",now));
        audits.save(new ShipmentAudit(s,"ENTREGA",actor,"status","RUTA_ENTREGA","ENTREGADO",ip,"Entrega confirmada con evidencia "+type,now));
        return service.get(id);
    }
    @Transactional public ShipmentResponses.ShipmentDetail failed(Long id,Long version,String reason,String actor,String ip) {
        Shipment s=require(id,version);
        if(s.status!=ShipmentStatus.RUTA_ENTREGA) throw new ShipmentConflictException("Registra el intento desde En ruta de entrega");
        LocalDate next=LocalDate.now(clock.withZone(ZoneId.of("America/Bogota"))).plusDays(1);
        while(next.getDayOfWeek()==DayOfWeek.SATURDAY||next.getDayOfWeek()==DayOfWeek.SUNDAY) next=next.plusDays(1);
        int attempt=db.queryForObject("select count(*) from intento_entrega where envio_id=?",Integer.class,id)+1;
        OffsetDateTime now=OffsetDateTime.now(clock);
        db.update("insert into intento_entrega(envio_id,numero,motivo,proxima_fecha,actor,ocurrido_en) values (?,?,?,?,?,?)",id,attempt,reason.strip(),next,actor,now);
        s.statusBeforeIncident=s.status; s.status=ShipmentStatus.INCIDENCIA; s.updatedAt=now; shipments.saveAndFlush(s);
        events.save(ShipmentEvent.statusChange(s,ShipmentStatus.RUTA_ENTREGA,actor,"Intento de entrega fallido. Nuevo intento previsto: "+next,now));
        audits.save(new ShipmentAudit(s,"INTENTO_ENTREGA",actor,"status","RUTA_ENTREGA","INCIDENCIA",ip,"Intento #"+attempt+": "+reason+"; siguiente: "+next,now));
        return service.get(id);
    }
    @Transactional public ShipmentResponses.ShipmentDetail resolve(Long id,Long version,String solution,String actor,String ip) {
        Shipment s=require(id,version);
        if(s.status!=ShipmentStatus.INCIDENCIA||s.statusBeforeIncident==null) throw new ShipmentConflictException("El envío no tiene una incidencia abierta");
        ShipmentStatus next=s.statusBeforeIncident;
        s.status=next; s.statusBeforeIncident=null; s.updatedAt=OffsetDateTime.now(clock); shipments.saveAndFlush(s);
        events.save(ShipmentEvent.statusChange(s,ShipmentStatus.INCIDENCIA,actor,"Incidencia resuelta; el envío retoma su proceso",s.updatedAt));
        audits.save(new ShipmentAudit(s,"RESOLUCION_INCIDENCIA",actor,"status","INCIDENCIA",next.name(),ip,solution.strip(),s.updatedAt));
        return service.get(id);
    }
    @Transactional public ShipmentResponses.ShipmentDetail contacts(Long id,Long version,String senderPhone,String senderEmail,String recipientPhone,String recipientEmail,String actor,String ip) {
        Shipment s=require(id,version);
        if(Set.of(ShipmentStatus.ENTREGADO,ShipmentStatus.CANCELADO).contains(s.status)) throw new ShipmentConflictException("Un envío finalizado conserva sus datos históricos");
        Map<String,String> before=Map.of("senderPhone",s.senderPhone,"senderEmail",s.senderEmail,"recipientPhone",s.recipientPhone,"recipientEmail",s.recipientEmail);
        Map<String,String> after=Map.of("senderPhone",senderPhone.strip(),"senderEmail",senderEmail.strip(),"recipientPhone",recipientPhone.strip(),"recipientEmail",recipientEmail.strip());
        s.senderPhone=after.get("senderPhone");s.senderEmail=after.get("senderEmail");s.recipientPhone=after.get("recipientPhone");s.recipientEmail=after.get("recipientEmail");
        s.updatedAt=OffsetDateTime.now(clock);shipments.saveAndFlush(s);
        before.forEach((field,value)->{ if(!value.equals(after.get(field))) audits.save(new ShipmentAudit(s,"CONTACTO_ENVIO",actor,field,value,after.get(field),ip,"Corrección de contacto en ficha",s.updatedAt)); });
        return service.get(id);
    }
    public Map<String,Object> operation(Long id) {
        service.get(id);
        return Map.of("attempts",db.queryForList("select id,numero,motivo,proxima_fecha,actor,ocurrido_en from intento_entrega where envio_id=? order by numero desc",id),
            "evidence",db.queryForList("select receptor,tipo,actor,ocurrido_en from evidencia_entrega where envio_id=?",id));
    }
    public Map<String,Object> evidence(Long id) { return db.queryForList("select mime,contenido from evidencia_entrega where envio_id=?",id).stream().findFirst().orElseThrow(()->new BusinessException("No hay evidencia")); }
}
