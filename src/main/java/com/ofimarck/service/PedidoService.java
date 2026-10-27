package com.ofimarck.service;

import com.ofimarck.domain.*;
import com.ofimarck.dto.request.CambioEstadoPedidoDTO;
import com.ofimarck.dto.request.EnvioUpdateDTO;
import com.ofimarck.dto.response.ValidacionPagoDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final HistorialEstadoPedidoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository,
            PagoRepository pagoRepository,
            EnvioRepository envioRepository,
            HistorialEstadoPedidoRepository historialRepository,
            UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
    }

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

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
    }

    public Pedido buscarPorCodigo(String codigo) {
        return pedidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con código: " + codigo));
    }

    public List<HistorialEstadoPedido> obtenerHistorial(Long idPedido) {
        return historialRepository.findByPedido_IdPedidoOrderByFechaDesc(idPedido);
    }

    @Transactional
    public Pedido cambiarEstado(Long idPedido, CambioEstadoPedidoDTO dto, Integer idUsuario) {
        Pedido pedido = buscarPorId(idPedido);
        String estadoAnterior = pedido.getEstado();
        String nuevoEstado = dto.getNuevoEstado().toUpperCase().trim();

        pedido.setEstado(nuevoEstado);
        Pedido guardado = pedidoRepository.save(pedido);

        // Registrar en historial si no fue registrado automáticamente por trigger
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

        return guardado;
    }

    @Transactional
    public Envio actualizarEnvio(Long idPedido, EnvioUpdateDTO dto) {
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

        return envioRepository.save(envio);
    }

    @Transactional
    public Pago validarPago(Long idPedido, ValidacionPagoDTO dto, Integer idUsuario) {
        Pedido pedido = buscarPorId(idPedido);
        Pago pago = pedido.getPago();
        if (pago == null) {
            throw new BadRequestException("El pedido no tiene un pago asociado para validar");
        }

        String nuevoEstado = dto.getEstado().toUpperCase().trim();
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

        // Historial
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

        return guardado;
    }
}
