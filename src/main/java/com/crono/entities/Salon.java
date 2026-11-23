package com.crono.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Salon {

    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "edificio_nombre")
    private Edificio edificio;

    public Salon(Long id, Edificio edificio) {
        this.id = id;
        this.edificio = edificio;
    }
}
