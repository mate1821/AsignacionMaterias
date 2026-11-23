package com.crono.service;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
import com.crono.entities.EstadoNotificacion;
import com.crono.entities.Estudiante;
import com.crono.entities.Materia;
import com.crono.entities.Notificacion;
import com.crono.entities.PreferenciaHoraria;
import com.crono.entities.Profesor;
import com.crono.entities.Salon;
import com.crono.entities.Universidad;
import com.crono.repository.ClaseRepository;
import com.crono.repository.EstudianteRepository;
import com.crono.repository.MateriaRepository;
import com.crono.repository.ProfesorRepository;
import com.crono.repository.SalonRepository;
import com.crono.repository.UniversidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee el JSON de entrada y lo guarda en la base de datos.
 *
 * Sin clases DTO: Jackson convierte el JSON en Map / List / String / Number
 *   { "profesores": [ {...}, {...} ] }  ->  Map con la llave "profesores" -> List de Map
 * y aquí se pasa cada Map a su entidad.
 *
 * Reemplaza profesores, materias, clases y estudiantes (con sus inscripciones y notificaciones).
 * Universidad, edificios y salones NO se tocan: vienen del DataLoader.
 * Todo ocurre en una sola transacción: si algo del JSON está mal, no se guarda nada.
 *
 * Nota: Spring Boot 4 usa Jackson 3, por eso ObjectMapper viene del paquete tools.jackson.
 */
@Service
public class CargaJsonService {

    private final ObjectMapper objectMapper;
    private final UniversidadRepository universidadRepository;
    private final SalonRepository salonRepository;
    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final ClaseRepository claseRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorService profesorService;
    private final MateriaService materiaService;
    private final ClaseService claseService;
    private final EstudianteService estudianteService;

    public CargaJsonService(ObjectMapper objectMapper, UniversidadRepository universidadRepository,
                            SalonRepository salonRepository, ProfesorRepository profesorRepository,
                            MateriaRepository materiaRepository, ClaseRepository claseRepository,
                            EstudianteRepository estudianteRepository, ProfesorService profesorService,
                            MateriaService materiaService, ClaseService claseService,
                            EstudianteService estudianteService) {
        this.objectMapper = objectMapper;
        this.universidadRepository = universidadRepository;
        this.salonRepository = salonRepository;
        this.profesorRepository = profesorRepository;
        this.materiaRepository = materiaRepository;
        this.claseRepository = claseRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorService = profesorService;
        this.materiaService = materiaService;
        this.claseService = claseService;
        this.estudianteService = estudianteService;
    }

