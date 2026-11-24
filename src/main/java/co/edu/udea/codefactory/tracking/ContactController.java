package co.edu.udea.codefactory.tracking;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/contactos")
public class ContactController {
    private final ContactRegistry contacts; private final AuditService audit;
    public ContactController(ContactRegistry contacts,AuditService audit) { this.contacts=contacts; this.audit=audit; }
    public record Input(@NotBlank @Size(max=120) String name,@NotBlank @Pattern(regexp="[A-Za-z0-9-]{3,30}") String document,
        @NotBlank @Email @Size(max=254) String email,@NotBlank @Pattern(regexp="^[+0-9 ()-]{7,30}$") String phone,Long version) {}
    @GetMapping public List<Map<String,Object>> list(@RequestParam(defaultValue="") String q) { return contacts.list(q); }
    @GetMapping("/{id}/envios") public List<Map<String,Object>> history(@PathVariable Long id) { return contacts.history(id); }
    @PutMapping("/{id}") public Map<String,Object> save(@PathVariable Long id,@Valid @RequestBody Input in,Authentication a,HttpServletRequest req) {
        var before=id==0?null:contacts.get(id);
        var result=contacts.save(id,in.name(),in.document(),in.email(),in.phone(),in.version());
        audit.recordAction("CONTACTO",a.getName(),null,req.getRemoteAddr(),"Contacto #"+result.get("id")+"; anterior="+before+"; nuevo="+result); return result;
    }
}
