package com.ofimarck.controller;

import com.ofimarck.domain.Categoria;
import com.ofimarck.dto.response.CategoriaDTO;
import com.ofimarck.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categorias")
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    public AdminCategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<Categoria>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivas) {
        if (soloActivas) {
            return ResponseEntity.ok(categoriaService.listarActivas());
        }
        return ResponseEntity.ok(categoriaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody CategoriaDTO dto) {
        Categoria nueva = categoriaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizar(@PathVariable Integer id, @Valid @RequestBody CategoriaDTO dto) {
        return ResponseEntity.ok(categoriaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Categoria> cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) {
        Boolean estado = body.getOrDefault("estado", true);
        return ResponseEntity.ok(categoriaService.cambiarEstado(id, estado));
    }
}
