package com.ofimarck.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CambioEstadoPedidoDTO {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String nuevoEstado;

    private String comentario;

    public CambioEstadoPedidoDTO() {
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
