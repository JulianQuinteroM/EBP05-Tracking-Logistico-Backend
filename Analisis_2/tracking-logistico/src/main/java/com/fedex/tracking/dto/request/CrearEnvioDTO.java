package com.fedex.tracking.dto.request;

import com.fedex.tracking.model.enums.TipoPrioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CrearEnvioDTO {

    @Valid
    private ClienteEnvioDTO remitente;

    @Valid
    private ClienteEnvioDTO destinatario;

    @Valid
    private DireccionEnvioDTO direccionOrigen;

    @Valid
    private DireccionEnvioDTO direccionDestino;

    // IDs are retained for clients of the previous API.
    private Long idRemitente;
    private Long idDestinatario;
    private Long idDireccionOrigen;
    private Long idDireccionDestino;

    @NotNull(message = "La prioridad es obligatoria")
    private TipoPrioridad prioridad;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso;

    @NotBlank(message = "Las dimensiones son obligatorias")
    private String dimensiones;
    
    // Opcional: solo si es programado
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2})?",
            message = "La fecha estimada debe usar formato ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss)")
    private String fechaEstimadaEntrega;

    @Data
    public static class ClienteEnvioDTO {
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 150)
        private String nombre;
        @NotBlank(message = "El teléfono del cliente es obligatorio")
        @Size(max = 50)
        private String telefono;
        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El email del cliente no es válido")
        @Size(max = 150)
        private String email;
    }

    @Data
    public static class DireccionEnvioDTO {
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 200)
        private String calle;
        @NotBlank(message = "El número es obligatorio")
        @Size(max = 30)
        private String numero;
        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 100)
        private String ciudad;
        @Size(max = 100)
        private String departamentoProvincia;
        @Size(max = 20)
        private String codigoPostal;
        @Size(max = 100)
        private String pais;
    }

}
