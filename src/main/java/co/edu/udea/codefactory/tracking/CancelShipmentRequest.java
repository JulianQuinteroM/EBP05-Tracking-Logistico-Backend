package co.edu.udea.codefactory.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CancelShipmentRequest(@NotNull @PositiveOrZero Long version, @NotBlank @Size(max = 500) String reason) {}
