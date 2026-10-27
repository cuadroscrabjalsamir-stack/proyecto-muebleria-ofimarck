package com.ofimarck.mapper;

import com.ofimarck.domain.Pedido;
import com.ofimarck.dto.response.PedidoResponse;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {

    public PedidoResponse toDto(Pedido pedido) {
        return new PedidoResponse(
                pedido.getIdPedido(),
                pedido.getCodigo(),
                pedido.getTotal(),
                pedido.getEstado(),
                pedido.getFechaPedido()
        );
    }
}