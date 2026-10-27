package com.ofimarck.controller;

import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.ForgotPasswordRequest;
import com.ofimarck.dto.request.LoginRequest;
import com.ofimarck.dto.request.ResetPasswordRequest;
import com.ofimarck.dto.response.LoginResponse;
import com.ofimarck.service.PasswordResetService;
import com.ofimarck.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final PasswordResetService passwordResetService;

    public AuthController(UsuarioService usuarioService,
                          PasswordResetService passwordResetService) {
        this.usuarioService = usuarioService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorCorreo(request.getCorreo());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Credenciales incorrectas", null));
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.getEstado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new LoginResponse(false, "La cuenta se encuentra inactiva", null));
        }

        if (!usuarioService.verificarPassword(request.getPassword(), usuario.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Credenciales incorrectas", null));
        }

        usuarioService.registrarAcceso(usuario.getIdUsuario());
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("USUARIO_LOGUEADO", usuario);
        session.setAttribute("ID_USUARIO", usuario.getIdUsuario());
        session.setAttribute("ROL", usuario.getRol().getNombreRol());

        return ResponseEntity.ok(new LoginResponse(true, "Inicio de sesión exitoso", usuario));
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("USUARIO_LOGUEADO") != null) {
            Usuario usuario = (Usuario) session.getAttribute("USUARIO_LOGUEADO");
            return ResponseEntity.ok(usuario);
        }
        // Retornar información del administrador por defecto para navegación del panel
        Optional<Usuario> adminOpt = usuarioService.buscarPorCorreo("admin@ofimarck.pe");
        return adminOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.solicitarRestablecimiento(request.getCorreo());
        return ResponseEntity.ok(Map.of("mensaje", "Si el correo existe, se ha enviado un token de restablecimiento"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.restablecerContrasena(request);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña restablecida con éxito"));
    }
}
