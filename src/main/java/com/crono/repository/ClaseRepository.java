package com.crono.repository;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface ClaseRepository extends JpaRepository<Clase, Long> {

    List<Clase> findByMateria_Codigo(Long materiaCodigo);

    List<Clase> findBySalon_IdAndDia(Long salonId, Dia dia);

    List<Clase> findByProfesor_IdAndDia(String profesorId, Dia dia);

    Optional<Clase> findTopByOrderByIdDesc();
}
