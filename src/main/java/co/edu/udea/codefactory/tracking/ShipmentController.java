package co.edu.udea.codefactory.tracking;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletRequest;
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
    private final PublicTrackingRateLimiter trackingLimiter;
    private final AuditService auditService;

    public ShipmentController(ShipmentService service, ServiceEstimate estimates, Clock clock,
                              PublicTrackingRateLimiter trackingLimiter, AuditService auditService) {
        this.service = service;
        this.estimates = estimates;
        this.clock = clock;
        this.trackingLimiter = trackingLimiter;
        this.auditService = auditService;
    }

    public record EstimateRequest(@NotNull ServiceType serviceType, LocalDate scheduledDeliveryDate) {}

    @PostMapping("/estimaciones")
    public ServiceEstimate.Estimate estimate(@Valid @RequestBody EstimateRequest request) {
        return estimates.calculate(request.serviceType(), request.scheduledDeliveryDate(),
            LocalDate.now(clock.withZone(ZoneId.of("America/Bogota"))));
    }

    @PostMapping("/envios")
    public ResponseEntity<ShipmentResponses.ShipmentDetail> create(@Valid @RequestBody ShipmentInput input,
        Authentication authentication, HttpServletRequest servletRequest) {
        ShipmentResponses.ShipmentDetail created = service.create(input, authentication.getName(), servletRequest.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/envios/" + created.id())).body(created);
    }

    @GetMapping("/seguimiento/{codigo}")
    public ShipmentResponses.PublicTracking track(@PathVariable String codigo, HttpServletRequest servletRequest) {
        String address = servletRequest.getRemoteAddr();
        trackingLimiter.check(address);
        try {
            ShipmentResponses.PublicTracking result = service.track(codigo);
            trackingLimiter.found(address);
            return result;
        } catch (ShipmentNotFoundException exception) {
            trackingLimiter.notFound(address);
            throw exception;
        }
    }

    @GetMapping("/ciudades")
    public java.util.List<String> cities(@RequestParam(defaultValue = "") String q) {
        return ColombianCities.suggest(q);
    }

    @GetMapping("/envios")
    public ShipmentResponses.ListPage list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "") String q,
                                                  @RequestParam(defaultValue = "createdAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ShipmentResponses.ListPage.of(service.list(page, size, q, sortBy, direction));
    }

    @GetMapping("/envios/{id}")
    public ShipmentResponses.ShipmentDetail get(@PathVariable Long id, Authentication authentication,
        HttpServletRequest servletRequest) {
        ShipmentResponses.ShipmentDetail result = service.get(id);
        auditService.recordAction("CONSULTA_HISTORIAL", authentication.getName(), id,
            servletRequest.getRemoteAddr(), "Consulta de ficha e historial de movimientos");
        return result;
    }

    @PutMapping("/envios/{id}")
    public ShipmentResponses.ShipmentDetail update(@PathVariable Long id,
        @Valid @RequestBody UpdateShipmentRequest request, Authentication authentication,
        HttpServletRequest servletRequest) {
        return service.update(id, request, authentication.getName(), servletRequest.getRemoteAddr());
    }

    @PostMapping("/envios/{id}/cancelacion")
    public ShipmentResponses.ShipmentDetail cancel(@PathVariable Long id,
        @Valid @RequestBody CancelShipmentRequest request, Authentication authentication,
        HttpServletRequest servletRequest) {
        return service.cancel(id, request, authentication.getName(), servletRequest.getRemoteAddr());
    }

    @PostMapping("/envios/{id}/estado")
    public ShipmentResponses.ShipmentDetail changeStatus(@PathVariable Long id,
        @Valid @RequestBody ChangeStatusRequest request, Authentication authentication,
        HttpServletRequest servletRequest) {
        try {
            return service.changeStatus(id, request, authentication.getName(), servletRequest.getRemoteAddr());
        } catch (InvalidStatusTransitionException exception) {
            auditService.recordAction("TRANSICION_INUSUAL", authentication.getName(), id,
                servletRequest.getRemoteAddr(), "Intento de cambio a " + request.status());
            throw exception;
        }
    }

    @PostMapping("/envios/{id}/ubicaciones")
    public ShipmentResponses.ShipmentDetail registerLocation(@PathVariable Long id,
        @Valid @RequestBody RegisterLocationRequest request, Authentication authentication,
        HttpServletRequest servletRequest) {
        return service.registerLocation(id, request, authentication.getName(), servletRequest.getRemoteAddr());
    }

    @DeleteMapping("/envios/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
        @Valid @RequestBody DeleteShipmentRequest request) {
        service.delete(id, request);
        return ResponseEntity.noContent().build();
    }
}
