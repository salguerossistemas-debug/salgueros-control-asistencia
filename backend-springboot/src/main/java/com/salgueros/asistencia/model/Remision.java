package com.salgueros.asistencia.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "remisiones", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tipo_numero", columnNames = {"tipo", "numero_remision"})
})
public class Remision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_remision")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoRemision tipo = TipoRemision.INSUMOS;

    @Column(name = "numero_remision", nullable = false)
    private Integer numeroRemision;

    @Column(name = "punto_venta", nullable = false, length = 100)
    private String puntoVenta;

    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad = "BOGOTÁ D.C.";

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "despachado_por", nullable = false, length = 100)
    private String despachadoPor;

    @Column(name = "recibido_por", length = 100)
    private String recibidoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoRemision estado = EstadoRemision.PENDIENTE;

    @Column(name = "novedades", length = 1000)
    private String novedades;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @OneToMany(mappedBy = "remision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<RemisionItem> items = new ArrayList<>();

    public Remision() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public void addItem(RemisionItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setRemision(this);
    }

    // Getters y Setters explícitos
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoRemision getTipo() {
        return tipo;
    }

    public void setTipo(TipoRemision tipo) {
        this.tipo = tipo;
    }

    public Integer getNumeroRemision() {
        return numeroRemision;
    }

    public void setNumeroRemision(Integer numeroRemision) {
        this.numeroRemision = numeroRemision;
    }

    public String getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(String puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDespachadoPor() {
        return despachadoPor;
    }

    public void setDespachadoPor(String despachadoPor) {
        this.despachadoPor = despachadoPor;
    }

    public String getRecibidoPor() {
        return recibidoPor;
    }

    public void setRecibidoPor(String recibidoPor) {
        this.recibidoPor = recibidoPor;
    }

    public EstadoRemision getEstado() {
        return estado;
    }

    public void setEstado(EstadoRemision estado) {
        this.estado = estado;
    }

    public String getNovedades() {
        return novedades;
    }

    public void setNovedades(String novedades) {
        this.novedades = novedades;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public List<RemisionItem> getItems() {
        return items;
    }

    public void setItems(List<RemisionItem> items) {
        this.items = items;
        if (items != null) {
            for (RemisionItem item : items) {
                item.setRemision(this);
            }
        }
    }
}