package com.ofimarck.controller;

import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.response.CarritoDTO;
import com.ofimarck.service.CarritoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador MVC puro para renderizar la vista del carrito de compras.
 *
 * <p>Anotado con {@code @Controller}, devuelve nombres de vista Thymeleaf y no
 * cuerpos JSON. Pertenece al lado MVC del patrón híbrido; los endpoints de API
 * REST del carrito residen en {@code CarritoController} bajo el prefijo
 * {@code /api/carrito}.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): este controlador solo
 * gestiona la presentación del carrito. La lógica de agregar, actualizar y
 * eliminar ítems reside en {@link CarritoService}.
 *
 * <p>Uso de Logback/SLF4J: se registran eventos de acceso al carrito para
 * auditoría (sin loguear datos sensibles ni detalles de tarjetas).
 */
@Controller
@RequestMapping("/carrito")
public class CarritoViewController {

    private static final Logger log = LoggerFactory.getLogger(CarritoViewController.class);

    private final CarritoService carritoService;

    public CarritoViewController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /**
     * Renderiza la vista del carrito de compras del usuario en sesión.
     *
     * @param session sesión HTTP que contiene el carrito bajo la clave "CARRITO"
     * @param model   modelo Thymeleaf
     * @return nombre de la vista Thymeleaf {@code carrito}
     */
    @GetMapping
    public String verCarrito(HttpSession session, Model model) {
        CarritoDTO carritoDTO = carritoService.obtenerCarrito(session);

        // Obtener usuario de la sesión para mostrar nombre (si está autenticado)
        Usuario usuario = (Usuario) session.getAttribute("USUARIO_LOGUEADO");
        if (usuario != null) {
            model.addAttribute("nombreUsuario", usuario.getNombres());
            model.addAttribute("rolUsuario", usuario.getRol().getNombreRol());
        }

        model.addAttribute("carrito", carritoDTO);

        log.debug("carrito_vista_renderizada items={} totalItems={} total={}",
                carritoDTO.getItems() != null ? carritoDTO.getItems().size() : 0,
                carritoDTO.getTotalItems(), carritoDTO.getTotal());

        return "carrito";
    }
}
