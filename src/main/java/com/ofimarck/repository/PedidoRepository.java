package com.ofimarck.repository;

import com.ofimarck.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findByOrderByFechaPedidoDesc();

    List<Pedido> findByEstadoOrderByFechaPedidoDesc(String estado);

    List<Pedido> findByUsuario_IdUsuarioOrderByFechaPedidoDesc(Integer idUsuario);

    List<Pedido> findByEstadoAndUsuario_IdUsuarioOrderByFechaPedidoDesc(String estado, Integer idUsuario);

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado = 'PENDIENTE'")
    long countPendientes();

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado IN ('PAGADO', 'EN_PREPARACION', 'EN_TRANSITO', 'ENTREGADO')")
    long countPagadosValidos();
}
