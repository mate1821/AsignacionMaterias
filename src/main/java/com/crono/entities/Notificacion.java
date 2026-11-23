package com.crono.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CORRECCIÓN: en el diagrama la notificación era una columna de estudiante_inscripciones.
 * Eso no sirve para los rechazos: si la materia NO se pudo inscribir no existe fila de inscripción
 * donde guardar el mensaje. Ahora es una tabla propia, igual al bloque "notificaciones" del JSON de salida.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    /** Puede ser null si el mensaje es general (no de una materia). */
    @ManyToOne
    @JoinColumn(name = "materia_codigo")
    private Materia materia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(length = 500)
    private String sugerencia;

    public Notificacion(Materia materia, EstadoNotificacion estado, String motivo, String sugerencia) {
        this.materia = materia;
        this.estado = estado;
        this.motivo = motivo;
        this.sugerencia = sugerencia;
    }
}
