package com.crono.controllers;

import com.crono.entities.Dia;
import com.crono.entities.Estudiante;
import com.crono.entities.PreferenciaHoraria;
import com.crono.service.CargaJsonService;
import com.crono.service.EstudianteService;
import com.crono.service.MateriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Sección "Datos": subir el JSON o registrar un estudiante a mano. */
@Controller
@RequestMapping("/datos")
public class DatosController {

    private final CargaJsonService cargaJsonService;
    private final EstudianteService estudianteService;
    private final MateriaService materiaService;

    public DatosController(CargaJsonService cargaJsonService, EstudianteService estudianteService,
        MateriaService materiaService) {
        this.cargaJsonService = cargaJsonService;
        this.estudianteService = estudianteService;
        this.materiaService = materiaService;
    }

    /** Tarjeta "Subir Archivo de Datos". */
    @PostMapping("/cargar-json")
    public String cargarJson(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes redirect) {
        if (archivo.isEmpty()) {
            redirect.addFlashAttribute("error", "Seleccione un archivo JSON");
            return "redirect:/";
        }
        try (InputStream contenido = archivo.getInputStream()) {
            redirect.addFlashAttribute("mensaje", cargaJsonService.importarArchivo(contenido));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "No se pudo leer el archivo: " + e.getMessage());
        }
        return "redirect:/";
    }

    /** Tarjeta "Ingreso Manual": muestra el formulario. */
    @GetMapping("/manual")
    public String formularioManual(Model model) {
        model.addAttribute("estudiante", new Estudiante());
        agregarOpciones(model);
        return "estudiante-form";
    }

    /**
     * Botón "Guardar Datos de Estudiante".
     * Spring llena el objeto Estudiante con los campos del formulario que tienen el mismo nombre
     * (id, nombre, correo, preferidos "P201, E1002" -> Set, diasNoDisponibles...).
     * Las materias llegan aparte como lista de códigos (checkboxes name="materias").
     */
    @PostMapping("/manual")
    public String guardarManual(@ModelAttribute("estudiante") Estudiante estudiante, BindingResult binding, @RequestParam(required = false) List<Long> materias,Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("error", "Revise los campos numéricos (créditos, minutos, carga máxima)");
            agregarOpciones(model);
            return "estudiante-form";
        }
        try {
            Estudiante guardado = estudianteService.crear(estudiante, materias);
            redirect.addFlashAttribute("mensaje", "Estudiante " + guardado.getId() + " guardado");
            return "redirect:/estudiantes";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            agregarOpciones(model);
            return "estudiante-form"; // vuelve al formulario sin perder lo escrito
        }
    }

    private void agregarOpciones(Model model) {
        model.addAttribute("preferencias", PreferenciaHoraria.values());
        model.addAttribute("dias", Dia.values());
        model.addAttribute("materias", materiaService.listar());
    }
}
