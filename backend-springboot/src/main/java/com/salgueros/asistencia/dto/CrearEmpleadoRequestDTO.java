package com.salgueros.asistencia.dto;

public class CrearEmpleadoRequestDTO {
    private String adminPin;
    private String cedula;
    private String nombreApellido;
    private String area;

    public CrearEmpleadoRequestDTO() {}

    public CrearEmpleadoRequestDTO(String adminPin, String cedula, String nombreApellido, String area) {
        this.adminPin = adminPin;
        this.cedula = cedula;
        this.nombreApellido = nombreApellido;
        this.area = area;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAdminPin() { return adminPin; }
    public void setAdminPin(String adminPin) { this.adminPin = adminPin; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombreApellido() { return nombreApellido; }
    public void setNombreApellido(String nombreApellido) { this.nombreApellido = nombreApellido; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public static class Builder {
        private String adminPin;
        private String cedula;
        private String nombreApellido;
        private String area;

        public Builder adminPin(String adminPin) { this.adminPin = adminPin; return this; }
        public Builder cedula(String cedula) { this.cedula = cedula; return this; }
        public Builder nombreApellido(String nombreApellido) { this.nombreApellido = nombreApellido; return this; }
        public Builder area(String area) { this.area = area; return this; }

        public CrearEmpleadoRequestDTO build() {
            return new CrearEmpleadoRequestDTO(adminPin, cedula, nombreApellido, area);
        }
    }
}
