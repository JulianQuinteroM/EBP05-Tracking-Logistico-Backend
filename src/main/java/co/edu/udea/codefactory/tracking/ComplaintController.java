package co.edu.udea.codefactory.tracking;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reclamaciones")
public class ComplaintController {
    private final ComplaintService complaints;private final AuditService audit;private final PublicTrackingRateLimiter limits;
    public ComplaintController(ComplaintService complaints,AuditService audit,Clock clock) { this.complaints=complaints;this.audit=audit;this.limits=new PublicTrackingRateLimiter(clock); }
    public record Create(@NotBlank @Size(max=30) String trackingCode,@NotBlank @Email @Size(max=254) String email,@NotBlank String category,@NotBlank @Size(min=5,max=150) String subject,@NotBlank @Size(min=20,max=3000) String description) {}
    public record Query(@NotBlank @Size(max=40) String id,@NotBlank @Size(max=100) String accessKey,Long version,@Size(max=3000) String message) {}
    public record Update(@NotNull Long version,@NotBlank String status,@Size(max=100) String assignee,@Size(max=3000) String message) {}
    private void attempt(HttpServletRequest req) { limits.check(req.getRemoteAddr());limits.notFound(req.getRemoteAddr()); }
    @PostMapping("/publicas") public Object create(@Valid @RequestBody Create in,HttpServletRequest req) {
        attempt(req);return complaints.create(in.trackingCode(),in.email(),in.category(),in.subject(),in.description());
    }
    @PostMapping("/consulta") public Object query(@Valid @RequestBody Query in,HttpServletRequest req) {
        attempt(req);complaints.checkKey(in.id(),in.accessKey());return complaints.detail(in.id(),true);
    }
    @PostMapping("/mensaje") public Object message(@Valid @RequestBody Query in,HttpServletRequest req) {
        attempt(req);complaints.checkKey(in.id(),in.accessKey());return complaints.update(in.id(),in.version(),null,null,in.message(),"CLIENTE",true);
    }
    @GetMapping public Object list(@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String q) { if(q.length()>120) throw new BusinessException("Búsqueda demasiado larga");return complaints.list(status,q); }
    @GetMapping("/responsables") public Object assignees() { return complaints.assignees(); }
    @GetMapping("/{id}") public Object detail(@PathVariable String id) { return complaints.detail(id,false); }
    @PutMapping("/{id}") public Object update(@PathVariable String id,@Valid @RequestBody Update in,Authentication a,HttpServletRequest req) {
        var result=complaints.update(id,in.version(),in.status(),in.assignee(),in.message(),a.getName(),false);
        audit.recordAction("RECLAMACION",a.getName(),null,req.getRemoteAddr(),id+": "+in.status()+"; responsable="+in.assignee());return result;
    }
}
