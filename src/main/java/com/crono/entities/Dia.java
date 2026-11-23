package com.crono.entities;

import java.text.Normalizer;
import java.util.Locale;

/** Días hábiles en los que se dictan clases (lunes a viernes). Se guarda como texto: "LUNES". */
public enum Dia {
    LUNES("Lunes"),
    MARTES("Martes"),
    MIERCOLES("Miércoles"),
    JUEVES("Jueves"),
    VIERNES("Viernes");

    private final String nombre;

    Dia(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /** Acepta "lunes", "Lunes", "miércoles", "MIERCOLES"... */
    public static Dia desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El día es obligatorio");
        }
        String limpio = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        try {
            return Dia.valueOf(limpio);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Día no válido: '" + texto + "' (solo de lunes a viernes)");
        }
    }
}
