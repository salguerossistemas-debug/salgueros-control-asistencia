-- ========================================================
-- Salgueros Control de Asistencia - PostgreSQL / Neon
-- ========================================================

-- 1. Limpieza inicial
DROP TABLE IF EXISTS registros_asistencia CASCADE;
DROP TABLE IF EXISTS programacion_semanal CASCADE;
DROP TABLE IF EXISTS vectores_faciales CASCADE;
DROP TABLE IF EXISTS empleados CASCADE;

DROP TYPE IF EXISTS estado_empleado CASCADE;
DROP TYPE IF EXISTS tipo_novedad CASCADE;
DROP TYPE IF EXISTS estado_llegada CASCADE;
DROP TYPE IF EXISTS estado_dia CASCADE;

-- 2. Tipos ENUM para PostgreSQL
CREATE TYPE estado_empleado AS ENUM ('ACTIVO', 'INACTIVO', 'EN_PROCESO_RETIRO');
CREATE TYPE tipo_novedad AS ENUM ('NINGUNA', 'INCAPACIDAD', 'PERMISO', 'VACACIONES', 'RETIRO');
CREATE TYPE estado_llegada AS ENUM ('A_TIEMPO', 'RETARDO', 'NO_APLICA');
CREATE TYPE estado_dia AS ENUM ('EN_CURSO', 'COMPLETADO', 'FALTO', 'NOVEDAD_JUSTIFICADA');

-- 3. Tabla de Empleados
CREATE TABLE empleados (
    id_empleado SERIAL PRIMARY KEY,
    cedula VARCHAR(20) NOT NULL UNIQUE,
    nombre_apellido VARCHAR(100) NOT NULL,
    area VARCHAR(50) NOT NULL DEFAULT 'PLANTA',
    estado estado_empleado DEFAULT 'ACTIVO'
);

-- 4. Tabla de Vectores Faciales
CREATE TABLE vectores_faciales (
    id_vector SERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    vector_embedding JSONB NOT NULL,
    CONSTRAINT fk_vector_empleado FOREIGN KEY (id_empleado) 
        REFERENCES empleados(id_empleado) ON DELETE CASCADE
);

-- 5. Tabla de Programación Semanal
CREATE TABLE programacion_semanal (
    id_programacion BIGSERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    fecha DATE NOT NULL,
    hora_entrada_programada TIME NOT NULL,
    hora_salida_programada TIME NULL,
    minutos_almuerzo_programados INT DEFAULT 60,
    horas_extras_autorizadas BOOLEAN DEFAULT FALSE,
    tipo_novedad_precargada tipo_novedad DEFAULT 'NINGUNA',
    bloqueado BOOLEAN DEFAULT FALSE,
    fecha_modificacion TIMESTAMP NULL,
    CONSTRAINT fk_prog_empleado FOREIGN KEY (id_empleado) 
        REFERENCES empleados(id_empleado),
    CONSTRAINT uq_empleado_fecha UNIQUE (id_empleado, fecha)
);

-- 6. Tabla de Registros de Asistencia
CREATE TABLE registros_asistencia (
    id_registro BIGSERIAL PRIMARY KEY,
    id_programacion BIGINT NOT NULL UNIQUE,
    hora_entrada_real TIMESTAMP NOT NULL,
    hora_salida_almuerzo TIMESTAMP NULL,
    hora_regreso_almuerzo TIMESTAMP NULL,
    hora_salida_real TIMESTAMP NULL,
    minutos_retardo INT DEFAULT 0,
    estado_llegada estado_llegada NOT NULL,
    estado_dia estado_dia DEFAULT 'EN_CURSO',
    CONSTRAINT fk_registro_programacion FOREIGN KEY (id_programacion) 
        REFERENCES programacion_semanal(id_programacion)
);

-- 7. Datos iniciales (Empleados)
INSERT INTO empleados (id_empleado, cedula, nombre_apellido, estado) VALUES
(1, '1020304050', 'Daniel Muñoz (Admin/Dev)', 'ACTIVO'),
(2, '9876543210', 'Carlos Gómez (Cajero POS 1)', 'ACTIVO'),
(3, '1122334455', 'Laura Restrepo (Ventas POS 2)', 'ACTIVO');

-- Ajustar el contador de IDs para que el siguiente empleado sea el 4
SELECT setval('empleados_id_empleado_seq', (SELECT MAX(id_empleado) FROM empleados));

-- 8. Programación inicial (Fecha actual)
INSERT INTO programacion_semanal (id_empleado, fecha, hora_entrada_programada, minutos_almuerzo_programados, horas_extras_autorizadas, tipo_novedad_precargada) VALUES
(1, CURRENT_DATE, '08:00:00', 60, TRUE, 'NINGUNA'),
(2, CURRENT_DATE, '07:00:00', 0, FALSE, 'NINGUNA'),
(3, CURRENT_DATE, '09:00:00', 60, FALSE, 'NINGUNA');
