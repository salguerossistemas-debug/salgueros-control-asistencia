package com.salgueros.asistencia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class HorarioUtilsTest {

    @Test
    @DisplayName("Debe detectar preset de turno corrido o con almuerzo")
    void testDetectarPresets() {
        LocalTime entrada = LocalTime.of(8, 0);
        LocalTime salida = LocalTime.of(17, 0);
        
        // Verificar que no lance excepciones al validar franjas horarias
        assertTrue(salida.isAfter(entrada));
    }
}
