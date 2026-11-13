package com.salgueros.asistencia.service;

import com.salgueros.asistencia.model.EstadoLlegada;
import com.salgueros.asistencia.model.ProgramacionSemanal;
import com.salgueros.asistencia.model.RegistroAsistencia;
import com.salgueros.asistencia.model.TipoNovedad;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Motor de Cálculo de Nómina, Horas Extras y Recargos según la Legislación Laboral de Colombia.
 *
 * Franjas:
 *  - Diurna: 06:00 a 19:00 (Jornada diurna en Colombia).
 *  - Nocturna: 19:00 a 06:00 (Recargo Nocturno Ordinario 35% desde las 7:00 PM).
 *
 * Extras y Recargos:
 *  1. Horas Ordinarias: Dentro del horario programado en días normales.
 *  2. Recargo Nocturno (RN - 35%): Ordinarias laboradas entre 19:00 y 06:00.
 *  3. Hora Extra Diurna (HED - 25%): Tiempo excedente en franja diurna (06:00 a 19:00).
 *  4. Hora Extra Nocturna (HEN - 75%): Tiempo excedente en franja nocturna (19:00 a 06:00).
 *  5. Recargo Dominical / Festivo (75%): Horas laboradas en domingos o festivos.
 *  6. Hora Extra Dominical (Diurna 100% / Nocturna 150%): Tiempo excedente en domingos o festivos.
 */
public final class CalculadoraNominaColombia {

    private CalculadoraNominaColombia() {}

    private static final DateTimeFormatter SHORT_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<LocalDate> FESTIVOS_COLOMBIA = new HashSet<>();

    static {
        // Festivos Colombia 2025
        List<String> f2025 = List.of(
            "2025-01-01", "2025-01-06", "2025-03-24", "2025-04-17", "2025-04-18",
            "2025-05-01", "2025-06-02", "2025-06-23", "2025-06-30", "2025-07-20",
            "2025-08-07", "2025-08-18", "2025-10-13", "2025-11-03", "2025-11-17",
            "2025-12-08", "2025-12-25"
        );
        for (String s : f2025) FESTIVOS_COLOMBIA.add(LocalDate.parse(s));

        // Festivos Colombia 2026
        List<String> f2026 = List.of(
            "2026-01-01", "2026-01-12", "2026-03-23", "2026-04-02", "2026-04-03",
            "2026-05-01", "2026-05-18", "2026-06-08", "2026-06-15", "2026-06-29",
            "2026-07-20", "2026-08-07", "2026-08-17", "2026-10-12", "2026-11-02",
            "2026-11-16", "2026-12-08", "2026-12-25"
        );
        for (String s : f2026) FESTIVOS_COLOMBIA.add(LocalDate.parse(s));

        // Festivos Colombia 2027
        List<String> f2027 = List.of(
            "2027-01-01", "2027-01-11", "2027-03-22", "2027-03-25", "2027-03-26",
            "2027-05-01", "2027-05-10", "2027-05-31", "2027-06-07", "2027-07-05",
            "2027-07-20", "2027-08-07", "2027-08-16", "2027-10-18", "2027-11-01",
            "2027-11-15", "2027-12-08", "2027-12-25"
        );
        for (String s : f2027) FESTIVOS_COLOMBIA.add(LocalDate.parse(s));
    }

