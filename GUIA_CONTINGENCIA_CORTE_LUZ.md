# GUÍA DE CONTINGENCIA (CORTE DE LUZ / INTERNET) Y SINCRONIZACIÓN EN VS CODE

Este documento contiene la explicación completa y el código exacto para programar el botón de **Contingencia por Corte de Luz / Internet con PIN privado 2586**, disponible directamente dentro de tu proyecto para que lo puedas ver y editar desde Visual Studio Code.

---

## 1. CÓMO VER ESTOS CAMBIOS EN TU VISUAL STUDIO CODE

Si tu VS Code está en otra ventana o en una terminal diferente:

1. Abre tu terminal de **Visual Studio Code** (`Ctrl + ~`).
2. Asegúrate de estar en la carpeta raíz del proyecto:
   ```powershell
   cd C:\salgueros-control-asistencia\salgueros-control-asistencia
   ```
3. Ejecuta:
   ```powershell
   git status
   ```
   Verás este archivo creado como `GUIA_CONTINGENCIA_CORTE_LUZ.md`.
4. Si quieres sincronizar lo que hemos subido a GitHub hasta ahora:
   ```powershell
   git pull origin main
   ```

---

## 2. ARQUITECTURA DE LA CONTINGENCIA (POR QUÉ FUNCIONA ASÍ)

Cuando hay un corte de luz en el punto de venta o se cae el internet:
1. El encargado llena un formato físico o digital con los nombres y las horas reales de llegada/salida.
2. Te lo envían al correo como soporte auditado.
3. Tú en el sistema abres el botón **⚡ Contingencia: Corte de Luz / Internet**.
4. El sistema **no te pide fecha** (asume automáticamente hoy `LocalDate.now()`).
5. Ingresas tu PIN maestro privado: **`2586`** (nadie más lo conoce).
6. Seleccionas el empleado del correo y el tipo de jornada (**JORNADA_COMPLETA**, **ENTRADA** o **SALIDA**).
7. El sistema en la nube (TiDB Cloud) le asigna su asistencia a tiempo exacto sin cobrar retardos injustos y deja la auditoría oficial en el Excel de nómina.

---

## 3. CÓDIGO EXACTO PARA PROGRAMARLO TÚ MISMO

### PASO 1: Crear el DTO en el Backend
Ruta del archivo:
`backend-springboot/src/main/java/com/salgueros/asistencia/dto/ContingenciaRequestDTO.java`

```java
package com.salgueros.asistencia.dto;

public class ContingenciaRequestDTO {
    private String pinContingencia; // Debe ser 2586
    private Long idEmpleado;
    private String tipoAccion;      // "JORNADA_COMPLETA", "ENTRADA", "SALIDA"
    private String horaManual;      // Opcional (ej: "06:15", o null para hora programada)
    private String observacion;     // Ej: "Reporte de contingencia por correo"

    public ContingenciaRequestDTO() {}

    public String getPinContingencia() { return pinContingencia; }
    public void setPinContingencia(String pinContingencia) { this.pinContingencia = pinContingencia; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getTipoAccion() { return tipoAccion; }
    public void setTipoAccion(String tipoAccion) { this.tipoAccion = tipoAccion; }

    public String getHoraManual() { return horaManual; }
    public void setHoraManual(String horaManual) { this.horaManual = horaManual; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
```

---

### PASO 2: Agregar el método en `AsistenciaService.java`
Ruta: `backend-springboot/src/main/java/com/salgueros/asistencia/service/AsistenciaService.java`

