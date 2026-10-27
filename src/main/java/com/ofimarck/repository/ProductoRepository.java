package com.ofimarck.repository;

import com.ofimarck.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    List<Producto> findByOrderByFechaCreacionDesc();

    List<Producto> findByCategoria_IdCategoriaOrderByFechaCreacionDesc(Integer idCategoria);

    @Query("SELECT p FROM Producto p WHERE lower(p.nombre) LIKE lower(concat('%', :termino, '%')) OR lower(p.sku) LIKE lower(concat('%', :termino, '%')) ORDER BY p.idProducto DESC")
    List<Producto> buscarPorTermino(@Param("termino") String termino);

    @Query("SELECT p FROM Producto p WHERE p.categoria.idCategoria = :idCategoria AND (lower(p.nombre) LIKE lower(concat('%', :termino, '%')) OR lower(p.sku) LIKE lower(concat('%', :termino, '%'))) ORDER BY p.idProducto DESC")
    List<Producto> buscarPorCategoriaYTermino(@Param("idCategoria") Integer idCategoria, @Param("termino") String termino);

    @Query("SELECT p FROM Producto p WHERE p.estado = true AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findAlertasStock();
}
