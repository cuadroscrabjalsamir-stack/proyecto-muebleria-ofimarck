package com.ofimarck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ofimarck.domain.Envio;
import com.ofimarck.domain.HistorialEstadoPedido;
import com.ofimarck.domain.Pago;
import com.ofimarck.domain.Pedido;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.CambioEstadoPedidoDTO;
import com.ofimarck.dto.response.ValidacionPagoDTO;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.EnvioRepository;
import com.ofimarck.repository.HistorialEstadoPedidoRepository;
import com.ofimarck.repository.PagoRepository;
import com.ofimarck.repository.PedidoRepository;
import com.ofimarck.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private EnvioRepository envioRepository;
    @Mock private HistorialEstadoPedidoRepository historialRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private com.ofimarck.service.PedidoService service;

    private Pedido pedido;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setEstado(true);

        pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setCodigo("ORD-20260101-0001");
        pedido.setEstado("PENDIENTE");
        pedido.setUsuario(usuario);

        Pago pago = new Pago();
        pago.setIdPago(1L);
        pago.setPedido(pedido);
        pago.setEstado("PENDIENTE");
        pago.setMonto(new BigDecimal("215.00"));
        pedido.setPago(pago);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cambiarEstadoExitoso() {
        CambioEstadoPedidoDTO dto = new CambioEstadoPedidoDTO();
        dto.setNuevoEstado("PAGADO");
        dto.setComentario("Pago confirmado");

        com.ofimarck.domain.Pedido res = service.cambiarEstado(1L, dto, 1);

        assertThat(res.getEstado()).isEqualTo("PAGADO");
        verify(historialRepository, times(1)).save(any(HistorialEstadoPedido.class));
    }

    @Test
    void cambiarEstadoProductoInexistente() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        CambioEstadoPedidoDTO dto = new CambioEstadoPedidoDTO();
        dto.setNuevoEstado("PAGADO");

        assertThatThrownBy(() -> service.cambiarEstado(999L, dto, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validarPagoAprobadoDescuentaStockYPasaPedidoAPagado() {
        ValidacionPagoDTO dto = new ValidacionPagoDTO();
        dto.setEstado("APROBADO");
        dto.setReferencia("ref-xyz");

        Pago res = service.validarPago(1L, dto, 1);

        assertThat(res.getEstado()).isEqualTo("APROBADO");
        assertThat(res.getReferencia()).isEqualTo("ref-xyz");
        assertThat(res.getFechaPago()).isNotNull();
        assertThat(pedido.getEstado()).isEqualTo("PAGADO");
        verify(pagoRepository, times(1)).save(any(Pago.class));
        verify(historialRepository, times(1)).save(any(HistorialEstadoPedido.class));
    }

    @Test
    void validarPagoRechazado() {
        ValidacionPagoDTO dto = new ValidacionPagoDTO();
        dto.setEstado("RECHAZADO");
        dto.setComentario("Fondos insuficientes");

        Pago res = service.validarPago(1L, dto, 1);

        assertThat(res.getEstado()).isEqualTo("RECHAZADO");
        assertThat(pedido.getEstado()).isEqualTo("RECHAZADO");
    }

    @Test
    void validarPagoPedidoSinPago() {
        Pedido sinPago = new Pedido();
        sinPago.setIdPedido(2L);
        sinPago.setEstado("PENDIENTE");
        when(pedidoRepository.findById(2L)).thenReturn(Optional.of(sinPago));

        ValidacionPagoDTO dto = new ValidacionPagoDTO();
        dto.setEstado("APROBADO");

        assertThatThrownBy(() -> service.validarPago(2L, dto, 1))
                .isInstanceOf(com.ofimarck.exception.BadRequestException.class)
                .hasMessageContaining("no tiene un pago asociado");
    }
}
