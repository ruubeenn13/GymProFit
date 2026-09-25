package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioLogroRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// GuardadoSesionCompletaTest — POST /sesiones/completa guarda todo o nada (GP-006).
//
// ESTE TEST *NO* ES @Transactional, Y ES LO QUE LO HACE VALER.
// Los demás e2e del proyecto envuelven el test en una transacción que se revierte
// al final, y eso aquí destruiría justo lo que hay que comprobar: si el guardado
// del servicio se uniera a la transacción del test, la sesión a medias seguiría
// viva en la sesión de Hibernate después del fallo y el test vería una fila que en
// producción no existe. Dicho de otro modo: con @Transactional el test de rollback
// no prueba el rollback, prueba el del propio test.
//
// El precio es limpiar a mano en @AfterEach.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GP-006 — la sesión se guarda entera o no se guarda")
class GuardadoSesionCompletaTest {

    private static final String USUARIO = "__gp006_owner__";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    @Autowired
    private ISesionEntrenamientoRepository sesionRepository;

    @Autowired
    private IEjercicioRealizadoRepository ejercicioRealizadoRepository;

    // Guardar una sesión completada desbloquea logros, y esas filas cuelgan del
    // usuario: sin borrarlas no se puede borrar el usuario al limpiar.
    @Autowired
    private IUsuarioLogroRepository usuarioLogroRepository;

    private Usuario usuario;
    private Integer ejercicioId;

    @BeforeEach
    void sembrar() {
        limpiar();

        List<Role> roles = roleRepository.findByNombreIn(List.of(RoleType.USER.getValue()));
        Usuario u = new Usuario();
        u.setUsername(USUARIO);
        u.setPassword(passwordEncoder.encode("Test1234"));
        u.setEmail(USUARIO + "@test.local");
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        u.setRoles(roles);
        usuario = usuarioRepository.save(u);

        Ejercicio e = new Ejercicio();
        e.setNombre("Ejercicio GP-006");
        e.setGrupoMuscular(GrupoMuscular.values()[0]);
        e.setDificultad(Dificultad.values()[0]);
        e.setActivo(true);
        ejercicioId = ejercicioRepository.save(e).getId();
    }

    @AfterEach
    void limpiarDespues() {
        limpiar();
    }

    // Borra en orden de dependencia: series (por cascada de la FK), ejercicios de la
    // sesión, sesiones, los logros desbloqueados, el ejercicio del catálogo y el usuario.
    private void limpiar() {
        usuarioRepository.findByUsername(USUARIO).ifPresent(u -> {
            for (SesionEntrenamiento s : sesionRepository.findByUsuarioId(u.getId())) {
                ejercicioRealizadoRepository.deleteAll(ejercicioRealizadoRepository.findBySesionId(s.getId()));
                sesionRepository.delete(s);
            }
            usuarioLogroRepository.deleteAll(usuarioLogroRepository.findByUsuarioId(u.getId()));
            usuarioRepository.delete(u);
        });
        ejercicioRepository.findAll().stream()
                .filter(e -> "Ejercicio GP-006".equals(e.getNombre()))
                .forEach(ejercicioRepository::delete);
    }

    // Cuerpo JSON escrito a mano: así el test fija el CONTRATO que ve la app, no la
    // forma que tengan hoy las clases Java.
    private String cuerpo(String clave, String ejerciciosJson) {
        return "{"
                + "\"claveIdempotencia\":\"" + clave + "\","
                + "\"duracionMinutos\":45,"
                + "\"valoracion\":4,"
                + "\"notas\":\"Buenas sensaciones\","
                + "\"completada\":true,"
                + "\"ejercicios\":[" + ejerciciosJson + "]"
                + "}";
    }

    private String ejercicio(int id) {
        return "{\"ejercicioId\":" + id + ",\"repeticionesReales\":10,\"series\":["
                + "{\"numero\":1,\"repeticiones\":10,\"peso\":60.00,\"completada\":true},"
                + "{\"numero\":2,\"repeticiones\":8,\"peso\":65.00,\"completada\":true}"
                + "]}";
    }

