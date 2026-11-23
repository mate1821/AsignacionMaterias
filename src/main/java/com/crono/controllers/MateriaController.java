package com.crono.controllers;

import com.crono.service.MateriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Regla "Registro dinámico de materias". */
@Controller
@RequestMapping("/materias")
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("materias", materiaService.listar());
        return "materias";
    }

    @PostMapping
    public String crear(@RequestParam(required = false) Long codigo, @RequestParam String nombre,
                        @RequestParam(required = false) Integer creditos, RedirectAttributes redirect) {
        try {
            materiaService.crear(codigo, nombre, creditos);
            redirect.addFlashAttribute("mensaje", "Materia " + codigo + " registrada");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/materias";
    }
}
