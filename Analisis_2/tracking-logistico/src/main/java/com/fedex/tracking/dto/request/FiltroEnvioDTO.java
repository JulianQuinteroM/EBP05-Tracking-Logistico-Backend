package com.fedex.tracking.dto.request;

import com.fedex.tracking.model.enums.EstadoEnvio;
import com.fedex.tracking.model.enums.TipoPrioridad;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FiltroEnvioDTO {
    private String textoBusqueda; // código, remitente, destinatario
    private EstadoEnvio estado;
    private TipoPrioridad tipo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
}

