package com.ofimarck.controller;

import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.response.LoginResponse;
import com.ofimarck.dto.request.RegistroRequest;
import com.ofimarck.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ClienteAuthController {

    private final UsuarioService usuarioService;

    public ClienteAuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest request) {
        if (usuarioService.buscarPorCorreo(request.getCorreo()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya se encuentra registrado"));
        }

        Usuario u = usuarioService.registrarCliente(
                request.getNombres(),
                request.getApellidos(),
                request.getCorreo(),
                request.getTelefono(),
                request.getPassword()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LoginResponse(true, "Usuario registrado exitosamente", u));
    }
}