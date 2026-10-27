package com.ofimarck.dto.response;

import java.math.BigDecimal;

public class InventarioAlertaDTO {

    private Integer idProducto;
    private String sku;
    private String nombre;
    private String categoria;
    private Integer stock;
    private Integer stockMinimo;
    private String estadoStock;
    private BigDecimal valorInventario;

    public InventarioAlertaDTO() {
    }

    public InventarioAlertaDTO(Integer idProducto, String sku, String nombre, String categoria, Integer stock,
            Integer stockMinimo, String estadoStock, BigDecimal valorInventario) {
        this.idProducto = idProducto;
        this.sku = sku;
        this.nombre = nombre;
        this.categoria = categoria;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.estadoStock = estadoStock;
        this.valorInventario = valorInventario;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getEstadoStock() {
        return estadoStock;
    }

    public void setEstadoStock(String estadoStock) {
        this.estadoStock = estadoStock;
    }

    public BigDecimal getValorInventario() {
        return valorInventario;
    }

    public void setValorInventario(BigDecimal valorInventario) {
        this.valorInventario = valorInventario;
    }
}
