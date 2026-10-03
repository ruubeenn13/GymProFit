package com.gymprofit.api.integration;

import com.gymprofit.api.pruebas.ContadorSentencias;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// FotoPerfilTopeTest — el tope de la foto de perfil y lo que cuesta guardarla (GP-188)
// Como mucho 1 MB; por encima, 413 que dice que es la foto y cuánto admite, en ES y EN.
// Sustituir una foto no lee la anterior, y servirla no carga la entidad entera.
// (El 413 contra el servidor arrancado, con una subida de 8 MB, en FotoPerfilServidorTest.)
// ============================================================
@Import(ContadorSentencias.class)
class FotoPerfilTopeTest extends AbstractOwnershipTest {

    private static final int MB = 1024 * 1024;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("por encima de 1 MB, 413 con el tope en el mensaje, en español y en inglés")
    void encima_413() throws Exception {
        byte[] grande = jpeg(MB + 1);
        String es = subir(grande, "es").andExpect(status().isPayloadTooLarge())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(es).contains("foto").contains("1 MB");
        String en = subir(grande, "en").andExpect(status().isPayloadTooLarge())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(en).contains("photo").contains("1 MB");
        assertThat(subirDescargar()).isNull();
    }

    @Test
    @DisplayName("justo 1 MB se guarda, y se lee igual")
    void tope_justo() throws Exception {
        byte[] foto = jpeg(MB);
        subir(foto, "es").andExpect(status().isOk());
        assertThat(subirDescargar()).isEqualTo(foto);
    }

    @Test
    @DisplayName("sustituir funciona y no lee la foto anterior de la base")
    void sustituir() throws Exception {
        subir(jpeg(300_000), "es").andExpect(status().isOk());
        em.flush();
        em.clear();
        byte[] nueva = jpeg(200_000);
        ContadorSentencias.empezar();
        subir(nueva, "es").andExpect(status().isOk());
        em.flush();
        List<String> sentencias = ContadorSentencias.sentencias();
        assertThat(sentencias).noneMatch(s -> s.toLowerCase(Locale.ROOT).startsWith("select")
                && s.toLowerCase(Locale.ROOT).contains("datos"));
        em.clear();
        assertThat(subirDescargar()).isEqualTo(nueva);
    }

    @Test
    @DisplayName("servirla lee solo los bytes, no la entidad")
    void servir_solo_bytes() throws Exception {
        subir(jpeg(100_000), "es").andExpect(status().isOk());
        em.flush();
        em.clear();
        ContadorSentencias.empezar();
        subirDescargar();
        assertThat(ContadorSentencias.sentencias())
                .filteredOn(s -> s.toLowerCase(Locale.ROOT).contains("fotos_perfil"))
                .allMatch(s -> !s.toLowerCase(Locale.ROOT).contains("content_type"));
    }

    private ResultActions subir(byte[] datos, String idioma) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.multipart("/usuarios/" + owner.getId() + "/foto")
                .file(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", datos))
                .header("Authorization", "Bearer " + token(owner))
                .header("Accept-Language", idioma));
    }

    // La foto guardada, o null si no hay.
    private byte[] subirDescargar() throws Exception {
        var r = pedir(owner, "GET /usuarios/" + owner.getId() + "/foto").andReturn().getResponse();
        return r.getStatus() == 200 ? r.getContentAsByteArray() : null;
    }

    static byte[] jpeg(int tamano) {
        byte[] d = new byte[tamano];
        Arrays.fill(d, (byte) 7);
        d[0] = (byte) 0xFF;
        d[1] = (byte) 0xD8;
        d[2] = (byte) 0xFF;
        return d;
    }
}
