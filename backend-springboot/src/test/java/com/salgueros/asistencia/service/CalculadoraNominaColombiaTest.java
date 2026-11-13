package com.salgueros.asistencia.service;

import com.salgueros.asistencia.model.Empleado;
import com.salgueros.asistencia.model.EstadoLlegada;
import com.salgueros.asistencia.model.ProgramacionSemanal;
import com.salgueros.asistencia.model.RegistroAsistencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraNominaColombiaTest {

    @Test
    @DisplayName("Debe detectar domingos y festivos colombianos correctamente")
    void testDominicalesYFestivos() {
        // Domingo
        LocalDate domingo = LocalDate.of(2026, 11, 15);
        assertTrue(CalculadoraNominaColombia.esDominicalOFestivo(domingo));

        // Año Nuevo (1 de Enero) - Festivo
        LocalDate anoNuevo = LocalDate.of(2026, 1, 1);
        assertTrue(CalculadoraNominaColombia.esDominicalOFestivo(anoNuevo));

        // Día ordinario (ej: Miércoles 11 de Noviembre 2026 no festivo)
        LocalDate diaHabil = LocalDate.of(2026, 11, 11);
        // Si no está en FESTIVOS_COLOMBIA, no es festivo
        assertFalse(CalculadoraNominaColombia.esDominicalOFestivo(LocalDate.of(2026, 11, 10)));
    }

    @Test
    @DisplayName("Debe calcular turno diurno estándar de 8 horas")
    void testCalculoTurnoDiurnoOrdinario() {
        LocalDate fecha = LocalDate.of(2026, 11, 10); // Martes
        Empleado emp = Empleado.builder()
                .idEmpleado(1)
                .cedula("123456789")
                .nombreApellido("Juan Perez")
                .area("PLANTA")
                .build();

        ProgramacionSemanal prog = ProgramacionSemanal.builder()
                .idProgramacion(100L)
                .empleado(emp)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(8, 0))
                .horaSalidaProgramada(LocalTime.of(17, 0))
                .minutosAlmuerzoProgramados(60)
                .horasExtrasAutorizadas(false)
                .build();

        RegistroAsistencia reg = RegistroAsistencia.builder()
                .idRegistro(200L)
                .programacion(prog)
                .horaEntradaReal(LocalDateTime.of(fecha, LocalTime.of(8, 0)))
                .horaSalidaAlmuerzo(LocalDateTime.of(fecha, LocalTime.of(12, 0)))
                .horaRegresoAlmuerzo(LocalDateTime.of(fecha, LocalTime.of(13, 0)))
                .horaSalidaReal(LocalDateTime.of(fecha, LocalTime.of(17, 0)))
                .estadoLlegada(EstadoLlegada.A_TIEMPO)
                .minutosRetardo(0)
                .build();

        CalculadoraNominaColombia.DetalleNominaDia detalle = CalculadoraNominaColombia.calcularDia(prog, reg);

        assertNotNull(detalle);
        assertEquals("123456789", detalle.getCedula());
        assertEquals("Juan Perez", detalle.getEmpleado());
        assertEquals(8.0, detalle.getHorasLaboradasReales(), 0.01);
        assertEquals(8.0, detalle.getHorasOrdinarias(), 0.01);
        assertEquals(0.0, detalle.getRecargoNocturno(), 0.01);
        assertEquals(0.0, detalle.getHorasExtraDiurna(), 0.01);
    }

    @Test
    @DisplayName("Llegada temprana y horario corrido con almuerzo cumplido no debe generar tiempo extra ficticio")
    void testHorarioNormalSinTiempoExtraFicticio() {
        LocalDate fecha = LocalDate.of(2026, 11, 12);
        Empleado lorena = Empleado.builder()
                .idEmpleado(2)
                .cedula("1065813453")
                .nombreApellido("LORENA YUSLIETH RODRIGUEZ SAUCEDO")
                .area("PLANTA")
                .build();

        ProgramacionSemanal prog = ProgramacionSemanal.builder()
                .idProgramacion(101L)
                .empleado(lorena)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(6, 0))
                .horaSalidaProgramada(LocalTime.of(14, 30))
                .minutosAlmuerzoProgramados(30)
                .horasExtrasAutorizadas(false)
                .build();

        // Caso real: entra 06:11:31, almuerza 12:00:04 a 12:29:42 (29m 38s -> 30m cumplidos), sale 14:42:26
        RegistroAsistencia reg = RegistroAsistencia.builder()
                .idRegistro(15L)
                .programacion(prog)
                .horaEntradaReal(LocalDateTime.of(fecha, LocalTime.of(6, 11, 31)))
                .horaSalidaAlmuerzo(LocalDateTime.of(fecha, LocalTime.of(12, 0, 4)))
                .horaRegresoAlmuerzo(LocalDateTime.of(fecha, LocalTime.of(12, 29, 42)))
                .horaSalidaReal(LocalDateTime.of(fecha, LocalTime.of(14, 42, 26)))
                .estadoLlegada(EstadoLlegada.RETARDO)
                .minutosRetardo(11)
                .build();

        CalculadoraNominaColombia.DetalleNominaDia detalle = CalculadoraNominaColombia.calcularDia(prog, reg);

        assertEquals(LocalTime.of(6, 3), detalle.getEntradaReal().toLocalTime());
        assertEquals(30, detalle.getMinutosAlmuerzo());
        assertEquals(8.0, detalle.getHorasLaboradasReales(), 0.01);
        assertEquals(8.0, detalle.getHorasOrdinarias(), 0.01);
        assertEquals(0.0, detalle.getHorasExtraDiurna(), 0.01);
        assertEquals(8.0, detalle.getTotalHorasAPagar(), 0.01);
    }
}
