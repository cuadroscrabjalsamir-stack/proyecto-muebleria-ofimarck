package com.ofimarck.service;

import com.ofimarck.domain.Pedido;
import com.ofimarck.dto.request.PedidoRequest;
import com.ofimarck.dto.response.PedidoDetalleDTO;
import com.ofimarck.dto.response.PedidoResponse;
import java.util.List;

/**
 * Contrato de la capa de negocio para la creación de pedidos desde el carrito de compras (módulo cliente).
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta interfaz agrupa
 * exclusivamente operaciones de creación y consulta de pedidos realizados por
 * clientes autenticados. La gestión administrativa de pedidos pertenece a
 * {@link IPedidoService}.
 *
 * <p>Principio de Inversión de Dependencias (D de SOLID): los controladores
 * dependen de esta interfaz, permitiendo probar el flujo de checkout con
 * mocks sin tocar la base de datos.
 *
 * <p>Uso de librerías obligatorias (rúbrica):
 * <ul>
 *   <li><b>Google Guava:</b> {@code Preconditions.checkArgument()} para validar
 *       que el carrito no esté vacío antes del checkout; {@code ImmutableList}
 *       para proteger las colecciones de detalles de pedido.</li>
 *   <li><b>Logback/SLF4J:</b> registro de eventos de auditoría en el checkout.</li>
 * </ul>
 */
public interface IPedidoClienteService {

    /**
     * Crea un pedido a partir del carrito de compras del usuario autenticado.
     * Valida stock, crea detalles, pago y envío, y limpia el carrito.
     *
     * @param idUsuario identificador del usuario autenticado
     * @param request datos del pedido (método de pago, referencia, dirección)
     * @param session sesión HTTP que contiene el carrito
     * @return respuesta resumida del pedido creado
     */
    PedidoResponse crearPedido(Integer idUsuario, PedidoRequest request, jakarta.servlet.http.HttpSession session);

    /**
     * Lista los pedidos de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @return lista de resúmenes de pedidos
     */
    List<PedidoResponse> listarPedidos(Integer idUsuario);

    /**
     * Obtiene el resumen de un pedido por su identificador.
     *
     * @param idPedido identificador del pedido
     * @param idUsuario identificador del usuario propietario
     * @return respuesta del pedido
     */
    PedidoResponse obtenerPedido(Long idPedido, Integer idUsuario);

    /**
     * Obtiene el resumen de un pedido por su código.
     *
     * @param código del pedido
     * @param idUsuario identificador del usuario propietario
     * @return respuesta del pedido
     */
    PedidoResponse obtenerPedidoPorCodigo(String codigo, Integer idUsuario);

    /**
     * Obtiene el detalle completo de un pedido (items, pago, envío, dirección).
     *
     * @param idPedido identificador del pedido
     * @param idUsuario identificador del usuario propietario
     * @return detalle del pedido
     */
    PedidoDetalleDTO obtenerPedidoDetalle(Long idPedido, Integer idUsuario);
}
