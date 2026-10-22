package com.gymprofit.api.service.diario;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.diario.AguaDiaDTO;
import com.gymprofit.api.dto.entity.diario.AlimentoApuntadoDTO;
import com.gymprofit.api.dto.entity.diario.DiaDiarioDTO;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IAguaDiariaRepository;
import com.gymprofit.api.repository.jpa.IComidaHoyNoRepository;
import com.gymprofit.api.service.comida.AnadirAlimentoService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ============================================================
// DiarioService — el agua, «Hoy no» y los días del diario (lote 1.6.8, GP-162 y GP-166)
//
// Todo es de la cuenta del token (DEC-013): la fecha y la comida de la ruta eligen un día
// y una comida de esa cuenta, nunca de otra.
//
// Los días se piden por rango, como la tira de la semana: cuatro consultas sea cual sea
// el rango (comidas, «Hoy no», agua y entrenos), en vez de una por día.
// ============================================================
@Service
public class DiarioService {

    /** Las cinco comidas del diario, en su orden. SNACK no está en el diario. */
    static final Set<TipoComida> DEL_DIARIO = EnumSet.of(
            TipoComida.DESAYUNO, TipoComida.ALMUERZO, TipoComida.COMIDA, TipoComida.MERIENDA, TipoComida.CENA);

    /** Días como mucho en un rango: un mes, que es lo más que pinta la app. */
    static final int MAXIMO_DIAS = 31;

    /** Días como mucho en «Lo que más has comido»: una semana. */
    static final int MAXIMO_DIAS_SEMANA = 7;

    /** Cuántos alimentos salen en «Lo que más has comido». */
    static final int MAS_APUNTADOS = 3;

    private final SecurityUtils securityUtils;
    private final IAguaDiariaRepository aguaRepository;
    private final IComidaHoyNoRepository hoyNoRepository;
    private final JdbcTemplate jdbc;

    public DiarioService(SecurityUtils securityUtils, IAguaDiariaRepository aguaRepository,
                         IComidaHoyNoRepository hoyNoRepository, JdbcTemplate jdbc) {
        this.securityUtils = securityUtils;
        this.aguaRepository = aguaRepository;
        this.hoyNoRepository = hoyNoRepository;
        this.jdbc = jdbc;
    }

