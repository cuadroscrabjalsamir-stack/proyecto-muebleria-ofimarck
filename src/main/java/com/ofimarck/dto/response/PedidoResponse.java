package com.ofimarck.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PedidoResponse {

    private Long idPedido;
    private String codigo;
    private BigDecimal total;
    private String estado;
    private OffsetDateTime fechaPedido;

    public PedidoResponse() {
    }

    public PedidoResponse(Long idPedido, String codigo, BigDecimal total, String estado, OffsetDateTime fechaPedido) {
        this.idPedido = idPedido;
        this.codigo = codigo;
        this.total = total;
        this.estado = estado;
        this.fechaPedido = fechaPedido;
    }

    public Long getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Long idPedido) {
        this.idPedido = idPedido;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(OffsetDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }
}