# Proyecto Innovación de Software Salgueros
## Sistema Web de Control de Asistencia y Puntualidad por Reconocimiento Facial

Este repositorio contiene la arquitectura completa construida con base en las **Especificaciones Técnicas Oficiales de Salgueros (PDF)**.

---

### Estructura del Proyecto

```
salgueros-control-asistencia/
│
├── poc-camara/
│   └── index.html                <-- PoC Cámara POS (Vue 3 + face-api.js 128d)
│
├── database/
│   └── schema-and-seed.sql       <-- Script DDL & Datos iniciales para MySQL Workbench
│
└── backend-springboot/           <-- API REST Spring Boot (Java 17)
    ├── pom.xml                   <-- Dependencias (Spring Web, Data JPA, MySQL, Apache POI)
    └── src/main/java/com/salgueros/asistencia/
        ├── model/                <-- Entidades (Empleado, VectorFacial, Programacion, Registro)
        ├── repository/           <-- Spring Data Repositories
        ├── service/
        │   ├── AsistenciaService.java   <-- Comparación Euclidiana + Motor de Tolerancia 3 Minutos
        │   └── ExcelReporteService.java <-- Generador de reportes Excel (.xlsx) con Apache POI
        └── controller/
            ├── AsistenciaController.java  <-- POST /api/asistencia/marcar
            └── ReporteExcelController.java<-- GET /api/reportes/asistencia/excel
```

---

### Paso 1: Ejecutar la PoC de la Cámara Web (Inmediato)

Puedes probar la cámara web integrada del computador POS de inmediato:
1. Haz doble clic o abre con tu navegador (Chrome / Edge):
   `salgueros-control-asistencia/poc-camara/index.html`
2. Presiona el botón verde **"Iniciar Cámara"**.
3. El sistema descargará los modelos neuronales, detectará tu rostro en tiempo real y mostrará:
   - **Tiempo de inferencia** (debe ser $\le 1500$ ms para cumplir el criterio de la PoC).
   - **Vector numérico de 128 flotantes** (RNF-03).
   - Botón de **"Simular Marcación POS"** con sonido de confirmación (RNF-04).

---

### Paso 2: Crear la Base de Datos en MySQL Workbench

1. Abre **MySQL Workbench**.
2. Conéctate a tu servidor local de MySQL.
3. Abre el archivo `database/schema-and-seed.sql` y ejecútalo (`Ctrl + Shift + Enter`).
4. Esto creará la base de datos `salgueros_asistencia`, las 4 tablas del MER y 3 empleados de prueba programados para el día de hoy.

---

### Paso 3: Ejecutar el Backend Spring Boot

1. Asegúrate de tener instalado Java 17+ ([adoptium.net](https://adoptium.net/)).
2. Abre la carpeta `backend-springboot` en tu **VS Code**.
3. Verifica la contraseña de tu base de datos en `src/main/resources/application.properties`.
4. Ejecuta el archivo principal:
   `SalguerosAsistenciaApplication.java`
5. El backend estará disponible en `http://localhost:8080`.

---

### Endpoints Principales

* **Marcación Biométrica:** `POST http://localhost:8080/api/asistencia/marcar`
* **Descarga de Reporte Excel:** `GET http://localhost:8080/api/reportes/asistencia/excel`
