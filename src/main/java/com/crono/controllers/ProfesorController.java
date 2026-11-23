package com.crono.controllers;

import com.crono.service.ProfesorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Regla "Registro dinámico de profesores". */
@Controller
@RequestMapping("/profesores")
public class ProfesorController {

    private final ProfesorService profesorService;

    public ProfesorController(ProfesorService profesorService) {
        this.profesorService = profesorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("profesores", profesorService.listar());
        return "profesores";
    }

    @PostMapping
    public String crear(@RequestParam String id, @RequestParam String nombre, @RequestParam String correo,
                        RedirectAttributes redirect) {
        try {
            profesorService.crear(id, nombre, correo);
            redirect.addFlashAttribute("mensaje", "Profesor " + id + " registrado");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profesores";
    }
}
