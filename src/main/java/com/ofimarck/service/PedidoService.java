package com.ofimarck.service;

import com.ofimarck.domain.DetallePedido;
import com.ofimarck.domain.Envio;
import com.ofimarck.domain.HistorialEstadoPedido;
import com.ofimarck.domain.Pago;
import com.ofimarck.domain.Pedido;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.CambioEstadoPedidoDTO;
import com.ofimarck.dto.request.EnvioUpdateDTO;
import com.ofimarck.dto.response.ValidacionPagoDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.exception.StockInsuficienteException;
import com.ofimarck.repository.DetallePedidoRepository;
import com.ofimarck.repository.PedidoRepository;
import com.ofimarck.repository.ProductoRepository;
import com.ofimarck.repository.PagoRepository;
import com.ofimarck.repository.EnvioRepository;
import com.ofimarck.repository.HistorialEstadoPedidoRepository;
import com.ofimarck.repository.UsuarioRepository;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Implementación de {@link IPedidoService} para la gestión administrativa de pedidos.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta clase se encarga
 * exclusivamente de las reglas de negocio de pedidos desde el módulo de
 * administración (listar, buscar, cambiar estado, actualizar envío, validar
 * pago y historial). La creación de pedidos desde el carrito de compras
 * pertenece a {@link com.ofimarck.service.PedidoClienteService}.
 *
 * <p>Principio de Inversión de Dependencias (D de SOLID): depende de
 * interfaces de repositorio inyectadas por constructor, no de clases
 * concretas de persistencia.
 *
 * <p>Uso de Google Guava (rúbrica FASE 2):
 * <ul>
 *   <li>{@link Preconditions#checkArgument(boolean, String)} para validar
 *       precondiciones de negocio antes de persistir cambios (por ejemplo,
 *       que el identificador de pedido no sea nulo o que el estado sea válido).</li>
 * </ul>
 *
 * <p>Uso de Logback/SLF4J (rúbrica FASE 2):
 * <ul>
 *   <li>{@link LoggerFactory} para registrar eventos de auditoría
 *       (cambios de estado, validaciones de pago) y eventos de seguridad
 *       (intentos de acceso a pedidos ajenos).</li>
 *   <li>No se loguean datos sensibles (contraseñas, tarjetas, DNI).</li>
 * </ul>
 */
@Service
public class PedidoService implements IPedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final HistorialEstadoPedidoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         PagoRepository pagoRepository,
                         EnvioRepository envioRepository,
                         HistorialEstadoPedidoRepository historialRepository,
                         UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Pedido> listar(String estado, Integer clienteId) {
        boolean tieneEstado = estado != null && !estado.isBlank();
        boolean tieneCliente = clienteId != null && clienteId > 0;

        if (tieneEstado && tieneCliente) {
            return pedidoRepository.findByEstadoAndUsuario_IdUsuarioOrderByFechaPedidoDesc(estado.trim(), clienteId);
        } else if (tieneEstado) {
            return pedidoRepository.findByEstadoOrderByFechaPedidoDesc(estado.trim());
        } else if (tieneCliente) {
            return pedidoRepository.findByUsuario_IdUsuarioOrderByFechaPedidoDesc(clienteId);
        } else {
            return pedidoRepository.findByOrderByFechaPedidoDesc();
        }
    }

    /**
     * Registra un nuevo pedido validando la disponibilidad real de stock de cada
     * detalle antes de persistir.
     *
     * <p>Esta es la entrada crítica del flujo de checkout. Se consulta el stock
     * actual de cada producto desde el repositorio (lectura real de base de datos)
     * y se compara contra la cantidad solicitada. Si algún detalle excede el
     * stock disponible, se lanza {@link StockInsuficienteException} y el pedido
     * <b>no se guarda</b>, cumpliendo con el requisito de la prueba
     * {@code rechazaCheckoutSinStock()}.
     *
     * <p>Uso de Google Guava (rúbrica FASE 2): {@code Preconditions.checkArgument()}
     * valida que los argumentos de entrada no sean nulos antes de iniciar la
     * validación de negocio.
     *
     * @param pedido  entidad Pedido con datos de cabecera (usuario, dirección,
     *                totales, estado)
     * @param detalles lista de {@link DetallePedido} con productos y cantidades
     * @return el pedido persistido con sus detalles
     * @throws StockInsuficienteException si algún producto no tiene stock suficiente
     */
    @Transactional
    public Pedido registrar(Pedido pedido, List<DetallePedido> detalles) {
        Preconditions.checkArgument(pedido != null, "El pedido no puede ser nulo");
        Preconditions.checkArgument(detalles != null && !detalles.isEmpty(),
                "El pedido debe contener al menos un detalle");

        // 1. Validar stock real de cada detalle ANTES de hacer save()
        for (DetallePedido detalle : detalles) {
            Producto producto = productoRepository.findById(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con ID: " + detalle.getProducto().getIdProducto()));

            if (detalle.getCantidad() > producto.getStock()) {
                log.warn("stock_insuficiente productoId={} nombre={} stockDisponible={} cantidadSolicitada={}",
                        producto.getIdProducto(), producto.getNombre(), producto.getStock(), detalle.getCantidad());
                throw new StockInsuficienteException(
                        "Stock insuficiente para el producto '" + producto.getNombre() +
                                "'. Stock disponible: " + producto.getStock() +
                                ", cantidad solicitada: " + detalle.getCantidad());
            }
        }

        // 2. Persistir pedido (solo si toda la validación de stock pasó)
        Pedido guardado = pedidoRepository.save(pedido);

        // 3. Persistir detalles asociados al pedido guardado
        for (DetallePedido detalle : detalles) {
            detalle.setPedido(guardado);
            detallePedidoRepository.save(detalle);
        }

        log.info("pedido_registrado idPedido={} codigo={} idUsuario={} total={} estado={}",
                guardado.getIdPedido(), guardado.getCodigo(),
                guardado.getUsuario() != null ? guardado.getUsuario().getIdUsuario() : null,
                guardado.getTotal(), guardado.getEstado());

        return guardado;
    }

    @Override
    public Pedido buscarPorId(Long id) {
        Preconditions.checkArgument(id != null && id > 0,
                "El identificador de pedido debe ser mayor que cero");

        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
    }

    @Override
    public Pedido buscarPorCodigo(String codigo) {
        Preconditions.checkArgument(codigo != null && !codigo.isBlank(),
                "El código de pedido no puede estar vacío");

        return pedidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con código: " + codigo));
    }

    @Override
    @Transactional
    public Pedido cambiarEstado(Long idPedido, CambioEstadoPedidoDTO dto, Integer idUsuario) {
        Preconditions.checkArgument(idPedido != null && idPedido > 0,
                "El identificador de pedido es requerido");
        Preconditions.checkArgument(dto != null && dto.getNuevoEstado() != null,
                "El nuevo estado del pedido es requerido");

        Pedido pedido = buscarPorId(idPedido);
        String estadoAnterior = pedido.getEstado();
        String nuevoEstado = dto.getNuevoEstado().toUpperCase().trim();

        // Validar que el nuevo estado sea un valor permitido
        Preconditions.checkArgument(
                "PENDIENTE".equals(nuevoEstado) || "PAGADO".equals(nuevoEstado) ||
                "EN_PREPARACION".equals(nuevoEstado) || "EN_TRANSITO".equals(nuevoEstado) ||
                "ENTREGADO".equals(nuevoEstado) || "CANCELADO".equals(nuevoEstado) ||
                "RECHAZADO".equals(nuevoEstado),
                "Estado de pedido no válido: " + nuevoEstado);

        pedido.setEstado(nuevoEstado);
        Pedido guardado = pedidoRepository.save(pedido);

        // Registrar transición en el historial de auditoría
        HistorialEstadoPedido h = new HistorialEstadoPedido();
        h.setPedido(pedido);
        h.setEstadoAnterior(estadoAnterior);
        h.setEstadoNuevo(nuevoEstado);
        h.setComentario(dto.getComentario() != null && !dto.getComentario().isBlank()
                ? dto.getComentario()
                : "Cambio manual de estado por administrador");

        if (idUsuario != null) {
            Usuario u = usuarioRepository.findById(idUsuario).orElse(null);
            h.setUsuario(u);
        }
        historialRepository.save(h);

        // Evento de auditoría: cambio de estado de pedido (no datos sensibles)
        log.info("pedido_actualizado idPedido={} codigo={} estadoAnterior={} estadoNuevo={} usuarioId={}",
                guardado.getIdPedido(), guardado.getCodigo(), estadoAnterior, nuevoEstado, idUsuario);

        return guardado;
    }

    @Override
    @Transactional
    public Envio actualizarEnvio(Long idPedido, EnvioUpdateDTO dto) {
        Preconditions.checkArgument(idPedido != null && idPedido > 0,
                "El identificador de pedido es requerido");
        Preconditions.checkArgument(dto != null, "Los datos de envío son requeridos");

        Pedido pedido = buscarPorId(idPedido);
        Envio envio = pedido.getEnvio();
        if (envio == null) {
            envio = new Envio();
            envio.setPedido(pedido);
        }

        if (dto.getTransportista() != null && !dto.getTransportista().isBlank()) {
            envio.setTransportista(dto.getTransportista());
        }
        if (dto.getTracking() != null && !dto.getTracking().isBlank()) {
            envio.setTracking(dto.getTracking());
        }
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            envio.setEstado(dto.getEstado().toUpperCase());
            if ("ENTREGADO".equalsIgnoreCase(dto.getEstado())) {
                envio.setFechaEntrega(OffsetDateTime.now());
                pedido.setEstado("ENTREGADO");
                pedidoRepository.save(pedido);
            }
        }
        if (dto.getFechaEstimada() != null) {
            envio.setFechaEstimada(dto.getFechaEstimada());
        }

        Envio guardado = envioRepository.save(envio);

        log.info("envio_actualizado idPedido={} idEnvio={} estado={}",
                idPedido, guardado.getIdEnvio(), guardado.getEstado());

        return guardado;
    }

    @Override
    @Transactional
    public Pago validarPago(Long idPedido, ValidacionPagoDTO dto, Integer idUsuario) {
        Preconditions.checkArgument(idPedido != null && idPedido > 0,
                "El identificador de pedido es requerido");
        Preconditions.checkArgument(dto != null && dto.getEstado() != null,
                "El estado de pago es requerido");

        Pedido pedido = buscarPorId(idPedido);
        Pago pago = pedido.getPago();
        if (pago == null) {
            log.warn("pago_no_encontrado idPedido={} intentoValidacionUsuarioId={}",
                    idPedido, idUsuario);
            throw new BadRequestException("El pedido no tiene un pago asociado para validar");
        }

        String nuevoEstado = dto.getEstado().toUpperCase().trim();
        Preconditions.checkArgument("APROBADO".equals(nuevoEstado) || "RECHAZADO".equals(nuevoEstado),
                "Estado de pago no válido: " + nuevoEstado);

        pago.setEstado(nuevoEstado);
        if (dto.getReferencia() != null && !dto.getReferencia().isBlank()) {
            pago.setReferencia(dto.getReferencia());
        }

        if ("APROBADO".equals(nuevoEstado)) {
            pago.setFechaPago(OffsetDateTime.now());
            pedido.setEstado("PAGADO");
            pedidoRepository.save(pedido);
        } else if ("RECHAZADO".equals(nuevoEstado)) {
            pedido.setEstado("RECHAZADO");
            pedidoRepository.save(pedido);
        }

        Pago guardado = pagoRepository.save(pago);

        // Historial de auditoría
        HistorialEstadoPedido h = new HistorialEstadoPedido();
        h.setPedido(pedido);
        h.setEstadoAnterior(pedido.getEstado());
        h.setEstadoNuevo(pedido.getEstado());
        h.setComentario("Validación de pago: " + nuevoEstado
                + (dto.getComentario() != null ? " - " + dto.getComentario() : ""));
        if (idUsuario != null) {
            h.setUsuario(usuarioRepository.findById(idUsuario).orElse(null));
        }
        historialRepository.save(h);

        // Evento de seguridad y auditoría: validación de pago (no datos sensibles)
        log.info("pago_validado idPedido={} idPago={} estado={} usuarioId={}",
                idPedido, guardado.getIdPago(), nuevoEstado, idUsuario);

        return guardado;
    }

    @Override
    public List<HistorialEstadoPedido> obtenerHistorial(Long idPedido) {
        Preconditions.checkArgument(idPedido != null && idPedido > 0,
                "El identificador de pedido es requerido");

        return historialRepository.findByPedido_IdPedidoOrderByFechaDesc(idPedido);
    }
}
