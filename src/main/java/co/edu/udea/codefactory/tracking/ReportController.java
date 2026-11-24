package co.edu.udea.codefactory.tracking;
import java.time.LocalDate;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
@RestController @RequestMapping("/api")
public class ReportController {
    private final ReportService reports;private final AuditService audit;
    public ReportController(ReportService reports,AuditService audit) { this.reports=reports;this.audit=audit; }
    @GetMapping("/reportes") public Map<String,Object> report(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="DIA") String group) { return reports.report(from,to,group); }
    static ResponseEntity<byte[]> file(String csv,String name) { return ResponseEntity.ok().header("Content-Type","text/csv; charset=utf-8").header("Content-Disposition","attachment; filename="+name+".csv").header("Cache-Control","no-store").body(csv.getBytes(StandardCharsets.UTF_8)); }
    @GetMapping("/envios/exportacion") public ResponseEntity<byte[]> export(@RequestParam(defaultValue="") String q,Authentication a,HttpServletRequest r) {
        String csv=reports.shipmentsCsv(q);audit.recordAction("EXPORTACION_ENVIOS",a.getName(),null,r.getRemoteAddr(),"Exportación del listado filtrado");return file(csv,"envios");
    }
    @SuppressWarnings("unchecked") @GetMapping("/reportes/exportacion") public ResponseEntity<byte[]> exportReport(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="DIA") String group,@RequestParam(defaultValue="retrasados") String type,Authentication a,HttpServletRequest r) {
        var report=reports.report(from,to,group);String csv;
        switch(type) {
            case "volumen" -> csv=ReportService.export((List<Map<String,Object>>)report.get("volume"),List.of("period","total"));
            case "retrasados" -> csv=ReportService.export((List<Map<String,Object>>)report.get("delayed"),List.of("codigo_seguimiento","estado","fecha_entrega_estimada","dias_retraso"));
            case "resumen" -> csv=ReportService.export(List.of(report),List.of("from","to","total","delivered","onTimePercent","averageTransitHours","transitSamples"));
            default -> throw new BusinessException("Reporte no permitido");
        }
        audit.recordAction("EXPORTACION_REPORTE",a.getName(),null,r.getRemoteAddr(),"Reporte "+type);return file(csv,"reporte-"+type);
    }
}
