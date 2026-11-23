package com.crono.controllers;

import com.crono.repository.ClaseRepository;
import com.crono.repository.EstudianteRepository;
import com.crono.service.AsignacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador del ALGORITMO.
 * GET  /asignacion           -> pantalla con el botón "Ejecutar asignación"
 * POST /asignacion/ejecutar  -> AsignacionService.ejecutar() -> asignar(...)  (el hueco)
 */
@Controller
@RequestMapping("/asignacion")
public class AsignacionController {

    private final AsignacionService asignacionService;
    private final EstudianteRepository estudianteRepository;
    private final ClaseRepository claseRepository;

    public AsignacionController(AsignacionService asignacionService, EstudianteRepository estudianteRepository,ClaseRepository claseRepository) {
        this.asignacionService = asignacionService;
        this.estudianteRepository = estudianteRepository;
        this.claseRepository = claseRepository;
    }

    @GetMapping
    public String ver(Model model) {
        model.addAttribute("totalEstudiantes", estudianteRepository.count());
        model.addAttribute("totalClases", claseRepository.count());
        return "asignacion";
    }

    @PostMapping("/ejecutar")
    public String ejecutar(RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("mensaje", asignacionService.ejecutar());
            return "redirect:/reportes";
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "Error en el algoritmo: " + e.getMessage());
            return "redirect:/asignacion";
        }
    }
}
