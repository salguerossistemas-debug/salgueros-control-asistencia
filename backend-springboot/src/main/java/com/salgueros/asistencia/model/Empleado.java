package com.salgueros.asistencia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "empleados")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "cedula", nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(name = "nombre_apellido", nullable = false, length = 100)
    private String nombreApellido;

    @Column(name = "area", nullable = false, length = 50)
    private String area = "PLANTA";

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    public Empleado() {}

    public Empleado(Integer idEmpleado, String cedula, String nombreApellido, String area, EstadoEmpleado estado) {
        this.idEmpleado = idEmpleado;
        this.cedula = cedula;
        this.nombreApellido = nombreApellido;
        this.area = area != null ? area : "PLANTA";
        this.estado = estado != null ? estado : EstadoEmpleado.ACTIVO;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombreApellido() { return nombreApellido; }
    public void setNombreApellido(String nombreApellido) { this.nombreApellido = nombreApellido; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public EstadoEmpleado getEstado() { return estado; }
    public void setEstado(EstadoEmpleado estado) { this.estado = estado; }

    public static class Builder {
        private Integer idEmpleado;
        private String cedula;
        private String nombreApellido;
        private String area = "PLANTA";
        private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

        public Builder idEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; return this; }
        public Builder cedula(String cedula) { this.cedula = cedula; return this; }
        public Builder nombreApellido(String nombreApellido) { this.nombreApellido = nombreApellido; return this; }
        public Builder area(String area) { this.area = area; return this; }
        public Builder estado(EstadoEmpleado estado) { this.estado = estado; return this; }

        public Empleado build() {
            return new Empleado(idEmpleado, cedula, nombreApellido, area, estado);
        }
    }
}
