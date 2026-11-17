package com.salgueros.asistencia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vectores_faciales")
public class VectorFacial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vector")
    private Integer idVector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    /**
     * Array JSON de 128 flotantes numéricos que representan la biometría facial.
     * RNF-03: Nunca se almacenan archivos JPG/PNG, solo este vector matemático.
     */
    @Column(name = "vector_embedding", columnDefinition = "TEXT", nullable = false)
    private String vectorEmbedding;

    public VectorFacial() {}

    public VectorFacial(Integer idVector, Empleado empleado, String vectorEmbedding) {
        this.idVector = idVector;
        this.empleado = empleado;
        this.vectorEmbedding = vectorEmbedding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getIdVector() { return idVector; }
    public void setIdVector(Integer idVector) { this.idVector = idVector; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public String getVectorEmbedding() { return vectorEmbedding; }
    public void setVectorEmbedding(String vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; }

    public static class Builder {
        private Integer idVector;
        private Empleado empleado;
        private String vectorEmbedding;

        public Builder idVector(Integer idVector) { this.idVector = idVector; return this; }
        public Builder empleado(Empleado empleado) { this.empleado = empleado; return this; }
        public Builder vectorEmbedding(String vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; return this; }

        public VectorFacial build() {
            return new VectorFacial(idVector, empleado, vectorEmbedding);
        }
    }
}
