package com.ofimarck.dto.request;

import java.time.LocalDate;

public class EnvioUpdateDTO {

    private String transportista;
    private String tracking;
    private String estado;
    private LocalDate fechaEstimada;

    public EnvioUpdateDTO() {
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public String getTracking() {
        return tracking;
    }

    public void setTracking(String tracking) {
        this.tracking = tracking;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaEstimada() {
        return fechaEstimada;
    }

    public void setFechaEstimada(LocalDate fechaEstimada) {
        this.fechaEstimada = fechaEstimada;
    }
}
