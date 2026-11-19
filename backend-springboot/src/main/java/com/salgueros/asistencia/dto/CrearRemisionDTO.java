package com.salgueros.asistencia.dto;

import com.salgueros.asistencia.model.TipoRemision;

import java.time.LocalDate;
import java.util.List;

public class CrearRemisionDTO {

    private TipoRemision tipo = TipoRemision.INSUMOS;
    private String puntoVenta;
    private String ciudad = "BOGOTÁ D.C.";
    private LocalDate fecha;
    private String despachadoPor;
    private List<ItemDTO> items;

    public CrearRemisionDTO() {}

    public static class ItemDTO {
        private String nombreProducto;
        private String cantidad;
        private String unidadMedida;
        private Boolean esAdicional = false;
        private Integer orden;

        public ItemDTO() {}

        public String getNombreProducto() { return nombreProducto; }
        public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

        public String getCantidad() { return cantidad; }
        public void setCantidad(String cantidad) { this.cantidad = cantidad; }

        public String getUnidadMedida() { return unidadMedida; }
        public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

        public Boolean getEsAdicional() { return esAdicional; }
        public void setEsAdicional(Boolean esAdicional) { this.esAdicional = esAdicional; }

        public Integer getOrden() { return orden; }
        public void setOrden(Integer orden) { this.orden = orden; }
    }

    public TipoRemision getTipo() { return tipo; }
    public void setTipo(TipoRemision tipo) { this.tipo = tipo; }

    public String getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(String puntoVenta) { this.puntoVenta = puntoVenta; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDespachadoPor() { return despachadoPor; }
    public void setDespachadoPor(String despachadoPor) { this.despachadoPor = despachadoPor; }

    public List<ItemDTO> getItems() { return items; }
    public void setItems(List<ItemDTO> items) { this.items = items; }
}