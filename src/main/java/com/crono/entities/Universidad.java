package com.crono.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Universidad {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    public Universidad(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}
