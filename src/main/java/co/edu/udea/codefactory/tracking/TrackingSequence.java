package co.edu.udea.codefactory.tracking;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrackingSequence {
    private final JdbcTemplate jdbc;

    public TrackingSequence(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long next() {
        Long serial = jdbc.queryForObject("select nextval('tracking_serial_seq')", Long.class);
        if (serial == null) throw new IllegalStateException("No se pudo generar la serie de seguimiento");
        return serial;
    }
}