    @Test
    @DisplayName("Guarda sesión, ejercicios y series en una sola llamada")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void guardaTodoDeUnaVez() throws Exception {
        String respuesta = mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("clave-ok", ejercicio(ejercicioId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracion").value(4))
                .andExpect(jsonPath("$.notas").value("Buenas sensaciones"))
                .andReturn().getResponse().getContentAsString();

        int sesionId = objectMapper.readTree(respuesta).get("id").asInt();

        assertThat(sesionRepository.findByUsuarioId(usuario.getId())).hasSize(1);
        assertThat(ejercicioRealizadoRepository.findBySesionId(sesionId)).hasSize(1);
        // El resumen lo deduce el servidor de las series: dos completadas y el peso
        // más alto de las dos.
        assertThat(ejercicioRealizadoRepository.findBySesionId(sesionId).get(0).getSeriesCompletadas()).isEqualTo(2);
        assertThat(ejercicioRealizadoRepository.findBySesionId(sesionId).get(0).getSeries()).hasSize(2);
    }

    @Test
    @DisplayName("Borrar una sesión guardada con ejercicios la borra entera, series incluidas")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void borrarSesionConEjercicios() throws Exception {
        // Desde GP-006 toda sesión nueva lleva ejercicios, y la clave ajena de
        // ejercicios_realizados a la sesión no tiene ON DELETE CASCADE: borrar solo la
        // sesión rompía la restricción al confirmar y la API respondía 500. Con un test
        // @Transactional no se ve, porque el borrado no llega a la base.
        String respuesta = mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("clave-borrar", ejercicio(ejercicioId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int sesionId = objectMapper.readTree(respuesta).get("id").asInt();

        mockMvc.perform(delete("/sesiones/" + sesionId))
                .andExpect(status().is2xxSuccessful());

        assertThat(sesionRepository.findById(sesionId)).isEmpty();
        assertThat(ejercicioRealizadoRepository.findBySesionId(sesionId)).isEmpty();
    }

    @Test
    @DisplayName("Un ejercicio inválido EN MEDIO de la lista no deja ni la sesión")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void ejercicioInvalidoEnMedio_noDejaNada() throws Exception {
        String lista = ejercicio(ejercicioId) + "," + ejercicio(999999) + "," + ejercicio(ejercicioId);

        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("clave-rota", lista)))
                .andExpect(status().isNotFound());

        // Lo que importa: NO queda una sesión huérfana con el primer ejercicio dentro.
        assertThat(sesionRepository.findByUsuarioId(usuario.getId())).isEmpty();
    }

    @Test
    @DisplayName("Dos peticiones con la MISMA clave crean UNA sesión, y la segunda devuelve la primera")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void mismaClave_noDuplica() throws Exception {
        String cuerpo = cuerpo("clave-repetida", ejercicio(ejercicioId));

        String primera = mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String segunda = mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int idPrimera = objectMapper.readTree(primera).get("id").asInt();
        int idSegunda = objectMapper.readTree(segunda).get("id").asInt();

        assertThat(idSegunda).isEqualTo(idPrimera);
        assertThat(sesionRepository.findByUsuarioId(usuario.getId())).hasSize(1);
        // Y tampoco se duplican los ejercicios de dentro.
        assertThat(ejercicioRealizadoRepository.findBySesionId(idPrimera)).hasSize(1);
    }

    @Test
    @DisplayName("Dos claves distintas sí crean dos sesiones: el usuario puede entrenar dos veces")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void clavesDistintas_creanDos() throws Exception {
        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("clave-a", ejercicio(ejercicioId))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("clave-b", ejercicio(ejercicioId))))
                .andExpect(status().isOk());

        assertThat(sesionRepository.findByUsuarioId(usuario.getId())).hasSize(2);
    }

    @Test
    @DisplayName("Sin clave de idempotencia → 400: sin ella el reintento duplicaría")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sinClave_rechaza() throws Exception {
        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracionMinutos\":45,\"ejercicios\":[]}"))
                .andExpect(status().isBadRequest());

        assertThat(sesionRepository.findByUsuarioId(usuario.getId())).isEmpty();
    }

    @Test
    @DisplayName("Una sesión sin ejercicios es válida: el entrenamiento libre sin detalle existe")
    @WithUserDetails(value = USUARIO, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sinEjercicios_esValido() throws Exception {
        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("clave-libre", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        assertThat(sesionRepository.findByUsuarioId(usuario.getId())).hasSize(1);
    }
}
