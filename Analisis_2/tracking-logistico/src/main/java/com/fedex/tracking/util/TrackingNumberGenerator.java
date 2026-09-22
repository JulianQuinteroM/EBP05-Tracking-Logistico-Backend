package com.fedex.tracking.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TrackingNumberGenerator {

    private static final String PREFIX = "LOG";
    // ^[a-zA-Z]{3}-?\d{8}-?\d{1}$
    private static final Pattern TRACKING_PATTERN = Pattern.compile("^[a-zA-Z]{3}-?\\d{8}-?\\d{1}$");

    public static String generarCodigo(long consecutivo) {
        if (consecutivo < 1 || consecutivo > 99_999_999) {
            throw new IllegalArgumentException("El consecutivo de tracking debe estar entre 1 y 99999999");
        }

        String baseDigits = String.format("%08d", consecutivo);
        int sumaDigitos = baseDigits.chars().map(Character::getNumericValue).sum();
        String verificador = String.valueOf(sumaDigitos % 10);
        return PREFIX + "-" + baseDigits + "-" + verificador;
    }

    public boolean validarFormato(String codigo) {
        if (codigo == null) return false;
        return TRACKING_PATTERN.matcher(codigo).matches();
    }
    
    public String normalizarCodigo(String codigo) {
        if (codigo == null) return null;
        codigo = codigo.toUpperCase();
        // Si viene sin guiones LOG384729155 lo convertimos a LOG-38472915-5
        if (!codigo.contains("-") && codigo.length() == 12) {
            return codigo.substring(0, 3) + "-" + codigo.substring(3, 11) + "-" + codigo.substring(11, 12);
        }
        return codigo;
    }
}
