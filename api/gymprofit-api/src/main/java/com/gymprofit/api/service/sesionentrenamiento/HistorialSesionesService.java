package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.HistorialSesionesDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SeriesMusculoDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.VezRutinaDTO;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.service.ejercicio.Musculos;
import com.gymprofit.api.service.record.IRecordService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// ============================================================
// HistorialSesionesService — las tarjetas de sesión y de rutina, de una pasada (GP-232)
//
// Antes cada tarjeta pedía lo suyo (/sesiones/{id}/volumen o /resumen) y los récords se
// sacaban de los vigentes, así que uno superado después desaparecía de su sesión. Aquí
// sale todo de un número fijo de consultas por petición, sea cual sea el historial:
//   · las sesiones terminadas del usuario, con su rutina (una);
//   · los kilos de todas ellas, serie a serie y desde el resumen, con la definición de
//     GP-011 (dos, agrupadas por sesión);
//   · los récords que batió cada una, con CalculadoraRecords (la de RecordService);
//   · las series hechas de todas ellas, para las barras (una, agrupada por sesión);
//   · y para las de la página, sus series y sus músculos (una, agrupada).
// Con todas las sesiones en memoria, las barras de una sesión (sus 6 últimas con la misma
// rutina) y la anterior salen aunque caigan en una página anterior del historial.
//
// Las semanas van por el lunes de la hora local: las fechas se guardan en la hora local
// del móvil, como en /volumen-semanal. Una página son hasta 8 semanas con sesiones y una
// semana nunca se parte: el cursor es el lunes de la última semana de la página.
// ============================================================
@Service
public class HistorialSesionesService {

    /** Semanas con sesiones por página. */
    public static final int SEMANAS_POR_PAGINA = 8;

    /** Barras de una tarjeta: las últimas veces con la misma rutina, hasta esta. */
    public static final int BARRAS = 6;

    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbc;
    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    private final IRecordService recordService;

    public HistorialSesionesService(SecurityUtils securityUtils, JdbcTemplate jdbc,
                                    IEjercicioRealizadoRepository ejercicioRealizadoRepository,
                                    IRecordService recordService) {
        this.securityUtils = securityUtils;
        this.jdbc = jdbc;
        this.ejercicioRealizadoRepository = ejercicioRealizadoRepository;
        this.recordService = recordService;
    }

    /** Una sesión terminada, tal como sale de la base. */
    private record Fila(Integer id, Integer rutinaId, String rutinaNombre, LocalDateTime inicio, Integer minutos) { }

    /** Lo que se cuenta de los ejercicios de una sesión. */
    private static final class Detalle {
        int series;
        int ejercicios;
        final Map<String, Integer> musculos = new LinkedHashMap<>();
    }

