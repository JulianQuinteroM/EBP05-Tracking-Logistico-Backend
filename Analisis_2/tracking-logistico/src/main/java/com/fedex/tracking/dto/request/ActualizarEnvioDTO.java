package com.fedex.tracking.dto.request;

import com.fedex.tracking.model.enums.TipoPrioridad;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ActualizarEnvioDTO {

    private Long idRemitente;

    private Long idDestinatario;

    private Long idDireccionOrigen;

    private Long idDireccionDestino;

    private TipoPrioridad prioridad;

    @Min(value = 0, message = "El peso debe ser mayor a 0")
    private Double peso;

    private String dimensiones;
}

