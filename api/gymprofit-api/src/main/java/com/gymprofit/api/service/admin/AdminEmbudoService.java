package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminEmbudoDTO;
import com.gymprofit.api.entity.PasoAltaDia;
import com.gymprofit.api.enums.PasoAlta;
import com.gymprofit.api.repository.jpa.IPasoAltaDiaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gymprofit.api.service.admin.AdminResumenService.MADRID;

// ============================================================
// AdminEmbudoService — el embudo del alta y el núcleo de las cuentas nuevas (GP-019)
//
// El embudo sale de alta_pasos, que ya es un recuento por día. El núcleo sale de lo que
// la base ya guarda, sin columnas ni seguimiento nuevos (DEC-064): la fecha de alta de
// cada cuenta y las fechas de sus entrenos, comidas, agua y peso.
//
// Los relojes, como en AdminResumenService: fecha_registro va en el del servidor y se
// lleva a Madrid; las fechas de lo apuntado son la hora de la pared del móvil y se toman
// tal cual. Se cargan ids y fechas para cruzarlos aquí, pero de aquí solo salen totales.
// ============================================================
@Service
@RequiredArgsConstructor
public class AdminEmbudoService implements IAdminEmbudoService {

    private static final int SEMANAS = 8;
    /** Los primeros 7 días de una cuenta: el del alta y los seis siguientes. */
    private static final int DIAS_NUCLEO = 7;

    @PersistenceContext
    private EntityManager em;

    private final IPasoAltaDiaRepository pasos;