    /** Punto de entrada desde DatosController (archivo subido). Devuelve un resumen para mostrar. */
    @Transactional
    @SuppressWarnings("unchecked")
    public String importarArchivo(InputStream contenido) {
        Map<String, Object> datos;
        try {
            datos = objectMapper.readValue(contenido, Map.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("El archivo no es un JSON válido: " + e.getOriginalMessage());
        }

        borrarDatosAcademicos();
        Universidad universidad = universidadRepository.findFirstByOrderByIdAsc().orElse(null);

        // ---------- 1. Profesores: { "id": "P201", "nombre": "...", "correo": "...", "cursos": [501] } ----------
        Map<String, Profesor> profesores = new HashMap<>();
        Map<Long, String> profesorPorClase = new HashMap<>(); // sale de "cursos"
        for (Map<String, Object> p : objetos(datos, "profesores")) {
            String id = mayuscula(texto(p, "id"));
            Profesor profesor = new Profesor(id, texto(p, "nombre"), texto(p, "correo"), universidad);
            profesorService.validar(profesor);
            if (profesores.containsKey(id)) {
                throw new IllegalArgumentException("Profesor repetido en el JSON: " + id);
            }
            profesores.put(id, profesorRepository.save(profesor));
            for (Long claseId : numeros(p, "cursos")) {
                profesorPorClase.put(claseId, id);
            }
        }

        // ---------- 2. Materias: { "codigo": 301, "nombre": "...", "creditos": 3, "clases": [501, 502] } ----------
        Map<Long, Materia> materias = new HashMap<>();
        Map<Long, Materia> materiaPorClase = new HashMap<>(); // sale de "clases"
        for (Map<String, Object> m : objetos(datos, "materias")) {
            Long codigo = numero(m, "codigo");
            Materia materia = new Materia(codigo, texto(m, "nombre"), entero(m, "creditos"));
            materiaService.validar(materia);
            if (materias.containsKey(codigo)) {
                throw new IllegalArgumentException("Materia repetida en el JSON: " + codigo);
            }
            materia = materiaRepository.save(materia);
            materias.put(codigo, materia);
            for (Long claseId : numeros(m, "clases")) {
                if (materiaPorClase.containsKey(claseId)) {
                    throw new IllegalArgumentException("La clase " + claseId + " aparece en dos materias");
                }
                materiaPorClase.put(claseId, materia);
            }
        }

        // ---------- 3. Clases: { "id": 501, "cupos": 35, "estudiantes_inscritos": [...], "profesor": "P201",
        //                         "salon": 301, "dia": "lunes", "horaInicio": "06:00", "horaFin": "08:00" } ----------
        Map<Long, Clase> clases = new HashMap<>();
        for (Map<String, Object> c : objetos(datos, "clases")) {
            Long id = numero(c, "id");
            if (id == null) {
                throw new IllegalArgumentException("Hay una clase sin id en el JSON");
            }
            if (clases.containsKey(id)) {
                throw new IllegalArgumentException("Clase repetida en el JSON: " + id);
            }
            // La clase no trae su materia: se saca de materias[].clases
            Materia materia = materiaPorClase.get(id);
            if (materia == null) {
                throw new IllegalArgumentException("La clase " + id + " no está en la lista 'clases' de ninguna materia");
            }
            // El profesor puede venir en la clase ("profesor") o en el profesor ("cursos")
            String profesorId = texto(c, "profesor") != null ? mayuscula(texto(c, "profesor")) : profesorPorClase.get(id);
            Profesor profesor = profesores.get(profesorId);
            if (profesor == null) {
                throw new IllegalArgumentException("La clase " + id + " tiene un profesor que no existe: " + profesorId);
            }
            Long salonId = numero(c, "salon");
            if (salonId == null) {
                throw new IllegalArgumentException("La clase " + id + " no tiene salón");
            }
            Salon salon = salonRepository.findById(salonId)
                    .orElseThrow(() -> new IllegalArgumentException("La clase " + id + " usa un salón que no existe: " + salonId));

            Clase clase = new Clase(id, materia, profesor, salon, entero(c, "cupos"), Dia.desde(texto(c, "dia")),
                    claseService.parsearHora(texto(c, "horaInicio"), id),
                    claseService.parsearHora(texto(c, "horaFin"), id));
            claseService.validar(clase);
            claseService.validarSinCruces(clase);
            clases.put(id, claseRepository.save(clase));
        }

        // ---------- 4. Estudiantes (Fig. 5) ----------
        Map<String, Estudiante> estudiantes = new HashMap<>();
        for (Map<String, Object> e : objetos(datos, "estudiantes")) {
            Estudiante estudiante = new Estudiante(
                    mayuscula(texto(e, "id")),
                    texto(e, "nombre"),
                    texto(e, "correo"),
                    entero(e, "tiempoDesplazamientoMax"),
                    entero(e, "creditosDisponibles"),
                    PreferenciaHoraria.desde(texto(e, "preferenciaHoraria")),
                    entero(e, "maxClasesPorDia"));
            estudiante.setUniversidad(universidad);
            estudiante.setMaxHuecoMinutos(entero(e, "maxHuecoMinutos"));
            estudiante.setPreferidos(estudianteService.limpiarIds(textos(e, "preferidos")));
            estudiante.setNoPreferidos(estudianteService.limpiarIds(textos(e, "noPreferidos")));
            for (Long codigo : numeros(e, "materias")) {
                Materia materia = materias.get(codigo);
                if (materia == null) {
                    throw new IllegalArgumentException("El estudiante " + estudiante.getId() + " solicita una materia que no existe: " + codigo);
                }
                estudiante.getMateriasSolicitadas().add(materia);
            }
            for (String dia : textos(e, "diasNoDisponibles")) {
                estudiante.getDiasNoDisponibles().add(Dia.desde(dia));
            }
            estudianteService.validar(estudiante);
            if (estudiantes.containsKey(estudiante.getId())) {
                throw new IllegalArgumentException("Estudiante repetido en el JSON: " + estudiante.getId());
            }

            // save() devuelve la instancia administrada por JPA: desde aquí se trabaja con esa
            estudiante = estudianteRepository.save(estudiante);
            estudiantes.put(estudiante.getId(), estudiante);

            // "horario": { "clases": [...], "notificacion": "..." } (si el JSON ya trae un horario previo)
            if (e.get("horario") instanceof Map<?, ?> horario) {
                Map<String, Object> h = (Map<String, Object>) horario;
                for (Long claseId : numeros(h, "clases")) {
                    Clase clase = clases.get(claseId);
                    if (clase == null) {
                        throw new IllegalArgumentException("El horario del estudiante " + estudiante.getId()
                                + " tiene una clase que no existe: " + claseId);
                    }
                    estudiante.inscribir(clase);
                }
                String notificacion = texto(h, "notificacion");
                if (notificacion != null && !notificacion.isBlank()) {
                    estudiante.agregarNotificacion(new Notificacion(null, EstadoNotificacion.INFO, notificacion, null));
                }
            }
        }

        // "estudiantes_inscritos" de cada clase (misma información que horario.clases, vista desde la clase)
        for (Map<String, Object> c : objetos(datos, "clases")) {
            for (String estudianteId : textos(c, "estudiantes_inscritos")) {
                Estudiante estudiante = estudiantes.get(mayuscula(estudianteId));
                if (estudiante == null) {
                    throw new IllegalArgumentException("La clase " + numero(c, "id")
                            + " tiene inscrito un estudiante que no existe: " + estudianteId);
                }
                estudiante.inscribir(clases.get(numero(c, "id")));
            }
        }

        return "Se cargaron " + profesores.size() + " profesores, " + materias.size() + " materias, "
                + clases.size() + " clases y " + estudiantes.size() + " estudiantes.";
    }

    /** Borra los datos académicos (no la infraestructura). El orden importa por las llaves foráneas. */
    @Transactional
    public void borrarDatosAcademicos() {
        estudianteRepository.deleteAll();   // también borra inscripciones, preferencias y notificaciones
        claseRepository.deleteAll();
        materiaRepository.deleteAll();
        profesorRepository.deleteAll();
        // flush: ejecuta los DELETE ya, antes de insertar registros con los mismos ids
        estudianteRepository.flush();
    }

    // ------------------------------------------------------------------
    // Ayudas para leer el Map que produce Jackson.
    // Un número del JSON llega como Integer o Long, un texto como String, un arreglo como List.
    // ------------------------------------------------------------------

    /** "profesores": [ {...}, {...} ]  ->  lista de Map. Si la llave no está, lista vacía. */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> objetos(Map<String, Object> mapa, String llave) {
        Object valor = mapa.get(llave);
        if (valor == null) {
            return List.of();
        }
        if (!(valor instanceof List<?> lista)) {
            throw new IllegalArgumentException("'" + llave + "' debe ser un arreglo [ ... ]");
        }
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object elemento : lista) {
            if (!(elemento instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Cada elemento de '" + llave + "' debe ser un objeto { ... }");
            }
            resultado.add((Map<String, Object>) elemento);
        }
        return resultado;
    }

    private String texto(Map<String, Object> mapa, String llave) {
        Object valor = mapa.get(llave);
        return valor == null ? null : valor.toString();
    }

    private Long numero(Map<String, Object> mapa, String llave) {
        Object valor = mapa.get(llave);
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(valor.toString().trim()); // por si viene como "301"
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + llave + "' debe ser un número: " + valor);
        }
    }

    private Integer entero(Map<String, Object> mapa, String llave) {
        Long valor = numero(mapa, llave);
        return valor == null ? null : valor.intValue();
    }

    /** [501, 502] -> [501L, 502L]. Si la llave no está, lista vacía. */
    private List<Long> numeros(Map<String, Object> mapa, String llave) {
        List<Long> resultado = new ArrayList<>();
        if (mapa.get(llave) instanceof List<?> lista) {
            for (Object elemento : lista) {
                resultado.add(elemento instanceof Number n ? n.longValue() : Long.parseLong(elemento.toString().trim()));
            }
        }
        return resultado;
    }

    /** ["E1002", "P202"] -> lista de String. Si la llave no está, lista vacía. */
    private List<String> textos(Map<String, Object> mapa, String llave) {
        List<String> resultado = new ArrayList<>();
        if (mapa.get(llave) instanceof List<?> lista) {
            for (Object elemento : lista) {
                if (elemento != null) {
                    resultado.add(elemento.toString());
                }
            }
        }
        return resultado;
    }

    private String mayuscula(String texto) {
        return texto == null ? null : texto.trim().toUpperCase();
    }
}
