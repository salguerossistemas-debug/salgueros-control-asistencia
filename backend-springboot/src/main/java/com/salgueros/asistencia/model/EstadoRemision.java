package com.salgueros.asistencia.model;

public enum EstadoRemision {
    PENDIENTE("Pendiente"),
    RECIBIDA("Recibida"),
    RECHAZADA("Rechazada");

    private final String descripcion;

    EstadoRemision(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}