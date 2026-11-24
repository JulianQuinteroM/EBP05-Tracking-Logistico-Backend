package co.edu.udea.codefactory.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:sprint3;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "app.security.operator-username=operador","app.security.operator-password=testing-password-123456","app.security.admin-username=administrador","app.security.admin-password=admin-password-123456","app.security.viewer-username=consulta","app.security.viewer-password=viewer-password-123456","app.ai.key="})
class Sprint3ApiIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;@Autowired Clock clock;@Autowired ComplaintAi ai;
    MockHttpServletRequestBuilder operator(MockHttpServletRequestBuilder b) { return b.with(httpBasic("operador","testing-password-123456")); }
    MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder b) { return b.with(httpBasic("administrador","admin-password-123456")); }
    JsonNode call(MockHttpServletRequestBuilder b,Object body) throws Exception { return json.readTree(mvc.perform(b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()); }
    JsonNode create() throws Exception {
        return json.readTree(mvc.perform(operator(post("/api/envios")).contentType(MediaType.APPLICATION_JSON).content("""
            {"senderName":"Ana Gómez","senderPhone":"+57 300 1234567","senderEmail":"ana@example.com","recipientName":"Luis Ruiz","recipientPhone":"+57 310 7654321","recipientEmail":"luis@example.com","destinationStreet":"Calle 10","destinationNumber":"20","destinationCity":"Medellín","weightKg":2.5,"lengthCm":30,"widthCm":20,"heightCm":15,"packagingType":"CAJA","serviceType":"ESTANDAR"}
            """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    JsonNode route(JsonNode s) throws Exception { for(String next:List.of("RECOGIDO","EN_TRANSITO","CENTRO_DISTRIBUCION","RUTA_ENTREGA")) s=call(operator(post("/api/envios/"+s.path("id").asLong()+"/estado")),Map.of("version",s.path("version").asLong(),"status",next));return s; }
    JsonNode claim(JsonNode s) throws Exception { return call(post("/api/reclamaciones/publicas"),Map.of("trackingCode",s.path("trackingCode").asText(),"email","ana@example.com","category","ENTREGA","subject","Consulta de entrega","description","Necesito saber por qué mi paquete no ha sido entregado todavía.")); }
    @Test void persistentAccountsCustomPermissionsAndImmediateRevocation() throws Exception {
        String username="prueba-"+UUID.randomUUID().toString().substring(0,8);
        JsonNode account=call(admin(put("/api/admin/usuarios/"+username)),Map.of("name","Persona prueba","role","REPARTIDOR","permissions",List.of("ENVIO_LEER"),"password","temporary-test-password","active",true));
        JsonNode login=call(post("/api/auth/login"),Map.of("username",username,"password","temporary-test-password"));String token=login.path("token").asText();
        mvc.perform(get("/api/envios").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/reportes").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        call(admin(put("/api/admin/usuarios/"+username)),Map.of("name","Persona prueba","role","REPARTIDOR","permissions",List.of("ENVIO_LEER"),"active",false,"version",account.path("version").asLong()));
        mvc.perform(get("/api/envios").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",username,"password","temporary-test-password")))).andExpect(status().isUnauthorized());
        JsonNode me=json.readTree(mvc.perform(admin(get("/api/auth/me"))).andReturn().getResponse().getContentAsString());
        mvc.perform(admin(put("/api/admin/usuarios/administrador")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Admin","role","ADMIN","active",false,"version",me.path("version").asLong())))).andExpect(status().isBadRequest());
    }
    @Test void contactsHistoryAndContactCorrectionPreserveOtherShipments() throws Exception {
        JsonNode one=create(),two=create();long id=one.path("id").asLong();
        long contact=db.queryForObject("select remitente_contacto_id from envio where id=?",Long.class,id);
        call(operator(put("/api/contactos/"+contact)),Map.of("name","Ana Gómez","document","CC12345678","email","ana@example.com","phone","+57 300 1234567","version",db.queryForObject("select version from contacto where id=?",Long.class,contact)));
        mvc.perform(operator(get("/api/contactos?q=CC12345678"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(contact));
        String history=mvc.perform(operator(get("/api/contactos/"+contact+"/envios"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(history.contains(two.path("trackingCode").asText()));
        JsonNode picked=call(operator(post("/api/envios/"+id+"/estado")),Map.of("version",one.path("version").asLong(),"status","RECOGIDO"));
        JsonNode edited=call(operator(post("/api/envios/"+id+"/contactos")),Map.of("version",picked.path("version").asLong(),"senderPhone","+57 300 7654321","senderEmail","nuevo@example.com","recipientPhone","+57 310 7654321","recipientEmail","luis@example.com"));
        assertEquals("nuevo@example.com",edited.path("shipment").path("senderEmail").asText());assertEquals("ana@example.com",db.queryForObject("select remitente_correo from envio where id=?",String.class,two.path("id").asLong()));
    }
    @Test void failedAttemptResolutionAndPrivateDeliveryEvidence() throws Exception {
        JsonNode s=route(create());long id=s.path("id").asLong();
        mvc.perform(operator(post("/api/envios/"+id+"/estado")).contentType(MediaType.APPLICATION_JSON).content("{\"version\":"+s.path("version").asLong()+",\"status\":\"ENTREGADO\"}")).andExpect(status().isBadRequest());
        s=call(operator(post("/api/envios/"+id+"/intentos")),Map.of("version",s.path("version").asLong(),"reason","El destinatario no estaba disponible"));assertEquals("INCIDENCIA",s.path("status").asText());
        LocalDate next=db.queryForObject("select proxima_fecha from intento_entrega where envio_id=?",LocalDate.class,id);assertTrue(next.isAfter(LocalDate.now(clock.withZone(ReportService.ZONE))));assertFalse(Set.of(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY).contains(next.getDayOfWeek()));
        s=call(operator(post("/api/envios/"+id+"/incidencia")),Map.of("version",s.path("version").asLong(),"reason","Destinatario confirmó disponibilidad para el siguiente intento"));
        assertEquals("RUTA_ENTREGA",s.path("status").asText());
        var image=new java.awt.image.BufferedImage(30,30,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        s=call(operator(post("/api/envios/"+id+"/entrega")),Map.of("version",s.path("version").asLong(),"receiver","Luis Ruiz","type","FOTO","mime","image/png","image",Base64.getEncoder().encodeToString(bytes.toByteArray())));assertEquals("ENTREGADO",s.path("status").asText());
        mvc.perform(get("/api/envios/"+id+"/evidencia")).andExpect(status().isUnauthorized());
        mvc.perform(operator(get("/api/envios/"+id+"/evidencia"))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        String pub=mvc.perform(get("/api/seguimiento/"+s.path("trackingCode").asText())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(pub.contains("receptor"));assertFalse(pub.contains("Luis Ruiz"));
        mvc.perform(admin(get("/api/admin/auditoria?shipmentId="+id))).andExpect(status().isOk());
        assertEquals(0,db.queryForObject("select count(*) from auditoria_envio where envio_referencia=? and accion='TRANSICION_INUSUAL'",Integer.class,id));
    }
    @Test void complaintPrivateKeyConversationAndClosure() throws Exception {
        JsonNode s=create(),c=claim(s);String id=c.path("id").asText(),key=c.path("accessKey").asText();
        mvc.perform(post("/api/reclamaciones/consulta").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id",id,"accessKey","incorrecta")))).andExpect(status().isBadRequest());
        JsonNode detail=call(post("/api/reclamaciones/consulta"),Map.of("id",id,"accessKey",key));assertFalse(detail.has("correo"));assertFalse(detail.has("clave_hash"));
        detail=call(operator(put("/api/reclamaciones/"+id)),Map.of("version",detail.path("version").asLong(),"status","RESUELTA","assignee","operador","message","Verificamos el envío y coordinamos la entrega con el equipo."));
        JsonNode pub=call(post("/api/reclamaciones/consulta"),Map.of("id",id,"accessKey",key));assertTrue(pub.path("messages").get(0).path("texto").asText().contains("Verificamos"));assertFalse(pub.toString().contains("operador"));
        JsonNode closed=call(operator(put("/api/reclamaciones/"+id)),Map.of("version",detail.path("version").asLong(),"status","CERRADA","assignee","operador","message",""));
        mvc.perform(post("/api/reclamaciones/mensaje").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id",id,"accessKey",key,"version",closed.path("version").asLong(),"message","Otro mensaje")))).andExpect(status().isBadRequest());
    }
    @Test void reportCalculatesTransitFromEventsAndExcludesCancelledFromDelay() throws Exception {
        JsonNode s=create(),cancel=create(),late=create();long id=s.path("id").asLong(),other=cancel.path("id").asLong();
        db.update("update envio set estado='EN_TRANSITO',creado_en=?,fecha_entrega_estimada=? where id=?",OffsetDateTime.parse("2020-01-01T10:00:00Z"),LocalDate.parse("2020-01-02"),late.path("id").asLong());
        db.update("update envio set estado='ENTREGADO',creado_en=?,fecha_entrega_estimada=? where id=?",OffsetDateTime.parse("2020-01-01T10:00:00Z"),LocalDate.parse("2020-01-05"),id);
        db.update("update envio set estado='CANCELADO',creado_en=?,fecha_entrega_estimada=? where id=?",OffsetDateTime.parse("2020-01-01T10:00:00Z"),LocalDate.parse("2020-01-01"),other);
        for(var e:List.of(Map.of("state","RECOGIDO","time","2020-01-02T10:00:00Z"),Map.of("state","ENTREGADO","time","2020-01-03T10:00:00Z"))) db.update("insert into evento_envio(envio_id,tipo,estado_resultante,publico,ocurrido_en,descripcion) values (?,'ESTADO',?,true,?,'Prueba')",id,e.get("state"),OffsetDateTime.parse(e.get("time")));
        mvc.perform(operator(get("/api/reportes?from=2020-01-01&to=2020-01-31"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.delayed.length()").value(1)).andExpect(jsonPath("$.delayed[0].id").value(late.path("id").asLong())).andExpect(jsonPath("$.averageTransitHours").value(24.0)).andExpect(jsonPath("$.onTimePercent").value(100.0));
        mvc.perform(operator(get("/api/reportes?from=2019-01-01&to=2019-01-31"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.averageTransitHours").isEmpty());
        mvc.perform(operator(get("/api/reportes/exportacion?from=2020-01-01&to=2020-01-31&type=resumen"))).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"));
        assertEquals("\"'=FORMULA\"",ReportService.csv("=FORMULA"));
    }
    @Test void aiContextIsReducedAndMissingKeyNeverFakesAnAnswer() throws Exception {
        JsonNode c=claim(create());String id=c.path("id").asText();
        String context=json.writeValueAsString(ai.context(id));assertFalse(context.contains("ana@example.com"));assertFalse(context.contains("Calle 10"));assertFalse(context.contains("Ana Gómez"));
        mvc.perform(operator(post("/api/reclamaciones/"+id+"/analisis")).contentType(MediaType.APPLICATION_JSON).content("{\"caseText\":\"Mi paquete no ha sido entregado\"}")).andExpect(status().isServiceUnavailable());
        assertEquals(0,db.queryForObject("select count(*) from analisis_ia where reclamacion_id=?",Integer.class,id));
        JsonNode invented=json.readTree("{\"summary\":\"Resumen\",\"priority\":\"MEDIA\",\"findings\":[{\"eventId\":999,\"observation\":\"Supuesto\"}],\"actions\":[],\"missingInfo\":[],\"draftReply\":\"Borrador\"}");
        assertThrows(GeminiClient.Unavailable.class,()->ComplaintAi.validate(invented,Set.of(1L)));
    }
}
