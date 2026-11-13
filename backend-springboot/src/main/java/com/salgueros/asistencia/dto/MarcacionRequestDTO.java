package com.salgueros.asistencia.dto;

import java.util.List;

public class MarcacionRequestDTO {
    /**
     * Vector biométrico de 128 flotantes extraído en el navegador del POS con TensorFlow.js
     */
    private List<Double> vectorEmbedding;

    /**
     * Tipo: ENTRADA, SALIDA_ALMUERZO, REGRESO_ALMUERZO, SALIDA
     */
    private String tipoMarcacion;

    /**
     * Flag opcional para confirmar marcación si salta la alerta de doble marcación (HU-03)
     */
    private Boolean forzarConfirmacion;

    public MarcacionRequestDTO() {}

    public MarcacionRequestDTO(List<Double> vectorEmbedding, String tipoMarcacion, Boolean forzarConfirmacion) {
        this.vectorEmbedding = vectorEmbedding;
        this.tipoMarcacion = tipoMarcacion;
        this.forzarConfirmacion = forzarConfirmacion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Double> getVectorEmbedding() { return vectorEmbedding; }
    public void setVectorEmbedding(List<Double> vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; }

    public String getTipoMarcacion() { return tipoMarcacion; }
    public void setTipoMarcacion(String tipoMarcacion) { this.tipoMarcacion = tipoMarcacion; }

    public Boolean getForzarConfirmacion() { return forzarConfirmacion; }
    public void setForzarConfirmacion(Boolean forzarConfirmacion) { this.forzarConfirmacion = forzarConfirmacion; }

    public static class Builder {
        private List<Double> vectorEmbedding;
        private String tipoMarcacion;
        private Boolean forzarConfirmacion;

        public Builder vectorEmbedding(List<Double> vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; return this; }
        public Builder tipoMarcacion(String tipoMarcacion) { this.tipoMarcacion = tipoMarcacion; return this; }
        public Builder forzarConfirmacion(Boolean forzarConfirmacion) { this.forzarConfirmacion = forzarConfirmacion; return this; }

        public MarcacionRequestDTO build() {
            return new MarcacionRequestDTO(vectorEmbedding, tipoMarcacion, forzarConfirmacion);
        }
    }
}
