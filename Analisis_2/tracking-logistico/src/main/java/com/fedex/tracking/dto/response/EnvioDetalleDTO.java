package com.fedex.tracking.dto.response;

import com.fedex.tracking.model.enums.EstadoEnvio;
import com.fedex.tracking.model.enums.TipoPrioridad;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EnvioDetalleDTO {
    private Long id;
    private String trackingNumber;
    private TipoPrioridad prioridad;
    private Double peso;
    private String dimensiones;
    private BigDecimal costo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaEstimadaEntrega;
    private EstadoEnvio estadoOperativo;
    
    private Long idRemitente;
    private String nombreRemitente;
    private String telefonoRemitente;
    private String emailRemitente;
    private DireccionDTO direccionOrigen;
    
    private Long idDestinatario;
    private String nombreDestinatario;
    private String telefonoDestinatario;
    private String emailDestinatario;
    private DireccionDTO direccionDestino;
    private List<MovimientoEnvioDTO> historialMovimientos;
    private String ubicacionActual;
}
