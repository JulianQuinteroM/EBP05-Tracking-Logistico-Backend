package co.edu.udea.codefactory.tracking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sprint2;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.security.operator-username=operador", "app.security.operator-password=testing-password-123456",
    "app.security.admin-username=administrador", "app.security.admin-password=admin-password-123456",
    "app.security.viewer-username=consulta", "app.security.viewer-password=viewer-password-123456"
})
class Sprint2ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ShipmentRepository shipments;
    @Autowired ShipmentEventRepository events;
    @Autowired ShipmentAuditRepository audits;

    private String input() {
        return """
            {"senderName":"Ana Gómez","senderPhone":"+57 300 1234567","senderEmail":"ana@example.com",
             "recipientName":"Luis Ruiz","recipientPhone":"+57 310 7654321","recipientEmail":"luis@example.com",
             "destinationStreet":"Calle 10","destinationNumber":"20","destinationCity":"Medellín",
             "weightKg":2.50,"lengthCm":30,"widthCm":20,"heightCm":15,
             "packagingType":"CAJA","serviceType":"ESTANDAR"}
            """;
    }

    private JsonNode create() throws Exception {
        String body = mvc.perform(post("/api/envios").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(input()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode change(long id, long version, String status, String reason) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("version", version, "status", status,
            "reason", reason == null ? "" : reason));
        return json.readTree(mvc.perform(post("/api/envios/" + id + "/estado")
                .with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test void physicalDataJourneyLocationPublicPrivacyAndAudit() throws Exception {
        mvc.perform(post("/api/envios").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(input().replace("\"lengthCm\":30", "\"lengthCm\":201")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.lengthCm").exists());
        mvc.perform(post("/api/envios").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(input().replace("\"packagingType\":\"CAJA\"", "\"packagingType\":\"OTRO\"")))
            .andExpect(status().isBadRequest());
        JsonNode created = create();
        long id = created.get("id").asLong();
        String code = created.get("trackingCode").asText();
        long version = created.get("version").asLong();
        mvc.perform(post("/api/envios/" + id + "/ubicaciones")
                .with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + ",\"city\":\"Bogotá\"}"))
            .andExpect(status().isConflict());

        JsonNode picked = change(id, version, "RECOGIDO", null);
        version = picked.get("version").asLong();
        String locatedBody = mvc.perform(post("/api/envios/" + id + "/ubicaciones")
                .with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + ",\"city\":\"Medellin\",\"reference\":\"Centro principal\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.currentCity").value("Medellín"))
            .andReturn().getResponse().getContentAsString();
        version = json.readTree(locatedBody).get("version").asLong();
        String editedBody = mvc.perform(put("/api/envios/" + id).with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + ",\"shipment\":" + input().replace("\"weightKg\":2.50", "\"weightKg\":3.25") + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.shipment.weightKg").value(3.25))
            .andReturn().getResponse().getContentAsString();
        version = json.readTree(editedBody).get("version").asLong();
        JsonNode transit = change(id, version, "EN_TRANSITO", null);
        version = transit.get("version").asLong();
        mvc.perform(post("/api/envios/" + id + "/estado")
                .with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + ",\"status\":\"ENTREGADO\"}"))
            .andExpect(status().isConflict());
        JsonNode incident = change(id, version, "INCIDENCIA", "Retraso por revisión operativa");
        assertTrue(incident.get("events").toString().contains("INCIDENCIA"));
        mvc.perform(get("/api/seguimiento/" + code))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INCIDENCIA"))
            .andExpect(jsonPath("$.journey[2].phase").value("ACTUAL"));
        JsonNode resumed = change(id, incident.get("version").asLong(), "EN_TRANSITO", null);
        assertTrue(resumed.get("allowedStatusTransitions").toString().contains("CENTRO_DISTRIBUCION"));

        String publicBody = mvc.perform(get("/api/seguimiento/" + code))
            .andExpect(status().isOk()).andExpect(jsonPath("$.currentCity").value("Medellín"))
            .andExpect(jsonPath("$.maskedSenderName").value("Ana G."))
            .andExpect(jsonPath("$.journey").isArray())
            .andReturn().getResponse().getContentAsString();
        assertFalse(publicBody.contains("ana@example.com"));
        assertFalse(publicBody.contains("Calle 10"));
        assertFalse(publicBody.contains("Centro principal"));
        assertFalse(publicBody.contains("Gómez"));

        mvc.perform(get("/api/envios/" + id).with(httpBasic("consulta", "viewer-password-123456")))
            .andExpect(status().isOk());
        mvc.perform(post("/api/envios/" + id + "/estado").with(httpBasic("consulta", "viewer-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"status\":\"ENTREGADO\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/auditoria").with(httpBasic("operador", "testing-password-123456")))
            .andExpect(status().isForbidden());
        String audit = mvc.perform(get("/api/admin/auditoria?shipmentId=" + id)
                .with(httpBasic("administrador", "admin-password-123456")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(audit.contains("TRANSICION_INUSUAL"));
        assertTrue(audit.contains("UBICACION"));
        assertTrue(audit.contains("INTENTO_NO_AUTORIZADO"));
        assertTrue(audit.contains("CONSULTA_HISTORIAL"));
        assertTrue(audit.contains("REGISTRO"));
        mvc.perform(delete("/api/admin/auditoria/1").with(httpBasic("administrador", "admin-password-123456")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("El historial de movimientos no puede modificarse"));
        mvc.perform(get("/api/admin/auditoria/exportacion?shipmentId=" + id)
                .with(httpBasic("administrador", "admin-password-123456")))
            .andExpect(status().isOk());
        JsonNode distribution = change(id, resumed.get("version").asLong(), "CENTRO_DISTRIBUCION", null);
        JsonNode route = change(id, distribution.get("version").asLong(), "RUTA_ENTREGA", null);
        JsonNode delivered = change(id, route.get("version").asLong(), "ENTREGADO", null);
        mvc.perform(get("/api/seguimiento/" + code))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGADO"));
        mvc.perform(post("/api/envios/" + id + "/ubicaciones")
                .with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + delivered.get("version").asLong() + ",\"city\":\"Bogotá\"}"))
            .andExpect(status().isConflict());
    }

    @Test void citySuggestionsAndInvalidLocation() throws Exception {
        mvc.perform(get("/api/ciudades?q=med").with(httpBasic("consulta", "viewer-password-123456")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Medellín"));
        JsonNode created = create();
        JsonNode picked = change(created.get("id").asLong(), created.get("version").asLong(), "RECOGIDO", null);
        mvc.perform(post("/api/envios/" + created.get("id").asLong() + "/ubicaciones")
                .with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + picked.get("version").asLong() + ",\"city\":\"Ciudad inventada\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void detectsInconsistentPersistedHistoryWithoutDuplicatingAlerts() throws Exception {
        JsonNode created = create();
        long id = created.get("id").asLong();
        Shipment shipment = shipments.findById(id).orElseThrow();
        ShipmentEvent inconsistent = new ShipmentEvent(shipment, "ESTADO", "Evento histórico inconsistente", OffsetDateTime.now());
        inconsistent.previousStatus = "PENDIENTE_RECOGIDA";
        inconsistent.resultingStatus = "ENTREGADO";
        events.save(inconsistent);
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/admin/auditoria?shipmentId=" + id)
                    .with(httpBasic("administrador", "admin-password-123456")))
                .andExpect(status().isOk());
        }
        long alerts = audits.findByShipmentReferenceOrderByOccurredAtDescIdDesc(id).stream()
            .filter(row -> "TRANSICION_INUSUAL".equals(row.action)
                && ("evento:" + inconsistent.id).equals(row.field)).count();
        org.junit.jupiter.api.Assertions.assertEquals(1L, alerts);
    }
}