    /**
     * Una página del historial del usuario del token.
     *
     * @param antes cursor: solo entran las semanas cuyo lunes es anterior; {@code null} para la primera.
     * @return hasta 8 semanas con sesiones, de la más nueva a la más vieja, y el cursor de la siguiente.
     */
    @Transactional(readOnly = true)
    public HistorialSesionesDTO historial(LocalDate antes) {
        Integer usuarioId = securityUtils.getCurrentUserId();
        List<Fila> sesiones = sesionesTerminadas(usuarioId);

        TreeMap<LocalDate, List<Fila>> porSemana = new TreeMap<>(Comparator.reverseOrder());
        for (Fila f : sesiones) {
            LocalDate lunes = lunesDe(f.inicio());
            if (antes == null || lunes.isBefore(antes)) {
                porSemana.computeIfAbsent(lunes, k -> new ArrayList<>()).add(f);
            }
        }
        if (porSemana.isEmpty()) return new HistorialSesionesDTO(List.of(), null);

        List<LocalDate> pagina = porSemana.keySet().stream().limit(SEMANAS_POR_PAGINA).toList();
        LocalDate siguiente = porSemana.size() > SEMANAS_POR_PAGINA ? pagina.get(pagina.size() - 1) : null;

        Map<Integer, BigDecimal> kilos = kilosPorSesion(usuarioId);
        Map<Integer, Integer> records = recordService.recordsPorSesion(usuarioId);
        Map<Integer, Integer> series = seriesPorSesion(usuarioId);
        Map<Integer, List<Fila>> porRutina = porRutina(sesiones);

        List<Integer> ids = new ArrayList<>();
        for (LocalDate lunes : pagina) for (Fila f : porSemana.get(lunes)) ids.add(f.id());
        Map<Integer, Detalle> detalles = detallesDe(ids);

        List<HistorialSesionesDTO.Semana> semanas = new ArrayList<>(pagina.size());
        for (LocalDate lunes : pagina) {
            List<Fila> deLaSemana = new ArrayList<>(porSemana.get(lunes));
            Collections.reverse(deLaSemana);   // de la más nueva a la más vieja
            BigDecimal total = BigDecimal.ZERO;
            List<HistorialSesionesDTO.Sesion> tarjetas = new ArrayList<>(deLaSemana.size());
            for (Fila f : deLaSemana) {
                HistorialSesionesDTO.Sesion s = tarjeta(f, kilos, records, series, porRutina,
                        detalles.getOrDefault(f.id(), new Detalle()));
                total = total.add(s.getKilos());
                tarjetas.add(s);
            }
            semanas.add(new HistorialSesionesDTO.Semana(lunes, tarjetas.size(), total, tarjetas));
        }
        return new HistorialSesionesDTO(semanas, siguiente);
    }

    /**
     * Pone en las rutinas sus series pautadas por músculo y sus 6 últimas veces (GP-232), para
     * GET /rutinas/usuario/{id}/activas. Consultas agrupadas, ninguna por rutina.
     *
     * @param usuarioId dueño de las rutinas (ya comprobado por quien llama).
     * @param rutinas   las rutinas a completar; se modifican en el sitio.
     */
    @Transactional(readOnly = true)
    public void completarRutinas(Integer usuarioId, List<RutinaDTO> rutinas) {
        if (rutinas.isEmpty()) return;
        List<Integer> ids = rutinas.stream().map(RutinaDTO::getId).toList();
        Map<Integer, List<SeriesMusculoDTO>> musculos = musculosDeRutinas(ids);

        Map<Integer, List<Fila>> porRutina = porRutina(sesionesTerminadas(usuarioId));
        boolean alguna = ids.stream().anyMatch(porRutina::containsKey);
        Map<Integer, BigDecimal> kilos = alguna ? kilosPorSesion(usuarioId) : Map.of();
        Map<Integer, Integer> records = alguna ? recordService.recordsPorSesion(usuarioId) : Map.of();
        Map<Integer, Integer> series = alguna ? seriesPorSesion(usuarioId) : Map.of();

        for (RutinaDTO r : rutinas) {
            r.setMusculos(musculos.getOrDefault(r.getId(), List.of()));
            List<Fila> hechas = porRutina.getOrDefault(r.getId(), List.of());
            r.setUltimas(veces(hechas.subList(Math.max(0, hechas.size() - BARRAS), hechas.size()), kilos, records,
                    series));
        }
    }

    // --- Armado -------------------------------------------------------------

    private HistorialSesionesDTO.Sesion tarjeta(Fila f, Map<Integer, BigDecimal> kilos, Map<Integer, Integer> records,
                                               Map<Integer, Integer> series, Map<Integer, List<Fila>> porRutina,
                                               Detalle d) {
        List<VezRutinaDTO> barras = List.of();
        BigDecimal anterior = null;
        if (f.rutinaId() != null) {
            List<Fila> deLaRutina = porRutina.get(f.rutinaId());
            int pos = deLaRutina.indexOf(f);
            barras = veces(deLaRutina.subList(Math.max(0, pos + 1 - BARRAS), pos + 1), kilos, records, series);
            if (pos > 0) anterior = kilos.getOrDefault(deLaRutina.get(pos - 1).id(), BigDecimal.ZERO);
        }
        return new HistorialSesionesDTO.Sesion(f.id(), f.rutinaId(), f.rutinaNombre(), f.inicio(), f.minutos(),
                kilos.getOrDefault(f.id(), BigDecimal.ZERO), d.series, d.ejercicios,
                records.getOrDefault(f.id(), 0), anterior, deMasAMenos(d.musculos), barras);
    }

