package com.gymprofit.api.service.favorito;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.FavoritosDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.PropuestaFavorito;
import com.gymprofit.api.exceptions.UnauthorizedException;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IFavoritoRepository;
import com.gymprofit.api.service.alimentocomida.AlimentosConUltima;
import com.gymprofit.api.service.codigo.CodigoBarrasService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// ============================================================
// FavoritoService — los favoritos de cada cuenta y su propuesta (lote 1.6.3, A3 y A4)
//
// Un favorito es de la cuenta del token (DEC-013) y de un alimento que esa cuenta puede
// ver: del catálogo o suyo. El id llega en la ruta y lo que manda es su dueño (DEC-027):
// el alimento propio de otro es un 403, y uno que no existe, el mismo 403, para no decir
// si existe. Marcar y desmarcar son repetibles: marcar dos veces deja un favorito, y
// quitar lo que no estaba no falla.
//
// La propuesta (decisión 6 del lienzo): como mucho un alimento que no es favorito, que
// no se ha propuesto ya y que aparece en 4 comidas o más de los últimos 14 días; si hay
// varios, el más usado. «Ya propuesto» queda en propuestas_favorito: lo que se rechazó y
// lo que alguna vez fue favorito, aunque se quitara. Así sale una sola vez.
// ============================================================
@Service
@Transactional(readOnly = true)
public class FavoritoService {

    /** Los favoritos se ordenan por las comidas en que aparecen en estos días, hoy incluido. */
    static final int DIAS_USO = 60;
    /** La propuesta mira estos días, hoy incluido... */
    static final int DIAS_PROPUESTA = 14;
    /** ...y pide al menos estas comidas. */
    static final int VECES_PROPUESTA = 4;

    private final IFavoritoRepository favoritoRepository;
    private final IAlimentoRepository alimentoRepository;
    private final AlimentoMapper alimentoMapper;
    private final CodigoBarrasService codigoBarrasService;
    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbc;
    private final AlimentosConUltima alimentosConUltima;

    public FavoritoService(IFavoritoRepository favoritoRepository, IAlimentoRepository alimentoRepository,
                           AlimentoMapper alimentoMapper, CodigoBarrasService codigoBarrasService,
                           SecurityUtils securityUtils, JdbcTemplate jdbc, AlimentosConUltima alimentosConUltima) {
        this.favoritoRepository = favoritoRepository;
        this.alimentoRepository = alimentoRepository;
        this.alimentoMapper = alimentoMapper;
        this.codigoBarrasService = codigoBarrasService;
        this.securityUtils = securityUtils;
        this.jdbc = jdbc;
        this.alimentosConUltima = alimentosConUltima;
    }

    /**
     * Marca el alimento como favorito de la cuenta. Repetible.
     *
     * @return el alimento, con {@code favorito} a true.
     * @throws UnauthorizedException (403) si no existe o es propio de otra cuenta.
     */
    @Transactional
    public AlimentoDTO marcar(Integer alimentoId) {
        Alimento alimento = visible(alimentoId);
        guardar(alimento.getId());
        AlimentoDTO dto = alimentoMapper.toDTO(alimento);
        dto.setFavorito(true);
        return dto;
    }

    /**
     * Marca por su código un producto que puede no estar aún en el catálogo: se
     * materializa como al añadirlo (CodigoBarrasService, con su cupo). Repetible.
     *
     * @return el alimento con su id y {@code favorito} a true.
     */
    @Transactional
    public AlimentoDTO marcarPorCodigo(String codigo) {
        AlimentoDTO dto = codigoBarrasService.porCodigo(codigo);
        guardar(dto.getId());
        dto.setFavorito(true);
        return dto;
    }

    /**
     * Quita el favorito. Repetible: si no lo era, no pasa nada.
     *
     * @throws UnauthorizedException (403) si el alimento no existe o es propio de otra cuenta.
     */
    @Transactional
    public void quitar(Integer alimentoId) {
        visible(alimentoId);
        favoritoRepository.quitar(securityUtils.getCurrentUserId(), alimentoId);
    }

    /**
     * Rechaza la propuesta de ese alimento: no se vuelve a proponer a esta cuenta. Repetible.
     *
     * @throws UnauthorizedException (403) si el alimento no existe o es propio de otra cuenta.
     */
    @Transactional
    public void rechazar(Integer alimentoId) {
        visible(alimentoId);
        cerrarPropuesta(alimentoId, PropuestaFavorito.RECHAZADA);
    }

    /** Pone en el alimento si es favorito de la cuenta que pregunta. */
    public AlimentoDTO conFavorito(AlimentoDTO dto) {
        if (dto != null && dto.getId() != null) {
            dto.setFavorito(favoritoRepository.existsByUsuarioIdAndAlimentoId(securityUtils.getCurrentUserId(),
                    dto.getId()));
        }
        return dto;
    }

