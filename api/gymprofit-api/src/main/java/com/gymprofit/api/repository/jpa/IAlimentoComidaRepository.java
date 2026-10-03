package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AlimentoComida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IAlimentoComidaRepository — acceso JPA a la tabla intermedia alimento-comida
// Gestiona la relación N:M entre alimentos y comidas (cantidad de cada
// alimento dentro de una comida registrada).
// ============================================================
@Repository
public interface IAlimentoComidaRepository extends JpaRepository<AlimentoComida, Integer> {

    // Devuelve todos los alimentos asociados a una comida.
    List<AlimentoComida> findByComidaId(Integer comidaId);

    // Las líneas de varias comidas con su alimento y su ración, en una consulta (lote
    // 1.6.4): las comidas recientes y copiar no hacen un viaje por comida ni por línea.
    @Query("SELECT ac FROM AlimentoComida ac JOIN FETCH ac.alimento LEFT JOIN FETCH ac.racion "
            + "WHERE ac.comida.id IN :comidas ORDER BY ac.id")
    List<AlimentoComida> conAlimento(@Param("comidas") java.util.Collection<Integer> comidas);

    // Devuelve todas las comidas en las que aparece un alimento.
    List<AlimentoComida> findByAlimentoId(Integer alimentoId);

    // Busca la asociación concreta entre una comida y un alimento.
    Optional<AlimentoComida> findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Elimina todos los alimentos asociados a una comida (p. ej. al borrar la comida).
    void deleteByComidaId(Integer comidaId);

    // Borra las líneas de una comida en una sentencia (GP-190): la clave ajena no borra en
    // cascada, y la comida no se puede borrar mientras tenga líneas.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AlimentoComida ac WHERE ac.comida.id = :comida")
    int borrarDeComida(@Param("comida") Integer comida);

    // Elimina la asociación concreta entre una comida y un alimento.
    void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Comprueba si un alimento ya está asociado a una comida.
    boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Cuenta cuántos alimentos tiene registrados una comida.
    Long countByComidaId(Integer comidaId);

    // Cuenta en cuántas comidas aparece un alimento.
    Long countByAlimentoId(Integer alimentoId);
}