```java
@Value("${salgueros.pin.contingencia:2586}")
private String pinContingenciaMaestro;

@Transactional
public Map<String, Object> aplicarContingencia(ContingenciaRequestDTO req) {
    if (req.getPinContingencia() == null || !req.getPinContingencia().trim().equals(pinContingenciaMaestro)) {
        throw new SecurityException("⛔ PIN de contingencia inválido o no autorizado.");
    }

    LocalDate fechaHoy = LocalDate.now();

    Empleado emp = empleadoRepository.findById(req.getIdEmpleado().intValue())
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con ID: " + req.getIdEmpleado()));

    ProgramacionSemanal prog = programacionRepository.findByEmpleadoAndFecha(emp, fechaHoy)
            .orElseGet(() -> {
                ProgramacionSemanal p = ProgramacionSemanal.builder()
                        .empleado(emp)
                        .fecha(fechaHoy)
                        .horaEntradaProgramada(LocalTime.of(6, 0))
                        .horaSalidaProgramada(LocalTime.of(14, 30))
                        .minutosAlmuerzoProgramados(30)
                        .horasExtrasAutorizadas(false)
                        .tipoNovedadPrecargada(TipoNovedad.NINGUNA)
                        .bloqueado(false)
                        .fechaModificacion(LocalDateTime.now())
                        .build();
                return programacionRepository.save(p);
            });

    RegistroAsistencia reg = registroRepository.findByProgramacion(prog)
            .orElseGet(() -> {
                RegistroAsistencia r = RegistroAsistencia.builder()
                        .programacion(prog)
                        .estadoDia(EstadoDia.INCOMPLETO)
                        .estadoLlegada(EstadoLlegada.A_TIEMPO)
                        .minutosRetardo(0)
                        .horasExtrasCalculadas(0)
                        .build();
                return r;
            });

    LocalTime horaEntradaProg = prog.getHoraEntradaProgramada();
    LocalTime horaSalidaProg = prog.getHoraSalidaProgramada() != null 
            ? prog.getHoraSalidaProgramada() 
            : horaEntradaProg.plusHours(8);

    if ("ENTRADA".equalsIgnoreCase(req.getTipoAccion()) || "JORNADA_COMPLETA".equalsIgnoreCase(req.getTipoAccion())) {
        LocalTime horaEntradaFinal = req.getHoraManual() != null && !req.getHoraManual().isBlank()
                ? LocalTime.parse(req.getHoraManual()) : horaEntradaProg;
        reg.setHoraEntradaReal(LocalDateTime.of(fechaHoy, horaEntradaFinal));
        reg.setEstadoLlegada(EstadoLlegada.A_TIEMPO);
        reg.setMinutosRetardo(0);
    }

    if ("SALIDA".equalsIgnoreCase(req.getTipoAccion()) || "JORNADA_COMPLETA".equalsIgnoreCase(req.getTipoAccion())) {
        LocalTime horaSalidaFinal = req.getHoraManual() != null && !req.getHoraManual().isBlank()
                ? LocalTime.parse(req.getHoraManual()) : horaSalidaProg;
        reg.setHoraSalidaReal(LocalDateTime.of(fechaHoy, horaSalidaFinal));
        reg.setEstadoDia(EstadoDia.COMPLETADO);
    }

    registroRepository.save(reg);

    return Map.of(
        "exito", true,
        "mensaje", "✅ Contingencia aplicada con éxito para " + emp.getNombreApellido() + " en fecha " + fechaHoy
    );
}
```

---

### PASO 3: Exponer el Endpoint en `AsistenciaController.java`
Ruta: `backend-springboot/src/main/java/com/salgueros/asistencia/controller/AsistenciaController.java`

```java
@PostMapping("/contingencia/aplicar")
public ResponseEntity<?> aplicarContingencia(@RequestBody ContingenciaRequestDTO request) {
    try {
        Map<String, Object> resp = asistenciaService.aplicarContingencia(request);
        return ResponseEntity.ok(resp);
    } catch (SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("exito", false, "mensaje", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", e.getMessage()));
    }
}
```

---

### PASO 4: En el Frontend (`poc-camara/index.html` y `docs/index.html`)

1. **Variables Reactivas en Vue (`setup`):**
```javascript
const mostrarModalContingencia = ref(false);
const formContingencia = reactive({
  pin: '',
  idEmpleado: '',
  tipoAccion: 'JORNADA_COMPLETA',
  horaManual: '',
  observacion: 'Corte de luz / red reportado por correo'
});
const procesandoContingencia = ref(false);
const mensajeContingencia = ref(null);
```

2. **Función de Envío:**
```javascript
const enviarContingencia = async () => {
  if (formContingencia.pin !== '2586') {
    mensajeContingencia.value = { exito: false, texto: '⛔ PIN de contingencia inválido.' };
    return;
  }
  if (!formContingencia.idEmpleado) {
    mensajeContingencia.value = { exito: false, texto: '⚠️ Selecciona el empleado a justificar.' };
    return;
  }

  procesandoContingencia.value = true;
  mensajeContingencia.value = null;

  try {
    const resp = await fetch(`${apiBaseUrl}/api/asistencia/contingencia/aplicar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pinContingencia: formContingencia.pin,
        idEmpleado: Number(formContingencia.idEmpleado),
        tipoAccion: formContingencia.tipoAccion,
        horaManual: formContingencia.horaManual || null,
        observacion: formContingencia.observacion
      })
    });
    const data = await resp.json();
    if (resp.ok && data.exito) {
      mensajeContingencia.value = { exito: true, texto: data.mensaje };
      formContingencia.pin = '';
      await consultarAsistencias();
    } else {
      mensajeContingencia.value = { exito: false, texto: data.mensaje || 'Error al aplicar contingencia' };
    }
  } catch (err) {
    mensajeContingencia.value = { exito: false, texto: 'Error de red: ' + err.message };
  } finally {
    procesandoContingencia.value = false;
  }
};
```

3. **HTML del Botón y el Modal:**
Se ubica dentro del Panel Administrativo de `poc-camara/index.html` (para que al pulsar el botón se abra la ventana emergente con el PIN 2586).
