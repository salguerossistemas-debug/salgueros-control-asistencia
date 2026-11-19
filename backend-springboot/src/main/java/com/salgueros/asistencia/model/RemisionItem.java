package com.salgueros.asistencia.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "remisiones_items")
public class RemisionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_remision", nullable = false)
    @JsonIgnore
    private Remision remision;

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombreProducto;

    @Column(name = "cantidad", nullable = false, length = 50)
    private String cantidad;

    @Column(name = "unidad_medida", length = 50)
    private String unidadMedida; // ej: "Cajas", "Paquetes", "Unidades", "Bolsas"

    @Column(name = "recibido_ok")
    private Boolean recibidoOk;

    @Column(name = "es_adicional", nullable = false)
    private Boolean esAdicional = false;

    @Column(name = "orden")
    private Integer orden;

    public RemisionItem() {
    }

    public RemisionItem(String nombreProducto, String cantidad, String unidadMedida, Boolean esAdicional, Integer orden) {
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.unidadMedida = unidadMedida;
        this.esAdicional = esAdicional != null ? esAdicional : false;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Remision getRemision() {
        return remision;
    }

    public void setRemision(Remision remision) {
        this.remision = remision;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public Boolean getRecibidoOk() {
        return recibidoOk;
    }

    public void setRecibidoOk(Boolean recibidoOk) {
        this.recibidoOk = recibidoOk;
    }

    public Boolean getEsAdicional() {
        return esAdicional;
    }

    public void setEsAdicional(Boolean esAdicional) {
        this.esAdicional = esAdicional;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}