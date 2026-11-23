package com.crono.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;


@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_clase_salon_horario", columnNames = {"salon_id", "dia", "hora_inicio"}),
        @UniqueConstraint(name = "uk_clase_profesor_horario", columnNames = {"profesor_id", "dia", "hora_inicio"})
})
@Getter
@Setter
@NoArgsConstructor
public class Clase {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_codigo")
    private Materia materia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    /** Cupo máximo de estudiantes. */
    @Column(nullable = false)
    private Integer cupos;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia", nullable = false, length = 20)
    private Dia dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Lado inverso de Estudiante.clasesInscritas (tabla estudiante_inscripciones). */
    @ManyToMany(mappedBy = "clasesInscritas")
    private Set<Estudiante> estudiantesInscritos = new LinkedHashSet<>();

    public Clase(Long id, Materia materia, Profesor profesor, Salon salon, Integer cupos,
                Dia dia, LocalTime horaInicio, LocalTime horaFin) {
        this.id = id;
        this.materia = materia;
        this.profesor = profesor;
        this.salon = salon;
        this.cupos = cupos;
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public boolean seCruzaCon(Clase otra) {
        return this.dia == otra.dia
                && this.horaInicio.isBefore(otra.horaFin)
                && otra.horaInicio.isBefore(this.horaFin);
    }

    /** "Lunes 06:00 - 08:00" (formato del JSON de salida). */
    public String getHorarioTexto() {
        return dia.getNombre() + " " + horaInicio.format(HORA) + " - " + horaFin.format(HORA);
    }
}
