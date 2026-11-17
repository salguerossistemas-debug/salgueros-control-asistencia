package com.salgueros.asistencia.dto;

import com.salgueros.asistencia.model.TipoNovedad;

import java.time.LocalDate;
import java.time.LocalTime;

public class ProgramacionRequestDTO {
    
    // PIN de seguridad administrativa
    private String adminPin;

    private Integer idEmpleado;
    private String area; // null/TODOS, PLANTA, VENTAS, TRANSPORTADORES, ADMINISTRADORES

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private Integer minutosAlmuerzo = 0;
    private Boolean horasExtras;
    private TipoNovedad tipoNovedad;
    private Boolean bloqueado;

    public ProgramacionRequestDTO() {}

    public ProgramacionRequestDTO(String adminPin, Integer idEmpleado, String area, LocalDate fechaInicio,
                                  LocalDate fechaFin, LocalTime horaEntrada, LocalTime horaSalida,
                                  Integer minutosAlmuerzo, Boolean horasExtras, TipoNovedad tipoNovedad,
                                  Boolean bloqueado) {
        this.adminPin = adminPin;
        this.idEmpleado = idEmpleado;
        this.area = area;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.minutosAlmuerzo = minutosAlmuerzo != null ? minutosAlmuerzo : 0;
        this.horasExtras = horasExtras;
        this.tipoNovedad = tipoNovedad;
        this.bloqueado = bloqueado;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAdminPin() { return adminPin; }
    public void setAdminPin(String adminPin) { this.adminPin = adminPin; }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }

    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }

    public Integer getMinutosAlmuerzo() {
        return minutosAlmuerzo != null ? minutosAlmuerzo : 0;
    }
    public void setMinutosAlmuerzo(Integer minutosAlmuerzo) {
        this.minutosAlmuerzo = minutosAlmuerzo != null ? minutosAlmuerzo : 0;
    }

    public Boolean getHorasExtras() { return horasExtras; }
    public void setHorasExtras(Boolean horasExtras) { this.horasExtras = horasExtras; }

    public TipoNovedad getTipoNovedad() { return tipoNovedad; }
    public void setTipoNovedad(TipoNovedad tipoNovedad) { this.tipoNovedad = tipoNovedad; }

    public Boolean getBloqueado() { return bloqueado; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }

    public static class Builder {
        private String adminPin;
        private Integer idEmpleado;
        private String area;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private LocalTime horaEntrada;
        private LocalTime horaSalida;
        private Integer minutosAlmuerzo = 0;
        private Boolean horasExtras;
        private TipoNovedad tipoNovedad;
        private Boolean bloqueado;

        public Builder adminPin(String adminPin) { this.adminPin = adminPin; return this; }
        public Builder idEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; return this; }
        public Builder area(String area) { this.area = area; return this; }
        public Builder fechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; return this; }
        public Builder fechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; return this; }
        public Builder horaEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; return this; }
        public Builder horaSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; return this; }
        public Builder minutosAlmuerzo(Integer minutosAlmuerzo) { this.minutosAlmuerzo = minutosAlmuerzo; return this; }
        public Builder horasExtras(Boolean horasExtras) { this.horasExtras = horasExtras; return this; }
        public Builder tipoNovedad(TipoNovedad tipoNovedad) { this.tipoNovedad = tipoNovedad; return this; }
        public Builder bloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; return this; }

        public ProgramacionRequestDTO build() {
            return new ProgramacionRequestDTO(adminPin, idEmpleado, area, fechaInicio, fechaFin,
                    horaEntrada, horaSalida, minutosAlmuerzo, horasExtras, tipoNovedad, bloqueado);
        }
    }
}
