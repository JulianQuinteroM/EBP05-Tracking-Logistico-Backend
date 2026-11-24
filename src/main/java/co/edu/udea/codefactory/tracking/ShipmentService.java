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
    private final ContactRegistry contacts;

    public ShipmentService(ShipmentRepository shipments, ShipmentEventRepository events,
                           ShipmentAuditRepository audits, TrackingSequence sequence,
                           ServiceEstimate estimates, Clock clock, ObjectMapper json,
                           EntityManager entityManager, ContactRegistry contacts) {
        this.shipments = shipments;
        this.events = events;
        this.audits = audits;
        this.sequence = sequence;
        this.estimates = estimates;
        this.clock = clock;
        this.json = json;
        this.entityManager = entityManager;
        this.contacts = contacts;
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail create(ShipmentInput input) {
        return create(input, "sistema", "local");
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail create(ShipmentInput input, String actor, String ipAddress) {
        validatePackaging(input);
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
        ShipmentEvent registration = new ShipmentEvent(shipment, "REGISTRO", "Envío registrado; pendiente de recogida", now);
        registration.actor = actor;
        events.save(registration);
        audits.save(new ShipmentAudit(shipment, "REGISTRO", actor, "status", null,
            ShipmentStatus.PENDIENTE_RECOGIDA.name(), ipAddress, "Envío registrado", now));
        entityManager.flush();
        return detail(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponses.PublicTracking track(String rawCode) {
        String code = TrackingCode.normalize(rawCode);
        Shipment shipment = shipments.findByTrackingCode(code).orElseThrow(ShipmentNotFoundException::new);
        ShipmentResponses.PublicEventView latest = events
            .findFirstByShipmentIdAndPublicVisibleTrueOrderByOccurredAtDescIdDesc(shipment.id)
            .map(ShipmentResponses.PublicEventView::of).orElse(null);
        LocalDate registeredDate = shipment.createdAt.atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
        LocalDate start = shipment.serviceType == ServiceType.PROGRAMADO
            ? shipment.scheduledDeliveryDate
            : registeredDate.plusDays(shipment.serviceType == ServiceType.EXPRES ? 1 : 3);
          int index = ShipmentStatusPolicy.JOURNEY.indexOf(shipment.status == ShipmentStatus.INCIDENCIA
              ? shipment.statusBeforeIncident : shipment.status);
        List<ShipmentResponses.JourneyStep> journey = index < 0 ? List.of()
            : java.util.stream.IntStream.range(0, ShipmentStatusPolicy.JOURNEY.size())
                .mapToObj(i -> new ShipmentResponses.JourneyStep(ShipmentStatusPolicy.JOURNEY.get(i),
                    i < index ? "COMPLETADO" : i == index ? "ACTUAL" : "PENDIENTE"))
                .toList();
        return new ShipmentResponses.PublicTracking(shipment.trackingCode, shipment.status,
            shipment.serviceType, shipment.updatedAt, start, shipment.estimatedDeliveryDate,
            currentCity(shipment.id), maskName(shipment.senderName), maskName(shipment.recipientName),
            journey, latest);
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
    public ShipmentResponses.ShipmentDetail update(Long id, UpdateShipmentRequest request, String actor, String ipAddress) {
        Shipment shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        requireVersion(shipment, request.version());
        if (shipment.status != ShipmentStatus.PENDIENTE_RECOGIDA && shipment.status != ShipmentStatus.RECOGIDO)
            throw new ShipmentConflictException("Los datos físicos ya no pueden modificarse");
        validatePackaging(request.shipment());
        if (shipment.status == ShipmentStatus.RECOGIDO && !sameNonPhysicalFields(shipment, request.shipment()))
            throw new ShipmentConflictException("Después de la recogida solo se pueden editar peso, dimensiones y empaque");
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
            String detail = toJson(changes);
            changes.forEach((field, value) -> {
                @SuppressWarnings("unchecked") Map<String, Object> pair = (Map<String, Object>) value;
                audits.save(new ShipmentAudit(shipment, "EDICION", actor, field,
                    String.valueOf(pair.get("before")), String.valueOf(pair.get("after")),
                    ipAddress, detail, now));
            });
            entityManager.flush();
        }
        return detail(shipment);
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail cancel(Long id, CancelShipmentRequest request, String actor, String ipAddress) {
        Shipment shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        requirePendingAndVersion(shipment, request.version());
        OffsetDateTime now = OffsetDateTime.now(clock);
        shipment.status = ShipmentStatus.CANCELADO;
        shipment.updatedAt = now;
        shipments.saveAndFlush(shipment);
        events.save(new ShipmentEvent(shipment, "CANCELACION", "Envío cancelado", now));
        audits.save(new ShipmentAudit(shipment, "CANCELACION", actor, "status",
            ShipmentStatus.PENDIENTE_RECOGIDA.name(), ShipmentStatus.CANCELADO.name(),
            ipAddress, request.reason().strip(), now));
        entityManager.flush();
        return detail(shipment);
    }

    @Transactional
    public void delete(Long id, DeleteShipmentRequest request) {
        throw new ShipmentConflictException("La eliminación permanente está deshabilitada para conservar la trazabilidad");
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail changeStatus(Long id, ChangeStatusRequest request,
                                                           String actor, String ipAddress) {
        Shipment shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        requireVersion(shipment, request.version());
        if (!ShipmentStatusPolicy.allowed(shipment).contains(request.status()))
            throw new InvalidStatusTransitionException();
        if(request.status()==ShipmentStatus.ENTREGADO) throw new BusinessException("Usa Confirmar entrega y adjunta una evidencia");
        if(shipment.status==ShipmentStatus.INCIDENCIA) throw new BusinessException("Usa Resolver incidencia e indica la solución aplicada");
        if (request.status() == ShipmentStatus.INCIDENCIA
            && (request.reason() == null || request.reason().strip().length() < 10))
            throw new BusinessException("Describe el motivo de la incidencia (10 a 250 caracteres)");
        ShipmentStatus previous = shipment.status;
        if (request.status() == ShipmentStatus.INCIDENCIA) shipment.statusBeforeIncident = previous;
        else if (previous == ShipmentStatus.INCIDENCIA) shipment.statusBeforeIncident = null;
        shipment.status = request.status();
        OffsetDateTime now = OffsetDateTime.now(clock);
        shipment.updatedAt = now;
        shipments.saveAndFlush(shipment);
        String description = request.status() == ShipmentStatus.INCIDENCIA
            ? "Tu envío presenta una incidencia. Contacta a la empresa para más información."
            : previous == ShipmentStatus.INCIDENCIA ? "Incidencia resuelta"
            : "Estado actualizado a " + request.status().name();
        events.save(ShipmentEvent.statusChange(shipment, previous, actor, description, now));
        audits.save(new ShipmentAudit(shipment, "ESTADO", actor, "status", previous.name(),
            request.status().name(), ipAddress,
            request.status() == ShipmentStatus.INCIDENCIA ? request.reason().strip() : description, now));
        entityManager.flush();
        return detail(shipment);
    }

    @Transactional
    public ShipmentResponses.ShipmentDetail registerLocation(Long id, RegisterLocationRequest request,
                                                               String actor, String ipAddress) {
        Shipment shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        requireVersion(shipment, request.version());
        if (!ShipmentStatusPolicy.canRegisterLocation(shipment.status))
            throw new ShipmentConflictException("No disponible para este estado");
        String city = ColombianCities.requireCity(request.city());
        String reference = request.reference() == null || request.reference().isBlank()
            ? null : request.reference().strip();
        String before = currentCity(id);
        OffsetDateTime now = OffsetDateTime.now(clock);
        shipment.updatedAt = now;
        shipments.saveAndFlush(shipment);
        events.save(ShipmentEvent.location(shipment, city, reference, actor, now));
        audits.save(new ShipmentAudit(shipment, "UBICACION", actor, "currentCity", before,
            city, ipAddress, reference == null ? "Ubicación registrada" : reference, now));
        entityManager.flush();
        return detail(shipment);
    }

    private void requirePendingAndVersion(Shipment shipment, Long expectedVersion) {
        if (shipment.status != ShipmentStatus.PENDIENTE_RECOGIDA)
            throw new ShipmentConflictException("El envío ya no está pendiente de recogida");
        requireVersion(shipment, expectedVersion);
    }

    private void requireVersion(Shipment shipment, Long expectedVersion) {
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
        s.senderContactId = contacts.resolve(s.senderName,s.senderPhone,s.senderEmail);
        s.recipientContactId = contacts.resolve(s.recipientName,s.recipientPhone,s.recipientEmail);
        s.destinationStreet = in.destinationStreet().strip();
        s.destinationNumber = in.destinationNumber().strip();
        s.destinationCity = in.destinationCity().strip();
        s.weightKg = in.weightKg();
        s.lengthCm = in.lengthCm();
        s.widthCm = in.widthCm();
        s.heightCm = in.heightCm();
        s.packagingType = in.packagingType();
        s.packagingDescription = in.packagingType() == PackagingType.OTRO
            ? in.packagingDescription().strip() : null;
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
        values.put("lengthCm", s.lengthCm);
        values.put("widthCm", s.widthCm);
        values.put("heightCm", s.heightCm);
        values.put("packagingType", s.packagingType);
        values.put("packagingDescription", s.packagingDescription);
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
        List<ShipmentResponses.EventView> history = events.findByShipmentIdOrderByOccurredAtDescIdDesc(s.id)
            .stream().map(ShipmentResponses.EventView::of).toList();
        ShipmentInput input = new ShipmentInput(s.senderName, s.senderPhone, s.senderEmail,
            s.recipientName, s.recipientPhone, s.recipientEmail, s.destinationStreet,
            s.destinationNumber, s.destinationCity, s.weightKg, s.lengthCm, s.widthCm,
            s.heightCm, s.packagingType, s.packagingDescription, s.serviceType,
            s.scheduledDeliveryDate);
        return new ShipmentResponses.ShipmentDetail(s.id, s.trackingCode, s.status, s.version,
            input, s.estimatedCostCop, s.estimatedDeliveryDate, s.createdAt, s.updatedAt,
            currentCity(s.id), ShipmentStatusPolicy.allowed(s), history);
    }

    private String currentCity(Long id) {
        return events.findFirstByShipmentIdAndCityIsNotNullOrderByOccurredAtDescIdDesc(id)
            .map(event -> event.city).orElse(null);
    }

    private String maskName(String fullName) {
        String[] parts = fullName.strip().split("\\s+");
        return parts.length < 2 ? parts[0] : parts[0] + " " + parts[parts.length - 1].substring(0, 1).toUpperCase() + ".";
    }

    private void validatePackaging(ShipmentInput input) {
        if (input.packagingType() == PackagingType.OTRO
            && (input.packagingDescription() == null
                || input.packagingDescription().strip().length() < 3
                || input.packagingDescription().strip().length() > 100))
            throw new BusinessException("Describe el tipo de empaque (3 a 100 caracteres)");
    }

    private boolean sameNonPhysicalFields(Shipment s, ShipmentInput in) {
        return Objects.equals(s.senderName, in.senderName().strip())
            && Objects.equals(s.senderPhone, in.senderPhone().strip())
            && Objects.equals(s.senderEmail, in.senderEmail().strip())
            && Objects.equals(s.recipientName, in.recipientName().strip())
            && Objects.equals(s.recipientPhone, in.recipientPhone().strip())
            && Objects.equals(s.recipientEmail, in.recipientEmail().strip())
            && Objects.equals(s.destinationStreet, in.destinationStreet().strip())
            && Objects.equals(s.destinationNumber, in.destinationNumber().strip())
            && Objects.equals(s.destinationCity, in.destinationCity().strip())
            && s.serviceType == in.serviceType()
            && Objects.equals(s.scheduledDeliveryDate, in.scheduledDeliveryDate());
    }
}
