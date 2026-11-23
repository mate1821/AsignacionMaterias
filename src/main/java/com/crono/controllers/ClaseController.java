package com.crono.controllers;

import com.crono.entities.Clase;
import com.crono.entities.Dia;
import com.crono.service.ClaseService;
import com.crono.service.InfraestructuraService;
import com.crono.service.MateriaService;
import com.crono.service.ProfesorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Regla "Creación dinámica de clases por materia". */
@Controller
@RequestMapping("/clases")
public class ClaseController {

    private final ClaseService claseService;
    private final MateriaService materiaService;
    private final ProfesorService profesorService;
    private final InfraestructuraService infraestructuraService;

    public ClaseController(ClaseService claseService, MateriaService materiaService,
        ProfesorService profesorService, InfraestructuraService infraestructuraService) {
        this.claseService = claseService;
        this.materiaService = materiaService;
        this.profesorService = profesorService;
        this.infraestructuraService = infraestructuraService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clases", claseService.listar());
        model.addAttribute("siguienteId", claseService.siguienteId());
        model.addAttribute("materias", materiaService.listar());
        model.addAttribute("profesores", profesorService.listar());
        model.addAttribute("salones", infraestructuraService.listarSalones());
        model.addAttribute("dias", Dia.values());
        model.addAttribute("horas", ClaseService.INICIOS_DE_BLOQUE);
        return "clases";
    }

    @PostMapping
    public String crear(@RequestParam(required = false) Long id,
                        @RequestParam Long materiaCodigo,
                        @RequestParam String profesorId,
                        @RequestParam Long salonId,
                        @RequestParam(required = false) Integer cupos,
                        @RequestParam String dia,
                        @RequestParam String horaInicio,
                        RedirectAttributes redirect) {
        try {
            Clase clase = claseService.crear(id, materiaCodigo, profesorId, salonId, cupos, dia, horaInicio);
            redirect.addFlashAttribute("mensaje", "Clase " + clase.getId() + " registrada");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clases";
    }
}
