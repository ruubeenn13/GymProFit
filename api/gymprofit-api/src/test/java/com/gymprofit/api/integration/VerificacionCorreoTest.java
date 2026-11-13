package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.email.IEmailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// VerificacionCorreoTest — GP-045 con el contexto entero (DEC-066)
//
// El servicio de correo se sustituye por un mock solo para LEER el código que saldría
// hacia la bandeja: todo lo demás —filtro JWT, reglas de SecurityConfig, validación,
// base de datos— es el de verdad. Las peticiones llevan un JWT real (pedir()).
//
// Cubre los criterios de la ficha que necesitan la API entera: el alta manda el código
// y no falla si el correo falla, el perfil dice el estado, el código de una cuenta no
// verifica otra (la cuenta sale del token, DEC-013), el perfil no se puede verificar a
// mano, cambiar el correo deja la cuenta sin verificar sin cerrar la sesión, y una
// cuenta sin verificar se usa con normalidad.
// ============================================================
@DisplayName("GP-045 — verificación del correo con la API entera")
class VerificacionCorreoTest extends AbstractOwnershipTest {

    private static final String PASSWORD = "Frase corta de alta";
    private static final String VERIFICAR = "POST /usuarios/me/email/verificar";
    private static final String REENVIAR = "POST /usuarios/me/email/reenviar";

    @MockitoBean
    private IEmailService emailService;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    // --- El alta ------------------------------------------------------------

