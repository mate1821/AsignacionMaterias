package com.crono;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
import com.crono.entities.Edificio;
import com.crono.entities.Estudiante;
import com.crono.entities.Materia;
import com.crono.entities.PreferenciaHoraria;
import com.crono.entities.Profesor;
import com.crono.entities.Salon;
import com.crono.entities.Universidad;
import com.crono.repository.ClaseRepository;
import com.crono.repository.EdificioRepository;
import com.crono.repository.EstudianteRepository;
import com.crono.repository.MateriaRepository;
import com.crono.repository.ProfesorRepository;
import com.crono.repository.SalonRepository;
import com.crono.repository.UniversidadRepository;

import jakarta.transaction.Transactional;

@Component
@Transactional
public class DataLoader implements CommandLineRunner {

        @Autowired
        private UniversidadRepository universidadRepo;

        @Autowired
        private EdificioRepository edificioRepo;

        @Autowired
        private SalonRepository salonRepo;

        @Autowired
        private MateriaRepository materiaRepo;

        @Autowired
        private ProfesorRepository profesorRepo;

        @Autowired
        private ClaseRepository claseRepo;

        @Autowired
        private EstudianteRepository estudianteRepo;

        @Override
        public void run(String... args) throws Exception {

                // Si la BD ya tiene datos (ej. PostgreSQL con ddl-auto=update) no se vuelve a cargar
                if (universidadRepo.count() > 0) {
                        return;
                }

                Random random = new Random(42);

                // IMPORTANTE: aquí los ids NO son autogenerados (301, "P201", "E1001"...).
                // Con ids asignados, save() hace un merge y devuelve OTRA instancia (la administrada por JPA).
                // Por eso, cuando el objeto se vuelve a usar, se guarda lo que devuelve save().

                // Universidad
                Universidad javeriana = universidadRepo.save(new Universidad(1, "Pontificia Universidad Javeriana"));

                // Edificios
                Edificio maldonado = edificioRepo.save(new Edificio("Maldonado", javeriana));
                Edificio giraldo = edificioRepo.save(new Edificio("Giraldo", javeriana));
                Edificio baron = edificioRepo.save(new Edificio("Baron", javeriana));

                // Tiempos de desplazamiento entre edificios (minutos, en los dos sentidos)
                maldonado.getTiempos().put("Giraldo", 10);
                giraldo.getTiempos().put("Maldonado", 10);
                maldonado.getTiempos().put("Baron", 7);
                baron.getTiempos().put("Maldonado", 7);
                giraldo.getTiempos().put("Baron", 5);
                baron.getTiempos().put("Giraldo", 5);

                // Salones: 15 por edificio -> 101-115 Maldonado, 201-215 Giraldo, 301-315 Baron
                for (int i = 1; i <= 15; i++) {
                        salonRepo.save(new Salon(100L + i, maldonado));
                        salonRepo.save(new Salon(200L + i, giraldo));
                        salonRepo.save(new Salon(300L + i, baron));
                }

                // Materias (código, nombre, créditos)
                materiaRepo.save(new Materia(301L, "Cálculo Diferencial", 3));
                materiaRepo.save(new Materia(302L, "Introducción a la Programación", 3));
                materiaRepo.save(new Materia(303L, "Introducción a la Ingeniería", 2));
                materiaRepo.save(new Materia(304L, "Pensamiento Sistémico", 3));
                materiaRepo.save(new Materia(305L, "Lógica y Matemáticas Discretas", 3));
                materiaRepo.save(new Materia(306L, "Epistemología", 2));
                materiaRepo.save(new Materia(307L, "Fundamentos de Seguridad de la Información", 2));

                // Cupos de cada clase según la materia (enunciado)
                Map<Long, Integer> cuposPorMateria = Map.of(
                                301L, 35, 302L, 20, 303L, 40, 304L, 20, 305L, 30, 306L, 15, 307L, 24);

                // Profesores: 3 por materia (en orden: P201-P203 -> 301, P204-P206 -> 302, ...)
                // Materia 301
                profesorRepo.save(new Profesor("P201", "Carlos Gomez", "cgomez@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P202", "Ana Rodriguez", "arodriguez@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P203", "Luis Martinez", "lmartinez@javeriana.edu.co", javeriana));
                // Materia 302
                profesorRepo.save(new Profesor("P204", "Maria Fernandez", "mfernandez@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P205", "Jorge Ramirez", "jramirez@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P206", "Laura Torres", "ltorres@javeriana.edu.co", javeriana));
                // Materia 303
                profesorRepo.save(new Profesor("P207", "Andres Castillo", "acastillo@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P208", "Paula Herrera", "pherrera@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P209", "Diego Morales", "dmorales@javeriana.edu.co", javeriana));
                // Materia 304
                profesorRepo.save(new Profesor("P210", "Camila Vargas", "cvargas@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P211", "Felipe Rojas", "frojas@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P212", "Natalia Jimenez", "njimenez@javeriana.edu.co", javeriana));
                // Materia 305
                profesorRepo.save(new Profesor("P213", "Sergio Mendoza", "smendoza@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P214", "Daniela Ortiz", "dortiz@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P215", "Ricardo Silva", "rsilva@javeriana.edu.co", javeriana));
                // Materia 306
                profesorRepo.save(new Profesor("P216", "Valentina Cruz", "vcruz@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P217", "Oscar Pineda", "opineda@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P218", "Juliana Rios", "jrios@javeriana.edu.co", javeriana));
                // Materia 307
                profesorRepo.save(new Profesor("P219", "Andrea Salazar", "asalazar@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P220", "Mauricio Leon", "mleon@javeriana.edu.co", javeriana));
                profesorRepo.save(new Profesor("P221", "Carolina Duarte", "cduarte@javeriana.edu.co", javeriana));

                // Estudiantes (id, nombre, correo, tiempoDesplazamientoMax, creditosDisponibles, jornada, maxClasesPorDia)
                estudianteRepo.save(new Estudiante("E1001", "Eileen Rodriguez", "ro-eileen@javeriana.edu.co",
                                10, 20, PreferenciaHoraria.MANANA, 3));
                estudianteRepo.save(new Estudiante("E1002", "Mateo Zamora", "zamorapmateo@javeriana.edu.co",
                                10, 18, PreferenciaHoraria.TARDE, 3));
                estudianteRepo.save(new Estudiante("E1003", "Santiago Perez", "s.perez@javeriana.edu.co",
                                7, 16, PreferenciaHoraria.MANANA, 2));
                estudianteRepo.save(new Estudiante("E1004", "Isabella Gomez", "i.gomez@javeriana.edu.co",
                                5, 12, PreferenciaHoraria.TARDE, 2));
                estudianteRepo.save(new Estudiante("E1005", "Tomas Restrepo", "t.restrepo@javeriana.edu.co",
                                10, 20, PreferenciaHoraria.MANANA, 3));
                estudianteRepo.save(new Estudiante("E1006", "Sofia Ramirez", "s.ramirez@javeriana.edu.co",
                                10, 9, PreferenciaHoraria.TARDE, 2));
                estudianteRepo.save(new Estudiante("E1007", "Juan Esteban Diaz", "je.diaz@javeriana.edu.co",
                                7, 15, PreferenciaHoraria.MANANA, 2));
                estudianteRepo.save(new Estudiante("E1008", "Mariana Lopez", "m.lopez@javeriana.edu.co",
                                10, 18, PreferenciaHoraria.TARDE, 3));
                estudianteRepo.save(new Estudiante("E1009", "Nicolas Herrera", "n.herrera@javeriana.edu.co",
                                5, 14, PreferenciaHoraria.MANANA, 2));
                estudianteRepo.save(new Estudiante("E1010", "Gabriela Castro", "g.castro@javeriana.edu.co",
                                10, 20, PreferenciaHoraria.TARDE, 3));

                // LOS DATOS QUE SON LLAVES FORÁNEAS SE ELIGEN ALEATORIAMENTE AQUI

                // Clases: 6 por materia, cada profesor dicta 2 clases de SU materia.
                // Día, hora y salón son aleatorios, pero se repite el sorteo si el salón o el profesor
                // ya están ocupados a esa hora (si no, fallarían los UNIQUE de la tabla clase).
                List<Materia> todasLasMaterias = materiaRepo.findAll(Sort.by("codigo"));
                List<Profesor> todosLosProfesores = profesorRepo.findAll(Sort.by("id"));
                List<Salon> todosLosSalones = salonRepo.findAll(Sort.by("id"));
                Dia[] dias = Dia.values();

                Set<String> salonOcupado = new HashSet<>();
                Set<String> profesorOcupado = new HashSet<>();
                long idClase = 501;

                for (int i = 0; i < todasLasMaterias.size(); i++) {
                        Materia materia = todasLasMaterias.get(i);

                        for (int k = 0; k < 6; k++) {
                                // clases 0-1 -> primer profesor, 2-3 -> segundo, 4-5 -> tercero
                                Profesor profesor = todosLosProfesores.get(i * 3 + k / 2);

                                Dia dia;
                                LocalTime horaInicio;
                                Salon salon;
                                do {
                                        dia = dias[random.nextInt(0, dias.length)];
                                        horaInicio = LocalTime.of(6 + 2 * random.nextInt(0, 6), 0); // 6,8,...,16
                                        salon = todosLosSalones.get(random.nextInt(0, todosLosSalones.size()));
                                } while (salonOcupado.contains(salon.getId() + "-" + dia + "-" + horaInicio)
                                                || profesorOcupado.contains(profesor.getId() + "-" + dia + "-" + horaInicio));

                                salonOcupado.add(salon.getId() + "-" + dia + "-" + horaInicio);
                                profesorOcupado.add(profesor.getId() + "-" + dia + "-" + horaInicio);

                                claseRepo.save(new Clase(idClase, materia, profesor, salon,
                                                cuposPorMateria.get(materia.getCodigo()),
                                                dia, horaInicio, horaInicio.plusHours(2)));
                                idClase++;
                        }
                }

                // Estudiantes: universidad, materias solicitadas, preferidos, no preferidos y días no disponibles
                List<Estudiante> todosLosEstudiantes = estudianteRepo.findAll(Sort.by("id"));
                int cantidadMaterias = todasLasMaterias.size();
                int cantidadProfesores = todosLosProfesores.size();
                int cantidadEstudiantes = todosLosEstudiantes.size();

                for (Estudiante e : todosLosEstudiantes) {
                        e.setUniversidad(javeriana);

                        // Entre 2 y 4 materias distintas (el Set evita repetidas)
                        int cantidadSolicitadas = random.nextInt(2, 5);
                        while (e.getMateriasSolicitadas().size() < cantidadSolicitadas) {
                                e.getMateriasSolicitadas().add(todasLasMaterias.get(random.nextInt(0, cantidadMaterias)));
                        }

                        // Un compañero preferido (E...) que no sea él mismo
                        Estudiante companero = todosLosEstudiantes.get(random.nextInt(0, cantidadEstudiantes));
                        if (!companero.getId().equals(e.getId())) {
                                e.getPreferidos().add(companero.getId());
                        }

                        // Un profesor preferido (P...) y uno no preferido, distintos entre sí
                        Profesor preferido = todosLosProfesores.get(random.nextInt(0, cantidadProfesores));
                        Profesor noPreferido = todosLosProfesores.get(random.nextInt(0, cantidadProfesores));
                        e.getPreferidos().add(preferido.getId());
                        if (!noPreferido.getId().equals(preferido.getId())) {
                                e.getNoPreferidos().add(noPreferido.getId());
                        }

                        // 1 de cada 3 estudiantes tiene un día no disponible
                        if (random.nextInt(0, 3) == 0) {
                                e.getDiasNoDisponibles().add(dias[random.nextInt(0, dias.length)]);
                        }

                        estudianteRepo.save(e);
                }
        }

}