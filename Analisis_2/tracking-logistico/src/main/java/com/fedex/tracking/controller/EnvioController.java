package com.fedex.tracking.controller;

import com.fedex.tracking.dto.request.ActualizarEnvioDTO;
import com.fedex.tracking.dto.request.CrearEnvioDTO;
import com.fedex.tracking.dto.request.FiltroEnvioDTO;
import com.fedex.tracking.dto.response.EnvioDetalleDTO;
import com.fedex.tracking.dto.response.RespuestaPaginadaDTO;
import com.fedex.tracking.service.IEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.fedex.tracking.dto.response.MovimientoEnvioDTO;

@RestController
@RequestMapping("/api/envios")
@RequiredArgsConstructor
@Tag(name = "Envíos", description = "API para la gestión de envíos (Tracking Logístico)")
public class EnvioController {

    private final IEnvioService envioService;

    @PostMapping
    @Operation(summary = "Crear un nuevo envío", description = "Genera el envío y calcula su costo y fecha estimada")
    public ResponseEntity<EnvioDetalleDTO> crearEnvio(@Valid @RequestBody CrearEnvioDTO dto, @RequestParam(defaultValue = "1") Long operadorId) {
        EnvioDetalleDTO creado = envioService.crearEnvio(dto, operadorId);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @GetMapping("/tracking/{codigo}")
    @Operation(summary = "Rastrear envío por Tracking Number", description = "Búsqueda pública por código LOG-XXXXXXXX-X")
    public ResponseEntity<EnvioDetalleDTO> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(envioService.rastrearPorTrackingNumber(codigo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvioDetalleDTO> obtenerEnvio(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.obtenerEnvio(id));
    }

    @GetMapping("/{id}/movimientos")
    public ResponseEntity<List<MovimientoEnvioDTO>> listarMovimientos(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.listarMovimientos(id));
    }

    @GetMapping
    @Operation(summary = "Listar envíos con filtros y paginación")
    public ResponseEntity<RespuestaPaginadaDTO<EnvioDetalleDTO>> listarEnvios(
            @ModelAttribute FiltroEnvioDTO filtro,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(envioService.listarEnvios(filtro, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos del envío (Auditoría)")
    public ResponseEntity<EnvioDetalleDTO> actualizarEnvio(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEnvioDTO dto,
            @RequestParam(defaultValue = "Admin") String usuario) {
        return ResponseEntity.ok(envioService.actualizarEnvio(id, dto, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrado lógico (Cancelar envío)", description = "Solo permitido en estado PENDIENTE_RECOGIDA")
    public ResponseEntity<Void> eliminarEnvio(@PathVariable Long id, @RequestParam(defaultValue = "Admin") String usuario) {
        envioService.eliminarEnvio(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Exportar a Excel", description = "Descarga la lista filtrada de envíos en formato Excel")
    public ResponseEntity<byte[]> exportarReporte(@ModelAttribute FiltroEnvioDTO filtro) {
        byte[] excelBytes = envioService.exportarEnviosExcel(filtro);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "envios.xlsx");
        
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
