package com.salgueros.asistencia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_asistencia")
public class RegistroAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long idRegistro;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programacion", nullable = false, unique = true)
    private ProgramacionSemanal programacion;

    @Column(name = "hora_entrada_real", nullable = false)
    private LocalDateTime horaEntradaReal;

    @Column(name = "hora_salida_almuerzo")
    private LocalDateTime horaSalidaAlmuerzo;

    @Column(name = "hora_regreso_almuerzo")
    private LocalDateTime horaRegresoAlmuerzo;

    @Column(name = "hora_salida_real")
    private LocalDateTime horaSalidaReal;

    @Column(name = "minutos_retardo", nullable = false)
    private Integer minutosRetardo = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_llegada", nullable = false)
    private EstadoLlegada estadoLlegada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_dia", nullable = false)
    private EstadoDia estadoDia = EstadoDia.EN_CURSO;

    public RegistroAsistencia() {}

    public RegistroAsistencia(Long idRegistro, ProgramacionSemanal programacion, LocalDateTime horaEntradaReal,
                              LocalDateTime horaSalidaAlmuerzo, LocalDateTime horaRegresoAlmuerzo,
                              LocalDateTime horaSalidaReal, Integer minutosRetardo, EstadoLlegada estadoLlegada,
                              EstadoDia estadoDia) {
        this.idRegistro = idRegistro;
        this.programacion = programacion;
        this.horaEntradaReal = horaEntradaReal;
        this.horaSalidaAlmuerzo = horaSalidaAlmuerzo;
        this.horaRegresoAlmuerzo = horaRegresoAlmuerzo;
        this.horaSalidaReal = horaSalidaReal;
        this.minutosRetardo = minutosRetardo != null ? minutosRetardo : 0;
        this.estadoLlegada = estadoLlegada;
        this.estadoDia = estadoDia != null ? estadoDia : EstadoDia.EN_CURSO;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public ProgramacionSemanal getProgramacion() { return programacion; }
    public void setProgramacion(ProgramacionSemanal programacion) { this.programacion = programacion; }

    public LocalDateTime getHoraEntradaReal() { return horaEntradaReal; }
    public void setHoraEntradaReal(LocalDateTime horaEntradaReal) { this.horaEntradaReal = horaEntradaReal; }

    public LocalDateTime getHoraSalidaAlmuerzo() { return horaSalidaAlmuerzo; }
    public void setHoraSalidaAlmuerzo(LocalDateTime horaSalidaAlmuerzo) { this.horaSalidaAlmuerzo = horaSalidaAlmuerzo; }

    public LocalDateTime getHoraRegresoAlmuerzo() { return horaRegresoAlmuerzo; }
    public void setHoraRegresoAlmuerzo(LocalDateTime horaRegresoAlmuerzo) { this.horaRegresoAlmuerzo = horaRegresoAlmuerzo; }

    public LocalDateTime getHoraSalidaReal() { return horaSalidaReal; }
    public void setHoraSalidaReal(LocalDateTime horaSalidaReal) { this.horaSalidaReal = horaSalidaReal; }

    public Integer getMinutosRetardo() {
        return minutosRetardo != null ? minutosRetardo : 0;
    }
    public void setMinutosRetardo(Integer minutosRetardo) {
        this.minutosRetardo = minutosRetardo != null ? minutosRetardo : 0;
    }

    public EstadoLlegada getEstadoLlegada() { return estadoLlegada; }
    public void setEstadoLlegada(EstadoLlegada estadoLlegada) { this.estadoLlegada = estadoLlegada; }

    public EstadoDia getEstadoDia() { return estadoDia; }
    public void setEstadoDia(EstadoDia estadoDia) { this.estadoDia = estadoDia; }

    public static class Builder {
        private Long idRegistro;
        private ProgramacionSemanal programacion;
        private LocalDateTime horaEntradaReal;
        private LocalDateTime horaSalidaAlmuerzo;
        private LocalDateTime horaRegresoAlmuerzo;
        private LocalDateTime horaSalidaReal;
        private Integer minutosRetardo = 0;
        private EstadoLlegada estadoLlegada;
        private EstadoDia estadoDia = EstadoDia.EN_CURSO;

        public Builder idRegistro(Long idRegistro) { this.idRegistro = idRegistro; return this; }
        public Builder programacion(ProgramacionSemanal programacion) { this.programacion = programacion; return this; }
        public Builder horaEntradaReal(LocalDateTime horaEntradaReal) { this.horaEntradaReal = horaEntradaReal; return this; }
        public Builder horaSalidaAlmuerzo(LocalDateTime horaSalidaAlmuerzo) { this.horaSalidaAlmuerzo = horaSalidaAlmuerzo; return this; }
        public Builder horaRegresoAlmuerzo(LocalDateTime horaRegresoAlmuerzo) { this.horaRegresoAlmuerzo = horaRegresoAlmuerzo; return this; }
        public Builder horaSalidaReal(LocalDateTime horaSalidaReal) { this.horaSalidaReal = horaSalidaReal; return this; }
        public Builder minutosRetardo(Integer minutosRetardo) { this.minutosRetardo = minutosRetardo; return this; }
        public Builder estadoLlegada(EstadoLlegada estadoLlegada) { this.estadoLlegada = estadoLlegada; return this; }
        public Builder estadoDia(EstadoDia estadoDia) { this.estadoDia = estadoDia; return this; }

        public RegistroAsistencia build() {
            return new RegistroAsistencia(idRegistro, programacion, horaEntradaReal, horaSalidaAlmuerzo,
                    horaRegresoAlmuerzo, horaSalidaReal, minutosRetardo, estadoLlegada, estadoDia);
        }
    }
}
