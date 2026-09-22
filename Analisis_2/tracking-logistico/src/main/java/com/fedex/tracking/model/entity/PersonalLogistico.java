package com.fedex.tracking.model.entity;

import com.fedex.tracking.model.enums.RolPersonal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personal_logistico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalLogistico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "email_corporativo", nullable = false, unique = true)
    private String emailCorporativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolPersonal rol;
}