    private static List<VezRutinaDTO> veces(List<Fila> filas, Map<Integer, BigDecimal> kilos,
                                            Map<Integer, Integer> records, Map<Integer, Integer> series) {
        List<VezRutinaDTO> lista = new ArrayList<>(filas.size());
        for (Fila f : filas) {
            lista.add(new VezRutinaDTO(f.id(), f.inicio(), kilos.getOrDefault(f.id(), BigDecimal.ZERO),
                    records.getOrDefault(f.id(), 0) > 0, series.getOrDefault(f.id(), 0),
                    records.getOrDefault(f.id(), 0)));
        }
        return lista;
    }

    // De más a menos series; a igualdad, en el orden en que aparecieron.
    private static List<SeriesMusculoDTO> deMasAMenos(Map<String, Integer> musculos) {
        List<SeriesMusculoDTO> lista = new ArrayList<>(musculos.size());
        musculos.forEach((m, n) -> lista.add(new SeriesMusculoDTO(m, n)));
        lista.sort(Comparator.comparing(SeriesMusculoDTO::getSeries).reversed());
        return lista;
    }

    // Las sesiones de cada rutina en orden cronológico (las filas ya llegan así).
    private static Map<Integer, List<Fila>> porRutina(List<Fila> sesiones) {
        Map<Integer, List<Fila>> mapa = new HashMap<>();
        for (Fila f : sesiones) {
            if (f.rutinaId() != null) mapa.computeIfAbsent(f.rutinaId(), k -> new ArrayList<>()).add(f);
        }
        return mapa;
    }

