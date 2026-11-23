package com.salgueros.asistencia.controller;

import com.salgueros.asistencia.dto.ConfirmarRecepcionDTO;
import com.salgueros.asistencia.dto.CrearRemisionDTO;
import com.salgueros.asistencia.model.Remision;
import com.salgueros.asistencia.model.TipoRemision;
import com.salgueros.asistencia.service.RemisionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/remisiones")
public class RemisionController {

    private final RemisionService remisionService;

    @Value("${salgueros.planta.pin:planta2026}")
    private String plantaPin;

    public RemisionController(RemisionService remisionService) {
        this.remisionService = remisionService;
    }

    private boolean esPinInvalido(String pinHeader) {
        if (pinHeader == null || pinHeader.trim().isEmpty()) {
            return true;
        }
        return !plantaPin.equals(pinHeader.trim());
    }

    @GetMapping("/catalogo")
    public ResponseEntity<List<String>> obtenerCatalogo(
            @RequestParam(value = "tipo", defaultValue = "INSUMOS") TipoRemision tipo) {
        return ResponseEntity.ok(remisionService.obtenerCatalogoPorTipo(tipo));
    }

    @GetMapping("/siguiente-numero")
    public ResponseEntity<Map<String, Object>> obtenerSiguienteNumero(
            @RequestParam(value = "tipo", defaultValue = "INSUMOS") TipoRemision tipo) {
        Integer siguiente = remisionService.obtenerSiguienteConsecutivo(tipo);
        String formateado = String.format("%04d", siguiente);
        return ResponseEntity.ok(Map.of(
                "tipo", tipo,
                "siguienteNumero", siguiente,
                "numeroFormateado", formateado
        ));
    }

    @PostMapping
    public ResponseEntity<?> crearRemision(
            @RequestHeader(value = "X-Admin-PIN", required = false) String pinHeader,
            @RequestBody CrearRemisionDTO dto) {

        if (esPinInvalido(pinHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Clave de Planta inválida o no proporcionada"));
        }

        try {
            Remision remision = remisionService.crearRemision(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(remision);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al crear remisión: " + e.getMessage()));
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Remision>> listarPendientes(
            @RequestParam(value = "tipo", required = false) TipoRemision tipo) {
        return ResponseEntity.ok(remisionService.listarPendientes(tipo));
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<Remision>> listarRecientes() {
        return ResponseEntity.ok(remisionService.listarTodas());
    }

    @GetMapping("/sede/{puntoVenta}")
    public ResponseEntity<List<Remision>> listarPorSede(@PathVariable String puntoVenta) {
        return ResponseEntity.ok(remisionService.listarPorSede(puntoVenta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return remisionService.obtenerPorId(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Remisión no encontrada con ID: " + id)));
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmarRecepcion(
            @PathVariable Long id,
            @RequestBody ConfirmarRecepcionDTO dto) {
        try {
            Remision confirmada = remisionService.confirmarRecepcion(id, dto);
            return ResponseEntity.ok(confirmada);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al confirmar recepción: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTodas(
            @RequestHeader(value = "X-Admin-PIN", required = false) String pinHeader,
            @RequestParam(value = "tipo", required = false) TipoRemision tipo) {

        if (esPinInvalido(pinHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Clave de Planta inválida o no proporcionada"));
        }

        if (tipo != null) {
            return ResponseEntity.ok(remisionService.listarPorTipo(tipo));
        }
        return ResponseEntity.ok(remisionService.listarTodas());
    }
}
