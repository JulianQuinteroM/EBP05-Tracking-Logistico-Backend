package com.fedex.tracking.dto.response;

import com.fedex.tracking.model.enums.EstadoEnvio;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MovimientoEnvioDTO {
    private Long id;
    private EstadoEnvio estado;
    private String descripcion;
    private String ubicacion;
    private LocalDateTime fechaHora;
}
