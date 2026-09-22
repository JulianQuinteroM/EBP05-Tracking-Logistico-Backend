package com.fedex.tracking.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envio_id", nullable = false)
    private Long envioId;

    @Column(name = "usuario_modificador", nullable = false)
    private String usuarioModificador;

    @Column(name = "campos_modificados", columnDefinition = "TEXT")
    private String camposModificados;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}

