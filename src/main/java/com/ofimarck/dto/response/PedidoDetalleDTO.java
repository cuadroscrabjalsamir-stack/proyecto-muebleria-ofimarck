package com.ofimarck.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class PedidoDetalleDTO {

    private Long idPedido;
    private String codigo;
    private OffsetDateTime fechaPedido;
    private BigDecimal subtotal;
    private BigDecimal costoEnvio;
    private BigDecimal total;
    private String estado;
    private String observaciones;

    private DireccionDTO direccion;
    private PagoDTO pago;
    private EnvioDTO envio;
    private List<ItemPedidoDTO> items;

    public PedidoDetalleDTO() {
    }

    public Long getIdPedido() { return idPedido; }
    public void setIdPedido(Long idPedido) { this.idPedido = idPedido; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public OffsetDateTime getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(OffsetDateTime fechaPedido) { this.fechaPedido = fechaPedido; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(BigDecimal costoEnvio) { this.costoEnvio = costoEnvio; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public DireccionDTO getDireccion() { return direccion; }
    public void setDireccion(DireccionDTO direccion) { this.direccion = direccion; }
    public PagoDTO getPago() { return pago; }
    public void setPago(PagoDTO pago) { this.pago = pago; }
    public EnvioDTO getEnvio() { return envio; }
    public void setEnvio(EnvioDTO envio) { this.envio = envio; }
    public List<ItemPedidoDTO> getItems() { return items; }
    public void setItems(List<ItemPedidoDTO> items) { this.items = items; }

    public static class DireccionDTO {
        private String destinatario;
        private String telefono;
        private String departamento;
        private String provincia;
        private String distrito;
        private String lineaDireccion;
        private String referencia;

        public DireccionDTO() {}
        public String getDestinatario() { return destinatario; }
        public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
        public String getDepartamento() { return departamento; }
        public void setDepartamento(String departamento) { this.departamento = departamento; }
        public String getProvincia() { return provincia; }
        public void setProvincia(String provincia) { this.provincia = provincia; }
        public String getDistrito() { return distrito; }
        public void setDistrito(String distrito) { this.distrito = distrito; }
        public String getLineaDireccion() { return lineaDireccion; }
        public void setLineaDireccion(String lineaDireccion) { this.lineaDireccion = lineaDireccion; }
        public String getReferencia() { return referencia; }
        public void setReferencia(String referencia) { this.referencia = referencia; }
    }

    public static class PagoDTO {
        private String metodo;
        private String estado;
        private String referencia;
        private BigDecimal monto;

        public PagoDTO() {}
        public String getMetodo() { return metodo; }
        public void setMetodo(String metodo) { this.metodo = metodo; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getReferencia() { return referencia; }
        public void setReferencia(String referencia) { this.referencia = referencia; }
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
    }

    public static class EnvioDTO {
        private String transportista;
        private String tracking;
        private String estado;

        public EnvioDTO() {}
        public String getTransportista() { return transportista; }
        public void setTransportista(String transportista) { this.transportista = transportista; }
        public String getTracking() { return tracking; }
        public void setTracking(String tracking) { this.tracking = tracking; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }

    public static class ItemPedidoDTO {
        private String nombreProducto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;

        public ItemPedidoDTO() {}
        public String getNombreProducto() { return nombreProducto; }
        public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}
