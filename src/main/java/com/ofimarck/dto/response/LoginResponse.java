package com.ofimarck.dto.response;

import com.ofimarck.domain.Usuario;

public class LoginResponse {

    private boolean authenticated;
    private String mensaje;
    private Usuario usuario;

    public LoginResponse() {
    }

    public LoginResponse(boolean authenticated, String mensaje, Usuario usuario) {
        this.authenticated = authenticated;
        this.mensaje = mensaje;
        this.usuario = usuario;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
