package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AguaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

// ============================================================
// IAguaDiariaRepository — el agua de cada cuenta, por día (lote 1.6.8)
// ============================================================
@Repository
public interface IAguaDiariaRepository extends JpaRepository<AguaDiaria, Integer> {

    // Crea o pisa la fila del día en una sola sentencia: dos vasos tocados a la vez desde
    // dos móviles no chocan con la clave única, gana el último.
    @Modifying
    @Query(value = """
            INSERT INTO agua_diaria (usuario_id, fecha, mililitros, actualizado)
            VALUES (:usuarioId, :fecha, :mililitros, NOW())
            ON DUPLICATE KEY UPDATE mililitros = :mililitros, actualizado = NOW()""", nativeQuery = true)
    void guardar(Integer usuarioId, LocalDate fecha, int mililitros);
}
