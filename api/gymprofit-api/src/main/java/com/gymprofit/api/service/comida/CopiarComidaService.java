package com.gymprofit.api.service.comida;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.comida.ComidaRecienteDTO;
import com.gymprofit.api.dto.entity.comida.CopiaComidaRespuestaDTO;
import com.gymprofit.api.dto.entity.comida.CopiarComidaDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.UnauthorizedException;
import com.gymprofit.api.mappers.AlimentoComidaMapper;
import com.gymprofit.api.mappers.ComidaMapper;
import com.gymprofit.api.repository.jpa.IAlimentoComidaRepository;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.alimentocomida.RacionVigente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// ============================================================
// CopiarComidaService — las comidas recientes y copiar una entera (lote 1.6.4, A1 y A2)
//
// Copiar una comida es añadir cada uno de sus alimentos, con la regla de siempre
// (AnadirAlimentoService): si ya está en el destino, se suma. Lo que no se puede copiar
// —un alimento desactivado, o uno que la cuenta ya no ve— se salta, y en las comidas
// recientes ni se enseña: lo que sale es exactamente lo que se copiaría.
//
// Las recientes miran los 14 días que acaban en el día de destino (ese incluido) y nunca
// la comida de destino. Primero, la más reciente del mismo tipo anterior a ese día («la
// de ayer», si la hubo); después, las más recientes de cualquier tipo: por día y, en el
// mismo día, la más tardía primero. Como mucho tres, en tres consultas sean cuantas sean.
// ============================================================
@Service
public class CopiarComidaService {

    /** Las recientes miran estos días, el de destino incluido. */
    static final int DIAS_RECIENTES = 14;
    /** Como mucho, estas. */
    static final int MAXIMO_RECIENTES = 3;

    private final IComidaRepository comidaRepository;
    private final IAlimentoComidaRepository lineaRepository;
    private final IAlimentoComidaService alimentoComidaService;
    private final AnadirAlimentoService anadirAlimentoService;
    private final ComidaMapper comidaMapper;
    private final AlimentoComidaMapper lineaMapper;
    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbc;

    public CopiarComidaService(IComidaRepository comidaRepository, IAlimentoComidaRepository lineaRepository,
                               IAlimentoComidaService alimentoComidaService, AnadirAlimentoService anadirAlimentoService,
                               ComidaMapper comidaMapper, AlimentoComidaMapper lineaMapper, SecurityUtils securityUtils,
                               JdbcTemplate jdbc) {
        this.comidaRepository = comidaRepository;
        this.lineaRepository = lineaRepository;
        this.alimentoComidaService = alimentoComidaService;
        this.anadirAlimentoService = anadirAlimentoService;
        this.comidaMapper = comidaMapper;
        this.lineaMapper = lineaMapper;
        this.securityUtils = securityUtils;
        this.jdbc = jdbc;
    }

