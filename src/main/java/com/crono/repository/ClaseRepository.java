package com.crono.repository;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaseRepository extends JpaRepository<Clase, Long> {

    List<Clase> findByMateria_CodigoOrderByIdAsc(Long materiaCodigo);

    /** Clases de un salón en un día: para validar que no se crucen. */
    List<Clase> findBySalon_IdAndDia(Long salonId, Dia dia);

    /** Clases de un profesor en un día: para validar que no se crucen. */
    List<Clase> findByProfesor_IdAndDia(String profesorId, Dia dia);

    /** La de mayor id, para proponer el siguiente id al crear una clase manualmente. */
    Optional<Clase> findTopByOrderByIdDesc();
}
