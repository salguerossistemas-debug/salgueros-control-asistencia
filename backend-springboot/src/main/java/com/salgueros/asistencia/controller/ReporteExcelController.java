package com.salgueros.asistencia.controller;

import com.salgueros.asistencia.service.ExcelReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping({"/api/reportes", "/api/reportes/asistencia", "/api/asistencia/reportes"})
public class ReporteExcelController {

    private final ExcelReporteService excelReporteService;

    public ReporteExcelController(ExcelReporteService excelReporteService) {
        this.excelReporteService = excelReporteService;
    }

    /**
     * Endpoint 1: Malla Semanal de Horarios (Lunes a Domingo)
     * GET /api/reportes/excel/malla-horarios?fechaInicio=YYYY-MM-DD
     */
    @GetMapping({"/excel/malla-horarios", "/asistencia/excel/malla-horarios", "/excel/matriz-semanal", "/asistencia/excel/matriz-semanal", "/malla-horarios"})
    public ResponseEntity<byte[]> descargarMallaHorarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicioSemana) {

        LocalDate fechaBase = fechaInicio != null ? fechaInicio : (fechaInicioSemana != null ? fechaInicioSemana : LocalDate.now());
        int dayVal = fechaBase.getDayOfWeek().getValue();
        LocalDate lunes = fechaBase.minusDays(dayVal - 1);

        try {
            byte[] excelBytes = excelReporteService.generarMatrizSemanalExcel(lunes);
            String filename = "malla-horarios-semana-" + lunes + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint 2: Liquidación de Nómina, Asistencia y Horas Extras (Colombia)
     * GET /api/reportes/excel/nomina-asistencia?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD
     */
    @GetMapping({"/excel/nomina-asistencia", "/asistencia/excel/nomina-asistencia", "/asistencia/excel", "/excel", "/nomina-asistencia"})
    public ResponseEntity<byte[]> descargarNominaAsistencia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String periodo) {

        if (fechaInicio == null) fechaInicio = LocalDate.now().minusDays(7);
        if (fechaFin == null) fechaFin = LocalDate.now();

        try {
            byte[] excelBytes = excelReporteService.generarNominaAsistenciaExcel(fechaInicio, fechaFin);
            String filename = "reporte-nomina-asistencia-" + fechaInicio + "-al-" + fechaFin + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint 3: Inventario Total de Cierre Quincenal / Mensual (Cortes Día 15 y Día 29 -> Reinicio Día 16 y Día 30)
     * GET /api/reportes/excel/inventario-corte?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD
     */
    @GetMapping({"/excel/inventario-corte", "/asistencia/excel/inventario-corte", "/inventario-corte"})
    public ResponseEntity<byte[]> descargarInventarioCorte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        if (fechaInicio == null) fechaInicio = LocalDate.now().withDayOfMonth(1);
        if (fechaFin == null) fechaFin = LocalDate.now();

        try {
            byte[] excelBytes = excelReporteService.generarInventarioCorteExcel(fechaInicio, fechaFin);
            String filename = "inventario-total-corte-" + fechaInicio + "-al-" + fechaFin + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
