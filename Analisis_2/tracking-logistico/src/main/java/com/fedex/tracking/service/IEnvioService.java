package com.fedex.tracking.service;

import com.fedex.tracking.dto.request.ActualizarEnvioDTO;
import com.fedex.tracking.dto.request.CrearEnvioDTO;
import com.fedex.tracking.dto.request.FiltroEnvioDTO;
import com.fedex.tracking.dto.response.EnvioDetalleDTO;
import com.fedex.tracking.dto.response.RespuestaPaginadaDTO;
import com.fedex.tracking.dto.response.MovimientoEnvioDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface IEnvioService {
    EnvioDetalleDTO crearEnvio(CrearEnvioDTO dto, Long operadorId);
    EnvioDetalleDTO rastrearPorTrackingNumber(String codigo);
    EnvioDetalleDTO obtenerEnvio(Long id);
    List<MovimientoEnvioDTO> listarMovimientos(Long id);
    RespuestaPaginadaDTO<EnvioDetalleDTO> listarEnvios(FiltroEnvioDTO filtro, Pageable pageable);
    EnvioDetalleDTO actualizarEnvio(Long id, ActualizarEnvioDTO dto, String usuario);
    void eliminarEnvio(Long id, String usuario);
    byte[] exportarEnviosExcel(FiltroEnvioDTO filtro);
}
