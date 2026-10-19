package co.edu.udea.codefactory.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShipmentInput(
    @NotBlank @Size(max = 120) String senderName,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]{7,30}$") String senderPhone,
    @NotBlank @Email @Size(max = 254) String senderEmail,
    @NotBlank @Size(max = 120) String recipientName,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]{7,30}$") String recipientPhone,
    @NotBlank @Email @Size(max = 254) String recipientEmail,
    @NotBlank @Size(max = 180) String destinationStreet,
    @NotBlank @Size(max = 30) String destinationNumber,
    @NotBlank @Size(max = 120) String destinationCity,
    @NotNull @DecimalMin("0.01") @DecimalMax("50") @Digits(integer = 2, fraction = 2) BigDecimal weightKg,
    @NotNull @DecimalMin("0.01") @DecimalMax("200") @Digits(integer = 3, fraction = 2) BigDecimal lengthCm,
    @NotNull @DecimalMin("0.01") @DecimalMax("200") @Digits(integer = 3, fraction = 2) BigDecimal widthCm,
    @NotNull @DecimalMin("0.01") @DecimalMax("200") @Digits(integer = 3, fraction = 2) BigDecimal heightCm,
    @NotNull PackagingType packagingType,
    @Size(max = 100) String packagingDescription,
    @NotNull ServiceType serviceType,
    LocalDate scheduledDeliveryDate
) {}
