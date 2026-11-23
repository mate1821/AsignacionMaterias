package com.crono.service;

import com.crono.entities.Edificio;
import com.crono.entities.Salon;
import com.crono.repository.EdificioRepository;
import com.crono.repository.SalonRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Edificios y salones (solo lectura: se precargan en DataLoader). */
@Service
public class InfraestructuraService {

    private final EdificioRepository edificioRepository;
    private final SalonRepository salonRepository;

    public InfraestructuraService(EdificioRepository edificioRepository, SalonRepository salonRepository) {
        this.edificioRepository = edificioRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional(readOnly = true)
    public List<Edificio> listarEdificios() {
        return edificioRepository.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public List<Salon> listarSalones() {
        return salonRepository.findAll(Sort.by("id"));
    }
}
