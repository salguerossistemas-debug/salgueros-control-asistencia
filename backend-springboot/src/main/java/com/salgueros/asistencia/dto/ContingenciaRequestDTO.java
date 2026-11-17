package com.salgueros.asistencia.dto;

public class ContingenciaRequestDTO {
    // PIN privado exclusivo (2586)
    private String pinContingencia;
    
    // ID del empleado que reportaron en el correo
    private Long idEmpleado;
    
    // Fecha a justificar (YYYY-MM-DD), null para hoy
    private String fecha;

    // Acción: "JORNADA_COMPLETA", "ENTRADA" o "SALIDA"
    private String tipoAccion;
    
    // Opcional: horas manuales de entrada y salida
    private String horaManual;
    private String horaEntradaManual;
    private String horaSalidaManual;

    // Minutos de descanso / almuerzo (ej: 0, 30, 45, 60)
    private Integer minutosAlmuerzo;
    
    // Justificación de auditoría
    private String observacion;

    public ContingenciaRequestDTO() {}

    public String getPinContingencia() { return pinContingencia; }
    public void setPinContingencia(String pinContingencia) { this.pinContingencia = pinContingencia; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getTipoAccion() { return tipoAccion; }
    public void setTipoAccion(String tipoAccion) { this.tipoAccion = tipoAccion; }

    public String getHoraManual() { return horaManual; }
    public void setHoraManual(String horaManual) { this.horaManual = horaManual; }

    public String getHoraEntradaManual() { return horaEntradaManual; }
    public void setHoraEntradaManual(String horaEntradaManual) { this.horaEntradaManual = horaEntradaManual; }

    public String getHoraSalidaManual() { return horaSalidaManual; }
    public void setHoraSalidaManual(String horaSalidaManual) { this.horaSalidaManual = horaSalidaManual; }

    public Integer getMinutosAlmuerzo() { return minutosAlmuerzo; }
    public void setMinutosAlmuerzo(Integer minutosAlmuerzo) { this.minutosAlmuerzo = minutosAlmuerzo; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}