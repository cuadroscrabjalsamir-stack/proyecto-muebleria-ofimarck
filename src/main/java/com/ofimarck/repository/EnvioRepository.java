package com.ofimarck.repository;

import com.ofimarck.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByPedido_IdPedido(Long idPedido);
    Optional<Envio> findByTracking(String tracking);
}
