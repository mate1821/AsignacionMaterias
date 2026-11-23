package com.crono.entities;


public enum EstadoNotificacion {
    INSCRITO("Inscrito"),
    RECHAZADO("Rechazado"),
    INFO("Información");

    private final String nombre;

    EstadoNotificacion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
