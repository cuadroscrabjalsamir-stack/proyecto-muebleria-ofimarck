package com.ofimarck.service;

import com.ofimarck.domain.Carrito;
import com.ofimarck.domain.CarritoItem;
import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.CarritoDTO;
import com.ofimarck.dto.response.CarritoItemDTO;
import com.ofimarck.dto.request.CarritoItemRequest;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.ProductoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarritoService {

    private static final String CARRITO_SESSION_ATTR = "CARRITO";

    private final ProductoRepository productoRepository;

    public CarritoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    private Carrito getCarrito(HttpSession session) {
        Carrito carrito = (Carrito) session.getAttribute(CARRITO_SESSION_ATTR);
        if (carrito == null) {
            carrito = new Carrito();
            session.setAttribute(CARRITO_SESSION_ATTR, carrito);
        }
        return carrito;
    }

    public CarritoDTO obtenerCarrito(HttpSession session) {
        Carrito carrito = getCarrito(session);
        List<CarritoItemDTO> itemsDTO = carrito.getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new CarritoDTO(itemsDTO, carrito.getTotalItems(), carrito.getTotal());
    }

    public CarritoDTO agregarItem(HttpSession session, CarritoItemRequest request) {
        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + request.getIdProducto()));

        if (!producto.getEstado()) {
            throw new BadRequestException("El producto está inactivo");
        }

        if (producto.getStock() < request.getCantidad()) {
            throw new BadRequestException("Stock insuficiente para el producto: " + producto.getNombre());
        }

        Carrito carrito = getCarrito(session);
        CarritoItem item = new CarritoItem(
                producto.getIdProducto(),
                producto.getSku(),
                producto.getNombre(),
                request.getCantidad(),
                producto.getPrecio(),
                producto.getUrlImagen()
        );
        carrito.addItem(item);
        session.setAttribute(CARRITO_SESSION_ATTR, carrito);

        return obtenerCarrito(session);
    }

    public CarritoDTO actualizarCantidad(HttpSession session, Integer idProducto, Integer cantidad) {
        if (cantidad == null || cantidad < 1) {
            throw new BadRequestException("La cantidad debe ser al menos 1");
        }

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + idProducto));

        if (producto.getStock() < cantidad) {
            throw new BadRequestException("Stock insuficiente para el producto: " + producto.getNombre());
        }

        Carrito carrito = getCarrito(session);
        carrito.updateQuantity(idProducto, cantidad);
        session.setAttribute(CARRITO_SESSION_ATTR, carrito);

        return obtenerCarrito(session);
    }

    public CarritoDTO eliminarItem(HttpSession session, Integer idProducto) {
        Carrito carrito = getCarrito(session);
        carrito.removeItem(idProducto);
        session.setAttribute(CARRITO_SESSION_ATTR, carrito);

        return obtenerCarrito(session);
    }

    public void limpiarCarrito(HttpSession session) {
        Carrito carrito = getCarrito(session);
        carrito.clear();
        session.setAttribute(CARRITO_SESSION_ATTR, carrito);
    }

    private CarritoItemDTO toDto(CarritoItem item) {
        CarritoItemDTO dto = new CarritoItemDTO();
        dto.setIdProducto(item.getIdProducto());
        dto.setSku(item.getSku());
        dto.setNombre(item.getNombre());
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setSubtotal(item.getSubtotal());
        dto.setUrlImagen(item.getUrlImagen());
        return dto;
    }
}