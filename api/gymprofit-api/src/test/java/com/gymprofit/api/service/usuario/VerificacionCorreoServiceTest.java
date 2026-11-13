package com.gymprofit.api.service.usuario;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.usuario.EstadoCorreoDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.entity.VerificacionCorreoCodigo;
import com.gymprofit.api.exceptions.ConflictEntityException;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.repository.jpa.IVerificacionCorreoCodigoRepository;
import com.gymprofit.api.service.email.IEmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// VerificacionCorreoServiceTest — GP-045, las reglas del código de verificación
//
// Mismo planteamiento que PasswordResetServiceTest, porque las reglas son las mismas
// (DEC-066): BCrypt real de coste 4, para que casar el código con su hash lo decida el
// servicio y no un stub, y un repositorio simulado CON memoria que filtra como la
// consulta de Spring Data (ni usado, ni caducado, id más alto). El tiempo pasa
// retrasando las fechas del código guardado.
//
// Lo que añade este test sobre aquél: el código está atado a la dirección a la que se
// mandó, verificar una cuenta ya verificada no gasta nada, reenviar a una verificada es
// un 409, y un fallo del envío no tumba a quien emite.
// ============================================================
@DisplayName("VerificacionCorreoService — verificar el correo con un código (GP-045)")
class VerificacionCorreoServiceTest {

    private static final String EMAIL = "ruben@example.com";

    // Los de DEC-066. Si cambian en el servicio, estos tests caen.
    private static final int MINUTOS_VALIDEZ = 15;
    private static final int MAX_INTENTOS = 5;

    private IUsuarioRepository usuarioRepository;
    private IVerificacionCorreoCodigoRepository codigoRepository;
    private IEmailService emailService;
    private SecurityUtils securityUtils;
    private PasswordEncoder passwordEncoder;
    private VerificacionCorreoService service;

    private Usuario usuario;

    private final List<VerificacionCorreoCodigo> almacen = new ArrayList<>();
    private int siguienteId = 1;

    private ListAppender<ILoggingEvent> registro;
    private ch.qos.logback.classic.Logger logger;
    private Level nivelPrevio;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(IUsuarioRepository.class);
        codigoRepository = mock(IVerificacionCorreoCodigoRepository.class);
        emailService = mock(IEmailService.class);
        securityUtils = mock(SecurityUtils.class);
        passwordEncoder = new BCryptPasswordEncoder(4);

        service = new VerificacionCorreoService(usuarioRepository, codigoRepository, emailService,
                passwordEncoder, securityUtils);

