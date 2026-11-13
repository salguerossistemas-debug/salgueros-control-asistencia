package com.salgueros.asistencia.service;

import com.salgueros.asistencia.dto.MarcacionResponseDTO;
import com.salgueros.asistencia.model.Empleado;
import com.salgueros.asistencia.model.EstadoLlegada;
import com.salgueros.asistencia.model.ProgramacionSemanal;
import com.salgueros.asistencia.model.RegistroAsistencia;
import com.salgueros.asistencia.repository.RegistroAsistenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AsistenciaAlivioTest {

    private AsistenciaService asistenciaService;
    private RegistroAsistenciaRepository registroRepository;

    @BeforeEach
    void setUp() {
        registroRepository = Mockito.mock(RegistroAsistenciaRepository.class);
        when(registroRepository.save(any(RegistroAsistencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        asistenciaService = new AsistenciaService(
                null,
                null,
                null,
                registroRepository,
                null
        );
        asistenciaService.setAlivioEntradaMinutos(8);
    }

    @Test
    @DisplayName("Pablo programado a las 06:00 llega a las 06:00 (Puntual) -> Registra 06:00, 0 retardo, A_TIEMPO")
    void testEntradaPuntual() {
        LocalDate fecha = LocalDate.of(2026, 11, 13);
        Empleado pablo = Empleado.builder()
                .idEmpleado(1)
                .cedula("1098765432")
                .nombreApellido("Pablo Marmol")
                .area("PLANTA")
                .build();

        ProgramacionSemanal prog = ProgramacionSemanal.builder()
                .idProgramacion(10L)
                .empleado(pablo)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(6, 0))
                .horaSalidaProgramada(LocalTime.of(14, 0))
                .build();

        LocalDateTime llegada = LocalDateTime.of(fecha, LocalTime.of(6, 0));
        RegistroAsistencia reg = new RegistroAsistencia();

        MarcacionResponseDTO resp = asistenciaService.ejecutarMotorReglas("ENTRADA", prog, reg, pablo, llegada, 0.20);

        assertTrue(resp.isExito());
        assertEquals("A_TIEMPO", resp.getEstadoLlegada());
        assertEquals(0, resp.getMinutosRetardo());
        assertEquals(LocalTime.of(6, 0), reg.getHoraEntradaReal().toLocalTime());
    }

    @Test
    @DisplayName("Pablo programado a las 06:00 llega a las 06:08 (Dentro del alivio de 8 min) -> Registra 06:00, 0 retardo, A_TIEMPO")
    void testEntradaDentroDeAlivio8Minutos() {
        LocalDate fecha = LocalDate.of(2026, 11, 13);
        Empleado pablo = Empleado.builder()
                .idEmpleado(1)
                .cedula("1098765432")
                .nombreApellido("Pablo Marmol")
                .area("PLANTA")
                .build();

        ProgramacionSemanal prog = ProgramacionSemanal.builder()
                .idProgramacion(10L)
                .empleado(pablo)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(6, 0))
                .horaSalidaProgramada(LocalTime.of(14, 0))
                .build();

        // Llega exactamente 8 minutos después por carga del backend en Render
        LocalDateTime llegada = LocalDateTime.of(fecha, LocalTime.of(6, 8));
        RegistroAsistencia reg = new RegistroAsistencia();

        MarcacionResponseDTO resp = asistenciaService.ejecutarMotorReglas("ENTRADA", prog, reg, pablo, llegada, 0.22);

        assertTrue(resp.isExito());
        assertEquals("A_TIEMPO", resp.getEstadoLlegada());
        assertEquals(0, resp.getMinutosRetardo());
        // Se acerca a la hora programada: 06:00:00
        assertEquals(LocalTime.of(6, 0), reg.getHoraEntradaReal().toLocalTime());
        assertTrue(resp.getMensaje().contains("Alivio aplicado: 06:00:00"));

        // Verificación de Nómina: No descuenta tiempo, liquida las 8 horas completas
        reg.setHoraSalidaReal(LocalDateTime.of(fecha, LocalTime.of(14, 0)));
        CalculadoraNominaColombia.DetalleNominaDia detalle = CalculadoraNominaColombia.calcularDia(prog, reg);
        assertEquals(8.0, detalle.getHorasLaboradasReales(), 0.01);
        assertEquals(8.0, detalle.getHorasOrdinarias(), 0.01);
    }

    @Test
    @DisplayName("Pablo programado a las 06:00 llega a las 06:09 (9 min) -> Descuenta 8 min de alivio, 1 min retardo, hora real 06:01")
    void testEntradaPasadoAlivio9Minutos() {
        LocalDate fecha = LocalDate.of(2026, 11, 13);
        Empleado pablo = Empleado.builder()
                .idEmpleado(1)
                .cedula("1098765432")
                .nombreApellido("Pablo Marmol")
                .area("PLANTA")
                .build();

        ProgramacionSemanal prog = ProgramacionSemanal.builder()
                .idProgramacion(10L)
                .empleado(pablo)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(6, 0))
                .horaSalidaProgramada(LocalTime.of(14, 0))
                .build();

        // Llega a las 6:09 (9 minutos de diferencia)
        LocalDateTime llegada = LocalDateTime.of(fecha, LocalTime.of(6, 9));
        RegistroAsistencia reg = new RegistroAsistencia();

        MarcacionResponseDTO resp = asistenciaService.ejecutarMotorReglas("ENTRADA", prog, reg, pablo, llegada, 0.18);

        assertTrue(resp.isExito());
        assertEquals("RETARDO", resp.getEstadoLlegada());
        // 9 min de diferencia - 8 min de alivio = 1 min de retardo efectivo
        assertEquals(1, resp.getMinutosRetardo());
        assertEquals(1, reg.getMinutosRetardo());
        // Hora acercada: 06:00 + 1 min = 06:01:00
        assertEquals(LocalTime.of(6, 1), reg.getHoraEntradaReal().toLocalTime());
        assertTrue(resp.getMensaje().contains("+1 min con alivio de 8 min aplicado"));

        // Verificación de Nómina: Descuenta únicamente el 1 minuto efectivo (7 horas y 59 minutos = ~7.98 hrs)
        reg.setHoraSalidaReal(LocalDateTime.of(fecha, LocalTime.of(14, 0)));
        CalculadoraNominaColombia.DetalleNominaDia detalle = CalculadoraNominaColombia.calcularDia(prog, reg);
        assertEquals(7.98, detalle.getHorasLaboradasReales(), 0.02);
    }

    @Test
    @DisplayName("Pablo programado a las 06:00 llega a las 06:20 (20 min) -> Descuenta 8 min de alivio, 12 min retardo, hora real 06:12")
    void testEntradaTardia20Minutos() {
        LocalDate fecha = LocalDate.of(2026, 11, 13);
        Empleado pablo = Empleado.builder()
                .idEmpleado(1)
                .cedula("1098765432")
                .nombreApellido("Pablo Marmol")
                .area("PLANTA")
                .build();

        ProgramacionSemanal prog = ProgramacionSemanal.builder()
                .idProgramacion(10L)
                .empleado(pablo)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(6, 0))
                .horaSalidaProgramada(LocalTime.of(14, 0))
                .build();

        LocalDateTime llegada = LocalDateTime.of(fecha, LocalTime.of(6, 20));
        RegistroAsistencia reg = new RegistroAsistencia();

        MarcacionResponseDTO resp = asistenciaService.ejecutarMotorReglas("ENTRADA", prog, reg, pablo, llegada, 0.15);

        assertTrue(resp.isExito());
        assertEquals("RETARDO", resp.getEstadoLlegada());
        // 20 min - 8 min = 12 min retardo
        assertEquals(12, resp.getMinutosRetardo());
        // 06:00 + 12 min = 06:12:00
        assertEquals(LocalTime.of(6, 12), reg.getHoraEntradaReal().toLocalTime());
    }
}
