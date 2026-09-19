package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.comida.IComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// AlimentoComidaOwnershipTest — e2e del 403 IDOR sobre /alimentos-comida.
// El ownership aquí es INDIRECTO: una relación alimento-comida no tiene dueño
// propio, sino que pertenece al usuario dueño de la comida asociada
// (comida.getUsuario()). Se siembra una comida del owner + una relación
// alimento-comida sobre ella, y se verifica que el atacante no puede
// leerla/borrarla ni listar los alimentos de esa comida ajena (403),
// mientras que el dueño sí (200).
// ============================================================
@DisplayName("IDOR /alimentos-comida — un usuario no accede a los alimentos de la comida de otro")
class AlimentoComidaOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IComidaService comidaService;

    @Autowired
    private IAlimentoComidaService alimentoComidaService;

    // Catálogo global de alimentos (sembrado por Flyway); se lee, no se crea.
    @Autowired
    private IAlimentoRepository alimentoRepository;

    // Id de la comida cuyo dueño es OWNER.
    private Integer comidaIdOwner;
    // Id de la relación alimento-comida asociada a esa comida.
    private Integer alimentoComidaIdOwner;

    // Alimento PERSONAL del owner: no es catálogo, tiene dueño, y por eso es el id que
    // el atacante no puede usar aunque la comida donde lo mete sea suya.
    private Integer alimentoPrivadoDelOwner;
    // Comida propia del ATACANTE y una línea suya, para atacar desde un recurso legítimo.
    private Integer comidaIdAttacker;
    private Integer alimentoComidaIdAttacker;

    // Siembra como owner: una comida propia + una relación alimento-comida sobre ella.
    // El service de comida fuerza usuarioId = usuario autenticado; el alimento se toma
    // del catálogo global ya existente.
    @BeforeEach
    void seedAlimentoComida() {
        runAs(owner, () -> {
            // 1) Comida del owner (mismo patrón que ComidaOwnershipTest).
            ComidaCreateDTO comidaDTO = new ComidaCreateDTO();
            comidaDTO.setUsuarioId(owner.getId());
            comidaDTO.setTipoComida("DESAYUNO");
            comidaIdOwner = comidaService.save(comidaDTO).getId();

            // 2) Alimento del catálogo global (findAll → primer elemento).
            // Siembra el alimento del catálogo (CI no lo trae por Flyway).
            Integer alimentoId = crearAlimentoCatalogo().getId();

            // 3) Relación alimento-comida sobre la comida del owner.
            AlimentoComidaCreateDTO acDTO = new AlimentoComidaCreateDTO();
            acDTO.setComidaId(comidaIdOwner);
            acDTO.setAlimentoId(alimentoId);
            acDTO.setCantidadGramos(new BigDecimal("100"));
            alimentoComidaIdOwner = alimentoComidaService.save(acDTO).getId();

            // 4) Alimento PERSONAL del owner: el dato ajeno que el atacante intentará leer.
            alimentoPrivadoDelOwner = crearAlimentoPersonalDe(owner).getId();
        });

        // El atacante tiene su propia comida y su propia línea: ataca desde algo legítimo,
        // que es lo que hacía que esto se colara — la comida sí era suya.
        runAs(attacker, () -> {
            ComidaCreateDTO comidaDTO = new ComidaCreateDTO();
            comidaDTO.setUsuarioId(attacker.getId());
            comidaDTO.setTipoComida("CENA");
            comidaIdAttacker = comidaService.save(comidaDTO).getId();

            AlimentoComidaCreateDTO acDTO = new AlimentoComidaCreateDTO();
            acDTO.setComidaId(comidaIdAttacker);
            acDTO.setAlimentoId(crearAlimentoCatalogo().getId());
            acDTO.setCantidadGramos(new BigDecimal("50"));
            alimentoComidaIdAttacker = alimentoComidaService.save(acDTO).getId();
        });
    }

    // Alimento con dueño (no catálogo). Se crea por repositorio y no por el service
    // para no depender de las reglas de creación de alimentos, que son otra ruta.
    private Alimento crearAlimentoPersonalDe(Usuario duenno) {
        Alimento alimento = new Alimento();
        alimento.setNombre("Alimento privado de " + duenno.getUsername());
        alimento.setCalorias(250);
        alimento.setActivo(true);
        alimento.setUsuario(duenno);
        return alimentoRepository.save(alimento);
    }

    @Test
    @DisplayName("GET alimento-comida ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentoComidaAjeno_devuelve403() throws Exception {
        mockMvc.perform(get("/alimentos-comida/" + alimentoComidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE alimento-comida ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteAlimentoComidaAjeno_devuelve403() throws Exception {
        mockMvc.perform(delete("/alimentos-comida/" + alimentoComidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET alimentos-comida/comida/{comidaAjena} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentosDeComidaAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/alimentos-comida/comida/" + comidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET alimento-comida propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentoComidaPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/alimentos-comida/" + alimentoComidaIdOwner))
                .andExpect(status().isOk());
    }

    // --- El id del alimento también tiene dueño -----------------------------

    @Test
    @DisplayName("POST metiendo el alimento privado de otro en la comida propia → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void postConAlimentoAjeno_devuelve403() throws Exception {
        AlimentoComidaCreateDTO dto = new AlimentoComidaCreateDTO();
        dto.setComidaId(comidaIdAttacker);
        dto.setAlimentoId(alimentoPrivadoDelOwner);
        dto.setCantidadGramos(new BigDecimal("100"));

        // La comida es suya, así que la comprobación de siempre pasa. Lo que no es suyo es
        // el alimento, y la respuesta devolvería su nombre, su categoría y sus macros.
        mockMvc.perform(post("/alimentos-comida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT cambiando la línea propia al alimento privado de otro → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void putConAlimentoAjeno_devuelve403() throws Exception {
        AlimentoComidaDTO dto = new AlimentoComidaDTO();
        dto.setId(alimentoComidaIdAttacker);
        dto.setComidaId(comidaIdAttacker);
        dto.setAlimentoId(alimentoPrivadoDelOwner);
        dto.setCantidadGramos(new BigDecimal("100"));

        // Misma puerta por la otra hoja: si solo se tapara el POST, bastaría con crear la
        // línea con un alimento de catálogo y luego cambiarla al ajeno.
        mockMvc.perform(put("/alimentos-comida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST con un alimento del catálogo → 200 (el caso normal sigue funcionando)")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void postConAlimentoDeCatalogo_devuelve200() throws Exception {
        // Un alimento sin dueño es de todos (DEC-027): negarlo rompería la aplicación
        // entera, que es de lo que hay que protegerse al arreglar una IDOR.
        AlimentoComidaCreateDTO dto = new AlimentoComidaCreateDTO();
        dto.setComidaId(comidaIdAttacker);
        dto.setAlimentoId(crearAlimentoCatalogo().getId());
        dto.setCantidadGramos(new BigDecimal("80"));

        mockMvc.perform(post("/alimentos-comida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
