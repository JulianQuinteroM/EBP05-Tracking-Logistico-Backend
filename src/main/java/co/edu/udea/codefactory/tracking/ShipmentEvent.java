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
@Table(name = "evento_envio")
public class ShipmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "envio_id") Shipment shipment;
    @Column(nullable = false) String tipo;
    @Column(name = "estado_resultante", nullable = false) String resultingStatus;
    @Column(name = "publico", nullable = false) boolean publicVisible;
    @Column(name = "ocurrido_en", nullable = false) OffsetDateTime occurredAt;
    @Column(name = "descripcion", nullable = false) String description;

    protected ShipmentEvent() {}

    ShipmentEvent(Shipment shipment, String type, String description, OffsetDateTime time) {
        this.shipment = shipment;
        this.tipo = type;
        this.resultingStatus = shipment.status.name();
        this.publicVisible = true;
        this.occurredAt = time;
        this.description = description;
    }
}
