package com.ofimarck.repository;

import com.ofimarck.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByPedido_IdPedido(Long idPedido);
    List<Pago> findByEstadoOrderByCreadoEnDesc(String estado);
}
