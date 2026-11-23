package com.crono.service;

import com.crono.entities.Profesor;
import com.crono.repository.ProfesorRepository;
import com.crono.repository.UniversidadRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regla "Registro dinámico de profesores". */
@Service
public class ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final UniversidadRepository universidadRepository;

    public ProfesorService(ProfesorRepository profesorRepository, UniversidadRepository universidadRepository) {
        this.profesorRepository = profesorRepository;
        this.universidadRepository = universidadRepository;
    }

    @Transactional(readOnly = true)
    public List<Profesor> listar() {
        return profesorRepository.findAll(Sort.by("id"));
    }

    @Transactional
    public Profesor crear(String id, String nombre, String correo) {
        Profesor profesor = new Profesor(
                id == null ? null : id.trim().toUpperCase(),
                nombre,
                correo == null ? null : correo.trim(),
                universidadRepository.findFirstByOrderByIdAsc().orElse(null));
        validar(profesor);
        if (profesorRepository.existsById(profesor.getId())) {
            throw new IllegalArgumentException("Ya existe un profesor con id " + profesor.getId());
        }
        return profesorRepository.save(profesor);
    }

    /** Lo usa también la carga del JSON. Lanza IllegalArgumentException con el motivo. */
    public void validar(Profesor profesor) {
        if (profesor.getId() == null || !profesor.getId().startsWith("P")) {
            throw new IllegalArgumentException("El id del profesor debe empezar con P: " + profesor.getId());
        }
        if (profesor.getNombre() == null || profesor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El profesor " + profesor.getId() + " no tiene nombre");
        }
        if (!esCorreoValido(profesor.getCorreo())) {
            throw new IllegalArgumentException("Correo no válido para el profesor " + profesor.getId() + ": " + profesor.getCorreo());
        }
    }

    /** algo@algo.algo, sin espacios. */
    private boolean esCorreoValido(String correo) {
        return correo != null && correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
