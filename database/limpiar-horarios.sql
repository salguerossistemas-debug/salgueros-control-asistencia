-- ============================================================================
-- SALGUEROS — LIMPIEZA EXCLUSIVA DE HORARIOS / PROGRAMACIÓN SEMANAL
-- ============================================================================
-- Este script elimina ÚNICAMENTE los horarios programados y asistencias asociadas.
-- PRESERVA al 100%:
--   1. Tabla `empleados`: nombres, apellidos, cédulas, áreas y estado.
--   2. Tabla `vectores_faciales`: embeddings y retratos biométricos (128d).
-- ============================================================================

-- Para MySQL / MariaDB / TiDB Cloud:
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM registros_asistencia;
DELETE FROM programacion_semanal;
SET FOREIGN_KEY_CHECKS = 1;

-- Para PostgreSQL / Neon (si aplica):
-- TRUNCATE TABLE registros_asistencia, programacion_semanal CASCADE;