        usuario = new Usuario();
        usuario.setId(7);
        usuario.setUsername("ruben");
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);

        when(securityUtils.getCurrentUserId()).thenReturn(7);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        cablearAlmacen();
        engancharElLog();
    }

    @AfterEach
    void soltarElLog() {
        logger.detachAppender(registro);
        registro.stop();
        logger.setLevel(nivelPrevio);
    }

    // --- Emisión ------------------------------------------------------------

    @Test
    @DisplayName("el código se guarda hasheado, atado al correo y con 15 minutos de vida")
    void el_codigo_se_emite_hasheado_atado_al_correo_y_caduca_a_los_quince_minutos() {
        String codigo = emitir();
        VerificacionCorreoCodigo guardado = ultimo();

        assertThat(codigo).matches("\\d{6}");
        assertThat(guardado.getCodigoHash()).isNotEqualTo(codigo);
        assertThat(passwordEncoder.matches(codigo, guardado.getCodigoHash())).isTrue();
        assertThat(guardado.getEmail()).isEqualTo(EMAIL);
        assertThat(guardado.isUsado()).isFalse();
        assertThat(guardado.getIntentos()).isZero();
        assertThat(guardado.getFechaExpiracion())
                .isAfterOrEqualTo(guardado.getFechaCreacion().plusMinutes(MINUTOS_VALIDEZ))
                .isBefore(guardado.getFechaCreacion().plusMinutes(MINUTOS_VALIDEZ).plusSeconds(5));
        verify(emailService).enviarCodigoVerificacion(usuario, codigo, MINUTOS_VALIDEZ);
    }

    @Test
    @DisplayName("emitir un código nuevo mata al anterior")
    void un_codigo_nuevo_invalida_el_anterior() {
        String primero = emitir();
        String segundo = emitir();

        assertThatThrownBy(() -> service.verificar(primero)).isInstanceOf(InvalidDataException.class);
        assertThat(service.verificar(segundo).isVerificado()).isTrue();
    }

    @Test
    @DisplayName("si el envío falla, emitir no lanza y el código queda guardado")
    void un_fallo_del_envio_no_se_propaga() {
        doThrow(new RuntimeException("Brevo caído")).when(emailService)
                .enviarCodigoVerificacion(any(), any(), anyInt());

        assertThatCode(() -> service.emitirCodigo(usuario)).doesNotThrowAnyException();
        assertThat(almacen).hasSize(1);
    }

    // --- Canje --------------------------------------------------------------

    @Test
    @DisplayName("el código bueno verifica el correo y guarda cuándo")
    void el_codigo_correcto_verifica_el_correo() {
        String codigo = emitir();

        EstadoCorreoDTO estado = service.verificar(codigo);

        assertThat(estado.isVerificado()).isTrue();
        assertThat(estado.getEmail()).isEqualTo(EMAIL);
        assertThat(estado.getVerificadoEn()).isNotNull();
        assertThat(usuario.isEmailVerificado()).isTrue();
        assertThat(usuario.getEmailVerificadoEn()).isNotNull();
        assertThat(ultimo().isUsado()).isTrue();
    }

    @Test
    @DisplayName("a los 14 minutos sirve; a los 15 minutos y un segundo, no")
    void el_plazo_es_de_quince_minutos() {
        String codigo = emitir();
        adelantarElReloj(ultimo(), 14, 0);
        assertThat(service.verificar(codigo).isVerificado()).isTrue();

        usuario.setEmailVerificado(false);
        String otro = emitir();
        adelantarElReloj(ultimo(), MINUTOS_VALIDEZ, 1);
        assertThatThrownBy(() -> service.verificar(otro)).isInstanceOf(InvalidDataException.class);
        assertThat(usuario.isEmailVerificado()).isFalse();
    }

    @Test
    @DisplayName("tras cinco fallos el código se quema, aunque luego llegue el bueno")
    void cinco_intentos_y_el_codigo_muere() {
        String codigo = emitir();
        String malo = "000000".equals(codigo) ? "111111" : "000000";

        for (int i = 1; i <= MAX_INTENTOS; i++) {
            assertThatThrownBy(() -> service.verificar(malo)).isInstanceOf(InvalidDataException.class);
        }
        assertThat(ultimo().getIntentos()).isEqualTo(MAX_INTENTOS);

        assertThatThrownBy(() -> service.verificar(codigo)).isInstanceOf(InvalidDataException.class);
        assertThat(usuario.isEmailVerificado()).isFalse();
        assertThat(ultimo().isUsado()).isTrue();
    }

    @Test
    @DisplayName("un código sirve una sola vez")
    void un_solo_uso() {
        String codigo = emitir();
        assertThat(service.verificar(codigo).isVerificado()).isTrue();

        // El correo vuelve a estar sin verificar (como tras cambiarlo): el código ya
        // usado no lo verifica otra vez.
        usuario.setEmailVerificado(false);
        usuario.setEmailVerificadoEn(null);
        assertThatThrownBy(() -> service.verificar(codigo)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("un código mandado a otra dirección no verifica la actual")
    void el_codigo_esta_atado_a_la_direccion() {
        String codigo = emitir();
        usuario.setEmail("otra@example.com");

        assertThatThrownBy(() -> service.verificar(codigo)).isInstanceOf(InvalidDataException.class);
        assertThat(usuario.isEmailVerificado()).isFalse();
    }

    @Test
    @DisplayName("todos los fallos dan el mismo error")
    void el_mismo_error_para_todos_los_fallos() {
        Throwable sinCodigo = capturar(() -> service.verificar("123456"));
        String codigo = emitir();
        String malo = "000000".equals(codigo) ? "111111" : "000000";
        Throwable equivocado = capturar(() -> service.verificar(malo));
        adelantarElReloj(ultimo(), MINUTOS_VALIDEZ, 1);
        Throwable caducado = capturar(() -> service.verificar(codigo));

        assertThat(List.of(sinCodigo, equivocado, caducado))
                .allSatisfy(e -> assertThat(e).isInstanceOf(InvalidDataException.class)
                        .hasMessage(sinCodigo.getMessage()));
    }

    @Test
    @DisplayName("verificar una cuenta ya verificada devuelve su estado sin gastar código")
    void verificar_ya_verificada_es_idempotente() {
        usuario.setEmailVerificado(true);
        usuario.setEmailVerificadoEn(LocalDateTime.now().minusDays(1));

        EstadoCorreoDTO estado = service.verificar("123456");

        assertThat(estado.isVerificado()).isTrue();
        verify(codigoRepository, never())
                .findFirstByUsuarioIdAndUsadoFalseAndFechaExpiracionAfterOrderByIdDesc(anyInt(), any());
    }

    // --- Reenviar -----------------------------------------------------------

    @Test
    @DisplayName("reenviar a una cuenta sin verificar manda otro código a su correo")
    void reenviar_emite_otro_codigo() {
        service.reenviar();

        assertThat(almacen).hasSize(1);
        verify(emailService).enviarCodigoVerificacion(eq(usuario), any(), eq(MINUTOS_VALIDEZ));
    }

    @Test
    @DisplayName("reenviar a una cuenta ya verificada es un 409 y no manda nada")
    void reenviar_ya_verificada_es_conflicto() {
        usuario.setEmailVerificado(true);

        assertThatThrownBy(() -> service.reenviar()).isInstanceOf(ConflictEntityException.class);
        assertThat(almacen).isEmpty();
        verify(emailService, never()).enviarCodigoVerificacion(any(), any(), anyInt());
    }

    // --- El código no sale del servidor salvo por el correo -----------------

    @Test
    @DisplayName("el código no se escribe en el log en ningún nivel")
    void el_codigo_no_sale_en_el_log() {
        String codigo = emitir();
        String malo = "000000".equals(codigo) ? "111111" : "000000";
        capturar(() -> service.verificar(malo));
        service.verificar(codigo);

        assertThat(registro.list).isNotEmpty();
        assertThat(registro.list).noneMatch(e -> e.getFormattedMessage().contains(codigo));
    }

    // --- Apoyo --------------------------------------------------------------

    private String emitir() {
        service.emitirCodigo(usuario);
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoVerificacion(eq(usuario), codigo.capture(), anyInt());
        return codigo.getValue();
    }

    private VerificacionCorreoCodigo ultimo() {
        return almacen.get(almacen.size() - 1);
    }

    // Retrasa las dos fechas del código: es como si hubieran pasado esos minutos.
    private static void adelantarElReloj(VerificacionCorreoCodigo c, int minutos, int segundos) {
        c.setFechaCreacion(c.getFechaCreacion().minusMinutes(minutos).minusSeconds(segundos));
        c.setFechaExpiracion(c.getFechaExpiracion().minusMinutes(minutos).minusSeconds(segundos));
    }

    private static Throwable capturar(Runnable r) {
        try {
            r.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private void cablearAlmacen() {
        when(codigoRepository.save(any(VerificacionCorreoCodigo.class))).thenAnswer(inv -> {
            VerificacionCorreoCodigo c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(siguienteId++);
                almacen.add(c);
            }
            return c;
        });

        when(codigoRepository.findFirstByUsuarioIdAndUsadoFalseAndFechaExpiracionAfterOrderByIdDesc(
                anyInt(), any(LocalDateTime.class))).thenAnswer(inv -> {
            Integer usuarioId = inv.getArgument(0);
            LocalDateTime ahora = inv.getArgument(1);
            return almacen.stream()
                    .filter(c -> c.getUsuario().getId().equals(usuarioId))
                    .filter(c -> !c.isUsado())
                    .filter(c -> c.getFechaExpiracion().isAfter(ahora))
                    .max(Comparator.comparing(VerificacionCorreoCodigo::getId));
        });

        doAnswer(inv -> {
            Integer usuarioId = inv.getArgument(0);
            almacen.stream()
                    .filter(c -> c.getUsuario().getId().equals(usuarioId))
                    .forEach(c -> c.setUsado(true));
            return null;
        }).when(codigoRepository).invalidarTodosDeUsuario(anyInt());
    }

    // Nivel ALL: el código no puede salir en NINGÚN perfil.
    @SuppressWarnings("deprecation")
    private void engancharElLog() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(VerificacionCorreoService.class);
        nivelPrevio = logger.getLevel();
        logger.setLevel(Level.ALL);
        registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
    }
}
