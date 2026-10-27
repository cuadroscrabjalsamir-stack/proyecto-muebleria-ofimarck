package com.ofimarck.repository;

import com.ofimarck.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByEstadoTrueOrderByNombreAsc();

    Optional<Categoria> findBySlug(String slug);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsBySlugIgnoreCase(String slug);
}
