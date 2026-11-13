package es.pmdm.gymprofit.ui.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.ui.viewmodels.VerificarCorreoViewModel.Estado;
import es.pmdm.gymprofit.utils.VerificacionCorreo;

// ============================================================
// VerificarCorreoViewModelTest — entrar desde Perfil pide un código nuevo, una vez (GP-267).
//
// El puerto es un falso que apunta cada petición y no responde hasta que el test lo pide,
// como en RegistrarSesionViewModelTest: así se ve qué pasa al girar con la petición en
// vuelo. «Girar» es volver a llamar a entrar() sobre el mismo ViewModel, que es lo que hace
// la pantalla nueva; «matar el proceso», serializar el SavedStateHandle y crear otro.
// ============================================================
public class VerificarCorreoViewModelTest {

    @Rule
    public InstantTaskExecutorRule sincrono = new InstantTaskExecutorRule();

    private static final long AHORA = 1_000_000_000L;

    /** Puerto falso: guarda las respuestas pendientes y lo que se apunta. */
    private static final class PuertoFalso implements VerificarCorreoViewModel.Puerto {
        final List<VerificarCorreoViewModel.Puerto.Respuesta> pedidas = new ArrayList<>();
        final List<Long> apuntados = new ArrayList<>();

        @Override
        public void pedirCodigo(VerificarCorreoViewModel.Puerto.Respuesta respuesta) {
            pedidas.add(respuesta);
        }

        @Override
        public void apuntarCodigo(long ahora) {
            apuntados.add(ahora);
        }
    }

    private final PuertoFalso puerto = new PuertoFalso();

    private VerificarCorreoViewModel nuevo(SavedStateHandle handle) {
        return new VerificarCorreoViewModel(handle, puerto, () -> AHORA);
    }

    // ── Una sola petición por entrada ────────────────────────────────────────

