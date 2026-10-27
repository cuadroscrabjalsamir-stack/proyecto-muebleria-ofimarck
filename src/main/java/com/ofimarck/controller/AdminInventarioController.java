package com.ofimarck.controller;

import com.ofimarck.domain.MovimientoInventario;
import com.ofimarck.domain.Producto;
import com.ofimarck.dto.request.AjusteInventarioDTO;
import com.ofimarck.service.InventarioService;
import com.ofimarck.service.ProductoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventario")
public class AdminInventarioController {

    private final InventarioService inventarioService;
    private final ProductoService productoService;

    public AdminInventarioController(InventarioService inventarioService, ProductoService productoService) {
        this.inventarioService = inventarioService;
        this.productoService = productoService;
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoInventario>> listarMovimientos() {
        return ResponseEntity.ok(inventarioService.listarMovimientosRecientes());
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<Producto>> listarAlertas() {
        return ResponseEntity.ok(productoService.listarAlertasStock());
    }

    @PostMapping("/ajustes")
    public ResponseEntity<MovimientoInventario> registrarAjuste(
            @Valid @RequestBody AjusteInventarioDTO dto,
            HttpServletRequest request) {
        Integer idUsuario = null;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ID_USUARIO") != null) {
            idUsuario = (Integer) session.getAttribute("ID_USUARIO");
        }
        MovimientoInventario mov = inventarioService.registrarAjuste(dto, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(mov);
    }
}
