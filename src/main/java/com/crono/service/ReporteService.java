package com.crono.service;

import com.crono.entities.Clase;
import com.crono.entities.Estudiante;
import com.crono.entities.Notificacion;
import com.crono.repository.EstudianteRepository;
import com.crono.repository.NotificacionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sección "Reportes": JSON de salida general y notificaciones por estudiante. */
@Service
public class ReporteService {

    private final EstudianteRepository estudianteRepository;
    private final NotificacionRepository notificacionRepository;
    private final HorarioService horarioService;

    public ReporteService(EstudianteRepository estudianteRepository, NotificacionRepository notificacionRepository,
                          HorarioService horarioService) {
        this.estudianteRepository = estudianteRepository;
        this.notificacionRepository = notificacionRepository;
        this.horarioService = horarioService;
    }

    /**
     * Arma el JSON de salida (Fig. 6) con lo que está guardado en la base de datos.
     * Sin DTOs: se construye con LinkedHashMap (mantiene el orden de las llaves) y Spring lo convierte a JSON.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generarSalida() {
        List<Map<String, Object>> asignaciones = new ArrayList<>();
        List<Map<String, Object>> notificaciones = new ArrayList<>();

        for (Estudiante estudiante : estudianteRepository.findAll(Sort.by("id"))) {
            List<Map<String, Object>> clasesInscritas = new ArrayList<>();
            for (Clase clase : horarioService.clasesOrdenadas(estudiante)) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("clase_id", clase.getId());
                c.put("materia", clase.getMateria().getNombre());
                c.put("profesor", clase.getProfesor().getNombre());
                c.put("horario", clase.getHorarioTexto());
                c.put("ubicacion", HorarioService.ubicacion(clase));
                clasesInscritas.add(c);
            }

            Map<String, Object> asignacion = new LinkedHashMap<>();
            asignacion.put("estudiante_id", estudiante.getId());
            asignacion.put("estudiante_nombre", estudiante.getNombre());
            asignacion.put("clases_inscritas", clasesInscritas);
            asignaciones.add(asignacion);

            for (Notificacion n : estudiante.getNotificaciones()) {
                Map<String, Object> notificacion = new LinkedHashMap<>();
                notificacion.put("estudiante_id", estudiante.getId());
                notificacion.put("materia_codigo", n.getMateria() == null ? null : n.getMateria().getCodigo());
                notificacion.put("materia_nombre", n.getMateria() == null ? null : n.getMateria().getNombre());
                notificacion.put("estado", n.getEstado().getNombre());
                notificacion.put("motivo", n.getMotivo());
                notificacion.put("sugerencia", n.getSugerencia());
                notificaciones.add(notificacion);
            }
        }

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("asignaciones", asignaciones);
        salida.put("notificaciones", notificaciones);
        return salida;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> notificacionesDe(String estudianteId) {
        return notificacionRepository.findByEstudiante_IdOrderByIdAsc(estudianteId);
    }
}
