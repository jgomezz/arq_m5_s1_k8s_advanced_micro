
package com.tecsup.app.micro.user.infrastructure.persistence.repository;

import com.tecsup.app.micro.user.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA de Rol
 * Sesión 1 - Módulo 4: Seguridad en Microservicios
 */
public interface JpaRoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);
}
