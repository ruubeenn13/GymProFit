package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.FotoPerfil;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

// ============================================================
// IFotoPerfilRepository — repositorio JPA de las fotos de perfil (BLOB en BD).
// PK = usuario_id. Guardar y leer van sin cargar la entidad (GP-188): sustituir una foto
// no lee la anterior (y Hibernate no guarda otra copia para compararla), y servirla lee
// solo los bytes. No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IFotoPerfilRepository extends JpaRepository<FotoPerfil, Integer> {

    /** Sustituye la foto del usuario sin leer la anterior; 0 si no tenía. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE FotoPerfil f SET f.datos = :datos, f.contentType = :tipo, "
            + "f.fechaActualizacion = :fecha WHERE f.usuarioId = :usuario")
    int sustituir(@org.springframework.data.repository.query.Param("usuario") Integer usuario,
                  @org.springframework.data.repository.query.Param("datos") byte[] datos,
                  @org.springframework.data.repository.query.Param("tipo") String tipo,
                  @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha);

    /** La primera foto del usuario, sin pasar por merge (que leería antes la fila); si ya hay, la sustituye. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO fotos_perfil (usuario_id, datos, content_type, "
            + "fecha_actualizacion) VALUES (:usuario, :datos, :tipo, :fecha) ON DUPLICATE KEY UPDATE datos = VALUES(datos), "
            + "content_type = VALUES(content_type), fecha_actualizacion = VALUES(fecha_actualizacion)", nativeQuery = true)
    int insertar(@org.springframework.data.repository.query.Param("usuario") Integer usuario,
                 @org.springframework.data.repository.query.Param("datos") byte[] datos,
                 @org.springframework.data.repository.query.Param("tipo") String tipo,
                 @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha);

    /** Solo los bytes de la foto, sin la entidad. */
    @org.springframework.data.jpa.repository.Query("SELECT f.datos FROM FotoPerfil f WHERE f.usuarioId = :usuario")
    java.util.Optional<byte[]> datos(@org.springframework.data.repository.query.Param("usuario") Integer usuario);
}
