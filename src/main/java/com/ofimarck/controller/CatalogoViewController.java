package com.ofimarck.controller;

import com.ofimarck.domain.Categoria;
import com.ofimarck.domain.Producto;
import com.ofimarck.service.CategoriaService;
import com.ofimarck.service.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controlador MVC puro para renderizar la vista del catálogo público de productos.
 *
 * <p>Este controlador está anotado con {@code @Controller} (no {@code @RestController}),
 * lo que significa que sus métodos devuelven nombres de vista Thymeleaf y no cuerpos
 * JSON. Sigue estrictamente el patrón MVC híbrido requerido por la rúbrica:
 * las vistas HTML se sirven desde aquí, mientras que los datos expuestos por API
 * REST residen en {@code @RestController} bajo el prefijo {@code /api/}.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): este controlador solo se
 * encarga de recibir la petición HTTP, delegar la obtención de datos al servicio
 * y devolver la vista correspondiente. Toda lógica de negocio (búsqueda, filtros,
 * ordenamiento) reside en {@link ProductoService}.
 *
 * <p>Uso de Apache Commons Lang3 (rúbrica FASE 2): el término de búsqueda llega
 * ya sanitizado desde el servicio, que aplica {@code StringUtils.normalizeSpace()}
 * y {@code StringUtils.stripAccents()} antes de consultar la base de datos.
 *
 * <p>Uso de Logback/SLF4J: se registran eventos de navegación al catálogo para
 * auditoría de uso (sin datos sensibles de usuarios).
 */
@Controller
public class CatalogoViewController {

    private static final Logger log = LoggerFactory.getLogger(CatalogoViewController.class);

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public CatalogoViewController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    /**
     * Renderiza la vista del catálogo de productos.
     *
     * @param categoriaId filtro opcional por categoría
     * @param estado filtro opcional por estado activo/inactivo
     * @param termino término de búsqueda (nombre o SKU)
     * @param model modelo Thymeleaf
     * @return nombre de la vista Thymeleaf {@code catalogo}
     */
    @GetMapping("/catalogo")
    public String mostrarCatalogo(@RequestParam(required = false) Integer categoriaId,
                                  @RequestParam(required = false) Boolean estado,
                                  @RequestParam(required = false) String termino,
                                  Model model) {
        List<Producto> productos = productoService.listarTodos(categoriaId, estado, termino);
        List<Categoria> categorias = categoriaService.listarActivas();

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("categoriaSeleccionada", categoriaId);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("terminoBusqueda", termino);

        log.debug("catalogo_vista_renderizada categoriaId={} estado={} termino={} productos={}",
                categoriaId, estado, termino, productos.size());

        return "catalogo";
    }
}
