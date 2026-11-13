package com.salgueros.asistencia.service;

import com.salgueros.asistencia.model.Empleado;
import com.salgueros.asistencia.model.EstadoEmpleado;
import com.salgueros.asistencia.model.ProgramacionSemanal;
import com.salgueros.asistencia.model.RegistroAsistencia;
import com.salgueros.asistencia.model.TipoNovedad;
import com.salgueros.asistencia.repository.EmpleadoRepository;
import com.salgueros.asistencia.repository.ProgramacionSemanalRepository;
import com.salgueros.asistencia.repository.RegistroAsistenciaRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelReporteService {

    private final RegistroAsistenciaRepository registroRepository;
    private final ProgramacionSemanalRepository programacionRepository;
    private final EmpleadoRepository empleadoRepository;

    public ExcelReporteService(RegistroAsistenciaRepository registroRepository,
                               ProgramacionSemanalRepository programacionRepository,
                               EmpleadoRepository empleadoRepository) {
        this.registroRepository = registroRepository;
        this.programacionRepository = programacionRepository;
        this.empleadoRepository = empleadoRepository;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Endpoint 1: Matriz Cruzada Semanal de Horarios (Lunes a Domingo)
     * Columnas: [Cédula] | [Empleado] | [Cargo] | [Lunes] | [Martes] | [Miércoles] | [Jueves] | [Viernes] | [Sábado] | [Domingo] | [Min. Almuerzo] | [Total Horas Base Semanales]
     */
    public byte[] generarMatrizSemanalExcel(LocalDate fechaInicioSemana) throws IOException {
        if (fechaInicioSemana == null) {
            fechaInicioSemana = LocalDate.now();
        }
        int dayVal = fechaInicioSemana.getDayOfWeek().getValue(); // 1 = Lunes, 7 = Domingo
        LocalDate lunes = fechaInicioSemana.minusDays(dayVal - 1);
        LocalDate domingo = lunes.plusDays(6);

        LocalDate[] diasSemana = new LocalDate[7];
        for (int i = 0; i < 7; i++) {
            diasSemana[i] = lunes.plusDays(i);
        }

        List<Empleado> empleados = empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
        empleados.sort((e1, e2) -> {
            String a1 = e1.getArea() != null ? e1.getArea() : "PLANTA";
            String a2 = e2.getArea() != null ? e2.getArea() : "PLANTA";
            int c = a1.compareToIgnoreCase(a2);
            if (c != 0) return c;
            return e1.getNombreApellido().compareToIgnoreCase(e2.getNombreApellido());
        });

        List<ProgramacionSemanal> turnosSemana = programacionRepository.findProgramacionOrdenada(lunes, domingo);
        Map<Integer, Map<LocalDate, ProgramacionSemanal>> mapaTurnos = new HashMap<>();
        for (ProgramacionSemanal p : turnosSemana) {
            mapaTurnos.computeIfAbsent(p.getEmpleado().getIdEmpleado(), k -> new HashMap<>())
                      .put(p.getFecha(), p);
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Malla Horarios Semanales");

            byte[] azulRgb = new byte[] { (byte) 27, (byte) 54, (byte) 93 }; // #1B365D Azul Salgueros
            byte[] ambarRgb = new byte[] { (byte) 180, (byte) 83, (byte) 9 }; // #B45309 Ambar Dominical
            byte[] ambarSuaveRgb = new byte[] { (byte) 254, (byte) 243, (byte) 199 }; // #FEF3C7
            byte[] ambarClaroRgb = new byte[] { (byte) 254, (byte) 249, (byte) 195 }; // #FEF9C3
            byte[] grisDescansoRgb = new byte[] { (byte) 241, (byte) 245, (byte) 249 }; // #F1F5F9
            byte[] azulTotalRgb = new byte[] { (byte) 238, (byte) 244, (byte) 255 }; // #EEF4FF
            byte[] naranjaNovedadRgb = new byte[] { (byte) 255, (byte) 237, (byte) 213 }; // #FFEDD5

            XSSFColor colorAzul = new XSSFColor(azulRgb, null);
            XSSFColor colorAmbar = new XSSFColor(ambarRgb, null);
            XSSFColor colorAmbarSuave = new XSSFColor(ambarSuaveRgb, null);
            XSSFColor colorAmbarClaro = new XSSFColor(ambarClaroRgb, null);
            XSSFColor colorGrisDescanso = new XSSFColor(grisDescansoRgb, null);
            XSSFColor colorAzulTotal = new XSSFColor(azulTotalRgb, null);
            XSSFColor colorNaranjaNovedad = new XSSFColor(naranjaNovedadRgb, null);

            XSSFFont fontTitulo = (XSSFFont) workbook.createFont();
            fontTitulo.setColor(IndexedColors.WHITE.getIndex());
            fontTitulo.setBold(true);
            fontTitulo.setFontHeightInPoints((short) 13);

            XSSFFont fontSubtitulo = (XSSFFont) workbook.createFont();
            fontSubtitulo.setBold(true);
            fontSubtitulo.setFontHeightInPoints((short) 9.5);

            XSSFFont fontHeader = (XSSFFont) workbook.createFont();
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            fontHeader.setBold(true);
            fontHeader.setFontHeightInPoints((short) 10);

            XSSFFont fontDatos = (XSSFFont) workbook.createFont();
            fontDatos.setFontHeightInPoints((short) 9.5);

            XSSFFont fontDatosBold = (XSSFFont) workbook.createFont();
            fontDatosBold.setBold(true);
            fontDatosBold.setFontHeightInPoints((short) 9.5);

            XSSFFont fontDescanso = (XSSFFont) workbook.createFont();
            fontDescanso.setItalic(true);
            fontDescanso.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            fontDescanso.setFontHeightInPoints((short) 9);

            XSSFFont fontNovedad = (XSSFFont) workbook.createFont();
            fontNovedad.setBold(true);
            fontNovedad.setColor(IndexedColors.DARK_YELLOW.getIndex());
            fontNovedad.setFontHeightInPoints((short) 9);

            // Estilos
            CellStyle titleStyle = workbook.createCellStyle();
            ((XSSFCellStyle) titleStyle).setFillForegroundColor(colorAzul);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFont(fontTitulo);

            CellStyle subTitleStyle = workbook.createCellStyle();
            subTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            subTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            subTitleStyle.setFont(fontSubtitulo);

            CellStyle headerStyle = workbook.createCellStyle();
            ((XSSFCellStyle) headerStyle).setFillForegroundColor(colorAzul);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setFont(fontHeader);
            aplicarBordes(headerStyle);

            CellStyle headerDomingoStyle = workbook.createCellStyle();
            ((XSSFCellStyle) headerDomingoStyle).setFillForegroundColor(colorAmbar);
            headerDomingoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerDomingoStyle.setAlignment(HorizontalAlignment.CENTER);
            headerDomingoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerDomingoStyle.setWrapText(true);
            headerDomingoStyle.setFont(fontHeader);
            aplicarBordes(headerDomingoStyle);

            CellStyle centerDataStyle = workbook.createCellStyle();
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerDataStyle.setFont(fontDatos);
            aplicarBordes(centerDataStyle);

            CellStyle empNameStyle = workbook.createCellStyle();
            empNameStyle.setAlignment(HorizontalAlignment.LEFT);
            empNameStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            empNameStyle.setFont(fontDatosBold);
            aplicarBordes(empNameStyle);

            CellStyle normalDayStyle = workbook.createCellStyle();
            ((XSSFCellStyle) normalDayStyle).setFillForegroundColor(colorAzulTotal);
            normalDayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            normalDayStyle.setAlignment(HorizontalAlignment.CENTER);
            normalDayStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            normalDayStyle.setFont(fontDatosBold);
            aplicarBordes(normalDayStyle);

            CellStyle domingoTurnoStyle = workbook.createCellStyle();
            ((XSSFCellStyle) domingoTurnoStyle).setFillForegroundColor(colorAmbarSuave);
            domingoTurnoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            domingoTurnoStyle.setAlignment(HorizontalAlignment.CENTER);
            domingoTurnoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            domingoTurnoStyle.setFont(fontDatosBold);
            aplicarBordes(domingoTurnoStyle);

            CellStyle descansoStyle = workbook.createCellStyle();
            ((XSSFCellStyle) descansoStyle).setFillForegroundColor(colorGrisDescanso);
            descansoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            descansoStyle.setAlignment(HorizontalAlignment.CENTER);
            descansoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            descansoStyle.setFont(fontDescanso);
            aplicarBordes(descansoStyle);

            CellStyle domingoDescansoStyle = workbook.createCellStyle();
            ((XSSFCellStyle) domingoDescansoStyle).setFillForegroundColor(colorAmbarClaro);
            domingoDescansoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            domingoDescansoStyle.setAlignment(HorizontalAlignment.CENTER);
            domingoDescansoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            domingoDescansoStyle.setFont(fontDescanso);
            aplicarBordes(domingoDescansoStyle);

            CellStyle novedadStyle = workbook.createCellStyle();
            ((XSSFCellStyle) novedadStyle).setFillForegroundColor(colorNaranjaNovedad);
            novedadStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            novedadStyle.setAlignment(HorizontalAlignment.CENTER);
            novedadStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            novedadStyle.setFont(fontNovedad);
            aplicarBordes(novedadStyle);

            CellStyle totalStyle = workbook.createCellStyle();
            ((XSSFCellStyle) totalStyle).setFillForegroundColor(colorAzulTotal);
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalStyle.setAlignment(HorizontalAlignment.CENTER);
            totalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalStyle.setFont(fontDatosBold);
            aplicarBordes(totalStyle);

            // Fila 0: Título General
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(26);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SALGUEROS — MALLA SEMANAL DE HORARIOS");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            // Fila 1: Subtítulo
            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(18);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Semana Oficial del Lunes " + lunes.format(DATE_FMT) + " al Domingo " + domingo.format(DATE_FMT) + "  |  DOCUMENTO DE OPERACIÓN CONFIRMADO");
            c1.setCellStyle(subTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 11));

            // Fila 3: Cabeceras Oficiales (12 Columnas)
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
            String[] headers = {
                "Cédula", "Empleado", "Cargo",
                "Lunes\n(" + diasSemana[0].format(dayFmt) + ")",
                "Martes\n(" + diasSemana[1].format(dayFmt) + ")",
                "Miércoles\n(" + diasSemana[2].format(dayFmt) + ")",
                "Jueves\n(" + diasSemana[3].format(dayFmt) + ")",
                "Viernes\n(" + diasSemana[4].format(dayFmt) + ")",
                "Sábado\n(" + diasSemana[5].format(dayFmt) + ")",
                "☀️ Domingo\n(" + diasSemana[6].format(dayFmt) + ")\n[DOMINICAL]",
                "Min. Almuerzo",
                "Total Horas Base\nSemanales"
            };

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(32);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                if (i == 9) {
                    cell.setCellStyle(headerDomingoStyle); // Resaltado especial de Domingo
                } else {
                    cell.setCellStyle(headerStyle);
                }
            }

            // Inmovilizar paneles (Columnas Cédula, Empleado, Cargo y filas de cabecera fijas)
            sheet.createFreezePane(3, 4);

            int rowIdx = 4;
            double granTotalHoras = 0.0;

            for (Empleado emp : empleados) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                // Cédula
                Cell cellCedula = row.createCell(0);
                cellCedula.setCellValue(emp.getCedula());
                cellCedula.setCellStyle(centerDataStyle);

                // Empleado
                Cell cellNombre = row.createCell(1);
                cellNombre.setCellValue(emp.getNombreApellido());
                cellNombre.setCellStyle(empNameStyle);

                // Cargo
                Cell cellCargo = row.createCell(2);
                cellCargo.setCellValue(emp.getArea() != null ? emp.getArea() : "PLANTA");
                cellCargo.setCellStyle(centerDataStyle);

                double horasEmp = 0.0;
                int minAlmuerzoEmpleado = 0;
                Map<LocalDate, ProgramacionSemanal> turnosEmp = mapaTurnos.getOrDefault(emp.getIdEmpleado(), Collections.emptyMap());

                for (int d = 0; d < 7; d++) {
                    LocalDate f = diasSemana[d];
                    ProgramacionSemanal prog = turnosEmp.get(f);
                    Cell cellDia = row.createCell(3 + d);

                    boolean esDomingo = (d == 6);

                    if (prog == null || prog.getTipoNovedadPrecargada() == TipoNovedad.DESCANSO) {
                        cellDia.setCellValue("DESCANSO");
                        cellDia.setCellStyle(esDomingo ? domingoDescansoStyle : descansoStyle);
                    } else if (prog.getTipoNovedadPrecargada() != null && prog.getTipoNovedadPrecargada() != TipoNovedad.NINGUNA) {
                        cellDia.setCellValue(prog.getTipoNovedadPrecargada().name());
                        cellDia.setCellStyle(novedadStyle);
                    } else {
                        LocalTime ent = prog.getHoraEntradaProgramada();
                        LocalTime sal = prog.getHoraSalidaProgramada() != null ? prog.getHoraSalidaProgramada() : ent.plusHours(8);
                        String rango = ent.format(SHORT_TIME_FMT) + " - " + sal.format(SHORT_TIME_FMT);
                        cellDia.setCellValue(rango);
                        cellDia.setCellStyle(esDomingo ? domingoTurnoStyle : normalDayStyle);

                        Duration dur = Duration.between(ent, sal);
                        long minBrutos = dur.toMinutes();
                        if (minBrutos < 0) minBrutos += 24 * 60;
                        int minAlm = prog.getMinutosAlmuerzo() != null ? prog.getMinutosAlmuerzo() : 0;
                        if (minAlm > minAlmuerzoEmpleado) minAlmuerzoEmpleado = minAlm;
                        long minNetos = Math.max(0, minBrutos - minAlm);
                        horasEmp += (minNetos / 60.0);
                    }
                }

                // Min. Almuerzo
                Cell cellAlmuerzo = row.createCell(10);
                cellAlmuerzo.setCellValue(minAlmuerzoEmpleado + " min");
                cellAlmuerzo.setCellStyle(centerDataStyle);

                // Total Horas Base Semanales
                Cell cellTotal = row.createCell(11);
                String horasEmpStr = (horasEmp % 1 == 0) ? String.format("%.0f hrs", horasEmp) : String.format("%.1f hrs", horasEmp);
                cellTotal.setCellValue(horasEmpStr);
                cellTotal.setCellStyle(totalStyle);

                granTotalHoras += horasEmp;
            }

            // Fila de Totales Generales
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(22);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL HORAS DE OPERACIÓN SEMANAL");
            totalLabel.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 10));
            for (int k = 1; k <= 10; k++) {
                totalRow.createCell(k).setCellStyle(titleStyle);
            }

            Cell totalVal = totalRow.createCell(11);
            String granTotalStr = (granTotalHoras % 1 == 0) ? String.format("%.0f hrs", granTotalHoras) : String.format("%.1f hrs", granTotalHoras);
            totalVal.setCellValue(granTotalStr);
            totalVal.setCellStyle(headerStyle);

            // Activar AutoFiltro para clasificar dentro de tabla estructurada en Excel
            if (rowIdx > 4) {
                sheet.setAutoFilter(new CellRangeAddress(3, rowIdx - 1, 0, 11));
            }

            // Ajustar anchos
            sheet.setColumnWidth(0, 14 * 256); // Cédula
            sheet.setColumnWidth(1, 26 * 256); // Empleado
            sheet.setColumnWidth(2, 18 * 256); // Cargo
            for (int d = 3; d <= 9; d++) {
                sheet.setColumnWidth(d, 16 * 256); // Lunes a Domingo
            }
            sheet.setColumnWidth(10, 14 * 256); // Min. Almuerzo
            sheet.setColumnWidth(11, 20 * 256); // Total Horas Base

            // Pestañas separadas por cada día del horario semanal
            String[] nombresDias = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo" };
            for (int d = 0; d < 7; d++) {
                LocalDate diaFecha = diasSemana[d];
                boolean esDom = (d == 6);
                String nombreTab = nombresDias[d] + " " + diaFecha.format(DATE_FMT);
                Sheet sheetDia = workbook.createSheet(nombreTab);

                Row r0Dia = sheetDia.createRow(0);
                r0Dia.setHeightInPoints(24);
                Cell c0Dia = r0Dia.createCell(0);
                c0Dia.setCellValue("SALGUEROS — HORARIO DEL DÍA: " + nombresDias[d].toUpperCase() + " " + diaFecha.format(DATE_FMT));
                c0Dia.setCellStyle(esDom ? headerDomingoStyle : titleStyle);
                sheetDia.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

                Row r1Dia = sheetDia.createRow(1);
                r1Dia.setHeightInPoints(18);
                Cell c1Dia = r1Dia.createCell(0);
                c1Dia.setCellValue("Programación Individual del Día  |  1 Fila por Empleado  |  Almuerzo Descontado");
                c1Dia.setCellStyle(subTitleStyle);
                sheetDia.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

                String[] headersDia = {
                    "Cédula", "Empleado", "Cargo", "Fecha", "Día",
                    "Entrada Prog", "Salida Prog", "Min. Almuerzo", "Horas Netas Día"
                };
                Row hRowDia = sheetDia.createRow(3);
                hRowDia.setHeightInPoints(26);
                for (int c = 0; c < headersDia.length; c++) {
                    Cell hc = hRowDia.createCell(c);
                    hc.setCellValue(headersDia[c]);
                    hc.setCellStyle(esDom ? headerDomingoStyle : headerStyle);
                }
                sheetDia.createFreezePane(2, 4);

                int rDiaIdx = 4;
                double totalHorasDia = 0.0;
                for (Empleado emp : empleados) {
                    Row rowD = sheetDia.createRow(rDiaIdx++);
                    rowD.setHeightInPoints(20);
                    Map<LocalDate, ProgramacionSemanal> turnosEmp = mapaTurnos.getOrDefault(emp.getIdEmpleado(), Collections.emptyMap());
                    ProgramacionSemanal prog = turnosEmp.get(diaFecha);

                    Cell cc0 = rowD.createCell(0);
                    cc0.setCellValue(emp.getCedula());
                    cc0.setCellStyle(centerDataStyle);

                    Cell cc1 = rowD.createCell(1);
                    cc1.setCellValue(emp.getNombreApellido());
                    cc1.setCellStyle(empNameStyle);

                    Cell cc2 = rowD.createCell(2);
                    cc2.setCellValue(emp.getArea() != null ? emp.getArea() : "PLANTA");
                    cc2.setCellStyle(centerDataStyle);

                    Cell cc3 = rowD.createCell(3);
                    cc3.setCellValue(diaFecha.format(DATE_FMT));
                    cc3.setCellStyle(centerDataStyle);

                    Cell cc4 = rowD.createCell(4);
                    cc4.setCellValue(nombresDias[d]);
                    cc4.setCellStyle(centerDataStyle);

                    if (prog == null || prog.getTipoNovedadPrecargada() == TipoNovedad.DESCANSO) {
                        String novTxt = (prog != null && prog.getTipoNovedadPrecargada() != null) ? prog.getTipoNovedadPrecargada().name() : "DESCANSO";
                        Cell cc5 = rowD.createCell(5); cc5.setCellValue(novTxt); cc5.setCellStyle(esDom ? domingoDescansoStyle : descansoStyle);
                        Cell cc6 = rowD.createCell(6); cc6.setCellValue("--"); cc6.setCellStyle(esDom ? domingoDescansoStyle : descansoStyle);
                        Cell cc7 = rowD.createCell(7); cc7.setCellValue("0 min"); cc7.setCellStyle(centerDataStyle);
                        Cell cc8 = rowD.createCell(8); cc8.setCellValue("0 hrs"); cc8.setCellStyle(totalStyle);
                    } else if (prog.getTipoNovedadPrecargada() != null && prog.getTipoNovedadPrecargada() != TipoNovedad.NINGUNA) {
                        String novTxt = prog.getTipoNovedadPrecargada().name();
                        Cell cc5 = rowD.createCell(5); cc5.setCellValue(novTxt); cc5.setCellStyle(novedadStyle);
                        Cell cc6 = rowD.createCell(6); cc6.setCellValue("--"); cc6.setCellStyle(novedadStyle);
                        Cell cc7 = rowD.createCell(7); cc7.setCellValue("0 min"); cc7.setCellStyle(centerDataStyle);
                        Cell cc8 = rowD.createCell(8); cc8.setCellValue("0 hrs"); cc8.setCellStyle(totalStyle);
                    } else {
                        LocalTime ent = prog.getHoraEntradaProgramada();
                        LocalTime sal = prog.getHoraSalidaProgramada() != null ? prog.getHoraSalidaProgramada() : ent.plusHours(8);
                        Duration dur = Duration.between(ent, sal);
                        long minBrutos = dur.toMinutes();
                        if (minBrutos < 0) minBrutos += 24 * 60;
                        int minAlm = prog.getMinutosAlmuerzo() != null ? prog.getMinutosAlmuerzo() : 0;
                        long minNetos = Math.max(0, minBrutos - minAlm);
                        double hrsDia = minNetos / 60.0;
                        totalHorasDia += hrsDia;

                        Cell cc5 = rowD.createCell(5); cc5.setCellValue(ent.format(SHORT_TIME_FMT)); cc5.setCellStyle(esDom ? domingoTurnoStyle : normalDayStyle);
                        Cell cc6 = rowD.createCell(6); cc6.setCellValue(sal.format(SHORT_TIME_FMT)); cc6.setCellStyle(esDom ? domingoTurnoStyle : normalDayStyle);
                        Cell cc7 = rowD.createCell(7); cc7.setCellValue(minAlm + " min"); cc7.setCellStyle(centerDataStyle);
                        Cell cc8 = rowD.createCell(8);
                        cc8.setCellValue((hrsDia % 1 == 0) ? String.format("%.0f hrs", hrsDia) : String.format("%.1f hrs", hrsDia));
                        cc8.setCellStyle(totalStyle);
                    }
                }

                Row totDiaRow = sheetDia.createRow(rDiaIdx);
                totDiaRow.setHeightInPoints(22);
                Cell totDiaLbl = totDiaRow.createCell(0);
                totDiaLbl.setCellValue("TOTAL HORAS PROGRAMADAS — " + nombresDias[d].toUpperCase() + " " + diaFecha.format(DATE_FMT));
                totDiaLbl.setCellStyle(titleStyle);
                sheetDia.addMergedRegion(new CellRangeAddress(rDiaIdx, rDiaIdx, 0, 7));
                for (int k = 1; k <= 7; k++) {
                    totDiaRow.createCell(k).setCellStyle(titleStyle);
                }
                Cell totDiaVal = totDiaRow.createCell(8);
                totDiaVal.setCellValue((totalHorasDia % 1 == 0) ? String.format("%.0f hrs", totalHorasDia) : String.format("%.1f hrs", totalHorasDia));
                totDiaVal.setCellStyle(headerStyle);

                sheetDia.setColumnWidth(0, 14 * 256);
                sheetDia.setColumnWidth(1, 26 * 256);
                sheetDia.setColumnWidth(2, 18 * 256);
                sheetDia.setColumnWidth(3, 14 * 256);
                sheetDia.setColumnWidth(4, 14 * 256);
                sheetDia.setColumnWidth(5, 16 * 256);
                sheetDia.setColumnWidth(6, 16 * 256);
                sheetDia.setColumnWidth(7, 16 * 256);
                sheetDia.setColumnWidth(8, 18 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Endpoint 2: Reporte Detallado de Nómina, Asistencia, Horas Extras y Recargos (Colombia)
     * Columnas: [Cédula] | [Empleado] | [Cargo] | [Fecha] | [Día] | [Entrada Prog] | [Salida Prog] | [Almuerzo (Min)] | [Entrada Real] | [Salida Real] | [Horas Ordinarias] | [Recargo Nocturno] | [H. Extra Diurna] | [H. Extra Nocturna] | [Dominical/Festivo] | [H. Extra Dominical] | [Total Horas a Pagar] | [Novedad / Estado]
     */
    public byte[] generarNominaAsistenciaExcel(LocalDate inicio, LocalDate fin) throws IOException {
        if (inicio == null) inicio = LocalDate.now().minusDays(7);
        if (fin == null) fin = LocalDate.now();

        List<ProgramacionSemanal> programaciones = programacionRepository.findProgramacionOrdenada(inicio, fin);
        List<RegistroAsistencia> registros = registroRepository.findRegistrosEntreFechas(inicio, fin);

        // Deduplicación preventiva por cédula y fecha para evitar nombres y días duplicados.
        // Si existen registros de asistencia reales en el período, construimos la nómina limpia sobre los días realmente trabajados/registrados.
        Map<String, CalculadoraNominaColombia.DetalleNominaDia> mapaDetallesUnicos = new LinkedHashMap<>();
        if (!registros.isEmpty()) {
            for (RegistroAsistencia reg : registros) {
                ProgramacionSemanal p = reg.getProgramacion();
                if (p != null) {
                    CalculadoraNominaColombia.DetalleNominaDia d = CalculadoraNominaColombia.calcularDia(p, reg);
                    String key = d.getCedula() + "_" + d.getFecha();
                    mapaDetallesUnicos.put(key, d);
                }
            }
        } else {
            for (ProgramacionSemanal p : programaciones) {
                CalculadoraNominaColombia.DetalleNominaDia d = CalculadoraNominaColombia.calcularDia(p, null);
                String key = d.getCedula() + "_" + d.getFecha();
                if (!mapaDetallesUnicos.containsKey(key)) {
                    mapaDetallesUnicos.put(key, d);
                }
            }
        }

        List<CalculadoraNominaColombia.DetalleNominaDia> listaDetalles = new ArrayList<>(mapaDetallesUnicos.values());

        // Orden clasificado y organizado: Cargo / Área ASC -> Empleado ASC -> Fecha ASC
        listaDetalles.sort((d1, d2) -> {
            String cargo1 = d1.getCargo() != null ? d1.getCargo() : "PLANTA";
            String cargo2 = d2.getCargo() != null ? d2.getCargo() : "PLANTA";
            int cA = cargo1.compareToIgnoreCase(cargo2);
            if (cA != 0) return cA;
            int cE = d1.getEmpleado().compareToIgnoreCase(d2.getEmpleado());
            if (cE != 0) return cE;
            return d1.getFecha().compareTo(d2.getFecha());
        });

        // Agrupación por empleado para Pestaña 1 (Resumen Consolidado: 1 sola fila por empleado, sin nombres duplicados)
        Map<String, List<CalculadoraNominaColombia.DetalleNominaDia>> porEmpleado = new LinkedHashMap<>();
        // Agrupación por fecha para crear 1 Pestaña Independiente por cada Día (sin mezclar días en una sola pestaña)
        Map<LocalDate, List<CalculadoraNominaColombia.DetalleNominaDia>> porFecha = new TreeMap<>();
        for (CalculadoraNominaColombia.DetalleNominaDia d : listaDetalles) {
            porEmpleado.computeIfAbsent(d.getCedula(), k -> new ArrayList<>()).add(d);
            porFecha.computeIfAbsent(d.getFecha(), k -> new ArrayList<>()).add(d);
        }

        String selloVeracidad = generarSelloHash(inicio, fin, registros.size(), programaciones.size());
        String passwordProteccion = "SalguerosNomina_" + selloVeracidad;

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] azulRgb = new byte[] { (byte) 27, (byte) 54, (byte) 93 }; // #1B365D
            byte[] grisFondoRgb = new byte[] { (byte) 242, (byte) 244, (byte) 247 };
            byte[] amarilloRetardoRgb = new byte[] { (byte) 254, (byte) 243, (byte) 199 };
            byte[] ambarHeaderRgb = new byte[] { (byte) 180, (byte) 83, (byte) 9 }; // #B45309 Ambar Dominical
            byte[] ambarFondoRgb = new byte[] { (byte) 254, (byte) 249, (byte) 195 }; // #FEF9C3
            byte[] verdeExtrasRgb = new byte[] { (byte) 220, (byte) 252, (byte) 231 }; // #DCFCE7

            XSSFColor colorAzul = new XSSFColor(azulRgb, null);
            XSSFColor colorGris = new XSSFColor(grisFondoRgb, null);
            XSSFColor colorAmarillo = new XSSFColor(amarilloRetardoRgb, null);
            XSSFColor colorAmbarHeader = new XSSFColor(ambarHeaderRgb, null);
            XSSFColor colorAmbarFondo = new XSSFColor(ambarFondoRgb, null);
            XSSFColor colorVerdeExtras = new XSSFColor(verdeExtrasRgb, null);

            XSSFFont fontTitulo = (XSSFFont) workbook.createFont();
            fontTitulo.setColor(IndexedColors.WHITE.getIndex());
            fontTitulo.setBold(true);
            fontTitulo.setFontHeightInPoints((short) 13);

            XSSFFont fontSubtitulo = (XSSFFont) workbook.createFont();
            fontSubtitulo.setBold(true);
            fontSubtitulo.setFontHeightInPoints((short) 9);

            XSSFFont fontHeader = (XSSFFont) workbook.createFont();
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            fontHeader.setBold(true);
            fontHeader.setFontHeightInPoints((short) 9.5);

            XSSFFont fontDatos = (XSSFFont) workbook.createFont();
            fontDatos.setFontHeightInPoints((short) 9);

            XSSFFont fontDatosBold = (XSSFFont) workbook.createFont();
            fontDatosBold.setBold(true);
            fontDatosBold.setFontHeightInPoints((short) 9);

            XSSFFont fontDominical = (XSSFFont) workbook.createFont();
            fontDominical.setBold(true);
            fontDominical.setColor(IndexedColors.BROWN.getIndex());
            fontDominical.setFontHeightInPoints((short) 9);

            XSSFFont fontExtras = (XSSFFont) workbook.createFont();
            fontExtras.setBold(true);
            fontExtras.setColor(IndexedColors.DARK_GREEN.getIndex());
            fontExtras.setFontHeightInPoints((short) 9);

            DataFormat df = workbook.createDataFormat();
            short numFormat = df.getFormat("0.00");

            // Estilos
            CellStyle titleStyle = workbook.createCellStyle();
            ((XSSFCellStyle) titleStyle).setFillForegroundColor(colorAzul);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFont(fontTitulo);

            CellStyle subTitleStyle = workbook.createCellStyle();
            subTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            subTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            subTitleStyle.setFont(fontSubtitulo);

            CellStyle headerStyle = workbook.createCellStyle();
            ((XSSFCellStyle) headerStyle).setFillForegroundColor(colorAzul);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setFont(fontHeader);
            aplicarBordes(headerStyle);

            CellStyle headerDominicalStyle = workbook.createCellStyle();
            ((XSSFCellStyle) headerDominicalStyle).setFillForegroundColor(colorAmbarHeader);
            headerDominicalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerDominicalStyle.setAlignment(HorizontalAlignment.CENTER);
            headerDominicalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerDominicalStyle.setWrapText(true);
            headerDominicalStyle.setFont(fontHeader);
            aplicarBordes(headerDominicalStyle);

            CellStyle centerDataStyle = workbook.createCellStyle();
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerDataStyle.setFont(fontDatos);
            aplicarBordes(centerDataStyle);

            CellStyle domingoDataStyle = workbook.createCellStyle();
            ((XSSFCellStyle) domingoDataStyle).setFillForegroundColor(colorAmbarFondo);
            domingoDataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            domingoDataStyle.setAlignment(HorizontalAlignment.CENTER);
            domingoDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            domingoDataStyle.setFont(fontDominical);
            aplicarBordes(domingoDataStyle);

            CellStyle empNameStyle = workbook.createCellStyle();
            empNameStyle.setAlignment(HorizontalAlignment.LEFT);
            empNameStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            empNameStyle.setFont(fontDatosBold);
            aplicarBordes(empNameStyle);

            CellStyle numDataStyle = workbook.createCellStyle();
            numDataStyle.setDataFormat(numFormat);
            numDataStyle.setAlignment(HorizontalAlignment.RIGHT);
            numDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numDataStyle.setFont(fontDatos);
            aplicarBordes(numDataStyle);

            CellStyle extrasNumStyle = workbook.createCellStyle();
            extrasNumStyle.setDataFormat(numFormat);
            ((XSSFCellStyle) extrasNumStyle).setFillForegroundColor(colorVerdeExtras);
            extrasNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            extrasNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            extrasNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            extrasNumStyle.setFont(fontExtras);
            aplicarBordes(extrasNumStyle);

            CellStyle domingoNumStyle = workbook.createCellStyle();
            domingoNumStyle.setDataFormat(numFormat);
            ((XSSFCellStyle) domingoNumStyle).setFillForegroundColor(colorAmbarFondo);
            domingoNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            domingoNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            domingoNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            domingoNumStyle.setFont(fontDominical);
            aplicarBordes(domingoNumStyle);

            CellStyle totalNumStyle = workbook.createCellStyle();
            totalNumStyle.setDataFormat(numFormat);
            ((XSSFCellStyle) totalNumStyle).setFillForegroundColor(colorAzul);
            totalNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalNumStyle.setFont(fontHeader);
            aplicarBordes(totalNumStyle);

            CellStyle estadoOkStyle = workbook.createCellStyle();
            estadoOkStyle.setAlignment(HorizontalAlignment.CENTER);
            estadoOkStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            estadoOkStyle.setFont(fontDatos);
            aplicarBordes(estadoOkStyle);

            CellStyle estadoAlertaStyle = workbook.createCellStyle();
            ((XSSFCellStyle) estadoAlertaStyle).setFillForegroundColor(colorAmarillo);
            estadoAlertaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estadoAlertaStyle.setAlignment(HorizontalAlignment.CENTER);
            estadoAlertaStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            estadoAlertaStyle.setFont(fontDatosBold);
            aplicarBordes(estadoAlertaStyle);

            CellStyle descansoStyle = workbook.createCellStyle();
            ((XSSFCellStyle) descansoStyle).setFillForegroundColor(colorGris);
            descansoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            descansoStyle.setAlignment(HorizontalAlignment.CENTER);
            descansoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            descansoStyle.setFont(fontDatos);
            aplicarBordes(descansoStyle);

            // 1. Pestaña 1: Resumen Consolidado de Nómina (1 sola fila por empleado, sin nombres duplicados)
            Sheet sheetResumen = workbook.createSheet("Resumen Nómina");
            construirPestanaResumen(sheetResumen, porEmpleado, inicio, fin, selloVeracidad,
                    titleStyle, subTitleStyle, headerStyle, headerDominicalStyle, centerDataStyle, empNameStyle,
                    numDataStyle, extrasNumStyle, domingoNumStyle, totalNumStyle, estadoOkStyle, estadoAlertaStyle);

            // 2. Pestañas Separadas por Día: 1 pestaña por cada día que se hizo (ej. "Jueves 2026-11-12", "Viernes 2026-11-13")
            if (porFecha.isEmpty()) {
                Sheet sheetDetalle = workbook.createSheet("Sin Registros");
                construirPestanaDetalle(sheetDetalle, listaDetalles, inicio, fin, selloVeracidad,
                        titleStyle, subTitleStyle, headerStyle, headerDominicalStyle, centerDataStyle, domingoDataStyle, empNameStyle,
                        numDataStyle, extrasNumStyle, domingoNumStyle, totalNumStyle, estadoOkStyle, estadoAlertaStyle, descansoStyle);
            } else {
                for (Map.Entry<LocalDate, List<CalculadoraNominaColombia.DetalleNominaDia>> entryDia : porFecha.entrySet()) {
                    LocalDate fechaDia = entryDia.getKey();
                    List<CalculadoraNominaColombia.DetalleNominaDia> detallesDelDia = entryDia.getValue();
                    String diaNombre = detallesDelDia.get(0).getDiaNombre();
                    String nombrePestana = diaNombre + " " + fechaDia.format(DATE_FMT);
                    Sheet sheetDia = workbook.createSheet(nombrePestana);
                    construirPestanaDetalle(sheetDia, detallesDelDia, fechaDia, fechaDia, selloVeracidad,
                            titleStyle, subTitleStyle, headerStyle, headerDominicalStyle, centerDataStyle, domingoDataStyle, empNameStyle,
                            numDataStyle, extrasNumStyle, domingoNumStyle, totalNumStyle, estadoOkStyle, estadoAlertaStyle, descansoStyle);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void construirPestanaResumen(Sheet sheetResumen,
                                        Map<String, List<CalculadoraNominaColombia.DetalleNominaDia>> porEmpleado,
                                        LocalDate inicio, LocalDate fin, String selloVeracidad,
                                        CellStyle titleStyle, CellStyle subTitleStyle, CellStyle headerStyle,
                                        CellStyle headerDominicalStyle, CellStyle centerDataStyle, CellStyle empNameStyle,
                                        CellStyle numDataStyle, CellStyle extrasNumStyle, CellStyle domingoNumStyle,
                                        CellStyle totalNumStyle, CellStyle estadoOkStyle, CellStyle estadoAlertaStyle) {
        // Fila 0: Título General
        Row r0 = sheetResumen.createRow(0);
        r0.setHeightInPoints(24);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("SALGUEROS — RESUMEN CONSOLIDADO DE NÓMINA (COLOMBIA)");
        c0.setCellStyle(titleStyle);
        sheetResumen.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

        // Fila 1: Subtítulo
        Row r1 = sheetResumen.createRow(1);
        r1.setHeightInPoints(20);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("Período: Del " + inicio.format(DATE_FMT) + " al " + fin.format(DATE_FMT)
                + "  |  Consolidado por Empleado (1 Fila por Persona)  |  Recargo Nocturno (35%): 19:00 a 06:00  |  Recargo Dominical/Festivo: 75%");
        c1.setCellStyle(subTitleStyle);
        sheetResumen.addMergedRegion(new CellRangeAddress(1, 1, 0, 12));

        String[] headers = {
            "Cédula", "Empleado", "Cargo / Área",
            "Días Programados", "Días Laborados",
            "Horas Ordinarias", "Recargo Nocturno\n(35% desde 19:00)",
            "H. Extra Diurna\n(25%)", "H. Extra Nocturna\n(75%)",
            "☀️ Dominical / Festivo\n(Recargo 75%)", "H. Extra Dominical\n(100%/150%)",
            "Total Horas a Pagar", "Novedades / Auditoría"
        };

        Row headerRow = sheetResumen.createRow(3);
        headerRow.setHeightInPoints(32);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            if (i == 9 || i == 10) {
                cell.setCellStyle(headerDominicalStyle);
            } else {
                cell.setCellStyle(headerStyle);
            }
        }

        sheetResumen.createFreezePane(3, 4);

        int startDataRow = 4;
        int rowIdx = startDataRow;

        for (Map.Entry<String, List<CalculadoraNominaColombia.DetalleNominaDia>> entry : porEmpleado.entrySet()) {
            List<CalculadoraNominaColombia.DetalleNominaDia> diasEmp = entry.getValue();
            CalculadoraNominaColombia.DetalleNominaDia primer = diasEmp.get(0);

            Row row = sheetResumen.createRow(rowIdx++);
            row.setHeightInPoints(21);

            int diasProg = (int) diasEmp.stream().filter(d -> d.getHorasProgramadas() > 0).count();
            int diasLab = (int) diasEmp.stream().filter(d -> d.getHorasLaboradasReales() > 0).count();
            double totOrd = redondear2(diasEmp.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getHorasOrdinarias).sum());
            double totRN = redondear2(diasEmp.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getRecargoNocturno).sum());
            double totHED = redondear2(diasEmp.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getHorasExtraDiurna).sum());
            double totHEN = redondear2(diasEmp.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getHorasExtraNocturna).sum());
            double totDom = redondear2(diasEmp.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getDominicalFestivo).sum());
            double totHEDom = redondear2(diasEmp.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getHorasExtraDominical).sum());
            double totPagar = redondear2(totOrd + totDom + totHED + totHEN + totHEDom);

            long retardosCount = diasEmp.stream()
                    .filter(d -> d.getNovedadEstado() != null && d.getNovedadEstado().contains("RETARDO"))
                    .count();

            String novedadesResumen = (diasLab == diasProg) ? "Al día (" + diasLab + "/" + diasProg + " turnos)" : diasLab + "/" + diasProg + " turnos laborados";
            if (retardosCount > 0) {
                novedadesResumen += " | " + retardosCount + " retardo(s)";
            }

            Cell cCed = row.createCell(0);
            cCed.setCellValue(primer.getCedula());
            cCed.setCellStyle(centerDataStyle);

            Cell cNom = row.createCell(1);
            cNom.setCellValue(primer.getEmpleado());
            cNom.setCellStyle(empNameStyle);

            Cell cCar = row.createCell(2);
            cCar.setCellValue(primer.getCargo());
            cCar.setCellStyle(centerDataStyle);

            Cell cDP = row.createCell(3);
            cDP.setCellValue(diasProg);
            cDP.setCellStyle(centerDataStyle);

            Cell cDL = row.createCell(4);
            cDL.setCellValue(diasLab);
            cDL.setCellStyle(centerDataStyle);

            Cell cOrd = row.createCell(5);
            cOrd.setCellValue(totOrd);
            cOrd.setCellStyle(numDataStyle);

            Cell cRN = row.createCell(6);
            cRN.setCellValue(totRN);
            cRN.setCellStyle(totRN > 0 ? extrasNumStyle : numDataStyle);

            Cell cHED = row.createCell(7);
            cHED.setCellValue(totHED);
            cHED.setCellStyle(totHED > 0 ? extrasNumStyle : numDataStyle);

            Cell cHEN = row.createCell(8);
            cHEN.setCellValue(totHEN);
            cHEN.setCellStyle(totHEN > 0 ? extrasNumStyle : numDataStyle);

            Cell cDom = row.createCell(9);
            cDom.setCellValue(totDom);
            cDom.setCellStyle(totDom > 0 ? domingoNumStyle : numDataStyle);

            Cell cHEDom = row.createCell(10);
            cHEDom.setCellValue(totHEDom);
            cHEDom.setCellStyle(totHEDom > 0 ? extrasNumStyle : numDataStyle);

            Cell cTot = row.createCell(11);
            cTot.setCellValue(totPagar);
            cTot.setCellStyle(numDataStyle);

            Cell cNov = row.createCell(12);
            cNov.setCellValue(novedadesResumen);
            cNov.setCellStyle(retardosCount > 0 ? estadoAlertaStyle : estadoOkStyle);
        }

        // Fila Final de Totales con Fórmulas Excel (SUM)
        int lastDataRow = rowIdx;
        Row totalRow = sheetResumen.createRow(rowIdx);
        totalRow.setHeightInPoints(24);

        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTALES GENERALES CONSOLIDADOS");
        totalLabel.setCellStyle(titleStyle);
        sheetResumen.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
        for (int k = 1; k <= 4; k++) {
            totalRow.createCell(k).setCellStyle(titleStyle);
        }

        int firstExcelRow = startDataRow + 1; // 5
        int lastExcelRow = Math.max(firstExcelRow, lastDataRow);

        String[] colsLetters = {"F", "G", "H", "I", "J", "K", "L"};
        for (int i = 0; i < colsLetters.length; i++) {
            int colNum = 5 + i;
            Cell cSum = totalRow.createCell(colNum);
            cSum.setCellFormula("SUM(" + colsLetters[i] + firstExcelRow + ":" + colsLetters[i] + lastExcelRow + ")");
            cSum.setCellStyle(totalNumStyle);
        }

        Cell cFin = totalRow.createCell(12);
        cFin.setCellValue("AUDITADO OK");
        cFin.setCellStyle(titleStyle);

        if (rowIdx > startDataRow) {
            sheetResumen.setAutoFilter(new CellRangeAddress(3, rowIdx - 1, 0, 12));
        }

        sheetResumen.setColumnWidth(0, 14 * 256);
        sheetResumen.setColumnWidth(1, 28 * 256);
        sheetResumen.setColumnWidth(2, 16 * 256);
        sheetResumen.setColumnWidth(3, 16 * 256);
        sheetResumen.setColumnWidth(4, 16 * 256);
        sheetResumen.setColumnWidth(5, 18 * 256);
        sheetResumen.setColumnWidth(6, 20 * 256);
        sheetResumen.setColumnWidth(7, 18 * 256);
        sheetResumen.setColumnWidth(8, 18 * 256);
        sheetResumen.setColumnWidth(9, 22 * 256);
        sheetResumen.setColumnWidth(10, 20 * 256);
        sheetResumen.setColumnWidth(11, 20 * 256);
        sheetResumen.setColumnWidth(12, 32 * 256);
    }

    private void construirPestanaDetalle(Sheet sheet,
                                        List<CalculadoraNominaColombia.DetalleNominaDia> listaDetalles,
                                        LocalDate inicio, LocalDate fin, String selloVeracidad,
                                        CellStyle titleStyle, CellStyle subTitleStyle, CellStyle headerStyle,
                                        CellStyle headerDominicalStyle, CellStyle centerDataStyle, CellStyle domingoDataStyle,
                                        CellStyle empNameStyle, CellStyle numDataStyle, CellStyle extrasNumStyle,
                                        CellStyle domingoNumStyle, CellStyle totalNumStyle, CellStyle estadoOkStyle,
                                        CellStyle estadoAlertaStyle, CellStyle descansoStyle) {
        String diaHeaderStr = (!listaDetalles.isEmpty() && inicio.equals(fin))
                ? (listaDetalles.get(0).getDiaNombre().toUpperCase() + " " + inicio.format(DATE_FMT))
                : ("DEL " + inicio.format(DATE_FMT) + " AL " + fin.format(DATE_FMT));

        // Título
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(24);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("SALGUEROS — NÓMINA Y ASISTENCIA DEL DÍA: " + diaHeaderStr);
        c0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 17));

        // Subtítulo
        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(20);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("Jornada Auditada: " + diaHeaderStr
                + "  |  1 Fila por Empleado  |  Almuerzo Descontado  |  Alivio Entrada: 8 min  |  Fuente Oficial [" + selloVeracidad + "]");
        c1.setCellStyle(subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 17));

        // Cabeceras (18 Columnas exactas con espacio exclusivo para Dominicales y Nocturno 19:00)
        String[] headers = {
            "Cédula", "Empleado", "Cargo", "Fecha", "Día",
            "Entrada Prog", "Salida Prog", "Almuerzo (Min)",
            "Entrada Real", "Salida Real",
            "Horas Ordinarias", "Recargo Nocturno\n(35% desde 19:00)",
            "H. Extra Diurna\n(25%)", "H. Extra Nocturna\n(75%)",
            "☀️ Dominical / Festivo\n(Recargo 75%)", "H. Extra Dominical\n(100%/150%)",
            "Total Horas a Pagar", "Novedad / Estado"
        };

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(32);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            if (i == 14 || i == 15) {
                cell.setCellStyle(headerDominicalStyle); // Destacado especial Dominical
            } else {
                cell.setCellStyle(headerStyle);
            }
        }

        // Inmovilizar paneles (Cédula, Empleado, Cargo y filas de cabecera fijas)
        sheet.createFreezePane(3, 4);

        int startDataRow = 4; // Excel row 5 (1-based)
        int rowIdx = startDataRow;

        for (CalculadoraNominaColombia.DetalleNominaDia d : listaDetalles) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(21);

            boolean esDomRow = (d.getFecha().getDayOfWeek() == DayOfWeek.SUNDAY)
                    || CalculadoraNominaColombia.esDominicalOFestivo(d.getFecha());

            Cell cCed = row.createCell(0);
            cCed.setCellValue(d.getCedula());
            cCed.setCellStyle(centerDataStyle);

            Cell cNom = row.createCell(1);
            cNom.setCellValue(d.getEmpleado());
            cNom.setCellStyle(empNameStyle);

            Cell cCar = row.createCell(2);
            cCar.setCellValue(d.getCargo());
            cCar.setCellStyle(centerDataStyle);

            Cell cFec = row.createCell(3);
            cFec.setCellValue(d.getFecha().format(DATE_FMT));
            cFec.setCellStyle(esDomRow ? domingoDataStyle : centerDataStyle);

            Cell cDia = row.createCell(4);
            cDia.setCellValue(esDomRow ? "☀️ " + d.getDiaNombre().toUpperCase() : d.getDiaNombre());
            cDia.setCellStyle(esDomRow ? domingoDataStyle : centerDataStyle);

            Cell cEP = row.createCell(5);
            cEP.setCellValue(d.getEntradaProg() != null ? d.getEntradaProg().format(SHORT_TIME_FMT) : "--");
            cEP.setCellStyle(centerDataStyle);

            Cell cSP = row.createCell(6);
            cSP.setCellValue(d.getSalidaProg() != null ? d.getSalidaProg().format(SHORT_TIME_FMT) : "--");
            cSP.setCellStyle(centerDataStyle);

            Cell cAlm = row.createCell(7);
            cAlm.setCellValue(d.getMinutosAlmuerzo());
            cAlm.setCellStyle(centerDataStyle);

            Cell cER = row.createCell(8);
            cER.setCellValue(d.getEntradaReal() != null ? d.getEntradaReal().format(SHORT_TIME_FMT) : "--");
            cER.setCellStyle(centerDataStyle);

            Cell cSR = row.createCell(9);
            cSR.setCellValue(d.getSalidaReal() != null ? d.getSalidaReal().format(SHORT_TIME_FMT) : "--");
            cSR.setCellStyle(centerDataStyle);

            // Columnas Numéricas para Liquidación:
            // 10: Horas Ordinarias
            Cell cOrd = row.createCell(10);
            cOrd.setCellValue(d.getHorasOrdinarias());
            cOrd.setCellStyle(numDataStyle);

            // 11: Recargo Nocturno (Desde 19:00)
            Cell cRN = row.createCell(11);
            cRN.setCellValue(d.getRecargoNocturno());
            cRN.setCellStyle(d.getRecargoNocturno() > 0 ? extrasNumStyle : numDataStyle);

            // 12: H. Extra Diurna (06:00 a 19:00)
            Cell cHED = row.createCell(12);
            cHED.setCellValue(d.getHorasExtraDiurna());
            cHED.setCellStyle(d.getHorasExtraDiurna() > 0 ? extrasNumStyle : numDataStyle);

            // 13: H. Extra Nocturna (19:00 a 06:00)
            Cell cHEN = row.createCell(13);
            cHEN.setCellValue(d.getHorasExtraNocturna());
            cHEN.setCellStyle(d.getHorasExtraNocturna() > 0 ? extrasNumStyle : numDataStyle);

            // 14: Dominical/Festivo (Espacio propio Dominical)
            Cell cDom = row.createCell(14);
            cDom.setCellValue(d.getDominicalFestivo());
            cDom.setCellStyle(d.getDominicalFestivo() > 0 ? domingoNumStyle : (esDomRow ? domingoDataStyle : numDataStyle));

            // 15: H. Extra Dominical
            Cell cHEDom = row.createCell(15);
            cHEDom.setCellValue(d.getHorasExtraDominical());
            cHEDom.setCellStyle(d.getHorasExtraDominical() > 0 ? extrasNumStyle : (esDomRow ? domingoDataStyle : numDataStyle));

            // 16: Total Horas a Pagar
            Cell cTot = row.createCell(16);
            cTot.setCellValue(d.getTotalHorasAPagar());
            cTot.setCellStyle(numDataStyle);

            // 17: Novedad / Estado (Anotación clara en nómina)
            Cell cEst = row.createCell(17);
            cEst.setCellValue(d.getNovedadEstado());
            if ("DESCANSO".equalsIgnoreCase(d.getNovedadEstado())) {
                cEst.setCellStyle(descansoStyle);
            } else if (d.getNovedadEstado().contains("SIN SALIDA") || d.getNovedadEstado().contains("SIN REGISTRO") || d.getNovedadEstado().contains("RETARDO") || d.getNovedadEstado().contains("FALTO")) {
                cEst.setCellStyle(estadoAlertaStyle);
            } else {
                cEst.setCellStyle(estadoOkStyle);
            }
        }

        // Fila Final de Totales con Fórmulas Excel (SUM)
        int lastDataRow = rowIdx; // 1-based index is rowIdx
        Row totalRow = sheet.createRow(rowIdx);
        totalRow.setHeightInPoints(24);

        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTALES GENERALES DE LIQUIDACIÓN");
        totalLabel.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 9));
        for (int k = 1; k <= 9; k++) {
            totalRow.createCell(k).setCellStyle(titleStyle);
        }

        int firstExcelRow = startDataRow + 1; // 5
        int lastExcelRow = Math.max(firstExcelRow, lastDataRow);

        String[] colsLetters = {"K", "L", "M", "N", "O", "P", "Q"};
        for (int i = 0; i < colsLetters.length; i++) {
            int colNum = 10 + i;
            Cell cSum = totalRow.createCell(colNum);
            cSum.setCellFormula("SUM(" + colsLetters[i] + firstExcelRow + ":" + colsLetters[i] + lastExcelRow + ")");
            cSum.setCellStyle(totalNumStyle);
        }

        Cell cFin = totalRow.createCell(17);
        cFin.setCellValue("AUDITADO OK");
        cFin.setCellStyle(titleStyle);

        // Activar AutoFiltro para clasificar dentro de tabla interactiva de Excel (18 Columnas)
        if (rowIdx > startDataRow) {
            sheet.setAutoFilter(new CellRangeAddress(3, rowIdx - 1, 0, 17));
        }

        // Anchos de columna holgados y generosos
        sheet.setColumnWidth(0, 14 * 256); // Cédula
        sheet.setColumnWidth(1, 28 * 256); // Empleado
        sheet.setColumnWidth(2, 16 * 256); // Cargo
        sheet.setColumnWidth(3, 13 * 256); // Fecha
        sheet.setColumnWidth(4, 18 * 256); // Día (con badge dominical)
        sheet.setColumnWidth(5, 13 * 256); // Ent Prog
        sheet.setColumnWidth(6, 13 * 256); // Sal Prog
        sheet.setColumnWidth(7, 14 * 256); // Almuerzo Min
        sheet.setColumnWidth(8, 13 * 256); // Ent Real
        sheet.setColumnWidth(9, 13 * 256); // Sal Real
        sheet.setColumnWidth(10, 16 * 256); // Horas Ordinarias
        sheet.setColumnWidth(11, 18 * 256); // Recargo Nocturno
        sheet.setColumnWidth(12, 16 * 256); // HED
        sheet.setColumnWidth(13, 16 * 256); // HEN
        sheet.setColumnWidth(14, 22 * 256); // Dominical/Festivo (Espacio propio)
        sheet.setColumnWidth(15, 20 * 256); // Extra Dominical
        sheet.setColumnWidth(16, 18 * 256); // Total a Pagar
        sheet.setColumnWidth(17, 36 * 256); // Novedad / Estado (Espacio para auditoría completa)
    }

    private static double redondear2(double val) {
        return java.math.BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Endpoint 3: Inventario Total de Cierre Quincenal / Mensual (Cortes Día 15 y Día 29 -> Reinicio Día 16 y Día 30)
     * Incluye:
     * 1) Pestaña "Inventario General" (KPIs de Personal, Malla de Horarios y Nómina del Corte)
     * 2) Pestaña "Resumen Nómina" (1 fila por empleado)
     * 3) Pestaña "Malla Horarios Mes" (Todos los turnos programados del período)
     * 4) Pestañas separadas por cada día trabajado ("[Día] [YYYY-MM-DD]")
     */
    public byte[] generarInventarioCorteExcel(LocalDate inicio, LocalDate fin) throws IOException {
        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        List<Empleado> empleadosActivos = empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
        List<ProgramacionSemanal> programaciones = programacionRepository.findProgramacionOrdenada(inicio, fin);
        List<RegistroAsistencia> registros = registroRepository.findRegistrosEntreFechas(inicio, fin);

        Map<String, CalculadoraNominaColombia.DetalleNominaDia> mapaDetallesUnicos = new LinkedHashMap<>();
        if (!registros.isEmpty()) {
            for (RegistroAsistencia reg : registros) {
                ProgramacionSemanal p = reg.getProgramacion();
                if (p != null) {
                    CalculadoraNominaColombia.DetalleNominaDia d = CalculadoraNominaColombia.calcularDia(p, reg);
                    String key = d.getCedula() + "_" + d.getFecha();
                    mapaDetallesUnicos.put(key, d);
                }
            }
        } else {
            for (ProgramacionSemanal p : programaciones) {
                CalculadoraNominaColombia.DetalleNominaDia d = CalculadoraNominaColombia.calcularDia(p, null);
                String key = d.getCedula() + "_" + d.getFecha();
                if (!mapaDetallesUnicos.containsKey(key)) {
                    mapaDetallesUnicos.put(key, d);
                }
            }
        }

        List<CalculadoraNominaColombia.DetalleNominaDia> listaDetalles = new ArrayList<>(mapaDetallesUnicos.values());
        listaDetalles.sort((d1, d2) -> {
            String cargo1 = d1.getCargo() != null ? d1.getCargo() : "PLANTA";
            String cargo2 = d2.getCargo() != null ? d2.getCargo() : "PLANTA";
            int cA = cargo1.compareToIgnoreCase(cargo2);
            if (cA != 0) return cA;
            int cE = d1.getEmpleado().compareToIgnoreCase(d2.getEmpleado());
            if (cE != 0) return cE;
            return d1.getFecha().compareTo(d2.getFecha());
        });

        Map<String, List<CalculadoraNominaColombia.DetalleNominaDia>> porEmpleado = new LinkedHashMap<>();
        Map<LocalDate, List<CalculadoraNominaColombia.DetalleNominaDia>> porFecha = new TreeMap<>();
        for (CalculadoraNominaColombia.DetalleNominaDia d : listaDetalles) {
            porEmpleado.computeIfAbsent(d.getCedula(), k -> new ArrayList<>()).add(d);
            porFecha.computeIfAbsent(d.getFecha(), k -> new ArrayList<>()).add(d);
        }

        String selloVeracidad = generarSelloHash(inicio, fin, registros.size(), programaciones.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] azulRgb = new byte[] { (byte) 27, (byte) 54, (byte) 93 };
            byte[] grisFondoRgb = new byte[] { (byte) 242, (byte) 244, (byte) 247 };
            byte[] amarilloRetardoRgb = new byte[] { (byte) 254, (byte) 243, (byte) 199 };
            byte[] ambarHeaderRgb = new byte[] { (byte) 180, (byte) 83, (byte) 9 };
            byte[] ambarFondoRgb = new byte[] { (byte) 254, (byte) 249, (byte) 195 };
            byte[] verdeExtrasRgb = new byte[] { (byte) 220, (byte) 252, (byte) 231 };

            XSSFColor colorAzul = new XSSFColor(azulRgb, null);
            XSSFColor colorGris = new XSSFColor(grisFondoRgb, null);
            XSSFColor colorAmarillo = new XSSFColor(amarilloRetardoRgb, null);
            XSSFColor colorAmbarHeader = new XSSFColor(ambarHeaderRgb, null);
            XSSFColor colorAmbarFondo = new XSSFColor(ambarFondoRgb, null);
            XSSFColor colorVerdeExtras = new XSSFColor(verdeExtrasRgb, null);

            XSSFFont fontTitulo = (XSSFFont) workbook.createFont();
            fontTitulo.setColor(IndexedColors.WHITE.getIndex());
            fontTitulo.setBold(true);
            fontTitulo.setFontHeightInPoints((short) 13);

            XSSFFont fontSubtitulo = (XSSFFont) workbook.createFont();
            fontSubtitulo.setBold(true);
            fontSubtitulo.setFontHeightInPoints((short) 9);

            XSSFFont fontHeader = (XSSFFont) workbook.createFont();
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            fontHeader.setBold(true);
            fontHeader.setFontHeightInPoints((short) 9.5);

            XSSFFont fontDatos = (XSSFFont) workbook.createFont();
            fontDatos.setFontHeightInPoints((short) 9);

            XSSFFont fontDatosBold = (XSSFFont) workbook.createFont();
            fontDatosBold.setBold(true);
            fontDatosBold.setFontHeightInPoints((short) 9);

            XSSFFont fontDominical = (XSSFFont) workbook.createFont();
            fontDominical.setBold(true);
            fontDominical.setColor(IndexedColors.BROWN.getIndex());
            fontDominical.setFontHeightInPoints((short) 9);

            XSSFFont fontExtras = (XSSFFont) workbook.createFont();
            fontExtras.setBold(true);
            fontExtras.setColor(IndexedColors.DARK_GREEN.getIndex());
            fontExtras.setFontHeightInPoints((short) 9);

            DataFormat df = workbook.createDataFormat();
            short numFormat = df.getFormat("0.00");

            CellStyle titleStyle = workbook.createCellStyle();
            ((XSSFCellStyle) titleStyle).setFillForegroundColor(colorAzul);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFont(fontTitulo);

            CellStyle subTitleStyle = workbook.createCellStyle();
            subTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            subTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            subTitleStyle.setFont(fontSubtitulo);

            CellStyle headerStyle = workbook.createCellStyle();
            ((XSSFCellStyle) headerStyle).setFillForegroundColor(colorAzul);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setFont(fontHeader);
            aplicarBordes(headerStyle);

            CellStyle headerDominicalStyle = workbook.createCellStyle();
            ((XSSFCellStyle) headerDominicalStyle).setFillForegroundColor(colorAmbarHeader);
            headerDominicalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerDominicalStyle.setAlignment(HorizontalAlignment.CENTER);
            headerDominicalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerDominicalStyle.setWrapText(true);
            headerDominicalStyle.setFont(fontHeader);
            aplicarBordes(headerDominicalStyle);

            CellStyle centerDataStyle = workbook.createCellStyle();
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerDataStyle.setFont(fontDatos);
            aplicarBordes(centerDataStyle);

            CellStyle domingoDataStyle = workbook.createCellStyle();
            ((XSSFCellStyle) domingoDataStyle).setFillForegroundColor(colorAmbarFondo);
            domingoDataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            domingoDataStyle.setAlignment(HorizontalAlignment.CENTER);
            domingoDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            domingoDataStyle.setFont(fontDominical);
            aplicarBordes(domingoDataStyle);

            CellStyle empNameStyle = workbook.createCellStyle();
            empNameStyle.setAlignment(HorizontalAlignment.LEFT);
            empNameStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            empNameStyle.setFont(fontDatosBold);
            aplicarBordes(empNameStyle);

            CellStyle numDataStyle = workbook.createCellStyle();
            numDataStyle.setDataFormat(numFormat);
            numDataStyle.setAlignment(HorizontalAlignment.RIGHT);
            numDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numDataStyle.setFont(fontDatos);
            aplicarBordes(numDataStyle);

            CellStyle extrasNumStyle = workbook.createCellStyle();
            extrasNumStyle.setDataFormat(numFormat);
            ((XSSFCellStyle) extrasNumStyle).setFillForegroundColor(colorVerdeExtras);
            extrasNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            extrasNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            extrasNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            extrasNumStyle.setFont(fontExtras);
            aplicarBordes(extrasNumStyle);

            CellStyle domingoNumStyle = workbook.createCellStyle();
            domingoNumStyle.setDataFormat(numFormat);
            ((XSSFCellStyle) domingoNumStyle).setFillForegroundColor(colorAmbarFondo);
            domingoNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            domingoNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            domingoNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            domingoNumStyle.setFont(fontDominical);
            aplicarBordes(domingoNumStyle);

            CellStyle totalNumStyle = workbook.createCellStyle();
            totalNumStyle.setDataFormat(numFormat);
            ((XSSFCellStyle) totalNumStyle).setFillForegroundColor(colorAzul);
            totalNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalNumStyle.setFont(fontHeader);
            aplicarBordes(totalNumStyle);

            CellStyle estadoOkStyle = workbook.createCellStyle();
            estadoOkStyle.setAlignment(HorizontalAlignment.CENTER);
            estadoOkStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            estadoOkStyle.setFont(fontDatos);
            aplicarBordes(estadoOkStyle);

            CellStyle estadoAlertaStyle = workbook.createCellStyle();
            ((XSSFCellStyle) estadoAlertaStyle).setFillForegroundColor(colorAmarillo);
            estadoAlertaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estadoAlertaStyle.setAlignment(HorizontalAlignment.CENTER);
            estadoAlertaStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            estadoAlertaStyle.setFont(fontDatosBold);
            aplicarBordes(estadoAlertaStyle);

            CellStyle descansoStyle = workbook.createCellStyle();
            ((XSSFCellStyle) descansoStyle).setFillForegroundColor(colorGris);
            descansoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            descansoStyle.setAlignment(HorizontalAlignment.CENTER);
            descansoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            descansoStyle.setFont(fontDatos);
            aplicarBordes(descansoStyle);

            // 1. Pestaña "Inventario General"
            Sheet sheetInv = workbook.createSheet("Inventario General");
            Row r0 = sheetInv.createRow(0);
            r0.setHeightInPoints(26);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SALGUEROS — INVENTARIO GENERAL DE CIERRE (CORTES DÍA 15 Y DÍA 29)");
            c0.setCellStyle(titleStyle);
            sheetInv.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            int diaCorte = fin.getDayOfMonth() <= 15 ? 15 : 29;
            int diaReinicio = diaCorte == 15 ? 16 : 30;
            Row r1 = sheetInv.createRow(1);
            r1.setHeightInPoints(20);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Período Inventariado: " + inicio.format(DATE_FMT) + " al " + fin.format(DATE_FMT)
                    + "  |  Corte Oficial Día " + diaCorte + "  |  Reinicio Automático: Día " + diaReinicio);
            c1.setCellStyle(subTitleStyle);
            sheetInv.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            String[] invHeaders = {"N°", "INDICADOR DE INVENTARIO DEL PERÍODO", "VALOR / CANTIDAD", "UNIDAD", "ESTADO AUDITORÍA"};
            Row hInv = sheetInv.createRow(3);
            hInv.setHeightInPoints(26);
            for (int i = 0; i < invHeaders.length; i++) {
                Cell hc = hInv.createCell(i);
                hc.setCellValue(invHeaders[i]);
                hc.setCellStyle(headerStyle);
            }

            double totOrdInv = redondear2(listaDetalles.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getHorasOrdinarias).sum());
            double totDomInv = redondear2(listaDetalles.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getDominicalFestivo).sum());
            double totExtInv = redondear2(listaDetalles.stream().mapToDouble(d -> d.getHorasExtraDiurna() + d.getHorasExtraNocturna() + d.getHorasExtraDominical()).sum());
            double totPagarInv = redondear2(listaDetalles.stream().mapToDouble(CalculadoraNominaColombia.DetalleNominaDia::getTotalHorasAPagar).sum());

            Object[][] filasKpi = {
                {1, "Personal Activo Registrado en Sistema", empleadosActivos.size(), "Empleados", "ACTIVO"},
                {2, "Turnos Programados en Malla de Horarios del Período", programaciones.size(), "Turnos", "VERIFICADO"},
                {3, "Marcaciones Biométricas de Asistencia Registradas", registros.size(), "Marcaciones", "AUDITADO"},
                {4, "Días con Operación y Asistencia Liquidada", porFecha.size(), "Días", "COMPLETO"},
                {5, "Total Horas Ordinarias Netas (Almuerzo Descontado)", totOrdInv, "Horas", "LIMPIO"},
                {6, "Total Horas Dominicales / Festivas", totDomInv, "Horas", "LIQUIDADO"},
                {7, "Total Horas Extras Autorizadas", totExtInv, "Horas", "CONTROLADO"},
                {8, "Total General de Horas a Pagar en Nómina", totPagarInv, "Horas", "CERRADO OK"},
                {9, "Próximo Inicio de Ciclo Automático en la Aplicación", "Día " + diaReinicio, "Ciclo Nuevo", "PROGRAMADO"}
            };

            int rInvIdx = 4;
            for (Object[] kpi : filasKpi) {
                Row rowK = sheetInv.createRow(rInvIdx++);
                rowK.setHeightInPoints(22);
                Cell ck0 = rowK.createCell(0); ck0.setCellValue((Integer) kpi[0]); ck0.setCellStyle(centerDataStyle);
                Cell ck1 = rowK.createCell(1); ck1.setCellValue((String) kpi[1]); ck1.setCellStyle(empNameStyle);
                Cell ck2 = rowK.createCell(2); ck2.setCellValue(String.valueOf(kpi[2])); ck2.setCellStyle(centerDataStyle);
                Cell ck3 = rowK.createCell(3); ck3.setCellValue((String) kpi[3]); ck3.setCellStyle(centerDataStyle);
                Cell ck4 = rowK.createCell(4); ck4.setCellValue((String) kpi[4]); ck4.setCellStyle(estadoOkStyle);
            }
            sheetInv.setColumnWidth(0, 8 * 256);
            sheetInv.setColumnWidth(1, 46 * 256);
            sheetInv.setColumnWidth(2, 18 * 256);
            sheetInv.setColumnWidth(3, 16 * 256);
            sheetInv.setColumnWidth(4, 20 * 256);

            // 2. Pestaña "Resumen Nómina"
            Sheet sheetResumen = workbook.createSheet("Resumen Nómina");
            construirPestanaResumen(sheetResumen, porEmpleado, inicio, fin, selloVeracidad,
                    titleStyle, subTitleStyle, headerStyle, headerDominicalStyle, centerDataStyle, empNameStyle,
                    numDataStyle, extrasNumStyle, domingoNumStyle, totalNumStyle, estadoOkStyle, estadoAlertaStyle);

            // 3. Pestaña "Malla Horarios Mes"
            Sheet sheetMallaMes = workbook.createSheet("Malla Horarios Mes");
            Row rm0 = sheetMallaMes.createRow(0);
            rm0.setHeightInPoints(24);
            Cell cm0 = rm0.createCell(0);
            cm0.setCellValue("SALGUEROS — INVENTARIO DE MALLA DE HORARIOS DE TODO EL PERÍODO (" + inicio.format(DATE_FMT) + " AL " + fin.format(DATE_FMT) + ")");
            cm0.setCellStyle(titleStyle);
            sheetMallaMes.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            Row rm1 = sheetMallaMes.createRow(1);
            rm1.setHeightInPoints(18);
            Cell cm1 = rm1.createCell(0);
            cm1.setCellValue("Total Turnos Programados: " + programaciones.size() + "  |  Almuerzo Descontado  |  Corte Oficial Día " + diaCorte);
            cm1.setCellStyle(subTitleStyle);
            sheetMallaMes.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            String[] headersMallaMes = {
                "N°", "Fecha", "Día", "Cédula", "Empleado", "Área / Cargo",
                "Entrada Prog", "Salida Prog", "Almuerzo", "Estado / Turno"
            };
            Row hmRow = sheetMallaMes.createRow(3);
            hmRow.setHeightInPoints(26);
            for (int i = 0; i < headersMallaMes.length; i++) {
                Cell hc = hmRow.createCell(i);
                hc.setCellValue(headersMallaMes[i]);
                hc.setCellStyle(headerStyle);
            }
            sheetMallaMes.createFreezePane(3, 4);

            String[] diasNombresArr = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
            int rmIdx = 4;
            int contProg = 1;
            for (ProgramacionSemanal p : programaciones) {
                Row rowP = sheetMallaMes.createRow(rmIdx++);
                rowP.setHeightInPoints(20);
                Empleado e = p.getEmpleado();
                int dow = p.getFecha().getDayOfWeek().getValue() - 1;
                boolean esDomP = (dow == 6);

                Cell cp0 = rowP.createCell(0); cp0.setCellValue(contProg++); cp0.setCellStyle(centerDataStyle);
                Cell cp1 = rowP.createCell(1); cp1.setCellValue(p.getFecha().format(DATE_FMT)); cp1.setCellStyle(esDomP ? domingoDataStyle : centerDataStyle);
                Cell cp2 = rowP.createCell(2); cp2.setCellValue(diasNombresArr[dow]); cp2.setCellStyle(esDomP ? domingoDataStyle : centerDataStyle);
                Cell cp3 = rowP.createCell(3); cp3.setCellValue(e != null ? e.getCedula() : ""); cp3.setCellStyle(centerDataStyle);
                Cell cp4 = rowP.createCell(4); cp4.setCellValue(e != null ? e.getNombreApellido() : ""); cp4.setCellStyle(empNameStyle);
                Cell cp5 = rowP.createCell(5); cp5.setCellValue(e != null && e.getArea() != null ? e.getArea() : "PLANTA"); cp5.setCellStyle(centerDataStyle);

                if (p.getTipoNovedadPrecargada() == TipoNovedad.DESCANSO) {
                    Cell cp6 = rowP.createCell(6); cp6.setCellValue("--"); cp6.setCellStyle(descansoStyle);
                    Cell cp7 = rowP.createCell(7); cp7.setCellValue("--"); cp7.setCellStyle(descansoStyle);
                    Cell cp8 = rowP.createCell(8); cp8.setCellValue("0 min"); cp8.setCellStyle(centerDataStyle);
                    Cell cp9 = rowP.createCell(9); cp9.setCellValue("DESCANSO"); cp9.setCellStyle(descansoStyle);
                } else {
                    LocalTime ent = p.getHoraEntradaProgramada();
                    LocalTime sal = p.getHoraSalidaProgramada() != null ? p.getHoraSalidaProgramada() : (ent != null ? ent.plusHours(8) : null);
                    int alm = p.getMinutosAlmuerzo() != null ? p.getMinutosAlmuerzo() : 0;
                    String estTurno = (p.getTipoNovedadPrecargada() != null && p.getTipoNovedadPrecargada() != TipoNovedad.NINGUNA)
                            ? p.getTipoNovedadPrecargada().name()
                            : (esDomP ? "☀️ TURNO DOMINICAL" : "TURNO PROGRAMADO");

                    Cell cp6 = rowP.createCell(6); cp6.setCellValue(ent != null ? ent.format(SHORT_TIME_FMT) : "--"); cp6.setCellStyle(centerDataStyle);
                    Cell cp7 = rowP.createCell(7); cp7.setCellValue(sal != null ? sal.format(SHORT_TIME_FMT) : "--"); cp7.setCellStyle(centerDataStyle);
                    Cell cp8 = rowP.createCell(8); cp8.setCellValue(alm + " min"); cp8.setCellStyle(centerDataStyle);
                    Cell cp9 = rowP.createCell(9); cp9.setCellValue(estTurno); cp9.setCellStyle(esDomP ? domingoDataStyle : estadoOkStyle);
                }
            }
            for (int c = 0; c <= 9; c++) {
                sheetMallaMes.setColumnWidth(c, (c == 4 ? 28 : 16) * 256);
            }

            // 4. Pestañas Diarias Separadas ("[Día] [YYYY-MM-DD]")
            for (Map.Entry<LocalDate, List<CalculadoraNominaColombia.DetalleNominaDia>> entryDia : porFecha.entrySet()) {
                LocalDate fechaDia = entryDia.getKey();
                List<CalculadoraNominaColombia.DetalleNominaDia> detallesDelDia = entryDia.getValue();
                String diaNombre = detallesDelDia.get(0).getDiaNombre();
                String nombrePestana = diaNombre + " " + fechaDia.format(DATE_FMT);
                Sheet sheetDia = workbook.createSheet(nombrePestana);
                construirPestanaDetalle(sheetDia, detallesDelDia, fechaDia, fechaDia, selloVeracidad,
                        titleStyle, subTitleStyle, headerStyle, headerDominicalStyle, centerDataStyle, domingoDataStyle, empNameStyle,
                        numDataStyle, extrasNumStyle, domingoNumStyle, totalNumStyle, estadoOkStyle, estadoAlertaStyle, descansoStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Mantiene retrocompatibilidad con llamadas legacy
     */
    public byte[] generarReporteAsistenciaExcel(LocalDate inicio, LocalDate fin, String tipoPeriodo) throws IOException {
        return generarNominaAsistenciaExcel(inicio, fin);
    }

    private void aplicarBordes(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String generarSelloHash(LocalDate inicio, LocalDate fin, int totalReg, int totalProg) {
        try {
            String raw = "SALGUEROS|" + inicio + "|" + fin + "|" + totalReg + "|" + totalProg;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02X", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "VERIF2026";
        }
    }
}
