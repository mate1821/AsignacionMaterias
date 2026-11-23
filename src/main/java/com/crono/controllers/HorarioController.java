package com.crono.controllers;

import com.crono.entities.Estudiante;
import com.crono.service.EstudianteService;
import com.crono.service.HorarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Sección "Horario": lista desplegable de estudiantes + su horario. */
@Controller
@RequestMapping("/horario")
public class HorarioController {

    private final EstudianteService estudianteService;
    private final HorarioService horarioService;

    public HorarioController(EstudianteService estudianteService, HorarioService horarioService) {
        this.estudianteService = estudianteService;
        this.horarioService = horarioService;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) String estudianteId, Model model) {
        List<Estudiante> estudiantes = estudianteService.listar();
        model.addAttribute("estudiantes", estudiantes);

        // Si no eligió a nadie, se muestra el primero (como en el mockup)
        if ((estudianteId == null || estudianteId.isBlank()) && !estudiantes.isEmpty()) {
            estudianteId = estudiantes.get(0).getId();
        }
        model.addAttribute("estudianteId", estudianteId);
        if (estudianteId != null) {
            try {
                model.addAttribute("clases", horarioService.horarioDe(estudianteId));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "horario";
    }
}
