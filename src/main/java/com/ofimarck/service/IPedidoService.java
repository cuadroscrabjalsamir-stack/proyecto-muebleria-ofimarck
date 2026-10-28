package com.ofimarck.service;

import com.ofimarck.domain.Pedido;
import com.ofimarck.dto.request.CambioEstadoPedidoDTO;
import com.ofimarck.dto.request.EnvioUpdateDTO;
import com.ofimarck.dto.response.ValidacionPagoDTO;
import java.util.List;

/**
 * Contrato de la capa de negocio para la gestión de pedidos (módulo administrativo).
 *
 * <p>Principio de Inversión de Dependencias (D de SOLID): los controladores
 * dependen de esta interfaz y no de una implementación concreta. Esto permite
 * sustituir la implementación (por ejemplo, en pruebas unitarias con Mockito)
 * sin modificar el controlador.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta interfaz agrupa
 * exclusivamente operaciones de consulta, cambio de estado, actualización de
 * envío y validación de pago de pedidos. La creación de pedidos desde el
 * carrito de compras es responsabilidad de {@link IPedidoClienteService}.
 *
 * <p>Uso de librerías obligatorias (rúbrica):
 * <ul>
 *   <li><b>Google Guava:</b> {@code Preconditions.checkArgument()} para validar
 *       precondiciones de negocio antes de persistir.</li>
 *   <li><b>Logback/SLF4J:</b> registro de eventos de auditoría y seguridad
 *       mediante {@code LoggerFactory} en la implementación.</li>
 * </ul>
 */
public interface IPedidoService {

    /**
     * Lista pedidos aplicando filtros opcionales de estado y cliente.
     *
     * @param estado filtro de estado (puede ser {@code null})
     * @param clienteId identificador de usuario cliente (puede ser {@code null})
     * @return lista de pedidos ordenada por fecha descendente
     */
    List<Pedido> listar(String estado, Integer clienteId);

    /**
     * Busca un pedido por su identificador interno.
     *
     * @param id identificador del pedido
     * @return el pedido encontrado
     * @throws com.ofimarck.exception.ResourceNotFoundException si no existe
     */
    Pedido buscarPorId(Long id);

    /**
     * Busca un pedido por su código generado (formato ORD-YYYYMMDD-XXXX).
     *
     * @param código del pedido
     * @return el pedido encontrado
     * @throws com.ofimarck.exception.ResourceNotFoundException si no existe
     */
    Pedido buscarPorCodigo(String codigo);

    /**
     * Cambia el estado de un pedido y registra la transición en el historial.
     *
     * @param idPedido identificador del pedido
     * @param dto datos del nuevo estado
     * @param idUsuario identificador del usuario que ejecuta el cambio
     * @return el pedido actualizado
     */
    Pedido cambiarEstado(Long idPedido, CambioEstadoPedidoDTO dto, Integer idUsuario);

    /**
     * Actualiza los datos de envío de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param dto datos de envío a actualizar
     * @return el objeto envío actualizado
     */
    com.ofimarck.domain.Envio actualizarEnvio(Long idPedido, EnvioUpdateDTO dto);

    /**
     * Valida (aprueba o rechaza) el pago de un pedido. Cuando se aprueba,
     * el pedido pasa a estado PAGADO.
     *
     * @param idPedido identificador del pedido
     * @param dto resultado de la validación de pago
     * @param idUsuario identificador del usuario administrador
     * @return el pago actualizado
     */
    com.ofimarck.domain.Pago validarPago(Long idPedido, ValidacionPagoDTO dto, Integer idUsuario);

    /**
     * Obtiene el historial de cambios de estado de un pedido.
     *
     * @param idPedido identificador del pedido
     * @return lista de entradas de historial ordenadas por fecha descendente
     */
    java.util.List<com.ofimarck.domain.HistorialEstadoPedido> obtenerHistorial(Long idPedido);
}
