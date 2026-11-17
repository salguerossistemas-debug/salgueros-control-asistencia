package com.salgueros.asistencia.dto;

public class MarcacionResponseDTO {
    private boolean exito;
    private String mensaje;
    private String nombreEmpleado;
    private String cedula;
    private String tipoMarcacion;
    private String estadoLlegada;
    private int minutosRetardo;
    private String horaMarcacion;
    private double distanciaBiometrica;
    private boolean requiereConfirmacionDobleMarcacion;

    public MarcacionResponseDTO() {}

    public MarcacionResponseDTO(boolean exito, String mensaje, String nombreEmpleado, String cedula,
                                String tipoMarcacion, String estadoLlegada, int minutosRetardo,
                                String horaMarcacion, double distanciaBiometrica,
                                boolean requiereConfirmacionDobleMarcacion) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.nombreEmpleado = nombreEmpleado;
        this.cedula = cedula;
        this.tipoMarcacion = tipoMarcacion;
        this.estadoLlegada = estadoLlegada;
        this.minutosRetardo = minutosRetardo;
        this.horaMarcacion = horaMarcacion;
        this.distanciaBiometrica = distanciaBiometrica;
        this.requiereConfirmacionDobleMarcacion = requiereConfirmacionDobleMarcacion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipoMarcacion() { return tipoMarcacion; }
    public void setTipoMarcacion(String tipoMarcacion) { this.tipoMarcacion = tipoMarcacion; }

    public String getEstadoLlegada() { return estadoLlegada; }
    public void setEstadoLlegada(String estadoLlegada) { this.estadoLlegada = estadoLlegada; }

    public int getMinutosRetardo() { return minutosRetardo; }
    public void setMinutosRetardo(int minutosRetardo) { this.minutosRetardo = minutosRetardo; }

    public String getHoraMarcacion() { return horaMarcacion; }
    public void setHoraMarcacion(String horaMarcacion) { this.horaMarcacion = horaMarcacion; }

    public double getDistanciaBiometrica() { return distanciaBiometrica; }
    public void setDistanciaBiometrica(double distanciaBiometrica) { this.distanciaBiometrica = distanciaBiometrica; }

    public boolean isRequiereConfirmacionDobleMarcacion() { return requiereConfirmacionDobleMarcacion; }
    public void setRequiereConfirmacionDobleMarcacion(boolean requiereConfirmacionDobleMarcacion) { this.requiereConfirmacionDobleMarcacion = requiereConfirmacionDobleMarcacion; }

    public static class Builder {
        private boolean exito;
        private String mensaje;
        private String nombreEmpleado;
        private String cedula;
        private String tipoMarcacion;
        private String estadoLlegada;
        private int minutosRetardo;
        private String horaMarcacion;
        private double distanciaBiometrica;
        private boolean requiereConfirmacionDobleMarcacion;

        public Builder exito(boolean exito) { this.exito = exito; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }
        public Builder nombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; return this; }
        public Builder cedula(String cedula) { this.cedula = cedula; return this; }
        public Builder tipoMarcacion(String tipoMarcacion) { this.tipoMarcacion = tipoMarcacion; return this; }
        public Builder estadoLlegada(String estadoLlegada) { this.estadoLlegada = estadoLlegada; return this; }
        public Builder minutosRetardo(int minutosRetardo) { this.minutosRetardo = minutosRetardo; return this; }
        public Builder horaMarcacion(String horaMarcacion) { this.horaMarcacion = horaMarcacion; return this; }
        public Builder distanciaBiometrica(double distanciaBiometrica) { this.distanciaBiometrica = distanciaBiometrica; return this; }
        public Builder requiereConfirmacionDobleMarcacion(boolean requiereConfirmacionDobleMarcacion) { this.requiereConfirmacionDobleMarcacion = requiereConfirmacionDobleMarcacion; return this; }

        public MarcacionResponseDTO build() {
            return new MarcacionResponseDTO(exito, mensaje, nombreEmpleado, cedula, tipoMarcacion,
                    estadoLlegada, minutosRetardo, horaMarcacion, distanciaBiometrica, requiereConfirmacionDobleMarcacion);
        }
    }
}
