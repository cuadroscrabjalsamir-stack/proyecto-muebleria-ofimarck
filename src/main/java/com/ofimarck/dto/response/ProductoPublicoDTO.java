package com.ofimarck.dto.response;

import com.ofimarck.domain.Categoria;
import com.ofimarck.domain.Producto;
import java.math.BigDecimal;

public class ProductoPublicoDTO {

    private Integer idProducto;
    private String sku;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private Boolean estado;
    private String categoriaNombre;
    private String urlImagen;

    public ProductoPublicoDTO() {
    }

    public ProductoPublicoDTO(Producto producto) {
        this.idProducto = producto.getIdProducto();
        this.sku = producto.getSku();
        this.nombre = producto.getNombre();
        this.descripcion = producto.getDescripcion();
        this.precio = producto.getPrecio();
        this.stock = producto.getStock();
        this.estado = producto.getEstado();
        this.urlImagen = producto.getUrlImagen();
        if (producto.getCategoria() != null) {
            this.categoriaNombre = producto.getCategoria().getNombre();
        }
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }
}