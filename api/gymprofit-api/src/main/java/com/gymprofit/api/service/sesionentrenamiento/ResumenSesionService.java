package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.sesionentrenamiento.ResumenSesionDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// ResumenSesionService — el resumen del entreno (GP-210 paso 1, GP-023 y GP-011)
//
// Kilos y series por ejercicio, lo de la última vez de cada ejercicio y la sesión anterior
// de la misma rutina, para cualquier sesión del usuario del token, también las del
// historial. Los kilos usan la definición de GP-011: peso × repeticiones de las series sin
// segundos y, donde no hay registro por serie, el resumen del ejercicio.
//
// Consultas fijas sea cual sea el historial (ninguna por ejercicio): la sesión, sus
// ejercicios, la última vez de todos ellos de una vez y la sesión anterior. Todas entran
// por las claves ajenas (usuario, sesión, ejercicio), que ya están indexadas.
// ============================================================
@Service
public class ResumenSesionService {

    /** Kilos de un ejercicio realizado: las series sin segundos y, si no hay series, su resumen. */
    private static final String KILOS_DEL_EJERCICIO = """
            CASE WHEN COUNT(sr.id) > 0
                 THEN COALESCE(SUM(CASE WHEN sr.segundos IS NULL THEN sr.peso * sr.repeticiones END), 0)
                 ELSE COALESCE(er.peso_usado * er.repeticiones_rutinas * er.series_completadas, 0) END""";

    /** Series hechas: las marcadas con algo dentro (como la última vez) o, sin series, las del resumen. */
    private static final String SERIES_DEL_EJERCICIO = """
            CASE WHEN COUNT(sr.id) > 0
                 THEN SUM(CASE WHEN sr.completada = 1 AND (sr.repeticiones > 0 OR sr.segundos > 0) THEN 1 ELSE 0 END)
                 ELSE COALESCE(er.series_completadas, 0) END""";

    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbc;
    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;

    public ResumenSesionService(SecurityUtils securityUtils, JdbcTemplate jdbc,
                                IEjercicioRealizadoRepository ejercicioRealizadoRepository) {
        this.securityUtils = securityUtils;
        this.jdbc = jdbc;
        this.ejercicioRealizadoRepository = ejercicioRealizadoRepository;
    }

    /**
     * Resume una sesión.
     *
     * @param sesionId sesión a resumir (se comprueba la propiedad).
     * @return kilos, series, ejercicios y comparaciones; las comparaciones son nulas la primera vez.
     * @throws NotFoundEntityException (404) si la sesión no existe.
     */
    @Transactional(readOnly = true)
    public ResumenSesionDTO resumir(Integer sesionId) {
        Map<String, Object> sesion = jdbc.queryForList(
                "SELECT usuario_id, rutina_id, fecha_inicio FROM sesiones_entrenamiento WHERE id = ?", sesionId)
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundEntityException("error.sesion.noExiste", sesionId));
        Integer usuarioId = (Integer) sesion.get("usuario_id");
        securityUtils.checkDueno(usuarioId);
        Integer rutinaId = (Integer) sesion.get("rutina_id");
        Timestamp inicio = (Timestamp) sesion.get("fecha_inicio");

        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        List<ResumenSesionDTO.EjercicioResumen> ejercicios = new ArrayList<>();
        int series = 0;
        for (Map<String, Object> f : jdbc.queryForList(
                "SELECT er.id, er.ejercicio_id, e.nombre, e.nombre_en, COUNT(sr.id) AS filas, "
                        + "SUM(CASE WHEN sr.segundos IS NOT NULL THEN 1 ELSE 0 END) AS de_tiempo, "
                        + "er.tiempo_segundos, " + KILOS_DEL_EJERCICIO + " AS kilos, "
                        + SERIES_DEL_EJERCICIO + " AS hechas "
                        + "FROM ejercicios_realizados er JOIN ejercicios e ON e.id = er.ejercicio_id "
                        + "LEFT JOIN series_realizadas sr ON sr.ejercicio_realizado_id = er.id "
                        + "WHERE er.sesion_id = ? GROUP BY er.id, e.id ORDER BY er.id", sesionId)) {
            long filas = ((Number) f.get("filas")).longValue();
            long deTiempo = f.get("de_tiempo") == null ? 0 : ((Number) f.get("de_tiempo")).longValue();
            boolean porTiempo = filas > 0 ? deTiempo == filas : f.get("tiempo_segundos") != null;
            String nombre = (String) f.get("nombre");
            String en = (String) f.get("nombre_en");
            if (ingles && en != null && !en.isBlank()) nombre = en;
            int hechas = ((Number) f.get("hechas")).intValue();
            series += hechas;
            ejercicios.add(new ResumenSesionDTO.EjercicioResumen((Integer) f.get("ejercicio_id"), nombre,
                    kilos(f.get("kilos")), hechas, porTiempo, null));
        }

