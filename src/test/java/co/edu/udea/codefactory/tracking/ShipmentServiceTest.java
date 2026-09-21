package co.edu.udea.codefactory.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShipmentServiceTest {
    private ShipmentRepository shipments;
    private ShipmentEventRepository events;
    private ShipmentAuditRepository audits;
    private TrackingSequence sequence;
    private ShipmentService service;

    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class);
        events = mock(ShipmentEventRepository.class);
        audits = mock(ShipmentAuditRepository.class);
        sequence = mock(TrackingSequence.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T14:00:00Z"), ZoneOffset.UTC);
        service = new ShipmentService(shipments, events, audits, sequence, new ServiceEstimate(),
            clock, new ObjectMapper(), mock(EntityManager.class));
    }

    private ShipmentInput input() {
        return new ShipmentInput("Ana", "+57 300 1234567", "ana@example.com",
            "Luis", "+57 310 7654321", "luis@example.com", "Calle 10",
            "20", "Medellín", new BigDecimal("2.500"), ServiceType.ESTANDAR, null);
    }

    private Shipment pending() {
        Shipment s = new Shipment();
        s.id = 7L;
        s.version = 0L;
        s.status = ShipmentStatus.PENDIENTE_RECOGIDA;
        s.trackingCode = "LOG-29047381-8";
        s.createdAt = java.time.OffsetDateTime.parse("2026-09-15T14:00:00Z");
        s.updatedAt = s.createdAt;
        s.senderName = "Ana";
        s.senderPhone = "+57 300 1234567";
        s.senderEmail = "ana@example.com";
        s.recipientName = "Luis";
        s.recipientPhone = "+57 310 7654321";
        s.recipientEmail = "luis@example.com";
        s.destinationStreet = "Calle 10";
        s.destinationNumber = "20";
        s.destinationCity = "Medellín";
        s.weightKg = new BigDecimal("2.500");
        s.serviceType = ServiceType.ESTANDAR;
        s.estimatedCostCop = new BigDecimal("10000.00");
        s.estimatedDeliveryDate = java.time.LocalDate.of(2026, 9, 20);
        return s;
    }

    @Test void createStartsPendingWithLuhnCodeAndEvent() {
        when(sequence.next()).thenReturn(29047381L);
        when(shipments.saveAndFlush(any())).thenAnswer(invocation -> {
            Shipment s = invocation.getArgument(0);
            s.id = 7L;
            s.version = 0L;
            return s;
        });
        when(events.findByShipmentIdOrderByOccurredAtAscIdAsc(7L)).thenReturn(List.of());
        var response = service.create(input());
        assertEquals("LOG-29047381-8", response.trackingCode());
        assertEquals(ShipmentStatus.PENDIENTE_RECOGIDA, response.status());
        assertEquals(0L, response.version());
        verify(events).save(any(ShipmentEvent.class));
    }

    @Test void publicTrackingHasNoPartyOrAddressFields() {
        Shipment s = pending();
        when(shipments.findByTrackingCode(s.trackingCode)).thenReturn(Optional.of(s));
        when(events.findFirstByShipmentIdAndPublicVisibleTrueOrderByOccurredAtDescIdDesc(7L))
            .thenReturn(Optional.empty());
        String json = assertDoesSerialize(service.track("log290473818"));
        assertFalse(json.contains("Ana"));
        assertFalse(json.contains("Calle"));
        assertFalse(json.contains("senderName"));
        assertTrue(json.contains("LOG-29047381-8"));
    }

    private String assertDoesSerialize(Object result) {
        try { return new ObjectMapper().findAndRegisterModules().writeValueAsString(result); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @Test void staleVersionAndCancelledShipmentCannotBeEdited() {
        Shipment s = pending();
        when(shipments.findById(7L)).thenReturn(Optional.of(s));
        assertThrows(ShipmentConflictException.class,
            () -> service.update(7L, new UpdateShipmentRequest(3L, input()), "operador"));
        s.status = ShipmentStatus.CANCELADO;
        assertThrows(ShipmentConflictException.class,
            () -> service.update(7L, new UpdateShipmentRequest(0L, input()), "operador"));
        verify(audits, never()).save(any());
    }

    @Test void unchangedEditDoesNotMakeAuditOrShiftEta() {
        Shipment s = pending();
        when(shipments.findById(7L)).thenReturn(Optional.of(s));
        when(events.findByShipmentIdOrderByOccurredAtAscIdAsc(7L)).thenReturn(List.of());
        var response = service.update(7L, new UpdateShipmentRequest(0L, input()), "operador");
        assertEquals(java.time.LocalDate.of(2026, 9, 20), response.estimatedDeliveryDate());
        verify(audits, never()).save(any());
        verify(shipments, never()).saveAndFlush(any());
    }

    @Test void cancelDoesNotDeleteAndSecondAttemptFails() {
        Shipment s = pending();
        when(shipments.findById(7L)).thenReturn(Optional.of(s));
        when(shipments.saveAndFlush(any())).thenAnswer(invocation -> {
            Shipment loaded = invocation.getArgument(0);
            loaded.version = 1L;
            return loaded;
        });
        when(events.findByShipmentIdOrderByOccurredAtAscIdAsc(7L)).thenReturn(List.of());
        var response = service.cancel(7L, new CancelShipmentRequest(0L, "Registro duplicado"), "operador");
        assertEquals(ShipmentStatus.CANCELADO, response.status());
        verify(shipments, never()).delete(any(Shipment.class));
        verify(events).save(any(ShipmentEvent.class));
        verify(audits).save(any(ShipmentAudit.class));
        assertThrows(ShipmentConflictException.class,
            () -> service.cancel(7L, new CancelShipmentRequest(1L, "Otra vez"), "operador"));
    }

    @Test void deleteRemovesDependentsAndShipmentWhenVersionMatches() {
        Shipment s = pending();
        when(shipments.findById(7L)).thenReturn(Optional.of(s));

        service.delete(7L, new DeleteShipmentRequest(0L));

        verify(audits).deleteAllByShipmentId(7L);
        verify(events).deleteAllByShipmentId(7L);
        verify(shipments).delete(s);
    }

    @Test void deleteRejectsStaleVersionWithoutRemovingAnything() {
        Shipment s = pending();
        when(shipments.findById(7L)).thenReturn(Optional.of(s));

        assertThrows(ShipmentConflictException.class,
            () -> service.delete(7L, new DeleteShipmentRequest(3L)));

        verify(audits, never()).deleteAllByShipmentId(any());
        verify(events, never()).deleteAllByShipmentId(any());
        verify(shipments, never()).delete(any(Shipment.class));
    }
}
