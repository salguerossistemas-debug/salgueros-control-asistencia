package com.salgueros.asistencia.model;

public enum TipoRemision {
    INSUMOS("Insumos y Aseo"),
    COMIDA("Comida y Alimentos");

    private final String descripcion;

    TipoRemision(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}