package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.alimento.IAlimentoService;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AlimentoEscrituraOwnershipTest — quién puede escribir en los alimentos
//
// El catálogo de alimentos (usuario_id NULL) lo ve todo el mundo, GUEST incluido,
// y los tokens de GUEST se dan sin credenciales. Con el registro abierto y
// POST /alimentos permitido a cualquier USER, omitir el usuarioId del cuerpo era
// escritura efectivamente anónima en la comida compartida. En una aplicación de
// nutrición el daño no es de privacidad: son macros falsos que la gente se cree.
//
// Lo que se fija aquí: el propietario sale del token (DEC-013), el catálogo es
// cosa de ADMIN, y el import de Open Food Facts —que SÍ crea catálogo legítimo y
// lo usa un usuario normal al escanear— sigue funcionando.
// ============================================================
@DisplayName("Escritura de alimentos — el catálogo no lo escribe cualquiera")
class AlimentoEscrituraOwnershipTest extends AbstractOwnershipTest {

    private static final String ADMIN = "__alim_admin__";

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private IAlimentoService alimentoService;

    // El cliente de Open Food Facts se simula: el test no puede depender de una API
    // de terceros, y lo que se comprueba es el efecto local del import, no la red.
    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    private Usuario admin;
    private Integer alimentoDeCatalogo;
    private Integer alimentoPrivadoDelOwner;

    @BeforeEach
    void sembrarAlimentos() {
        admin = crearUsuario(ADMIN, RoleType.ADMIN);

        alimentoDeCatalogo = crearAlimentoCatalogo().getId();

        Alimento propio = new Alimento();
        propio.setNombre("Tortilla del owner");
        propio.setCalorias(300);
        propio.setActivo(true);
        propio.setUsuario(owner);
        alimentoPrivadoDelOwner = alimentoRepository.save(propio).getId();
    }

    // --- Creación -----------------------------------------------------------

    @Test
    @DisplayName("un USER no crea un alimento a nombre de otro: nace suyo")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void user_no_crea_alimentos_a_nombre_de_otro() throws Exception {
        AlimentoCreateDTO dto = new AlimentoCreateDTO();
        dto.setNombre("Alimento con dueño falsificado");
        dto.setCalorias(100);
        dto.setUsuarioId(owner.getId());

        String respuesta = mockMvc.perform(post("/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // El usuarioId del cuerpo se ignora, no se obedece: el dueño sale del token.
        Integer id = objectMapper.readValue(respuesta, AlimentoDTO.class).getId();
        assertThat(duenoDe(id))
                .as("el alimento tiene que ser del que lo crea, no del que diga el cuerpo")
                .isEqualTo(attacker.getId());
    }

    @Test
    @DisplayName("un USER no crea catálogo: omitir el usuarioId no deja la fila sin dueño")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void user_no_crea_catalogo() throws Exception {
        AlimentoCreateDTO dto = new AlimentoCreateDTO();
        dto.setNombre("Alimento que quería ser catálogo");
        dto.setCalorias(100);
        // Sin usuarioId: así es como se colaba una fila con usuario_id NULL.

        String respuesta = mockMvc.perform(post("/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer id = objectMapper.readValue(respuesta, AlimentoDTO.class).getId();
        assertThat(duenoDe(id))
                .as("sin dueño sería catálogo público, y el catálogo no lo escribe un USER")
                .isEqualTo(attacker.getId());
    }

    @Test
    @DisplayName("un ADMIN sí crea catálogo")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void admin_si_crea_catalogo() throws Exception {
        AlimentoCreateDTO dto = new AlimentoCreateDTO();
        dto.setNombre("Alimento de catálogo legítimo");
        dto.setCalorias(100);

        String respuesta = mockMvc.perform(post("/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer id = objectMapper.readValue(respuesta, AlimentoDTO.class).getId();
        assertThat(duenoDe(id)).as("el panel de administración sí mantiene el catálogo").isNull();
    }

    // --- Edición ------------------------------------------------------------

    @Test
    @DisplayName("un USER no edita un alimento del catálogo → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void user_no_edita_el_catalogo() throws Exception {
        // Sin esto, el agujero de la creación se reabre por la puerta de al lado:
        // cambiarle los macros a un alimento del catálogo se los cambia a todo el mundo.
        mockMvc.perform(put("/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoDeEdicion(alimentoDeCatalogo))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un USER no edita el alimento privado de otro → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void user_no_edita_alimentos_ajenos() throws Exception {
        mockMvc.perform(put("/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoDeEdicion(alimentoPrivadoDelOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el dueño sí edita el suyo → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void el_dueno_edita_el_suyo() throws Exception {
        mockMvc.perform(put("/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoDeEdicion(alimentoPrivadoDelOwner))))
                .andExpect(status().isOk());
    }

    // --- El escaneo de códigos de barras sigue vivo -------------------------

    @Test
    @DisplayName("el import por código de barras sigue creando catálogo, y lo hace un USER")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void el_import_de_open_food_facts_sigue_funcionando() {
        AlimentoDTO externo = new AlimentoDTO();
        externo.setNombre("Producto escaneado");
        externo.setBarcode("8410000000000");
        externo.setCalorias(120);
        when(openFoodFactsClient.porBarcode(anyString())).thenReturn(Optional.of(externo));

        // Esta vía crea filas SIN dueño a propósito: un producto de Open Food Facts es
        // catálogo real, no la comida de nadie, y lo materializa un usuario normal al
        // escanear. Si el arreglo de arriba la hubiera tocado, se rompía el escáner.
        AlimentoDTO importado = alimentoService.importarPorBarcode("8410000000000");

        assertThat(duenoDe(importado.getId()))
                .as("el producto importado es catálogo, no del que escanea")
                .isNull();
    }

    // --- Andamiaje ----------------------------------------------------------

    // Dueño del alimento leído de la base de datos, no del DTO de la respuesta.
    private Integer duenoDe(Integer alimentoId) {
        Alimento alimento = alimentoRepository.findById(alimentoId).orElseThrow();
        return alimento.getUsuario() == null ? null : alimento.getUsuario().getId();
    }

    private AlimentoDTO dtoDeEdicion(Integer id) {
        AlimentoDTO dto = new AlimentoDTO();
        dto.setId(id);
        dto.setNombre("Macros falseados");
        dto.setCalorias(1);
        dto.setActivo(true);
        return dto;
    }
}
