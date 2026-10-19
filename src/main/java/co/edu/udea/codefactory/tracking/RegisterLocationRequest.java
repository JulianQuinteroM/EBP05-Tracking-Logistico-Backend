package co.edu.udea.codefactory.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegisterLocationRequest(@NotNull @PositiveOrZero Long version,
                                      @NotBlank String city,
                                      @Size(max = 100) String reference) {}
