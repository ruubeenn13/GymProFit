package com.gymprofit.api.integration;

import com.gymprofit.api.entity.FotoPerfil;
import com.gymprofit.api.repository.jpa.IFotoPerfilRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// FotoPerfilTest — e2e de la foto de perfil persistida en BD (BLOB).
// Verifica el ciclo subir→descargar (los bytes devueltos son los subidos), el 404 sin
// foto y el 403 IDOR en las DOS direcciones: subir foto a otro y —lo que faltaba—
// descargar la de otro, que respondía 200 y además estaba abierto a GUEST.
// ============================================================
@DisplayName("Foto de perfil en BD — subir/descargar/ownership")
class FotoPerfilTest extends AbstractOwnershipTest {

    // Para sembrar la foto del owner directamente en BD. No se usa runAs + el servicio:
    // runAs toca el SecurityContext que comparte el request del test, y el propio test
    // tiene que salir autenticado como el atacante. Aquí la siembra no necesita permisos.
    @Autowired
    private IFotoPerfilRepository fotoPerfilRepository;

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

    /**
     * El agujero que se cierra: la <b>descarga</b> no comprobaba nada. Solo estaba cubierta
     * la subida, así que subir la foto de otro daba 403 pero bajarla daba 200, y con la
     * ruta llevando el id en el camino se descargaban todas iterando ids.
     * <p>
     * La foto del owner se siembra de verdad, y no se da por hecho que no exista: si el
     * 403 saltara solo porque no hay foto, el test pasaría igual con el fallo puesto.
     */
    @Test
    @DisplayName("Descargar la foto de un usuario ajeno → 403 (IDOR)")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void descargarFotoAjena_devuelve403() throws Exception {
        sembrarFotoDe(owner.getId());

        mockMvc.perform(get("/usuarios/" + owner.getId() + "/foto"))
                .andExpect(status().isForbidden());
    }

    /**
     * El caso que convertía el fallo en un álbum público: un token GUEST se obtiene sin
     * credenciales, así que cualquiera con la URL de la API podía recorrer las caras de
     * todos los usuarios. Ahora la ruta ni siquiera admite ese rol.
     * <p>
     * Pide <b>su propia</b> foto, no la del owner: con el id ajeno el 403 lo daría ya
     * {@code checkOwnership} y el test pasaría aunque SecurityConfig siguiera dejando
     * entrar a GUEST. Pidiendo la suya, lo único que puede rechazarlo es la regla de rol.
     */
    @Test
    @DisplayName("Descargar la foto propia con token GUEST → 403 (la ruta no admite ese rol)")
    @WithUserDetails(value = GUEST, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void descargarFotoConTokenGuest_devuelve403() throws Exception {
        sembrarFotoDe(guest.getId());

        mockMvc.perform(get("/usuarios/" + guest.getId() + "/foto"))
                .andExpect(status().isForbidden());
    }

    // Persiste una foto en BD para ese usuario, sin pasar por el servicio ni por HTTP.
    private void sembrarFotoDe(Integer usuarioId) {
        FotoPerfil foto = new FotoPerfil();
        foto.setUsuarioId(usuarioId);
        foto.setDatos(BYTES_FOTO);
        foto.setContentType("image/jpeg");
        foto.setFechaActualizacion(LocalDateTime.now());
        fotoPerfilRepository.save(foto);
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
