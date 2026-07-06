package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.FotoPerfil;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

// ============================================================
// IFotoPerfilRepository — repositorio JPA de las fotos de perfil (BLOB en BD).
// PK = usuario_id, así que el CRUD estándar de JpaRepository basta (findById/save
// hacen el upsert por usuario). No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IFotoPerfilRepository extends JpaRepository<FotoPerfil, Integer> {
}
