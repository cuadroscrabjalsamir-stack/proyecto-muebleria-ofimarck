package com.ofimarck.repository;

import com.ofimarck.domain.DireccionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DireccionEnvioRepository extends JpaRepository<DireccionEnvio, Integer> {
    List<DireccionEnvio> findByUsuario_IdUsuarioAndEstadoTrue(Integer idUsuario);
    Optional<DireccionEnvio> findByUsuario_IdUsuarioAndPredeterminadaTrueAndEstadoTrue(Integer idUsuario);
}
