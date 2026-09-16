package co.edu.udea.codefactory.tracking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sprint1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.security.operator-username=operador",
    "app.security.operator-password=testing-password-123456"
})
class Sprint1ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ShipmentEventRepository events;
    @Autowired ShipmentAuditRepository audits;

    private String shipmentJson() {
        return """
            {"senderName":"Ana","senderPhone":"+57 300 1234567","senderEmail":"ana@example.com",
             "recipientName":"Luis","recipientPhone":"+57 310 7654321","recipientEmail":"luis@example.com",
             "destinationStreet":"Calle 10","destinationNumber":"20","destinationCity":"Medellín",
             "weightKg":2.500,"serviceType":"ESTANDAR"}
            """;
    }

    @Test void createTrackListEditCancelAndPrivacy() throws Exception {
        mvc.perform(post("/api/envios").contentType(MediaType.APPLICATION_JSON).content(shipmentJson()))
            .andExpect(status().isUnauthorized());
        String createdBody = mvc.perform(post("/api/envios").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(shipmentJson()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDIENTE_RECOGIDA"))
            .andReturn().getResponse().getContentAsString();
        JsonNode created = json.readTree(createdBody);
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();
        String code = created.get("trackingCode").asText();

        mvc.perform(get("/api/seguimiento/" + code.toLowerCase().replace("-", "")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.trackingCode").value(code))
            .andExpect(jsonPath("$.senderName").doesNotExist())
            .andExpect(jsonPath("$.destinationStreet").doesNotExist());
        mvc.perform(get("/api/envios")) .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/envios/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/envios").with(httpBasic("operador", "testing-password-123456")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].trackingCode").value(code));
        mvc.perform(get("/api/envios?q=Ana").with(httpBasic("operador", "testing-password-123456")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/envios?q=%25").with(httpBasic("operador", "testing-password-123456")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/envios?size=10").with(httpBasic("operador", "testing-password-123456")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/estimaciones").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceType\":\"EXPRES\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deliveryWindow").value("24-48 horas"));

        String editJson = "{\"version\":" + version + ",\"shipment\":"
            + shipmentJson().replace("Calle 10", "Calle 11") + "}";
        String editedBody = mvc.perform(put("/api/envios/" + id).with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(editJson))
            .andExpect(status().isOk()).andExpect(jsonPath("$.shipment.destinationStreet").value("Calle 11"))
            .andExpect(jsonPath("$.trackingCode").value(code))
            .andReturn().getResponse().getContentAsString();
        long editedVersion = json.readTree(editedBody).get("version").asLong();
        mvc.perform(put("/api/envios/" + id).with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(editJson))
            .andExpect(status().isConflict());

        String cancellation = "{\"version\":" + editedVersion + ",\"reason\":\"Error de registro\"}";
        mvc.perform(post("/api/envios/" + id + "/cancelacion").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(cancellation))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(post("/api/envios/" + id + "/cancelacion").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(cancellation))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/seguimiento/" + code))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        org.junit.jupiter.api.Assertions.assertEquals(2L, events.countByShipmentId(id));
        org.junit.jupiter.api.Assertions.assertEquals(2L, audits.countByShipmentId(id));
    }

    @Test void invalidInputAndForgedFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/envios").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(shipmentJson().replace("2.500", "0")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.weightKg").exists());
        mvc.perform(post("/api/envios").with(httpBasic("operador", "testing-password-123456"))
                .contentType(MediaType.APPLICATION_JSON).content(shipmentJson().replace("\"serviceType\"", "\"status\":\"CANCELADO\",\"serviceType\"")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_JSON"));
        mvc.perform(get("/api/seguimiento/LOG-29047381-7"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TRACKING_CODE"));
    }
}
