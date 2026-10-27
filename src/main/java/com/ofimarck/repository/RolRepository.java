package com.ofimarck.repository;

import com.ofimarck.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Short> {
    Optional<Rol> findByNombreRol(String nombreRol);
}
