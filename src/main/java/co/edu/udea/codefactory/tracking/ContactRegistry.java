package co.edu.udea.codefactory.tracking;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;

@Service
public class ContactRegistry implements ApplicationRunner {
    private final JdbcTemplate db;
    public ContactRegistry(JdbcTemplate db) { this.db=db; }
    @Override @Transactional public void run(ApplicationArguments args) {
        for(Map<String,Object> s:db.queryForList("select * from envio where remitente_contacto_id is null or destinatario_contacto_id is null")) {
            Long sender=resolve((String)s.get("remitente_nombre"),(String)s.get("remitente_telefono"),(String)s.get("remitente_correo"));
            Long recipient=resolve((String)s.get("destinatario_nombre"),(String)s.get("destinatario_telefono"),(String)s.get("destinatario_correo"));
            db.update("update envio set remitente_contacto_id=?,destinatario_contacto_id=? where id=?",sender,recipient,s.get("id"));
        }
    }
    public Long resolve(String name,String phone,String email) {
        String normalized=email.strip().toLowerCase(Locale.ROOT);
        List<Long> ids=db.queryForList("select id from contacto where correo=?",Long.class,normalized);
        if(!ids.isEmpty()) return ids.getFirst();
        db.update("insert into contacto(nombre,telefono,correo) values (?,?,?)",name,phone,normalized);
        return db.queryForObject("select id from contacto where correo=?",Long.class,normalized);
    }
    public List<Map<String,Object>> list(String q) {
        if(q.length()>120) throw new BusinessException("Búsqueda demasiado larga");
        String term="%"+q.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return db.queryForList("select * from contacto where lower(nombre) like ? escape '!' or lower(coalesce(documento,'')) like ? escape '!' order by nombre limit 100",term,term);
    }
    public Map<String,Object> get(Long id) { return db.queryForList("select * from contacto where id=?",id).stream().findFirst().orElseThrow(()->new BusinessException("Contacto no encontrado")); }
    @Transactional public Map<String,Object> save(Long id,String name,String document,String email,String phone,Long version) {
        email=email.strip().toLowerCase(Locale.ROOT);
        if(id==0) {
            db.update("insert into contacto(nombre,documento,correo,telefono) values (?,?,?,?)",name.strip(),document.strip(),email,phone.strip());
            id=db.queryForObject("select id from contacto where correo=?",Long.class,email);
        } else if(db.update("update contacto set nombre=?,documento=?,correo=?,telefono=?,version=version+1 where id=? and version=?",name.strip(),document.strip(),email,phone.strip(),id,version)!=1)
            throw new ShipmentConflictException("El contacto cambió; recarga");
        return get(id);
    }
    public List<Map<String,Object>> history(Long id) {
        get(id);
        return db.queryForList("select id,codigo_seguimiento,estado,creado_en,case when remitente_contacto_id=? then 'REMITENTE' else 'DESTINATARIO' end as participacion from envio where remitente_contacto_id=? or destinatario_contacto_id=? order by creado_en desc",id,id,id);
    }
}
