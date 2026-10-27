package com.ofimarck.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categoria", schema = "muebleria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "slug", nullable = false, unique = true, length = 90)
    private String slug;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    public Categoria() {
    }

    public Categoria(Integer idCategoria, String nombre, String slug, String descripcion, Boolean estado) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
        this.slug = slug;
        this.descripcion = descripcion;
        this.estado = estado;
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