    @Test
    @DisplayName("el alta manda un código al correo de la cuenta y el perfil la da sin verificar")
    void el_alta_manda_el_codigo_y_la_cuenta_nace_sin_verificar() throws Exception {
        alta("gp045a@test.local").andExpect(status().isCreated());

        Usuario nuevo = usuarioRepository.findByEmail("gp045a@test.local").orElseThrow();
        String codigo = codigoMandadoA("gp045a@test.local");
        assertThat(codigo).matches("\\d{6}");

        pedir(nuevo, "GET /usuarios/" + nuevo.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerificado").value(false));
    }

    @Test
    @DisplayName("si el correo falla, el alta no falla")
    void el_alta_no_falla_si_el_correo_falla() throws Exception {
        doThrow(new RuntimeException("Brevo caído")).when(emailService)
                .enviarCodigoVerificacion(any(), any(), anyInt());

        alta("gp045b@test.local").andExpect(status().isCreated());

        assertThat(usuarioRepository.findByEmail("gp045b@test.local")).isPresent();
    }

    // --- Verificar ----------------------------------------------------------

    @Test
    @DisplayName("con el código bueno, el perfil pasa a verificado")
    void el_codigo_bueno_verifica_y_el_perfil_lo_dice() throws Exception {
        String codigo = reenviarYLeer(owner);

        pedir(owner, VERIFICAR, cuerpo(codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificado").value(true))
                .andExpect(jsonPath("$.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.verificadoEn").isNotEmpty());

        pedir(owner, "GET /usuarios/" + owner.getId())
                .andExpect(jsonPath("$.emailVerificado").value(true));
    }

    @Test
    @DisplayName("un código equivocado da 400 y la cuenta sigue sin verificar")
    void un_codigo_equivocado_da_400() throws Exception {
        String codigo = reenviarYLeer(owner);
        String malo = "000000".equals(codigo) ? "111111" : "000000";

        pedir(owner, VERIFICAR, cuerpo(malo)).andExpect(status().isBadRequest());

        assertThat(releer(owner).isEmailVerificado()).isFalse();
    }

    @Test
    @DisplayName("el código de una cuenta no verifica otra: la cuenta sale del token")
    void el_codigo_ajeno_no_verifica_la_cuenta_propia() throws Exception {
        String codigoDelOwner = reenviarYLeer(owner);

        pedir(attacker, VERIFICAR, cuerpo(codigoDelOwner)).andExpect(status().isBadRequest());

        assertThat(releer(attacker).isEmailVerificado()).isFalse();
        assertThat(releer(owner).isEmailVerificado()).isFalse();
    }

    @Test
    @DisplayName("el invitado no tiene correo que verificar")
    void el_invitado_no_entra() throws Exception {
        assertThat(estado(guest, REENVIAR)).isEqualTo(403);
        pedir(guest, VERIFICAR, cuerpo("123456")).andExpect(status().isForbidden());
    }

    // --- Reenviar -----------------------------------------------------------

    @Test
    @DisplayName("reenviar con el correo ya verificado da 409 y no manda nada")
    void reenviar_ya_verificado_es_409() throws Exception {
        verificarEnBase(owner);

        assertThat(estado(owner, REENVIAR)).isEqualTo(409);
        verify(emailService, never()).enviarCodigoVerificacion(any(), any(), anyInt());
    }

    // --- El estado no se toca por el perfil ---------------------------------

    @Test
    @DisplayName("ni PATCH ni PUT del perfil pueden marcar el correo como verificado")
    void el_perfil_no_verifica_el_correo() throws Exception {
        pedir(owner, "PATCH /usuarios/" + owner.getId(),
                json(Map.of("nombre", "Rubén", "emailVerificado", true)))
                .andExpect(status().isOk());
        assertThat(releer(owner).isEmailVerificado()).isFalse();

        // El PUT completo es de ADMIN, y solo sobre su propia cuenta (checkDueno).
        Usuario admin = crearUsuario("__gp045_admin__", com.gymprofit.api.enums.RoleType.ADMIN);
        pedir(admin, "PUT /usuarios",
                json(Map.of("id", admin.getId(), "email", admin.getEmail(), "emailVerificado", true)))
                .andExpect(status().isOk());

        assertThat(releer(admin).isEmailVerificado()).isFalse();
    }

    // --- Cambiar el correo --------------------------------------------------

    @Test
    @DisplayName("cambiar el correo lo deja sin verificar, manda código a la dirección nueva y no cierra la sesión")
    void cambiar_el_correo_reinicia_la_verificacion() throws Exception {
        String codigoViejo = reenviarYLeer(owner);
        verificarEnBase(owner);

        pedir(owner, "PUT /usuarios/me/email",
                json(Map.of("email", "gp045c@test.local", "password", "Test1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gp045c@test.local"))
                .andExpect(jsonPath("$.emailVerificado").value(false));

        String codigoNuevo = codigoMandadoA("gp045c@test.local");

        // El mismo token sigue valiendo: la cuenta no se pierde.
        pedir(owner, "GET /usuarios/" + owner.getId()).andExpect(status().isOk());

        // El código viejo era de la dirección vieja; solo el nuevo verifica la nueva.
        if (!codigoViejo.equals(codigoNuevo)) {
            pedir(owner, VERIFICAR, cuerpo(codigoViejo)).andExpect(status().isBadRequest());
        }
        pedir(owner, VERIFICAR, cuerpo(codigoNuevo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gp045c@test.local"))
                .andExpect(jsonPath("$.verificado").value(true));
    }

    // --- Una cuenta sin verificar se usa entera ------------------------------

    @Test
    @DisplayName("una cuenta sin verificar entra, lee y cambia su perfil con normalidad")
    void una_cuenta_sin_verificar_usa_la_app() throws Exception {
        assertThat(releer(owner).isEmailVerificado()).isFalse();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", OWNER, "password", "Test1234"))))
                .andExpect(status().isOk());
        pedir(owner, "GET /usuarios/" + owner.getId()).andExpect(status().isOk());
        pedir(owner, "GET /usuarios/" + owner.getId() + "/estadisticas").andExpect(status().isOk());
        pedir(owner, "PATCH /usuarios/" + owner.getId(), json(Map.of("nombre", "Rubén")))
                .andExpect(status().isOk());
    }

    // --- Apoyo --------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions alta(String email) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", PASSWORD, "nombre", "Lucía"))));
    }

    // Pide otro código como `quien` y devuelve los seis dígitos que salieron hacia su correo.
    private String reenviarYLeer(Usuario quien) throws Exception {
        assertThat(estado(quien, REENVIAR)).isEqualTo(204);
        return codigoMandadoA(quien.getEmail());
    }

    private String codigoMandadoA(String email) {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoVerificacion(
                argThat(u -> email.equals(u.getEmail())), codigo.capture(), anyInt());
        return codigo.getValue();
    }

    private void verificarEnBase(Usuario usuario) {
        Usuario u = releer(usuario);
        u.setEmailVerificado(true);
        u.setEmailVerificadoEn(java.time.LocalDateTime.now());
        usuarioRepository.saveAndFlush(u);
    }

    private Usuario releer(Usuario usuario) {
        em.flush();
        em.clear();
        return usuarioRepository.findById(usuario.getId()).orElseThrow();
    }

    private String cuerpo(String codigo) throws Exception {
        return json(Map.of("codigo", codigo));
    }

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }
}
