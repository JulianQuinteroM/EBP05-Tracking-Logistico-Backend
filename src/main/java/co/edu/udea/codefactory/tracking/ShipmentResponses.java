package co.edu.udea.codefactory.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public final class ShipmentResponses {
    private ShipmentResponses() {}

    public record EventView(String type, ShipmentStatus resultingStatus, OffsetDateTime occurredAt, String description) {
        static EventView of(ShipmentEvent event) {
            return new EventView(event.tipo, ShipmentStatus.valueOf(event.resultingStatus), event.occurredAt, event.description);
        }
    }

    public record PublicTracking(String trackingCode, ShipmentStatus status, ServiceType serviceType,
                                 OffsetDateTime updatedAt, LocalDate estimatedDeliveryDate, EventView latestPublicEvent) {}

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
                                 OffsetDateTime updatedAt, List<EventView> events) {}
}
