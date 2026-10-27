package com.ofimarck.controller;

import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.ProductoPublicoDTO;
import com.ofimarck.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
public class ProductoPublicoController {

    private final ProductoService productoService;

    public ProductoPublicoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoPublicoDTO>> listar(
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) String termino) {

        List<Producto> productos = productoService.listarTodos(categoriaId, true, termino);
        List<ProductoPublicoDTO> dtos = productos.stream()
                .map(ProductoPublicoDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoPublicoDTO> obtener(@PathVariable Integer id) {
        Producto producto = productoService.buscarPorId(id);
        if (!producto.getEstado()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ProductoPublicoDTO(producto));
    }
}