package co.edu.udea.codefactory.tracking;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTest {
    private final ObjectMapper json=new ObjectMapper();
    @Test void sendsStructuredRequestAndParsesProviderResponse() throws Exception {
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        var result=Map.of("summary","El envío tiene un evento registrado","priority","MEDIA","findings",List.of(Map.of("eventId",1,"observation","Registro documentado")),"actions",List.of("Consultar al equipo"),"missingInfo",List.of("Confirmar el motivo"),"draftReply","Estamos revisando el caso con el equipo operativo.");
        String response=json.writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",json.writeValueAsString(result))))))));
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent")).andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key","test-only-key"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("responseSchema"))).andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
        var client=new GeminiClient("test-only-key","test-model",json,builder.build());var parsed=client.analyze(Map.of("caseText","Consulta de prueba"));
        assertEquals("MEDIA",parsed.path("priority").asText());ComplaintAi.validate(parsed,Set.of(1L));server.verify();
    }
    @Test void quotaFailureIsExplicitAndDoesNotLeakCredentials() {
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        var client=new GeminiClient("test-only-key","test-model",json,builder.build());
        var error=assertThrows(GeminiClient.Unavailable.class,()->client.analyze(Map.of("caseText","Prueba")));
        assertTrue(error.getMessage().contains("cuota"));assertFalse(error.getMessage().contains("test-only-key"));server.verify();
    }
    @Test void truncatedGenerationIsNeverAcceptedAsSuccessfulAnalysis() throws Exception {
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent")).andRespond(withSuccess("{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}",MediaType.APPLICATION_JSON));
        var client=new GeminiClient("test-only-key","test-model",json,builder.build());
        assertThrows(GeminiClient.Unavailable.class,()->client.analyze(Map.of("caseText","Prueba")));server.verify();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"400,API_KEY_INVALID,clave de API","403,API_KEY_SERVICE_BLOCKED,denegó el acceso","404,NOT_FOUND,modelo configurado","503,UNAVAILABLE,temporalmente"})
    void distinguishesProviderErrorsWithoutExposingResponseBody(int status,String reason,String expected) {
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent")).andRespond(withStatus(HttpStatus.valueOf(status))
            .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"status\":\"UNKNOWN\",\"message\":\"private complaint text test-only-key\",\"details\":[{\"reason\":\""+reason+"\"}]}}"));
        var client=new GeminiClient("test-only-key","test-model",json,builder.build());
        var error=assertThrows(GeminiClient.Unavailable.class,()->client.analyze(Map.of("caseText","Private complaint")));
        assertTrue(error.getMessage().contains(expected));assertTrue(error.getMessage().contains(String.valueOf(status)));assertFalse(error.getMessage().contains("test-only-key"));assertFalse(error.getMessage().contains("private complaint"));server.verify();
    }
    @Test void distinguishesConnectionTimeout() {
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent")).andRespond(withException(new java.net.SocketTimeoutException("private test-only-key")));
        var client=new GeminiClient("test-only-key","test-model",json,builder.build());
        var error=assertThrows(GeminiClient.Unavailable.class,()->client.analyze(Map.of("caseText","Private complaint")));
        assertTrue(error.getMessage().contains("45 segundos"));assertFalse(error.getMessage().contains("test-only-key"));server.verify();
    }
}
