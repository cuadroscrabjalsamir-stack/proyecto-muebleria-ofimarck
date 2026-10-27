package com.ofimarck.repository;

import com.ofimarck.domain.HistorialEstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, Long> {
    List<HistorialEstadoPedido> findByPedido_IdPedidoOrderByFechaDesc(Long idPedido);
}