        completarUltimaVez(ejercicios, usuarioId, inicio);

        ResumenSesionDTO resumen = new ResumenSesionDTO();
        resumen.setSesionId(sesionId);
        resumen.setVolumenKg(volumenDe(sesionId));
        resumen.setSeries(series);
        resumen.setAnterior(sesionAnterior(usuarioId, rutinaId, inicio, sesionId));
        resumen.setEjercicios(ejercicios);
        return resumen;
    }

    /**
     * Pone en cada ejercicio lo que movió la última vez que se hizo antes de {@code inicio}.
     * Una sola consulta para todos; si el ejercicio salió dos veces ese día cuenta la primera.
     */
    private void completarUltimaVez(List<ResumenSesionDTO.EjercicioResumen> ejercicios, Integer usuarioId,
                                    Timestamp inicio) {
        if (ejercicios.isEmpty()) return;
        List<Integer> ids = ejercicios.stream().map(ResumenSesionDTO.EjercicioResumen::getEjercicioId)
                .distinct().toList();
        String marcas = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(usuarioId);
        args.add(inicio);
        args.addAll(ids);
        args.add(usuarioId);
        args.add(inicio);
        args.addAll(ids);

        Map<Integer, ResumenSesionDTO.UltimaMarca> ultima = new HashMap<>();
        jdbc.query("SELECT er.ejercicio_id, er.id, s.fecha_inicio, " + KILOS_DEL_EJERCICIO + " AS kilos "
                        + "FROM ejercicios_realizados er JOIN sesiones_entrenamiento s ON s.id = er.sesion_id "
                        + "LEFT JOIN series_realizadas sr ON sr.ejercicio_realizado_id = er.id "
                        + "WHERE s.usuario_id = ? AND s.completada = 1 AND s.fecha_inicio < ? "
                        + "AND er.ejercicio_id IN (" + marcas + ") "
                        + "AND s.fecha_inicio = (SELECT MAX(s2.fecha_inicio) FROM ejercicios_realizados er2 "
                        + "  JOIN sesiones_entrenamiento s2 ON s2.id = er2.sesion_id "
                        + "  WHERE s2.usuario_id = ? AND s2.completada = 1 AND s2.fecha_inicio < ? "
                        + "  AND er2.ejercicio_id = er.ejercicio_id AND er2.ejercicio_id IN (" + marcas + ")) "
                        + "GROUP BY er.id, s.id ORDER BY er.ejercicio_id, s.id DESC, er.id",
                rs -> {
                    // Las filas llegan de la sesión más nueva a la más vieja y, dentro, por
                    // aparición: gana la primera de cada ejercicio.
                    ultima.putIfAbsent(rs.getInt("ejercicio_id"), new ResumenSesionDTO.UltimaMarca(
                            rs.getTimestamp("fecha_inicio").toLocalDateTime(), kilos(rs.getBigDecimal("kilos"))));
                }, args.toArray());

        for (ResumenSesionDTO.EjercicioResumen e : ejercicios) e.setAnterior(ultima.get(e.getEjercicioId()));
    }

    /** La sesión terminada inmediatamente anterior de la misma rutina, o nulo si no hay rutina ni anterior. */
    private ResumenSesionDTO.SesionAnterior sesionAnterior(Integer usuarioId, Integer rutinaId, Timestamp inicio,
                                                           Integer sesionId) {
        if (rutinaId == null) return null;
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT id, fecha_inicio FROM sesiones_entrenamiento WHERE usuario_id = ? AND rutina_id = ? "
                        + "AND completada = 1 AND fecha_inicio < ? AND id <> ? "
                        + "ORDER BY fecha_inicio DESC, id DESC LIMIT 1", usuarioId, rutinaId, inicio, sesionId);
        if (filas.isEmpty()) return null;
        Integer id = (Integer) filas.get(0).get("id");
        LocalDateTime fecha = ((Timestamp) filas.get(0).get("fecha_inicio")).toLocalDateTime();
        return new ResumenSesionDTO.SesionAnterior(id, fecha, volumenDe(id));
    }

    /** Kilos de la sesión con la definición de GP-011 (la misma que {@code /sesiones/{id}/volumen}). */
    private BigDecimal volumenDe(Integer sesionId) {
        BigDecimal porSeries = ejercicioRealizadoRepository.volumenDeSeries(sesionId);
        if (porSeries != null && porSeries.signum() > 0) return porSeries;
        BigDecimal porResumen = ejercicioRealizadoRepository.volumenDeResumen(sesionId);
        return porResumen == null ? BigDecimal.ZERO : porResumen;
    }

    private static BigDecimal kilos(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        return valor instanceof BigDecimal b ? b : new BigDecimal(valor.toString());
    }
}
