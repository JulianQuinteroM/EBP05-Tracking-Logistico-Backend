package co.edu.udea.codefactory.tracking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Accounts implements UserDetailsService, ApplicationRunner {
    public static final Set<String> PERMISSIONS = Set.of("ENVIO_LEER","ENVIO_CREAR","ENVIO_EDITAR","ENVIO_ESTADO",
        "CONTACTO_LEER","CONTACTO_EDITAR","ENTREGA","REPORTES","RECLAMOS","IA","AUDITORIA","USUARIOS");
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final Environment env;
    private final Clock clock;
    public Accounts(JdbcTemplate db, PasswordEncoder encoder, Environment env, Clock clock) {
        this.db=db; this.encoder=encoder; this.env=env; this.clock=clock;
    }
    public record View(String username, String name, String role, List<String> permissions, boolean active, long version) {}
    public static Set<String> defaults(String role) {
        return switch(role) {
            case "ADMIN" -> PERMISSIONS;
            case "OPERADOR" -> Set.of("ENVIO_LEER","ENVIO_CREAR","ENVIO_EDITAR","ENVIO_ESTADO","CONTACTO_LEER","CONTACTO_EDITAR","ENTREGA","REPORTES","RECLAMOS","IA");
            case "REPARTIDOR" -> Set.of("ENVIO_LEER","ENVIO_ESTADO","ENTREGA");
            case "VIEWER" -> Set.of("ENVIO_LEER","CONTACTO_LEER","REPORTES");
            default -> throw new BusinessException("Rol no permitido");
        };
    }
    @Override public void run(ApplicationArguments args) {
        seed("operator", "operador", "OPERADOR"); seed("admin", "administrador", "ADMIN"); seed("viewer", "consulta", "VIEWER");
    }
    private void seed(String key, String fallback, String role) {
        String username=env.getProperty("app.security."+key+"-username", fallback);
        String password=env.getProperty("app.security."+key+"-password", "");
        if(username.isBlank() || password.isBlank()) return;
        if(password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72) throw new IllegalStateException("La contraseña inicial requiere 12 caracteres y como máximo 72 bytes UTF-8");
        if(db.queryForObject("select count(*) from cuenta_usuario where username=?",Integer.class,username)==0)
            db.update("insert into cuenta_usuario(username,nombre,password_hash,rol,permisos) values (?,?,?,?,?)",
                username,username,encoder.encode(password),role,String.join(",",defaults(role)));
    }
    public View get(String username) {
        return db.query("select * from cuenta_usuario where username=?",(r,n)->new View(r.getString("username"),r.getString("nombre"),r.getString("rol"),
            r.getString("permisos").isBlank()?List.of():List.of(r.getString("permisos").split(",")),r.getBoolean("activo"),r.getLong("version")),username)
            .stream().findFirst().orElseThrow(()->new UsernameNotFoundException("Cuenta no disponible"));
    }
    @Override public UserDetails loadUserByUsername(String username) {
        View v=get(username);
        String hash=db.queryForObject("select password_hash from cuenta_usuario where username=?",String.class,username);
        List<String> auth=new ArrayList<>(v.permissions()); auth.add("ROLE_"+v.role());
        return User.withUsername(username).password(hash).authorities(auth.toArray(String[]::new)).disabled(!v.active()).build();
    }
    public List<View> list() { return db.queryForList("select username from cuenta_usuario order by username",String.class).stream().map(this::get).toList(); }
    public static String secret() { byte[] b=new byte[32]; new SecureRandom().nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
    @Transactional public Map<String,Object> login(String username, String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw new org.springframework.security.authentication.BadCredentialsException("Credenciales incorrectas");
        View v;
        try { v=get(username); } catch(UsernameNotFoundException e) { encoder.matches(password,"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"); throw new org.springframework.security.authentication.BadCredentialsException("Credenciales incorrectas"); }
        String stored=db.queryForObject("select password_hash from cuenta_usuario where username=?",String.class,username);
        if(!v.active() || !encoder.matches(password,stored)) throw new org.springframework.security.authentication.BadCredentialsException("Credenciales incorrectas");
        String token=secret();
        db.update("delete from sesion_usuario where expira_en < ?",OffsetDateTime.now(clock));
        db.update("insert into sesion_usuario values (?,?,?)",hash(token),username,OffsetDateTime.now(clock).plusMinutes(30));
        return Map.of("token",token,"user",v);
    }
    public UserDetails tokenUser(String token) {
        List<String> names=db.queryForList("select username from sesion_usuario where token_hash=? and expira_en>?",String.class,hash(token),OffsetDateTime.now(clock));
        if(names.isEmpty()) return null;
        UserDetails user=loadUserByUsername(names.getFirst());
        if(!user.isEnabled()) return null;
        db.update("update sesion_usuario set expira_en=? where token_hash=?",OffsetDateTime.now(clock).plusMinutes(30),hash(token));
        return user;
    }
    @Transactional public View save(String username, String name, String role, List<String> permissions, boolean active, String password, Long version, String actor) {
        // Serialize access changes so two concurrent requests cannot remove the last active administrator.
        db.queryForList("select username from cuenta_usuario where rol='ADMIN' order by username for update");
        if(password!=null&&!password.isBlank()&&(password.length()<12||password.getBytes(StandardCharsets.UTF_8).length>72)) throw new BusinessException("Contraseña: mínimo 12 caracteres y máximo 72 bytes UTF-8");
        if(!username.matches("[a-zA-Z0-9._-]{3,100}")) throw new BusinessException("Usuario: 3 a 100 letras, números, punto, guion o guion bajo");
        defaults(role);
        Set<String> selected=permissions==null?defaults(role):new TreeSet<>(permissions);
        if(!PERMISSIONS.containsAll(selected)) throw new BusinessException("Permiso no permitido");
        if(selected.contains("IA")&&!selected.contains("RECLAMOS")) throw new BusinessException("El permiso de IA requiere acceso a reclamaciones");
        if(role.equals("ADMIN")) selected=PERMISSIONS;
        if(username.equals(actor) && (!active || !role.equals("ADMIN"))) throw new BusinessException("No puedes revocar tu propio acceso administrativo");
        boolean exists=db.queryForObject("select count(*) from cuenta_usuario where username=?",Integer.class,username)>0;
        if(exists) {
            View before=get(username);
            if(version==null || version!=before.version()) throw new ShipmentConflictException("La cuenta cambió; recarga");
            if(before.role().equals("ADMIN") && (!active || !role.equals("ADMIN")) && db.queryForObject("select count(*) from cuenta_usuario where rol='ADMIN' and activo=true",Integer.class)<=1)
                throw new BusinessException("Debe conservarse al menos un administrador activo");
            int changed=db.update("update cuenta_usuario set nombre=?,rol=?,permisos=?,activo=?,version=version+1 where username=? and version=?",
                name,role,String.join(",",selected),active,username,version);
            if(changed!=1) throw new ShipmentConflictException("La cuenta cambió; recarga");
            db.update("delete from sesion_usuario where username=?",username);
        } else {
            if(version!=null) throw new ShipmentConflictException("La cuenta no existe");
            if(password==null || password.length()<12) throw new BusinessException("Contraseña inicial: mínimo 12 caracteres");
            db.update("insert into cuenta_usuario(username,nombre,password_hash,rol,permisos,activo) values (?,?,?,?,?,?)",username,name,encoder.encode(password),role,String.join(",",selected),active);
        }
        if(password!=null && !password.isBlank()) {
            if(password.length()<12 || password.length()>72) throw new BusinessException("Contraseña: entre 12 y 72 caracteres");
            db.update("update cuenta_usuario set password_hash=? where username=?",encoder.encode(password),username);
        }
        return get(username);
    }
    public void logout(String token) { db.update("delete from sesion_usuario where token_hash=?",hash(token)); }
}
