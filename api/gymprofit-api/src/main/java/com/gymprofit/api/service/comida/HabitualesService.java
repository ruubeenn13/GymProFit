package com.gymprofit.api.service.comida;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.service.alimentocomida.AlimentosConUltima;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// ============================================================
// HabitualesService — «Lo que sueles merendar» (lote 1.6.4, A3)
//
// Para un tipo de comida, hasta tres alimentos que la cuenta ha apuntado en ese tipo en
// dos días distintos o más de los últimos 60 (hoy incluido): del más repetido —en días,
// no en líneas— al menos y, a igualdad, el apuntado más recientemente. Un solo día no es
// costumbre, aunque se apuntara dos veces. Solo lo que la cuenta ve y está activo.
//
// Cada uno con sus raciones, si es favorito y su última cantidad, que aquí es la última
// en ese tipo de comida: lo que añade el «+». Tres consultas, sean cuantos sean.
// ============================================================
@Service
public class HabitualesService {

    /** Los días que se miran, hoy incluido. */
    static final int DIAS = 60;
    /** Días distintos para que sea costumbre. */
    static final int DIAS_MINIMOS = 2;
    /** Como mucho, estos. */
    static final int MAXIMO = 3;

    private final SecurityUtils securityUtils;
    private final AlimentosConUltima alimentosConUltima;
    private final JdbcTemplate jdbc;

    public HabitualesService(SecurityUtils securityUtils, AlimentosConUltima alimentosConUltima, JdbcTemplate jdbc) {
        this.securityUtils = securityUtils;
        this.alimentosConUltima = alimentosConUltima;
        this.jdbc = jdbc;
    }

    /**
     * Lo que la cuenta suele tomar en ese tipo de comida.
     *
     * @param tipoComida DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA o SNACK.
     * @return de 0 a 3 alimentos, del más repetido al menos.
     * @throws com.gymprofit.api.exceptions.InvalidDataException (400) si el tipo no existe.
     */
    @Transactional(readOnly = true)
    public List<AlimentoDTO> habituales(String tipoComida) {
        TipoComida tipo = AnadirAlimentoService.tipo(tipoComida);
        Integer usuarioId = securityUtils.getCurrentUserId();
        List<Integer> ids = jdbc.queryForList("""
                SELECT ac.alimento_id
                FROM comidas c
                JOIN alimentos_comida ac ON ac.comida_id = c.id
                JOIN alimentos a ON a.id = ac.alimento_id
                WHERE c.usuario_id = ? AND c.tipo_comida = ? AND c.fecha >= ? AND a.activo = 1
                  AND (a.usuario_id IS NULL OR a.usuario_id = ?)
                GROUP BY ac.alimento_id
                HAVING COUNT(DISTINCT DATE(c.fecha)) >= ?
                ORDER BY COUNT(DISTINCT DATE(c.fecha)) DESC, MAX(c.fecha) DESC, MAX(ac.id) DESC
                LIMIT ?""", Integer.class,
                usuarioId, tipo.name(), Timestamp.valueOf(LocalDate.now().minusDays(DIAS - 1L).atStartOfDay()),
                usuarioId, DIAS_MINIMOS, MAXIMO);
        if (ids.isEmpty()) return List.of();

        Map<Integer, AlimentoDTO> dtos = alimentosConUltima.pintar(ids, usuarioId, tipo);
        Set<Integer> favoritos = alimentosConUltima.favoritos(ids, usuarioId);
        return ids.stream()
                .map(dtos::get)
                .filter(Objects::nonNull)
                .map(d -> {
                    d.setFavorito(favoritos.contains(d.getId()));
                    return d;
                })
                .toList();
    }
}
