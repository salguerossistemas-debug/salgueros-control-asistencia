package com.salgueros.asistencia.dto;

public class ContingenciaRequestDTO {
    // PIN privado exclusivo (2586)
    private String pinContingencia;
    
    // ID del empleado que reportaron en el correo
    private Long idEmpleado;
    
    // Acción: "JORNADA_COMPLETA", "ENTRADA" o "SALIDA"
    private String tipoAccion;
    
    // Opcional: si quieres forzar una hora manual ej: "06:15", o null para usar su turno programado
    private String horaManual;
    
    // Justificación de auditoría
    private String observacion;

    public ContingenciaRequestDTO() {}

    public String getPinContingencia() { return pinContingencia; }
    public void setPinContingencia(String pinContingencia) { this.pinContingencia = pinContingencia; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getTipoAccion() { return tipoAccion; }
    public void setTipoAccion(String tipoAccion) { this.tipoAccion = tipoAccion; }

    public String getHoraManual() { return horaManual; }
    public void setHoraManual(String horaManual) { this.horaManual = horaManual; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}