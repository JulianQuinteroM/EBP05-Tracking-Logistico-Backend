package com.fedex.tracking.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "direcciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String calle;

    private String numero;

    @Column(nullable = false)
    private String ciudad;

    @Column(name = "departamento_provincia")
    private String departamentoProvincia;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    private String pais;
}

