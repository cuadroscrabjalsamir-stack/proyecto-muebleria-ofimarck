package com.ofimarck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ofimarck.domain.Carrito;
import com.ofimarck.domain.CarritoItem;
import com.ofimarck.domain.DireccionEnvio;
import com.ofimarck.domain.Pedido;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.PedidoRequest;
import com.ofimarck.dto.response.PedidoResponse;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.DireccionEnvioRepository;
import com.ofimarck.repository.PedidoRepository;
import com.ofimarck.repository.ProductoRepository;
import com.ofimarck.repository.UsuarioRepository;
import com.ofimarck.service.CarritoService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoClienteServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private DireccionEnvioRepository direccionRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CarritoService carritoService;
    @Mock private HttpSession session;

    @InjectMocks private com.ofimarck.service.PedidoClienteService service;
    private Usuario usuario;
    private DireccionEnvio direccion;
    private Producto producto;
    private Carrito carrito;
    private PedidoRequest request;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setEstado(true);

        direccion = new DireccionEnvio();
        direccion.setIdDireccion(10);
        direccion.setUsuario(usuario);

        producto = new Producto();
        producto.setIdProducto(100);
        producto.setEstado(true);
        producto.setStock(10);
        producto.setPrecio(new BigDecimal("100.00"));

        carrito = new Carrito();
        carrito.addItem(new CarritoItem(100, "SKU-1", "Producto prueba", 2, new BigDecimal("100.00"), null));

        request = new PedidoRequest();
        request.setIdDireccionEnvio(10);
        request.setMetodoPago("TARJETA");
        request.setReferencia("ref-123");

        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setIdPedido(1L);
            p.setCodigo("ORD-20260101-0001");
            p.setFechaPedido(OffsetDateTime.now());
            return p;
        });
    }

    @Test
    void crearPedidoExitoso() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(direccionRepository.findById(10)).thenReturn(Optional.of(direccion));
        when(session.getAttribute("CARRITO")).thenReturn(carrito);
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        PedidoResponse res = service.crearPedido(1, request, session);

        assertThat(res).isNotNull();
        assertThat(res.getEstado()).isEqualTo("PENDIENTE");
        assertThat(res.getTotal()).isEqualByComparingTo(new BigDecimal("215.00"));
        assertThat(res.getCodigo()).isNotNull();
        verify(pedidoRepository, times(2)).save(any(Pedido.class));
        verify(carritoService, times(1)).limpiarCarrito(session);
    }

    @Test
    void crearPedidoRechazaStockInsuficiente() {
        producto.setStock(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(direccionRepository.findById(10)).thenReturn(Optional.of(direccion));
        when(session.getAttribute("CARRITO")).thenReturn(carrito);
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.crearPedido(1, request, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearPedidoRechazaCarritoVacio() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(direccionRepository.findById(10)).thenReturn(Optional.of(direccion));
        when(session.getAttribute("CARRITO")).thenReturn(new Carrito());
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.crearPedido(1, request, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("El carrito está vacío");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearPedidoRechazaDireccionDeOtroUsuario() {
        Usuario otro = new Usuario();
        otro.setIdUsuario(2);
        otro.setEstado(true);
        direccion.setUsuario(otro);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(direccionRepository.findById(10)).thenReturn(Optional.of(direccion));
        when(session.getAttribute("CARRITO")).thenReturn(carrito);
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.crearPedido(1, request, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no pertenece al usuario autenticado");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearPedidoRechazaIdUsuarioNulo() {
        assertThatThrownBy(() -> service.crearPedido(null, request, session))
                .isInstanceOf(IllegalArgumentException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearPedidoProductoNoExistente() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(direccionRepository.findById(10)).thenReturn(Optional.of(direccion));
        when(session.getAttribute("CARRITO")).thenReturn(carrito);
        when(productoRepository.findById(100)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearPedido(1, request, session))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pedidoRepository, never()).save(any());
    }
}
