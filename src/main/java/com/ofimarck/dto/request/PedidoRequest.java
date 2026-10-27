package com.ofimarck.dto.request;

import com.ofimarck.domain.DireccionEnvio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PedidoRequest {

    @NotNull(message = "La dirección de envío es obligatoria")
    private Integer idDireccionEnvio;

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;

    @NotBlank(message = "La referencia es obligatoria")
    private String referencia;

    public PedidoRequest() {
    }

    public Integer getIdDireccionEnvio() {
        return idDireccionEnvio;
    }

    public void setIdDireccionEnvio(Integer idDireccionEnvio) {
        this.idDireccionEnvio = idDireccionEnvio;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}