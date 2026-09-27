package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminResumenDTO;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

// ============================================================
// AdminResumenService — los números de la pantalla de Resumen (GP-085)
//
// Días y semanas de lunes a domingo, en hora de Madrid. Las fechas de la base no
// están todas en el mismo reloj, y aquí se trata cada una como lo que es:
//
//  · fecha_registro la pone el servidor con su reloj (en Render, UTC). Los límites
//    del día de Madrid se pasan a ese reloj antes de preguntar, y cada alta se
//    lleva a Madrid antes de contarla en su semana.
//  · fecha_inicio de una sesión y fecha de una comida son la hora de la pared del
//    teléfono de quien las apuntó. Se toman tal cual, como hora de Madrid.
//
// Solo recuentos, sin cargar entidades: nada de lo que sale de aquí es de nadie.
// ============================================================
@Service
@RequiredArgsConstructor
public class AdminResumenService implements IAdminResumenService {

    /** Zona en la que se cuentan los días y las semanas. */
    public static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private static final int SEMANAS = 8;
    private static final int DIAS = 14;

    @PersistenceContext
    private EntityManager em;

    private final IEjercicioRepository ejercicioRepository;
    private final IAlimentoRepository alimentoRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminResumenDTO resumen() {
        return resumen(ZonedDateTime.now(MADRID));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResumenDTO resumen(ZonedDateTime ahora) {
        LocalDate hoy = ahora.withZoneSameInstant(MADRID).toLocalDate();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primerLunes = lunes.minusWeeks(SEMANAS - 1);
        LocalDate primerDia = hoy.minusDays(DIAS - 1);
        LocalDate manana = hoy.plusDays(1);

        // --- Cuentas (reloj del servidor) ---------------------------------
        List<LocalDateTime> altas = em.createQuery(
                        "SELECT u.fechaRegistro FROM Usuario u WHERE u.fechaRegistro >= :desde AND u.fechaRegistro < :hasta",
                        LocalDateTime.class)
                .setParameter("desde", relojServidor(primerLunes))
                .setParameter("hasta", relojServidor(manana))
                .getResultList();
        Map<LocalDate, Long> altasPorLunes = contar(altas.stream()
                .map(f -> f.atZone(ZoneId.systemDefault()).withZoneSameInstant(MADRID).toLocalDate())
                .map(d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))).toList());
        List<AdminResumenDTO.Semana> semanas = new ArrayList<>();
        for (int i = 0; i < SEMANAS; i++) {
            LocalDate l = primerLunes.plusWeeks(i);
            semanas.add(new AdminResumenDTO.Semana(l, altasPorLunes.getOrDefault(l, 0L)));
        }

        AdminResumenDTO.Cuentas cuentas = new AdminResumenDTO.Cuentas(
                contar("SELECT COUNT(u) FROM Usuario u"),
                altasPorLunes.getOrDefault(lunes, 0L),
                contar("SELECT COUNT(u) FROM Usuario u WHERE u.activo = true"));

        // --- Sesiones (hora de la pared, tomada como Madrid) ---------------
        String completadas = "s.completada = true AND s.fechaInicio >= :desde AND s.fechaInicio < :hasta";
        List<LocalDateTime> inicios = em.createQuery(
                        "SELECT s.fechaInicio FROM SesionEntrenamiento s WHERE " + completadas, LocalDateTime.class)
                .setParameter("desde", primerDia.atStartOfDay())
                .setParameter("hasta", manana.atStartOfDay())
                .getResultList();
        Map<LocalDate, Long> porDia = contar(inicios.stream().map(LocalDateTime::toLocalDate).toList());
        List<AdminResumenDTO.Dia> dias = new ArrayList<>();
        for (int i = 0; i < DIAS; i++) {
            LocalDate d = primerDia.plusDays(i);
            dias.add(new AdminResumenDTO.Dia(d, porDia.getOrDefault(d, 0L)));
        }
        // La semana cabe entera en los 14 días: se suma de lo ya contado.
        long sesionesSemana = porDia.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(lunes)).mapToLong(Map.Entry::getValue).sum();

        AdminResumenDTO.Sesiones sesiones = new AdminResumenDTO.Sesiones(
                porDia.getOrDefault(hoy, 0L), sesionesSemana,
                contar("SELECT COUNT(s) FROM SesionEntrenamiento s WHERE s.completada = true"));

        long entrenaronSemana = em.createQuery(
                        "SELECT COUNT(DISTINCT s.usuario.id) FROM SesionEntrenamiento s WHERE " + completadas, Long.class)
                .setParameter("desde", lunes.atStartOfDay())
                .setParameter("hasta", manana.atStartOfDay())
                .getSingleResult();

        long comidaHoy = em.createQuery("""
                        SELECT COUNT(DISTINCT c.usuario.id) FROM Comida c
                        WHERE c.fecha >= :desde AND c.fecha < :hasta""", Long.class)
                .setParameter("desde", hoy.atStartOfDay())
                .setParameter("hasta", manana.atStartOfDay())
                .getSingleResult();

        AdminResumenDTO.Catalogo catalogo = new AdminResumenDTO.Catalogo(
                ejercicioRepository.countByActivoTrue(),
                ejercicioRepository.countByActivoTrueAndNombreRevisadoFalse(),
                alimentoRepository.countByUsuarioIsNull(),
                alimentoRepository.contarCatalogoSinIngles());

        return new AdminResumenDTO(hoy, cuentas, sesiones, entrenaronSemana, comidaHoy, semanas, dias, catalogo);
    }

    // Medianoche de Madrid de ese día, expresada en el reloj del servidor.
    private static LocalDateTime relojServidor(LocalDate dia) {
        return dia.atStartOfDay(MADRID).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private long contar(String jpql) {
        return em.createQuery(jpql, Long.class).getSingleResult();
    }

    private static Map<LocalDate, Long> contar(List<LocalDate> fechas) {
        return fechas.stream().collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    }
}
