package com.crono.service;

import com.crono.entities.Estudiante;
import com.crono.repository.EstudianteRepository;
import com.crono.repository.UniversidadRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UniversidadRepository universidadRepository;
    private final MateriaService materiaService;

    public EstudianteService(EstudianteRepository estudianteRepository, UniversidadRepository universidadRepository,
                             MateriaService materiaService) {
        this.estudianteRepository = estudianteRepository;
        this.universidadRepository = universidadRepository;
        this.materiaService = materiaService;
    }

    @Transactional(readOnly = true)
    public List<Estudiante> listar() {
        return estudianteRepository.findAll(Sort.by("id"));
    }

    /**
     * Formulario "Ingreso manual". Spring ya llenó el objeto Estudiante con los campos del formulario
     * (nombre, correo, preferidos, díasNoDisponibles...). Aquí se limpian los datos, se le agregan
     * las materias elegidas y se guarda.
     */
    @Transactional
    public Estudiante crear(Estudiante estudiante, List<Long> codigosMaterias) {
        estudiante.setId(estudiante.getId() == null ? null : estudiante.getId().trim().toUpperCase());
        estudiante.setCorreo(estudiante.getCorreo() == null ? null : estudiante.getCorreo().trim());
        estudiante.setUniversidad(universidadRepository.findFirstByOrderByIdAsc().orElse(null));
        estudiante.setPreferidos(limpiarIds(estudiante.getPreferidos()));
        estudiante.setNoPreferidos(limpiarIds(estudiante.getNoPreferidos()));

        if (codigosMaterias != null) {
            for (Long codigo : codigosMaterias) {
                estudiante.getMateriasSolicitadas().add(materiaService.buscar(codigo));
            }
        }

        validar(estudiante);
        if (estudianteRepository.existsById(estudiante.getId())) {
            throw new IllegalArgumentException("Ya existe un estudiante con id " + estudiante.getId());
        }
        return estudianteRepository.save(estudiante);
    }

    /** Lo usa también la carga del JSON. */
    public void validar(Estudiante e) {
        if (e.getId() == null || !e.getId().startsWith("E")) {
            throw new IllegalArgumentException("El id del estudiante debe empezar con E: " + e.getId());
        }
        if (e.getNombre() == null || e.getNombre().isBlank()) {
            throw new IllegalArgumentException("El estudiante " + e.getId() + " no tiene nombre");
        }
        if (!esCorreoValido(e.getCorreo())) {
            throw new IllegalArgumentException("Correo no válido para el estudiante " + e.getId() + ": " + e.getCorreo());
        }
        noNegativo(e.getCreditosDisponibles(), "créditos disponibles", e.getId());
        noNegativo(e.getTiempoDesplazamientoMax(), "tiempo de desplazamiento máximo", e.getId());
        noNegativo(e.getMaxClasesPorDia(), "máximo de clases por día", e.getId());
        noNegativo(e.getMaxHuecoMinutos(), "máximo hueco en minutos", e.getId());

        // preferidos / noPreferidos: solo ids E... o P..., no él mismo, y no la misma persona en ambas listas
        validarIdsPersona(e.getPreferidos(), e.getId());
        validarIdsPersona(e.getNoPreferidos(), e.getId());
        for (String id : e.getPreferidos()) {
            if (e.getNoPreferidos().contains(id)) {
                throw new IllegalArgumentException("El estudiante " + e.getId() + " tiene a " + id
                        + " como preferido y como no preferido a la vez");
            }
        }
    }

    /** Quita espacios y vacíos, y pasa a mayúscula: [" p201", ""] -> ["P201"]. */
    public Set<String> limpiarIds(Collection<String> ids) {
        Set<String> limpios = new LinkedHashSet<>();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    limpios.add(id.trim().toUpperCase());
                }
            }
        }
        return limpios;
    }

    private void validarIdsPersona(Set<String> ids, String propioId) {
        for (String id : ids) {
            if (!id.startsWith("E") && !id.startsWith("P")) {
                throw new IllegalArgumentException("Id de persona no válido en preferencias de " + propioId
                        + ": " + id + " (debe empezar con E o P)");
            }
            if (id.equals(propioId)) {
                throw new IllegalArgumentException("El estudiante " + propioId + " no puede incluirse a sí mismo en sus preferencias");
            }
        }
    }

    private void noNegativo(Integer valor, String campo, String estudianteId) {
        if (valor != null && valor < 0) {
            throw new IllegalArgumentException("El campo " + campo + " del estudiante " + estudianteId + " no puede ser negativo");
        }
    }

    /** algo@algo.algo, sin espacios. */
    private boolean esCorreoValido(String correo) {
        return correo != null && correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
