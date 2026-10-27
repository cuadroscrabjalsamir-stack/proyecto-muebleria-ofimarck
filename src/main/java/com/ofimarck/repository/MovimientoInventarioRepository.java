package com.ofimarck.repository;

import com.ofimarck.domain.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findTop50ByOrderByFechaDesc();
    List<MovimientoInventario> findByProducto_IdProductoOrderByFechaDesc(Integer idProducto);
}
