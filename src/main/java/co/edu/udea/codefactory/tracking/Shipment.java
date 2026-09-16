package co.edu.udea.codefactory.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "envio")
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "tracking_serial", nullable = false, unique = true) Long trackingSerial;
    @Column(name = "codigo_seguimiento", nullable = false, unique = true) String trackingCode;
    @Column(name = "remitente_nombre", nullable = false) String senderName;
    @Column(name = "remitente_telefono", nullable = false) String senderPhone;
    @Column(name = "remitente_correo", nullable = false) String senderEmail;
    @Column(name = "destinatario_nombre", nullable = false) String recipientName;
    @Column(name = "destinatario_telefono", nullable = false) String recipientPhone;
    @Column(name = "destinatario_correo", nullable = false) String recipientEmail;
    @Column(name = "destino_calle", nullable = false) String destinationStreet;
    @Column(name = "destino_numero", nullable = false) String destinationNumber;
    @Column(name = "destino_ciudad", nullable = false) String destinationCity;
    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 3) BigDecimal weightKg;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_servicio", nullable = false) ServiceType serviceType;
    @Column(name = "fecha_entrega_programada") LocalDate scheduledDeliveryDate;
    @Column(name = "fecha_entrega_estimada", nullable = false) LocalDate estimatedDeliveryDate;
    @Column(name = "costo_estimado_cop", nullable = false, precision = 12, scale = 2) BigDecimal estimatedCostCop;
    @Enumerated(EnumType.STRING) @Column(name = "estado", nullable = false) ShipmentStatus status;
    @Column(name = "creado_en", nullable = false) OffsetDateTime createdAt;
    @Column(name = "actualizado_en", nullable = false) OffsetDateTime updatedAt;
    @Version @Column(name = "version", nullable = false) Long version;

    protected Shipment() {}
}
