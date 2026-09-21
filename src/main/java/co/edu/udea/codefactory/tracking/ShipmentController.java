package co.edu.udea.codefactory.tracking;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShipmentController {
    private final ShipmentService service;
    private final ServiceEstimate estimates;
    private final Clock clock;

    public ShipmentController(ShipmentService service, ServiceEstimate estimates, Clock clock) {
        this.service = service;
        this.estimates = estimates;
        this.clock = clock;
    }

    public record EstimateRequest(@NotNull ServiceType serviceType, LocalDate scheduledDeliveryDate) {}

    @PostMapping("/estimaciones")
    public ServiceEstimate.Estimate estimate(@Valid @RequestBody EstimateRequest request) {
        return estimates.calculate(request.serviceType(), request.scheduledDeliveryDate(),
            LocalDate.now(clock.withZone(ZoneId.of("America/Bogota"))));
    }

    @PostMapping("/envios")
    public ResponseEntity<ShipmentResponses.ShipmentDetail> create(@Valid @RequestBody ShipmentInput input) {
        ShipmentResponses.ShipmentDetail created = service.create(input);
        return ResponseEntity.created(URI.create("/api/envios/" + created.id())).body(created);
    }

    @GetMapping("/seguimiento/{codigo}")
    public ShipmentResponses.PublicTracking track(@PathVariable String codigo) { return service.track(codigo); }

    @GetMapping("/envios")
    public ShipmentResponses.ListPage list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "") String q,
                                                  @RequestParam(defaultValue = "createdAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ShipmentResponses.ListPage.of(service.list(page, size, q, sortBy, direction));
    }

    @GetMapping("/envios/{id}")
    public ShipmentResponses.ShipmentDetail get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/envios/{id}")
    public ShipmentResponses.ShipmentDetail update(@PathVariable Long id,
        @Valid @RequestBody UpdateShipmentRequest request, Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @PostMapping("/envios/{id}/cancelacion")
    public ShipmentResponses.ShipmentDetail cancel(@PathVariable Long id,
        @Valid @RequestBody CancelShipmentRequest request, Authentication authentication) {
        return service.cancel(id, request, authentication.getName());
    }

    @DeleteMapping("/envios/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
        @Valid @RequestBody DeleteShipmentRequest request) {
        service.delete(id, request);
        return ResponseEntity.noContent().build();
    }
}
