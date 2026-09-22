package com.fedex.tracking.service.impl;

import com.fedex.tracking.dto.request.ActualizarEnvioDTO;
import com.fedex.tracking.dto.request.CrearEnvioDTO;
import com.fedex.tracking.dto.request.FiltroEnvioDTO;
import com.fedex.tracking.dto.response.EnvioDetalleDTO;
import com.fedex.tracking.dto.response.RespuestaPaginadaDTO;
import com.fedex.tracking.dto.response.MovimientoEnvioDTO;
import com.fedex.tracking.dto.response.DireccionDTO;
import com.fedex.tracking.exception.BusinessRuleException;
import com.fedex.tracking.exception.ResourceNotFoundException;
import com.fedex.tracking.model.entity.*;
import com.fedex.tracking.model.enums.EstadoEnvio;
import com.fedex.tracking.model.enums.TipoPrioridad;
import com.fedex.tracking.repository.*;
import com.fedex.tracking.service.IEnvioService;
import com.fedex.tracking.util.TrackingNumberGenerator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnvioServiceImpl implements IEnvioService {

    private final EnvioRepository envioRepository;
    private final ClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;
    private final PersonalLogisticoRepository personalLogisticoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final TrackingNumberGenerator trackingGenerator;
    private final MovimientoEnvioRepository movimientoRepository;
    private final TrackingNumberSequenceRepository trackingSequenceRepository;

    @Override
    @Transactional
    public EnvioDetalleDTO crearEnvio(CrearEnvioDTO dto, Long operadorId) {
        if (dto.getPrioridad() == TipoPrioridad.PROGRAMADO
                && (dto.getFechaEstimadaEntrega() == null || dto.getFechaEstimadaEntrega().isBlank())) {
            throw new BusinessRuleException("La fecha estimada es obligatoria para envíos programados");
        }
        Cliente remitente = resolveCliente(dto.getRemitente(), dto.getIdRemitente(), "remitente");
        Cliente destinatario = resolveCliente(dto.getDestinatario(), dto.getIdDestinatario(), "destinatario");
        Direccion dirOrigen = resolveDireccion(dto.getDireccionOrigen(), dto.getIdDireccionOrigen(), "origen");
        Direccion dirDestino = resolveDireccion(dto.getDireccionDestino(), dto.getIdDireccionDestino(), "destino");
        PersonalLogistico operador = getOperador(operadorId);

        Envio envio = Envio.builder()
                .trackingNumber(nextTrackingNumber())
                .prioridad(dto.getPrioridad())
                .peso(dto.getPeso())
                .dimensiones(dto.getDimensiones())
                .remitente(remitente)
                .destinatario(destinatario)
                .direccionOrigen(dirOrigen)
                .direccionDestino(dirDestino)
                .operadorCreador(operador)
                .build();
        if (dto.getPrioridad() == TipoPrioridad.PROGRAMADO
                && dto.getFechaEstimadaEntrega() != null && !dto.getFechaEstimadaEntrega().isBlank()) {
            envio.setFechaEstimadaEntrega(parseFechaEstimada(dto.getFechaEstimadaEntrega()));
        }
                
        envio = envioRepository.save(envio);
        registrarMovimiento(envio, envio.getEstadoOperativo(), "Envío creado", dirOrigen.getCiudad());
        return mapToDTO(envio);
    }

    private String nextTrackingNumber() {
        TrackingNumberSequence sequence = trackingSequenceRepository.save(new TrackingNumberSequence());
        String tracking = TrackingNumberGenerator.generarCodigo(sequence.getId());
        while (envioRepository.existsByTrackingNumberIgnoreCase(tracking)) {
            sequence = trackingSequenceRepository.save(new TrackingNumberSequence());
            tracking = TrackingNumberGenerator.generarCodigo(sequence.getId());
        }
        return tracking;
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioDetalleDTO obtenerEnvio(Long id) {
        return mapToDTO(envioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoEnvioDTO> listarMovimientos(Long id) {
        if (!envioRepository.existsById(id)) throw new ResourceNotFoundException("Envío no encontrado");
        return movimientoRepository.findByEnvioIdOrderByFechaHoraAsc(id).stream().map(this::mapMovimiento).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioDetalleDTO rastrearPorTrackingNumber(String codigo) {
        if (!trackingGenerator.validarFormato(codigo)) {
            throw new BusinessRuleException("Formato de tracking number inválido");
        }
        String codigoNormalizado = trackingGenerator.normalizarCodigo(codigo);
        
        Envio envio = envioRepository.findByTrackingNumberIgnoreCase(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado con código: " + codigoNormalizado));
                
        return mapToDTO(envio);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPaginadaDTO<EnvioDetalleDTO> listarEnvios(FiltroEnvioDTO filtro, Pageable pageable) {
        Specification<Envio> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filtro.getTextoBusqueda() != null && !filtro.getTextoBusqueda().isEmpty()) {
                String searchPattern = "%" + filtro.getTextoBusqueda().toLowerCase() + "%";
                Predicate porTracking = cb.like(cb.lower(root.get("trackingNumber")), searchPattern);
                Predicate porRemitente = cb.like(cb.lower(root.get("remitente").get("nombre")), searchPattern);
                Predicate porDestinatario = cb.like(cb.lower(root.get("destinatario").get("nombre")), searchPattern);
                predicates.add(cb.or(porTracking, porRemitente, porDestinatario));
            }
            if (filtro.getEstado() != null) {
                predicates.add(cb.equal(root.get("estadoOperativo"), filtro.getEstado()));
            }
            if (filtro.getTipo() != null) {
                predicates.add(cb.equal(root.get("prioridad"), filtro.getTipo()));
            }
            if (filtro.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaInicio()));
            }
            if (filtro.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaFin()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Envio> page = envioRepository.findAll(spec, pageable);
        List<EnvioDetalleDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return new RespuestaPaginadaDTO<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional
    public EnvioDetalleDTO actualizarEnvio(Long id, ActualizarEnvioDTO dto, String usuario) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado"));
                
        StringBuilder cambios = new StringBuilder();

        if (dto.getPrioridad() != null && envio.getPrioridad() != dto.getPrioridad()) {
            cambios.append("Prioridad modificada: ").append(envio.getPrioridad()).append(" -> ").append(dto.getPrioridad()).append("; ");
            envio.setPrioridad(dto.getPrioridad());
            envio.calcularCosto();
            envio.calcularFechaEstimada();
        }
        if (dto.getPeso() != null && !envio.getPeso().equals(dto.getPeso())) {
            cambios.append("Peso modificado: ").append(envio.getPeso()).append(" -> ").append(dto.getPeso()).append("; ");
            envio.setPeso(dto.getPeso());
            envio.calcularCosto();
        }
        if (dto.getDimensiones() != null && !envio.getDimensiones().equals(dto.getDimensiones())) {
            cambios.append("Dimensiones modificadas: ").append(envio.getDimensiones()).append(" -> ").append(dto.getDimensiones()).append("; ");
            envio.setDimensiones(dto.getDimensiones());
        }
        if (dto.getIdRemitente() != null && !envio.getRemitente().getId().equals(dto.getIdRemitente())) {
            Cliente rem = getCliente(dto.getIdRemitente());
            cambios.append("Remitente modificado; ");
            envio.setRemitente(rem);
        }
        if (dto.getIdDestinatario() != null && !envio.getDestinatario().getId().equals(dto.getIdDestinatario())) {
            Cliente dest = getCliente(dto.getIdDestinatario());
            cambios.append("Destinatario modificado; ");
            envio.setDestinatario(dest);
        }
        if (dto.getIdDireccionOrigen() != null && !envio.getDireccionOrigen().getId().equals(dto.getIdDireccionOrigen())) {
            envio.setDireccionOrigen(getDireccion(dto.getIdDireccionOrigen()));
            cambios.append("Dirección de origen modificada; ");
        }
        if (dto.getIdDireccionDestino() != null && !envio.getDireccionDestino().getId().equals(dto.getIdDireccionDestino())) {
            envio.setDireccionDestino(getDireccion(dto.getIdDireccionDestino()));
            cambios.append("Dirección de destino modificada; ");
        }

        if (!cambios.isEmpty()) {
            RegistroAuditoria auditoria = RegistroAuditoria.builder()
                    .envioId(envio.getId())
                    .usuarioModificador(usuario)
                    .camposModificados(cambios.toString())
                    .fechaHora(LocalDateTime.now())
                    .build();
            auditoriaRepository.save(auditoria);
        }

        envio = envioRepository.save(envio);
        return mapToDTO(envio);
    }

    @Override
    @Transactional
    public void eliminarEnvio(Long id, String usuario) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado"));

        if (!envio.esEliminable()) {
            throw new BusinessRuleException("Solo se pueden eliminar envíos en estado PENDIENTE_RECOGIDA");
        }

        envio.setEstadoOperativo(EstadoEnvio.CANCELADO);
        envioRepository.save(envio);

        RegistroAuditoria auditoria = RegistroAuditoria.builder()
                .envioId(envio.getId())
                .usuarioModificador(usuario)
                .camposModificados("Envio cancelado lógicamente")
                .fechaHora(LocalDateTime.now())
                .build();
        auditoriaRepository.save(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarEnviosExcel(FiltroEnvioDTO filtro) {
        List<Envio> envios = listarEnviosParaExportacion(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Envíos");

            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Tracking Number", "Prioridad", "Estado", "Remitente", "Destinatario", "Fecha Registro", "Costo"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (Envio envio : envios) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(envio.getId());
                row.createCell(1).setCellValue(envio.getTrackingNumber());
                row.createCell(2).setCellValue(envio.getPrioridad().name());
                row.createCell(3).setCellValue(envio.getEstadoOperativo().name());
                row.createCell(4).setCellValue(envio.getRemitente().getNombre());
                row.createCell(5).setCellValue(envio.getDestinatario().getNombre());
                row.createCell(6).setCellValue(envio.getFechaRegistro().toString());
                row.createCell(7).setCellValue(envio.getCosto().doubleValue());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar Excel: " + e.getMessage());
        }
    }

    private List<Envio> listarEnviosParaExportacion(FiltroEnvioDTO filtro) {
        Specification<Envio> spec = (root, query, cb) -> cb.conjunction(); // Same logic as list, simplified
        return envioRepository.findAll(spec);
    }

    private EnvioDetalleDTO mapToDTO(Envio envio) {
        return EnvioDetalleDTO.builder()
                .id(envio.getId())
                .trackingNumber(envio.getTrackingNumber())
                .prioridad(envio.getPrioridad())
                .peso(envio.getPeso())
                .dimensiones(envio.getDimensiones())
                .costo(envio.getCosto())
                .fechaRegistro(envio.getFechaRegistro())
                .fechaEstimadaEntrega(envio.getFechaEstimadaEntrega())
                .estadoOperativo(envio.getEstadoOperativo())
                .idRemitente(envio.getRemitente().getId())
                .nombreRemitente(envio.getRemitente().getNombre())
                .telefonoRemitente(envio.getRemitente().getTelefono())
                .emailRemitente(envio.getRemitente().getEmail())
                .direccionOrigen(DireccionDTO.from(envio.getDireccionOrigen()))
                .idDestinatario(envio.getDestinatario().getId())
                .nombreDestinatario(envio.getDestinatario().getNombre())
                .telefonoDestinatario(envio.getDestinatario().getTelefono())
                .emailDestinatario(envio.getDestinatario().getEmail())
                .direccionDestino(DireccionDTO.from(envio.getDireccionDestino()))
                .historialMovimientos(movimientoRepository.findByEnvioIdOrderByFechaHoraAsc(envio.getId()).stream()
                        .map(this::mapMovimiento).toList())
                .ubicacionActual(movimientoRepository.findByEnvioIdOrderByFechaHoraAsc(envio.getId()).stream()
                        .filter(m -> m.getUbicacion() != null && !m.getUbicacion().isBlank())
                        .reduce((first, second) -> second).map(MovimientoEnvio::getUbicacion).orElse(null))
                .build();
    }

    private MovimientoEnvioDTO mapMovimiento(MovimientoEnvio movimiento) {
        return MovimientoEnvioDTO.builder().id(movimiento.getId()).estado(movimiento.getEstado())
                .descripcion(movimiento.getDescripcion()).ubicacion(movimiento.getUbicacion())
                .fechaHora(movimiento.getFechaHora()).build();
    }

    private void registrarMovimiento(Envio envio, EstadoEnvio estado, String descripcion, String ubicacion) {
        movimientoRepository.save(MovimientoEnvio.builder().envio(envio).estado(estado)
                .descripcion(descripcion).ubicacion(ubicacion).build());
    }

    private Cliente getCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private Cliente resolveCliente(CrearEnvioDTO.ClienteEnvioDTO data, Long legacyId, String role) {
        if (data == null) {
            if (legacyId == null) {
                throw new BusinessRuleException("Debe informar los datos del " + role + " o su ID");
            }
            return getCliente(legacyId);
        }
        String email = data.getEmail().trim();
        Cliente cliente = clienteRepository.findFirstByEmailIgnoreCase(email).orElse(null);
        if (cliente != null) {
            if (!cliente.getNombre().equalsIgnoreCase(data.getNombre().trim())
                    || !cliente.getTelefono().equals(data.getTelefono().trim())) {
                throw new BusinessRuleException("El email del " + role + " ya pertenece a otro cliente");
            }
            return cliente;
        }
        return clienteRepository.save(Cliente.builder().nombre(data.getNombre().trim())
                .telefono(data.getTelefono().trim()).email(email).build());
    }

    private Direccion resolveDireccion(CrearEnvioDTO.DireccionEnvioDTO data, Long legacyId, String role) {
        if (data == null) {
            if (legacyId == null) {
                throw new BusinessRuleException("Debe informar la dirección de " + role + " o su ID");
            }
            return getDireccion(legacyId);
        }
        String calle = data.getCalle().trim();
        String numero = data.getNumero().trim();
        String ciudad = data.getCiudad().trim();
        String departamento = normalize(data.getDepartamentoProvincia());
        String postal = normalize(data.getCodigoPostal());
        String pais = normalize(data.getPais());
        return direccionRepository
                .findFirstByCalleIgnoreCaseAndNumeroIgnoreCaseAndCiudadIgnoreCaseAndDepartamentoProvinciaIgnoreCaseAndCodigoPostalIgnoreCaseAndPaisIgnoreCase(
                        calle, numero, ciudad, departamento, postal, pais)
                .orElseGet(() -> direccionRepository.save(Direccion.builder().calle(calle).numero(numero)
                        .ciudad(ciudad).departamentoProvincia(departamento).codigoPostal(postal).pais(pais).build()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime parseFechaEstimada(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new BusinessRuleException("La fecha estimada debe usar formato ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss)");
            }
        }
    }

    private Direccion getDireccion(Long id) {
        return direccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada: " + id));
    }

    private PersonalLogistico getOperador(Long id) {
        return personalLogisticoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personal logístico no encontrado: " + id));
    }
}
