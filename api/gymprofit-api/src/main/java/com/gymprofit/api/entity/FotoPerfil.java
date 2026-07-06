package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
// ============================================================
// FotoPerfil — foto de perfil del usuario persistida en BD (BLOB).
// Vive en tabla propia (1:1 con usuarios, PK = usuario_id) para que las queries
// de Usuario no arrastren el binario. Sustituye al almacenamiento en disco,
// que en Render (filesystem efímero) se perdía en cada redeploy.
// ============================================================
@Table(name = "fotos_perfil")
public class FotoPerfil {

    // PK = id del usuario dueño de la foto (relación 1:1 implícita, sin navegación).
    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    // Bytes de la imagen. columnDefinition explícito: con @Lob a secas el dialecto
    // MariaDB esperaba TINYBLOB en el validate de Hibernate y rompía el arranque.
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] datos;

    // Tipo MIME de la imagen (por ahora siempre image/jpeg desde la app).
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    // Última vez que se subió/reemplazó la foto.
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
