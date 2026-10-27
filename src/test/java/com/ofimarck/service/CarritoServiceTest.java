package com.ofimarck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ofimarck.domain.Carrito;
import com.ofimarck.domain.CarritoItem;
import com.ofimarck.domain.Producto;
import com.ofimarck.dto.request.CarritoItemRequest;
import com.ofimarck.dto.response.CarritoDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.ProductoRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private HttpSession session;

    @InjectMocks private com.ofimarck.service.CarritoService service;

    private Producto producto;
    private Carrito carrito;
    private CarritoItemRequest request;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setIdProducto(100);
        producto.setEstado(true);
        producto.setStock(10);
        producto.setPrecio(new BigDecimal("100.00"));

        carrito = new Carrito();

        request = new CarritoItemRequest();
        request.setIdProducto(100);
        request.setCantidad(2);

        when(session.getAttribute("CARRITO")).thenReturn(carrito);
    }

    @Test
    void agregarItemExitoso() {
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        CarritoDTO dto = service.agregarItem(session, request);

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalItems()).isEqualTo(2);
        assertThat(dto.getTotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(productoRepository, times(1)).findById(100);
    }

    @Test
    void agregarItemRechazaStockInsuficiente() {
        producto.setStock(0);
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.agregarItem(session, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void agregarItemRechazaProductoInactivo() {
        producto.setEstado(false);
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.agregarItem(session, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void actualizarCantidadExitoso() {
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));
        service.agregarItem(session, request);

        CarritoItemRequest upd = new CarritoItemRequest();
        upd.setIdProducto(100);
        upd.setCantidad(5);

        CarritoDTO dto = service.actualizarCantidad(session, 100, 5);

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalItems()).isEqualTo(5);
    }

    @Test
    void eliminarItemExitoso() {
        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));
        service.agregarItem(session, request);

        CarritoDTO dto = service.eliminarItem(session, 100);

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalItems()).isEqualTo(0);
    }

    @Test
    void actualizarCantidadRechazaProductoInexistente() {
        when(productoRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarCantidad(session, 999, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
