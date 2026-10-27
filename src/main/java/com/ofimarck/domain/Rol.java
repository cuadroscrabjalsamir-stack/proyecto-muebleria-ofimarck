package com.ofimarck.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "rol", schema = "muebleria")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Short idRol;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 30)
    private String nombreRol;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    public Rol() {
    }

    public Rol(Short idRol, String nombreRol, String descripcion, Boolean estado) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Short getIdRol() {
        return idRol;
    }

    public void setIdRol(Short idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
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
