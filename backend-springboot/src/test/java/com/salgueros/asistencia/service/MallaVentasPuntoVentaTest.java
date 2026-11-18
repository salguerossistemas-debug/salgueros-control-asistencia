package com.salgueros.asistencia.service;

import com.salgueros.asistencia.dto.MarcacionResponseDTO;
import com.salgueros.asistencia.model.*;
import com.salgueros.asistencia.repository.EmpleadoRepository;
import com.salgueros.asistencia.repository.ProgramacionSemanalRepository;
import com.salgueros.asistencia.repository.RegistroAsistenciaRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MallaVentasPuntoVentaTest {

    private EmpleadoRepository empleadoRepository;
    private ProgramacionSemanalRepository programacionRepository;
    private RegistroAsistenciaRepository registroRepository;
    private AsistenciaService asistenciaService;
    private ExcelReporteService excelReporteService;

    private Empleado empVentas;
    private Empleado empPlanta;

    @BeforeEach
    void setUp() {
        empleadoRepository = Mockito.mock(EmpleadoRepository.class);
        programacionRepository = Mockito.mock(ProgramacionSemanalRepository.class);
        registroRepository = Mockito.mock(RegistroAsistenciaRepository.class);

        asistenciaService = new AsistenciaService(
                empleadoRepository, null, programacionRepository, registroRepository, null
        );
        excelReporteService = new ExcelReporteService(
                registroRepository, programacionRepository, empleadoRepository
        );

        empVentas = Empleado.builder()
                .idEmpleado(10)
                .cedula("1013582352")
                .nombreApellido("PAULA DANIELA GONZALES RIOS")
                .area("VENTAS")
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        empPlanta = Empleado.builder()
                .idEmpleado(20)
                .cedula("1065813453")
                .nombreApellido("LORENA YUSLIETH RODRIGUEZ SAUCEDO")
                .area("PLANTA")
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empVentas));
        when(empleadoRepository.findById(20)).thenReturn(Optional.of(empPlanta));
        when(programacionRepository.save(any(ProgramacionSemanal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registroRepository.save(any(RegistroAsistencia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Debe guardar puntoVenta únicamente para empleados de VENTAS en días laborales")
    void testGuardarMatrizPuntoVentaSoloVentas() {
        LocalDate fecha = LocalDate.of(2026, 11, 16);
        when(programacionRepository.findByEmpleadoAndFecha(any(), any())).thenReturn(Optional.empty());

        List<Map<String, Object>> turnos = List.of(
                Map.of(
                        "idEmpleado", 10,
                        "fecha", fecha.toString(),
                        "tipo", "TURNO",
                        "horaEntrada", "08:00:00",
                        "horaSalida", "17:00:00",
                        "minutosAlmuerzo", 60,
                        "tipoNovedad", "NINGUNA",
                        "puntoVenta", "BULEVAR"
                ),
                Map.of(
                        "idEmpleado", 20,
                        "fecha", fecha.toString(),
                        "tipo", "TURNO",
                        "horaEntrada", "06:00:00",
                        "horaSalida", "14:30:00",
                        "minutosAlmuerzo", 30,
                        "tipoNovedad", "NINGUNA",
                        "puntoVenta", "BULEVAR"
                ),
                Map.of(
                        "idEmpleado", 10,
                        "fecha", fecha.plusDays(1).toString(),
                        "tipo", "NOVEDAD",
                        "horaEntrada", "08:00:00",
                        "horaSalida", "17:00:00",
                        "minutosAlmuerzo", 0,
                        "tipoNovedad", "PERMISO",
                        "puntoVenta", "COUNTRY"
                )
        );

        Map<String, Object> body = new HashMap<>();
        body.put("bloqueado", true);
        body.put("turnos", turnos);

        Map<String, Object> res = asistenciaService.guardarMatrizSemanal(body);
        assertEquals(true, res.get("exito"));

        ArgumentCaptor<ProgramacionSemanal> captor = ArgumentCaptor.forClass(ProgramacionSemanal.class);
        verify(programacionRepository, times(3)).save(captor.capture());

        List<ProgramacionSemanal> guardados = captor.getAllValues();
        assertEquals("BULEVAR", guardados.get(0).getPuntoVenta());
        assertNull(guardados.get(1).getPuntoVenta(), "PLANTA no debe recibir punto de venta");
        assertNull(guardados.get(2).getPuntoVenta(), "Días con novedad no deben conservar punto de venta");
    }

    @Test
    @DisplayName("La asignación de puntoVenta no altera cálculos de nómina ni reglas de marcación")
    void testPuntoVentaNoInterfiereConNominaNiMarcacion() {
        LocalDate fecha = LocalDate.of(2026, 11, 16);

        ProgramacionSemanal progSinPunto = ProgramacionSemanal.builder()
                .idProgramacion(1L)
                .empleado(empVentas)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(8, 0))
                .horaSalidaProgramada(LocalTime.of(17, 0))
                .minutosAlmuerzoProgramados(60)
                .horasExtrasAutorizadas(false)
                .tipoNovedadPrecargada(TipoNovedad.NINGUNA)
                .puntoVenta(null)
                .build();

        ProgramacionSemanal progConPunto = ProgramacionSemanal.builder()
                .idProgramacion(2L)
                .empleado(empVentas)
                .fecha(fecha)
                .horaEntradaProgramada(LocalTime.of(8, 0))
                .horaSalidaProgramada(LocalTime.of(17, 0))
                .minutosAlmuerzoProgramados(60)
                .horasExtrasAutorizadas(false)
                .tipoNovedadPrecargada(TipoNovedad.NINGUNA)
                .puntoVenta("CAFAM FLORESTA")
                .build();

        LocalDateTime llegada = LocalDateTime.of(fecha, LocalTime.of(8, 5)); // dentro de alivio 8 min
        RegistroAsistencia regSin = new RegistroAsistencia();
        RegistroAsistencia regCon = new RegistroAsistencia();

        MarcacionResponseDTO marcSin = asistenciaService.ejecutarMotorReglas("ENTRADA", progSinPunto, regSin, empVentas, llegada, 0.21);
        MarcacionResponseDTO marcCon = asistenciaService.ejecutarMotorReglas("ENTRADA", progConPunto, regCon, empVentas, llegada, 0.21);

        assertEquals(marcSin.isExito(), marcCon.isExito());
        assertEquals(marcSin.getEstadoLlegada(), marcCon.getEstadoLlegada());
        assertEquals(marcSin.getMinutosRetardo(), marcCon.getMinutosRetardo());
        assertEquals(marcSin.getMensaje(), marcCon.getMensaje());

        regSin.setHoraSalidaReal(LocalDateTime.of(fecha, LocalTime.of(17, 0)));
        regCon.setHoraSalidaReal(LocalDateTime.of(fecha, LocalTime.of(17, 0)));

        CalculadoraNominaColombia.DetalleNominaDia nomSin = CalculadoraNominaColombia.calcularDia(progSinPunto, regSin);
        CalculadoraNominaColombia.DetalleNominaDia nomCon = CalculadoraNominaColombia.calcularDia(progConPunto, regCon);

        assertEquals(nomSin.getHorasProgramadas(), nomCon.getHorasProgramadas(), 0.001);
        assertEquals(nomSin.getHorasLaboradasReales(), nomCon.getHorasLaboradasReales(), 0.001);
        assertEquals(nomSin.getHorasOrdinarias(), nomCon.getHorasOrdinarias(), 0.001);
        assertEquals(nomSin.getTotalHorasAPagar(), nomCon.getTotalHorasAPagar(), 0.001);
        assertEquals(nomSin.getNovedadEstado(), nomCon.getNovedadEstado());
    }

    @Test
    @DisplayName("Excel de Malla Semanal incluye punto de venta en Ventas y sección estática de direcciones por zona")
    void testExcelMallaSemanalIncluyePuntosYDirecciones() throws Exception {
        LocalDate lunes = LocalDate.of(2026, 11, 16);
        LocalDate domingo = lunes.plusDays(6);

        ProgramacionSemanal progVentas = ProgramacionSemanal.builder()
                .idProgramacion(100L)
                .empleado(empVentas)
                .fecha(lunes)
                .horaEntradaProgramada(LocalTime.of(8, 0))
                .horaSalidaProgramada(LocalTime.of(17, 0))
                .minutosAlmuerzoProgramados(60)
                .tipoNovedadPrecargada(TipoNovedad.NINGUNA)
                .puntoVenta("PLAZA 1")
                .build();

        when(empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO)).thenReturn(new ArrayList<>(List.of(empVentas, empPlanta)));
        when(programacionRepository.findProgramacionOrdenada(lunes, domingo)).thenReturn(List.of(progVentas));

        byte[] xlsxBytes = excelReporteService.generarMatrizSemanalExcel(lunes, "VENTAS");
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet mainSheet = wb.getSheet("Malla Horarios Semanales");
            assertNotNull(mainSheet);

            List<String> textos = new ArrayList<>();
            DataFormatter fmt = new DataFormatter();
            for (Row row : mainSheet) {
                for (Cell cell : row) {
                    String val = fmt.formatCellValue(cell);
                    if (!val.isBlank()) textos.add(val);
                }
            }

            String joined = String.join("\n", textos);
            assertTrue(joined.contains("PAULA DANIELA GONZALES RIOS"));
            assertFalse(joined.contains("LORENA YUSLIETH RODRIGUEZ SAUCEDO"), "Filtro VENTAS no debe incluir PLANTA");
            assertTrue(joined.contains("08:00 - 17:00\n📍 PLAZA 1"));

            // Verificar guía de direcciones exacta al pie de la hoja
            assertTrue(joined.contains("ZONA SUR"));
            assertTrue(joined.contains("1. DRIVER PLAZA: transversal 99 # 70a-89"));
            assertTrue(joined.contains("2. BAZAAR: CALLE 12B # 71D-61"));
            assertTrue(joined.contains("3. COLSEGUROS: CARRERA 10 # 17-72"));
            assertTrue(joined.contains("4. PLAZA 1: carrera 65 n° 11-50 local 211"));
            assertTrue(joined.contains("5. PLAZA 2: carrera 65 n° 11-50 local 283"));

            assertTrue(joined.contains("ZONA NORTE"));
            assertTrue(joined.contains("1. BULEVAR: Carrera 58 # 127-59"));
            assertTrue(joined.contains("2. PORTAL 80: Carrera 68 No. 90-88"));
            assertTrue(joined.contains("3. COUNTRY: Calle 134 No. 9-51"));
            assertTrue(joined.contains("4. CAFAM FLORESTA: Transversal 100a No. 80a-20"));
            assertTrue(joined.contains("5. MAZUREN: Cra 46 #150-46"));
        }
    }
}