    /**
     * Cada día del rango, ambos incluidos y en orden, con lo que necesita el diario.
     *
     * @param desde primer día.
     * @param hasta último día, como mucho 30 después de {@code desde}.
     * @return un elemento por día, también los vacíos.
     * @throws InvalidDataException (400) si el rango va al revés o pasa de 31 días.
     */
    @Transactional(readOnly = true)
    public List<DiaDiarioDTO> dias(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) throw new InvalidDataException("error.diario.rango.invertido");
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS) {
            throw new InvalidDataException("error.diario.rango.largo", MAXIMO_DIAS);
        }

        Integer usuarioId = securityUtils.getCurrentUserId();
        Map<LocalDate, DiaDiarioDTO> dias = new LinkedHashMap<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            dias.put(d, new DiaDiarioDTO(d, new ArrayList<>(), new ArrayList<>(), 0, 0, null));
        }
        Timestamp inicio = Timestamp.valueOf(desde.atStartOfDay());
        Timestamp fin = Timestamp.valueOf(hasta.plusDays(1).atStartOfDay());

        // Una comida está apuntada si tiene al menos un alimento; la que existe vacía, no.
        Map<LocalDate, Set<TipoComida>> apuntadas = new HashMap<>();
        jdbc.query("""
                SELECT DISTINCT DATE(c.fecha) AS dia, c.tipo_comida
                FROM comidas c
                WHERE c.usuario_id = ? AND c.fecha >= ? AND c.fecha < ?
                  AND EXISTS (SELECT 1 FROM alimentos_comida ac WHERE ac.comida_id = c.id)""",
                rs -> {
                    TipoComida tipo = tipoDelDiario(rs.getString("tipo_comida"));
                    if (tipo != null) {
                        apuntadas.computeIfAbsent(rs.getDate("dia").toLocalDate(),
                                k -> EnumSet.noneOf(TipoComida.class)).add(tipo);
                    }
                }, usuarioId, inicio, fin);

        Map<LocalDate, Set<TipoComida>> hoyNo = new HashMap<>();
        jdbc.query("SELECT fecha, tipo_comida FROM comidas_hoy_no WHERE usuario_id = ? AND fecha BETWEEN ? AND ?",
                rs -> {
                    TipoComida tipo = tipoDelDiario(rs.getString("tipo_comida"));
                    if (tipo != null) {
                        hoyNo.computeIfAbsent(rs.getDate("fecha").toLocalDate(),
                                k -> EnumSet.noneOf(TipoComida.class)).add(tipo);
                    }
                }, usuarioId, Date.valueOf(desde), Date.valueOf(hasta));

        jdbc.query("SELECT fecha, mililitros FROM agua_diaria WHERE usuario_id = ? AND fecha BETWEEN ? AND ?",
                rs -> {
                    DiaDiarioDTO dia = dias.get(rs.getDate("fecha").toLocalDate());
                    if (dia != null) dia.setAguaMl(rs.getInt("mililitros"));
                }, usuarioId, Date.valueOf(desde), Date.valueOf(hasta));

        // Del más reciente al más antiguo: el primer nombre de rutina de cada día es el del
        // último entreno con rutina. Solo las sesiones terminadas: una a medias no es entreno.
        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        jdbc.query("""
                SELECT DATE(s.fecha_inicio) AS dia, r.nombre, r.nombre_en
                FROM sesiones_entrenamiento s
                LEFT JOIN rutinas r ON r.id = s.rutina_id
                WHERE s.usuario_id = ? AND s.completada = 1 AND s.fecha_inicio >= ? AND s.fecha_inicio < ?
                ORDER BY s.fecha_inicio DESC, s.id DESC""",
                rs -> {
                    DiaDiarioDTO dia = dias.get(rs.getDate("dia").toLocalDate());
                    if (dia == null) return;
                    dia.setEntrenos(dia.getEntrenos() + 1);
                    String nombre = rs.getString("nombre");
                    String en = rs.getString("nombre_en");
                    if (ingles && en != null && !en.isBlank()) nombre = en;
                    if (dia.getRutinaEntreno() == null && nombre != null && !nombre.isBlank()) {
                        dia.setRutinaEntreno(nombre);
                    }
                }, usuarioId, inicio, fin);

        // Los EnumSet van en el orden del enum, que es el del día.
        for (DiaDiarioDTO dia : dias.values()) {
            dia.getComidasApuntadas().addAll(apuntadas.getOrDefault(dia.getFecha(), Set.of()));
            dia.getHoyNo().addAll(hoyNo.getOrDefault(dia.getFecha(), Set.of()));
        }
        return new ArrayList<>(dias.values());
    }

    /**
     * Los alimentos que más se apuntaron en el rango, con las veces (tablero 12 de nutrición).
     * Cuenta apuntes, no gramos: tres veces pollo son tres, pese a las cantidades. Empata por
     * nombre y, después, por id, para que la lista no baile entre dos peticiones.
     *
     * @param desde primer día.
     * @param hasta último día, como mucho 6 después de {@code desde} (una semana).
     * @return hasta {@value #MAS_APUNTADOS} alimentos, de más a menos; vacío si no hay nada.
     * @throws InvalidDataException (400) si el rango va al revés o pasa de 7 días.
     */
    @Transactional(readOnly = true)
    public List<AlimentoApuntadoDTO> masApuntados(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) throw new InvalidDataException("error.diario.rango.invertido");
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS_SEMANA) {
            throw new InvalidDataException("error.diario.rango.largo", MAXIMO_DIAS_SEMANA);
        }
        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        return jdbc.query("""
                SELECT a.id, a.nombre, a.nombre_en, COUNT(*) AS veces
                FROM alimentos_comida ac
                JOIN comidas c ON c.id = ac.comida_id
                JOIN alimentos a ON a.id = ac.alimento_id
                WHERE c.usuario_id = ? AND c.fecha >= ? AND c.fecha < ?
                GROUP BY a.id
                ORDER BY veces DESC, a.nombre, a.id
                LIMIT ?""",
                (rs, n) -> {
                    String nombre = rs.getString("nombre");
                    String en = rs.getString("nombre_en");
                    if (ingles && en != null && !en.isBlank()) nombre = en;
                    return new AlimentoApuntadoDTO(rs.getInt("id"), nombre, rs.getInt("veces"));
                },
                securityUtils.getCurrentUserId(), Timestamp.valueOf(desde.atStartOfDay()),
                Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()), MAS_APUNTADOS);
    }

    /**
     * Guarda el agua de un día de la cuenta: el total, no lo que se suma.
     *
     * @throws InvalidDataException (400) si el día no ha llegado.
     */
    @Transactional
    public AguaDiaDTO guardarAgua(LocalDate fecha, int mililitros) {
        noFutura(fecha);
        aguaRepository.guardar(securityUtils.getCurrentUserId(), fecha, mililitros);
        return new AguaDiaDTO(fecha, mililitros);
    }

    /**
     * Marca una comida de un día como «Hoy no». Repetible.
     *
     * @throws InvalidDataException (400) si el día no ha llegado o la comida no es del diario.
     */
    @Transactional
    public void marcarHoyNo(LocalDate fecha, String tipoComida) {
        TipoComida tipo = tipoValido(tipoComida);
        noFutura(fecha);
        hoyNoRepository.marcar(securityUtils.getCurrentUserId(), fecha, tipo.name());
    }

    /**
     * Quita la marca «Hoy no». Repetible: si no estaba, no hace nada.
     *
     * @throws InvalidDataException (400) si la comida no es del diario.
     */
    @Transactional
    public void quitarHoyNo(LocalDate fecha, String tipoComida) {
        TipoComida tipo = tipoValido(tipoComida);
        hoyNoRepository.quitar(securityUtils.getCurrentUserId(), fecha, tipo.name());
    }

    // Mañana todavía vale: la API va en su hora y el móvil puede ir por delante.
    private static void noFutura(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now().plusDays(1))) {
            throw new InvalidDataException("error.diario.futuro");
        }
    }

    private static TipoComida tipoValido(String texto) {
        TipoComida tipo = AnadirAlimentoService.tipo(texto);
        if (!DEL_DIARIO.contains(tipo)) throw new InvalidDataException("error.diario.comida", texto);
        return tipo;
    }

    private static TipoComida tipoDelDiario(String texto) {
        try {
            TipoComida tipo = TipoComida.valueOf(texto);
            return DEL_DIARIO.contains(tipo) ? tipo : null;
        } catch (IllegalArgumentException e) {
            // Un tipo que ya no existe en el enum no es del diario: se salta.
            return null;
        }
    }
}
