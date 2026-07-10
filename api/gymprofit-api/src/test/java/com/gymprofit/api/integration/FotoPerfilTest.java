package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// FotoPerfilTest — e2e de la foto de perfil persistida en BD (BLOB).
// Verifica el ciclo subir→descargar (los bytes devueltos son los subidos),
// el 404 sin foto y el 403 IDOR (un usuario no sube foto a otro).
// ============================================================
@DisplayName("Foto de perfil en BD — subir/descargar/ownership")
class FotoPerfilTest extends AbstractOwnershipTest {

    // Bytes con cabecera JPEG real (FF D8 FF) → pasan la validación de magic bytes.
    private static final byte[] BYTES_FOTO = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    // Binario que NO es imagen (sin magic bytes válidos) → debe rechazarse.
    private static final byte[] BYTES_NO_IMAGEN = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    @Test
    @DisplayName("Subir y descargar la foto propia → 200 y mismos bytes")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void subirYDescargarFotoPropia_devuelveMismosBytes() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", BYTES_FOTO);

        mockMvc.perform(multipart("/usuarios/" + owner.getId() + "/foto").file(foto))
                .andExpect(status().isOk());

        mockMvc.perform(get("/usuarios/" + owner.getId() + "/foto"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(BYTES_FOTO));
    }

    @Test
    @DisplayName("Descargar sin foto subida → 404")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void descargarSinFoto_devuelve404() throws Exception {
        mockMvc.perform(get("/usuarios/" + owner.getId() + "/foto"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Subir foto a un usuario ajeno → 403 (IDOR)")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void subirFotoAjena_devuelve403() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", BYTES_FOTO);

        mockMvc.perform(multipart("/usuarios/" + owner.getId() + "/foto").file(foto))
                .andExpect(status().isForbidden());
    }

    // El content-type dice "image/jpeg" pero los bytes NO son imagen: se valida por
    // magic bytes, no por lo que declara el cliente → 400 (no se persiste).
    @Test
    @DisplayName("Subir un binario que no es imagen (aunque mienta el content-type) → 400")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void subirNoImagen_devuelve400() throws Exception {
        MockMultipartFile falsa = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", BYTES_NO_IMAGEN);

        mockMvc.perform(multipart("/usuarios/" + owner.getId() + "/foto").file(falsa))
                .andExpect(status().isBadRequest());
    }
}
