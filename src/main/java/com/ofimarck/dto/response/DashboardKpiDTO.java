package com.ofimarck.dto.response;

import java.math.BigDecimal;

public class DashboardKpiDTO {

    private BigDecimal totalVentasMes;
    private long pedidosPendientes;
    private long productosBajoStock;
    private long totalProductos;

    public DashboardKpiDTO() {
    }

    public DashboardKpiDTO(BigDecimal totalVentasMes, long pedidosPendientes, long productosBajoStock,
            long totalProductos) {
        this.totalVentasMes = totalVentasMes;
        this.pedidosPendientes = pedidosPendientes;
        this.productosBajoStock = productosBajoStock;
        this.totalProductos = totalProductos;
    }

    public BigDecimal getTotalVentasMes() {
        return totalVentasMes;
    }

    public void setTotalVentasMes(BigDecimal totalVentasMes) {
        this.totalVentasMes = totalVentasMes;
    }

    public long getPedidosPendientes() {
        return pedidosPendientes;
    }

    public void setPedidosPendientes(long pedidosPendientes) {
        this.pedidosPendientes = pedidosPendientes;
    }

    public long getProductosBajoStock() {
        return productosBajoStock;
    }

    public void setProductosBajoStock(long productosBajoStock) {
        this.productosBajoStock = productosBajoStock;
    }

    public long getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(long totalProductos) {
        this.totalProductos = totalProductos;
    }
}
