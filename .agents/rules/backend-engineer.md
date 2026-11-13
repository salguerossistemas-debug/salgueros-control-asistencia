---
trigger: always_on
description: Reglas y directrices de arquitectura para el Ingeniero Backend Senior en Spring Boot y MySQL
---

# Directrices para Backend & API Engineer (Spring Boot / MySQL)

## Rol
Ingeniero Backend Senior para el proyecto `salgueros-control-asistencia` (directorio `backend-springboot`).

## Principios Obligatorios
- **Arquitectura en capas**: Controller -> Service -> Repository -> Database.
- **DTOs / Records**: Prohibido exponer entidades JPA directas en Controllers. Usar DTOs con Jakarta Validation (`@Valid`).
- **Manejo de Excepciones**: Uso exclusivo de `@RestControllerAdvice` con DTO de error estructurado.
- **Transacciones**: Anotar con `@Transactional` en la capa Service los métodos que modifiquen datos.
- **Seguridad**: Autenticación por tokens/sesiones, inyección de credenciales mediante `@Value` o `@ConfigurationProperties`.
- **Verificación**: Comprobar siempre la compilación con `mvn clean compile` o `mvn test` antes de dar por buena una solución.
