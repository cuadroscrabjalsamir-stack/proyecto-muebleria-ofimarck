package com.ofimarck.controller;

import com.ofimarck.domain.Envio;
import com.ofimarck.domain.HistorialEstadoPedido;
import com.ofimarck.domain.Pago;
import com.ofimarck.domain.Pedido;
import com.ofimarck.dto.request.CambioEstadoPedidoDTO;
import com.ofimarck.dto.request.EnvioUpdateDTO;
import com.ofimarck.dto.response.ValidacionPagoDTO;
import com.ofimarck.service.PedidoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminPedidoController {

    private final PedidoService pedidoService;

    public AdminPedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/pedidos")
    public ResponseEntity<List<Pedido>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer clienteId) {
        return ResponseEntity.ok(pedidoService.listar(estado, clienteId));
    }

    @GetMapping("/pedidos/{id}")
    public ResponseEntity<Pedido> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @GetMapping("/pedidos/{id}/historial")
    public ResponseEntity<List<HistorialEstadoPedido>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerHistorial(id));
    }

    @PatchMapping("/pedidos/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoPedidoDTO dto,
            HttpServletRequest request) {
        Integer idUsuario = obtenerIdUsuarioLogueado(request);
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, dto, idUsuario));
    }

    @PutMapping("/pedidos/{id}/envio")
    public ResponseEntity<Envio> actualizarEnvio(
            @PathVariable Long id,
            @Valid @RequestBody EnvioUpdateDTO dto) {
        return ResponseEntity.ok(pedidoService.actualizarEnvio(id, dto));
    }

    @PatchMapping("/pagos/{id}/validar")
    public ResponseEntity<Pago> validarPago(
            @PathVariable Long id,
            @Valid @RequestBody ValidacionPagoDTO dto,
            HttpServletRequest request) {
        Integer idUsuario = obtenerIdUsuarioLogueado(request);
        return ResponseEntity.ok(pedidoService.validarPago(id, dto, idUsuario));
    }

    private Integer obtenerIdUsuarioLogueado(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ID_USUARIO") != null) {
            return (Integer) session.getAttribute("ID_USUARIO");
        }
        return null;
    }
}
