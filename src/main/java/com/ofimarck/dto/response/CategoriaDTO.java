package com.ofimarck.dto.response;

import jakarta.validation.constraints.NotBlank;

public class CategoriaDTO {

    
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;
    private Integer idCategoria;
    private String slug;
    private String descripcion;
    private Boolean estado = true;

    public CategoriaDTO() {
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}
