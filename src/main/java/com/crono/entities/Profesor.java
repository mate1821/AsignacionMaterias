package com.crono.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Profesor {

    /** Empieza con "P". */
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

    @OneToMany(mappedBy = "profesor")
    private List<Clase> cursos = new ArrayList<>();

    public Profesor(String id, String nombre, String correo, Universidad universidad) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.universidad = universidad;
    }
}
