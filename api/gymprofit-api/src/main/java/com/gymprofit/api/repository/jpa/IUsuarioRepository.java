package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Usuario;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IUsuarioRepository — repositorio JPA de usuarios
// Acceso a los usuarios de la aplicación, usado en autenticación (búsqueda
// por username/email) y en la gestión de cuentas activas. No exportado como REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IUsuarioRepository extends CrudRepository<Usuario, Integer> {

    // Busca un usuario por su nombre de usuario (login).
    Optional<Usuario> findByUsername(String username);

    // Busca un usuario por su email.
    Optional<Usuario> findByEmail(String email);

    // Comprueba si ya existe un usuario con ese username.
    Boolean existsByUsername(String username);

    // Comprueba si ya existe un usuario con ese email.
    Boolean existsByEmail(String email);

    // Usuarios con la cuenta activa.
    List<Usuario> findByActivoTrue();
}