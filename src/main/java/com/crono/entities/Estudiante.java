package com.crono.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@Entity
@Getter
@Setter
@NoArgsConstructor
public class Estudiante {

    /** Empieza con "E". */
    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String correo;

    @ManyToOne
    @JoinColumn(name = "universidad_id")
    private Universidad universidad;

    private Integer tiempoDesplazamientoMax;

    private Integer creditosDisponibles;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PreferenciaHoraria preferenciaHoraria;

    private Integer maxClasesPorDia;

    private Integer maxHuecoMinutos;

    @ElementCollection
    @CollectionTable(name = "estudiante_preferidos", joinColumns = @JoinColumn(name = "estudiante_id"))
    @Column(name = "persona_preferida_id", length = 50)
    private Set<String> preferidos = new LinkedHashSet<>();

    /** IDs de personas (E... / P...) que quiere evitar. */
    @ElementCollection
    @CollectionTable(name = "estudiante_no_preferidos", joinColumns = @JoinColumn(name = "estudiante_id"))
    @Column(name = "persona_no_preferida_id", length = 50)
    private Set<String> noPreferidos = new LinkedHashSet<>();

    /** CORRECCIÓN: lo pide la regla "Días no disponibles" y no estaba en la BD. */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "estudiante_dias_no_disponibles", joinColumns = @JoinColumn(name = "estudiante_id"))
    @Column(name = "dia", length = 20)
    private Set<Dia> diasNoDisponibles = new LinkedHashSet<>();

    /** Materias que el estudiante solicita cursar. */
    @ManyToMany
    @JoinTable(name = "estudiante_materias_solicitadas",
            joinColumns = @JoinColumn(name = "estudiante_id"),
            inverseJoinColumns = @JoinColumn(name = "materia_codigo"))
    private Set<Materia> materiasSolicitadas = new LinkedHashSet<>();

    /** Clases en las que quedó inscrito (resultado del algoritmo). */
    @ManyToMany
    @JoinTable(name = "estudiante_inscripciones",
            joinColumns = @JoinColumn(name = "estudiante_id"),
            inverseJoinColumns = @JoinColumn(name = "clase_id"))
    private Set<Clase> clasesInscritas = new LinkedHashSet<>();

    /** Mensajes del resultado de la asignación. */
    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Notificacion> notificaciones = new ArrayList<>();

    /** Constructor con los datos simples (lo usa el DataLoader); las relaciones se asignan después. */
    public Estudiante(String id, String nombre, String correo, Integer tiempoDesplazamientoMax,
                      Integer creditosDisponibles, PreferenciaHoraria preferenciaHoraria, Integer maxClasesPorDia) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.tiempoDesplazamientoMax = tiempoDesplazamientoMax;
        this.creditosDisponibles = creditosDisponibles;
        this.preferenciaHoraria = preferenciaHoraria;
        this.maxClasesPorDia = maxClasesPorDia;
    }

    /** Inscribe manteniendo sincronizados los dos lados de la relación. */
    public void inscribir(Clase clase) {
        clasesInscritas.add(clase);
        clase.getEstudiantesInscritos().add(this);
    }

    public void agregarNotificacion(Notificacion notificacion) {
        notificacion.setEstudiante(this);
        notificaciones.add(notificacion);
    }
}
