package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.PasoAltaDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// IPasoAltaDiaRepository — el recuento del embudo del alta (GP-019)
// ============================================================
@Repository
public interface IPasoAltaDiaRepository extends JpaRepository<PasoAltaDia, PasoAltaDia.Clave> {

    // Crea la fila del día con 1 o le suma 1, en una sola sentencia: dos altas que llegan
    // al mismo paso a la vez no chocan con la clave primaria ni se pierde ninguna.
    @Modifying
    @Query(value = """
            INSERT INTO alta_pasos (dia, paso, total) VALUES (:dia, :paso, 1)
            ON DUPLICATE KEY UPDATE total = total + 1""", nativeQuery = true)
    void sumar(LocalDate dia, String paso);

    /** Las filas de los días de {@code desde} a {@code hasta}, los dos incluidos. */
    @Query("SELECT p FROM PasoAltaDia p WHERE p.clave.dia >= :desde AND p.clave.dia <= :hasta")
    List<PasoAltaDia> entre(LocalDate desde, LocalDate hasta);
}
