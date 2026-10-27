package com.ofimarck.controller;

import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.ProductoDTO;
import com.ofimarck.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/productos")
public class AdminProductoController {

    private final ProductoService productoService;

    public AdminProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listar(
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(required = false) String termino) {
        return ResponseEntity.ok(productoService.listarTodos(categoriaId, estado, termino));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoDTO dto) {
        Producto nuevo = productoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Integer id, @Valid @RequestBody ProductoDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Producto> cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) {
        Boolean estado = body.getOrDefault("estado", true);
        return ResponseEntity.ok(productoService.cambiarEstado(id, estado));
    }
}
