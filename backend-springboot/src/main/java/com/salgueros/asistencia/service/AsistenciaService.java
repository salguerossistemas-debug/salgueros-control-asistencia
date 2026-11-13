package com.salgueros.asistencia.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgueros.asistencia.dto.ContingenciaRequestDTO;
import com.salgueros.asistencia.dto.CrearEmpleadoRequestDTO;
import com.salgueros.asistencia.dto.EnrolamientoRequestDTO;
import com.salgueros.asistencia.dto.MarcacionRequestDTO;
import com.salgueros.asistencia.dto.MarcacionResponseDTO;
import com.salgueros.asistencia.dto.ProgramacionRequestDTO;
import com.salgueros.asistencia.model.*;
import com.salgueros.asistencia.repository.EmpleadoRepository;
import com.salgueros.asistencia.repository.ProgramacionSemanalRepository;
import com.salgueros.asistencia.repository.RegistroAsistenciaRepository;
import com.salgueros.asistencia.repository.VectorFacialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AsistenciaService {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaService.class);

    private final EmpleadoRepository empleadoRepository;
    private final VectorFacialRepository vectorRepository;
    private final ProgramacionSemanalRepository programacionRepository;
    private final RegistroAsistenciaRepository registroRepository;
    private final ObjectMapper objectMapper;

    @Value("${salgueros.pin.contingencia:2586}")
    private  String pinContingenciaMaestro;

    @Value("${salgueros.asistencia.tolerancia-minutos:5}")
    private int toleranciaMinutos;

    @Value("${salgueros.asistencia.alivio-entrada-minutos:8}")
    private int alivioEntradaMinutos = 8;

    @Value("${salgueros.asistencia.umbral-distancia-vector:0.45}")
    private double umbralDistancia;

    @Value("${salgueros.admin.pin:2026}")
    private String adminPin;

    public AsistenciaService(EmpleadoRepository empleadoRepository,
                             VectorFacialRepository vectorRepository,
                             ProgramacionSemanalRepository programacionRepository,
                             RegistroAsistenciaRepository registroRepository,
                             ObjectMapper objectMapper) {
        this.empleadoRepository = empleadoRepository;
        this.vectorRepository = vectorRepository;
        this.programacionRepository = programacionRepository;
        this.registroRepository = registroRepository;
        this.objectMapper = objectMapper;
    }

    public int getAlivioEntradaMinutos() {
        return alivioEntradaMinutos;
    }

    public void setAlivioEntradaMinutos(int alivioEntradaMinutos) {
        this.alivioEntradaMinutos = alivioEntradaMinutos;
    }

    public List<Empleado> listarEmpleadosActivos() {
        return empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
    }

    public void validarPinAdmin(String pin) {
        if (pin == null || !adminPin.equals(pin.trim())) {
            throw new SecurityException("Acceso Denegado: PIN de Administrador incorrecto.");
        }
    }

    @Transactional
    public Map<String, Object> crearNuevoEmpleado(CrearEmpleadoRequestDTO request) {
        validarPinAdmin(request.getAdminPin());

        if (request.getCedula() == null || request.getCedula().isBlank()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (request.getNombreApellido() == null || request.getNombreApellido().isBlank()) {
            throw new IllegalArgumentException("El nombre y apellido son requeridos");
        }

        String cedula = request.getCedula().trim();
        if (empleadoRepository.findByCedula(cedula).isPresent()) {
            throw new IllegalArgumentException("Ya existe un empleado con la cédula " + cedula);
        }

        String area = request.getArea() != null && !request.getArea().isBlank()
                ? request.getArea().trim().toUpperCase()
                : "PLANTA";

        Empleado nuevo = Empleado.builder()
                .cedula(cedula)
                .nombreApellido(request.getNombreApellido().trim())
                .area(area)
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        empleadoRepository.save(nuevo);

        // Crear turno automático para hoy para que pueda probar de inmediato
        LocalDate hoy = LocalDate.now();
        if (programacionRepository.findByEmpleadoAndFecha(nuevo, hoy).isEmpty()) {
            ProgramacionSemanal prog = ProgramacionSemanal.builder()
                    .empleado(nuevo)
                    .fecha(hoy)
                    .horaEntradaProgramada(LocalTime.of(8, 0))
                    .horaSalidaProgramada(LocalTime.of(17, 0))
                    .minutosAlmuerzoProgramados(60)
                    .horasExtrasAutorizadas(true)
                    .tipoNovedadPrecargada(TipoNovedad.NINGUNA)
                    .bloqueado(false)
                    .fechaModificacion(LocalDateTime.now())
                    .build();
            programacionRepository.save(prog);
        }

        return Map.of(
                "exito", true,
                "mensaje", "Empleado creado exitosamente en área " + nuevo.getArea() + " con turno activo para hoy",
                "idEmpleado", nuevo.getIdEmpleado(),
                "cedula", nuevo.getCedula(),
                "nombreApellido", nuevo.getNombreApellido(),
                "area", nuevo.getArea()
        );
    }

    @Transactional
    public Map<String, Object> programarTurnos(ProgramacionRequestDTO request) {
        validarPinAdmin(request.getAdminPin());

        LocalDate fechaInicio = request.getFechaInicio() != null ? request.getFechaInicio() : LocalDate.now();
        LocalDate fechaFin = request.getFechaFin() != null ? request.getFechaFin() : fechaInicio;

        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }

        LocalTime horaEntrada = request.getHoraEntrada() != null ? request.getHoraEntrada() : LocalTime.of(8, 0);
        LocalTime horaSalida = request.getHoraSalida() != null ? request.getHoraSalida() : horaEntrada.plusHours(8);
        int minutosAlmuerzo = request.getMinutosAlmuerzo() != null ? request.getMinutosAlmuerzo() : 60;
        boolean horasExtras = Boolean.TRUE.equals(request.getHorasExtras());
        TipoNovedad novedad = request.getTipoNovedad() != null ? request.getTipoNovedad() : TipoNovedad.NINGUNA;
        boolean bloqueado = request.getBloqueado() != null ? Boolean.TRUE.equals(request.getBloqueado()) : true;

        List<Empleado> empleadosObjetivo;
        String criterioAsignacion;

        if (request.getIdEmpleado() != null) {
            Empleado emp = empleadoRepository.findById(request.getIdEmpleado())
                    .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con ID: " + request.getIdEmpleado()));
            empleadosObjetivo = List.of(emp);
            criterioAsignacion = "Empleado: " + emp.getNombreApellido();
        } else if (request.getArea() != null && !request.getArea().isBlank() && !"TODOS".equalsIgnoreCase(request.getArea())) {
            // Asignación Masiva por Área (PLANTA, VENTAS, TRANSPORTADORES, ADMINISTRADORES)
            String areaFiltro = request.getArea().trim().toUpperCase();
            empleadosObjetivo = empleadoRepository.findByEstadoAndArea(EstadoEmpleado.ACTIVO, areaFiltro);
            criterioAsignacion = "Área: " + areaFiltro;
        } else {
            // Aplicar a TODOS los empleados activos
            empleadosObjetivo = empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
            criterioAsignacion = "TODOS los empleados activos";
        }

        if (empleadosObjetivo.isEmpty()) {
            throw new IllegalArgumentException("No hay empleados activos en el criterio (" + criterioAsignacion + ") para programar turnos");
        }

        int turnosCreados = 0;
        int turnosActualizados = 0;
        int turnosProtegidos = 0;

        for (Empleado emp : empleadosObjetivo) {
            LocalDate fechaActual = fechaInicio;
            while (!fechaActual.isAfter(fechaFin)) {
                Optional<ProgramacionSemanal> optProg = programacionRepository.findByEmpleadoAndFecha(emp, fechaActual);

                if (optProg.isPresent()) {
                    ProgramacionSemanal prog = optProg.get();
                    // REGLA DE VERACIDAD E INMUTABILIDAD:
                    // Si el empleado YA registró marcación biométrica en este turno, el horario de ese día
                    // queda congelado para que los reportes semanales, quincenales y mensuales sean 100% verídicos.
                    // Los demás días pendientes sí se actualizan normalmente.
                    boolean yaTieneAsistencia = registroRepository.findByProgramacion(prog).isPresent();
                    if (yaTieneAsistencia) {
                        turnosProtegidos++;
                    } else {
                        prog.setHoraEntradaProgramada(horaEntrada);
                        prog.setHoraSalidaProgramada(horaSalida);
                        prog.setMinutosAlmuerzoProgramados(minutosAlmuerzo);
                        prog.setHorasExtrasAutorizadas(horasExtras);
                        prog.setTipoNovedadPrecargada(novedad);
                        prog.setBloqueado(bloqueado);
                        prog.setFechaModificacion(LocalDateTime.now());
                        programacionRepository.save(prog);
                        turnosActualizados++;
                    }
                } else {
                    ProgramacionSemanal prog = ProgramacionSemanal.builder()
                            .empleado(emp)
                            .fecha(fechaActual)
                            .horaEntradaProgramada(horaEntrada)
                            .horaSalidaProgramada(horaSalida)
                            .minutosAlmuerzoProgramados(minutosAlmuerzo)
                            .horasExtrasAutorizadas(horasExtras)
                            .tipoNovedadPrecargada(novedad)
                            .bloqueado(bloqueado)
                            .fechaModificacion(LocalDateTime.now())
                            .build();
                    programacionRepository.save(prog);
                    turnosCreados++;
                }

                fechaActual = fechaActual.plusDays(1);
            }
        }

        int totalProcesados = turnosCreados + turnosActualizados;
        StringBuilder msg = new StringBuilder();
        if (totalProcesados > 0) {
            msg.append("✅ Horario aplicado y ordenado correctamente (")
               .append(turnosCreados).append(" nuevos, ")
               .append(turnosActualizados).append(" modificados)");
            if (turnosProtegidos > 0) {
                msg.append(". 🔒 ").append(turnosProtegidos)
                   .append(" turno(s) ya tenían asistencia biométrica registrada y se mantuvieron intactos para garantizar la veracidad del reporte.");
            }
        } else if (turnosProtegidos > 0) {
            msg.append("🔒 Los ").append(turnosProtegidos)
               .append(" turno(s) seleccionados ya tienen marcación biométrica registrada y no pueden alterarse para preservar la veracidad del historial.");
        } else {
            msg.append("Turnos procesados sin cambios.");
        }

        Map<String, Object> respuesta = new java.util.HashMap<>();
        respuesta.put("exito", totalProcesados > 0 || turnosProtegidos > 0);
        respuesta.put("mensaje", msg.toString());
        respuesta.put("totalTurnos", totalProcesados);
        respuesta.put("turnosCreados", turnosCreados);
        respuesta.put("turnosActualizados", turnosActualizados);
        respuesta.put("turnosProtegidos", turnosProtegidos);
        respuesta.put("totalEmpleados", empleadosObjetivo.size());
        respuesta.put("desde", fechaInicio.toString());
        respuesta.put("hasta", fechaFin.toString());
        respuesta.put("horaEntrada", horaEntrada.toString());
        respuesta.put("horaSalida", horaSalida.toString());
        return respuesta;
    }

    public List<Map<String, Object>> listarProgramacionRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null) inicio = LocalDate.now();
        if (fin == null) fin = inicio;

        DateTimeFormatter timeShortFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<ProgramacionSemanal> lista = programacionRepository.findProgramacionOrdenada(inicio, fin);
        List<RegistroAsistencia> registrosEnRango = registroRepository.findRegistrosEntreFechas(inicio, fin);
        java.util.Set<Long> idsConAsistencia = registrosEnRango.stream()
                .map(r -> r.getProgramacion().getIdProgramacion())
                .collect(java.util.stream.Collectors.toSet());

        return lista.stream().map(p -> {
            Map<String, Object> map = new java.util.HashMap<>();
            LocalTime hEnt = p.getHoraEntradaProgramada();
            LocalTime hSal = p.getHoraSalidaProgramada() != null ? p.getHoraSalidaProgramada() : hEnt.plusHours(8);
            boolean tieneAsistencia = idsConAsistencia.contains(p.getIdProgramacion());

            map.put("idProgramacion", p.getIdProgramacion());
            map.put("idEmpleado", p.getEmpleado().getIdEmpleado());
            map.put("nombreEmpleado", p.getEmpleado().getNombreApellido());
            map.put("cedula", p.getEmpleado().getCedula());
            map.put("area", p.getEmpleado().getArea() != null ? p.getEmpleado().getArea() : "PLANTA");
            map.put("fecha", p.getFecha().toString());
            map.put("horaEntrada", hEnt.format(timeShortFmt));
            map.put("horaSalida", hSal.format(timeShortFmt));
            map.put("rangoHorario", hEnt.format(timeShortFmt) + " a " + hSal.format(timeShortFmt));
            map.put("minutosAlmuerzo", p.getMinutosAlmuerzoProgramados());
            map.put("horasExtras", p.getHorasExtrasAutorizadas());
            map.put("novedad", p.getTipoNovedadPrecargada().name());
            map.put("tieneAsistencia", tieneAsistencia);
            map.put("bloqueado", Boolean.TRUE.equals(p.getBloqueado()));
            map.put("estadoTurno", tieneAsistencia ? "EJECUTADO_INMUTABLE" : (Boolean.TRUE.equals(p.getBloqueado()) ? "CONFIRMADO_BLOQUEADO" : "PROGRAMADO_OFICIAL"));
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> alternarBloqueoTurnos(LocalDate inicio, LocalDate fin, boolean bloquear) {
        if (inicio == null) inicio = LocalDate.now();
        if (fin == null) fin = inicio;

        int total = programacionRepository.actualizarBloqueoEnRango(inicio, fin, bloquear);

        return Map.of(
                "exito", true,
                "bloqueado", bloquear,
                "totalAfectados", total,
                "desde", inicio.toString(),
                "hasta", fin.toString(),
                "mensaje", bloquear
                        ? "Horarios Semanales Confirmados (Modo Lectura)"
                        : "Edición de horarios desbloqueada exitosamente"
        );
    }

    @Transactional
    public Map<String, Object> guardarMatrizSemanal(Map<String, Object> body) {
        String adminPin = (String) body.get("adminPin");
        if (adminPin != null && !adminPin.isBlank()) {
            validarPinAdmin(adminPin);
        }

        boolean bloquear = body.get("bloqueado") != null ? Boolean.parseBoolean(String.valueOf(body.get("bloqueado"))) : true;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listaTurnos = (List<Map<String, Object>>) body.get("turnos");
        if (listaTurnos == null || listaTurnos.isEmpty()) {
            return Map.of("exito", false, "mensaje", "No se recibieron turnos para procesar");
        }

        int creados = 0;
        int actualizados = 0;
        int descansos = 0;
        int protegidos = 0;

        for (Map<String, Object> t : listaTurnos) {
            if (t.get("idEmpleado") == null || t.get("fecha") == null) continue;

            Integer idEmpleado = Integer.valueOf(t.get("idEmpleado").toString());
            LocalDate fecha = LocalDate.parse(t.get("fecha").toString());
            String tipo = (String) t.getOrDefault("tipo", "TURNO");
            String novedadStr = (String) t.getOrDefault("tipoNovedad", "NINGUNA");
            TipoNovedad novedad;
            try {
                novedad = TipoNovedad.valueOf(novedadStr);
            } catch (Exception ex) {
                novedad = TipoNovedad.NINGUNA;
            }

            Empleado emp = empleadoRepository.findById(idEmpleado).orElse(null);
            if (emp == null) continue;

            Optional<ProgramacionSemanal> optProg = programacionRepository.findByEmpleadoAndFecha(emp, fecha);

            if ("DESCANSO".equalsIgnoreCase(tipo) || novedad == TipoNovedad.DESCANSO) {
                if (optProg.isPresent()) {
                    ProgramacionSemanal prog = optProg.get();
                    boolean yaTieneAsistencia = registroRepository.findByProgramacion(prog).isPresent();
                    if (yaTieneAsistencia) {
                        protegidos++;
                    } else {
                        programacionRepository.delete(prog);
                        descansos++;
                    }
                }
                continue;
            }

            // Es turno laboral o novedad programada
            String entStr = (String) t.getOrDefault("horaEntrada", "08:00");
            String salStr = (String) t.getOrDefault("horaSalida", "17:00");
            if (entStr.length() > 5) entStr = entStr.substring(0, 5);
            if (salStr.length() > 5) salStr = salStr.substring(0, 5);

            LocalTime horaEnt = LocalTime.parse(entStr);
            LocalTime horaSal = LocalTime.parse(salStr);
            int minutosAlm = 0;
            if (t.get("minutosAlmuerzo") != null) {
                try { minutosAlm = Integer.parseInt(t.get("minutosAlmuerzo").toString()); } catch (Exception ignored) {}
            } else if (t.get("minutosAlmuerzoProgramados") != null) {
                try { minutosAlm = Integer.parseInt(t.get("minutosAlmuerzoProgramados").toString()); } catch (Exception ignored) {}
            }
            boolean horasExtras = Boolean.parseBoolean(String.valueOf(t.getOrDefault("horasExtras", false)));

            if (optProg.isPresent()) {
                ProgramacionSemanal prog = optProg.get();
                boolean yaTieneAsistencia = registroRepository.findByProgramacion(prog).isPresent();
                if (yaTieneAsistencia) {
                    protegidos++;
                } else {
                    prog.setHoraEntradaProgramada(horaEnt);
                    prog.setHoraSalidaProgramada(horaSal);
                    prog.setMinutosAlmuerzoProgramados(minutosAlm);
                    prog.setHorasExtrasAutorizadas(horasExtras);
                    prog.setTipoNovedadPrecargada(novedad);
                    prog.setBloqueado(bloquear);
                    prog.setFechaModificacion(LocalDateTime.now());
                    programacionRepository.save(prog);
                    actualizados++;
                }
            } else {
                ProgramacionSemanal prog = ProgramacionSemanal.builder()
                        .empleado(emp)
                        .fecha(fecha)
                        .horaEntradaProgramada(horaEnt)
                        .horaSalidaProgramada(horaSal)
                        .minutosAlmuerzoProgramados(minutosAlm)
                        .horasExtrasAutorizadas(horasExtras)
                        .tipoNovedadPrecargada(novedad)
                        .bloqueado(bloquear)
                        .fechaModificacion(LocalDateTime.now())
                        .build();
                programacionRepository.save(prog);
                creados++;
            }
        }

        return Map.of(
                "exito", true,
                "mensaje", "Matriz semanal guardada y confirmada (" + creados + " nuevos, " + actualizados + " actualizados, " + descansos + " descansos)",
                "creados", creados,
                "actualizados", actualizados,
                "descansos", descansos,
                "protegidos", protegidos,
                "bloqueado", bloquear
        );
    }

    public List<Map<String, Object>> listarRegistrosAsistencia(LocalDate inicio, LocalDate fin) {
        if (inicio == null) inicio = LocalDate.now().minusDays(7);
        if (fin == null) fin = LocalDate.now();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter shortTimeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<RegistroAsistencia> registros = registroRepository.findRegistrosEntreFechas(inicio, fin);

        return registros.stream().map(r -> {
            Map<String, Object> map = new java.util.HashMap<>();
            LocalTime hEntProg = r.getProgramacion().getHoraEntradaProgramada();
            LocalTime hSalProg = r.getProgramacion().getHoraSalidaProgramada() != null
                    ? r.getProgramacion().getHoraSalidaProgramada()
                    : hEntProg.plusHours(8);

            // Cálculo de nómina, alivio de 8 min, descuento de almuerzo y horas netas sincronizado con CalculadoraNominaColombia
            CalculadoraNominaColombia.DetalleNominaDia detalle = CalculadoraNominaColombia.calcularDia(r.getProgramacion(), r);

            LocalDateTime entEfectiva = detalle.getEntradaReal() != null ? detalle.getEntradaReal() : r.getHoraEntradaReal();
            int retardoEfectivo = r.getMinutosRetardo() != null ? r.getMinutosRetardo() : 0;
            EstadoLlegada estadoLlegadaEfectivo = r.getEstadoLlegada() != null ? r.getEstadoLlegada() : EstadoLlegada.A_TIEMPO;
            if (entEfectiva != null && hEntProg != null) {
                long diffRet = Duration.between(hEntProg, entEfectiva.toLocalTime()).toMinutes();
                if (diffRet <= 0) {
                    retardoEfectivo = 0;
                    estadoLlegadaEfectivo = EstadoLlegada.A_TIEMPO;
                } else {
                    retardoEfectivo = (int) diffRet;
                    estadoLlegadaEfectivo = EstadoLlegada.RETARDO;
                }
            }

            map.put("idRegistro", r.getIdRegistro());
            map.put("cedula", r.getProgramacion().getEmpleado().getCedula());
            map.put("nombreEmpleado", r.getProgramacion().getEmpleado().getNombreApellido());
            map.put("area", r.getProgramacion().getEmpleado().getArea() != null ? r.getProgramacion().getEmpleado().getArea() : "PLANTA");
            map.put("fecha", r.getProgramacion().getFecha().format(dateFmt));
            map.put("horaEntradaProgramada", hEntProg.format(timeFmt));
            map.put("horaSalidaProgramada", hSalProg.format(timeFmt));
            map.put("turnoProgramado", hEntProg.format(shortTimeFmt) + " a " + hSalProg.format(shortTimeFmt));
            map.put("minutosProgramados", (long) Math.round(detalle.getHorasProgramadas() * 60.0));
            map.put("horaEntradaReal", entEfectiva != null ? entEfectiva.format(timeFmt) : "--");
            map.put("horaSalidaAlmuerzo", r.getHoraSalidaAlmuerzo() != null ? r.getHoraSalidaAlmuerzo().format(timeFmt) : "--");
            map.put("horaRegresoAlmuerzo", r.getHoraRegresoAlmuerzo() != null ? r.getHoraRegresoAlmuerzo().format(timeFmt) : "--");
            map.put("horaSalidaReal", r.getHoraSalidaReal() != null ? r.getHoraSalidaReal().format(timeFmt) : "--");
            map.put("minutosRetardo", retardoEfectivo);
            map.put("estadoLlegada", estadoLlegadaEfectivo.name());
            map.put("estadoDia", r.getEstadoDia().name());

            long minAlmuerzo = detalle.getMinutosAlmuerzo();
            long minutosEfectivos = (long) Math.round(detalle.getHorasLaboradasReales() * 60.0);
            long minutosBrutos = minutosEfectivos > 0 ? (minutosEfectivos + minAlmuerzo) : 0;
            long minutosExtras = (long) Math.round((detalle.getHorasExtraDiurna() + detalle.getHorasExtraNocturna() + detalle.getHorasExtraDominical()) * 60.0);

            String horasTrabajadas;
            if (r.getHoraEntradaReal() != null && r.getHoraSalidaReal() != null) {
                long h = minutosEfectivos / 60;
                long m = minutosEfectivos % 60;
                horasTrabajadas = h + "h " + m + "m (" + detalle.getHorasLaboradasReales() + " hrs)";
            } else if (r.getHoraEntradaReal() != null) {
                horasTrabajadas = "En curso (Sin salida)";
            } else {
                horasTrabajadas = "--";
            }

            map.put("minutosBrutos", minutosBrutos);
            map.put("horasBrutas", (minutosBrutos / 60) + "h " + (minutosBrutos % 60) + "m");
            map.put("minutosAlmuerzo", minAlmuerzo);
            map.put("minutosEfectivos", minutosEfectivos);
            map.put("minutosExtras", minutosExtras);
            map.put("horasTrabajadas", horasTrabajadas);
            map.put("horasExtrasCalculadas", (minutosExtras / 60) + "h " + (minutosExtras % 60) + "m (" + (Math.round((minutosExtras / 60.0) * 100.0) / 100.0) + " hrs)");

            // Variables de liquidación oficial de nómina
            map.put("horasOrdinarias", detalle.getHorasOrdinarias());
            map.put("recargoNocturno", detalle.getRecargoNocturno());
            map.put("horasExtraDiurna", detalle.getHorasExtraDiurna());
            map.put("horasExtraNocturna", detalle.getHorasExtraNocturna());
            map.put("dominicalFestivo", detalle.getDominicalFestivo());
            map.put("horasExtraDominical", detalle.getHorasExtraDominical());
            map.put("totalHorasAPagar", detalle.getTotalHorasAPagar());
            map.put("novedadEstado", detalle.getNovedadEstado());

            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> enrolarRostro(EnrolamientoRequestDTO request) {
        if (request.getCedula() == null || request.getCedula().isBlank()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (request.getVectorEmbedding() == null || request.getVectorEmbedding().size() < 128) {
            throw new IllegalArgumentException("Se requiere un vector de 128 dimensiones extraído por la cámara");
        }

        Empleado empleado = empleadoRepository.findByCedula(request.getCedula().trim())
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con cédula: " + request.getCedula()));

        try {
            String jsonVector = objectMapper.writeValueAsString(request.getVectorEmbedding());
            List<VectorFacial> existentes = vectorRepository.findByEmpleado(empleado);
            VectorFacial vectorFacial;

            if (!existentes.isEmpty()) {
                vectorFacial = existentes.get(0);
                vectorFacial.setVectorEmbedding(jsonVector);
            } else {
                vectorFacial = VectorFacial.builder()
                        .empleado(empleado)
                        .vectorEmbedding(jsonVector)
                        .build();
            }

            vectorRepository.save(vectorFacial);

            return Map.of(
                    "exito", true,
                    "mensaje", "Rostro biométrico enrolado con éxito",
                    "empleado", empleado.getNombreApellido(),
                    "cedula", empleado.getCedula(),
                    "dimensiones", request.getVectorEmbedding().size()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar vector biométrico: " + e.getMessage());
        }
    }

    @Transactional
    public MarcacionResponseDTO procesarMarcacion(MarcacionRequestDTO request) {
        List<Double> vectorEntrada = request.getVectorEmbedding();
        if (vectorEntrada == null || vectorEntrada.size() < 128) {
            return MarcacionResponseDTO.builder()
                    .exito(false)
                    .mensaje("Vector facial inválido o incompleto (se requieren 128 dimensiones)")
                    .build();
        }

        // 1. Búsqueda y comparación vectorial (Distancia Euclidiana)
        MatchBiometrico match = buscarMejorCoincidencia(vectorEntrada);
        if (match == null || match.distancia > umbralDistancia) {
            return MarcacionResponseDTO.builder()
                    .exito(false)
                    .mensaje("Rostro no reconocido en la base de empleados activos")
                    .distanciaBiometrica(match != null ? match.distancia : 999.0)
                    .build();
        }

        Empleado empleado = match.empleado;
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        // 2. Consulta de la Programación Semanal (Fuente de Verdad)
        Optional<ProgramacionSemanal> optProg = programacionRepository.findByEmpleadoAndFecha(empleado, hoy);
        if (optProg.isEmpty()) {
            return MarcacionResponseDTO.builder()
                    .exito(false)
                    .nombreEmpleado(empleado.getNombreApellido())
                    .cedula(empleado.getCedula())
                    .mensaje("Empleado no tiene turno programado para la fecha de hoy")
                    .build();
        }

        ProgramacionSemanal prog = optProg.get();
        if (prog.getTipoNovedadPrecargada() != TipoNovedad.NINGUNA) {
            return MarcacionResponseDTO.builder()
                    .exito(false)
                    .nombreEmpleado(empleado.getNombreApellido())
                    .cedula(empleado.getCedula())
                    .mensaje("Turno no disponible por novedad: " + prog.getTipoNovedadPrecargada())
                    .build();
        }

        // 3. Obtener o crear Registro de Asistencia
        Optional<RegistroAsistencia> optReg = registroRepository.findByProgramacion(prog);
        RegistroAsistencia registro = optReg.orElse(null);

        String tipo = request.getTipoMarcacion() != null ? request.getTipoMarcacion().toUpperCase() : "ENTRADA";
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        // REGLA FUNDAMENTAL DE SEGURIDAD:
        // Si la jornada ya fue completada y cerrada hoy (salida laboral registrada),
        // queda bloqueada permanentemente para evitar cualquier manipulación de horas.
        if (registro != null && registro.getHoraSalidaReal() != null) {
            return MarcacionResponseDTO.builder()
                    .exito(false)
                    .nombreEmpleado(empleado.getNombreApellido())
                    .cedula(empleado.getCedula())
                    .tipoMarcacion(tipo)
                    .horaMarcacion(registro.getHoraSalidaReal().format(timeFmt))
                    .distanciaBiometrica(Math.round(match.distancia * 1000.0) / 1000.0)
                    .mensaje("⛔ Tu jornada de hoy ya fue finalizada y cerrada a las " + 
                            registro.getHoraSalidaReal().format(timeFmt) + 
                            ". Tu horario está bloqueado y no admite más marcaciones.")
                    .build();
        }

        // VALIDACIONES ESPECÍFICAS DE BLOQUEO POR TIPO DE MARCACIÓN
        switch (tipo) {
            case "ENTRADA":
                // Si la hora de entrada ya existe, se BLOQUEA de forma inmutable. No se permite sobreescribir.
                if (registro != null && registro.getHoraEntradaReal() != null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .horaMarcacion(registro.getHoraEntradaReal().format(timeFmt))
                            .distanciaBiometrica(Math.round(match.distancia * 1000.0) / 1000.0)
                            .mensaje("⛔ Ya registraste tu ENTRADA hoy a las " + 
                                    registro.getHoraEntradaReal().format(timeFmt) + 
                                    ". Tu hora de entrada está bloqueada y no puede ser modificada.")
                            .build();
                }
                break;

            case "SALIDA_ALMUERZO":
                if (registro == null || registro.getHoraEntradaReal() == null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .mensaje("⚠️ Debes registrar primero tu ENTRADA laboral antes de salir a almorzar.")
                            .build();
                }
                if (prog.getMinutosAlmuerzoProgramados() == 0) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .mensaje("⚠️ Tu turno es de jornada continua (0 min de almuerzo autorizados).")
                            .build();
                }
                // Si ya marcó salida a almuerzo, se BLOQUEA.
                if (registro.getHoraSalidaAlmuerzo() != null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .horaMarcacion(registro.getHoraSalidaAlmuerzo().format(timeFmt))
                            .distanciaBiometrica(Math.round(match.distancia * 1000.0) / 1000.0)
                            .mensaje("⛔ Ya registraste tu SALIDA A ALMUERZO hoy a las " + 
                                    registro.getHoraSalidaAlmuerzo().format(timeFmt) + 
                                    ". Esta hora ya está bloqueada.")
                            .build();
                }
                break;

            case "REGRESO_ALMUERZO":
                if (registro == null || registro.getHoraEntradaReal() == null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .mensaje("⚠️ Debes registrar primero tu ENTRADA laboral.")
                            .build();
                }
                if (registro.getHoraSalidaAlmuerzo() == null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .mensaje("⚠️ Debes registrar primero la SALIDA A ALMUERZO antes de registrar tu retorno.")
                            .build();
                }
                // Si ya marcó regreso de almuerzo, se BLOQUEA.
                if (registro.getHoraRegresoAlmuerzo() != null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .horaMarcacion(registro.getHoraRegresoAlmuerzo().format(timeFmt))
                            .distanciaBiometrica(Math.round(match.distancia * 1000.0) / 1000.0)
                            .mensaje("⛔ Ya registraste tu RETORNO DE ALMUERZO hoy a las " + 
                                    registro.getHoraRegresoAlmuerzo().format(timeFmt) + 
                                    ". Esta hora ya está bloqueada.")
                            .build();
                }
                break;

            case "SALIDA":
                if (registro == null || registro.getHoraEntradaReal() == null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .mensaje("⚠️ Debes registrar primero tu ENTRADA laboral antes de marcar la salida.")
                            .build();
                }
                if (registro.getHoraSalidaAlmuerzo() != null && registro.getHoraRegresoAlmuerzo() == null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .mensaje("⚠️ Tienes una pausa de almuerzo pendiente. Registra tu RETORNO DE ALMUERZO antes de marcar la salida laboral.")
                            .build();
                }
                // Si ya marcó salida laboral, se BLOQUEA.
                if (registro.getHoraSalidaReal() != null) {
                    return MarcacionResponseDTO.builder()
                            .exito(false)
                            .nombreEmpleado(empleado.getNombreApellido())
                            .cedula(empleado.getCedula())
                            .tipoMarcacion(tipo)
                            .horaMarcacion(registro.getHoraSalidaReal().format(timeFmt))
                            .distanciaBiometrica(Math.round(match.distancia * 1000.0) / 1000.0)
                            .mensaje("⛔ Ya registraste tu SALIDA LABORAL hoy a las " + 
                                    registro.getHoraSalidaReal().format(timeFmt) + 
                                    ". Tu jornada ya fue cerrada y bloqueada.")
                            .build();
                }
                break;

            default:
                return MarcacionResponseDTO.builder()
                        .exito(false)
                        .mensaje("Tipo de marcación no reconocido: " + tipo)
                        .build();
        }

        // 4. Ejecución del Motor de Registro (
        return ejecutarMotorReglas(tipo, prog, registro, empleado, ahora, match.distancia);
    }

    MarcacionResponseDTO ejecutarMotorReglas(String tipo, ProgramacionSemanal prog, 
                                            RegistroAsistencia registro, Empleado empleado, 
                                            LocalDateTime ahora, double distancia) {
        if (registro == null) {
            registro = RegistroAsistencia.builder()
                    .programacion(prog)
                    .horaEntradaReal(ahora)
                    .estadoLlegada(EstadoLlegada.A_TIEMPO)
                    .estadoDia(EstadoDia.EN_CURSO)
                    .build();
        }

        String mensajeResultado;
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        switch (tipo) {
            case "ENTRADA":
                LocalTime programada = prog.getHoraEntradaProgramada();
                if (programada == null) {
                    programada = LocalTime.of(8, 0);
                }
                LocalTime real = ahora.toLocalTime();
                long minutosDiferencia = Duration.between(programada, real).toMinutes();

                // Lógica de Alivio a la Entrada (Buffer para Render spin-up / retraso de arranque):
                // 1. Llegada antes o exacta (minutosDiferencia <= 0): Puntual, 0 retardo.
                // 2. Llegada dentro del rango de alivio (ej: 1 a 8 min): Se acerca a la hora programada (ej: llega 6:08 -> registra 6:00, 0 retardo).
                // 3. Llegada posterior al alivio (ej: 6:09): Se descuentan los 8 min de alivio (ej: 9 - 8 = 1 min de retardo).
                if (minutosDiferencia <= 0) {
                    LocalDateTime horaAcercada = LocalDateTime.of(ahora.toLocalDate(), programada);
                    registro.setHoraEntradaReal(horaAcercada);
                    registro.setEstadoLlegada(EstadoLlegada.A_TIEMPO);
                    registro.setMinutosRetardo(0);
                    mensajeResultado = "✅ ENTRADA REGISTRADA A TIEMPO";
                } else if (minutosDiferencia <= alivioEntradaMinutos) {
                    LocalDateTime horaAcercada = LocalDateTime.of(ahora.toLocalDate(), programada);
                    registro.setHoraEntradaReal(horaAcercada);
                    registro.setEstadoLlegada(EstadoLlegada.A_TIEMPO);
                    registro.setMinutosRetardo(0);
                    mensajeResultado = "✅ ENTRADA REGISTRADA A TIEMPO (Alivio aplicado: " + programada.format(timeFmt) + ")";
                } else {
                    int retardoEfectivo = (int) (minutosDiferencia - alivioEntradaMinutos);
                    LocalDateTime horaEntradaAjustada = LocalDateTime.of(ahora.toLocalDate(), programada).plusMinutes(retardoEfectivo);
                    registro.setHoraEntradaReal(horaEntradaAjustada);
                    registro.setEstadoLlegada(EstadoLlegada.RETARDO);
                    registro.setMinutosRetardo(retardoEfectivo);
                    mensajeResultado = "⚠️ ENTRADA REGISTRADA CON RETARDO (+" + retardoEfectivo + " min con alivio de " + alivioEntradaMinutos + " min aplicado)";
                }
                break;

            case "SALIDA_ALMUERZO":
                registro.setHoraSalidaAlmuerzo(ahora);
                mensajeResultado = "🍽️ SALIDA A ALMUERZO REGISTRADA. ¡Buen provecho!";
                break;

            case "REGRESO_ALMUERZO":
                registro.setHoraRegresoAlmuerzo(ahora);
                mensajeResultado = "☕ RETORNO DE ALMUERZO REGISTRADO. ¡Bienvenido de vuelta!";
                break;

            case "SALIDA":
                registro.setHoraSalidaReal(ahora);
                registro.setEstadoDia(EstadoDia.COMPLETADO);
                mensajeResultado = "🏁 SALIDA LABORAL REGISTRADA. ¡ADIOS, nos vemos manaña ... !";
                break;

            default:
                mensajeResultado = "Marcación procesada: " + tipo;
        }

        registroRepository.save(registro);

        return MarcacionResponseDTO.builder()
                .exito(true)
                .nombreEmpleado(empleado.getNombreApellido())
                .cedula(empleado.getCedula())
                .tipoMarcacion(tipo)
                .estadoLlegada(registro.getEstadoLlegada().name())
                .minutosRetardo(registro.getMinutosRetardo())
                .horaMarcacion(ahora.format(timeFmt))
                .distanciaBiometrica(Math.round(distancia * 1000.0) / 1000.0)
                .mensaje(mensajeResultado)
                .build();
    }

    private MatchBiometrico buscarMejorCoincidencia(List<Double> vectorTarget) {
        List<VectorFacial> activos = vectorRepository.findAllActiveEmbeddings();
        MatchBiometrico mejor = null;
        double menorDistancia = Double.MAX_VALUE;

        for (VectorFacial vf : activos) {
            try {
                List<Double> base = objectMapper.readValue(vf.getVectorEmbedding(), new TypeReference<List<Double>>() {});
                double dist = calcularDistanciaEuclidiana(vectorTarget, base);
                if (dist < menorDistancia) {
                    menorDistancia = dist;
                    mejor = new MatchBiometrico(vf.getEmpleado(), dist);
                }
            } catch (Exception e) {
                log.error("Error al deserializar vector id {}: {}", vf.getIdVector(), e.getMessage());
            }
        }
        return mejor;
    }

    /**
     * Fórmula: d(p, q) = sqrt( sum( (pi - qi)^2 ) )
     */
    private double calcularDistanciaEuclidiana(List<Double> a, List<Double> b) {
        int len = Math.min(a.size(), b.size());
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static class MatchBiometrico {
        Empleado empleado;
        double distancia;
        MatchBiometrico(Empleado e, double d) { this.empleado = e; this.distancia = d; }
    }

    @Transactional
    public Map<String, Object> aplicarContingencia(ContingenciaRequestDTO req) {
        // 1. Verificación del PIN secreto (debe ser exactamente 2586)
        if (req.getPinContingencia() == null || !req.getPinContingencia().trim().equals(pinContingenciaMaestro)) {
            throw new SecurityException("⛔ PIN de contingencia inválido o no autorizado.");
        }

        // 2. Fecha automática: siempre toma el día de HOY sin pedir fecha
        LocalDate fechaHoy = LocalDate.now();

        // 3. Buscar el empleado
        Empleado emp = empleadoRepository.findById(req.getIdEmpleado().intValue())
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con ID: " + req.getIdEmpleado()));

        // 4. Buscar su programación de hoy o crearla si no existía
        ProgramacionSemanal prog = programacionRepository.findByEmpleadoAndFecha(emp, fechaHoy)
                .orElseGet(() -> {
                    ProgramacionSemanal p = ProgramacionSemanal.builder()
                            .empleado(emp)
                            .fecha(fechaHoy)
                            .horaEntradaProgramada(LocalTime.of(6, 0))
                            .horaSalidaProgramada(LocalTime.of(14, 30))
                            .minutosAlmuerzoProgramados(30)
                            .horasExtrasAutorizadas(false)
                            .tipoNovedadPrecargada(TipoNovedad.NINGUNA)
                            .bloqueado(false)
                            .fechaModificacion(LocalDateTime.now())
                            .build();
                    return programacionRepository.save(p);
                });

        // 5. Buscar o crear el registro de asistencia
        RegistroAsistencia reg = registroRepository.findByProgramacion(prog)
                .orElseGet(() -> {
                    RegistroAsistencia r = RegistroAsistencia.builder()
                            .programacion(prog)
                            .estadoDia(EstadoDia.EN_CURSO)
                            .estadoLlegada(EstadoLlegada.A_TIEMPO)
                            .minutosRetardo(0)
                            .build();
                    return r;
                });

        LocalTime horaEntradaProg = prog.getHoraEntradaProgramada();
        LocalTime horaSalidaProg = prog.getHoraSalidaProgramada() != null 
                ? prog.getHoraSalidaProgramada() 
                : horaEntradaProg.plusHours(8);

        // 6. Aplicar ENTRADA a tiempo (sin retardo)
        if ("ENTRADA".equalsIgnoreCase(req.getTipoAccion()) || "JORNADA_COMPLETA".equalsIgnoreCase(req.getTipoAccion())) {
            LocalTime horaEntradaFinal = req.getHoraManual() != null && !req.getHoraManual().isBlank()
                    ? LocalTime.parse(req.getHoraManual()) : horaEntradaProg;
            reg.setHoraEntradaReal(LocalDateTime.of(fechaHoy, horaEntradaFinal));
            reg.setEstadoLlegada(EstadoLlegada.A_TIEMPO);
            reg.setMinutosRetardo(0);
        }

        // 7. Aplicar SALIDA completada
        if ("SALIDA".equalsIgnoreCase(req.getTipoAccion()) || "JORNADA_COMPLETA".equalsIgnoreCase(req.getTipoAccion())) {
            LocalTime horaSalidaFinal = req.getHoraManual() != null && !req.getHoraManual().isBlank()
                    ? LocalTime.parse(req.getHoraManual()) : horaSalidaProg;
            reg.setHoraSalidaReal(LocalDateTime.of(fechaHoy, horaSalidaFinal));
            reg.setEstadoDia(EstadoDia.COMPLETADO);
        }

        registroRepository.save(reg);

        return Map.of(
            "exito", true,
            "mensaje", "✅ Contingencia aplicada con éxito para " + emp.getNombreApellido() + " (Fecha: " + fechaHoy + ")"
        );
    }



}
