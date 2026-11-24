package co.edu.udea.codefactory.tracking;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reclamaciones/{id}")
public class ComplaintAiController {
    private final ComplaintAi ai;private final AuditService audit;
    public ComplaintAiController(ComplaintAi ai,AuditService audit) { this.ai=ai;this.audit=audit; }
    public record Input(@NotBlank @Size(min=10,max=3500) String caseText) {}
    @GetMapping("/contexto-ia") public Object context(@PathVariable String id) { return ai.context(id); }
    @PostMapping("/analisis") public Object analyze(@PathVariable String id,@Valid @RequestBody Input in,Authentication a,HttpServletRequest r) {
        var result=ai.analyze(id,in.caseText(),a.getName());audit.recordAction("ANALISIS_IA",a.getName(),null,r.getRemoteAddr(),"Análisis asistido de "+id+"; requiere revisión humana");return result;
    }
}