    public static boolean esDominicalOFestivo(LocalDate fecha) {
        if (fecha == null) return false;
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) return true;
        return FESTIVOS_COLOMBIA.contains(fecha);
    }

    public static class DetalleNominaDia {
        private String cedula;
        private String empleado;
        private String cargo;
        private LocalDate fecha;
        private String diaNombre;
        private LocalTime entradaProg;
        private LocalTime salidaProg;
        private int minutosAlmuerzo;
        private LocalDateTime entradaReal;
        private LocalDateTime salidaReal;
        private double horasProgramadas;
        private double horasLaboradasReales;
        private double horasOrdinarias;
        private double recargoNocturno;
        private double horasExtraDiurna;
        private double horasExtraNocturna;
        private double dominicalFestivo;
        private double horasExtraDominical;
        private double totalHorasAPagar;
        private String novedadEstado;
        private boolean bloqueado;

        public DetalleNominaDia() {}

        public DetalleNominaDia(String cedula, String empleado, String cargo, LocalDate fecha, String diaNombre,
                                LocalTime entradaProg, LocalTime salidaProg, int minutosAlmuerzo,
                                LocalDateTime entradaReal, LocalDateTime salidaReal, double horasProgramadas,
                                double horasLaboradasReales, double horasOrdinarias, double recargoNocturno,
                                double horasExtraDiurna, double horasExtraNocturna, double dominicalFestivo,
                                double horasExtraDominical, double totalHorasAPagar, String novedadEstado,
                                boolean bloqueado) {
            this.cedula = cedula;
            this.empleado = empleado;
            this.cargo = cargo;
            this.fecha = fecha;
            this.diaNombre = diaNombre;
            this.entradaProg = entradaProg;
            this.salidaProg = salidaProg;
            this.minutosAlmuerzo = minutosAlmuerzo;
            this.entradaReal = entradaReal;
            this.salidaReal = salidaReal;
            this.horasProgramadas = horasProgramadas;
            this.horasLaboradasReales = horasLaboradasReales;
            this.horasOrdinarias = horasOrdinarias;
            this.recargoNocturno = recargoNocturno;
            this.horasExtraDiurna = horasExtraDiurna;
            this.horasExtraNocturna = horasExtraNocturna;
            this.dominicalFestivo = dominicalFestivo;
            this.horasExtraDominical = horasExtraDominical;
            this.totalHorasAPagar = totalHorasAPagar;
            this.novedadEstado = novedadEstado;
            this.bloqueado = bloqueado;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getCedula() { return cedula; }
        public void setCedula(String cedula) { this.cedula = cedula; }
        public String getEmpleado() { return empleado; }
        public void setEmpleado(String empleado) { this.empleado = empleado; }
        public String getCargo() { return cargo; }
        public void setCargo(String cargo) { this.cargo = cargo; }
        public LocalDate getFecha() { return fecha; }
        public void setFecha(LocalDate fecha) { this.fecha = fecha; }
        public String getDiaNombre() { return diaNombre; }
        public void setDiaNombre(String diaNombre) { this.diaNombre = diaNombre; }
        public LocalTime getEntradaProg() { return entradaProg; }
        public void setEntradaProg(LocalTime entradaProg) { this.entradaProg = entradaProg; }
        public LocalTime getSalidaProg() { return salidaProg; }
        public void setSalidaProg(LocalTime salidaProg) { this.salidaProg = salidaProg; }
        public int getMinutosAlmuerzo() { return minutosAlmuerzo; }
        public void setMinutosAlmuerzo(int minutosAlmuerzo) { this.minutosAlmuerzo = minutosAlmuerzo; }
        public LocalDateTime getEntradaReal() { return entradaReal; }
        public void setEntradaReal(LocalDateTime entradaReal) { this.entradaReal = entradaReal; }
        public LocalDateTime getSalidaReal() { return salidaReal; }
        public void setSalidaReal(LocalDateTime salidaReal) { this.salidaReal = salidaReal; }
        public double getHorasProgramadas() { return horasProgramadas; }
        public void setHorasProgramadas(double horasProgramadas) { this.horasProgramadas = horasProgramadas; }
        public double getHorasLaboradasReales() { return horasLaboradasReales; }
        public void setHorasLaboradasReales(double horasLaboradasReales) { this.horasLaboradasReales = horasLaboradasReales; }
        public double getHorasOrdinarias() { return horasOrdinarias; }
        public void setHorasOrdinarias(double horasOrdinarias) { this.horasOrdinarias = horasOrdinarias; }
        public double getRecargoNocturno() { return recargoNocturno; }
        public void setRecargoNocturno(double recargoNocturno) { this.recargoNocturno = recargoNocturno; }
        public double getHorasExtraDiurna() { return horasExtraDiurna; }
        public void setHorasExtraDiurna(double horasExtraDiurna) { this.horasExtraDiurna = horasExtraDiurna; }
        public double getHorasExtraNocturna() { return horasExtraNocturna; }
        public void setHorasExtraNocturna(double horasExtraNocturna) { this.horasExtraNocturna = horasExtraNocturna; }
        public double getDominicalFestivo() { return dominicalFestivo; }
        public void setDominicalFestivo(double dominicalFestivo) { this.dominicalFestivo = dominicalFestivo; }
        public double getHorasExtraDominical() { return horasExtraDominical; }
        public void setHorasExtraDominical(double horasExtraDominical) { this.horasExtraDominical = horasExtraDominical; }
        public double getTotalHorasAPagar() { return totalHorasAPagar; }
        public void setTotalHorasAPagar(double totalHorasAPagar) { this.totalHorasAPagar = totalHorasAPagar; }
        public String getNovedadEstado() { return novedadEstado; }
        public void setNovedadEstado(String novedadEstado) { this.novedadEstado = novedadEstado; }
        public boolean isBloqueado() { return bloqueado; }
        public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }

        public static class Builder {
            private String cedula;
            private String empleado;
            private String cargo;
            private LocalDate fecha;
            private String diaNombre;
            private LocalTime entradaProg;
            private LocalTime salidaProg;
            private int minutosAlmuerzo;
            private LocalDateTime entradaReal;
            private LocalDateTime salidaReal;
            private double horasProgramadas;
            private double horasLaboradasReales;
            private double horasOrdinarias;
            private double recargoNocturno;
            private double horasExtraDiurna;
            private double horasExtraNocturna;
            private double dominicalFestivo;
            private double horasExtraDominical;
            private double totalHorasAPagar;
            private String novedadEstado;
            private boolean bloqueado;

            public Builder cedula(String cedula) { this.cedula = cedula; return this; }
            public Builder empleado(String empleado) { this.empleado = empleado; return this; }
            public Builder cargo(String cargo) { this.cargo = cargo; return this; }
            public Builder fecha(LocalDate fecha) { this.fecha = fecha; return this; }
            public Builder diaNombre(String diaNombre) { this.diaNombre = diaNombre; return this; }
            public Builder entradaProg(LocalTime entradaProg) { this.entradaProg = entradaProg; return this; }
            public Builder salidaProg(LocalTime salidaProg) { this.salidaProg = salidaProg; return this; }
            public Builder minutosAlmuerzo(int minutosAlmuerzo) { this.minutosAlmuerzo = minutosAlmuerzo; return this; }
            public Builder entradaReal(LocalDateTime entradaReal) { this.entradaReal = entradaReal; return this; }
            public Builder salidaReal(LocalDateTime salidaReal) { this.salidaReal = salidaReal; return this; }
            public Builder horasProgramadas(double horasProgramadas) { this.horasProgramadas = horasProgramadas; return this; }
            public Builder horasLaboradasReales(double horasLaboradasReales) { this.horasLaboradasReales = horasLaboradasReales; return this; }
            public Builder horasOrdinarias(double horasOrdinarias) { this.horasOrdinarias = horasOrdinarias; return this; }
            public Builder recargoNocturno(double recargoNocturno) { this.recargoNocturno = recargoNocturno; return this; }
            public Builder horasExtraDiurna(double horasExtraDiurna) { this.horasExtraDiurna = horasExtraDiurna; return this; }
            public Builder horasExtraNocturna(double horasExtraNocturna) { this.horasExtraNocturna = horasExtraNocturna; return this; }
            public Builder dominicalFestivo(double dominicalFestivo) { this.dominicalFestivo = dominicalFestivo; return this; }
            public Builder horasExtraDominical(double horasExtraDominical) { this.horasExtraDominical = horasExtraDominical; return this; }
            public Builder totalHorasAPagar(double totalHorasAPagar) { this.totalHorasAPagar = totalHorasAPagar; return this; }
            public Builder novedadEstado(String novedadEstado) { this.novedadEstado = novedadEstado; return this; }
            public Builder bloqueado(boolean bloqueado) { this.bloqueado = bloqueado; return this; }

            public DetalleNominaDia build() {
                return new DetalleNominaDia(cedula, empleado, cargo, fecha, diaNombre, entradaProg,
                        salidaProg, minutosAlmuerzo, entradaReal, salidaReal, horasProgramadas,
                        horasLaboradasReales, horasOrdinarias, recargoNocturno, horasExtraDiurna,
                        horasExtraNocturna, dominicalFestivo, horasExtraDominical, totalHorasAPagar,
                        novedadEstado, bloqueado);
            }
        }
    }

    public static DetalleNominaDia calcularDia(ProgramacionSemanal prog, RegistroAsistencia reg) {
        LocalDate fecha = prog != null ? prog.getFecha()
                : (reg != null && reg.getHoraEntradaReal() != null ? reg.getHoraEntradaReal().toLocalDate() : LocalDate.now());

        String diaNombre = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
        diaNombre = diaNombre.substring(0, 1).toUpperCase(new Locale("es", "CO")) + diaNombre.substring(1);

        String cedula = prog != null ? prog.getEmpleado().getCedula()
                : (reg != null ? reg.getProgramacion().getEmpleado().getCedula() : "--");
        String nombre = prog != null ? prog.getEmpleado().getNombreApellido()
                : (reg != null ? reg.getProgramacion().getEmpleado().getNombreApellido() : "--");
        String cargo = prog != null ? (prog.getEmpleado().getArea() != null ? prog.getEmpleado().getArea() : "PLANTA")
                : (reg != null && reg.getProgramacion().getEmpleado().getArea() != null ? reg.getProgramacion().getEmpleado().getArea() : "PLANTA");

        LocalTime entProg = prog != null ? prog.getHoraEntradaProgramada() : null;
        LocalTime salProg = prog != null ? (prog.getHoraSalidaProgramada() != null ? prog.getHoraSalidaProgramada() : (entProg != null ? entProg.plusHours(8) : null)) : null;
        int minAlmuerzo = prog != null ? prog.getMinutosAlmuerzo() : 0;
        boolean bloqueado = prog != null && Boolean.TRUE.equals(prog.getBloqueado());

        double horasProgramadas = 0.0;
        if (entProg != null && salProg != null) {
            long minTurno = Duration.between(entProg, salProg).toMinutes();
            if (minTurno < 0) minTurno += 24 * 60;
            if (minAlmuerzo == 0) {
                if (minTurno == 510 || minTurno == 570) {
                    minAlmuerzo = 30;
                } else if (minTurno >= 540) {
                    minAlmuerzo = 60;
                }
            }
            long minNetos = Math.max(0, minTurno - minAlmuerzo);
            horasProgramadas = redondear2(minNetos / 60.0);
        }

        LocalDateTime entRealRaw = reg != null ? reg.getHoraEntradaReal() : null;
        LocalDateTime salReal = reg != null ? reg.getHoraSalidaReal() : null;

        // Normalización de Hora de Entrada (Llegada temprana y Alivio de 8 minutos):
        // 1. Si llega más temprano de su hora habitual (entReal < entProg), se acerca a entProg (A TIEMPO).
        // 2. Si el registro aún tiene segundos crudos sin ajuste de alivio:
        //    - Dentro de 8 min: se acerca a entProg (0 retardo, A TIEMPO).
        //    - Más de 8 min: se dan los 8 min de alivio hacia la hora que tenía que llegar (diff - 8).
        LocalDateTime entReal = entRealRaw;
        int minutosRetardoEfectivo = reg != null && reg.getMinutosRetardo() != null ? reg.getMinutosRetardo() : 0;
        EstadoLlegada estadoLlegadaEfectivo = reg != null && reg.getEstadoLlegada() != null ? reg.getEstadoLlegada() : EstadoLlegada.A_TIEMPO;

        if (entRealRaw != null && entProg != null) {
            LocalDateTime entProgDT = LocalDateTime.of(fecha, entProg);
            if (entRealRaw.isBefore(entProgDT)) {
                entReal = entProgDT;
                minutosRetardoEfectivo = 0;
                estadoLlegadaEfectivo = EstadoLlegada.A_TIEMPO;
            } else if (entRealRaw.getSecond() != 0) {
                long diffMin = Duration.between(entProgDT, entRealRaw).toMinutes();
                if (diffMin <= 8) {
                    entReal = entProgDT;
                    minutosRetardoEfectivo = 0;
                    estadoLlegadaEfectivo = EstadoLlegada.A_TIEMPO;
                } else {
                    int retEfectivo = (int) (diffMin - 8);
                    entReal = entProgDT.plusMinutes(retEfectivo);
                    minutosRetardoEfectivo = retEfectivo;
                    estadoLlegadaEfectivo = EstadoLlegada.RETARDO;
                }
            }
        }

        // Caso 1: No hubo marcación biométrica
        if (reg == null || entReal == null) {
            String novedad;
            if (prog != null && prog.getTipoNovedadPrecargada() == TipoNovedad.DESCANSO) {
                novedad = "DESCANSO";
            } else if (prog != null && prog.getTipoNovedadPrecargada() != TipoNovedad.NINGUNA) {
                novedad = prog.getTipoNovedadPrecargada().name();
            } else {
                novedad = "FALTO / SIN MARCACIÓN";
            }

            return DetalleNominaDia.builder()
                    .cedula(cedula)
                    .empleado(nombre)
                    .cargo(cargo)
                    .fecha(fecha)
                    .diaNombre(diaNombre)
                    .entradaProg(entProg)
                    .salidaProg(salProg)
                    .minutosAlmuerzo(minAlmuerzo)
                    .entradaReal(null)
                    .salidaReal(null)
                    .horasProgramadas(horasProgramadas)
                    .horasLaboradasReales(0.0)
                    .horasOrdinarias(0.0)
                    .recargoNocturno(0.0)
                    .horasExtraDiurna(0.0)
                    .horasExtraNocturna(0.0)
                    .dominicalFestivo(0.0)
                    .horasExtraDominical(0.0)
                    .totalHorasAPagar(0.0)
                    .novedadEstado(novedad)
                    .bloqueado(bloqueado)
                    .build();
        }

        // Caso 2: Marcación con Entrada Real pero sin Salida Real registrada
        if (salReal == null) {
            // Conexión Horario Semanal y Nómina (Requerimiento 2):
            // Si el empleado registró entrada pero no salida, se toma como base cumplida el horario montado
            // en la semana para garantizar el pago de horas ordinarias o dominicales y se anota en nómina.
            boolean esDom = esDominicalOFestivo(fecha);
            double hOrd = esDom ? 0.0 : horasProgramadas;
            double hDom = esDom ? horasProgramadas : 0.0;
            double totalAPagar = horasProgramadas;

            String novedadSinSalida = (horasProgramadas > 0)
                    ? "SIN REGISTRO DE SALIDA (BASE TURNO " + (entProg != null ? entProg.format(SHORT_TIME_FMT) : "") + "-" + (salProg != null ? salProg.format(SHORT_TIME_FMT) : "") + ")"
                    : "SIN SALIDA REGISTRADA (PENDIENTE AUDITORÍA)";

            return DetalleNominaDia.builder()
                    .cedula(cedula)
                    .empleado(nombre)
                    .cargo(cargo)
                    .fecha(fecha)
                    .diaNombre(diaNombre)
                    .entradaProg(entProg)
                    .salidaProg(salProg)
                    .minutosAlmuerzo(minAlmuerzo)
                    .entradaReal(entReal)
                    .salidaReal(null)
                    .horasProgramadas(horasProgramadas)
                    .horasLaboradasReales(horasProgramadas)
                    .horasOrdinarias(hOrd)
                    .recargoNocturno(0.0)
                    .horasExtraDiurna(0.0)
                    .horasExtraNocturna(0.0)
                    .dominicalFestivo(hDom)
                    .horasExtraDominical(0.0)
                    .totalHorasAPagar(totalAPagar)
                    .novedadEstado(novedadSinSalida)
                    .bloqueado(bloqueado)
                    .build();
        }

        // Caso 3: Turno completado con entrada y salida reales
        LocalDateTime finReal = salReal;
        if (finReal.isBefore(entReal)) {
            finReal = finReal.plusDays(1);
        }

        long minBrutos = Math.max(0, Duration.between(entReal, finReal).toMinutes());

        // Descuento de almuerzo en limpio:
        // Se toma el tiempo de almuerzo cumplido (redondeado al minuto más cercano) garantizando
        // como mínimo el descuento del almuerzo programado del turno para evitar extras ficticias.
        long minAlmDesc = minAlmuerzo;
        if (reg.getHoraSalidaAlmuerzo() != null && reg.getHoraRegresoAlmuerzo() != null) {
            long segAlm = Math.max(0, Duration.between(reg.getHoraSalidaAlmuerzo(), reg.getHoraRegresoAlmuerzo()).getSeconds());
            long minAlmReal = Math.round(segAlm / 60.0);
            minAlmDesc = Math.max(minAlmuerzo, minAlmReal);
        }

        long limiteOrdinarioMin = (horasProgramadas > 0) ? Math.round(horasProgramadas * 60) : 8 * 60;
        long minutosNetosLaborados = Math.max(0, minBrutos - minAlmDesc);

        // Si el turno es un horario normal sin horas extras autorizadas, o cuando las horas se corren
        // para compensar la llegada y cerrar el turno normal (el alivio de 8 min nunca debe convertirse en extra,
        // ni márgenes <= 20 min de fila en cámara), se topa al límite ordinario del turno.
        boolean extrasAutorizadas = prog != null && Boolean.TRUE.equals(prog.getHorasExtrasAutorizadas());
        if (minutosNetosLaborados > limiteOrdinarioMin) {
            long minBrutosFisicos = (entRealRaw != null && entRealRaw.isAfter(entReal))
                    ? Math.max(0, Duration.between(entRealRaw, finReal).toMinutes())
                    : minBrutos;
            long minNetosFisicos = Math.max(0, minBrutosFisicos - minAlmDesc);
            if (!extrasAutorizadas || minNetosFisicos <= limiteOrdinarioMin + 20) {
                minutosNetosLaborados = limiteOrdinarioMin;
            } else {
                minutosNetosLaborados = minNetosFisicos;
            }
        }

        double horasLaboradasReales = redondear2(minutosNetosLaborados / 60.0);

        long minOrdinarios = 0;
        long minRecargoNocturno = 0;
        long minHED = 0;
        long minHEN = 0;
        long minDominical = 0;
        long minExtraDominical = 0;

        LocalDateTime cursor = entReal;
        LocalDateTime almLlegada = reg.getHoraSalidaAlmuerzo();
        LocalDateTime almSalida = reg.getHoraRegresoAlmuerzo();
        if (almLlegada == null && almSalida == null && minAlmDesc > 0 && minBrutos > minAlmDesc) {
            long mitadTurno = Math.max(0, (minBrutos - minAlmDesc) / 2);
            almLlegada = entReal.plusMinutes(mitadTurno);
            almSalida = almLlegada.plusMinutes(minAlmDesc);
        }
        long trabajadosMin = 0;

        while (cursor.isBefore(finReal) && trabajadosMin < minutosNetosLaborados) {
            if (almLlegada != null && almSalida != null && !cursor.isBefore(almLlegada) && cursor.isBefore(almSalida)) {
                cursor = cursor.plusMinutes(1);
                continue;
            }

            LocalDate fechaCur = cursor.toLocalDate();
            LocalTime horaCur = cursor.toLocalTime();
            boolean esDomFest = esDominicalOFestivo(fechaCur);
            // Franja nocturna en Colombia inicia a las 7:00 PM (19:00) hasta las 06:00
            boolean esNocturna = horaCur.getHour() >= 19 || horaCur.getHour() < 6;
            boolean esOrdinario = (trabajadosMin < limiteOrdinarioMin);

            if (!esDomFest) {
                if (esOrdinario) {
                    minOrdinarios++;
                    if (esNocturna) {
                        minRecargoNocturno++;
                    }
                } else {
                    if (esNocturna) {
                        minHEN++;
                    } else {
                        minHED++;
                    }
                }
            } else {
                if (esOrdinario) {
                    minDominical++;
                    if (esNocturna) {
                        minRecargoNocturno++;
                    }
                } else {
                    minExtraDominical++;
                }
            }

            trabajadosMin++;
            cursor = cursor.plusMinutes(1);
        }

        double horasOrd = redondear2(minOrdinarios / 60.0);
        double recNoct = redondear2(minRecargoNocturno / 60.0);
        double hed = redondear2(minHED / 60.0);
        double hen = redondear2(minHEN / 60.0);
        double domFest = redondear2(minDominical / 60.0);
        double extraDom = redondear2(minExtraDominical / 60.0);
        double totalAPagar = redondear2(horasOrd + domFest + hed + hen + extraDom);

        String estado;
        if (estadoLlegadaEfectivo == EstadoLlegada.RETARDO && minutosRetardoEfectivo > 0) {
            estado = "RETARDO (" + minutosRetardoEfectivo + " min)";
        } else {
            estado = "A TIEMPO";
        }
        if (hed > 0 || hen > 0 || extraDom > 0) {
            double totalExtras = redondear2(hed + hen + extraDom);
            estado += " | EXTRAS REGISTRADAS (" + totalExtras + "h)";
        }

        return DetalleNominaDia.builder()
                .cedula(cedula)
                .empleado(nombre)
                .cargo(cargo)
                .fecha(fecha)
                .diaNombre(diaNombre)
                .entradaProg(entProg)
                .salidaProg(salProg)
                .minutosAlmuerzo((int) minAlmDesc)
                .entradaReal(entReal)
                .salidaReal(salReal)
                .horasProgramadas(horasProgramadas)
                .horasLaboradasReales(horasLaboradasReales)
                .horasOrdinarias(horasOrd)
                .recargoNocturno(recNoct)
                .horasExtraDiurna(hed)
                .horasExtraNocturna(hen)
                .dominicalFestivo(domFest)
                .horasExtraDominical(extraDom)
                .totalHorasAPagar(totalAPagar)
                .novedadEstado(estado)
                .bloqueado(bloqueado)
                .build();
    }

    private static double redondear2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
