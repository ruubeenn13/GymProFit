package com.gymprofit.api.integration;

import com.gymprofit.api.config.security.JwtTokenProvider;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// FotoPerfilServidorTest — el tope de la foto contra el servidor arrancado (GP-188)
// MockMvc no pasa por el multipart de Tomcat ni por su límite de lo que traga tras un
// error: una subida de 8 MB tiene que recibir el 413 con su mensaje, no una conexión
// cortada. Con Tomcat de verdad en un puerto libre y un JWT de verdad.
// ============================================================
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FotoPerfilServidorTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario usuario;

    @BeforeEach
    void crear() {
        jdbc.update("DELETE ur FROM usuario_roles ur JOIN usuarios u ON u.id = ur.usuario_id WHERE u.username = '__foto_servidor__'");
        jdbc.update("DELETE FROM usuarios WHERE username = '__foto_servidor__'");
        Usuario u = new Usuario();
        u.setUsername("__foto_servidor__");
        u.setPassword(passwordEncoder.encode("Test1234"));
        u.setEmail("foto-servidor@test.local");
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        u.setRoles(roleRepository.findByNombreIn(List.of(RoleType.USER.getValue())));
        usuario = usuarioRepository.save(u);
    }

    @AfterEach
    void borrar() {
        jdbc.update("DELETE FROM fotos_perfil WHERE usuario_id = ?", usuario.getId());
        jdbc.update("DELETE FROM usuario_roles WHERE usuario_id = ?", usuario.getId());
        jdbc.update("DELETE FROM usuarios WHERE id = ?", usuario.getId());
    }

    @Test
    @DisplayName("una subida de 8 MB recibe el 413 con su mensaje, no una conexión cortada")
    void ocho_megas_413() throws Exception {
        HttpResponse<String> r = subir(8 * 1024 * 1024, "es");
        assertThat(r.statusCode()).isEqualTo(413);
        assertThat(r.body()).contains("foto").contains("1 MB");
        HttpResponse<String> en = subir(8 * 1024 * 1024, "en");
        assertThat(en.statusCode()).isEqualTo(413);
        assertThat(en.body()).contains("photo").contains("1 MB");
    }

    @Test
    @DisplayName("una foto de 1 MB justo pasa el multipart de Tomcat y se guarda")
    void un_mega_200() throws Exception {
        assertThat(subir(1024 * 1024, "es").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT LENGTH(datos) FROM fotos_perfil WHERE usuario_id = ?", Integer.class,
                usuario.getId())).isEqualTo(1024 * 1024);
    }

    private HttpResponse<String> subir(int tamano, String idioma) throws Exception {
        String limite = "----limitefoto";
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        cuerpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"f.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.write(FotoPerfilTopeTest.jpeg(tamano));
        cuerpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest peticion = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/usuarios/" + usuario.getId() + "/foto"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + jwtTokenProvider.generateToken(usuario))
                .header("Accept-Language", idioma)
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
                .build();
        return HttpClient.newHttpClient().send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
