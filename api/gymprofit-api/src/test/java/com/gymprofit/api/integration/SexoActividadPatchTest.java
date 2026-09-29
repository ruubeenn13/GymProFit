package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.NivelActividad;
import com.gymprofit.api.enums.Sexo;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SexoActividadPatchTest — GP-111
//
// El sexo y el nivel de actividad vivían solo en el móvil: quien reinstalaba tenía las
// calorías calculadas como hombre moderado sin saberlo. Pasan a la API como campos
// opcionales del PATCH del perfil y de UsuarioDTO. Aquí se prueba que un valor fuera
// de la lista da 400 sin tocar nada, y que cada campo solo toca lo suyo: las builds ya
// repartidas no los mandan y no deben borrarlos.
// ============================================================
@DisplayName("GP-111 — sexo y nivel de actividad en el perfil")
class SexoActividadPatchTest extends AbstractOwnershipTest {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    private String ruta() {
        return "PATCH /usuarios/" + owner.getId();
    }

    private Usuario releer() {
        em.flush();
        em.clear();
        return usuarioRepository.findById(owner.getId()).orElseThrow();
    }

    @Test
    @DisplayName("guarda el sexo y la actividad, y los devuelve en el PATCH y en el GET")
    void guarda_y_devuelve() throws Exception {
        pedir(owner, ruta(), "{\"sexo\":\"MUJER\",\"nivelActividad\":\"ACTIVO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sexo").value("MUJER"))
                .andExpect(jsonPath("$.nivelActividad").value("ACTIVO"));

        Usuario leido = releer();
        assertThat(leido.getSexo()).isEqualTo(Sexo.MUJER);
        assertThat(leido.getNivelActividad()).isEqualTo(NivelActividad.ACTIVO);

        pedir(owner, "GET /usuarios/" + owner.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sexo").value("MUJER"))
                .andExpect(jsonPath("$.nivelActividad").value("ACTIVO"));
    }

    @Test
    @DisplayName("una cuenta sin ellos los devuelve a null")
    void sin_ellos_null() throws Exception {
        pedir(owner, "GET /usuarios/" + owner.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sexo").doesNotExist())
                .andExpect(jsonPath("$.nivelActividad").doesNotExist());
    }

    @Test
    @DisplayName("un sexo fuera de la lista da 400 y no cambia nada")
    void sexo_invalido_400() throws Exception {
        pedir(owner, ruta(), "{\"sexo\":\"OTRO\",\"edad\":40}")
                .andExpect(status().isBadRequest());

        Usuario leido = releer();
        assertThat(leido.getSexo()).isNull();
        assertThat(leido.getEdad()).as("el 400 no aplica el resto del cuerpo").isNull();
    }

    @Test
    @DisplayName("una actividad fuera de la lista da 400 y no cambia nada")
    void actividad_invalida_400() throws Exception {
        pedir(owner, ruta(), "{\"nivelActividad\":\"MUCHISIMO\"}")
                .andExpect(status().isBadRequest());

        assertThat(releer().getNivelActividad()).isNull();
    }

    @Test
    @DisplayName("mandar solo el sexo no toca la actividad, y al revés")
    void cada_campo_lo_suyo() throws Exception {
        pedir(owner, ruta(), "{\"sexo\":\"MUJER\",\"nivelActividad\":\"LIGERO\"}")
                .andExpect(status().isOk());

        pedir(owner, ruta(), "{\"sexo\":\"HOMBRE\"}").andExpect(status().isOk());
        Usuario leido = releer();
        assertThat(leido.getSexo()).isEqualTo(Sexo.HOMBRE);
        assertThat(leido.getNivelActividad()).isEqualTo(NivelActividad.LIGERO);

        pedir(owner, ruta(), "{\"nivelActividad\":\"SEDENTARIO\"}").andExpect(status().isOk());
        leido = releer();
        assertThat(leido.getSexo()).isEqualTo(Sexo.HOMBRE);
        assertThat(leido.getNivelActividad()).isEqualTo(NivelActividad.SEDENTARIO);
    }

    @Test
    @DisplayName("el PATCH de una build repartida, sin los campos nuevos, no los borra")
    void build_vieja_no_los_borra() throws Exception {
        pedir(owner, ruta(), "{\"sexo\":\"MUJER\",\"nivelActividad\":\"ACTIVO\"}")
                .andExpect(status().isOk());

        // Lo que manda Editar perfil de la 1.1.2: sin sexo ni actividad, y nulls explícitos.
        pedir(owner, ruta(), "{\"peso\":70.5,\"altura\":null,\"edad\":30,"
                + "\"nivelExperiencia\":\"INTERMEDIO\",\"objetivo\":\"MANTENER_PESO\"}")
                .andExpect(status().isOk());

        Usuario leido = releer();
        assertThat(leido.getSexo()).isEqualTo(Sexo.MUJER);
        assertThat(leido.getNivelActividad()).isEqualTo(NivelActividad.ACTIVO);
        assertThat(leido.getEdad()).isEqualTo(30);
    }

    @Test
    @DisplayName("null en el cuerpo no borra el valor guardado")
    void null_no_borra() throws Exception {
        pedir(owner, ruta(), "{\"sexo\":\"MUJER\",\"nivelActividad\":\"ACTIVO\"}")
                .andExpect(status().isOk());
        pedir(owner, ruta(), "{\"sexo\":null,\"nivelActividad\":null}")
                .andExpect(status().isOk());

        Usuario leido = releer();
        assertThat(leido.getSexo()).isEqualTo(Sexo.MUJER);
        assertThat(leido.getNivelActividad()).isEqualTo(NivelActividad.ACTIVO);
    }
}
