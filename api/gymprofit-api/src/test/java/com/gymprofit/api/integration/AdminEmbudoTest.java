package com.gymprofit.api.integration;

import com.gymprofit.api.dto.admin.AdminEmbudoDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.PasoAlta;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.admin.IAdminEmbudoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.gymprofit.api.service.admin.AdminResumenService.MADRID;
import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// AdminEmbudoTest — GP-019, el embudo y el núcleo con datos sembrados
//
// Como AdminResumenTest: marzo de 2021, donde la base no tiene nada, y «ahora» es el
// miércoles 10 de marzo a las 12:00 de Madrid. La semana que se mira es la del lunes
// 1 al domingo 7, cuyas cuentas aún no han cumplido todas sus 7 días (completa=false).
//
// Cada cuenta prueba un borde del núcleo:
//  · a1: entrena el mismo día del alta → entrena, pero no ha vuelto.
//  · a2: apunta una comida con un alimento dos días después → come y vuelve.
//  · a3: una comida sin alimentos, agua a 0 ml, un entreno sin terminar y el peso al
//        octavo día → nada de eso cuenta.
//  · a4: alta el domingo 7 a las 23:50 de Madrid (en UTC, aún domingo o ya lunes según
//        la hora del servidor: se guarda en su reloj) y agua al día siguiente → vuelve.
//  · a5: alta el lunes 8 a las 00:30 de Madrid → semana siguiente, no esta.
// ============================================================
@DisplayName("GP-019 — /admin/embudo con datos sembrados, en hora de Madrid")
class AdminEmbudoTest extends AbstractOwnershipTest {

    private static final ZonedDateTime AHORA = ZonedDateTime.of(2021, 3, 10, 12, 0, 0, 0, MADRID);
    private static final LocalDate SEMANA = LocalDate.of(2021, 3, 1);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private IUsuarioRepository usuarios;

    @Autowired
    private IAdminEmbudoService servicio;

    private Alimento alimento;

    @BeforeEach
    void sembrarAlimento() {
        alimento = crearAlimentoCatalogo();
    }

