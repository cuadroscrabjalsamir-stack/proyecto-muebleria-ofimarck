package com.ofimarck.domain;

import java.math.BigDecimal;

public class CarritoItem {

    private Integer idProducto;
    private String sku;
    private String nombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private String urlImagen;

    public CarritoItem() {
    }

    public CarritoItem(Integer idProducto, String sku, String nombre, Integer cantidad, BigDecimal precioUnitario, String urlImagen) {
        this.idProducto = idProducto;
        this.sku = sku;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.urlImagen = urlImagen;
        recalcularSubtotal();
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        recalcularSubtotal();
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(new BigDecimal(cantidad));
    }

    public void recalcularSubtotal() {
        // Subtotal is computed on the fly in getSubtotal()
    }
}