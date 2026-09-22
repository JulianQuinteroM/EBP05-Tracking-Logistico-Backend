package com.fedex.tracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RespuestaPaginadaDTO<T> {
    private List<T> contenido;
    private int numeroPagina;
    private int tamanoPagina;
    private long totalElementos;
    private int totalPaginas;
    private boolean ultima;
}

