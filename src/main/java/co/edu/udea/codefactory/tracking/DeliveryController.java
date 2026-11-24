package co.edu.udea.codefactory.tracking;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/envios/{id}")
public class DeliveryController {
    private final DeliveryService service;
    public DeliveryController(DeliveryService service) { this.service=service; }
    public record Delivery(@NotNull Long version,@NotBlank @Size(max=120) String receiver,@NotBlank String type,@NotBlank String mime,@NotBlank @Size(max=2_000_000) String image) {}
    public record Reason(@NotNull Long version,@NotBlank @Size(min=10,max=500) String reason) {}
    public record Contacts(@NotNull Long version,@NotBlank @Pattern(regexp="^[+0-9 ()-]{7,30}$") String senderPhone,@NotBlank @Email @Size(max=254) String senderEmail,
        @NotBlank @Pattern(regexp="^[+0-9 ()-]{7,30}$") String recipientPhone,@NotBlank @Email @Size(max=254) String recipientEmail) {}
    @GetMapping("/operacion") public Map<String,Object> operation(@PathVariable Long id) { return service.operation(id); }
    @GetMapping("/evidencia") public ResponseEntity<byte[]> evidence(@PathVariable Long id) {
        var e=service.evidence(id);return ResponseEntity.ok().header("Content-Type",(String)e.get("mime")).header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body((byte[])e.get("contenido"));
    }
    @PostMapping("/entrega") public Object deliver(@PathVariable Long id,@Valid @RequestBody Delivery in,Authentication a,HttpServletRequest r) { return service.deliver(id,in.version(),in.receiver(),in.type(),in.mime(),in.image(),a.getName(),r.getRemoteAddr()); }
    @PostMapping("/intentos") public Object attempt(@PathVariable Long id,@Valid @RequestBody Reason in,Authentication a,HttpServletRequest r) { return service.failed(id,in.version(),in.reason(),a.getName(),r.getRemoteAddr()); }
    @PostMapping("/incidencia") public Object resolve(@PathVariable Long id,@Valid @RequestBody Reason in,Authentication a,HttpServletRequest r) { return service.resolve(id,in.version(),in.reason(),a.getName(),r.getRemoteAddr()); }
    @PostMapping("/contactos") public Object contacts(@PathVariable Long id,@Valid @RequestBody Contacts in,Authentication a,HttpServletRequest r) { return service.contacts(id,in.version(),in.senderPhone(),in.senderEmail(),in.recipientPhone(),in.recipientEmail(),a.getName(),r.getRemoteAddr()); }
}
