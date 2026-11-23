package com.crono.entities;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.Locale;

public enum PreferenciaHoraria {
    MANANA("Mañana", LocalTime.of(6, 0), LocalTime.of(12, 0)),
    TARDE("Tarde", LocalTime.of(12, 0), LocalTime.of(18, 0));

    private final String nombre;
    private final LocalTime desde;
    private final LocalTime hasta;

    PreferenciaHoraria(String nombre, LocalTime desde, LocalTime hasta) {
        this.nombre = nombre;
        this.desde = desde;
        this.hasta = hasta;
    }

    public String getNombre() {
        return nombre;
    }

    /** true si la clase [inicio, fin] cae completa dentro de esta jornada. Útil para sumar el punto de preferencia. */
    public boolean contiene(LocalTime inicio, LocalTime fin) {
        return !inicio.isBefore(desde) && !fin.isAfter(hasta);
    }

    /** Acepta "Mañana", "manana", "Tarde". Vacío o null = sin preferencia. */
    public static PreferenciaHoraria desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        // "Mañana" -> "MANANA": quita tildes (la ñ queda como n) y pasa a mayúscula
        String limpio = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        try {
            return PreferenciaHoraria.valueOf(limpio);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Preferencia horaria no válida: '" + texto + "' (Mañana o Tarde)");
        }
    }
}
