package com.gymprofit.api.integration;

import com.gymprofit.api.dto.admin.AdminResumenDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.admin.IAdminResumenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.gymprofit.api.service.admin.AdminResumenService.MADRID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AdminResumenTest — GP-085, GET /admin/resumen
//
// Se siembra en marzo de 2021, donde la base no tiene nada, y se pide el resumen
// «como si fuera» el miércoles 10 de marzo de 2021 a las 12:00 de Madrid. Así las
// semanas y los días salen exactos y el test no depende del día en que se ejecute.
//
// Semana actual: lunes 8 a domingo 14. Los casos del borde son los que importan:
//  - un alta el lunes 8 a las 00:30 de Madrid, que en UTC es aún domingo: cuenta
//    en esta semana (fecha_registro va en el reloj del servidor);
//  - una sesión el domingo 7 a las 23:30 de la pared: cuenta en la semana anterior;
//  - una sesión sin completar no cuenta; una cuenta que entrena dos veces cuenta una.
// Y solo ADMIN: USER e invitado, 403.
// ============================================================
@DisplayName("GP-085 — /admin/resumen con datos sembrados, en hora de Madrid")
class AdminResumenTest extends AbstractOwnershipTest {

    private static final ZonedDateTime AHORA = ZonedDateTime.of(2021, 3, 10, 12, 0, 0, 0, MADRID);
    private static final LocalDate HOY = LocalDate.of(2021, 3, 10);
    private static final LocalDate LUNES = LocalDate.of(2021, 3, 8);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private IUsuarioRepository usuarios;

    @Autowired
    private IAdminResumenService resumenService;

