package com.salgueros.asistencia.config;

import com.salgueros.asistencia.model.EstadoLlegada;
import com.salgueros.asistencia.model.ProgramacionSemanal;
import com.salgueros.asistencia.model.RegistroAsistencia;
import com.salgueros.asistencia.repository.RegistroAsistenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final RegistroAsistenciaRepository registroRepository;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate, RegistroAsistenciaRepository registroRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.registroRepository = registroRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Iniciando validación y sincronización de columnas en PostgreSQL / MySQL...");
        
        // 1. Agregar columna 'bloqueado' si no existe
        ejecutarSqlSeguro("ALTER TABLE programacion_semanal ADD COLUMN IF NOT EXISTS bloqueado BOOLEAN DEFAULT FALSE");
        ejecutarSqlSeguro("UPDATE programacion_semanal SET bloqueado = FALSE WHERE bloqueado IS NULL");

        // 2. Agregar columna 'hora_salida_programada' si no existe
        ejecutarSqlSeguro("ALTER TABLE programacion_semanal ADD COLUMN IF NOT EXISTS hora_salida_programada TIME NULL");

        // 3. Agregar columna 'fecha_modificacion' si no existe
        ejecutarSqlSeguro("ALTER TABLE programacion_semanal ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP NULL");

        // 4. Normalizar registros existentes con la regla de llegada temprana y alivio de 8 minutos
        normalizarRegistrosAsistenciaConAlivio();

        log.info("Sincronización de esquema completada exitosamente.");
    }

    private void normalizarRegistrosAsistenciaConAlivio() {
        try {
            List<RegistroAsistencia> registros = registroRepository.findRegistrosEntreFechas(
                    LocalDate.of(2025, 1, 1), LocalDate.of(2030, 12, 31));
            int actualizados = 0;
            for (RegistroAsistencia r : registros) {
                ProgramacionSemanal p = r.getProgramacion();
                if (p == null || p.getHoraEntradaProgramada() == null || r.getHoraEntradaReal() == null) {
                    continue;
                }
                LocalDate fecha = p.getFecha() != null ? p.getFecha() : r.getHoraEntradaReal().toLocalDate();
                LocalTime entProg = p.getHoraEntradaProgramada();
                LocalDateTime entProgDT = LocalDateTime.of(fecha, entProg);
                LocalDateTime entReal = r.getHoraEntradaReal();

                if (entReal.isBefore(entProgDT)) {
                    r.setHoraEntradaReal(entProgDT);
                    r.setMinutosRetardo(0);
                    r.setEstadoLlegada(EstadoLlegada.A_TIEMPO);
                    registroRepository.save(r);
                    actualizados++;
                } else if (entReal.getSecond() != 0) {
                    long diffMin = Duration.between(entProgDT, entReal).toMinutes();
                    if (diffMin <= 8) {
                        r.setHoraEntradaReal(entProgDT);
                        r.setMinutosRetardo(0);
                        r.setEstadoLlegada(EstadoLlegada.A_TIEMPO);
                    } else {
                        int retardoEfectivo = (int) (diffMin - 8);
                        r.setHoraEntradaReal(entProgDT.plusMinutes(retardoEfectivo));
                        r.setMinutosRetardo(retardoEfectivo);
                        r.setEstadoLlegada(EstadoLlegada.RETARDO);
                    }
                    registroRepository.save(r);
                    actualizados++;
                }
            }
            if (actualizados > 0) {
                log.info("Se normalizaron {} registros históricos con la regla de alivio de 8 minutos.", actualizados);
            }
        } catch (Exception e) {
            log.warn("No se pudo ejecutar normalización de registros históricos: {}", e.getMessage());
        }
    }

    private void ejecutarSqlSeguro(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("Sentencia SQL no requerida o ya aplicada: {}", e.getMessage());
        }
    }
}