    private Usuario altaEl(String nombre, LocalDateTime madrid) {
        Usuario u = crearUsuario(nombre, RoleType.USER);
        u.setFechaRegistro(madrid.atZone(MADRID).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        return usuarios.saveAndFlush(u);
    }

    private void sesion(Usuario u, LocalDateTime pared, boolean completada) {
        em.createNativeQuery("""
                        INSERT INTO sesiones_entrenamiento (usuario_id, fecha_inicio, fecha_fin, completada)
                        VALUES (?, ?, ?, ?)""")
                .setParameter(1, u.getId()).setParameter(2, pared).setParameter(3, pared.plusHours(1))
                .setParameter(4, completada).executeUpdate();
    }

    private void comida(Usuario u, LocalDateTime pared, boolean conAlimento) {
        em.createNativeQuery("""
                        INSERT INTO comidas (usuario_id, tipo_comida, total_calorias, fecha)
                        VALUES (?, 'COMIDA', 500, ?)""")
                .setParameter(1, u.getId()).setParameter(2, pared).executeUpdate();
        if (!conAlimento) return;
        Object id = em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        em.createNativeQuery("""
                        INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos,
                            proteinas_totales, carbohidratos_totales, grasas_totales)
                        VALUES (?, ?, 100, 0, 0, 0)""")
                .setParameter(1, ((Number) id).longValue()).setParameter(2, alimento.getId()).executeUpdate();
    }

    private void agua(Usuario u, LocalDate dia, int ml) {
        em.createNativeQuery("""
                        INSERT INTO agua_diaria (usuario_id, fecha, mililitros, actualizado)
                        VALUES (?, ?, ?, NOW())""")
                .setParameter(1, u.getId()).setParameter(2, dia).setParameter(3, ml).executeUpdate();
    }

    private void peso(Usuario u, LocalDateTime pared) {
        em.createNativeQuery("INSERT INTO mediciones_corporales (usuario_id, fecha, peso) VALUES (?, ?, 70)")
                .setParameter(1, u.getId()).setParameter(2, pared).executeUpdate();
    }

    private void pasos(LocalDate dia, PasoAlta paso, int total) {
        em.createNativeQuery("INSERT INTO alta_pasos (dia, paso, total) VALUES (?, ?, ?)")
                .setParameter(1, dia).setParameter(2, paso.name()).setParameter(3, BigInteger.valueOf(total))
                .executeUpdate();
    }

    @Test
    @DisplayName("núcleo de la semana del 1 de marzo: 4 cuentas, 1 entrena, 1 come, 2 vuelven")
    void nucleo() {
        Usuario a1 = altaEl("__gp019_a1__", LocalDateTime.of(2021, 3, 1, 10, 0));
        sesion(a1, LocalDateTime.of(2021, 3, 1, 19, 0), true);

        Usuario a2 = altaEl("__gp019_a2__", LocalDateTime.of(2021, 3, 2, 9, 0));
        comida(a2, LocalDateTime.of(2021, 3, 4, 14, 0), true);

        Usuario a3 = altaEl("__gp019_a3__", LocalDateTime.of(2021, 3, 3, 9, 0));
        comida(a3, LocalDateTime.of(2021, 3, 5, 14, 0), false);
        agua(a3, LocalDate.of(2021, 3, 6), 0);
        sesion(a3, LocalDateTime.of(2021, 3, 7, 18, 0), false);
        peso(a3, LocalDateTime.of(2021, 3, 10, 8, 0));

        Usuario a4 = altaEl("__gp019_a4__", LocalDateTime.of(2021, 3, 7, 23, 50));
        agua(a4, LocalDate.of(2021, 3, 8), 250);

        Usuario a5 = altaEl("__gp019_a5__", LocalDateTime.of(2021, 3, 8, 0, 30));
        sesion(a5, LocalDateTime.of(2021, 3, 9, 18, 0), true);

        AdminEmbudoDTO r = servicio.embudo(AHORA);

        assertThat(r.hoy()).isEqualTo(LocalDate.of(2021, 3, 10));
        assertThat(r.nucleo()).hasSize(8);
        assertThat(r.nucleo().get(6)).isEqualTo(new AdminEmbudoDTO.SemanaNucleo(SEMANA, 4, 1, 1, 2, false));
        // a5 entrena al día siguiente del alta: entrena y vuelve.
        assertThat(r.nucleo().get(7)).isEqualTo(
                new AdminEmbudoDTO.SemanaNucleo(LocalDate.of(2021, 3, 8), 1, 1, 0, 1, false));
        // La del 22 de febrero: su última alta posible (el 28) cumplió sus 7 días el 6 de marzo.
        assertThat(r.nucleo().get(5).completa()).isTrue();
        assertThat(r.nucleo().get(5).cuentas()).isZero();
    }

    @Test
    @DisplayName("embudo por semana: los días se suman en su semana y salen los 10 pasos en orden")
    void embudo() {
        pasos(LocalDate.of(2021, 3, 1), PasoAlta.OBJETIVO, 3);
        pasos(LocalDate.of(2021, 3, 7), PasoAlta.OBJETIVO, 2);
        pasos(LocalDate.of(2021, 3, 7), PasoAlta.CUENTA, 1);
        pasos(LocalDate.of(2021, 3, 8), PasoAlta.BIENVENIDA, 5);
        pasos(LocalDate.of(2021, 1, 17), PasoAlta.BIENVENIDA, 9); // fuera de las 8 semanas

        AdminEmbudoDTO r = servicio.embudo(AHORA);

        assertThat(r.embudo()).hasSize(8);
        assertThat(r.embudo().get(0).lunes()).isEqualTo(LocalDate.of(2021, 1, 18));
        AdminEmbudoDTO.SemanaEmbudo semana = r.embudo().get(6);
        assertThat(semana.lunes()).isEqualTo(SEMANA);
        assertThat(semana.pasos()).extracting(AdminEmbudoDTO.PasoTotal::paso).containsExactly(PasoAlta.values());
        assertThat(semana.pasos().get(PasoAlta.OBJETIVO.ordinal()).total()).isEqualTo(5);
        assertThat(semana.pasos().get(PasoAlta.CUENTA.ordinal()).total()).isEqualTo(1);
        assertThat(semana.pasos().get(PasoAlta.BIENVENIDA.ordinal()).total()).isZero();
        assertThat(r.embudo().get(7).pasos().get(PasoAlta.BIENVENIDA.ordinal()).total()).isEqualTo(5);
        assertThat(r.embudo().stream().mapToLong(s -> s.pasos().stream().mapToLong(AdminEmbudoDTO.PasoTotal::total).sum())
                .sum()).isEqualTo(11);
    }
}
