package com.ofimarck.dto.response;

import jakarta.validation.constraints.NotBlank;

public class ValidacionPagoDTO {

    @NotBlank(message = "El estado del pago es obligatorio (APROBADO o RECHAZADO)")
    private String estado;

    private String referencia;
    private String comentario;

    public ValidacionPagoDTO() {
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
