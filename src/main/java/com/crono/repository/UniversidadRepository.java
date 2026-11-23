package com.crono.repository;

import com.crono.entities.Universidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversidadRepository extends JpaRepository<Universidad, Integer> {

    /** El sistema maneja una sola universidad: la primera que exista. */
    Optional<Universidad> findFirstByOrderByIdAsc();
}
