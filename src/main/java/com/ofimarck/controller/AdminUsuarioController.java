package com.ofimarck.controller;

import com.ofimarck.domain.Usuario;
import com.ofimarck.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Usuario> cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) {
        Boolean estado = body.getOrDefault("estado", true);
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, estado));
    }
}
