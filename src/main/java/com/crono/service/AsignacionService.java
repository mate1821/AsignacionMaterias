package com.crono.service;

import com.crono.entities.Clase;
import com.crono.entities.Edificio;
import com.crono.entities.EstadoNotificacion;
import com.crono.entities.Estudiante;
import com.crono.entities.Materia;
import com.crono.entities.Notificacion;
import com.crono.repository.ClaseRepository;
import com.crono.repository.EstudianteRepository;
import com.crono.repository.MateriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio del ALGORITMO de asignación.
 *
 * AsignacionController (POST /asignacion/ejecutar)
 *   -> ejecutar()   carga los datos y borra la asignación anterior
 *     -> asignar(...)   <-- HUECO: aquí va el algoritmo
 *
 * Todo corre en una transacción: los cambios que el algoritmo haga sobre las entidades
 * (estudiante.inscribir(clase), estudiante.agregarNotificacion(...)) se guardan solos al terminar.
 * Si el algoritmo lanza una excepción, se revierte todo y no queda nada a medias.
 */
@Service
public class AsignacionService {

    private final EstudianteRepository estudianteRepository;
    private final MateriaRepository materiaRepository;
    private final ClaseRepository claseRepository;

    public AsignacionService(EstudianteRepository estudianteRepository, MateriaRepository materiaRepository,
                             ClaseRepository claseRepository) {
        this.estudianteRepository = estudianteRepository;
        this.materiaRepository = materiaRepository;
        this.claseRepository = claseRepository;
    }

    /** Devuelve un resumen para mostrar en pantalla. */
    @Transactional
    public String ejecutar() {
        List<Estudiante> estudiantes = estudianteRepository.findAll(Sort.by("id"));
        List<Materia> materias = materiaRepository.findAll(Sort.by("codigo"));
        List<Clase> clases = claseRepository.findAll(Sort.by("id"));

        limpiarAsignacionAnterior(estudiantes, clases);

        long inicio = System.currentTimeMillis();
        asignar(estudiantes, materias, clases);
        long milisegundos = System.currentTimeMillis() - inicio;

        int inscripciones = 0;
        for (Estudiante estudiante : estudiantes) {
            inscripciones += estudiante.getClasesInscritas().size();
        }
        return "Asignación terminada en " + milisegundos + " ms: " + inscripciones
                + " inscripciones para " + estudiantes.size() + " estudiantes.";
    }

    /**
     * ============================================================================
     *  HUECO PARA EL ALGORITMO
     * ============================================================================
     *
     * Para inscribir:       estudiante.inscribir(clase)
     * Para notificar:       estudiante.agregarNotificacion(new Notificacion(materia, EstadoNotificacion.RECHAZADO,
     *                                                                       "motivo", "sugerencia"))
     *
     * Ayudas que ya existen:
     *   clase.getMateria().getClases()        alternativas (grupos) de una materia
     *   clase.seCruzaCon(otra)                 no solapamiento
     *   cuposRestantes(clase)                  regla de cupos
     *   minutosEntre(claseA, claseB)           distancia entre edificios
     *   estudiante.getPreferenciaHoraria().contiene(clase.getHoraInicio(), clase.getHoraFin())
     *
     * Reglas del documento:
     *   - Solo materias solicitadas (estudiante.getMateriasSolicitadas())
     *   - Cupos de clase
     *   - No solapamiento
     *   - Créditos disponibles (estudiante.getCreditosDisponibles(), materia.getCreditos())
     *   - Preferencia de franja horaria (+1 por clase en su jornada)
     *   - Días no disponibles (estudiante.getDiasNoDisponibles())
     *   - Carga máxima diaria (estudiante.getMaxClasesPorDia())
     *   - Preferencia / no preferencia de profesores (ids que empiezan con P en preferidos / noPreferidos)
     *   - Preferencia / no preferencia de compañeros (ids que empiezan con E)
     *   - Distancia entre edificios (minutosEntre vs estudiante.getTiempoDesplazamientoMax())
     *   - Notificación + alternativa sugerida cuando una regla no se cumple
     */
    private void asignar(List<Estudiante> estudiantes, List<Materia> materias, List<Clase> clases) {

        // TODO: implementar el algoritmo de asignación aquí.

        // Mientras tanto solo se deja un aviso por estudiante, para poder probar el flujo completo
        // (botón -> servicio -> base de datos -> Reportes / JSON de salida).
        for (Estudiante estudiante : estudiantes) {
            estudiante.agregarNotificacion(new Notificacion(null, EstadoNotificacion.INFO,
                    "El algoritmo de asignación aún no está implementado.", null));
        }
    }

    // ------------------------------------------------------------------ ayudas para el algoritmo

    /** Cupos que le quedan a la clase (cuenta a los inscritos con estudiante.inscribir en esta ejecución). */
    private int cuposRestantes(Clase clase) {
        return clase.getCupos() - clase.getEstudiantesInscritos().size();
    }

    /**
     * Minutos caminando entre el edificio de una clase y el de otra.
     * 0 si es el mismo edificio; Integer.MAX_VALUE si no hay tiempo registrado entre esos edificios.
     */
    private int minutosEntre(Clase origen, Clase destino) {
        Edificio edificioOrigen = origen.getSalon().getEdificio();
        Edificio edificioDestino = destino.getSalon().getEdificio();
        if (edificioOrigen.getNombre().equals(edificioDestino.getNombre())) {
            return 0;
        }
        Integer minutos = edificioOrigen.getTiempos().get(edificioDestino.getNombre());
        if (minutos == null) { // por si solo se registró en un sentido
            minutos = edificioDestino.getTiempos().get(edificioOrigen.getNombre());
        }
        return minutos != null ? minutos : Integer.MAX_VALUE;
    }

    /** Borra inscripciones y notificaciones de la ejecución anterior. */
    private void limpiarAsignacionAnterior(List<Estudiante> estudiantes, List<Clase> clases) {
        for (Estudiante estudiante : estudiantes) {
            estudiante.getClasesInscritas().clear();
            estudiante.getNotificaciones().clear(); // orphanRemoval las borra de la BD
        }
        for (Clase clase : clases) {
            clase.getEstudiantesInscritos().clear(); // lado inverso: para que cuposRestantes() empiece en cero
        }
    }
}
