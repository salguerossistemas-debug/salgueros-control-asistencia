package com.salgueros.asistencia.controller;

import com.salgueros.asistencia.dto.CrearEmpleadoRequestDTO;
import com.salgueros.asistencia.dto.EnrolamientoRequestDTO;
import com.salgueros.asistencia.dto.MarcacionRequestDTO;
import com.salgueros.asistencia.dto.MarcacionResponseDTO;
import com.salgueros.asistencia.dto.ProgramacionRequestDTO;
import com.salgueros.asistencia.model.Empleado;
import com.salgueros.asistencia.service.AsistenciaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencia")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    // Carga la lista de IPs autorizadas desde application.properties
    // Valor por defecto en caso de no definir la propiedad: localhost (127.0.0.1 y 0:0:0:0:0:0:0:1)
    @Value("${salgueros.sedes.ips-autorizadas:127.0.0.1,0:0:0:0:0:0:0:1}")
    private List<String> ipsAutorizadas;

    @GetMapping("/empleados")
    public ResponseEntity<List<Empleado>> listarEmpleados() {
        return ResponseEntity.ok(asistenciaService.listarEmpleadosActivos());
    }

    @PostMapping("/empleados/crear")
    public ResponseEntity<?> crearEmpleado(@RequestBody CrearEmpleadoRequestDTO request) {
        try {
            Map<String, Object> res = asistenciaService.crearNuevoEmpleado(request);
            return ResponseEntity.ok(res);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("exito", false, "mensaje", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/admin/validar-pin")
    public ResponseEntity<?> validarPin(@RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        try {
            asistenciaService.validarPinAdmin(pin);
            return ResponseEntity.ok(Map.of("valido", true, "mensaje", "Autenticación administrativa exitosa"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("valido", false, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/programacion/guardar")
    public ResponseEntity<?> programarTurnos(@RequestBody ProgramacionRequestDTO request) {
        try {
            Map<String, Object> res = asistenciaService.programarTurnos(request);
            return ResponseEntity.ok(res);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("exito", false, "mensaje", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/programacion/toggle-bloqueo")
    public ResponseEntity<?> alternarBloqueo(@RequestBody Map<String, Object> body) {
        try {
            LocalDate inicio = body.get("fechaInicio") != null ? LocalDate.parse(body.get("fechaInicio").toString()) : LocalDate.now();
            LocalDate fin = body.get("fechaFin") != null ? LocalDate.parse(body.get("fechaFin").toString()) : inicio;
            boolean bloquear = Boolean.parseBoolean(String.valueOf(body.get("bloqueado")));
            return ResponseEntity.ok(asistenciaService.alternarBloqueoTurnos(inicio, fin, bloquear));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/programacion/guardar-matriz")
    public ResponseEntity<?> guardarMatriz(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(asistenciaService.guardarMatrizSemanal(body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }

    
    @GetMapping("/programacion/listar")
    public ResponseEntity<List<Map<String, Object>>> listarProgramacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(asistenciaService.listarProgramacionRango(inicio, fin));
    }

    @GetMapping("/registros/listar")
    public ResponseEntity<List<Map<String, Object>>> listarRegistros(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(asistenciaService.listarRegistrosAsistencia(inicio, fin));
    }

    @PostMapping("/enrolar")
    public ResponseEntity<?> enrolarRostro(@RequestBody EnrolamientoRequestDTO request) {
        try {
            Map<String, Object> res = asistenciaService.enrolarRostro(request);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/marcar")
    public ResponseEntity<MarcacionResponseDTO> registrarMarcacion(
            @RequestBody MarcacionRequestDTO request, 
            HttpServletRequest httpRequest) {
        
        // 1. Extraer la IP del dispositivo que realiza la marcación
        String ipCliente = obtenerIpCliente(httpRequest);

        // 2. Validar que la IP esté dentro del listado de sedes autorizadas (o comodín '*' para pruebas)
        if (!esIpAutorizada(ipCliente)) {
            MarcacionResponseDTO respuestaRechazada = MarcacionResponseDTO.builder()
                    .exito(false)
                    .mensaje("Marcación denegada: Su dispositivo (IP: " + ipCliente + ") no está en la red de un punto autorizado.")
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respuestaRechazada);
        }

        // 3. Procesar la marcación si la IP es válida
        MarcacionResponseDTO response = asistenciaService.procesarMarcacion(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Valida si una IP cliente está autorizada, soportando IPs fijas, comodín '*' y resolución de DNS dinámico (DuckDNS).
     */
    private boolean esIpAutorizada(String ipCliente) {
        if (ipsAutorizadas == null || ipsAutorizadas.isEmpty()) {
            return true;
        }
        if (ipsAutorizadas.contains("*") || ipsAutorizadas.contains(ipCliente)) {
            return true;
        }
        for (String entry : ipsAutorizadas) {
            String trimmed = entry != null ? entry.trim() : "";
            if (trimmed.isEmpty()) continue;
            if (trimmed.equalsIgnoreCase(ipCliente)) {
                return true;
            }
            // Si la entrada es un hostname/dominio dinámico (ej: salgueros-punto1.duckdns.org)
            if (trimmed.contains(".") && !trimmed.matches("^[0-9.]+$")) {
                try {
                    InetAddress[] resolvedAddrs = InetAddress.getAllByName(trimmed);
                    for (InetAddress addr : resolvedAddrs) {
                        if (addr.getHostAddress().equalsIgnoreCase(ipCliente)) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                    // Si el DNS no resuelve en este momento, continuar evaluando siguientes entradas
                }
            }
        }
        return false;
    }

    /**
     * Extrae la IP de origen del cliente manejando posibles proxies o cabeceras X-Forwarded-For
     */
    private String obtenerIpCliente(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

        @PostMapping("/contingencia/aplicar")
    public ResponseEntity<?> aplicarContingencia(@RequestBody com.salgueros.asistencia.dto.ContingenciaRequestDTO request) {
        try {
            Map<String, Object> resp = asistenciaService.aplicarContingencia(request);
            return ResponseEntity.ok(resp);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("exito", false, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }
}