package co.edu.udea.codefactory.tracking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private final ShipmentRepository shipments;
    private final ShipmentEventRepository events;
    private final ShipmentAuditRepository audits;
    private final TrackingSequence sequence;
    private final ServiceEstimate estimates;
    private final Clock clock;
    private final ObjectMapper json;
    private final EntityManager entityManager;

    public ShipmentService(ShipmentRepository shipments, ShipmentEventRepository events,
                           ShipmentAuditRepository audits, TrackingSequence sequence,
                           ServiceEstimate estimates, Clock clock, ObjectMapper json,
                           EntityManager entityManager) {
        this.shipments = shipments;
        this.events = events;
        this.audits = audits;
        this.sequence = sequence;
        this.estimates = estimates;
        this.clock = clock;
        this.json = json;
        this.entityManager = entityManager;
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail create(ShipmentInput input) {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        ServiceEstimate.Estimate estimate = estimates.calculate(input.serviceType(), input.scheduledDeliveryDate(), today);
        OffsetDateTime now = OffsetDateTime.now(clock);
        Shipment shipment = new Shipment();
        shipment.trackingSerial = sequence.next();
        shipment.trackingCode = TrackingCode.create(shipment.trackingSerial);
        apply(shipment, input, estimate);
        shipment.status = ShipmentStatus.PENDIENTE_RECOGIDA;
        shipment.createdAt = now;
        shipment.updatedAt = now;
        shipments.saveAndFlush(shipment);
        events.save(new ShipmentEvent(shipment, "REGISTRO", "Envío registrado; pendiente de recogida", now));
        entityManager.flush();
        return detail(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponses.PublicTracking track(String rawCode) {
        String code = TrackingCode.normalize(rawCode);
        Shipment shipment = shipments.findByTrackingCode(code).orElseThrow(ShipmentNotFoundException::new);
        ShipmentResponses.EventView latest = events
            .findFirstByShipmentIdAndPublicVisibleTrueOrderByOccurredAtDescIdDesc(shipment.id)
            .map(ShipmentResponses.EventView::of).orElse(null);
        return new ShipmentResponses.PublicTracking(shipment.trackingCode, shipment.status,
            shipment.serviceType, shipment.updatedAt, shipment.estimatedDeliveryDate, latest);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponses.ListItem> list(int page, int size, String query, String sortBy, String direction) {
        if (page < 0) throw new BusinessException("La página no puede ser negativa");
        if (size != 20 && size != 50) throw new BusinessException("El tamaño de página debe ser 20 o 50");
        String field = switch (sortBy) {
            case "createdAt", "trackingCode", "status", "serviceType" -> sortBy;
            default -> throw new BusinessException("Campo de ordenamiento no permitido");
        };
        Sort.Direction dir = switch (direction.toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BusinessException("Dirección de ordenamiento no permitida");
        };
        Sort sort = Sort.by(dir, field).and(Sort.by(dir, "id"));
        String q = query == null ? "" : query.strip();
        if (q.length() > 120) throw new BusinessException("La búsqueda es demasiado larga");
        String escaped = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return shipments.search(escaped, PageRequest.of(page, size, sort)).map(ShipmentResponses.ListItem::of);
    }

    @Transactional(readOnly = true)
    public ShipmentResponses.ShipmentDetail get(Long id) {
        return detail(shipments.findById(id).orElseThrow(ShipmentNotFoundException::new));
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail update(Long id, UpdateShipmentRequest request, String actor) {
        Shipment shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        requirePendingAndVersion(shipment, request.version());
        boolean estimateInputsChanged = shipment.serviceType != request.shipment().serviceType()
            || !Objects.equals(shipment.scheduledDeliveryDate, request.shipment().scheduledDeliveryDate());
        ServiceEstimate.Estimate estimate = estimateInputsChanged
            ? estimates.calculate(request.shipment().serviceType(), request.shipment().scheduledDeliveryDate(),
                LocalDate.now(clock.withZone(BUSINESS_ZONE)))
            : new ServiceEstimate.Estimate(shipment.estimatedCostCop, shipment.estimatedDeliveryDate, "Sin cambio");
        Map<String, Object> before = snapshot(shipment);
        apply(shipment, request.shipment(), estimate);
        Map<String, Object> after = snapshot(shipment);
        Map<String, Object> changes = new LinkedHashMap<>();
        before.forEach((key, value) -> {
            if (!Objects.equals(value, after.get(key)))
                changes.put(key, Map.of("before", value == null ? "" : value,
                    "after", after.get(key) == null ? "" : after.get(key)));
        });
        if (!changes.isEmpty()) {
            OffsetDateTime now = OffsetDateTime.now(clock);
            shipment.updatedAt = now;
            shipments.saveAndFlush(shipment);
            audits.save(new ShipmentAudit(shipment, "EDICION", actor, toJson(changes), now));
            entityManager.flush();
        }
        return detail(shipment);
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail cancel(Long id, CancelShipmentRequest request, String actor) {
        Shipment shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        requirePendingAndVersion(shipment, request.version());
        OffsetDateTime now = OffsetDateTime.now(clock);
        shipment.status = ShipmentStatus.CANCELADO;
        shipment.updatedAt = now;
        shipments.saveAndFlush(shipment);
        events.save(new ShipmentEvent(shipment, "CANCELACION", "Envío cancelado", now));
        audits.save(new ShipmentAudit(shipment, "CANCELACION", actor, request.reason().strip(), now));
        entityManager.flush();
        return detail(shipment);
    }

    private void requirePendingAndVersion(Shipment shipment, Long expectedVersion) {
        if (shipment.status != ShipmentStatus.PENDIENTE_RECOGIDA)
            throw new ShipmentConflictException("El envío ya no está pendiente de recogida");
        if (!Objects.equals(shipment.version, expectedVersion))
            throw new ShipmentConflictException("El envío cambió; recargue la ficha antes de continuar");
    }

    private void apply(Shipment s, ShipmentInput in, ServiceEstimate.Estimate estimate) {
        s.senderName = in.senderName().strip();
        s.senderPhone = in.senderPhone().strip();
        s.senderEmail = in.senderEmail().strip();
        s.recipientName = in.recipientName().strip();
        s.recipientPhone = in.recipientPhone().strip();
        s.recipientEmail = in.recipientEmail().strip();
        s.destinationStreet = in.destinationStreet().strip();
        s.destinationNumber = in.destinationNumber().strip();
        s.destinationCity = in.destinationCity().strip();
        s.weightKg = in.weightKg();
        s.serviceType = in.serviceType();
        s.scheduledDeliveryDate = in.scheduledDeliveryDate();
        s.estimatedCostCop = estimate.costCop();
        s.estimatedDeliveryDate = estimate.deliveryDate();
    }

    private Map<String, Object> snapshot(Shipment s) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("senderName", s.senderName);
        values.put("senderPhone", s.senderPhone);
        values.put("senderEmail", s.senderEmail);
        values.put("recipientName", s.recipientName);
        values.put("recipientPhone", s.recipientPhone);
        values.put("recipientEmail", s.recipientEmail);
        values.put("destinationStreet", s.destinationStreet);
        values.put("destinationNumber", s.destinationNumber);
        values.put("destinationCity", s.destinationCity);
        values.put("weightKg", s.weightKg);
        values.put("serviceType", s.serviceType);
        values.put("scheduledDeliveryDate", s.scheduledDeliveryDate);
        values.put("estimatedCostCop", s.estimatedCostCop);
        values.put("estimatedDeliveryDate", s.estimatedDeliveryDate);
        return values;
    }

    private String toJson(Map<String, Object> data) {
        try { return json.writeValueAsString(data); }
        catch (JsonProcessingException e) { throw new IllegalStateException("No se pudo auditar la edición", e); }
    }

    private ShipmentResponses.ShipmentDetail detail(Shipment s) {
        List<ShipmentResponses.EventView> history = events.findByShipmentIdOrderByOccurredAtAscIdAsc(s.id)
            .stream().map(ShipmentResponses.EventView::of).toList();
        ShipmentInput input = new ShipmentInput(s.senderName, s.senderPhone, s.senderEmail,
            s.recipientName, s.recipientPhone, s.recipientEmail, s.destinationStreet,
            s.destinationNumber, s.destinationCity, s.weightKg, s.serviceType, s.scheduledDeliveryDate);
        return new ShipmentResponses.ShipmentDetail(s.id, s.trackingCode, s.status, s.version,
            input, s.estimatedCostCop, s.estimatedDeliveryDate, s.createdAt, s.updatedAt, history);
    }
}
