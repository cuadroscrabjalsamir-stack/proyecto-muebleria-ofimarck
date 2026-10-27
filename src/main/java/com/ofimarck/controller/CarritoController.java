package com.ofimarck.controller;

import com.ofimarck.dto.response.CarritoDTO;
import com.ofimarck.dto.request.CarritoItemRequest;
import com.ofimarck.service.CarritoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public ResponseEntity<CarritoDTO> obtener(HttpSession session) {
        CarritoDTO carrito = carritoService.obtenerCarrito(session);
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregar(@RequestBody CarritoItemRequest request, HttpSession session) {
        CarritoDTO carrito = carritoService.agregarItem(session, request);
        return ResponseEntity.ok(carrito);
    }

    @PutMapping("/items/{idProducto}")
    public ResponseEntity<CarritoDTO> actualizarCantidad(
            @PathVariable Integer idProducto,
            @RequestParam Integer cantidad,
            HttpSession session) {

        CarritoDTO carrito = carritoService.actualizarCantidad(session, idProducto, cantidad);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping("/items/{idProducto}")
    public ResponseEntity<CarritoDTO> eliminarItem(
            @PathVariable Integer idProducto,
            HttpSession session) {

        CarritoDTO carrito = carritoService.eliminarItem(session, idProducto);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping
    public ResponseEntity<Void> limpiar(HttpSession session) {
        carritoService.limpiarCarrito(session);
        return ResponseEntity.noContent().build();
    }
}