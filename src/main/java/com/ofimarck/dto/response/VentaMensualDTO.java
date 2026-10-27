package com.ofimarck.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VentaMensualDTO {

    private LocalDate mes;
    private long pedidos;
    private BigDecimal ventas;
    private BigDecimal ticketPromedio;

    public VentaMensualDTO() {
    }

    public VentaMensualDTO(LocalDate mes, long pedidos, BigDecimal ventas, BigDecimal ticketPromedio) {
        this.mes = mes;
        this.pedidos = pedidos;
        this.ventas = ventas;
        this.ticketPromedio = ticketPromedio;
    }

    public LocalDate getMes() {
        return mes;
    }

    public void setMes(LocalDate mes) {
        this.mes = mes;
    }

    public long getPedidos() {
        return pedidos;
    }

    public void setPedidos(long pedidos) {
        this.pedidos = pedidos;
    }

    public BigDecimal getVentas() {
        return ventas;
    }

    public void setVentas(BigDecimal ventas) {
        this.ventas = ventas;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }
}