    private static LocalDate lunesDe(LocalDateTime inicio) {
        return inicio.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // --- Consultas ----------------------------------------------------------

    // Las sesiones terminadas del usuario, de la más vieja a la más nueva (por inicio y, en
    // empate, por id, como los récords). La rutina sale también si está desactivada.
    private List<Fila> sesionesTerminadas(Integer usuarioId) {
        return jdbc.query("SELECT s.id, s.rutina_id, r.nombre, s.fecha_inicio, s.duracion_minutos "
                        + "FROM sesiones_entrenamiento s LEFT JOIN rutinas r ON r.id = s.rutina_id "
                        + "WHERE s.usuario_id = ? AND s.completada = 1 AND s.fecha_inicio IS NOT NULL "
                        + "ORDER BY s.fecha_inicio, s.id",
                (rs, i) -> new Fila(rs.getInt("id"), (Integer) rs.getObject("rutina_id"), rs.getString("nombre"),
                        rs.getTimestamp("fecha_inicio").toLocalDateTime(), (Integer) rs.getObject("duracion_minutos")),
                usuarioId);
    }

    // Kilos de cada sesión con la definición de GP-011: serie a serie si las tiene; si no,
    // desde el resumen del ejercicio (como /sesiones/{id}/volumen y /volumen-semanal).
    private Map<Integer, BigDecimal> kilosPorSesion(Integer usuarioId) {
        Map<Integer, BigDecimal> porSeries = aMapa(ejercicioRealizadoRepository.volumenDeSeriesDeUsuario(usuarioId));
        Map<Integer, BigDecimal> kilos = aMapa(ejercicioRealizadoRepository.volumenDeResumenDeUsuario(usuarioId));
        porSeries.forEach((id, k) -> {
            if (k.signum() > 0) kilos.put(id, k);
        });
        return kilos;
    }

    // Series hechas de cada sesión del usuario, en una consulta: las de las barras pueden caer
    // fuera de la página. Cuenta como detallesDe (SERIES_DEL_EJERCICIO, ejercicio a ejercicio).
    private Map<Integer, Integer> seriesPorSesion(Integer usuarioId) {
        Map<Integer, Integer> series = new HashMap<>();
        jdbc.query("SELECT x.sesion_id, SUM(x.hechas) AS hechas FROM ("
                        + "SELECT er.sesion_id, " + ResumenSesionService.SERIES_DEL_EJERCICIO + " AS hechas "
                        + "FROM ejercicios_realizados er JOIN sesiones_entrenamiento s ON s.id = er.sesion_id "
                        + "LEFT JOIN series_realizadas sr ON sr.ejercicio_realizado_id = er.id "
                        + "WHERE s.usuario_id = ? AND s.completada = 1 GROUP BY er.id) x GROUP BY x.sesion_id",
                rs -> {
                    series.put(rs.getInt("sesion_id"), rs.getInt("hechas"));
                }, usuarioId);
        return series;
    }

    // Series hechas, ejercicios y series por músculo de las sesiones pedidas, en una consulta.
    private Map<Integer, Detalle> detallesDe(Collection<Integer> sesionIds) {
        Map<Integer, Detalle> detalles = new HashMap<>();
        if (sesionIds.isEmpty()) return detalles;
        String marcas = String.join(",", Collections.nCopies(sesionIds.size(), "?"));
        jdbc.query("SELECT er.sesion_id, e.musculo_primario, e.grupo_muscular, "
                        + ResumenSesionService.SERIES_DEL_EJERCICIO + " AS hechas "
                        + "FROM ejercicios_realizados er JOIN ejercicios e ON e.id = er.ejercicio_id "
                        + "LEFT JOIN series_realizadas sr ON sr.ejercicio_realizado_id = er.id "
                        + "WHERE er.sesion_id IN (" + marcas + ") GROUP BY er.id, e.id ORDER BY er.sesion_id, er.id",
                rs -> {
                    Detalle d = detalles.computeIfAbsent(rs.getInt("sesion_id"), k -> new Detalle());
                    int hechas = rs.getInt("hechas");
                    d.ejercicios++;
                    d.series += hechas;
                    String musculo = Musculos.normalizar(rs.getString("musculo_primario"), rs.getString("grupo_muscular"));
                    if (musculo != null && hechas > 0) d.musculos.merge(musculo, hechas, Integer::sum);
                }, sesionIds.toArray());
        return detalles;
    }

    // Series pautadas por músculo de cada rutina, en una consulta.
    private Map<Integer, List<SeriesMusculoDTO>> musculosDeRutinas(List<Integer> rutinaIds) {
        Map<Integer, Map<String, Integer>> porRutina = new HashMap<>();
        String marcas = String.join(",", Collections.nCopies(rutinaIds.size(), "?"));
        jdbc.query("SELECT re.rutina_id, re.series, e.musculo_primario, e.grupo_muscular "
                        + "FROM rutina_ejercicio re JOIN ejercicios e ON e.id = re.ejercicio_id "
                        + "WHERE re.rutina_id IN (" + marcas + ") ORDER BY re.rutina_id, re.orden, re.id",
                rs -> {
                    String musculo = Musculos.normalizar(rs.getString("musculo_primario"), rs.getString("grupo_muscular"));
                    if (musculo == null) return;
                    // Un ejercicio sin series pautadas cuenta una, como en ZonasDelDia de la app.
                    int series = Math.max(1, rs.getInt("series"));
                    porRutina.computeIfAbsent(rs.getInt("rutina_id"), k -> new LinkedHashMap<>())
                            .merge(musculo, series, Integer::sum);
                }, rutinaIds.toArray());
        Map<Integer, List<SeriesMusculoDTO>> resultado = new HashMap<>();
        porRutina.forEach((id, m) -> resultado.put(id, deMasAMenos(m)));
        return resultado;
    }

    // Filas [sesionId, kilos] a mapa; un NULL (sin pesos) cuenta como cero.
    private static Map<Integer, BigDecimal> aMapa(List<Object[]> filas) {
        Map<Integer, BigDecimal> mapa = new HashMap<>();
        for (Object[] f : filas) {
            mapa.put((Integer) f[0], f[1] == null ? BigDecimal.ZERO : new BigDecimal(f[1].toString()));
        }
        return mapa;
    }
}
