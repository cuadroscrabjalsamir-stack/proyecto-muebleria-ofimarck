package com.ofimarck.dto.response;

import java.math.BigDecimal;

public class TopProductoDTO {

    private Integer idProducto;
    private String nombre;
    private String categoria;
    private Integer unidadesVendidas;
    private BigDecimal ingresos;

    public TopProductoDTO() {
    }

    public TopProductoDTO(Integer idProducto, String nombre, String categoria, Integer unidadesVendidas,
            BigDecimal ingresos) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidadesVendidas = unidadesVendidas;
        this.ingresos = ingresos;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
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

    public Integer getUnidadesVendidas() {
        return unidadesVendidas;
    }

    public void setUnidadesVendidas(Integer unidadesVendidas) {
        this.unidadesVendidas = unidadesVendidas;
    }

    public BigDecimal getIngresos() {
        return ingresos;
    }

    public void setIngresos(BigDecimal ingresos) {
        this.ingresos = ingresos;
    }
}
