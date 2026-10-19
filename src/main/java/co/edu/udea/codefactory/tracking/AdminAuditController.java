package co.edu.udea.codefactory.tracking;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auditoria")
public class AdminAuditController {
    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) { this.auditService = auditService; }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<java.util.Map<String, String>> immutable() {
        return ResponseEntity.status(409).body(java.util.Map.of("code", "IMMUTABLE_HISTORY",
            "message", "El historial de movimientos no puede modificarse"));
    }

    @GetMapping
    public List<ShipmentResponses.AuditView> history(
        @RequestParam(required = false) Long shipmentId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String actor,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Authentication authentication, HttpServletRequest request) {
        auditService.verifySequences(shipmentId);
        List<ShipmentResponses.AuditView> result = auditService.history(shipmentId, action, actor, from, to);
        auditService.recordAction("CONSULTA_AUDITORIA", authentication.getName(), shipmentId,
            request.getRemoteAddr(), "Consulta de historial de auditoría");
        return result;
    }

    @GetMapping(value = "/exportacion", produces = "text/csv")
    public ResponseEntity<byte[]> export(
        @RequestParam(required = false) Long shipmentId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String actor,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Authentication authentication, HttpServletRequest request) {
        auditService.verifySequences(shipmentId);
        List<ShipmentResponses.AuditView> rows = auditService.history(shipmentId, action, actor, from, to);
        StringBuilder csv = new StringBuilder("\uFEFFid,envio_id,accion,usuario,campo,valor_anterior,valor_nuevo,ip,fecha_hora,detalle\r\n");
        for (var row : rows) {
            csv.append(cell(row.id())).append(',').append(cell(row.shipmentId())).append(',')
                .append(cell(row.action())).append(',').append(cell(row.actor())).append(',')
                .append(cell(row.field())).append(',').append(cell(row.beforeValue())).append(',')
                .append(cell(row.afterValue())).append(',').append(cell(row.ipAddress())).append(',')
                .append(cell(row.occurredAt())).append(',').append(cell(row.detail())).append("\r\n");
        }
        auditService.recordAction("EXPORTACION_AUDITORIA", authentication.getName(), shipmentId,
            request.getRemoteAddr(), "Exportación de " + rows.size() + " registros CSV");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria-envios.csv")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
