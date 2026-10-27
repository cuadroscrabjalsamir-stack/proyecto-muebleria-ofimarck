package com.ofimarck.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PagoRequest {

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodo;

    @NotBlank(message = "La referencia es obligatoria")
    private String referencia;

    public PagoRequest() {
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}