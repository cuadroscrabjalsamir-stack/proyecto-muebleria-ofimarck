package com.ofimarck.controller;

import com.ofimarck.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminViewController {

    @GetMapping({"/", "/admin", "/admin/"})
    public String adminDashboard(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USUARIO_LOGUEADO") == null) {
            return "redirect:/login";
        }
        Usuario usuario = (Usuario) session.getAttribute("USUARIO_LOGUEADO");
        if (!"ADMIN".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            return "redirect:/usuario";
        }
        return "admin";
    }
}
