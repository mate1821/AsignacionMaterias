package com.crono.service;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
import com.crono.entities.Materia;
import com.crono.entities.Profesor;
import com.crono.entities.Salon;
import com.crono.repository.ClaseRepository;
import com.crono.repository.ProfesorRepository;
import com.crono.repository.SalonRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Regla "Creación dinámica de clases por materia". */
@Service
public class ClaseService {

    /** Las clases van de 6:00 a 18:00 en bloques de 2 horas. */
    public static final LocalTime HORA_MINIMA = LocalTime.of(6, 0);
    public static final LocalTime HORA_MAXIMA = LocalTime.of(18, 0);
    public static final int DURACION_BLOQUE_HORAS = 2;
    public static final List<String> INICIOS_DE_BLOQUE = List.of("06:00", "08:00", "10:00", "12:00", "14:00", "16:00");

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");

    private final ClaseRepository claseRepository;
    private final MateriaService materiaService;
    private final ProfesorRepository profesorRepository;
    private final SalonRepository salonRepository;

    public ClaseService(ClaseRepository claseRepository, MateriaService materiaService,
                        ProfesorRepository profesorRepository, SalonRepository salonRepository) {
        this.claseRepository = claseRepository;
        this.materiaService = materiaService;
        this.profesorRepository = profesorRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public List<Clase> listar() {
        return claseRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Long siguienteId() {
        return claseRepository.findTopByOrderByIdDesc().map(c -> c.getId() + 1).orElse(501L);
    }

    /** Crea una clase desde el formulario. Si id es null se usa el siguiente disponible. */
    @Transactional
    public Clase crear(Long id, Long materiaCodigo, String profesorId, Long salonId,
                       Integer cupos, String dia, String horaInicio) {
        Long idFinal = (id != null) ? id : siguienteId();
        if (claseRepository.existsById(idFinal)) {
            throw new IllegalArgumentException("Ya existe una clase con id " + idFinal);
        }
        Materia materia = materiaService.buscar(materiaCodigo);
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el profesor " + profesorId));
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el salón " + salonId));
        LocalTime inicio = parsearHora(horaInicio, idFinal);

        Clase clase = new Clase(idFinal, materia, profesor, salon, cupos, Dia.desde(dia),
                inicio, inicio.plusHours(DURACION_BLOQUE_HORAS));
        validar(clase);
        validarSinCruces(clase);
        return claseRepository.save(clase);
    }

    /** Validaciones propias de la clase (sin consultar otras clases). */
    public void validar(Clase clase) {
        if (clase.getCupos() == null || clase.getCupos() <= 0) {
            throw new IllegalArgumentException("La clase " + clase.getId() + " debe tener cupos mayores a 0");
        }
        if (!clase.getHoraFin().isAfter(clase.getHoraInicio())) {
            throw new IllegalArgumentException("En la clase " + clase.getId() + " la hora fin debe ser mayor a la hora inicio");
        }
        if (clase.getHoraInicio().isBefore(HORA_MINIMA) || clase.getHoraFin().isAfter(HORA_MAXIMA)) {
            throw new IllegalArgumentException("La clase " + clase.getId() + " debe estar entre las 6:00 y las 18:00");
        }
    }

    /** Un salón y un profesor no pueden tener dos clases que se crucen. */
    public void validarSinCruces(Clase clase) {
        for (Clase otra : claseRepository.findBySalon_IdAndDia(clase.getSalon().getId(), clase.getDia())) {
            if (!otra.getId().equals(clase.getId()) && clase.seCruzaCon(otra)) {
                throw new IllegalArgumentException("La clase " + clase.getId() + " se cruza con la clase "
                        + otra.getId() + " en el salón " + clase.getSalon().getId());
            }
        }
        for (Clase otra : claseRepository.findByProfesor_IdAndDia(clase.getProfesor().getId(), clase.getDia())) {
            if (!otra.getId().equals(clase.getId()) && clase.seCruzaCon(otra)) {
                throw new IllegalArgumentException("El profesor " + clase.getProfesor().getId()
                        + " ya dicta la clase " + otra.getId() + " a esa hora");
            }
        }
    }

    /** Acepta "06:00" y "6:00". */
    public LocalTime parsearHora(String texto, Long claseId) {
        try {
            return LocalTime.parse(texto.trim(), FORMATO_HORA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Hora no válida en la clase " + claseId + ": '" + texto + "' (formato HH:mm)");
        }
    }
}
