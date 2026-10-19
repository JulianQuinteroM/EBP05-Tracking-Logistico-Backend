package co.edu.udea.codefactory.tracking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(@NotNull @PositiveOrZero Long version,
                                  @NotNull ShipmentStatus status,
                                  @Size(max = 250) String reason) {}
