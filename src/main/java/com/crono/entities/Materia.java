package com.crono.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class Materia {

    @Id
    private Long codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer creditos;

    /** Todas las clases (grupos) de la materia. Se obtiene de clase.materia_codigo. */
    @OneToMany(mappedBy = "materia")
    private List<Clase> clases = new ArrayList<>();

    public Materia(Long codigo, String nombre, Integer creditos) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.creditos = creditos;
    }
}
