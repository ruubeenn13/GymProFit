package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.alimento.FrenoAvisos;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AvisosAlimentoTest — reportar un alimento y la lista de avisos de administración (1.6.1)
// Sin guardar quién: el alimento (o el código, si aún no está en el catálogo) y un motivo
// de una lista cerrada. Un aviso abierto por alimento y motivo, con las veces que se
// repite. Sin texto libre y sin usuario, para que no entre en la política ni en el
// borrado de la cuenta; el freno por cuenta vive en memoria.
// ============================================================
class AvisosAlimentoTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private ProductoOffService productoOffService;

    @Autowired
    private FrenoAvisos freno;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario admin;
    private Alimento catalogo;

    @BeforeEach
    void sembrar() {
        freno.reiniciar();
        admin = crearUsuario("__avisos_admin__", RoleType.ADMIN);
        catalogo = crearAlimentoCatalogo();
        catalogo.setNombre("Zaviso yogur");
        alimentoRepository.saveAndFlush(catalogo);
    }

    @AfterEach
    void sinRastro() {
        freno.reiniciar();
    }

    private static String porId(Integer id, String motivo) {
        return "{\"alimentoId\":" + id + ",\"motivo\":\"" + motivo + "\"}";
    }

    @Test
    @DisplayName("reportar un alimento del catálogo abre un aviso que el administrador ve")
    void reportar_y_ver() throws Exception {
        pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());

        JsonNode aviso = pendientes().get(0);
        assertThat(aviso.get("alimentoId").asInt()).isEqualTo(catalogo.getId());
        assertThat(aviso.get("nombre").asText()).isEqualTo("Zaviso yogur");
        assertThat(aviso.get("motivo").asText()).isEqualTo("VALORES");
        assertThat(aviso.get("veces").asInt()).isEqualTo(1);
        // El alimento entero, para editarlo con el editor de siempre.
        assertThat(aviso.get("alimento").get("id").asInt()).isEqualTo(catalogo.getId());
        assertThat(aviso.get("alimento").get("calorias").asInt()).isEqualTo(100);
    }

    @Test
    @DisplayName("un aviso por alimento y motivo: otra cuenta lo repite, otro motivo abre otro")
    void uno_por_alimento_y_motivo() throws Exception {
        pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());
        pedir(attacker, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());
        pedir(attacker, "POST /alimentos/avisos", porId(catalogo.getId(), "RACION")).andExpect(status().isNoContent());

        JsonNode lista = pendientes();
        assertThat(lista).hasSize(2);
        assertThat(veces(lista, "VALORES")).isEqualTo(2);
        assertThat(veces(lista, "RACION")).isEqualTo(1);
    }

    @Test
    @DisplayName("la misma cuenta repitiendo lo mismo no suma: el freno lo recuerda en memoria")
    void la_misma_cuenta_no_suma() throws Exception {
        for (int i = 0; i < 3; i++) {
            pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), "NOMBRE")).andExpect(status().isNoContent());
        }
        assertThat(veces(pendientes(), "NOMBRE")).isEqualTo(1);
    }

    @Test
    @DisplayName("el freno: más de 10 avisos en una hora de una cuenta no se guardan")
    void freno_por_cuenta() throws Exception {
        for (int i = 0; i < 12; i++) {
            Alimento a = crearAlimentoCatalogo();
            pedir(owner, "POST /alimentos/avisos", porId(a.getId(), "OTRO")).andExpect(status().isNoContent());
        }
        assertThat(pendientes()).hasSize(10);
        // Otra cuenta, sí.
        pedir(attacker, "POST /alimentos/avisos", porId(catalogo.getId(), "OTRO")).andExpect(status().isNoContent());
        assertThat(pendientes()).hasSize(11);
    }

    @Test
    @DisplayName("por código: un producto sin materializar se reporta por su código, con su nombre")
    void por_codigo() throws Exception {
        productoOffService.importarLote(List.of(new ProductoOffImportDTO("8400000730013", "Galletas zaviso",
                "Marca", 450.0, 7.0, 70.0, 16.0, null, null, null, null, 5, null, null)));
        pedir(owner, "POST /alimentos/avisos", "{\"barcode\":\"8400000730013\",\"motivo\":\"REPETIDO\"}")
                .andExpect(status().isNoContent());
        JsonNode aviso = pendientes().get(0);
        assertThat(aviso.get("alimentoId").isNull()).isTrue();
        assertThat(aviso.get("barcode").asText()).isEqualTo("8400000730013");
        assertThat(aviso.get("nombre").asText()).isEqualTo("Galletas zaviso");
        assertThat(aviso.get("alimento").isNull()).isTrue();

        // Un código que no está en ninguna parte, 404.
        pedir(owner, "POST /alimentos/avisos", "{\"barcode\":\"8400000730020\",\"motivo\":\"OTRO\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("por código de un producto ya en el catálogo: el aviso es del alimento")
    void por_codigo_materializado() throws Exception {
        catalogo.setBarcode("8400000730037");
        alimentoRepository.saveAndFlush(catalogo);
        pedir(owner, "POST /alimentos/avisos", "{\"barcode\":\"8400000730037\",\"motivo\":\"VALORES\"}")
                .andExpect(status().isNoContent());
        pedir(attacker, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());
        JsonNode lista = pendientes();
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("alimentoId").asInt()).isEqualTo(catalogo.getId());
        assertThat(lista.get(0).get("veces").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("DEC-027: el alimento de otro usuario, 403 sin decir nada de él; el tuyo, 400")
    void propiedad() throws Exception {
        Alimento privado = crearAlimentoCatalogo();
        privado.setUsuario(owner);
        privado.setNombre("Zaviso privado");
        alimentoRepository.saveAndFlush(privado);

        String cuerpo = pedir(attacker, "POST /alimentos/avisos", porId(privado.getId(), "VALORES"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).doesNotContain("Zaviso privado");
        // Lo tuyo lo arreglas tú: el administrador no ve tus alimentos.
        pedir(owner, "POST /alimentos/avisos", porId(privado.getId(), "VALORES")).andExpect(status().isBadRequest());
        assertThat(pendientes()).isEmpty();
    }

    @Test
    @DisplayName("datos que no cuadran: sin alimento, con los dos, motivo que no existe, id que no existe")
    void datos() throws Exception {
        pedir(owner, "POST /alimentos/avisos", "{\"motivo\":\"OTRO\"}").andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos/avisos", "{\"alimentoId\":" + catalogo.getId()
                + ",\"barcode\":\"8400000730013\",\"motivo\":\"OTRO\"}").andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), "ME_CAE_MAL")).andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), null)).andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos/avisos", porId(999_999_999, "OTRO")).andExpect(status().isNotFound());
        pedir(guest, "POST /alimentos/avisos", porId(catalogo.getId(), "OTRO")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("«Resuelto» lo quita de pendientes; un aviso nuevo después abre otro")
    void resolver() throws Exception {
        pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());
        int id = pendientes().get(0).get("id").asInt();
        pedir(admin, "PUT /admin/avisos-alimento/" + id + "/resuelto").andExpect(status().isNoContent());
        assertThat(pendientes()).isEmpty();

        pedir(attacker, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());
        JsonNode lista = pendientes();
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("veces").asInt()).isEqualTo(1);

        pedir(admin, "PUT /admin/avisos-alimento/999999999/resuelto").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/admin/avisos-alimento: USER e invitado, 403")
    void solo_admin() throws Exception {
        pedir(owner, "POST /alimentos/avisos", porId(catalogo.getId(), "VALORES")).andExpect(status().isNoContent());
        int id = pendientes().get(0).get("id").asInt();
        for (Usuario quien : List.of(owner, attacker, guest)) {
            pedir(quien, "GET /admin/avisos-alimento").andExpect(status().isForbidden());
            pedir(quien, "PUT /admin/avisos-alimento/" + id + "/resuelto").andExpect(status().isForbidden());
        }
        assertThat(pendientes()).hasSize(1);
    }

    @Test
    @DisplayName("la tabla no guarda quién: ninguna columna de usuario ni de texto libre")
    void sin_usuario() {
        List<String> columnas = jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'avisos_alimento'", String.class);
        assertThat(columnas).isNotEmpty();
        assertThat(columnas).noneMatch(c -> c.toLowerCase().contains("usuario") || c.toLowerCase().contains("texto")
                || c.toLowerCase().contains("comentario"));
    }

    // --- Andamiaje ----------------------------------------------------------

    private JsonNode pendientes() throws Exception {
        String json = pedir(admin, "GET /admin/avisos-alimento?size=100").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get("content");
    }

    private static int veces(JsonNode lista, String motivo) {
        for (JsonNode a : lista) if (motivo.equals(a.get("motivo").asText())) return a.get("veces").asInt();
        return 0;
    }
}
