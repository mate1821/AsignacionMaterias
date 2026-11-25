package com.crono.service;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio del ALGORITMO de asignación.
 *
 * AsignacionController (POST /asignacion/ejecutar)
 * -> ejecutar() carga los datos y borra la asignación anterior
 * -> asignar(...) dos voraces + mejora local, un estudiante a la vez
 *
 * Todo corre en una transacción: los cambios que el algoritmo haga sobre las
 * entidades
 * (estudiante.inscribir(clase), estudiante.agregarNotificacion(...)) se guardan
 * solos al terminar.
 * Si el algoritmo lanza una excepción, se revierte todo y no queda nada a
 * medias.
 */
@Service
public class AsignacionService {

    private static final int MINUTOS_POR_DIA = 24 * 60;
    private static final int DIAS = Dia.values().length;

    private final EstudianteRepository estudianteRepository;
    private final MateriaRepository materiaRepository;
    private final ClaseRepository claseRepository;

    public AsignacionService(EstudianteRepository estudianteRepository, MateriaRepository materiaRepository,
            ClaseRepository claseRepository) {
        this.estudianteRepository = estudianteRepository;
        this.materiaRepository = materiaRepository;
        this.claseRepository = claseRepository;
    }

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
     * Dos algoritmos voraces + mejora local, un estudiante a la vez.
     *
     * Reglas obligatorias (si una clase no las cumple, se descarta):
     * - Solo materias solicitadas.
     * - Cupos: x(e, c) = 1 si queda inscrito, 0 si no. Criterio: se atiende primero
     * a
     * los estudiantes con menos opciones (empate: id); el que se atiende primero
     * gana el cupo.
     * - Días no disponibles.
     * - Compañeros no preferidos (E...): no puede quedar en la misma clase que uno
     * de su lista, ni con alguien
     * que lo tenga a él en la suya.
     * - No solapamiento.
     * - Créditos: los disponibles no quedan negativos.
     * - Carga máxima diaria (maxClasesPorDia).
     * - Distancia entre edificios: entre dos clases seguidas del mismo día, el
     * tiempo de desplazamiento
     * no puede superar tiempoDesplazamientoMax.
     *
     * Criterios para comparar horarios, en este orden:
     * 1) más materias inscritas;
     * 2) más puntos: +1 por clase en su jornada, +1 por profesor preferido (P...),
     * +1 por cada compañero
     * preferido (E...) que ya esté en la clase.
     * Un profesor de la lista de no preferidos da 0;
     * 3) más créditos.
     *
     * Complejidad por estudiante: cada voraz es O(n · k · N · n log n), n =
     * materias solicitadas,
     * k = opciones por materia, N = opciones en total; la mejora local lo repite
     * unas n veces por vuelta.
     * Es polinomial: no garantiza el óptimo, pero siempre cumple las reglas.
     */
    private void asignar(List<Estudiante> estudiantes, List<Materia> materias, List<Clase> clases) {
        Map<Long, List<Bloque>> bloquesPorMateria = agruparPorMateria(materias, clases);

        // Criterio de cupos: primero los estudiantes con MENOS opciones (los que más
        // fácil se quedan sin materias
        // si llegan tarde). Empate: por id. Se ordena una copia para no cambiar el
        // orden de la lista original.
        Map<Estudiante, Integer> opciones = new HashMap<>();
        for (Estudiante estudiante : estudiantes) {
            opciones.put(estudiante, contarOpciones(estudiante, bloquesPorMateria));
        }
        List<Estudiante> orden = new ArrayList<>(estudiantes);
        orden.sort(Comparator.comparing((Estudiante e) -> opciones.get(e)).thenComparing(Estudiante::getId));

        for (Estudiante estudiante : orden) {
            asignarEstudiante(estudiante, bloquesPorMateria);
        }
    }

    /**
     * Clases de sus materias solicitadas que caen en días en los que sí puede ir.
     */
    private int contarOpciones(Estudiante estudiante, Map<Long, List<Bloque>> bloquesPorMateria) {
        int total = 0;
        for (Materia materia : estudiante.getMateriasSolicitadas()) {
            for (Bloque bloque : bloquesPorMateria.getOrDefault(materia.getCodigo(), List.of())) {
                if (!estudiante.getDiasNoDisponibles().contains(bloque.dia)) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Paso 1: se recorren las clases una sola vez y quedan agrupadas por materia
     * (código -> bloques).
     */
    private Map<Long, List<Bloque>> agruparPorMateria(List<Materia> materias, List<Clase> clases) {
        Map<Long, List<Bloque>> bloquesPorMateria = new HashMap<>();
        for (Materia materia : materias) {
            bloquesPorMateria.put(materia.getCodigo(), new ArrayList<>());
        }
        for (Clase clase : clases) {
            bloquesPorMateria.computeIfAbsent(clase.getMateria().getCodigo(), codigo -> new ArrayList<>())
                    .add(new Bloque(clase, cuposRestantes(clase)));
        }
        return bloquesPorMateria;
    }

    private void asignarEstudiante(Estudiante estudiante, Map<Long, List<Bloque>> bloquesPorMateria) {
        List<Materia> solicitadas = new ArrayList<>(estudiante.getMateriasSolicitadas());
        solicitadas.sort(Comparator.comparing(Materia::getCodigo));

        if (solicitadas.isEmpty()) {
            estudiante.agregarNotificacion(new Notificacion(null, EstadoNotificacion.INFO,
                    "No solicitó materias, por eso no se le asignó ninguna clase.", null));
            return;
        }

        int creditosDisponibles = estudiante.getCreditosDisponibles() == null ? 0 : estudiante.getCreditosDisponibles();

        // Paso 2: opciones de cada materia (ya filtradas y con sus puntos)
        List<List<Opcion>> opciones = new ArrayList<>();
        List<Integer> creditosMateria = new ArrayList<>();
        for (int i = 0; i < solicitadas.size(); i++) {
            Materia materia = solicitadas.get(i);
            opciones.add(opcionesDe(estudiante, i, bloquesPorMateria.getOrDefault(materia.getCodigo(), List.of())));
            creditosMateria.add(materia.getCreditos());
        }

        // Pasos 3 a 6
        List<Opcion> horario = mejorHorario(estudiante, opciones, creditosMateria, creditosDisponibles);

        // x(e, c) = 1 para las clases del mejor horario
        List<Bloque> inscritas = new ArrayList<>();
        int creditosUsados = 0;
        for (Opcion opcion : horario) {
            estudiante.inscribir(opcion.bloque().clase);
            opcion.bloque().cuposRestantes--;
            inscritas.add(opcion.bloque());
            creditosUsados += opcion.bloque().creditos;
        }

        notificar(estudiante, solicitadas, bloquesPorMateria, inscritas,
                creditosDisponibles - creditosUsados, creditosDisponibles);
    }

    // ------------------------------------------------------------------
    // Paso 2: opciones de una materia para ESTE estudiante
    // ------------------------------------------------------------------

    /**
     * a) Se descartan las clases que no cumplen una regla obligatoria: sin cupo,
     * día no disponible,
     * compañero no preferido.
     * b) Se calculan los puntos de las que quedan.
     * c) Dos clases con el mismo horario y en el mismo edificio son iguales para
     * las reglas:
     * se deja la de más puntos y, si empatan, la de más cupos.
     * El orden importa: primero se descarta y después se agrupa, para no perder una
     * clase válida
     * por quedarse con otra del mismo horario que luego no sirve.
     */
    private List<Opcion> opcionesDe(Estudiante estudiante, int materia, List<Bloque> bloques) {
        List<Opcion> opciones = new ArrayList<>();
        for (Bloque bloque : bloques) {
            if (!cumpleFiltros(estudiante, bloque)) {
                continue;
            }
            Opcion nueva = new Opcion(bloque, materia, razonesDePuntos(estudiante, bloque).size());

            int posicion = -1;
            for (int i = 0; i < opciones.size(); i++) {
                Bloque otro = opciones.get(i).bloque();
                if (otro.mismaFranja(bloque) && otro.edificio().equals(bloque.edificio())) {
                    posicion = i;
                    break;
                }
            }
            if (posicion == -1) {
                opciones.add(nueva);
            } else {
                Opcion actual = opciones.get(posicion);
                boolean masPuntos = nueva.puntos() > actual.puntos();
                boolean masCupos = nueva.puntos() == actual.puntos()
                        && bloque.cuposRestantes > actual.bloque().cuposRestantes;
                if (masPuntos || masCupos) {
                    opciones.set(posicion, nueva);
                }
            }
        }
        return opciones;
    }

    private boolean cumpleFiltros(Estudiante estudiante, Bloque bloque) {
        return bloque.cuposRestantes > 0
                && !estudiante.getDiasNoDisponibles().contains(bloque.dia)
                && companeroNoPreferido(estudiante, bloque) == null;
    }

    /**
     * Alguien ya inscrito en la clase que está en la lista de no preferidos del
     * estudiante, o que lo tiene a él en la suya.
     */
    private Estudiante companeroNoPreferido(Estudiante estudiante, Bloque bloque) {
        for (Estudiante otro : bloque.clase.getEstudiantesInscritos()) {
            if (otro == estudiante) {
                continue;
            }
            if (contieneId(estudiante.getNoPreferidos(), otro.getId(), "E")
                    || contieneId(otro.getNoPreferidos(), estudiante.getId(), "E")) {
                return otro;
            }
        }
        return null;
    }

    /**
     * Un texto por cada punto que suma la clase: jornada, profesor preferido,
     * compañero preferido.
     */
    private List<String> razonesDePuntos(Estudiante estudiante, Bloque bloque) {
        List<String> razones = new ArrayList<>();
        Clase clase = bloque.clase;

        if (estudiante.getPreferenciaHoraria() != null
                && estudiante.getPreferenciaHoraria().contiene(clase.getHoraInicio(), clase.getHoraFin())) {
            razones.add("en su jornada");
        }

        String profesor = clase.getProfesor().getId();
        if (contieneId(estudiante.getPreferidos(), profesor, "P")
                && !contieneId(estudiante.getNoPreferidos(), profesor, "P")) {
            razones.add("con su profesor preferido");
        }

        // Un punto por CADA compañero preferido en la clase: suma de x(e,c) ·
        // x(amigo,c) sobre cada amigo
        for (Estudiante otro : clase.getEstudiantesInscritos()) {
            if (otro != estudiante && contieneId(estudiante.getPreferidos(), otro.getId(), "E")) {
                razones.add("con " + otro.getNombre());
            }
        }
        return razones;
    }

    /** true si 'id' empieza con 'prefijo' (E o P) y está en la lista. */
    private boolean contieneId(Set<String> ids, String id, String prefijo) {
        if (id == null || !id.toUpperCase().startsWith(prefijo)) {
            return false;
        }
        for (String otro : ids) {
            if (otro.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Pasos 3 a 6: dos voraces + mejora local
    // ------------------------------------------------------------------

    /**
     * 3) Se corren dos voraces que solo se diferencian en cómo eligen la clase de
     * cada materia:
     * - "por impacto": la que menos opciones les quita a las demás (cuida la
     * cantidad de materias);
     * - "por puntos": la de más puntos (cuida las preferencias).
     * 4) Se queda el mejor de los dos: más materias, luego más puntos, luego más
     * créditos.
     * 5) Mejora local sobre ese horario, mientras alguna mejore:
     * a) sacar una materia y volver a llenar con el voraz sin ella; al final se
     * intenta meterla de nuevo
     * (a veces caben dos donde había una);
     * b) cambiar la clase de una materia por otra de la misma materia con más
     * puntos.
     * Cada cambio se acepta solo si el horario sigue siendo válido y queda
     * estrictamente mejor,
     * así que el ciclo siempre termina.
     */
    private List<Opcion> mejorHorario(Estudiante estudiante, List<List<Opcion>> opcionesPorMateria,
            List<Integer> creditosMateria, int creditosDisponibles) {
        List<Opcion> porImpacto = horarioVoraz(estudiante, opcionesPorMateria, creditosMateria, creditosDisponibles,
                List.of(), false, -1);
        List<Opcion> porPuntos = horarioVoraz(estudiante, opcionesPorMateria, creditosMateria, creditosDisponibles,
                List.of(), true, -1);
        List<Opcion> horario = esMejor(porPuntos, porImpacto) ? porPuntos : porImpacto;

        boolean mejoro = true;
        while (mejoro) {
            mejoro = false;

            // a) sacar una materia y volver a llenar: primero SIN ella (si no, el voraz la
            // vuelve a elegir de primera)
            // y al final se intenta meterla otra vez
            for (int quitar = 0; quitar < horario.size() && !mejoro; quitar++) {
                List<Opcion> base = new ArrayList<>(horario);
                int sacada = base.remove(quitar).materia();
                for (boolean primeroPuntos : new boolean[] { false, true }) {
                    List<Opcion> lleno = horarioVoraz(estudiante, opcionesPorMateria, creditosMateria,
                            creditosDisponibles, base, primeroPuntos, sacada);
                    lleno = horarioVoraz(estudiante, opcionesPorMateria, creditosMateria,
                            creditosDisponibles, lleno, primeroPuntos, -1);
                    if (esMejor(lleno, horario)) {
                        horario = lleno;
                        mejoro = true;
                        break;
                    }
                }
            }

            // b) cambiar una clase por otra de la misma materia con más puntos
            for (int i = 0; i < horario.size() && !mejoro; i++) {
                Opcion actual = horario.get(i);
                for (Opcion otra : opcionesPorMateria.get(actual.materia())) {
                    if (otra.puntos() <= actual.puntos()) {
                        continue;
                    }
                    List<Opcion> cambiado = new ArrayList<>(horario);
                    cambiado.set(i, otra);
                    if (esHorarioValido(estudiante, bloquesDe(cambiado))) {
                        horario = cambiado;
                        mejoro = true;
                        break;
                    }
                }
            }
        }
        return horario;
    }

    /**
     * Voraz: las mismas ideas del backtracking, pero cada decisión es definitiva
     * (no se devuelve).
     * Empieza desde 'inicial' (vacío, o un horario al que se le sacó una materia),
     * no toca la materia
     * 'excluida' (-1 = ninguna) y en cada vuelta:
     * - elige la materia pendiente con MENOS clases que todavía caben (menor
     * diversidad horaria);
     * empate: la de menos créditos (deja más créditos para las demás);
     * - de esa materia elige la clase:
     * primeroPuntos = false: menos impacto en las demás, luego más puntos, luego
     * más cupos;
     * primeroPuntos = true: más puntos, luego menos impacto, luego más cupos;
     * - la inscribe y repite hasta que ninguna materia pendiente quepa.
     */
    private List<Opcion> horarioVoraz(Estudiante estudiante, List<List<Opcion>> opcionesPorMateria,
            List<Integer> creditosMateria, int creditosDisponibles,
            List<Opcion> inicial, boolean primeroPuntos, int excluida) {
        List<Opcion> horario = new ArrayList<>(inicial);
        List<Bloque> bloques = bloquesDe(horario);
        int creditos = 0;
        List<Integer> pendientes = new ArrayList<>();
        for (int i = 0; i < opcionesPorMateria.size(); i++) {
            if (i != excluida) {
                pendientes.add(i);
            }
        }
        for (Opcion opcion : horario) {
            creditos += opcion.bloque().creditos;
            pendientes.remove(Integer.valueOf(opcion.materia()));
        }

        while (true) {
            // Opciones que todavía caben de cada materia pendiente (con lo ya elegido y los
            // créditos que quedan)
            Map<Integer, List<Opcion>> caben = new LinkedHashMap<>();
            for (int i : pendientes) {
                if (creditos + creditosMateria.get(i) > creditosDisponibles) {
                    continue;
                }
                List<Opcion> validas = opcionesQueCaben(estudiante, opcionesPorMateria.get(i), bloques);
                if (!validas.isEmpty()) {
                    caben.put(i, validas);
                }
            }
            if (caben.isEmpty()) {
                break;
            }

            // La materia con menos opciones; empate: la de menos créditos
            int materia = -1;
            for (int i : caben.keySet()) {
                boolean menosOpciones = materia == -1 || caben.get(i).size() < caben.get(materia).size();
                boolean menosCreditos = materia != -1 && caben.get(i).size() == caben.get(materia).size()
                        && creditosMateria.get(i) < creditosMateria.get(materia);
                if (menosOpciones || menosCreditos) {
                    materia = i;
                }
            }

            // La clase de esa materia
            Opcion elegida = null;
            int impactoElegida = 0;
            for (Opcion opcion : caben.get(materia)) {
                bloques.add(opcion.bloque());
                int impacto = 0;
                for (Map.Entry<Integer, List<Opcion>> otra : caben.entrySet()) {
                    if (otra.getKey() != materia) {
                        impacto += otra.getValue().size()
                                - opcionesQueCaben(estudiante, otra.getValue(), bloques).size();
                    }
                }
                bloques.remove(bloques.size() - 1);

                if (elegida == null || esMejorClase(opcion, impacto, elegida, impactoElegida, primeroPuntos)) {
                    elegida = opcion;
                    impactoElegida = impacto;
                }
            }

            // Decisión definitiva
            horario.add(elegida);
            bloques.add(elegida.bloque());
            creditos += elegida.bloque().creditos;
            pendientes.remove(Integer.valueOf(materia));
        }
        return horario;
    }

    private boolean esMejorClase(Opcion opcion, int impacto, Opcion actual, int impactoActual, boolean primeroPuntos) {
        if (primeroPuntos && opcion.puntos() != actual.puntos()) {
            return opcion.puntos() > actual.puntos();
        }
        if (impacto != impactoActual) {
            return impacto < impactoActual;
        }
        if (opcion.puntos() != actual.puntos()) {
            return opcion.puntos() > actual.puntos();
        }
        return opcion.bloque().cuposRestantes > actual.bloque().cuposRestantes;
    }

    /**
     * a es mejor que b si tiene más materias; si empatan, más puntos; si empatan,
     * más créditos.
     */
    private boolean esMejor(List<Opcion> a, List<Opcion> b) {
        if (a.size() != b.size()) {
            return a.size() > b.size();
        }
        int puntosA = 0, puntosB = 0, creditosA = 0, creditosB = 0;
        for (Opcion opcion : a) {
            puntosA += opcion.puntos();
            creditosA += opcion.bloque().creditos;
        }
        for (Opcion opcion : b) {
            puntosB += opcion.puntos();
            creditosB += opcion.bloque().creditos;
        }
        if (puntosA != puntosB) {
            return puntosA > puntosB;
        }
        return creditosA > creditosB;
    }

    private List<Bloque> bloquesDe(List<Opcion> horario) {
        List<Bloque> bloques = new ArrayList<>();
        for (Opcion opcion : horario) {
            bloques.add(opcion.bloque());
        }
        return bloques;
    }

    /**
     * Las opciones que se pueden agregar a 'bloques' sin romper ninguna regla del
     * horario.
     */
    private List<Opcion> opcionesQueCaben(Estudiante estudiante, List<Opcion> opciones, List<Bloque> bloques) {
        List<Opcion> caben = new ArrayList<>();
        for (Opcion opcion : opciones) {
            bloques.add(opcion.bloque());
            if (esHorarioValido(estudiante, bloques)) {
                caben.add(opcion);
            }
            bloques.remove(bloques.size() - 1);
        }
        return caben;
    }

    // ------------------------------------------------------------------
    // Paso 7: notificaciones y alternativa
    // ------------------------------------------------------------------

    private void notificar(Estudiante estudiante, List<Materia> solicitadas, Map<Long, List<Bloque>> bloquesPorMateria,
            List<Bloque> inscritas, int creditosRestantes, int creditosDisponibles) {
        for (Materia materia : solicitadas) {
            Bloque inscrita = null;
            for (Bloque bloque : inscritas) {
                if (bloque.materiaCodigo == materia.getCodigo()) {
                    inscrita = bloque;
                }
            }

            if (inscrita != null) {
                List<String> razones = razonesDePuntos(estudiante, inscrita);
                estudiante.agregarNotificacion(new Notificacion(materia, EstadoNotificacion.INSCRITO,
                        "Inscrito en la clase " + inscrita.claseId + ": " + inscrita.horario()
                                + " con " + inscrita.clase.getProfesor().getNombre()
                                + (razones.isEmpty() ? "." : " (" + String.join(", ", razones) + ")."),
                        null));
            } else {
                notificarRechazo(estudiante, materia, bloquesPorMateria.getOrDefault(materia.getCodigo(), List.of()),
                        inscritas, creditosRestantes, creditosDisponibles);
            }
        }
    }

    /**
     * El voraz (y la mejora local) termina cuando ninguna materia pendiente cabe,
     * así que si una materia quedó por fuera es porque
     * meterla obligaría a sacar otra. Por eso la alternativa sugerida es un cambio
     * de una materia por otra.
     */
    private void notificarRechazo(Estudiante estudiante, Materia materia, List<Bloque> bloques,
            List<Bloque> inscritas, int creditosRestantes, int creditosDisponibles) {
        List<Bloque> conCupo = new ArrayList<>();
        List<Bloque> enDiasPermitidos = new ArrayList<>();
        List<Bloque> disponibles = new ArrayList<>();
        for (Bloque bloque : bloques) {
            if (bloque.cuposRestantes <= 0) {
                continue;
            }
            conCupo.add(bloque);
            if (estudiante.getDiasNoDisponibles().contains(bloque.dia)) {
                continue;
            }
            enDiasPermitidos.add(bloque);
            if (companeroNoPreferido(estudiante, bloque) == null) {
                disponibles.add(bloque);
            }
        }

        String motivo;
        String sugerencia;
        if (bloques.isEmpty()) {
            motivo = "La materia no tiene clases programadas.";
            sugerencia = "Consultar con la facultad si se abrirá algún grupo en este periodo.";

        } else if (conCupo.isEmpty()) {
            motivo = "Todas las clases de la materia están sin cupo.";
            Bloque cabe = primeraQueCabe(estudiante, bloques, inscritas);
            sugerencia = cabe != null
                    ? "Solicitar un cupo adicional en la clase " + cabe.claseId + " (" + cabe.horario()
                            + "), que encaja en su horario."
                    : "Solicitar un cupo adicional o inscribirla en el siguiente periodo.";

        } else if (enDiasPermitidos.isEmpty()) {
            Set<String> dias = new LinkedHashSet<>();
            for (Bloque bloque : conCupo) {
                dias.add(bloque.dia.getNombre());
            }
            motivo = "Las clases con cupo caen en días que marcó como no disponibles: " + String.join(", ", dias) + ".";
            Bloque cabe = primeraQueCabe(estudiante, conCupo, inscritas);
            sugerencia = cabe != null
                    ? "Si habilita el " + cabe.dia.getNombre() + ", puede inscribir la clase " + cabe.claseId
                            + " (" + cabe.horario() + ")."
                    : "Inscribirla en el siguiente periodo.";

        } else if (disponibles.isEmpty()) {
            Bloque ejemplo = enDiasPermitidos.get(0);
            Estudiante companero = companeroNoPreferido(estudiante, ejemplo);
            motivo = "Las clases disponibles tienen un compañero con el que no puede coincidir (por ejemplo, "
                    + companero.getNombre() + " en la clase " + ejemplo.claseId + ").";
            sugerencia = "Revisar la lista de compañeros no preferidos o inscribirla en el siguiente periodo.";

        } else if (materia.getCreditos() > creditosDisponibles) {
            motivo = "La materia tiene " + materia.getCreditos() + " créditos y solo tiene "
                    + creditosDisponibles + " disponibles.";
            sugerencia = "Solicitar una ampliación de créditos o inscribirla en el siguiente periodo.";

        } else if (materia.getCreditos() > creditosRestantes) {
            motivo = "No le alcanzan los créditos: la materia tiene " + materia.getCreditos()
                    + " y le quedan " + creditosRestantes + " después de inscribir las demás.";
            sugerencia = sugerirCambio(estudiante, materia, disponibles, inscritas, creditosRestantes);

        } else {
            motivo = "Ninguna clase disponible cabe en su horario: "
                    + porQueNoCabe(estudiante, disponibles.get(0), inscritas);
            sugerencia = sugerirCambio(estudiante, materia, disponibles, inscritas, creditosRestantes);
        }

        estudiante.agregarNotificacion(new Notificacion(materia, EstadoNotificacion.RECHAZADO, motivo, sugerencia));
    }

    /**
     * Explica, con una clase de ejemplo, cuál regla impide agregarla: cruce, carga
     * diaria o distancia.
     */
    private String porQueNoCabe(Estudiante estudiante, Bloque bloque, List<Bloque> inscritas) {
        int delMismoDia = 0;
        for (Bloque inscrita : inscritas) {
            if (bloque.seCruzaCon(inscrita)) {
                return "la clase " + bloque.claseId + " (" + bloque.horario() + ") se cruza con "
                        + inscrita.clase.getMateria().getNombre() + ".";
            }
            if (inscrita.dia == bloque.dia) {
                delMismoDia++;
            }
        }
        Integer maxPorDia = estudiante.getMaxClasesPorDia();
        if (maxPorDia != null && delMismoDia >= maxPorDia) {
            return "con la clase " + bloque.claseId + " superaría su carga máxima de " + maxPorDia
                    + " clases el " + bloque.dia.getNombre() + ".";
        }
        Integer maxDesplazamiento = estudiante.getTiempoDesplazamientoMax();
        for (Bloque inscrita : inscritas) {
            if (maxDesplazamiento != null && sonSeguidas(inscrita, bloque, inscritas)) {
                Bloque primera = inscrita.fin <= bloque.inicio ? inscrita : bloque;
                Bloque segunda = primera == inscrita ? bloque : inscrita;
                int minutos = minutosEntre(primera.clase, segunda.clase);
                if (minutos > maxDesplazamiento) {
                    return "ir de " + primera.edificio() + " a " + segunda.edificio() + " (clase " + bloque.claseId
                            + ") toma "
                            + (minutos == Integer.MAX_VALUE ? "un tiempo no registrado" : minutos + " minutos")
                            + " y su máximo es " + maxDesplazamiento + ".";
                }
            }
        }
        return "meterla obligaría a sacar otra materia.";
    }

    /** true si a y b son del mismo día y no hay otra clase inscrita entre ellas. */
    private boolean sonSeguidas(Bloque a, Bloque b, List<Bloque> inscritas) {
        if (a.dia != b.dia) {
            return false;
        }
        int desde = Math.min(a.fin, b.fin);
        int hasta = Math.max(a.inicio, b.inicio);
        for (Bloque otra : inscritas) {
            if (otra != a && otra != b && otra.inicio >= desde && otra.fin <= hasta) {
                return false;
            }
        }
        return true;
    }

    /**
     * Alternativa: cambiar UNA materia inscrita por una clase de esta materia, si
     * así se cumplen todas las reglas.
     */
    private String sugerirCambio(Estudiante estudiante, Materia materia, List<Bloque> opciones,
            List<Bloque> inscritas, int creditosRestantes) {
        for (Bloque opcion : opciones) {
            for (Bloque inscrita : inscritas) {
                if (creditosRestantes + inscrita.creditos < materia.getCreditos()) {
                    continue;
                }
                List<Bloque> cambiado = new ArrayList<>(inscritas);
                cambiado.remove(inscrita);
                cambiado.add(opcion);
                if (esHorarioValido(estudiante, cambiado)) {
                    return "Puede cambiar " + inscrita.clase.getMateria().getNombre() + " (clase " + inscrita.claseId
                            + ") por la clase " + opcion.claseId + " de esta materia (" + opcion.horario() + ").";
                }
            }
        }
        return "No hay un cambio sencillo que permita inscribirla; puede inscribirla en el siguiente periodo.";
    }

    private Bloque primeraQueCabe(Estudiante estudiante, List<Bloque> bloques, List<Bloque> inscritas) {
        for (Bloque bloque : bloques) {
            List<Bloque> conBloque = new ArrayList<>(inscritas);
            conBloque.add(bloque);
            if (esHorarioValido(estudiante, conBloque)) {
                return bloque;
            }
        }
        return null;
    }

    /**
     * Revisa las reglas que dependen del horario completo: cruces, carga diaria y
     * distancia entre clases seguidas.
     */
    private boolean esHorarioValido(Estudiante estudiante, List<Bloque> horario) {
        List<Bloque> ordenado = new ArrayList<>(horario);
        ordenado.sort(Comparator.comparingInt(b -> b.inicio));
        Integer maxPorDia = estudiante.getMaxClasesPorDia();
        Integer maxDesplazamiento = estudiante.getTiempoDesplazamientoMax();

        int[] clasesPorDia = new int[DIAS];
        for (int i = 0; i < ordenado.size(); i++) {
            Bloque actual = ordenado.get(i);
            clasesPorDia[actual.dia.ordinal()]++;
            if (maxPorDia != null && clasesPorDia[actual.dia.ordinal()] > maxPorDia) {
                return false;
            }
            if (i > 0) {
                Bloque anterior = ordenado.get(i - 1);
                if (anterior.seCruzaCon(actual)) {
                    return false;
                }
                if (anterior.dia == actual.dia && maxDesplazamiento != null
                        && minutosEntre(anterior.clase, actual.clase) > maxDesplazamiento) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Datos que usa el algoritmo
    // ------------------------------------------------------------------

    /**
     * Una clase posible para un estudiante: su bloque, el índice de la materia
     * (bit) y los puntos que le da.
     */
    private record Opcion(Bloque bloque, int materia, int puntos) {
    }

    /**
     * Copia liviana de una clase con lo que usa el algoritmo: id, materia,
     * créditos, día y horas.
     * inicio y fin son minutos contados desde el lunes 00:00 (la semana como una
     * sola línea de tiempo):
     * martes 08:00 = 1 * 1440 + 480 = 1920.
     * Se arma una sola vez; durante el algoritmo no se vuelve a consultar la base
     * de datos.
     */
    private static final class Bloque {

        final Clase clase;
        final long claseId;
        final long materiaCodigo;
        final int creditos;
        final Dia dia;
        final int inicio;
        final int fin;
        int cuposRestantes;

        Bloque(Clase clase, int cuposRestantes) {
            this.clase = clase;
            this.claseId = clase.getId();
            this.materiaCodigo = clase.getMateria().getCodigo();
            this.creditos = clase.getMateria().getCreditos();
            this.dia = clase.getDia();
            this.inicio = dia.ordinal() * MINUTOS_POR_DIA + clase.getHoraInicio().toSecondOfDay() / 60;
            this.fin = dia.ordinal() * MINUTOS_POR_DIA + clase.getHoraFin().toSecondOfDay() / 60;
            this.cuposRestantes = cuposRestantes;
        }

        boolean seCruzaCon(Bloque otro) {
            return inicio < otro.fin && otro.inicio < fin;
        }

        boolean mismaFranja(Bloque otro) {
            return inicio == otro.inicio && fin == otro.fin;
        }

        String edificio() {
            return clase.getSalon().getEdificio().getNombre();
        }

        String horario() {
            return clase.getHorarioTexto();
        }
    }

    private int cuposRestantes(Clase clase) {
        return clase.getCupos() - clase.getEstudiantesInscritos().size();
    }

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

    private void limpiarAsignacionAnterior(List<Estudiante> estudiantes, List<Clase> clases) {
        for (Estudiante estudiante : estudiantes) {
            estudiante.getClasesInscritas().clear();
            estudiante.getNotificaciones().clear();
        }
        for (Clase clase : clases) {
            clase.getEstudiantesInscritos().clear();
        }
    }
}