package co.edu.udea.codefactory.tracking;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditoria_envio")
public class ShipmentAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "envio_id") Shipment shipment;
    @Column(name = "accion", nullable = false) String action;
    @Column(name = "actor", nullable = false) String actor;
    @Column(name = "ocurrido_en", nullable = false) OffsetDateTime occurredAt;
    @Column(name = "detalle", nullable = false, columnDefinition = "text") String detail;

    protected ShipmentAudit() {}

    ShipmentAudit(Shipment shipment, String action, String actor, String detail, OffsetDateTime time) {
        this.shipment = shipment;
        this.action = action;
        this.actor = actor;
        this.detail = detail;
        this.occurredAt = time;
    }
}
