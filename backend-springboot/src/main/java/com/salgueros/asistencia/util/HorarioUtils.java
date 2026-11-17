package com.salgueros.asistencia.util;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades centrales para manejar horarios laborales.
 *
 * Reglas:
 *  - Toda la app trabaja con la hora oficial de Colombia (America/Bogota),
 *    sin importar si el servidor (Render) está en UTC.
 *  - Acepta cualquier hora personalizada escrita por el usuario (ej. "2:30", "3:0",
 *    "02:30", "14:30", "14:30:00", "2:30 PM", "3:0 p.m.", "6:15 am", etc.).
 *  - Un turno puede cruzar la medianoche (ej. 22:00 a 06:00). En ese caso la
 *    salida es "del día siguiente" y la duración se calcula sumando 24 horas.
 */
public final class HorarioUtils {

    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private static final Pattern PATRON_HORA = Pattern.compile(
            "^(\\d{1,2})(?::(\\d{0,2}))?(?::(\\d{0,2}))?$"
    );

    private HorarioUtils() {}

    /**
     * Parsea cualquier texto de hora personalizado ("2:30", "3:0", "14:30", "2:30 PM", "3:0 p.m.", "08:00:00", etc.)
     * y lo convierte a LocalTime.
     */
    public static LocalTime parsearHoraFlexible(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Debes indicar la " + nombreCampo + " del turno (ej. 08:00, 2:30 PM, 14:30).");
        }

        String limpio = valor.trim().toLowerCase(Locale.ROOT);
        Boolean esPm = null;
        if (limpio.contains("p.m") || limpio.contains("p. m") || limpio.endsWith("pm") || limpio.contains(" pm")) {
            esPm = true;
        } else if (limpio.contains("a.m") || limpio.contains("a. m") || limpio.endsWith("am") || limpio.contains(" am")) {
            esPm = false;
        }

        // Quitar sufijos am/pm y normalizar separadores (., ;) a :
        limpio = limpio.replaceAll("[aApP]\\.?\\s*[mM]\\.?", "").trim();
        limpio = limpio.replace('.', ':').replace(';', ':').replace(',', ':').replaceAll("\\s+", "");

        Matcher m = PATRON_HORA.matcher(limpio);
        if (!m.matches()) {
            throw new IllegalArgumentException("Formato inválido en " + nombreCampo + " ('" + valor + "'). Escribe por ejemplo 2:30, 3:00, 14:30 o 08:00.");
        }

        int h = Integer.parseInt(m.group(1));
        int mi = (m.group(2) != null && !m.group(2).isEmpty()) ? Integer.parseInt(m.group(2)) : 0;
        int s = (m.group(3) != null && !m.group(3).isEmpty()) ? Integer.parseInt(m.group(3)) : 0;

        if (esPm != null) {
            if (Boolean.TRUE.equals(esPm) && h >= 1 && h < 12) {
                h += 12;
            } else if (Boolean.FALSE.equals(esPm) && h == 12) {
                h = 0;
            }
        }

        if (h < 0 || h > 23 || mi < 0 || mi > 59 || s < 0 || s > 59) {
            throw new IllegalArgumentException("La " + nombreCampo + " ('" + valor + "') está fuera de rango (horas 0-23, minutos 0-59).");
        }

        return LocalTime.of(h, mi, s);
    }

    /** true si el turno termina al día siguiente (ej. 22:00 → 06:00). */
    public static boolean esTurnoNocturno(LocalTime entrada, LocalTime salida) {
        return entrada != null && salida != null && salida.isBefore(entrada);
    }

    /** Minutos totales del turno (con almuerzo incluido), soportando cruce de medianoche. */
    public static long minutosTurno(LocalTime entrada, LocalTime salida) {
        if (entrada == null || salida == null) return 0;
        long min = Duration.between(entrada, salida).toMinutes();
        if (min < 0) min += 24 * 60;
        return min;
    }

    /**
     * Ajusta los minutos de almuerzo cuando el turno personalizado es corto
     * (por ejemplo un turno de 30 o 60 minutos donde el usuario dejó 60 min por defecto),
     * para nunca bloquear la creación del horario personalizado.
     */
    public static int ajustarAlmuerzoSegunTurno(LocalTime entrada, LocalTime salida, Integer minutosAlmuerzo) {
        int almuerzo = minutosAlmuerzo != null ? Math.max(0, minutosAlmuerzo) : 60;
        long duracion = minutosTurno(entrada, salida);
        if (duracion > 0 && duracion <= almuerzo) {
            return 0;
        }
        return almuerzo;
    }

    /** Minutos laborales netos programados (turno − almuerzo). */
    public static long minutosLaboralesProgramados(LocalTime entrada, LocalTime salida, Integer minutosAlmuerzo) {
        long turno = minutosTurno(entrada, salida);
        int almuerzo = minutosAlmuerzo != null ? Math.max(0, minutosAlmuerzo) : 0;
        if (turno <= almuerzo) {
            return turno;
        }
        return turno - almuerzo;
    }

    /**
     * Valida un horario enviado por el administrador. Lanza IllegalArgumentException
     * con un mensaje claro en vez de reemplazarlo en silencio por 08:00 – 17:00.
     */
    public static void validarHorario(LocalTime entrada, LocalTime salida, Integer minutosAlmuerzo) {
        if (entrada == null) {
            throw new IllegalArgumentException("Debes indicar la HORA DE ENTRADA del turno (ej. 06:00, 08:00, 2:30).");
        }
        if (salida == null) {
            throw new IllegalArgumentException("Debes indicar la HORA DE SALIDA del turno (ej. 2:30, 3:00, 14:30).");
        }
        if (entrada.equals(salida)) {
            throw new IllegalArgumentException("La hora de entrada y la de salida no pueden ser iguales (" + entrada + ").");
        }
        int almuerzo = minutosAlmuerzo != null ? minutosAlmuerzo : 0;
        if (almuerzo < 0) {
            throw new IllegalArgumentException("Los minutos de almuerzo no pueden ser negativos.");
        }
    }
}