    @Test
    public void desdePerfilPideUnCodigoNuevoAlEntrar() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        assertEquals(1, puerto.pedidas.size());
        assertEquals(Estado.ENVIANDO, vm.estado().getValue());
    }

    @Test
    public void desdeElAltaNoPideNada_laApiYaLoMando() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(false);
        assertEquals(0, puerto.pedidas.size());
        assertNull(vm.estado().getValue());
    }

    @Test
    public void girarConLaPeticionEnVueloNoPideOtra() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        vm.entrar(true);
        assertEquals(1, puerto.pedidas.size());
        // Y la respuesta que llega después de girar la ve la pantalla nueva.
        puerto.pedidas.get(0).ok();
        assertEquals(Estado.ENVIADO, vm.estado().getValue());
    }

    @Test
    public void girarYaRespondidoNoPideOtra() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).ok();
        vm.entrar(true);
        assertEquals(1, puerto.pedidas.size());
        assertEquals(Estado.ENVIADO, vm.estado().getValue());
    }

    @Test
    public void volverTrasMatarElProcesoNoPideOtra() throws Exception {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).ok();

        VerificarCorreoViewModel otro = nuevo(resucitar(vm.handle()));
        otro.entrar(true);
        assertEquals(1, puerto.pedidas.size());
        assertEquals(Estado.ENVIADO, otro.estado().getValue());
    }

    @Test
    public void siElProcesoMurioConLaPeticionEnVuelo_noSeRepiteYSeAbreNormal() throws Exception {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);

        VerificarCorreoViewModel otro = nuevo(resucitar(vm.handle()));
        otro.entrar(true);
        assertEquals(1, puerto.pedidas.size());
        assertEquals(Estado.INTERRUMPIDO, otro.estado().getValue());
        assertFalse(otro.abrirCaducado(0L, 0, AHORA));
    }

    // ── Lo que responde la API ───────────────────────────────────────────────

    @Test
    public void conCodigoEmitidoSeApuntaCuandoSeMando() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).ok();
        assertEquals(List.of(AHORA), puerto.apuntados);
        assertEquals(Estado.ENVIADO, vm.estado().getValue());
    }

    @Test
    public void con429AbreConLaCuentaAtrasDeLaApi() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).fallo(429, ApiCallback.PREFIJO_RETRY_AFTER + "42;Demasiados intentos");
        assertEquals(Estado.ESPERA, vm.estado().getValue());
        assertEquals(42, vm.segundosDeEspera(AHORA));
        // La cuenta sigue corriendo aunque se gire: se guarda cuándo acaba, no cuánto queda.
        assertEquals(30, vm.segundosDeEspera(AHORA + 12_000L));
        assertEquals(0, vm.segundosDeEspera(AHORA + 60_000L));
        assertTrue(puerto.apuntados.isEmpty());
    }

    @Test
    public void con409ElCorreoYaEstabaVerificado() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).fallo(409, "ya verificado");
        assertEquals(Estado.YA_VERIFICADO, vm.estado().getValue());
    }

    @Test
    public void sinRedSeQuedaElFalloParaEnsenarlo() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).fallo(-1, "sin conexión");
        assertEquals(Estado.FALLO, vm.estado().getValue());
        assertEquals(-1, vm.codigoFallo());
        assertEquals("sin conexión", vm.mensajeFallo());
    }

    @Test
    public void elAvisoDeCadaRespuestaSeDaUnaSolaVez() throws Exception {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).fallo(-1, "sin conexión");
        assertTrue(vm.tocaAvisar());
        vm.avisado();
        assertFalse(vm.tocaAvisar());
        assertFalse(nuevo(resucitar(vm.handle())).tocaAvisar());
    }

    // ── Nunca abre en «Ese código ya no vale» ────────────────────────────────

    @Test
    public void desdePerfilNoAbreCaducadoAunqueNoSeSepaCuandoSeMando() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        // Antes de que conteste, con un código de hace días apuntado, o sin ninguno.
        assertFalse(vm.abrirCaducado(0L, 0, AHORA));
        assertFalse(vm.abrirCaducado(AHORA - 3 * 86_400_000L, 0, AHORA));
        // Con el 429 y con un fallo de red, tampoco.
        puerto.pedidas.get(0).fallo(429, ApiCallback.PREFIJO_RETRY_AFTER + "42;x");
        assertFalse(vm.abrirCaducado(0L, 0, AHORA));
    }

    @Test
    public void desdePerfilSinRedTampocoAbreCaducado() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).fallo(-1, "sin conexión");
        assertFalse(vm.abrirCaducado(0L, 0, AHORA));
    }

    @Test
    public void conElCodigoNuevoMandanLasReglasDeSiempre() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(true);
        puerto.pedidas.get(0).ok();
        // Girar con el código nuevo recién mandado: se abre normal.
        assertFalse(vm.abrirCaducado(AHORA, 0, AHORA));
        // Girar tras gastar los cinco intentos, o pasados los 15 minutos: ya no vale.
        assertTrue(vm.abrirCaducado(AHORA, VerificacionCorreo.MAX_INTENTOS, AHORA));
        assertTrue(vm.abrirCaducado(AHORA, 0, AHORA + VerificacionCorreo.MINUTOS * 60_000L));
    }

    @Test
    public void desdeElAltaSiguenLasReglasDeSiempre() {
        VerificarCorreoViewModel vm = nuevo(new SavedStateHandle());
        vm.entrar(false);
        assertFalse(vm.abrirCaducado(AHORA, 0, AHORA));
        assertTrue(vm.abrirCaducado(0L, 0, AHORA));
    }

    // Lo que hace Android al matar el proceso: el handle, serializado y vuelto a leer.
    @SuppressWarnings("unchecked")
    private static SavedStateHandle resucitar(SavedStateHandle handle) throws Exception {
        Map<String, Object> guardado = new HashMap<>();
        for (String clave : handle.keys()) guardado.put(clave, handle.get(clave));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(guardado);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return new SavedStateHandle((Map<String, Object>) in.readObject());
        }
    }
}
