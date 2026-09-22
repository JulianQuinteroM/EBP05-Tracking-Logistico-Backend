package com.fedex.tracking.dto.response;

import com.fedex.tracking.model.entity.Direccion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DireccionDTO {
    private Long id;
    private String calle;
    private String numero;
    private String ciudad;
    private String departamentoProvincia;
    private String codigoPostal;
    private String pais;

    public static DireccionDTO from(Direccion d) {
        return d == null ? null : DireccionDTO.builder().id(d.getId()).calle(d.getCalle())
                .numero(d.getNumero()).ciudad(d.getCiudad()).departamentoProvincia(d.getDepartamentoProvincia())
                .codigoPostal(d.getCodigoPostal()).pais(d.getPais()).build();
    }
}
