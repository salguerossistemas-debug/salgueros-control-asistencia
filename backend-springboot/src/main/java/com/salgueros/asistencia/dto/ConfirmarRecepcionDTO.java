package com.salgueros.asistencia.dto;

import java.util.List;

public class ConfirmarRecepcionDTO {

    private String recibidoPor;
    private String novedades;
    private List<ItemCheckDTO> checks;

    public ConfirmarRecepcionDTO() {}

    public static class ItemCheckDTO {
        private Long idItem;
        private Boolean recibidoOk;

        public ItemCheckDTO() {}

        public Long getIdItem() { return idItem; }
        public void setIdItem(Long idItem) { this.idItem = idItem; }

        public Boolean getRecibidoOk() { return recibidoOk; }
        public void setRecibidoOk(Boolean recibidoOk) { this.recibidoOk = recibidoOk; }
    }

    public String getRecibidoPor() { return recibidoPor; }
    public void setRecibidoPor(String recibidoPor) { this.recibidoPor = recibidoPor; }

    public String getNovedades() { return novedades; }
    public void setNovedades(String novedades) { this.novedades = novedades; }

    public List<ItemCheckDTO> getChecks() { return checks; }
    public void setChecks(List<ItemCheckDTO> checks) { this.checks = checks; }
}