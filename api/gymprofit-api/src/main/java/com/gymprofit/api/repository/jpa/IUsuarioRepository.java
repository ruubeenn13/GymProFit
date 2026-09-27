package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
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
public interface IUsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Busca un usuario por su nombre de usuario (login).
    Optional<Usuario> findByUsername(String username);

    // Busca un usuario por su email.
    Optional<Usuario> findByEmail(String email);

    // Comprueba si ya existe un usuario con ese username.
    Boolean existsByUsername(String username);

    // Comprueba si ya existe un usuario con ese email.
    Boolean existsByEmail(String email);

    // ¿Usa este correo alguna cuenta que no sea la indicada? (cambio de correo, GP-083)
    boolean existsByEmailAndIdNot(String email, Integer id);

    // Usuarios con la cuenta activa.
    List<Usuario> findByActivoTrue();

    // Apunta el último acceso (GP-085) sin cargar la entidad ni tocar el resto de campos.
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.ultimoAcceso = :ahora WHERE u.id = :id")
    void registrarAcceso(@Param("id") Integer id, @Param("ahora") LocalDateTime ahora);

    /**
     * Cuentas para la web de administración (GP-085): búsqueda por usuario o correo y
     * filtros opcionales; un parámetro null no filtra.
     *
     * @param patron  "%texto%" en minúsculas, o null
     * @param rol     rol exacto, o null
     * @param activo  estado, o null
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:patron IS NULL OR LOWER(u.username) LIKE :patron OR LOWER(u.email) LIKE :patron)
              AND (:activo IS NULL OR u.activo = :activo)
              AND (:rol IS NULL OR EXISTS (SELECT r FROM Usuario u2 JOIN u2.roles r WHERE u2 = u AND r.nombre = :rol))
            """)
    Page<Usuario> buscarParaAdmin(@Param("patron") String patron, @Param("rol") RoleType rol,
                                  @Param("activo") Boolean activo, Pageable pageable);
}