package com.crono.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Edificio {

    @Id
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "universidad_id")
    private Universidad universidad;

    @ElementCollection
    @CollectionTable(name = "edificio_tiempos", joinColumns = @JoinColumn(name = "edificio_origen"))
    @MapKeyColumn(name = "edificio_destino")
    @Column(name = "minutos", nullable = false)
    private Map<String, Integer> tiempos = new HashMap<>();

    @OneToMany(mappedBy = "edificio")
    private List<Salon> salones = new ArrayList<>();

    public Edificio(String nombre, Universidad universidad) {
        this.nombre = nombre;
        this.universidad = universidad;
    }
}
