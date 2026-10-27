package com.ofimarck.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AjusteInventarioDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    private Integer idProducto;

    @NotBlank(message = "El tipo de movimiento es obligatorio (ENTRADA o AJUSTE)")
    private String tipo;

    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;

    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;

    public AjusteInventarioDTO() {
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
