package com.ofimarck.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "password_reset_token", schema = "muebleria")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expiracion", nullable = false)
    private OffsetDateTime expiracion;

    @Column(name = "usado", nullable = false)
    private Boolean usado = false;

    public PasswordResetToken() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public OffsetDateTime getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(OffsetDateTime expiracion) {
        this.expiracion = expiracion;
    }

    public Boolean getUsado() {
        return usado;
    }

    public void setUsado(Boolean usado) {
        this.usado = usado;
    }

    public boolean isExpired() {
        return expiracion != null && expiracion.isBefore(OffsetDateTime.now());
    }

    public boolean isValid() {
        return !usado && !isExpired();
    }
}
