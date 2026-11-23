package com.crono.repository;

import com.crono.entities.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfesorRepository extends JpaRepository<Profesor, String> {
}
