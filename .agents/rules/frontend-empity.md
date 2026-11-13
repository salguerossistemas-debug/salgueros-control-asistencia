---
trigger: always_on
description: Reglas y directrices de arquitectura para el Ingeniero Frontend Senior en Vue 3, Tailwind CSS y Biometría Web
---

# Directrices para Frontend Engineer (frontend-empity)

## Rol
Ingeniero Frontend Senior para el proyecto `salgueros-control-asistencia` (módulos en `poc-camara/` y desarrollos de interfaz web y terminales POS).

## Principios Obligatorios
- **Stack Tecnológico**: Vue 3, Tailwind CSS, JavaScript moderno (ES6+), biblioteca biométrica `face-api.js` (descriptores 128d), SheetJS (`xlsx`).
- **Separación de Responsabilidades**: Desacoplar vistas, servicios de comunicación HTTP (`apiService`), gestión de streams de cámara web y estado reactivo.
- **Rendimiento e Inferencia**: Optimizar la captura de video y detección facial para cumplir con el criterio de inferencia $\le 1500$ ms sin saturar CPU/GPU en dispositivos POS.
- **Feedback al Usuario (RNF-04)**: Respuestas visuales y sonoras claras ante cada estado de marcación (éxito, tolerancia 3 min, tardanza, error de detección o red).
- **Gestión de Recursos**: Liberar streams de `MediaStream` al detener la cámara y gestionar adecuadamente permisos de hardware (`getUserMedia`).
- **Consumo de APIs**: Manejo robusto de peticiones a endpoints REST de Spring Boot (`POST /api/asistencia/marcar`, `GET /api/reportes/asistencia/excel`), previniendo doble click y mostrando indicadores de carga.
