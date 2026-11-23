package com.crono.controllers;

import com.crono.entities.Estudiante;
import com.crono.service.EstudianteService;
import com.crono.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/** Sección "Reportes": descarga del JSON general y notificaciones por estudiante. */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final EstudianteService estudianteService;
    private final ReporteService reporteService;

    public ReporteController(EstudianteService estudianteService, ReporteService reporteService) {
        this.estudianteService = estudianteService;
        this.reporteService = reporteService;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) String estudianteId, Model model) {
        List<Estudiante> estudiantes = estudianteService.listar();
        model.addAttribute("estudiantes", estudiantes);

        if ((estudianteId == null || estudianteId.isBlank()) && !estudiantes.isEmpty()) {
            estudianteId = estudiantes.get(0).getId();
        }
        model.addAttribute("estudianteId", estudianteId);
        if (estudianteId != null) {
            model.addAttribute("notificaciones", reporteService.notificacionesDe(estudianteId));
        }
        return "reportes";
    }

    /** Botón "Descargar Archivo General JSON". Spring convierte el Map a JSON. */
    @GetMapping("/descargar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> descargar() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"asignaciones.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reporteService.generarSalida());
    }
}
