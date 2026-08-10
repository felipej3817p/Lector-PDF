package com.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Catalogo oficial de zonas.
 * Zonas principales: CENTRO, NORTE, OCCIDENTE, ORIENTE, PUERTO_BOYACA,
 * RICAURTE, SUGAMUXI, TUNDAMA.
 * Sub-zonas de CENTRO con coordinadores propios: CENTRO_TIBANA,
 * CENTRO_MIRAFLORES, CENTRO_VILLA_DE_LEYVA, CENTRO_SAMACA.
 * Direcciones: DIRECCION_MANTENIMIENTO, DIRECCION_OPERACION, DIRECCION_PERDIDAS.
 * Valores legados aceptados para datos existentes: PUERTO, EDIFICIO.
 */
public enum AreaCode {

    // Zonas principales
    CENTRO("CENTRO"),
    NORTE("NORTE"),
    OCCIDENTE("OCCIDENTE"),
    ORIENTE("ORIENTE"),
    PUERTO_BOYACA("PUERTO BOYACA"),
    RICAURTE("RICAURTE"),
    SUGAMUXI("SUGAMUXI"),
    TUNDAMA("TUNDAMA"),

    // Sub-zonas de CENTRO con coordinadores distintos
    CENTRO_TIBANA("CENTRO / TIBANA"),
    CENTRO_MIRAFLORES("CENTRO / MIRAFLORES"),
    CENTRO_VILLA_DE_LEYVA("CENTRO / VILLA DE LEYVA"),
    CENTRO_SAMACA("CENTRO / SAMACA"),

    // Direcciones
    DIRECCION_MANTENIMIENTO("DIRECCION DE MANTENIMIENTO"),
    DIRECCION_OPERACION("DIRECCION DE OPERACION"),
    DIRECCION_PERDIDAS("DIRECCION DE PERDIDAS"),
    DIRECCION_TELEMATICA("DIRECCION DE TELEMATICA"),
    DIRECCION_AUTOMATIZACION("DIRECCION DE AUTOMATIZACION"),

    /*
     * Compatibilidad con datos ya guardados y pantallas antiguas.
     * Para registros nuevos debe preferirse PUERTO_BOYACA sobre PUERTO.
     */
    PUERTO("PUERTO BOYACA"),
    EDIFICIO("EDIFICIO");

    private final String displayName;

    AreaCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static AreaCode fromJson(String value) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            return null;
        }

        if ("PUERTO".equals(normalized) || "PUERTO_BOYACA".equals(normalized)) {
            return PUERTO_BOYACA;
        }

        for (AreaCode areaCode : values()) {
            if (areaCode.name().equals(normalized) || normalize(areaCode.displayName).equals(normalized)) {
                return areaCode;
            }
        }

        throw new IllegalArgumentException("Zona no valida: " + value);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(String.valueOf(value == null ? "" : value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
    }
}