    /**
     * Las comidas que se pueden copiar a la de ese día y ese tipo.
     *
     * @param fecha      día de la comida de destino.
     * @param tipoComida tipo de la comida de destino.
     * @return de 0 a 3, en el orden en que se enseñan.
     * @throws InvalidDataException (400) si el tipo no existe.
     */
    @Transactional(readOnly = true)
    public List<ComidaRecienteDTO> recientes(LocalDate fecha, String tipoComida) {
        TipoComida tipo = AnadirAlimentoService.tipo(tipoComida);
        Integer usuarioId = securityUtils.getCurrentUserId();

        // Las candidatas: de la cuenta, en la ventana, no la de destino y con algo que copiar.
        record Candidata(int id, LocalDate dia, TipoComida tipo) {
        }
        List<Candidata> candidatas = jdbc.query("""
                SELECT c.id, c.fecha, c.tipo_comida
                FROM comidas c
                WHERE c.usuario_id = ? AND c.fecha >= ? AND c.fecha < ?
                  AND NOT (c.fecha >= ? AND c.tipo_comida = ?)
                  AND EXISTS (SELECT 1 FROM alimentos_comida ac JOIN alimentos a ON a.id = ac.alimento_id
                              WHERE ac.comida_id = c.id AND a.activo = 1
                                AND (a.usuario_id IS NULL OR a.usuario_id = ?))""",
                (rs, i) -> new Candidata(rs.getInt("id"), rs.getTimestamp("fecha").toLocalDateTime().toLocalDate(),
                        TipoComida.valueOf(rs.getString("tipo_comida"))),
                usuarioId, inicio(fecha.minusDays(DIAS_RECIENTES - 1L)), inicio(fecha.plusDays(1)),
                inicio(fecha), tipo.name(), usuarioId);

        Comparator<Candidata> masReciente = Comparator.comparing(Candidata::dia).reversed()
                .thenComparing(Comparator.comparingInt((Candidata c) -> c.tipo().ordinal()).reversed())
                .thenComparing(Comparator.comparingInt(Candidata::id).reversed());
        List<Candidata> elegidas = new ArrayList<>();
        candidatas.stream()
                .filter(c -> c.tipo() == tipo && c.dia().isBefore(fecha))
                .min(masReciente)
                .ifPresent(elegidas::add);
        candidatas.stream()
                .filter(c -> !elegidas.contains(c))
                .sorted(masReciente)
                .limit(MAXIMO_RECIENTES - (long) elegidas.size())
                .forEach(elegidas::add);
        if (elegidas.isEmpty()) return List.of();

        // Sus líneas, todas en una consulta.
        Map<Integer, List<AlimentoComida>> lineas = new LinkedHashMap<>();
        for (AlimentoComida l : lineaRepository.conAlimento(elegidas.stream().map(Candidata::id).toList())) {
            if (copiable(l.getAlimento(), usuarioId)) {
                lineas.computeIfAbsent(l.getComida().getId(), k -> new ArrayList<>()).add(l);
            }
        }
        List<ComidaRecienteDTO> recientes = new ArrayList<>();
        for (Candidata c : elegidas) {
            List<AlimentoComida> suyas = lineas.getOrDefault(c.id(), List.of());
            if (suyas.isEmpty()) continue;
            List<AlimentoComidaDTO> dtos = lineaMapper.toDTOList(suyas);
            int kcal = dtos.stream().map(AlimentoComidaDTO::getCaloriasTotales).filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).sum();
            recientes.add(new ComidaRecienteDTO(c.id(), c.tipo().name(), c.dia(), kcal, dtos));
        }
        return recientes;
    }

    /**
     * Copia cada alimento de una comida a la de ese día y ese tipo del usuario del token,
     * que se encuentra o se crea. Todo en una transacción.
     *
     * @return la comida de destino y, por cada alimento copiado, su línea y {@code anterior}.
     * @throws UnauthorizedException (403) si la comida de origen no es de la cuenta (o no existe).
     * @throws InvalidDataException  (400) si origen y destino son la misma comida, o no hay nada que copiar.
     */
    @Transactional
    public CopiaComidaRespuestaDTO copiar(CopiarComidaDTO pedido) {
        TipoComida tipo = AnadirAlimentoService.tipo(pedido.getTipoComida());
        Integer usuarioId = securityUtils.getCurrentUserId();
        // El id llega en el cuerpo y tiene dueño (DEC-014, DEC-027). Ni el ADMIN copia la
        // comida de otro: el destino es siempre del token. Que no exista da el mismo 403.
        Comida origen = comidaRepository.findById(pedido.getComidaId())
                .filter(c -> c.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new UnauthorizedException("error.acceso.recurso"));
        if (origen.getTipoComida() == tipo && origen.getFecha() != null
                && origen.getFecha().toLocalDate().equals(pedido.getFecha())) {
            throw new InvalidDataException("error.copiar.misma");
        }

        List<AlimentoComida> lineas = lineaRepository.conAlimento(List.of(origen.getId())).stream()
                .filter(l -> copiable(l.getAlimento(), usuarioId))
                .toList();
        if (lineas.isEmpty()) {
            throw new InvalidDataException("error.copiar.vacia");
        }

        Comida destino = anadirAlimentoService.comidaDelDia(tipo, pedido.getFecha());
        List<CopiaComidaRespuestaDTO.Copiada> copiadas = new ArrayList<>();
        for (AlimentoComida l : lineas) {
            AlimentoComida nueva = new AlimentoComida();
            nueva.setAlimento(l.getAlimento());
            // Con su ración si sigue pesando lo mismo (GP-177); si no, en gramos.
            AlimentoRacion racion = RacionVigente.de(l);
            if (racion != null) {
                alimentoComidaService.ponerCantidad(nueva, l.getCantidadGramos(), racion.getId(), l.getRaciones());
            } else {
                alimentoComidaService.ponerGramos(nueva, l.getCantidadGramos());
            }
            AnadirAlimentoService.Puesta puesta = anadirAlimentoService.ponerEn(destino, nueva);
            copiadas.add(new CopiaComidaRespuestaDTO.Copiada(lineaMapper.toDTO(puesta.linea()), puesta.anterior()));
        }
        alimentoComidaService.recalcularTotales(destino.getId());
        return new CopiaComidaRespuestaDTO(comidaMapper.toDTO(destino), copiadas);
    }

    // Lo que se copia: un alimento activo que la cuenta ve (del catálogo o suyo).
    private static boolean copiable(Alimento a, Integer usuarioId) {
        return Boolean.TRUE.equals(a.getActivo()) && (a.getUsuario() == null || a.getUsuario().getId().equals(usuarioId));
    }

    private static Timestamp inicio(LocalDate dia) {
        return Timestamp.valueOf(dia.atStartOfDay());
    }
}
