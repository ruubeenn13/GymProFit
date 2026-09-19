package com.gymprofit.api.service.auth;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gymprofit.api.dto.auth.ResetPasswordDTO;
import com.gymprofit.api.entity.PasswordResetCodigo;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IPasswordResetCodigoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ============================================================
// PasswordResetServiceTest — la cadena de recuperación de contraseña
//
// Cubre, una por una, las reglas que el servicio declara en su javadoc: plazo real
// de 15 minutos, límite real de 5 intentos, un solo uso, invalidación de los códigos
// anteriores, revocación de TODAS las sesiones al canjear, y el mismo error para
// cualquier motivo de fallo.
//
// Dos decisiones que hacen que estos tests prueben algo de verdad:
//
//  · El PasswordEncoder es real (BCrypt de coste 4, barato). Si fuera un mock, que un
//    código case con su hash sería una decisión del test, no del servicio: podría
//    borrarse la comprobación entera y el test seguiría verde.
//  · El repositorio de códigos es un mock con memoria: guarda de verdad, y la búsqueda
//    del código vivo aplica los mismos filtros que la consulta de Spring Data (ni usado,
//    ni caducado, el de id más alto). Sin eso, la caducidad y el un-solo-uso los
//    decidiría el stub y no el código bajo prueba.
//
// El paso del tiempo se simula retrasando las fechas del código guardado, porque el
// servicio llama a LocalDateTime.now() directamente y no hay un Clock que inyectar.
// ============================================================
@DisplayName("PasswordResetService — recuperación de contraseña por código")
class PasswordResetServiceTest {

    private static final String USERNAME = "ruben";
    private static final String EMAIL = "ruben@example.com";
    private static final String PASSWORD_VIEJA = "ViejaPass1!";
    private static final String PASSWORD_NUEVA = "NuevaPass1!";

    // Los mismos valores que declara el servicio. Si allí cambian, estos tests caen.
    private static final int MINUTOS_VALIDEZ = 15;
    private static final int MAX_INTENTOS = 5;

    private static final Pattern SEIS_DIGITOS = Pattern.compile("\\d{6}");

    private IUsuarioRepository usuarioRepository;
    private IPasswordResetCodigoRepository codigoRepository;
    private RefreshTokenService refreshTokenService;
    private IEmailService emailService;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;

    private Usuario usuario;

    // Memoria del repositorio simulado: los códigos emitidos, en orden de emisión.
    private final List<PasswordResetCodigo> almacen = new ArrayList<>();
    private int siguienteId = 1;

