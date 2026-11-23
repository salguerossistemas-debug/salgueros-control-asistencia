package com.salgueros.asistencia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PinAdminValidationTest {

    private AsistenciaService asistenciaService;

    @BeforeEach
    void setUp() {
        asistenciaService = new AsistenciaService(null, null, null, null, null);
        ReflectionTestUtils.setField(asistenciaService, "adminPin", "claveProduccion2026");
    }

    @Test
    @DisplayName("Acepta exactamente el PIN configurado")
    void aceptaPinConfigurado() {
        assertDoesNotThrow(() -> asistenciaService.validarPinAdmin("claveProduccion2026"));
        assertDoesNotThrow(() -> asistenciaService.validarPinAdmin("  claveProduccion2026  "));
    }

    @Test
    @DisplayName("Rechaza PINs por defecto cuando hay uno personalizado configurado")
    void rechazaBackdoorCuandoHayPinPersonalizado() {
        assertThrows(SecurityException.class, () -> asistenciaService.validarPinAdmin("salgueros2026"));
        assertThrows(SecurityException.class, () -> asistenciaService.validarPinAdmin("salgueros2026."));
    }

    @Test
    @DisplayName("Rechaza PINs vacíos o reservados")
    void rechazaPinsVaciosOReservados() {
        assertThrows(SecurityException.class, () -> asistenciaService.validarPinAdmin(null));
        assertThrows(SecurityException.class, () -> asistenciaService.validarPinAdmin("   "));

        ReflectionTestUtils.setField(asistenciaService, "adminPin", "2586");
        assertThrows(SecurityException.class, () -> asistenciaService.validarPinAdmin("2586"));

        ReflectionTestUtils.setField(asistenciaService, "adminPin", "2026");
        assertThrows(SecurityException.class, () -> asistenciaService.validarPinAdmin("2026"));
    }
}
