package com.crono.service;

import com.crono.entities.Clase;
import com.crono.entities.Estudiante;
import com.crono.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Sección "Horario": las clases en las que quedó inscrito un estudiante. */
@Service
public class HorarioService {

    public static final Comparator<Clase> POR_DIA_Y_HORA =
            Comparator.comparing(Clase::getDia).thenComparing(Clase::getHoraInicio);

    private final EstudianteRepository estudianteRepository;

    public HorarioService(EstudianteRepository estudianteRepository) {
        this.estudianteRepository = estudianteRepository;
    }

    @Transactional(readOnly = true)
    public List<Clase> horarioDe(String estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el estudiante " + estudianteId));
        return clasesOrdenadas(estudiante);
    }

    /** Clases inscritas ordenadas por día y hora. Debe llamarse dentro de una transacción (relación LAZY). */
    public List<Clase> clasesOrdenadas(Estudiante estudiante) {
        return estudiante.getClasesInscritas().stream()
                .sorted(POR_DIA_Y_HORA)
                .toList();
    }

    /** "Edificio Maldonado - S101" (formato del JSON de salida). */
    public static String ubicacion(Clase clase) {
        return "Edificio " + clase.getSalon().getEdificio().getNombre() + " - S" + clase.getSalon().getId();
    }
}
