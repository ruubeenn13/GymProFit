package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.ejerciciorealizado.IEjercicioRealizadoService;
import com.gymprofit.api.service.sesionentrenamiento.ISesionEntrenamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// EjercicioRealizadoOwnershipTest — e2e del aislamiento en /ejercicios-realizados.
// Ownership INDIRECTO: el ejercicio realizado no lleva usuarioId directo, la propiedad
// se resuelve vía sesion.getUsuario(). Siembra como owner una Sesion + un EjercicioRealizado
// sobre un ejercicio del catálogo y comprueba que el atacante no llega a ninguno.
//
// Hay dos familias de rutas y no se defienden igual, que es lo que había que separar:
//   - Las que llevan un id de SESIÓN o del propio registro: ese id SÍ es de un recurso
//     ajeno, así que la respuesta correcta es 403.
//   - Las que llevan un id de EJERCICIO DEL CATÁLOGO (/ejercicio/{id}, /count/ejercicio/
//     {id}): el catálogo es público y el mismo para todos, así que no hay «id ajeno» que
//     rechazar. Ahí el fallo era de ALCANCE —la consulta devolvía los registros de todos
//     los usuarios— y lo que se comprueba es que el atacante no ve nada del owner.
// ============================================================
@DisplayName("IDOR /ejercicios-realizados — un usuario no accede a los de otro (vía sesión)")
class EjercicioRealizadoOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IEjercicioRealizadoService ejercicioRealizadoService;

    @Autowired
    private ISesionEntrenamientoService sesionEntrenamientoService;

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    // Id del ejercicio realizado cuyo dueño (vía sesión) es OWNER.
    private Integer ejercicioRealizadoIdOwner;

    // Id de la sesión propiedad del OWNER a la que pertenece el ejercicio realizado.
    private Integer sesionIdOwner;

    // Id del ejercicio del CATÁLOGO global usado en la siembra. No pertenece a nadie: es
    // el que viaja en las rutas /ejercicio/{id}, donde el fallo era de alcance y no de
    // permisos, así que las subclases de tests lo necesitan para pedirlas.
    private Integer ejercicioIdCatalogo;

    // Siembra como owner: primero una Sesion (usuarioId=owner), luego un EjercicioRealizado
    // dentro de esa sesión, usando un ejercicioId del catálogo global (no lo crea).
    @BeforeEach
    void seedEjercicioRealizado() {
        // Siembra el ejercicio del catálogo (CI no lo trae por Flyway).
        ejercicioIdCatalogo = crearEjercicioCatalogo().getId();

        runAs(owner, () -> {
            // 1) Sesion del owner (solo usuarioId es @NotNull; el resto opcional).
            SesionEntrenamientoCreateDTO sesionDto = new SesionEntrenamientoCreateDTO();
            sesionDto.setUsuarioId(owner.getId());
            sesionDto.setFechaInicio(LocalDateTime.now());
            sesionDto.setDuracionMinutos(60);
            sesionDto.setCompletada(false);
            sesionIdOwner = sesionEntrenamientoService.save(sesionDto).getId();

            // 2) EjercicioRealizado dentro de esa sesión (sesionId + ejercicioId @NotNull).
            EjercicioRealizadoCreateDTO dto = new EjercicioRealizadoCreateDTO();
            dto.setSesionId(sesionIdOwner);
            dto.setEjercicioId(ejercicioIdCatalogo);
            // Métricas válidas mínimas (@Min(0) / @PositiveOrZero).
            dto.setSeriesCompletadas(3);
            dto.setRepeticionesReales(10);
            dto.setPesoUsado(BigDecimal.valueOf(40));
            dto.setTiempoSegundos(0);
            ejercicioRealizadoIdOwner = ejercicioRealizadoService.save(dto).getId();
        });
    }

    @Test
    @DisplayName("GET ejercicio realizado ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjercicioRealizadoAjeno_devuelve403() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/" + ejercicioRealizadoIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE ejercicio realizado ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteEjercicioRealizadoAjeno_devuelve403() throws Exception {
        mockMvc.perform(delete("/ejercicios-realizados/" + ejercicioRealizadoIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET ejercicios-realizados/sesion/{sesionAjena} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjerciciosDeSesionAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/sesion/" + sesionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET ejercicio realizado propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjercicioRealizadoPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/" + ejercicioRealizadoIdOwner))
                .andExpect(status().isOk());
    }

    // --- Rutas que llevan un id de EJERCICIO DEL CATÁLOGO, no de un recurso ajeno ------
    //
    // Aquí no hay 403 que dar: el id de la ruta es el de un ejercicio del catálogo, que es
    // público y el mismo para todos, así que no existe un «id ajeno» que rechazar. El
    // fallo era de ALCANCE, no de permisos —la consulta devolvía los registros de todos
    // los usuarios—, y lo que se comprueba es que un usuario no ve nada del otro.

    /**
     * El atacante pide el histórico del mismo ejercicio que acaba de registrar el owner.
     * Antes recibía la fila del owner —cuánto peso movió y qué día—; ahora recibe 404,
     * porque su propio histórico de ese ejercicio está vacío y el controlador traduce la
     * lista vacía a 404. Lo que importa no es el código, sino que el dato del owner no sale.
     */
    @Test
    @DisplayName("GET ejercicios-realizados/ejercicio/{id} no devuelve los registros de otro")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getPorEjercicio_noDevuelveRegistrosAjenos() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/ejercicio/" + ejercicioIdCatalogo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET ejercicios-realizados/ejercicio/{id} propio → 200 con un único registro")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getPorEjercicioPropio_devuelveSoloLosSuyos() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/ejercicio/" + ejercicioIdCatalogo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /**
     * Un contador global también es información ajena: revela que alguien más entrena ese
     * ejercicio y cuánto, sin devolver una sola fila. El atacante tiene que ver un cero.
     */
    @Test
    @DisplayName("GET count/ejercicio/{id} cuenta solo los propios, no los de todos")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void countPorEjercicio_noCuentaLosAjenos() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/count/ejercicio/" + ejercicioIdCatalogo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("GET count/ejercicio/{id} propio → cuenta 1 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void countPorEjercicioPropio_cuentaElSuyo() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/count/ejercicio/" + ejercicioIdCatalogo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    // --- Rutas con id de SESIÓN ajena: aquí sí toca 403 -------------------------------

    /**
     * Contar los ejercicios de una sesión ajena dibujaba el historial de entrenamiento de
     * cualquiera —qué días entrenó y cuánto hizo— iterando ids, sin leer ni un registro.
     */
    @Test
    @DisplayName("GET count/sesion/{sesionAjena} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void countDeSesionAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/count/sesion/" + sesionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET count/sesion/{sesionPropia} → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void countDeSesionPropia_devuelve200() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/count/sesion/" + sesionIdOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    /**
     * Un booleano también basta: preguntando ejercicio a ejercicio sobre una sesión ajena
     * se reconstruye el entrenamiento entero, un sí o un no cada vez.
     */
    @Test
    @DisplayName("GET exists/sesion/{sesionAjena}/ejercicio/{id} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void existsEnSesionAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/exists/sesion/" + sesionIdOwner
                            + "/ejercicio/" + ejercicioIdCatalogo))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET exists/sesion/{sesionPropia}/ejercicio/{id} → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void existsEnSesionPropia_devuelve200() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/exists/sesion/" + sesionIdOwner
                            + "/ejercicio/" + ejercicioIdCatalogo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
    }
}
