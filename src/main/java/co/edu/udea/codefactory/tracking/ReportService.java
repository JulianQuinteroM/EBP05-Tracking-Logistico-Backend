package co.edu.udea.codefactory.tracking;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final JdbcTemplate db; private final Clock clock;
    public ReportService(JdbcTemplate db,Clock clock) { this.db=db;this.clock=clock; }
    public static final ZoneId ZONE=ZoneId.of("America/Bogota");
    public static Instant instant(Object value) {
        if(value instanceof OffsetDateTime t) return t.toInstant();
        if(value instanceof java.sql.Timestamp t) return t.toInstant();
        return Instant.parse(value.toString());
    }
    public static String csv(Object value) {
        String s=value==null?"":value.toString();
        if(s.stripLeading().matches("^[=+@\\-\\t\\r].*")) s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static String export(List<Map<String,Object>> rows,List<String> columns) {
        StringBuilder out=new StringBuilder("\uFEFF").append(String.join(",",columns)).append("\r\n");
        for(var row:rows) out.append(columns.stream().map(c->csv(row.get(c))).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        return out.toString();
    }
    @Transactional(readOnly=true) public Map<String,Object> report(LocalDate from,LocalDate to,String group) {
        LocalDate today=LocalDate.now(clock.withZone(ZONE));
        if(from==null) from=today.minusDays(29); if(to==null) to=today;
        if(from.isAfter(to)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>1095) throw new BusinessException("Selecciona un periodo válido de hasta tres años");
        if(!Set.of("DIA","SEMANA","MES").contains(group)) throw new BusinessException("Agrupación no permitida");
        var rows=db.queryForList("""
            select s.id,s.codigo_seguimiento,s.estado,s.tipo_servicio,s.creado_en,s.fecha_entrega_estimada,
              e.recogido_en,e.entregado_en from envio s left join
              (select envio_id,min(case when estado_resultante='RECOGIDO' and tipo='ESTADO' then ocurrido_en end) as recogido_en,
               min(case when estado_resultante='ENTREGADO' and tipo='ESTADO' then ocurrido_en end) as entregado_en
               from evento_envio group by envio_id) e on e.envio_id=s.id
              where s.creado_en>=? and s.creado_en<? order by s.creado_en
            """,from.atStartOfDay(ZONE).toOffsetDateTime(),to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime());
        List<Map<String,Object>> delayed=new ArrayList<>();Map<String,Long> volume=new TreeMap<>();Map<String,Long> states=new TreeMap<>();
        double hours=0;int delivered=0,onTime=0;int transitSamples=0;
        for(var row:rows) {
            LocalDate date=instant(row.get("creado_en")).atZone(ZONE).toLocalDate();
            String period=switch(group) { case "MES"->date.withDayOfMonth(1).toString();case "SEMANA"->date.with(java.time.DayOfWeek.MONDAY).toString();default->date.toString(); };
            volume.merge(period,1L,Long::sum);String state=(String)row.get("estado");states.merge(state,1L,Long::sum);
            LocalDate estimated=LocalDate.parse(row.get("fecha_entrega_estimada").toString());
            if(!Set.of("ENTREGADO","CANCELADO").contains(state)&&estimated.isBefore(today)) {
                Map<String,Object> item=new LinkedHashMap<>(row);item.put("dias_retraso",java.time.temporal.ChronoUnit.DAYS.between(estimated,today));delayed.add(item);
            }
            if(state.equals("ENTREGADO")) {
                delivered++;
                if(row.get("entregado_en")!=null && !instant(row.get("entregado_en")).atZone(ZONE).toLocalDate().isAfter(estimated)) onTime++;
                if(row.get("recogido_en")!=null&&row.get("entregado_en")!=null) { double h=Duration.between(instant(row.get("recogido_en")),instant(row.get("entregado_en"))).toMillis()/3_600_000.0;if(h>=0) { hours+=h;transitSamples++; } }
            }
        }
        delayed.sort(Comparator.comparingLong(r->-((Number)r.get("dias_retraso")).longValue()));
        Map<String,Object> out=new LinkedHashMap<>();out.put("from",from);out.put("to",to);out.put("group",group);out.put("total",rows.size());out.put("delivered",delivered);
        out.put("onTimePercent",delivered==0?null:Math.round(onTime*10000.0/delivered)/100.0);
        out.put("averageTransitHours",transitSamples==0?null:Math.round(hours/transitSamples*100.0)/100.0);out.put("transitSamples",transitSamples);
        out.put("delayed",delayed);out.put("states",states);
        out.put("volume",volume.entrySet().stream().map(e->Map.of("period",e.getKey(),"total",e.getValue())).toList());
        out.put("complaints",db.queryForList("select estado,count(*) as total from reclamacion where creado_en>=? and creado_en<? group by estado",from.atStartOfDay(ZONE).toOffsetDateTime(),to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime()));
        return out;
    }
    public String shipmentsCsv(String query) {
        if(query.length()>120) throw new BusinessException("Búsqueda demasiado larga");
        String q="%"+query.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        var rows=db.queryForList("select codigo_seguimiento,remitente_nombre,destinatario_nombre,estado,tipo_servicio,peso_kg,largo_cm,ancho_cm,alto_cm,tipo_empaque,destino_ciudad,fecha_entrega_estimada,creado_en from envio where lower(codigo_seguimiento) like ? escape '!' or lower(remitente_nombre) like ? escape '!' or lower(destinatario_nombre) like ? escape '!' order by creado_en desc",q,q,q);
        return export(rows,List.of("codigo_seguimiento","remitente_nombre","destinatario_nombre","estado","tipo_servicio","peso_kg","largo_cm","ancho_cm","alto_cm","tipo_empaque","destino_ciudad","fecha_entrega_estimada","creado_en"));
    }
}