    // Hora de Madrid pasada al reloj del servidor, que es como se guarda fecha_registro.
    private static LocalDateTime relojServidor(LocalDateTime madrid) {
        return madrid.atZone(MADRID).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Usuario altaEl(String nombre, LocalDateTime madrid) {
        Usuario u = crearUsuario(nombre, RoleType.USER);
        u.setFechaRegistro(relojServidor(madrid));
        return usuarios.saveAndFlush(u);
    }

    private void sesion(Usuario u, LocalDateTime pared, boolean completada) {
        em.createNativeQuery("""
                        INSERT INTO sesiones_entrenamiento (usuario_id, fecha_inicio, fecha_fin, completada)
                        VALUES (?, ?, ?, ?)""")
                .setParameter(1, u.getId()).setParameter(2, pared).setParameter(3, pared.plusHours(1))
                .setParameter(4, completada).executeUpdate();
    }

    private void comida(Usuario u, LocalDateTime pared) {
        em.createNativeQuery("""
                        INSERT INTO comidas (usuario_id, tipo_comida, total_calorias, fecha)
                        VALUES (?, 'COMIDA', 500, ?)""")
                .setParameter(1, u.getId()).setParameter(2, pared).executeUpdate();
    }

    @Test
    @DisplayName("USER e invitado → 403")
    void solo_admin() throws Exception {
        for (Usuario quien : List.of(attacker, guest)) {
            pedir(quien, "GET /admin/resumen").andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("ADMIN → 200 con la forma completa, hoy en Madrid")
    void forma() throws Exception {
        Usuario admin = crearUsuario("__gp085_admin_res__", RoleType.ADMIN);
        pedir(admin, "GET /admin/resumen")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoy").value(LocalDate.now(MADRID).toString()))
                .andExpect(jsonPath("$.altasPorSemana.length()").value(8))
                .andExpect(jsonPath("$.sesionesPorDia.length()").value(14))
                .andExpect(jsonPath("$.sesionesPorDia[13].fecha").value(LocalDate.now(MADRID).toString()))
                .andExpect(jsonPath("$.cuentas.total").isNumber())
                .andExpect(jsonPath("$.catalogo.ejerciciosSinRevisar").isNumber())
                .andExpect(jsonPath("$.catalogo.alimentosSinIngles").isNumber());
    }

    @Test
    @DisplayName("altas por semana, con el lunes a las 00:30 de Madrid ya en la semana nueva")
    void altas() {
        altaEl("__gp085_a1__", LocalDateTime.of(2021, 3, 8, 0, 30));    // lunes, esta semana
        altaEl("__gp085_a2__", LocalDateTime.of(2021, 3, 10, 9, 0));    // hoy
        altaEl("__gp085_a3__", LocalDateTime.of(2021, 3, 7, 23, 50));   // domingo: la anterior
        altaEl("__gp085_a4__", LocalDateTime.of(2021, 1, 18, 10, 0));   // la más antigua de las 8
        altaEl("__gp085_a5__", LocalDateTime.of(2021, 1, 17, 10, 0));   // fuera de las 8

        AdminResumenDTO r = resumenService.resumen(AHORA);

        assertThat(r.hoy()).isEqualTo(HOY);
        assertThat(r.cuentas().altasSemana()).isEqualTo(2);
        assertThat(r.altasPorSemana()).hasSize(8);
        assertThat(r.altasPorSemana().get(0)).isEqualTo(new AdminResumenDTO.Semana(LocalDate.of(2021, 1, 18), 1));
        assertThat(r.altasPorSemana().get(6)).isEqualTo(new AdminResumenDTO.Semana(LocalDate.of(2021, 3, 1), 1));
        assertThat(r.altasPorSemana().get(7)).isEqualTo(new AdminResumenDTO.Semana(LUNES, 2));
        assertThat(r.altasPorSemana().stream().mapToLong(AdminResumenDTO.Semana::altas).sum()).isEqualTo(4);
    }

    @Test
    @DisplayName("sesiones por día, hoy, de la semana, y quién entrenó y quién apuntó comida")
    void actividad() {
        Usuario ana = altaEl("__gp085_ana__", LocalDateTime.of(2020, 1, 1, 10, 0));
        Usuario bea = altaEl("__gp085_bea__", LocalDateTime.of(2020, 1, 1, 10, 0));

        sesion(ana, LocalDateTime.of(2021, 3, 10, 8, 0), true);    // hoy
        sesion(ana, LocalDateTime.of(2021, 3, 8, 0, 10), true);    // lunes: esta semana
        sesion(bea, LocalDateTime.of(2021, 3, 10, 19, 0), false);  // sin completar: no cuenta
        sesion(bea, LocalDateTime.of(2021, 3, 7, 23, 30), true);   // domingo: la anterior
        sesion(bea, LocalDateTime.of(2021, 2, 25, 10, 0), true);   // fuera de los 14 días
        comida(ana, LocalDateTime.of(2021, 3, 10, 14, 0));
        comida(ana, LocalDateTime.of(2021, 3, 10, 21, 0));
        comida(bea, LocalDateTime.of(2021, 3, 9, 21, 0));          // ayer

        AdminResumenDTO r = resumenService.resumen(AHORA);

        assertThat(r.sesiones().hoy()).isEqualTo(1);
        assertThat(r.sesiones().semana()).isEqualTo(2);
        assertThat(r.entrenaronSemana()).isEqualTo(1);
        assertThat(r.comidaHoy()).isEqualTo(1);
        assertThat(r.sesionesPorDia()).hasSize(14);
        assertThat(r.sesionesPorDia().get(0).fecha()).isEqualTo(LocalDate.of(2021, 2, 25));
        assertThat(r.sesionesPorDia().get(13)).isEqualTo(new AdminResumenDTO.Dia(HOY, 1));
        assertThat(r.sesionesPorDia().get(12)).isEqualTo(new AdminResumenDTO.Dia(LocalDate.of(2021, 3, 9), 0));
        assertThat(r.sesionesPorDia().get(11)).isEqualTo(new AdminResumenDTO.Dia(LUNES, 1));
        assertThat(r.sesionesPorDia().get(10)).isEqualTo(new AdminResumenDTO.Dia(LocalDate.of(2021, 3, 7), 1));
        assertThat(r.sesionesPorDia().get(0).sesiones()).isEqualTo(1);
    }

    @Test
    @DisplayName("activas y totales cuentan de verdad: una desactivada baja las activas")
    void cuentas_y_totales() {
        AdminResumenDTO antes = resumenService.resumen(AHORA);
        Usuario c = altaEl("__gp085_c__", LocalDateTime.of(2020, 1, 1, 10, 0));
        c.setActivo(false);
        usuarios.saveAndFlush(c);
        sesion(c, LocalDateTime.of(2019, 5, 5, 10, 0), true);

        AdminResumenDTO despues = resumenService.resumen(AHORA);

        assertThat(despues.cuentas().total()).isEqualTo(antes.cuentas().total() + 1);
        assertThat(despues.cuentas().activas()).isEqualTo(antes.cuentas().activas());
        assertThat(despues.sesiones().total()).isEqualTo(antes.sesiones().total() + 1);
    }
}
