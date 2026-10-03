package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// ============================================================
// AlimentosConUltima — alimentos listos para una lista de la app (lotes 1.6.3 y 1.6.4)
//
// Los favoritos (1.6.3) y «Lo que sueles» (1.6.4) enseñan alimentos con sus raciones, su
// última cantidad y si son favoritos, y los dos con el mismo número de consultas sea
// cual sea el número de alimentos: las últimas líneas en una, los favoritos en otra y
// los alimentos con sus raciones en otra.
//
// «La última» es la de la línea más reciente (por la fecha de la comida y, a igualdad,
// la última creada); con un tipo de comida, la más reciente en ese tipo.
// ============================================================
@Component
public class AlimentosConUltima {

    private final IAlimentoRepository alimentoRepository;
    private final AlimentoMapper alimentoMapper;
    private final NamedParameterJdbcTemplate jdbc;

    public AlimentosConUltima(IAlimentoRepository alimentoRepository, AlimentoMapper alimentoMapper, JdbcTemplate jdbc) {
        this.alimentoRepository = alimentoRepository;
        this.alimentoMapper = alimentoMapper;
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    /**
     * Los alimentos con sus raciones y su última cantidad, en el idioma de la petición.
     * No pone {@code favorito}.
     *
     * @param ids       los alimentos; los que no existan no salen.
     * @param usuarioId la cuenta del token.
     * @param tipo      si no es null, la última cantidad es la de ese tipo de comida.
     * @return por id.
     */
    public Map<Integer, AlimentoDTO> pintar(Collection<Integer> ids, Integer usuarioId, TipoComida tipo) {
        Map<Integer, AlimentoDTO> dtos = new HashMap<>();
        if (ids.isEmpty()) return dtos;
        Map<Integer, UltimaCantidad.Linea> ultimas = new HashMap<>();
        MapSqlParameterSource parametros = new MapSqlParameterSource("usuario", usuarioId).addValue("ids", ids)
                .addValue("tipo", tipo == null ? null : tipo.name());
        jdbc.query("""
                SELECT x.alimento_id, x.cantidad_gramos, x.racion_id, x.raciones
                FROM (SELECT ac.alimento_id, ac.cantidad_gramos, ac.racion_id, ac.raciones,
                             ROW_NUMBER() OVER (PARTITION BY ac.alimento_id ORDER BY c.fecha DESC, ac.id DESC) AS n
                      FROM comidas c JOIN alimentos_comida ac ON ac.comida_id = c.id
                      WHERE c.usuario_id = :usuario AND ac.alimento_id IN (:ids)
                        AND (:tipo IS NULL OR c.tipo_comida = :tipo)) x
                WHERE x.n = 1""",
                parametros, rs -> {
                    Number racion = (Number) rs.getObject("racion_id");
                    ultimas.put(rs.getInt("alimento_id"), new UltimaCantidad.Linea(rs.getBigDecimal("cantidad_gramos"),
                            racion == null ? null : racion.intValue(), rs.getBigDecimal("raciones")));
                });
        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        for (Alimento a : alimentoRepository.conRaciones(ids)) {
            AlimentoDTO dto = alimentoMapper.toDTO(a);
            dto.setUltima(UltimaCantidad.de(ultimas.get(a.getId()), a.getRaciones(), ingles));
            dtos.put(a.getId(), dto);
        }
        return dtos;
    }

    /**
     * Cuáles de esos alimentos son favoritos de la cuenta, en una consulta.
     */
    public Set<Integer> favoritos(Collection<Integer> ids, Integer usuarioId) {
        Set<Integer> favoritos = new HashSet<>();
        if (ids.isEmpty()) return favoritos;
        jdbc.query("SELECT alimento_id FROM favoritos WHERE usuario_id = :usuario AND alimento_id IN (:ids)",
                new MapSqlParameterSource("usuario", usuarioId).addValue("ids", ids),
                rs -> {
                    favoritos.add(rs.getInt("alimento_id"));
                });
        return favoritos;
    }
}
