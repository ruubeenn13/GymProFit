package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ComidaHoyNo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

// ============================================================
// IComidaHoyNoRepository — las comidas marcadas «Hoy no» de cada cuenta (lote 1.6.8)
// Marcar y quitar son repetibles: ni duplican ni fallan.
// ============================================================
@Repository
public interface IComidaHoyNoRepository extends JpaRepository<ComidaHoyNo, Integer> {

    @Modifying
    @Query(value = """
            INSERT INTO comidas_hoy_no (usuario_id, fecha, tipo_comida, creado)
            VALUES (:usuarioId, :fecha, :tipo, NOW())
            ON DUPLICATE KEY UPDATE id = id""", nativeQuery = true)
    void marcar(Integer usuarioId, LocalDate fecha, String tipo);

    @Modifying
    @Query(value = "DELETE FROM comidas_hoy_no WHERE usuario_id = :usuarioId AND fecha = :fecha AND tipo_comida = :tipo",
            nativeQuery = true)
    void quitar(Integer usuarioId, LocalDate fecha, String tipo);
}
