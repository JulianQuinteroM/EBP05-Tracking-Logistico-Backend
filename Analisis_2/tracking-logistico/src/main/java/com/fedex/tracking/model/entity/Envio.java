package com.fedex.tracking.model.entity;

import com.fedex.tracking.model.enums.EstadoEnvio;
import com.fedex.tracking.model.enums.TipoPrioridad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "envios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", unique = true, length = 15)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPrioridad prioridad;

    @Column(nullable = false)
    private Double peso;

    private String dimensiones;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_estimada_entrega", nullable = false)
    private LocalDateTime fechaEstimadaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false)
    private EstadoEnvio estadoOperativo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_remitente", nullable = false)
    private Cliente remitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_destinatario", nullable = false)
    private Cliente destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_direccion_origen", nullable = false)
    private Direccion direccionOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_direccion_destino", nullable = false)
    private Direccion direccionDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado_creador", nullable = false)
    private PersonalLogistico operadorCreador;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.estadoOperativo == null) {
            this.estadoOperativo = EstadoEnvio.PENDIENTE_RECOGIDA;
        }
        calcularCosto();
        calcularFechaEstimada();
    }

    public void calcularCosto() {
        BigDecimal costoBase = BigDecimal.valueOf(peso * 2000); // 2000 por kg
        BigDecimal costoPrioridad = switch (prioridad) {
            case ESTANDAR -> BigDecimal.valueOf(10000);
            case PROGRAMADO -> BigDecimal.valueOf(15000);
            case EXPRES -> BigDecimal.valueOf(25000);
            default -> BigDecimal.ZERO;
        };
        this.costo = costoBase.add(costoPrioridad);
    }

    public void calcularFechaEstimada() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        if (this.fechaEstimadaEntrega != null) {
            return;
        }
        this.fechaEstimadaEntrega = switch (prioridad) {
            case ESTANDAR -> this.fechaRegistro.plusDays(4); // 3-5 days avg
            case PROGRAMADO -> this.fechaRegistro.plusDays(7);
            case EXPRES -> this.fechaRegistro.plusHours(36); // 24-48h avg
            default -> this.fechaRegistro.plusDays(4);
        };
    }

    public Boolean esEliminable() {
        return this.estadoOperativo == EstadoEnvio.PENDIENTE_RECOGIDA;
    }
}
