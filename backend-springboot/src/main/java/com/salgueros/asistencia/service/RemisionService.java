package com.salgueros.asistencia.service;
import com.salgueros.asistencia.dto.ConfirmarRecepcionDTO;
import com.salgueros.asistencia.dto.CrearRemisionDTO;
import com.salgueros.asistencia.model.EstadoRemision;
import com.salgueros.asistencia.model.Remision;
import com.salgueros.asistencia.model.RemisionItem;
import com.salgueros.asistencia.model.TipoRemision;
import com.salgueros.asistencia.repository.RemisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class RemisionService {
    private static final Logger log = LoggerFactory.getLogger(RemisionService.class);
    private static final int PRIMER_CONSECUTIVO = 1;
    // Catálogo 1: Insumos y Aseo (32 preimpresos del talonario físico)
    public static final List<String> PRODUCTOS_INSUMOS = List.of(
        "HUEVO",
  "CHOCOLATE POLVO",
  "AROMATICA SOBRE",
  "AROMATICA PANELA",
  "CAFÉ",
  "PREPARADO LACTEO",
  "LECHE LIQUIDA",
  "BOLSA MEDIANA DE PAPEL",
  "BOLSA GRANDE DE PAPEL",
  "SERVILLETAS",
  "TOALLAS BLANCAS",
  "TOALLAS DE MANO",
  "TAPABOCAS",
  "GUANTE DE MANIPULACION",
  "GUANTE DE ASEO",
  "PAPEL HORNO",
  "BOLSA BASURA NEGRA",
  "BOLSA BASURA BLANCA",
  "BOLSA BASURA VERDE",
  "MEZCLADORES DE 9 ONZ",
  "MEZCLADORES DE 12 ONZ",
  "VASOS DE 9 ONZ",
  "VASOS DE 12 ONZ",
  "TAPA VASOS DE 9 ONZ",
  "TAPA VASOS DE 12 ONZ",
  "DESENGRASANTE",
  "CLOROX",
  "GEL",
  "JABON DE MANOS",
  "PANELITA",
  "ESPONJA ALAMBRE",
  "BOLSA ANTIGRASO",
  "AZUCAR SOBRE",
  "AZUCAR",
  "AISLANTE DE CALOR",
  "AZUCAR LIGTH",
  "COFIAS",
  "ROLLO COMPUTADOR",
  "SABRAS",
  "TRAPERO",
  "ESCOBA",
  "PALO DE ESCOBA",
  "ESPONJA BRILLO",
  "RECOGEDOR",
  "TE CHAI",
  "BOLSA PAPEL EFECTIVO",
  "AZUCAR CANELA",
  "AZUCAR PULVERIZADA",
  "PITILLOS",
  "CUADERNO",
  "LAPIZ",
  "ESFERO",
  "BORRADOR",
  "TAJALAPIZ",
  "GANCHO COSEDORA",
  "CAJAS DE PEDIDO AMARILLA PEQUEÑA",
  "CAJAS DE PEDIDO AMARILLA MEDIANA",
  "CAJAS DE PEDIDO BLANCAS PEQUEÑAS",
  "CAJAS DE PEDIDO BLANCAS MEDIANA"
    );

    // Catálogo 2: Comida y Alimentos (Base editable / ampliable)
    public static final List<String> PRODUCTOS_COMIDA = List.of(
  "PASABOCAS DE POLLO",
  "PASABOCAS DE CARNE",
  "PASABOCAS DE QUESO",
  "PASABOCAS DE BOCADILLO",
  "PASABOCAS DE AREQUIPE CHOCOLATE",
  "PASABOCAS DE HAWAIANO",
  "PASTEL DE POLLO",
  "PASTEL DE CARNE",
  "PASTEL DE QUESO",
  "PASTEL BOCAQUESO",
  "PASTEL HAWAIANO",
  "PASTEL DE PIÑA CANELA",
  "PASTEL AREQUIPE/QUESO",
  "PASTEL CHOCOLATE",
  "PASTEL VAINILLA",
  "PASTEL BOCADILLO",
  "CORAZONES NATURAL * 6",
  "CORAZONES CANELA *6",
  "CORAZONES BOCADILLO *6",
  "CORAZON GRANDE",
  "GALLETA #3",
  "GALLETA CHOCOLATE",
  "GALLETA RED VELUET",
  "GALLETA VAINILLA",
  "TORTA VAINILLA",
  "TORTA CHOCOLATE",
  "KUMIS",
  "YOGURT FRESA",
  "YOGURT MELOCOTON",
  "COLOMBIANA",
  "AGUA",
  "ACQUA",
  "JUGO TROPICAL",
  "JUGO MANGO",
  "MR TE LIMON",
  "HATSU"
);
    private final RemisionRepository remisionRepository;
    public RemisionService(RemisionRepository remisionRepository) {
        this.remisionRepository = remisionRepository;
    }
    public List<String> obtenerCatalogoPorTipo(TipoRemision tipo) {
        if (tipo == TipoRemision.COMIDA) {
            return PRODUCTOS_COMIDA;
        }
        return PRODUCTOS_INSUMOS;
    }
    @Transactional(readOnly = true)
    public Integer obtenerSiguienteConsecutivo(TipoRemision tipo) {
        TipoRemision tipoConsultar = tipo != null ? tipo : TipoRemision.INSUMOS;
        return remisionRepository.findMaxNumeroRemisionPorTipo(tipoConsultar)
                .map(max -> max + 1)
                .orElse(PRIMER_CONSECUTIVO);
    }
    @Transactional
    public Remision crearRemision(CrearRemisionDTO dto) {
        if (dto.getPuntoVenta() == null || dto.getPuntoVenta().trim().isEmpty()) {
            throw new IllegalArgumentException("El punto de venta es obligatorio");
        }
        if (dto.getDespachadoPor() == null || dto.getDespachadoPor().trim().isEmpty()) {
            throw new IllegalArgumentException("El responsable de despacho es obligatorio");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un producto a despachar");
        }
        TipoRemision tipo = dto.getTipo() != null ? dto.getTipo() : TipoRemision.INSUMOS;
        Remision remision = new Remision();
        remision.setTipo(tipo);
        remision.setNumeroRemision(obtenerSiguienteConsecutivo(tipo));
        remision.setPuntoVenta(dto.getPuntoVenta().trim());
        remision.setCiudad(dto.getCiudad() != null ? dto.getCiudad().trim() : "BOGOTÁ D.C.");
        remision.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        remision.setDespachadoPor(dto.getDespachadoPor().trim());
        remision.setEstado(EstadoRemision.PENDIENTE);
        remision.setFechaCreacion(LocalDateTime.now());
        int ordenIndex = 1;
        for (CrearRemisionDTO.ItemDTO itemDTO : dto.getItems()) {
            if (itemDTO.getCantidad() != null && !itemDTO.getCantidad().trim().isEmpty()) {
                RemisionItem item = new RemisionItem();
                item.setNombreProducto(itemDTO.getNombreProducto().trim());
                item.setCantidad(itemDTO.getCantidad().trim());
                item.setUnidadMedida(itemDTO.getUnidadMedida() != null ? itemDTO.getUnidadMedida().trim() : null);
                item.setEsAdicional(Boolean.TRUE.equals(itemDTO.getEsAdicional()));
                item.setOrden(itemDTO.getOrden() != null ? itemDTO.getOrden() : ordenIndex++);
                remision.addItem(item);
            }
        }
        if (remision.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar la cantidad de al menos un producto");
        }
        Remision guardada = remisionRepository.save(remision);
        log.info("Remisión [{}] #{} creada exitosamente para sede '{}'", guardada.getTipo(), guardada.getNumeroRemision(), guardada.getPuntoVenta());
        return guardada;
    }
    @Transactional
    public Remision confirmarRecepcion(Long idRemision, ConfirmarRecepcionDTO dto) {
        Remision remision = remisionRepository.findById(idRemision)
                .orElseThrow(() -> new IllegalArgumentException("Remisión no encontrada con ID: " + idRemision));
        if (remision.getEstado() == EstadoRemision.RECIBIDA) {
            throw new IllegalStateException("Esta remisión ya fue confirmada previamente");
        }
        if (dto.getRecibidoPor() == null || dto.getRecibidoPor().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el nombre de la persona que recibe");
        }
        Map<Long, Boolean> checksMap = new HashMap<>();
        if (dto.getChecks() != null) {
            for (ConfirmarRecepcionDTO.ItemCheckDTO chk : dto.getChecks()) {
                if (chk.getIdItem() != null) {
                    checksMap.put(chk.getIdItem(), chk.getRecibidoOk());
                }
            }
        }
        for (RemisionItem item : remision.getItems()) {
            if (checksMap.containsKey(item.getId())) {
                item.setRecibidoOk(checksMap.get(item.getId()));
            } else {
                item.setRecibidoOk(true);
            }
        }
        remision.setRecibidoPor(dto.getRecibidoPor().trim());
        remision.setNovedades(dto.getNovedades() != null ? dto.getNovedades().trim() : null);
        remision.setEstado(EstadoRemision.RECIBIDA);
        remision.setFechaRecepcion(LocalDateTime.now());
        Remision actualizada = remisionRepository.save(remision);
        log.info("Remisión [{}] #{} confirmada por '{}' en sede '{}'", actualizada.getTipo(), actualizada.getNumeroRemision(), actualizada.getRecibidoPor(), actualizada.getPuntoVenta());
        return actualizada;
    }
    @Transactional(readOnly = true)
    public List<Remision> listarTodas() {
        return remisionRepository.findAll();
    }
    @Transactional(readOnly = true)
    public List<Remision> listarPorTipo(TipoRemision tipo) {
        return remisionRepository.findByTipoOrderByNumeroRemisionDesc(tipo);
    }
    @Transactional(readOnly = true)
    public List<Remision> listarPorSede(String puntoVenta) {
        return remisionRepository.findByPuntoVentaOrderByNumeroRemisionDesc(puntoVenta);
    }
    @Transactional(readOnly = true)
    public List<Remision> listarPendientes(TipoRemision tipo) {
        if (tipo != null) {
            return remisionRepository.findByTipoAndEstadoOrderByNumeroRemisionDesc(tipo, EstadoRemision.PENDIENTE);
        }
        return remisionRepository.findAll().stream()
                .filter(r -> r.getEstado() == EstadoRemision.PENDIENTE)
                .toList();
    }
    @Transactional(readOnly = true)
    public Optional<Remision> obtenerPorId(Long id) {
        return remisionRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public Optional<Remision> obtenerPorTipoYNumero(TipoRemision tipo, Integer numero) {
        return remisionRepository.findByTipoAndNumeroRemision(tipo, numero);
    }
}