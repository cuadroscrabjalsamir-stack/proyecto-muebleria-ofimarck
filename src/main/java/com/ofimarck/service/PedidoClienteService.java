package com.ofimarck.service;

import com.ofimarck.domain.*;
import com.ofimarck.dto.request.PedidoRequest;
import com.ofimarck.dto.response.PedidoDetalleDTO;
import com.ofimarck.dto.response.PedidoResponse;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoClienteService {

    private static final Logger log = LoggerFactory.getLogger(PedidoClienteService.class);

    private final PedidoRepository pedidoRepository;
    private final DireccionEnvioRepository direccionRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;

    public PedidoClienteService(PedidoRepository pedidoRepository,
                                DireccionEnvioRepository direccionRepository,
                                ProductoRepository productoRepository,
                                UsuarioRepository usuarioRepository,
                                CarritoService carritoService) {
        this.pedidoRepository = pedidoRepository;
        this.direccionRepository = direccionRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoService = carritoService;
    }

    @Transactional
    public PedidoResponse crearPedido(Integer idUsuario, PedidoRequest request, HttpSession session) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        log.info("crearPedido inicio correlationId={} idUsuario={}", correlationId, idUsuario);

        Preconditions.checkArgument(idUsuario != null && idUsuario > 0, "El id de usuario es obligatorio");
        Preconditions.checkArgument(request != null, "El pedido no puede ser nulo");

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        DireccionEnvio direccion = direccionRepository.findById(request.getIdDireccionEnvio())
                .orElseThrow(() -> new ResourceNotFoundException("Dirección de envío no encontrada"));

        if (!direccion.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new BadRequestException("La dirección de envío no pertenece al usuario autenticado");
        }

        Carrito carrito = (Carrito) session.getAttribute("CARRITO");
        if (carrito == null || carrito.isEmpty()) {
            throw new BadRequestException("El carrito está vacío");
        }

        // Validar stock de cada item antes de crear el pedido
        for (CarritoItem item : carrito.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.getIdProducto()));
            if (!producto.getEstado()) {
                throw new BadRequestException("El producto está inactivo: " + producto.getNombre());
            }
            if (producto.getStock() < item.getCantidad()) {
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
        String referencia = StringUtils.defaultIfBlank(request.getReferencia(), null);
        pedido.setObservaciones("Pedido desde carrito de compras" + (referencia != null ? " | ref: " + referencia : ""));
        pedido = pedidoRepository.save(pedido);

        // Crear detalles
        List<CarritoItem> items = ImmutableList.copyOf(carrito.getItems());
        for (CarritoItem item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.getIdProducto()));

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.setSubtotal(item.getSubtotal());
            pedido.getDetalles().add(detalle);
        }

        // Crear pago pendiente
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo(StringUtils.defaultIfBlank(request.getMetodoPago(), "CONTRA_ENTREGA"));
        pago.setEstado("PENDIENTE");
        pago.setMonto(total);
        pago.setReferencia(referencia);
        pedido.setPago(pago);

        // Crear envío pendiente
        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstado("PENDIENTE");
        envio.setCosto(costoEnvio);
        pedido.setEnvio(envio);

        pedidoRepository.save(pedido);

        // Limpiar carrito después de crear el pedido
        carritoService.limpiarCarrito(session);

        log.info("crearPedido fin correlationId={} pedidoId={} codigo={} total={}",
                correlationId, pedido.getIdPedido(), pedido.getCodigo(), total);
        MDC.clear();

        return new PedidoResponse(
                pedido.getIdPedido(),
                pedido.getCodigo(),
                pedido.getTotal(),
                pedido.getEstado(),
                pedido.getFechaPedido()
        );
    }

    public List<PedidoResponse> listarPedidos(Integer idUsuario) {
        List<Pedido> pedidos = pedidoRepository.findByUsuario_IdUsuarioOrderByFechaPedidoDesc(idUsuario);
        return pedidos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PedidoResponse obtenerPedido(Long idPedido, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + idPedido));

        if (!pedido.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new BadRequestException("El pedido no pertenece al usuario autenticado");
        }

        return toDto(pedido);
    }

    public PedidoResponse obtenerPedidoPorCodigo(String codigo, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con código: " + codigo));

        if (!pedido.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new BadRequestException("El pedido no pertenece al usuario autenticado");
        }

        return toDto(pedido);
    }

    public PedidoDetalleDTO obtenerPedidoDetalle(Long idPedido, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + idPedido));

        if (!pedido.getUsuario().getIdUsuario().equals(idUsuario)) {
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