    /** Los favoritos de la cuenta, por uso, y la propuesta si la hay. */
    public FavoritosDTO lista() {
        Integer usuarioId = securityUtils.getCurrentUserId();
        LocalDate hoy = LocalDate.now();

        // Los favoritos que se ven (activos y del catálogo o suyos), con su uso.
        record Uso(int id, int veces, LocalDateTime ultimo) {
        }
        List<Uso> usos = jdbc.query("""
                SELECT f.alimento_id,
                       (SELECT COUNT(DISTINCT c.id) FROM alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id
                        WHERE ac.alimento_id = f.alimento_id AND c.usuario_id = ? AND c.fecha >= ?) AS veces,
                       (SELECT MAX(c.fecha) FROM alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id
                        WHERE ac.alimento_id = f.alimento_id AND c.usuario_id = ?) AS ultimo
                FROM favoritos f JOIN alimentos a ON a.id = f.alimento_id
                WHERE f.usuario_id = ? AND a.activo = 1 AND (a.usuario_id IS NULL OR a.usuario_id = ?)""",
                (rs, i) -> {
                    Timestamp ultimo = rs.getTimestamp("ultimo");
                    return new Uso(rs.getInt("alimento_id"), rs.getInt("veces"),
                            ultimo == null ? null : ultimo.toLocalDateTime());
                },
                usuarioId, desde(hoy, DIAS_USO), usuarioId, usuarioId, usuarioId);

        record Propuesta(int id, int veces) {
        }
        List<Propuesta> propuestas = jdbc.query("""
                SELECT ac.alimento_id, COUNT(DISTINCT c.id) AS veces
                FROM comidas c
                JOIN alimentos_comida ac ON ac.comida_id = c.id
                JOIN alimentos a ON a.id = ac.alimento_id
                WHERE c.usuario_id = ? AND c.fecha >= ? AND a.activo = 1
                  AND (a.usuario_id IS NULL OR a.usuario_id = ?)
                  AND NOT EXISTS (SELECT 1 FROM favoritos f WHERE f.usuario_id = ? AND f.alimento_id = ac.alimento_id)
                  AND NOT EXISTS (SELECT 1 FROM propuestas_favorito p
                                  WHERE p.usuario_id = ? AND p.alimento_id = ac.alimento_id)
                GROUP BY ac.alimento_id
                HAVING COUNT(DISTINCT c.id) >= ?
                ORDER BY veces DESC, MAX(c.fecha) DESC, MAX(ac.id) DESC
                LIMIT 1""",
                (rs, i) -> new Propuesta(rs.getInt("alimento_id"), rs.getInt("veces")),
                usuarioId, desde(hoy, DIAS_PROPUESTA), usuarioId, usuarioId, usuarioId, VECES_PROPUESTA);

        Set<Integer> ids = new LinkedHashSet<>();
        usos.forEach(u -> ids.add(u.id()));
        propuestas.forEach(p -> ids.add(p.id()));
        Map<Integer, AlimentoDTO> dtos = alimentosConUltima.pintar(ids, usuarioId, null);

        Map<Integer, Uso> usoDe = new HashMap<>();
        usos.forEach(u -> usoDe.put(u.id(), u));
        List<AlimentoDTO> favoritos = new ArrayList<>(usos.stream()
                .map(u -> dtos.get(u.id())).filter(Objects::nonNull).toList());
        favoritos.forEach(d -> d.setFavorito(true));
        favoritos.sort(Comparator.comparingInt((AlimentoDTO d) -> -usoDe.get(d.getId()).veces())
                .thenComparing(d -> usoDe.get(d.getId()).ultimo(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AlimentoDTO::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AlimentoDTO::getId));

        FavoritosDTO.PropuestaFavoritoDTO propuesta = propuestas.stream()
                .filter(p -> dtos.containsKey(p.id()))
                .findFirst()
                .map(p -> {
                    AlimentoDTO d = dtos.get(p.id());
                    d.setFavorito(false);
                    return new FavoritosDTO.PropuestaFavoritoDTO(d, p.veces());
                })
                .orElse(null);
        return new FavoritosDTO(favoritos, propuesta);
    }

    // --- Andamiaje ----------------------------------------------------------

    // El alimento, si la cuenta lo puede ver; si no existe o es propio de otra, el mismo
    // 403, que no dice cuál de las dos es.
    private Alimento visible(Integer alimentoId) {
        Integer usuarioId = securityUtils.getCurrentUserId();
        return alimentoRepository.findById(alimentoId)
                .filter(a -> a.getUsuario() == null || a.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new UnauthorizedException("error.acceso.recurso"));
    }

    // Insertar si no estaba, sin carreras: dos «marcar» a la vez dejan un solo favorito.
    private void guardar(Integer alimentoId) {
        jdbc.update("""
                INSERT INTO favoritos (usuario_id, alimento_id, creado) VALUES (?, ?, NOW())
                ON DUPLICATE KEY UPDATE alimento_id = alimento_id""", securityUtils.getCurrentUserId(), alimentoId);
        // Lo que ha sido favorito ya no se propone.
        cerrarPropuesta(alimentoId, PropuestaFavorito.FAVORITO);
    }

    private void cerrarPropuesta(Integer alimentoId, String motivo) {
        jdbc.update("""
                INSERT INTO propuestas_favorito (usuario_id, alimento_id, motivo, creado) VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE alimento_id = alimento_id""",
                securityUtils.getCurrentUserId(), alimentoId, motivo);
    }

    // El primer instante de los últimos {@code dias} días, hoy incluido.
    private static Timestamp desde(LocalDate hoy, int dias) {
        return Timestamp.valueOf(hoy.minusDays(dias - 1L).atStartOfDay());
    }
}