    @Override
    @Transactional(readOnly = true)
    public AdminEmbudoDTO embudo() {
        return embudo(ZonedDateTime.now(MADRID));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEmbudoDTO embudo(ZonedDateTime ahora) {
        LocalDate hoy = ahora.withZoneSameInstant(MADRID).toLocalDate();
        LocalDate lunes = lunesDe(hoy);
        LocalDate primerLunes = lunes.minusWeeks(SEMANAS - 1);
        return new AdminEmbudoDTO(hoy, embudo(primerLunes, hoy), nucleo(primerLunes, hoy));
    }

    // ── Embudo ──────────────────────────────────────────────────────────────

    private List<AdminEmbudoDTO.SemanaEmbudo> embudo(LocalDate primerLunes, LocalDate hoy) {
        Map<LocalDate, Map<PasoAlta, Long>> porSemana = new HashMap<>();
        for (PasoAltaDia fila : pasos.entre(primerLunes, hoy)) {
            porSemana.computeIfAbsent(lunesDe(fila.getClave().getDia()), k -> new HashMap<>())
                    .merge(fila.getClave().getPaso(), fila.getTotal().longValue(), Long::sum);
        }
        List<AdminEmbudoDTO.SemanaEmbudo> semanas = new ArrayList<>();
        for (int i = 0; i < SEMANAS; i++) {
            LocalDate l = primerLunes.plusWeeks(i);
            Map<PasoAlta, Long> totales = porSemana.getOrDefault(l, Map.of());
            List<AdminEmbudoDTO.PasoTotal> fila = new ArrayList<>();
            for (PasoAlta p : PasoAlta.values()) {
                fila.add(new AdminEmbudoDTO.PasoTotal(p, totales.getOrDefault(p, 0L)));
            }
            semanas.add(new AdminEmbudoDTO.SemanaEmbudo(l, fila));
        }
        return semanas;
    }

    // ── Núcleo ──────────────────────────────────────────────────────────────

    // Lo que una cuenta hizo en sus primeros 7 días.
    private static final class Cuenta {
        final LocalDate alta;
        boolean entreno;
        boolean comida;
        boolean volvio;

        Cuenta(LocalDate alta) {
            this.alta = alta;
        }

        // Si el día cae en sus primeros 7, y si además es otro que el del alta, ha vuelto.
        boolean apuntar(LocalDate dia) {
            if (dia.isBefore(alta) || !dia.isBefore(alta.plusDays(DIAS_NUCLEO))) return false;
            if (!dia.equals(alta)) volvio = true;
            return true;
        }
    }

    private List<AdminEmbudoDTO.SemanaNucleo> nucleo(LocalDate primerLunes, LocalDate hoy) {
        LocalDate manana = hoy.plusDays(1);
        List<Object[]> altas = em.createQuery("""
                        SELECT u.id, u.fechaRegistro FROM Usuario u
                        WHERE u.fechaRegistro >= :desde AND u.fechaRegistro < :hasta""", Object[].class)
                .setParameter("desde", relojServidor(primerLunes))
                .setParameter("hasta", relojServidor(manana))
                .getResultList();
        Map<Integer, Cuenta> cuentas = new HashMap<>();
        for (Object[] a : altas) {
            LocalDate dia = ((LocalDateTime) a[1]).atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(MADRID).toLocalDate();
            cuentas.put((Integer) a[0], new Cuenta(dia));
        }

        if (!cuentas.isEmpty()) {
            Set<Integer> ids = cuentas.keySet();
            // Hasta la medianoche de mañana: lo de después no puede haber pasado aún.
            LocalDateTime desde = primerLunes.atStartOfDay();
            LocalDateTime hasta = manana.atStartOfDay();
            for (Object[] f : fechas("""
                    SELECT s.usuario.id, s.fechaInicio FROM SesionEntrenamiento s
                    WHERE s.completada = true AND s.usuario.id IN :ids
                      AND s.fechaInicio >= :desde AND s.fechaInicio < :hasta""", ids, desde, hasta)) {
                Cuenta c = cuentas.get((Integer) f[0]);
                if (c.apuntar(((LocalDateTime) f[1]).toLocalDate())) c.entreno = true;
            }
            // Una comida cuenta si lleva algún alimento: la comida vacía no es apuntar.
            for (Object[] f : fechas("""
                    SELECT DISTINCT c.usuario.id, c.fecha FROM AlimentoComida ac JOIN ac.comida c
                    WHERE c.usuario.id IN :ids AND c.fecha >= :desde AND c.fecha < :hasta""", ids, desde, hasta)) {
                Cuenta c = cuentas.get((Integer) f[0]);
                if (c.apuntar(((LocalDateTime) f[1]).toLocalDate())) c.comida = true;
            }
            for (Object[] f : fechas("""
                    SELECT m.usuario.id, m.fecha FROM MedicionCorporal m
                    WHERE m.usuario.id IN :ids AND m.fecha >= :desde AND m.fecha < :hasta""", ids, desde, hasta)) {
                cuentas.get((Integer) f[0]).apuntar(((LocalDateTime) f[1]).toLocalDate());
            }
            // El agua va por día, sin hora; un día con 0 ml es haber quitado los vasos.
            for (Object[] f : em.createQuery("""
                            SELECT a.usuarioId, a.fecha FROM AguaDiaria a
                            WHERE a.mililitros > 0 AND a.usuarioId IN :ids
                              AND a.fecha >= :desde AND a.fecha < :hasta""", Object[].class)
                    .setParameter("ids", ids)
                    .setParameter("desde", primerLunes)
                    .setParameter("hasta", manana)
                    .getResultList()) {
                cuentas.get((Integer) f[0]).apuntar((LocalDate) f[1]);
            }
        }

        Map<LocalDate, long[]> porSemana = new HashMap<>();
        for (Cuenta c : cuentas.values()) {
            long[] t = porSemana.computeIfAbsent(lunesDe(c.alta), k -> new long[4]);
            t[0]++;
            if (c.entreno) t[1]++;
            if (c.comida) t[2]++;
            if (c.volvio) t[3]++;
        }
        List<AdminEmbudoDTO.SemanaNucleo> semanas = new ArrayList<>();
        for (int i = 0; i < SEMANAS; i++) {
            LocalDate l = primerLunes.plusWeeks(i);
            long[] t = porSemana.getOrDefault(l, new long[4]);
            // La última alta posible es el domingo (l + 6), y sus 7 días acaban en l + 12.
            boolean completa = hoy.isAfter(l.plusDays(6 + DIAS_NUCLEO - 1));
            semanas.add(new AdminEmbudoDTO.SemanaNucleo(l, t[0], t[1], t[2], t[3], completa));
        }
        return semanas;
    }

    private List<Object[]> fechas(String jpql, Collection<Integer> ids, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery(jpql, Object[].class)
                .setParameter("ids", ids)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    private static LocalDate lunesDe(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // Medianoche de Madrid de ese día, expresada en el reloj del servidor.
    private static LocalDateTime relojServidor(LocalDate dia) {
        return dia.atStartOfDay(MADRID).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
