package com.ofimarck.exception;

/**
 * Excepción de dominio que se lanza cuando el stock disponible de un producto
 * es insuficiente para atender la cantidad solicitada en un detalle de pedido.
 *
 * <p>Esta excepción forma parte de la capa de negocio (Service) y es lanzada
 * por {@link com.ofimarck.service.IPedidoService} antes de persistir un pedido,
 * garantizando que nunca se registre una venta que no pueda ser surtida.
 * El controlador la captura a través de {@link com.ofimarck.exception.GlobalExceptionHandler}
 * y devuelve HTTP 400 al cliente.
 *
 * <p>Diseño didáctico: se separa como clase propia (en lugar de usar
 * {@code IllegalArgumentException}) para que el estudiante pueda identificar
 * claramente la regla de negocio violada durante la sustentación.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }

    public StockInsuficienteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
