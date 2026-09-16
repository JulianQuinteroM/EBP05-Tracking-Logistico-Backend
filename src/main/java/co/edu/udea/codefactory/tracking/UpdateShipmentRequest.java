package co.edu.udea.codefactory.tracking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateShipmentRequest(@NotNull @PositiveOrZero Long version, @NotNull @Valid ShipmentInput shipment) {}