    private ListAppender<ILoggingEvent> registro;
    private ch.qos.logback.classic.Logger logger;
    private Level nivelPrevio;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(IUsuarioRepository.class);
        codigoRepository = mock(IPasswordResetCodigoRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailService = mock(IEmailService.class);
        passwordEncoder = new BCryptPasswordEncoder(4);

        service = new PasswordResetService(usuarioRepository, codigoRepository,
                refreshTokenService, emailService, passwordEncoder);

        usuario = new Usuario();
        usuario.setId(7);
        usuario.setUsername(USERNAME);
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);
        usuario.setPassword(passwordEncoder.encode(PASSWORD_VIEJA));

        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        cablearAlmacenDeCodigos();
        engancharElLog();
    }

    @AfterEach
    void soltarElLog() {
        logger.detachAppender(registro);
        registro.stop();
        logger.setLevel(nivelPrevio);
    }

    // --- Emisión del código -------------------------------------------------

    @Test
    @DisplayName("el código se emite hasheado, de un solo uso y con 15 minutos de vida")
    void el_codigo_se_emite_hasheado_y_caduca_a_los_quince_minutos() {
        String codigo = pedirCodigo();
        PasswordResetCodigo guardado = ultimoCodigoGuardado();

        assertThat(codigo).matches("\\d{6}");

        // En la base de datos vive el hash, nunca los seis dígitos.
        assertThat(guardado.getCodigoHash()).isNotEqualTo(codigo);
        assertThat(passwordEncoder.matches(codigo, guardado.getCodigoHash())).isTrue();

        assertThat(guardado.isUsado()).isFalse();
        assertThat(guardado.getIntentos()).isZero();

        // El plazo sale de la constante del servicio: creación + 15 minutos, no un número
        // inventado aquí. El margen de 5 s absorbe la distancia entre los dos now().
        assertThat(guardado.getFechaExpiracion())
                .isAfterOrEqualTo(guardado.getFechaCreacion().plusMinutes(MINUTOS_VALIDEZ))
                .isBefore(guardado.getFechaCreacion().plusMinutes(MINUTOS_VALIDEZ).plusSeconds(5));

        // Y el correo anuncia ese mismo plazo, no otro.
        verify(emailService).enviarCodigoRecuperacion(usuario, codigo, MINUTOS_VALIDEZ);
    }

    @Test
    @DisplayName("pedir un código nuevo invalida los anteriores")
    void pedir_codigo_nuevo_invalida_los_anteriores() {
        pedirCodigo();
        String segundo = pedirCodigo();

        assertThat(almacen).hasSize(2);
        // El primero queda muerto en el mismo momento en que se emite el segundo.
        assertThat(almacen.get(0).isUsado()).isTrue();
        assertThat(almacen.get(1).isUsado()).isFalse();
        verify(codigoRepository, times(2)).invalidarTodosDeUsuario(usuario.getId());

        // Y el que sigue vivo es el último emitido.
        assertThatCode(() -> service.restablecer(canje(segundo))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pedir código para una cuenta inexistente no lanza, no emite y no envía")
    void cuenta_inexistente_no_lanza_ni_envia() {
        assertThatCode(() -> service.solicitarCodigo("no-existe-nadie")).doesNotThrowAnyException();

        verifyNoInteractions(emailService);
        verify(codigoRepository, never()).save(any(PasswordResetCodigo.class));
        assertThat(almacen).isEmpty();
    }

    @Test
    @DisplayName("una cuenta desactivada no recibe código")
    void cuenta_desactivada_no_recibe_codigo() {
        usuario.setActivo(false);

        assertThatCode(() -> service.solicitarCodigo(USERNAME)).doesNotThrowAnyException();

        verifyNoInteractions(emailService);
        verify(codigoRepository, never()).save(any(PasswordResetCodigo.class));
    }

    @Test
    @DisplayName("una cuenta sin correo no genera un código que nadie podría recibir")
    void cuenta_sin_correo_no_emite_codigo() {
        usuario.setEmail(null);

        assertThatCode(() -> service.solicitarCodigo(USERNAME)).doesNotThrowAnyException();

        verifyNoInteractions(emailService);
        verify(codigoRepository, never()).save(any(PasswordResetCodigo.class));
    }

    // --- Canje --------------------------------------------------------------

    @Test
    @DisplayName("el código correcto dentro de plazo canjea y cambia la contraseña")
    void codigo_correcto_dentro_de_plazo_cambia_la_contrasena() {
        String codigo = pedirCodigo();

        service.restablecer(canje(codigo));

        assertThat(passwordEncoder.matches(PASSWORD_NUEVA, usuario.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD_VIEJA, usuario.getPassword())).isFalse();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("a los 14 minutos el código sirve; pasados los 15 ya no")
    void codigo_caducado_se_rechaza_en_el_limite_real() {
        // Primer código: 14 minutos después sigue valiendo. Si el plazo fuera más corto
        // que 15 minutos, este canje ya fallaría.
        String primero = pedirCodigo();
        adelantarElReloj(ultimoCodigoGuardado(), 14, 0);
        assertThatCode(() -> service.restablecer(canje(primero))).doesNotThrowAnyException();

        // Segundo código: un segundo por encima del plazo. Si el plazo fuera más largo
        // que 15 minutos, este canje aún funcionaría.
        String segundo = pedirCodigo();
        adelantarElReloj(ultimoCodigoGuardado(), MINUTOS_VALIDEZ, 1);
        assertThatThrownBy(() -> service.restablecer(canje(segundo)))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("un código equivocado se rechaza y consume un intento")
    void codigo_equivocado_consume_intento() {
        String codigo = pedirCodigo();
        PasswordResetCodigo guardado = ultimoCodigoGuardado();
        String erroneo = otroCodigoDistintoDe(codigo);

        assertThatThrownBy(() -> service.restablecer(canje(erroneo)))
                .isInstanceOf(InvalidDataException.class);

        assertThat(guardado.getIntentos()).isEqualTo(1);
        assertThat(guardado.isUsado()).isFalse();
        assertThat(passwordEncoder.matches(PASSWORD_VIEJA, usuario.getPassword())).isTrue();
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(refreshTokenService, never()).revocarTodosDeUsuario(any(Usuario.class));
    }

    @Test
    @DisplayName("agotados los 5 intentos el código queda quemado aunque después llegue el correcto")
    void intentos_agotados_queman_el_codigo() {
        String codigo = pedirCodigo();
        PasswordResetCodigo guardado = ultimoCodigoGuardado();
        String erroneo = otroCodigoDistintoDe(codigo);

        for (int i = 1; i <= MAX_INTENTOS; i++) {
            final int intento = i;
            assertThatThrownBy(() -> service.restablecer(canje(erroneo)))
                    .as("el intento fallido nº %d debe rechazarse", intento)
                    .isInstanceOf(InvalidDataException.class);
        }
        assertThat(guardado.getIntentos()).isEqualTo(MAX_INTENTOS);

        // Ahora sí, el código bueno. Ya no vale: quemarlo es justo la protección.
        assertThatThrownBy(() -> service.restablecer(canje(codigo)))
                .isInstanceOf(InvalidDataException.class);

        assertThat(guardado.isUsado()).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD_VIEJA, usuario.getPassword())).isTrue();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("un código ya canjeado no vale una segunda vez")
    void codigo_canjeado_no_se_reutiliza() {
        String codigo = pedirCodigo();

        service.restablecer(canje(codigo));
        assertThat(ultimoCodigoGuardado().isUsado()).isTrue();

        assertThatThrownBy(() -> service.restablecer(canje(codigo)))
                .isInstanceOf(InvalidDataException.class);

        // La contraseña se escribió una sola vez: el segundo canje no llegó a tocarla.
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("al canjear se revocan TODAS las sesiones abiertas, no solo la actual")
    void canjear_revoca_todas_las_sesiones() {
        String codigo = pedirCodigo();

        service.restablecer(canje(codigo));

        verify(refreshTokenService).revocarTodosDeUsuario(usuario);
        // Revocar solo el refresh token en curso dejaría vivas las demás sesiones de la
        // cuenta, que es exactamente lo que obliga a cerrar un cambio de contraseña.
        verify(refreshTokenService, never()).revocarPorToken(anyString());
    }

    @Test
    @DisplayName("todos los fallos de canje devuelven el mismo mensaje")
    void todos_los_fallos_de_canje_dicen_lo_mismo() {
        String codigo = pedirCodigo();

        String porCuentaInexistente =
                mensajeDeFallo(new ResetPasswordDTO("no-existe-nadie", codigo, PASSWORD_NUEVA));
        String porCodigoEquivocado = mensajeDeFallo(canje(otroCodigoDistintoDe(codigo)));

        adelantarElReloj(ultimoCodigoGuardado(), MINUTOS_VALIDEZ, 1);
        String porCaducado = mensajeDeFallo(canje(codigo));

        assertThat(porCodigoEquivocado)
                .as("distinguir el código equivocado de la cuenta inexistente diría por dónde seguir probando")
                .isEqualTo(porCuentaInexistente);
        assertThat(porCaducado).isEqualTo(porCuentaInexistente);
    }

    // --- El código no se escribe en el log ----------------------------------

    @Test
    @DisplayName("el código de seis dígitos no se escribe en el log en ningún nivel")
    void el_codigo_nunca_aparece_en_el_log() {
        String codigo = pedirCodigo();
        String erroneo = otroCodigoDistintoDe(codigo);

        // Recorre las tres ramas que registran algo: emisión, fallo y canje correcto.
        assertThatThrownBy(() -> service.restablecer(canje(erroneo)))
                .isInstanceOf(InvalidDataException.class);
        service.restablecer(canje(codigo));

        assertThat(registro.list).as("el servicio debería haber registrado algo").isNotEmpty();

        for (ILoggingEvent evento : registro.list) {
            String linea = evento.getFormattedMessage();
            assertThat(linea).doesNotContain(codigo);
            assertThat(linea).doesNotContain(erroneo);
            // Ni siquiera una tira de seis dígitos: lo único numérico que sale por aquí
            // es el id del usuario y el contador de intentos.
            assertThat(SEIS_DIGITOS.matcher(linea).find())
                    .as("la línea '%s' contiene seis dígitos seguidos", linea)
                    .isFalse();
        }
    }

    // --- Andamiaje ----------------------------------------------------------

    // Repositorio simulado con memoria: guardar, buscar el código vivo con los mismos
    // filtros que la consulta real, e invalidar en bloque los de un usuario.
    private void cablearAlmacenDeCodigos() {
        when(codigoRepository.save(any(PasswordResetCodigo.class))).thenAnswer(inv -> {
            PasswordResetCodigo c = inv.getArgument(0);
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
                    .max(Comparator.comparing(PasswordResetCodigo::getId));
        });

        doAnswer(inv -> {
            Integer usuarioId = inv.getArgument(0);
            almacen.stream()
                    .filter(c -> c.getUsuario().getId().equals(usuarioId))
                    .forEach(c -> c.setUsado(true));
            return null;
        }).when(codigoRepository).invalidarTodosDeUsuario(anyInt());
    }

    // Nivel ALL a propósito: la regla es que el código no se escriba en NINGÚN perfil,
    // así que hay que capturar también lo que en producción no llegaría a salir.
    private void engancharElLog() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PasswordResetService.class);
        nivelPrevio = logger.getLevel();
        logger.setLevel(Level.ALL);
        registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
    }

    /** Pide un código y devuelve los seis dígitos que salieron hacia el correo. */
    private String pedirCodigo() {
        service.solicitarCodigo(USERNAME);
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce())
                .enviarCodigoRecuperacion(eq(usuario), codigo.capture(), anyInt());
        return codigo.getValue();
    }

    private PasswordResetCodigo ultimoCodigoGuardado() {
        return almacen.get(almacen.size() - 1);
    }

    private ResetPasswordDTO canje(String codigo) {
        return new ResetPasswordDTO(USERNAME, codigo, PASSWORD_NUEVA);
    }

    // Otro código de seis dígitos, seguro distinto del bueno.
    private String otroCodigoDistintoDe(String codigo) {
        return "000000".equals(codigo) ? "111111" : "000000";
    }

    // Retrasa las fechas del código para simular que ha pasado ese tiempo.
    private void adelantarElReloj(PasswordResetCodigo codigo, long minutos, long segundos) {
        codigo.setFechaCreacion(codigo.getFechaCreacion().minusMinutes(minutos).minusSeconds(segundos));
        codigo.setFechaExpiracion(codigo.getFechaExpiracion().minusMinutes(minutos).minusSeconds(segundos));
    }

    // Ejecuta un canje que debe fallar y devuelve el mensaje del error.
    private String mensajeDeFallo(ResetPasswordDTO dto) {
        try {
            service.restablecer(dto);
            return fail("se esperaba que el canje fallara");
        } catch (InvalidDataException e) {
            return e.getMessage();
        }
    }
}
