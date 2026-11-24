package co.edu.udea.codefactory.tracking;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

@Service
public class ComplaintAi {
    private final JdbcTemplate db;private final ComplaintService complaints;private final GeminiClient gemini;private final Accounts accounts;private final ObjectMapper json;private final Clock clock;
    private final Semaphore processing=new Semaphore(1);
    public ComplaintAi(JdbcTemplate db,ComplaintService complaints,GeminiClient gemini,Accounts accounts,ObjectMapper json,Clock clock) { this.db=db;this.complaints=complaints;this.gemini=gemini;this.accounts=accounts;this.json=json;this.clock=clock; }
    private String sanitize(String text,Map<String,Object> shipment) {
        String value=text;
        for(String column:List.of("remitente_nombre","destinatario_nombre","remitente_telefono","destinatario_telefono","remitente_correo","destinatario_correo","destino_calle","destino_numero")) {
            Object sensitive=shipment.get(column);if(sensitive!=null&&!sensitive.toString().isBlank()) value=value.replaceAll("(?i)(?<![\\p{L}\\p{N}])"+java.util.regex.Pattern.quote(sensitive.toString())+"(?![\\p{L}\\p{N}])","[dato omitido]");
        }
        return value.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}","[correo omitido]")
            .replaceAll("(?<![A-Za-z])\\+?\\d[\\d ()-]{5,}\\d","[número omitido]");
    }
    public Map<String,Object> context(String id) {
        var complaint=complaints.require(id);var s=db.queryForMap("select * from envio where id=?",complaint.get("envio_id"));
        var events=db.queryForList("select id,tipo,estado_anterior,estado_resultante,ciudad,ocurrido_en from evento_envio where envio_id=? order by ocurrido_en desc,id desc limit 60",complaint.get("envio_id"));
        Map<String,Object> ctx=new LinkedHashMap<>();ctx.put("category",complaint.get("categoria"));ctx.put("caseText",sanitize(complaint.get("asunto")+". "+complaint.get("descripcion"),s));
        ctx.put("status",s.get("estado"));ctx.put("estimatedDelivery",s.get("fecha_entrega_estimada"));ctx.put("today",LocalDate.now(clock.withZone(ReportService.ZONE)));ctx.put("events",events);
        return Map.of("configured",gemini.configured(),"model",gemini.model(),"context",ctx);
    }
    @SuppressWarnings("unchecked") public JsonNode analyze(String id,String reviewedText,String actor) {
        if(!accounts.get(actor).permissions().contains("RECLAMOS")) throw new AccessDeniedException("Requiere acceso a reclamaciones");
        if(!processing.tryAcquire()) throw new GeminiClient.Unavailable("Hay otro análisis en curso; espera un momento");
        try {
            int count=db.queryForObject("select count(*) from analisis_ia where actor=? and ocurrido_en>=?",Integer.class,actor,LocalDate.now(clock.withZone(ReportService.ZONE)).atStartOfDay(ReportService.ZONE).toOffsetDateTime());
            if(count>=20) throw new GeminiClient.Unavailable("Límite local: 20 análisis diarios por usuario");
            var complaint=complaints.require(id);var s=db.queryForMap("select * from envio where id=?",complaint.get("envio_id"));
            Map<String,Object> ctx=new LinkedHashMap<>((Map<String,Object>)context(id).get("context"));ctx.put("caseText",sanitize(reviewedText,s));
            JsonNode result=gemini.analyze(ctx);Set<Long> ids=new HashSet<>();for(var event:(List<Map<String,Object>>)ctx.get("events")) ids.add(((Number)event.get("id")).longValue());
            validate(result,ids);
            db.update("insert into analisis_ia(reclamacion_id,modelo,resultado,actor,ocurrido_en) values (?,?,?,?,?)",id,gemini.model(),result.toString(),actor,OffsetDateTime.now(clock));
            return result;
        } finally { processing.release(); }
    }
    public static void validate(JsonNode result,Set<Long> ids) {
        if(result==null||!result.isObject()||!result.path("summary").isTextual()||result.path("summary").asText().isBlank()||result.path("summary").asText().length()>4000
            ||!Set.of("BAJA","MEDIA","ALTA").contains(result.path("priority").asText())||!result.path("draftReply").isTextual()||result.path("draftReply").asText().length()>3000)
            throw new GeminiClient.Unavailable("La IA devolvió un análisis inválido; no se guardó");
        for(String field:List.of("actions","missingInfo","findings")) if(!result.path(field).isArray()||result.path(field).size()>20) throw new GeminiClient.Unavailable("Formato de análisis inválido");
        for(JsonNode finding:result.path("findings")) if(!finding.path("eventId").isIntegralNumber()||!ids.contains(finding.path("eventId").asLong())||!finding.path("observation").isTextual()||finding.path("observation").asText().length()>2000)
            throw new GeminiClient.Unavailable("La IA citó un evento inexistente; no se guardó el análisis");
        for(String field:List.of("actions","missingInfo")) for(JsonNode text:result.path(field)) if(!text.isTextual()||text.asText().length()>2000) throw new GeminiClient.Unavailable("Formato de análisis inválido");
    }
}
