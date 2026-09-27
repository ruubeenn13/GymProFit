package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AdminCuentasTest — GP-085, las cuentas en la web de administración
//
// Contexto entero y JWT real. Lo que fija:
//  - solo ADMIN: USER e invitado reciben 403 en las tres rutas, con un id ajeno
//    (DEC-014);
//  - la búsqueda por usuario o correo, los filtros y el total de páginas;
//  - ninguna respuesta lleva datos de salud;
//  - el borrado a petición: confirmación mala 400, la propia cuenta y otra ADMIN 409,
//    y en esos casos no se borra nada. Que el borrado bueno no deja filas lo prueba
//    BorradoCuentaTest, que es el que siembra todas las tablas;
//  - el último acceso se apunta al entrar y al renovar el token.
// ============================================================
@DisplayName("GP-085 — /admin/cuentas: listar, ficha y borrado a petición")
class AdminCuentasTest extends AbstractOwnershipTest {

    private static final List<String> SALUD = List.of("peso", "altura", "edad", "objetivo", "nivelExperiencia");

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private IUsuarioRepository usuarios;

    private Usuario admin;

    @BeforeEach
    void crearAdmin() {
        admin = crearUsuario("__gp085_admin__", RoleType.ADMIN);
    }

    private static String borrado(String confirmacion) {
        return "{\"confirmacion\":\"" + confirmacion + "\",\"motivo\":\"Petición por correo\"}";
    }

    private boolean existe(Usuario u) {
        em.flush();
        em.clear();
        return usuarios.findById(u.getId()).isPresent();
    }

    private LocalDateTime ultimoAcceso(Usuario u) {
        em.flush();
        em.clear();
        return usuarios.findById(u.getId()).orElseThrow().getUltimoAcceso();
    }

    @Test
    @DisplayName("USER e invitado → 403 en listar, ficha y borrar")
    void solo_admin() throws Exception {
        for (Usuario quien : List.of(attacker, guest)) {
            pedir(quien, "GET /admin/cuentas").andExpect(status().isForbidden());
            pedir(quien, "GET /admin/cuentas/" + owner.getId()).andExpect(status().isForbidden());
            pedir(quien, "DELETE /admin/cuentas/" + owner.getId(), borrado(OWNER))
                    .andExpect(status().isForbidden());
        }
        assertThat(existe(owner)).isTrue();
    }

    @Test
    @DisplayName("busca por usuario y por correo, filtra por rol y estado, y trae el total")
    void listar() throws Exception {
        pedir(admin, "GET /admin/cuentas?q=IDOR_OWNER&size=5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", hasItem(OWNER)))
                .andExpect(jsonPath("$.content[*].username", not(hasItem(ATTACKER))))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.content[0].rol").value("USER"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                // Instante con zona: sin ella, la web leería la hora del servidor como local.
                .andExpect(jsonPath("$.content[0].fechaRegistro",
                        org.hamcrest.Matchers.matchesPattern(".*(Z|[+-][0-9]{2}:[0-9]{2})$")));
        pedir(admin, "GET /admin/cuentas?q=" + ATTACKER + "@test.local")
                .andExpect(jsonPath("$.content[*].username", hasItem(ATTACKER)));
        pedir(admin, "GET /admin/cuentas?q=__idor&rol=GUEST")
                .andExpect(jsonPath("$.content[*].username", hasItem(GUEST)))
                .andExpect(jsonPath("$.content[*].username", not(hasItem(OWNER))));

        owner.setActivo(false);
        usuarios.saveAndFlush(owner);
        pedir(admin, "GET /admin/cuentas?q=__idor&activo=false")
                .andExpect(jsonPath("$.content[*].username", hasItem(OWNER)))
                .andExpect(jsonPath("$.content[*].username", not(hasItem(ATTACKER))));

        pedir(admin, "GET /admin/cuentas?rol=JEFE").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ni la lista ni la ficha llevan datos de salud")
    void sin_datos_de_salud() throws Exception {
        String lista = pedir(admin, "GET /admin/cuentas?q=" + OWNER)
                .andReturn().getResponse().getContentAsString();
        String ficha = pedir(admin, "GET /admin/cuentas/" + owner.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.username").value(OWNER))
                .andExpect(jsonPath("$.sesiones").value(0))
                .andExpect(jsonPath("$.comidas").value(0))
                .andReturn().getResponse().getContentAsString();
        for (String campo : SALUD) {
            assertThat(lista).doesNotContain("\"" + campo + "\"");
            assertThat(ficha).doesNotContain("\"" + campo + "\"");
        }
    }

    @Test
    @DisplayName("ficha de una cuenta que no existe → 404")
    void ficha_inexistente() throws Exception {
        pedir(admin, "GET /admin/cuentas/999999999").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("confirmación que no es su nombre de usuario, o sin motivo → 400 y no se borra")
    void confirmacion_mala() throws Exception {
        pedir(admin, "DELETE /admin/cuentas/" + owner.getId(), borrado(ATTACKER))
                .andExpect(status().isBadRequest());
        pedir(admin, "DELETE /admin/cuentas/" + owner.getId(), "{\"confirmacion\":\"" + OWNER + "\"}")
                .andExpect(status().isBadRequest());
        assertThat(existe(owner)).isTrue();
    }

    @Test
    @DisplayName("la propia cuenta y otra ADMIN → 409 y no se borran")
    void propia_y_admin() throws Exception {
        Usuario otroAdmin = crearUsuario("__gp085_otro_admin__", RoleType.ADMIN);
        pedir(admin, "DELETE /admin/cuentas/" + admin.getId(), borrado(admin.getUsername()))
                .andExpect(status().isConflict());
        pedir(admin, "DELETE /admin/cuentas/" + otroAdmin.getId(), borrado(otroAdmin.getUsername()))
                .andExpect(status().isConflict());
        assertThat(existe(admin)).isTrue();
        assertThat(existe(otroAdmin)).isTrue();
    }

    @Test
    @DisplayName("entrar y renovar el token apuntan el último acceso")
    void ultimo_acceso() throws Exception {
        assertThat(ultimoAcceso(owner)).isNull();

        MvcResult login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + OWNER + "\",\"password\":\"Test1234\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(ultimoAcceso(owner)).isNotNull();

        String refresh = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("refreshToken").asText();
        em.createNativeQuery("UPDATE usuarios SET ultimo_acceso = '2020-01-01 00:00:00' WHERE id = "
                + owner.getId()).executeUpdate();
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk());
        assertThat(ultimoAcceso(owner).getYear()).isGreaterThan(2020);
    }
}
