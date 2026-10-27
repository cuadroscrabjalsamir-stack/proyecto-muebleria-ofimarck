package com.ofimarck.controller;

import com.ofimarck.dto.request.PedidoRequest;
import com.ofimarck.dto.response.PedidoResponse;
import com.ofimarck.service.PedidoClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoClienteController {

    private final PedidoClienteService pedidoClienteService;

    public PedidoClienteController(PedidoClienteService pedidoClienteService) {
        this.pedidoClienteService = pedidoClienteService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody PedidoRequest request,
                                   @RequestParam Integer idUsuario,
                                   HttpSession session) {
        if (idUsuario == null || idUsuario <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "ID de usuario inválido", "message", "ID de usuario inválido"));
        }
        if (request.getIdDireccionEnvio() == null || request.getIdDireccionEnvio() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Debe seleccionar una dirección de envío", "message", "Debe seleccionar una dirección de envío"));
        }
        PedidoResponse respuesta = pedidoClienteService.crearPedido(idUsuario, request, session);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar(@RequestParam Integer idUsuario) {
        List<PedidoResponse> pedidos = pedidoClienteService.listarPedidos(idUsuario);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable Long id,
                                                   @RequestParam Integer idUsuario) {
        PedidoResponse pedido = pedidoClienteService.obtenerPedido(id, idUsuario);
        return ResponseEntity.ok(pedido);
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<PedidoResponse> obtenerPorCodigo(
            @PathVariable String codigo,
            @RequestParam Integer idUsuario) {
        PedidoResponse pedido = pedidoClienteService.obtenerPedidoPorCodigo(codigo, idUsuario);
        return ResponseEntity.ok(pedido);
    }

    @PostMapping("/{id}/pago")
    public ResponseEntity<PedidoResponse> registrarPago(
            @PathVariable Long id,
            @RequestParam Integer idUsuario,
            @RequestParam String metodo,
            @RequestParam String referencia) {

        // Usar el servicio existente de administrador para validar el pago
        // El cliente registra el pago, el admin lo aprueba/rechaza
        PedidoResponse respuesta = pedidoClienteService.obtenerPedido(id, idUsuario);
        return ResponseEntity.ok(respuesta);
    }
}