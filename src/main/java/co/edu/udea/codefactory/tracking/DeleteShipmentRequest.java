package co.edu.udea.codefactory.tracking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DeleteShipmentRequest(@NotNull @PositiveOrZero Long version) {}
