package com.ofimarck.service;

import com.ofimarck.domain.DetallePedido;
import com.ofimarck.domain.DireccionEnvio;
import com.ofimarck.domain.Pedido;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Usuario;
import com.ofimarck.exception.StockInsuficienteException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link PedidoService} siguiendo la metodología TDD.
 *
 * <p>Esta clase se diseña <b>antes</b> de completar la implementación de
 * {@link PedidoService#registrar(Pedido, List)} para demostrar el ciclo
 * Rojo-Verde-Refactor de TDD:
 *
 * <ol>
 *   <li><b>Rojo:</b> la prueba {@code rechazaCheckoutSinStock()} falla porque
 *       el servicio aún no valida el stock antes de persistir.</li>
 *   <li><b>Verde:</b> se implementa la validación de stock en
 *       {@code registrar()} lanzando {@link StockInsuficienteException} y la
 *       prueba pasa.</li>
 *   <li><b>Refactor:</b> se mejora la legibilidad y el logging de auditoría
 *       sin romper la prueba.</li>
 * </ol>
 *
 * <p>Uso de Mockito: se mockean todos los repositorios para aislar la capa de
 * servicio de la base de datos, verificando que la lógica de negocio se
 * ejecuta correctamente sin depender de PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private DetallePedidoRepository detallePedidoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private HistorialEstadoPedidoRepository historialRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private Producto productoStockBajo;
    private Pedido pedido;
    private List<DetallePedido> detalles;

    @BeforeEach
    void setUp() {
        // Producto con stock insuficiente (solo 2 unidades disponibles)
        productoStockBajo = new Producto();
        productoStockBajo.setIdProducto(1);
        productoStockBajo.setSku("MESA-001");
        productoStockBajo.setNombre("Mesa de oficina");
        productoStockBajo.setStock(2);
        productoStockBajo.setEstado(true);

        // Usuario propietario del pedido
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombres("Juan");
        usuario.setApellidos("Pérez");
        usuario.setCorreo("juan@test.com");

        // Dirección de envío
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setIdDireccion(1);
        direccion.setDestinatario("Juan Pérez");

        // Pedido de cabecera
        pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDireccionEnvio(direccion);
        pedido.setSubtotal(BigDecimal.valueOf(100.00));
        pedido.setCostoEnvio(BigDecimal.valueOf(15.00));
        pedido.setTotal(BigDecimal.valueOf(115.00));
        pedido.setEstado("PENDIENTE");

        // Detalle con cantidad solicitada MAYOR al stock disponible (10 > 2)
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(productoStockBajo);
        detalle.setCantidad(10);
        detalle.setPrecioUnitario(BigDecimal.valueOf(50.00));
        detalle.setSubtotal(BigDecimal.valueOf(500.00));

        detalles = new ArrayList<>();
        detalles.add(detalle);
    }

    /**
     * Prueba TDD: rechazaCheckoutSinStock().
     *
     * <p>Verifica que al intentar registrar un pedido con un detalle cuya
     * cantidad solicitada exceda el stock disponible, el servicio lance
     * {@link StockInsuficienteException} <b>antes</b> de guardar el pedido en
     * la base de datos.
     *
     * <p>Resultado esperado: el test pasa en verde cuando
     * {@code PedidoService.registrar()} consulta el stock real vía
     * {@code productoRepository.findById()} y compara contra la cantidad
     * solicitada.
     */
    @Test
    @DisplayName("Debe rechazar el checkout cuando no hay stock suficiente (rechazaCheckoutSinStock)")
    void rechazaCheckoutSinStock() {
        // Arrange: el producto consultado desde el repositorio tiene stock 2
        when(productoRepository.findById(1))
                .thenReturn(Optional.of(productoStockBajo));

        // Act & Assert: se espera StockInsuficienteException
        assertThrows(StockInsuficienteException.class, () -> {
            pedidoService.registrar(pedido, detalles);
        }, "Se esperaba StockInsuficienteException al solicitar más stock del disponible");

        // Verificar que el pedido NO se persistió (falló antes del save)
        verify(pedidoRepository, never()).save(any(Pedido.class));

        // Verificar que no se guardaron detalles
        verify(detallePedidoRepository, never()).save(any(DetallePedido.class));
    }

    @Test
    @DisplayName("Debe registrar el pedido cuando hay stock suficiente")
    void registraPedidoCuandoHayStockSuficiente() {
        // Arrange: producto con stock suficiente (10 unidades, se piden 2)
        Producto productoConStock = new Producto();
        productoConStock.setIdProducto(1);
        productoConStock.setSku("MESA-001");
        productoConStock.setNombre("Mesa de oficina");
        productoConStock.setStock(10);
        productoConStock.setEstado(true);

        DetallePedido detalleOk = new DetallePedido();
        detalleOk.setProducto(productoConStock);
        detalleOk.setCantidad(2);
        detalleOk.setPrecioUnitario(BigDecimal.valueOf(50.00));
        detalleOk.setSubtotal(BigDecimal.valueOf(100.00));

        List<DetallePedido> detallesOk = new ArrayList<>();
        detallesOk.add(detalleOk);

        Pedido pedidoGuardado = new Pedido();
        pedidoGuardado.setIdPedido(100L);
        pedidoGuardado.setCodigo("ORD-20261027-0100");

        when(productoRepository.findById(1)).thenReturn(Optional.of(productoConStock));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoGuardado);

        // Act
        Pedido resultado = pedidoService.registrar(pedido, detallesOk);

        // Assert
        assertNotNull(resultado);
        assertEquals(100L, resultado.getIdPedido());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(detallePedidoRepository, times(1)).save(any(DetallePedido.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el producto no existe")
    void lanzaExcepcionCuandoProductoNoExiste() {
        // Arrange: el repositorio devuelve vacío (producto no encontrado)
        when(productoRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            pedidoService.registrar(pedido, detalles);
        });

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la lista de detalles está vacía")
    void rechazaDetallesVacios() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pedidoService.registrar(pedido, new ArrayList<>());
        });

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }
}
