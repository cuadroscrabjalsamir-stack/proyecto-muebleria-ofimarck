package com.ofimarck.service;

import com.ofimarck.domain.Carrito;
import com.ofimarck.domain.CarritoItem;
import com.ofimarck.domain.DetallePedido;
import com.ofimarck.domain.DireccionEnvio;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Envio;
import com.ofimarck.domain.Pago;
import com.ofimarck.domain.Pedido;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.PedidoRequest;
import com.ofimarck.dto.response.PedidoDetalleDTO;
import com.ofimarck.dto.response.PedidoResponse;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.DetallePedidoRepository;
import com.ofimarck.repository.DireccionEnvioRepository;
import com.ofimarck.repository.PedidoRepository;
import com.ofimarck.repository.ProductoRepository;
import com.ofimarck.repository.UsuarioRepository;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IPedidoClienteService} para el flujo de checkout
 * desde el carrito de compras.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta clase se encarga
 * exclusivamente de crear pedidos desde el carrito de un cliente autenticado,
 * validar stock, calcular totales y persistir pago y envío.
 *
 * <p>Principio de Inversión de Dependencias (D de SOLID): depende de
 * interfaces de repositorio inyectadas por constructor.
 *
 * <p>Uso de Google Guava (rúbrica FASE 2):
 * <ul>
 *   <li>{@link Preconditions#checkArgument(boolean, String)}: valida que el
 *       carrito no esté vacío antes de proceder al checkout. Si falla, se
 *       lanza {@link IllegalArgumentException} y el checkout se detiene.</li>
 *   <li>{@link ImmutableList}: se utiliza para envolver la lista de detalles
 *       del pedido antes de persistir, garantizando que la colección de
 *       detalles sea inmutable y no pueda ser modificada accidentalmente
 *       después de su creación.</li>
 * </ul>
 *
 * <p>Uso de Logback/SLF4J (rúbrica FASE 2): registro de eventos de auditoría
 * del checkout (pedido creado, carrito vacío, stock insuficiente).
 */
@Service
public class PedidoClienteService implements IPedidoClienteService {

    private static final Logger log = LoggerFactory.getLogger(PedidoClienteService.class);

    private final PedidoRepository pedidoRepository;
    private final DireccionEnvioRepository direccionRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;
    private final DetallePedidoRepository detallePedidoRepository;

    public PedidoClienteService(PedidoRepository pedidoRepository,
                                DireccionEnvioRepository direccionRepository,
                                ProductoRepository productoRepository,
                                UsuarioRepository usuarioRepository,
                                CarritoService carritoService,
                                DetallePedidoRepository detallePedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.direccionRepository = direccionRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoService = carritoService;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Override
    @Transactional
    public PedidoResponse crearPedido(Integer idUsuario, PedidoRequest request, HttpSession session) {
        Preconditions.checkArgument(idUsuario != null && idUsuario > 0,
                "El identificador de usuario es requerido");
        Preconditions.checkArgument(request != null, "La solicitud de pedido es requerida");

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        DireccionEnvio direccion = direccionRepository.findById(request.getIdDireccionEnvio())
                .orElseThrow(() -> new ResourceNotFoundException("Dirección de envío no encontrada"));

        if (!direccion.getUsuario().getIdUsuario().equals(idUsuario)) {
            log.warn("direccion_ajena idUsuario={} idDireccion={} intentoCheckout",
                    idUsuario, request.getIdDireccionEnvio());
            throw new BadRequestException("La dirección de envío no pertenece al usuario autenticado");
        }

        Carrito carrito = (Carrito) session.getAttribute("CARRITO");

        // Guava: validar que el carrito no esté vacío antes del checkout
        Preconditions.checkArgument(carrito != null && !carrito.isEmpty(),
                "El carrito está vacío. Agregue productos antes de proceder al checkout.");

        // Validar stock de cada item antes de crear el pedido
        for (CarritoItem item : carrito.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.getIdProducto()));
            if (!producto.getEstado()) {
                throw new BadRequestException("El producto está inactivo: " + producto.getNombre());
            }
            if (producto.getStock() < item.getCantidad()) {
                log.warn("stock_insuficiente productoId={} nombre={} stockDisponible={} cantidadSolicitada={}",
                        producto.getIdProducto(), producto.getNombre(), producto.getStock(), item.getCantidad());
                throw new BadRequestException("Stock insuficiente para: " + producto.getNombre());
            }
        }

        // Calcular totales
        BigDecimal subtotal = carrito.getTotal();
        BigDecimal costoEnvio = BigDecimal.valueOf(15.00);
        BigDecimal total = subtotal.add(costoEnvio);

        // Crear pedido
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDireccionEnvio(direccion);
        pedido.setDireccionSnapshot(buildDireccionSnapshot(direccion));
        pedido.setSubtotal(subtotal);
        pedido.setCostoEnvio(costoEnvio);
        pedido.setTotal(total);
        pedido.setEstado("PENDIENTE");
        pedido.setObservaciones("Pedido desde carrito de compras");
        final Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // Crear detalles y guardarlos con colección inmutable (Guava ImmutableList)
        List<DetallePedido> detalles = carrito.getItems().stream()
                .map(item -> {
                    Producto producto = productoRepository.findById(item.getIdProducto())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Producto no encontrado: " + item.getIdProducto()));
                    DetallePedido detalle = new DetallePedido();
                    detalle.setPedido(pedidoGuardado);
                    detalle.setProducto(producto);
                    detalle.setNombreProducto(producto.getNombre());
                    detalle.setCantidad(item.getCantidad());
                    detalle.setPrecioUnitario(item.getPrecioUnitario());
                    detalle.setSubtotal(item.getSubtotal());
                    return detalle;
                })
                .collect(Collectors.toList());

        // InmutableList protege los detalles de pedido contra modificaciones accidentales
        ImmutableList<DetallePedido> detallesInmutables = ImmutableList.copyOf(detalles);
        detallesInmutables.forEach(detallePedidoRepository::save);

        pedidoGuardado.getDetalles().addAll(detalles);

        // Crear pago pendiente
        Pago pago = new Pago();
        pago.setPedido(pedidoGuardado);
        pago.setMetodo(request.getMetodoPago());
        pago.setEstado("PENDIENTE");
        pago.setMonto(total);
        pago.setReferencia(request.getReferencia());
        pedidoGuardado.setPago(pago);

        // Crear envío pendiente
        Envio envio = new Envio();
        envio.setPedido(pedidoGuardado);
        envio.setEstado("PENDIENTE");
        envio.setCosto(costoEnvio);
        pedidoGuardado.setEnvio(envio);

        pedidoRepository.save(pedidoGuardado);

        // Limpiar carrito después de crear el pedido
        carritoService.limpiarCarrito(session);

        // Evento de auditoría: pedido registrado (no datos sensibles)
        log.info("pedido_registrado idPedido={} codigo={} idUsuario={} total={} estado={}",
                pedidoGuardado.getIdPedido(), pedidoGuardado.getCodigo(), idUsuario, total, pedidoGuardado.getEstado());

        return new PedidoResponse(
                pedidoGuardado.getIdPedido(),
                pedidoGuardado.getCodigo(),
                pedidoGuardado.getTotal(),
                pedidoGuardado.getEstado(),
                pedidoGuardado.getFechaPedido()
        );
    }

    @Override
    public List<PedidoResponse> listarPedidos(Integer idUsuario) {
        List<Pedido> pedidos = pedidoRepository.findByUsuario_IdUsuarioOrderByFechaPedidoDesc(idUsuario);
        return pedidos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PedidoResponse obtenerPedido(Long idPedido, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + idPedido));

        if (!pedido.getUsuario().getIdUsuario().equals(idUsuario)) {
            log.warn("pedido_acceso_denegado idPedido={} idUsuario={} intentoAccesoAjeno",
                    idPedido, idUsuario);
            throw new BadRequestException("El pedido no pertenece al usuario autenticado");
        }

        return toDto(pedido);
    }

    @Override
    public PedidoResponse obtenerPedidoPorCodigo(String codigo, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con código: " + codigo));

        if (!pedido.getUsuario().getIdUsuario().equals(idUsuario)) {
            log.warn("pedido_acceso_denegado codigo={} idUsuario={} intentoAccesoAjeno", codigo, idUsuario);
            throw new BadRequestException("El pedido no pertenece al usuario autenticado");
        }

        return toDto(pedido);
    }

    @Override
    public PedidoDetalleDTO obtenerPedidoDetalle(Long idPedido, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + idPedido));

        if (!pedido.getUsuario().getIdUsuario().equals(idUsuario)) {
            log.warn("pedido_detalle_acceso_denegado idPedido={} idUsuario={}", idPedido, idUsuario);
            throw new BadRequestException("El pedido no pertenece al usuario autenticado");
        }

        PedidoDetalleDTO dto = new PedidoDetalleDTO();
        dto.setIdPedido(pedido.getIdPedido());
        dto.setCodigo(pedido.getCodigo());
        dto.setFechaPedido(pedido.getFechaPedido());
        dto.setSubtotal(pedido.getSubtotal());
        dto.setCostoEnvio(pedido.getCostoEnvio());
        dto.setTotal(pedido.getTotal());
        dto.setEstado(pedido.getEstado());
        dto.setObservaciones(pedido.getObservaciones());

        if (pedido.getDireccionEnvio() != null) {
            PedidoDetalleDTO.DireccionDTO dir = new PedidoDetalleDTO.DireccionDTO();
            dir.setDestinatario(pedido.getDireccionEnvio().getDestinatario());
            dir.setTelefono(pedido.getDireccionEnvio().getTelefono());
            dir.setDepartamento(pedido.getDireccionEnvio().getDepartamento());
            dir.setProvincia(pedido.getDireccionEnvio().getProvincia());
            dir.setDistrito(pedido.getDireccionEnvio().getDistrito());
            dir.setLineaDireccion(pedido.getDireccionEnvio().getLineaDireccion());
            dir.setReferencia(pedido.getDireccionEnvio().getReferencia());
            dto.setDireccion(dir);
        }

        if (pedido.getPago() != null) {
            PedidoDetalleDTO.PagoDTO pago = new PedidoDetalleDTO.PagoDTO();
            pago.setMetodo(pedido.getPago().getMetodo());
            pago.setEstado(pedido.getPago().getEstado());
            pago.setReferencia(pedido.getPago().getReferencia());
            pago.setMonto(pedido.getPago().getMonto());
            dto.setPago(pago);
        }

        if (pedido.getEnvio() != null) {
            PedidoDetalleDTO.EnvioDTO envio = new PedidoDetalleDTO.EnvioDTO();
            envio.setTransportista(pedido.getEnvio().getTransportista());
            envio.setTracking(pedido.getEnvio().getTracking());
            envio.setEstado(pedido.getEnvio().getEstado());
            dto.setEnvio(envio);
        }

        List<PedidoDetalleDTO.ItemPedidoDTO> items = pedido.getDetalles().stream()
                .map(d -> {
                    PedidoDetalleDTO.ItemPedidoDTO item = new PedidoDetalleDTO.ItemPedidoDTO();
                    item.setNombreProducto(d.getNombreProducto());
                    item.setCantidad(d.getCantidad());
                    item.setPrecioUnitario(d.getPrecioUnitario());
                    item.setSubtotal(d.getSubtotal());
                    return item;
                })
                .collect(Collectors.toList());
        dto.setItems(items);

        return dto;
    }

    private String buildDireccionSnapshot(DireccionEnvio direccion) {
        return String.format(
                "{\"destinatario\":\"%s\",\"telefono\":\"%s\",\"departamento\":\"%s\",\"provincia\":\"%s\",\"distrito\":\"%s\",\"direccion\":\"%s\",\"referencia\":\"%s\"}",
                direccion.getDestinatario(),
                direccion.getTelefono(),
                direccion.getDepartamento(),
                direccion.getProvincia(),
                direccion.getDistrito(),
                direccion.getLineaDireccion(),
                direccion.getReferencia() != null ? direccion.getReferencia() : ""
        );
    }

    private PedidoResponse toDto(Pedido pedido) {
        return new PedidoResponse(
                pedido.getIdPedido(),
                pedido.getCodigo(),
                pedido.getTotal(),
                pedido.getEstado(),
                pedido.getFechaPedido()
        );
    }
}
