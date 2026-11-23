package com.crono.controllers;

import com.crono.repository.ClaseRepository;
import com.crono.repository.EstudianteRepository;
import com.crono.repository.MateriaRepository;
import com.crono.repository.ProfesorRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class IndexController {

    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final ClaseRepository claseRepository;

    public IndexController(EstudianteRepository estudianteRepository, ProfesorRepository profesorRepository,MateriaRepository materiaRepository, ClaseRepository claseRepository) {
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.materiaRepository = materiaRepository;
        this.claseRepository = claseRepository;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("totalEstudiantes", estudianteRepository.count());
        model.addAttribute("totalProfesores", profesorRepository.count());
        model.addAttribute("totalMaterias", materiaRepository.count());
        model.addAttribute("totalClases", claseRepository.count());
        return "indice";
    }
}
