package com.ofimarck.controller;

import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.LoginRequest;
import com.ofimarck.dto.request.RegistroRequest;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping
public class UsuarioViewController {

    private final UsuarioService usuarioService;

    public UsuarioViewController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String mostrarLogin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("USUARIO_LOGUEADO") != null) {
            Usuario usuario = (Usuario) session.getAttribute("USUARIO_LOGUEADO");
            if ("ADMIN".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
                return "redirect:/admin";
            }
            return "redirect:/usuario";
        }
        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(@ModelAttribute @Valid LoginRequest loginRequest,
                                BindingResult result,
                                HttpServletRequest httpRequest,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("error", "Correo y contraseña son obligatorios");
            model.addAttribute("correo", loginRequest.getCorreo());
            return "login";
        }

        Optional<Usuario> usuarioOpt = usuarioService.buscarPorCorreo(loginRequest.getCorreo());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "Credenciales incorrectas");
            model.addAttribute("correo", loginRequest.getCorreo());
            return "login";
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.getEstado()) {
            model.addAttribute("error", "La cuenta se encuentra inactiva");
            model.addAttribute("correo", loginRequest.getCorreo());
            return "login";
        }

        if (!usuarioService.verificarPassword(loginRequest.getPassword(), usuario.getPasswordHash())) {
            model.addAttribute("error", "Credenciales incorrectas");
            model.addAttribute("correo", loginRequest.getCorreo());
            return "login";
        }

        usuarioService.registrarAcceso(usuario.getIdUsuario());
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("USUARIO_LOGUEADO", usuario);
        session.setAttribute("ID_USUARIO", usuario.getIdUsuario());
        session.setAttribute("ROL", usuario.getRol().getNombreRol());

        String rol = usuario.getRol().getNombreRol();
        if ("ADMIN".equalsIgnoreCase(rol)) {
            return "redirect:/admin";
        }
        return "redirect:/usuario";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroRequest", new RegistroRequest());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute @Valid RegistroRequest registroRequest,
                            BindingResult result,
                            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("error", "Corrija los errores del formulario");
            return "registro";
        }

        try {
            usuarioService.registrarCliente(
                    registroRequest.getNombres(),
                    registroRequest.getApellidos(),
                    registroRequest.getCorreo(),
                    registroRequest.getTelefono(),
                    registroRequest.getPassword()
            );
            model.addAttribute("exito", "Cuenta creada exitosamente. Ya puede iniciar sesión.");
            return "redirect:/login";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/usuario")
    public String panelUsuario(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USUARIO_LOGUEADO") == null) {
            return "redirect:/login";
        }
        Usuario usuario = (Usuario) session.getAttribute("USUARIO_LOGUEADO");
        model.addAttribute("usuario", usuario);
        return "usuario";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
