package co.edu.udea.codefactory.tracking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final Accounts accounts;
    private final AuditService audit;
    private final PublicTrackingRateLimiter limiter;
    public AuthController(Accounts accounts, AuditService audit, java.time.Clock clock) {
        this.accounts=accounts; this.audit=audit; this.limiter=new PublicTrackingRateLimiter(clock);
    }
    public record Login(@NotBlank @Size(max=100) String username,@NotBlank @Size(max=72) String password) {}
    public record Save(@NotBlank @Size(max=120) String name,@NotBlank String role,List<String> permissions,
                       boolean active,@Size(max=72) String password,Long version) {}
    @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody Login input,HttpServletRequest req) {
        String key=req.getRemoteAddr(); limiter.check(key);
        try { Map<String,Object> result=accounts.login(input.username(),input.password()); limiter.found(key); return result; }
        catch(org.springframework.security.authentication.BadCredentialsException e) { limiter.notFound(key); throw e; }
    }
    @GetMapping("/auth/me") public Accounts.View me(Authentication a) { return accounts.get(a.getName()); }
    @PostMapping("/auth/logout") public void logout(@RequestHeader(value="Authorization",defaultValue="") String auth) {
        if(auth.startsWith("Bearer ")) accounts.logout(auth.substring(7));
    }
    @GetMapping("/admin/usuarios") public Map<String,Object> users() { return Map.of("users",accounts.list(),"permissions",new TreeSet<>(Accounts.PERMISSIONS)); }
    @PutMapping("/admin/usuarios/{username}") public Accounts.View save(@PathVariable String username,@Valid @RequestBody Save in,Authentication a,HttpServletRequest req) {
        Accounts.View before=null;
        try { before=accounts.get(username); } catch(org.springframework.security.core.userdetails.UsernameNotFoundException ignored) { }
        Accounts.View result=accounts.save(username,in.name(),in.role(),in.permissions(),in.active(),in.password(),in.version(),a.getName());
        audit.recordAction("USUARIO_PERMISOS",a.getName(),null,req.getRemoteAddr(),"Cuenta "+username+"; anterior="+before+"; nuevo="+result+"; contraseña actualizada="+(in.password()!=null&&!in.password().isBlank()));
        return result;
    }
}
