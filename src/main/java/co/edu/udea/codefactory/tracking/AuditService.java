package co.edu.udea.codefactory.tracking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private final ShipmentAuditRepository audits;
    private final ShipmentEventRepository events;
    private final ShipmentRepository shipments;
    private final Clock clock;

    public AuditService(ShipmentAuditRepository audits, ShipmentEventRepository events,
                        ShipmentRepository shipments, Clock clock) {
        this.audits = audits;
        this.events = events;
        this.shipments = shipments;
        this.clock = clock;
    }

    @Transactional
    public void verifySequences(Long shipmentId) {
        List<Long> ids = shipmentId == null ? shipments.findAll().stream().map(s -> s.id).toList()
            : List.of(shipmentId);
        for (Long id : ids) verifyOne(id);
    }

    private void verifyOne(Long shipmentId) {
        List<ShipmentAudit> existing = new ArrayList<>(audits.findByShipmentReferenceOrderByOccurredAtDescIdDesc(shipmentId));
        ShipmentStatus current = null;
        ShipmentStatus beforeIncident = null;
        for (ShipmentEvent event : events.findByShipmentIdOrderByOccurredAtAscIdAsc(shipmentId)) {
            ShipmentStatus next;
            try { next = ShipmentStatus.valueOf(event.resultingStatus); }
            catch (RuntimeException exception) { recordSequenceAlert(shipmentId, event, existing, current, event.resultingStatus); continue; }
            boolean valid;
            switch (event.tipo) {
                case "REGISTRO" -> valid = current == null && next == ShipmentStatus.PENDIENTE_RECOGIDA;
                case "CANCELACION" -> valid = current == ShipmentStatus.PENDIENTE_RECOGIDA
                    && next == ShipmentStatus.CANCELADO;
                case "ESTADO" -> {
                    Shipment snapshot = new Shipment();
                    snapshot.status = current;
                    snapshot.statusBeforeIncident = beforeIncident;
                    valid = current != null && Objects.equals(event.previousStatus, current.name())
                        && ShipmentStatusPolicy.allowed(snapshot).contains(next);
                    if (next == ShipmentStatus.INCIDENCIA) beforeIncident = current;
                    else if (current == ShipmentStatus.INCIDENCIA) beforeIncident = null;
                }
                case "UBICACION" -> valid = current != null && next == current
                    && ShipmentStatusPolicy.canRegisterLocation(current);
                default -> valid = false;
            }
            if (!valid) recordSequenceAlert(shipmentId, event, existing, current, next.name());
            if (!"UBICACION".equals(event.tipo)) current = next;
        }
    }

    private void recordSequenceAlert(Long shipmentId, ShipmentEvent event,
                                     List<ShipmentAudit> existing, ShipmentStatus before, String after) {
        String field = "evento:" + event.id;
        if (existing.stream().anyMatch(a -> "TRANSICION_INUSUAL".equals(a.action) && field.equals(a.field))) return;
        ShipmentAudit alert = new ShipmentAudit(null, "TRANSICION_INUSUAL", "sistema", field,
            before == null ? null : before.name(), after, null,
            "Transición inusual detectada en evento #" + event.id, OffsetDateTime.now(clock));
        alert.shipmentReference = shipmentId;
        audits.save(alert);
        existing.add(alert);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAction(String action, String actor, Long shipmentId,
                             String ipAddress, String detail) {
        ShipmentAudit audit = new ShipmentAudit(null, action, actor, null, null,
            null, ipAddress, detail, OffsetDateTime.now(clock));
        audit.shipmentReference = shipmentId;
        audits.save(audit);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponses.AuditView> history(Long shipmentId, String action,
                                                      String actor, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new BusinessException("La fecha inicial debe ser anterior a la final");
        List<ShipmentAudit> source = shipmentId == null
            ? audits.findByOrderByOccurredAtDescIdDesc()
            : audits.findByShipmentReferenceOrderByOccurredAtDescIdDesc(shipmentId);
        return source.stream()
            .filter(audit -> action == null || action.isBlank() || audit.action.equalsIgnoreCase(action.strip()))
            .filter(audit -> actor == null || actor.isBlank() || audit.actor.toLowerCase().contains(actor.strip().toLowerCase()))
            .filter(audit -> from == null || !audit.occurredAt.atZoneSameInstant(ZONE).toLocalDate().isBefore(from))
            .filter(audit -> to == null || !audit.occurredAt.atZoneSameInstant(ZONE).toLocalDate().isAfter(to))
            .map(ShipmentResponses.AuditView::of).toList();
    }
}
