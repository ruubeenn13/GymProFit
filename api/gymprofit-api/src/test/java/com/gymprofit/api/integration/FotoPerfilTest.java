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

    // Bytes de imagen de mentira suficientes para el test (no se valida el formato).
    private static final byte[] BYTES_FOTO = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

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
}
