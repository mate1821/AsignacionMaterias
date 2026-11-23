package com.crono.service;

import com.crono.entities.Materia;
import com.crono.repository.MateriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regla "Registro dinámico de materias". */
@Service
public class MateriaService {

    private final MateriaRepository materiaRepository;

    public MateriaService(MateriaRepository materiaRepository) {
        this.materiaRepository = materiaRepository;
    }

    @Transactional(readOnly = true)
    public List<Materia> listar() {
        return materiaRepository.findAll(Sort.by("codigo"));
    }

    @Transactional(readOnly = true)
    public Materia buscar(Long codigo) {
        return materiaRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe la materia " + codigo));
    }

    @Transactional
    public Materia crear(Long codigo, String nombre, Integer creditos) {
        Materia materia = new Materia(codigo, nombre, creditos);
        validar(materia);
        if (materiaRepository.existsById(codigo)) {
            throw new IllegalArgumentException("Ya existe una materia con código " + codigo);
        }
        return materiaRepository.save(materia);
    }

    public void validar(Materia materia) {
        if (materia.getCodigo() == null || materia.getCodigo() <= 0) {
            throw new IllegalArgumentException("El código de la materia debe ser un número positivo");
        }
        if (materia.getNombre() == null || materia.getNombre().isBlank()) {
            throw new IllegalArgumentException("La materia " + materia.getCodigo() + " no tiene nombre");
        }
        if (materia.getCreditos() == null || materia.getCreditos() <= 0) {
            throw new IllegalArgumentException("La materia " + materia.getCodigo() + " debe tener créditos mayores a 0");
        }
    }
}
