package com.salgueros.asistencia.dto;

import java.util.List;

public class EnrolamientoRequestDTO {
    private String cedula;
    private List<Double> vectorEmbedding;

    public EnrolamientoRequestDTO() {}

    public EnrolamientoRequestDTO(String cedula, List<Double> vectorEmbedding) {
        this.cedula = cedula;
        this.vectorEmbedding = vectorEmbedding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public List<Double> getVectorEmbedding() { return vectorEmbedding; }
    public void setVectorEmbedding(List<Double> vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; }

    public static class Builder {
        private String cedula;
        private List<Double> vectorEmbedding;

        public Builder cedula(String cedula) { this.cedula = cedula; return this; }
        public Builder vectorEmbedding(List<Double> vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; return this; }

        public EnrolamientoRequestDTO build() {
            return new EnrolamientoRequestDTO(cedula, vectorEmbedding);
        }
    }
}
