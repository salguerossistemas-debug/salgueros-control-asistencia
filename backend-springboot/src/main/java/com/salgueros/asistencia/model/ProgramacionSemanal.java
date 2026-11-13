package com.salgueros.asistencia.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "programacion_semanal", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_empleado", "fecha"})
})
public class ProgramacionSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_programacion")
    private Long idProgramacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada_programada", nullable = false)
    private LocalTime horaEntradaProgramada;

    @Column(name = "hora_salida_programada")
    private LocalTime horaSalidaProgramada;

    /**
     * 0 = Jornada continua sin pausa de almuerzo.
     * Si es 0, la interfaz POS deshabilita la marcación de almuerzo.
     */
    @Column(name = "minutos_almuerzo_programados", nullable = false)
    private Integer minutosAlmuerzoProgramados = 0;

    @Column(name = "horas_extras_autorizadas", nullable = false)
    private Boolean horasExtrasAutorizadas = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_novedad_precargada", nullable = false)
    private TipoNovedad tipoNovedadPrecargada = TipoNovedad.NINGUNA;

    @Column(name = "bloqueado")
    private Boolean bloqueado = false;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    public ProgramacionSemanal() {}

    public ProgramacionSemanal(Long idProgramacion, Empleado empleado, LocalDate fecha,
                               LocalTime horaEntradaProgramada, LocalTime horaSalidaProgramada,
                               Integer minutosAlmuerzoProgramados, Boolean horasExtrasAutorizadas,
                               TipoNovedad tipoNovedadPrecargada, Boolean bloqueado,
                               LocalDateTime fechaModificacion) {
        this.idProgramacion = idProgramacion;
        this.empleado = empleado;
        this.fecha = fecha;
        this.horaEntradaProgramada = horaEntradaProgramada;
        this.horaSalidaProgramada = horaSalidaProgramada;
        this.minutosAlmuerzoProgramados = minutosAlmuerzoProgramados != null ? minutosAlmuerzoProgramados : 0;
        this.horasExtrasAutorizadas = horasExtrasAutorizadas != null ? horasExtrasAutorizadas : false;
        this.tipoNovedadPrecargada = tipoNovedadPrecargada != null ? tipoNovedadPrecargada : TipoNovedad.NINGUNA;
        this.bloqueado = bloqueado != null ? bloqueado : false;
        this.fechaModificacion = fechaModificacion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getIdProgramacion() { return idProgramacion; }
    public void setIdProgramacion(Long idProgramacion) { this.idProgramacion = idProgramacion; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraEntradaProgramada() { return horaEntradaProgramada; }
    public void setHoraEntradaProgramada(LocalTime horaEntradaProgramada) { this.horaEntradaProgramada = horaEntradaProgramada; }

    public LocalTime getHoraSalidaProgramada() { return horaSalidaProgramada; }
    public void setHoraSalidaProgramada(LocalTime horaSalidaProgramada) { this.horaSalidaProgramada = horaSalidaProgramada; }

    public Integer getMinutosAlmuerzoProgramados() {
        return minutosAlmuerzoProgramados != null ? minutosAlmuerzoProgramados : 0;
    }
    public void setMinutosAlmuerzoProgramados(Integer minutosAlmuerzoProgramados) {
        this.minutosAlmuerzoProgramados = minutosAlmuerzoProgramados != null ? minutosAlmuerzoProgramados : 0;
    }

    public Integer getMinutosAlmuerzo() {
        return minutosAlmuerzoProgramados != null ? minutosAlmuerzoProgramados : 0;
    }
    public void setMinutosAlmuerzo(Integer minutosAlmuerzo) {
        this.minutosAlmuerzoProgramados = minutosAlmuerzo != null ? minutosAlmuerzo : 0;
    }

    public Boolean getHorasExtrasAutorizadas() {
        return horasExtrasAutorizadas != null ? horasExtrasAutorizadas : false;
    }
    public void setHorasExtrasAutorizadas(Boolean horasExtrasAutorizadas) {
        this.horasExtrasAutorizadas = horasExtrasAutorizadas != null ? horasExtrasAutorizadas : false;
    }

    public TipoNovedad getTipoNovedadPrecargada() {
        return tipoNovedadPrecargada != null ? tipoNovedadPrecargada : TipoNovedad.NINGUNA;
    }
    public void setTipoNovedadPrecargada(TipoNovedad tipoNovedadPrecargada) {
        this.tipoNovedadPrecargada = tipoNovedadPrecargada != null ? tipoNovedadPrecargada : TipoNovedad.NINGUNA;
    }

    public Boolean getBloqueado() {
        return bloqueado != null ? bloqueado : false;
    }
    public void setBloqueado(Boolean bloqueado) {
        this.bloqueado = bloqueado != null ? bloqueado : false;
    }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public static class Builder {
        private Long idProgramacion;
        private Empleado empleado;
        private LocalDate fecha;
        private LocalTime horaEntradaProgramada;
        private LocalTime horaSalidaProgramada;
        private Integer minutosAlmuerzoProgramados = 0;
        private Boolean horasExtrasAutorizadas = false;
        private TipoNovedad tipoNovedadPrecargada = TipoNovedad.NINGUNA;
        private Boolean bloqueado = false;
        private LocalDateTime fechaModificacion;

        public Builder idProgramacion(Long idProgramacion) { this.idProgramacion = idProgramacion; return this; }
        public Builder empleado(Empleado empleado) { this.empleado = empleado; return this; }
        public Builder fecha(LocalDate fecha) { this.fecha = fecha; return this; }
        public Builder horaEntradaProgramada(LocalTime horaEntradaProgramada) { this.horaEntradaProgramada = horaEntradaProgramada; return this; }
        public Builder horaSalidaProgramada(LocalTime horaSalidaProgramada) { this.horaSalidaProgramada = horaSalidaProgramada; return this; }
        public Builder minutosAlmuerzoProgramados(Integer minutosAlmuerzoProgramados) { this.minutosAlmuerzoProgramados = minutosAlmuerzoProgramados; return this; }
        public Builder horasExtrasAutorizadas(Boolean horasExtrasAutorizadas) { this.horasExtrasAutorizadas = horasExtrasAutorizadas; return this; }
        public Builder tipoNovedadPrecargada(TipoNovedad tipoNovedadPrecargada) { this.tipoNovedadPrecargada = tipoNovedadPrecargada; return this; }
        public Builder bloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; return this; }
        public Builder fechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; return this; }

        public ProgramacionSemanal build() {
            return new ProgramacionSemanal(idProgramacion, empleado, fecha, horaEntradaProgramada,
                    horaSalidaProgramada, minutosAlmuerzoProgramados, horasExtrasAutorizadas,
                    tipoNovedadPrecargada, bloqueado, fechaModificacion);
        }
    }
}
