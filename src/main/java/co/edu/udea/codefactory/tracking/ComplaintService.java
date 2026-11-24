package co.edu.udea.codefactory.tracking;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintService {
    private final JdbcTemplate db;private final ShipmentRepository shipments;private final Accounts accounts;private final Clock clock;
    public ComplaintService(JdbcTemplate db,ShipmentRepository shipments,Accounts accounts,Clock clock) { this.db=db;this.shipments=shipments;this.accounts=accounts;this.clock=clock; }
    @Transactional public Map<String,Object> create(String code,String email,String category,String subject,String description) {
        if(!Set.of("RETRASO","DANO","ENTREGA","ATENCION","OTRO").contains(category)) throw new BusinessException("Categoría no permitida");
        Shipment s=shipments.findByTrackingCode(TrackingCode.normalize(code)).orElseThrow(()->new BusinessException("Verifica el código y el correo asociado al envío"));
        if(!email.equalsIgnoreCase(s.senderEmail)&&!email.equalsIgnoreCase(s.recipientEmail)) throw new BusinessException("Verifica el código y el correo asociado al envío");
        db.queryForList("select id from envio where id=? for update",s.id);
        if(db.queryForObject("select count(*) from reclamacion where envio_id=? and estado in ('ABIERTA','EN_REVISION')",Integer.class,s.id)>=3) throw new BusinessException("Este envío ya tiene tres reclamaciones abiertas; continúa en una de ellas");
        String id="REC-"+UUID.randomUUID().toString().substring(0,18).toUpperCase(Locale.ROOT);String key=Accounts.secret();OffsetDateTime now=OffsetDateTime.now(clock);
        db.update("insert into reclamacion(id,envio_id,clave_hash,categoria,asunto,descripcion,correo,estado,creado_en,actualizado_en) values (?,?,?,?,?,?,?,'ABIERTA',?,?)",id,s.id,Accounts.hash(key),category,subject.strip(),description.strip(),email.strip(),now,now);
        return Map.of("id",id,"accessKey",key,"status","ABIERTA");
    }
    public Map<String,Object> require(String id) { return db.queryForList("select r.*,s.codigo_seguimiento from reclamacion r join envio s on s.id=r.envio_id where r.id=?",id).stream().findFirst().orElseThrow(()->new BusinessException("Reclamación no disponible")); }
    public Map<String,Object> detail(String id,boolean publicView) {
        Map<String,Object> row=new LinkedHashMap<>(require(id));row.remove("clave_hash");
        if(publicView) { row.remove("correo");row.remove("envio_id");row.remove("responsable"); }
        var messages=db.queryForList("select id,autor,texto,ocurrido_en from mensaje_reclamacion where reclamacion_id=? order by ocurrido_en,id",id);
        if(publicView) for(var m:messages) if(!m.get("autor").equals("CLIENTE")) m.put("autor","Equipo de atención");
        row.put("messages",messages);
        if(!publicView) row.put("analyses",db.queryForList("select id,modelo,resultado,actor,ocurrido_en from analisis_ia where reclamacion_id=? order by ocurrido_en desc",id));
        return row;
    }
    public void checkKey(String id,String key) {
        Map<String,Object> row;
        try { row=require(id); } catch(BusinessException e) { throw new BusinessException("Radicado o clave incorrectos"); }
        if(!java.security.MessageDigest.isEqual(((String)row.get("clave_hash")).getBytes(java.nio.charset.StandardCharsets.UTF_8),Accounts.hash(key).getBytes(java.nio.charset.StandardCharsets.UTF_8))) throw new BusinessException("Radicado o clave incorrectos");
    }
    public List<Map<String,Object>> list(String state,String query) {
        return db.queryForList("select r.id,r.envio_id,r.categoria,r.asunto,r.estado,r.responsable,r.creado_en,r.actualizado_en,r.version,s.codigo_seguimiento from reclamacion r join envio s on s.id=r.envio_id where (?='' or r.estado=?) and (?='' or lower(r.id) like ? or lower(s.codigo_seguimiento) like ?) order by r.creado_en desc limit 100",state,state,query,"%"+query.toLowerCase(Locale.ROOT)+"%","%"+query.toLowerCase(Locale.ROOT)+"%");
    }
    public Object assignees() { return accounts.list().stream().filter(u->u.active()&&u.permissions().contains("RECLAMOS")).map(u->Map.of("username",u.username(),"name",u.name())).toList(); }
    @Transactional public Map<String,Object> update(String id,Long version,String state,String assignee,String message,String actor,boolean publicView) {
        var row=require(id);String before=(String)row.get("estado");
        if(publicView) {
            if(before.equals("CERRADA")) throw new BusinessException("La reclamación está cerrada");
            if(message==null||message.isBlank()) throw new BusinessException("Escribe un mensaje");
            state=before.equals("RESUELTA")?"EN_REVISION":before;assignee=(String)row.get("responsable");actor="CLIENTE";
        } else {
            if(!Set.of("ABIERTA","EN_REVISION","RESUELTA","CERRADA").contains(state)) throw new BusinessException("Estado no permitido");
            if(state.equals("CERRADA")&&!Set.of("RESUELTA","CERRADA").contains(before)) throw new BusinessException("Resuelve el caso antes de cerrarlo");
            if(state.equals("RESUELTA") && !before.equals("RESUELTA") && (message==null||message.strip().length()<10)) throw new BusinessException("Describe la solución para resolver el caso");
            if(assignee!=null&&!assignee.isBlank()) {
                Accounts.View user=accounts.get(assignee);if(!user.active()||!user.permissions().contains("RECLAMOS")) throw new BusinessException("El responsable debe estar activo y tener permiso de reclamaciones");
            } else assignee=null;
        }
        if(db.update("update reclamacion set estado=?,responsable=?,actualizado_en=?,version=version+1 where id=? and version=?",state,assignee,OffsetDateTime.now(clock),id,version)!=1) throw new ShipmentConflictException("La reclamación cambió; vuelve a consultarla");
        if(message!=null&&!message.isBlank()) db.update("insert into mensaje_reclamacion(reclamacion_id,autor,texto,ocurrido_en) values (?,?,?,?)",id,actor,message.strip(),OffsetDateTime.now(clock));
        return detail(id,publicView);
    }
}
