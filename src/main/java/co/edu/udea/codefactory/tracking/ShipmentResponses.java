package co.edu.udea.codefactory.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public final class ShipmentResponses {
    private ShipmentResponses() {}

    public record EventView(String type, ShipmentStatus previousStatus, ShipmentStatus resultingStatus,
                            String city, String reference, String actor,
                            OffsetDateTime occurredAt, String description) {
        static EventView of(ShipmentEvent event) {
            return new EventView(event.tipo,
                event.previousStatus == null ? null : ShipmentStatus.valueOf(event.previousStatus),
                ShipmentStatus.valueOf(event.resultingStatus), event.city, event.reference,
                event.actor, event.occurredAt, event.description);
        }
    }

    public record PublicEventView(String type, ShipmentStatus resultingStatus,
                                  OffsetDateTime occurredAt, String description, String city) {
        static PublicEventView of(ShipmentEvent event) {
            return new PublicEventView(event.tipo, ShipmentStatus.valueOf(event.resultingStatus),
                event.occurredAt, event.description, event.city);
        }
    }

    public record JourneyStep(ShipmentStatus status, String phase) {}

    public record PublicTracking(String trackingCode, ShipmentStatus status, ServiceType serviceType,
                                 OffsetDateTime updatedAt, LocalDate estimatedDeliveryStart,
                                 LocalDate estimatedDeliveryDate, String currentCity,
                                 String maskedSenderName, String maskedRecipientName,
                                 List<JourneyStep> journey, PublicEventView latestPublicEvent) {}

    public record ListItem(Long id, String trackingCode, String senderName, String recipientName,
                           ShipmentStatus status, ServiceType serviceType, OffsetDateTime createdAt, Long version) {
        static ListItem of(Shipment shipment) {
            return new ListItem(shipment.id, shipment.trackingCode, shipment.senderName, shipment.recipientName,
                shipment.status, shipment.serviceType, shipment.createdAt, shipment.version);
        }
    }

    public record ListPage(List<ListItem> content, int page, int size, long totalElements, int totalPages) {
        static ListPage of(Page<ListItem> data) {
            return new ListPage(data.getContent(), data.getNumber(), data.getSize(),
                data.getTotalElements(), data.getTotalPages());
        }
    }

    public record ShipmentDetail(Long id, String trackingCode, ShipmentStatus status, Long version,
                                 ShipmentInput shipment, BigDecimal estimatedCostCop,
                                 LocalDate estimatedDeliveryDate, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt, String currentCity,
                                 List<ShipmentStatus> allowedStatusTransitions,
                                 List<EventView> events) {}

    public record AuditView(Long id, Long shipmentId, String action, String actor, String field,
                            String beforeValue, String afterValue, String ipAddress,
                            OffsetDateTime occurredAt, String detail) {
        static AuditView of(ShipmentAudit audit) {
            return new AuditView(audit.id, audit.shipmentReference, audit.action,
                audit.actor, audit.field, audit.beforeValue, audit.afterValue,
                audit.ipAddress, audit.occurredAt, audit.detail);
        }
    }
}
