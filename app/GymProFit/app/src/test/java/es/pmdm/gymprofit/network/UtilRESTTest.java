package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// ============================================================
// UtilRESTTest — GP-091: la sesión sobrevive a que Android mate la app.
//
// Al matar el proceso en segundo plano, Android recrea la pantalla donde estaba el
// usuario sin pasar por la splash, que era la única que cargaba el token en memoria:
// la primera petición salía sin él, recibía 401 y la app cerraba la sesión. Ahora el
// token lo carga UtilREST del almacén cuando le falta. Aquí el almacén es uno en
// memoria y «matar el proceso» es olvidar la memoria de UtilREST sin tocarlo.
// ============================================================
public class UtilRESTTest {

    /** Almacén de mentira: lo que guardaría el cifrado en disco. */
    private static final class AlmacenFalso implements UtilREST.AlmacenTokens {
        String token, refresh;
        @Override public String leerToken() { return token; }
        @Override public String leerRefresh() { return refresh; }
        @Override public void guardar(String t, String r) { token = t; refresh = r; }
        @Override public void borrar() { token = null; refresh = null; }
    }

    private AlmacenFalso almacen;

    @Before
    public void preparar() {
        almacen = new AlmacenFalso();
        UtilREST.setAlmacen(almacen);
        UtilREST.olvidarMemoria();
    }

    @After
    public void limpiar() {
        UtilREST.setOnUnauthorizedListener(null);
        UtilREST.setAlmacen(null);
        UtilREST.olvidarMemoria();
    }

    @Test
    public void tras_matar_el_proceso_el_token_sale_del_almacen() {
        almacen.guardar("acceso-1", "refresco-1");

        assertEquals("acceso-1", UtilREST.getToken());
        assertEquals("refresco-1", UtilREST.getRefreshToken());
    }

    @Test
    public void el_token_renovado_queda_guardado_y_sobrevive_a_otra_muerte() {
        almacen.guardar("acceso-1", "refresco-1");
        UtilREST.onTokensRefreshed("acceso-2", "refresco-2");
        UtilREST.olvidarMemoria();

        assertEquals("acceso-2", UtilREST.getToken());
        assertEquals("refresco-2", UtilREST.getRefreshToken());
    }

    @Test
    public void cerrar_sesion_lo_borra_todo_y_no_vuelve_a_cargarse() {
        almacen.guardar("acceso-1", "refresco-1");
        UtilREST.getToken();

        UtilREST.clearToken();

        assertNull(almacen.leerToken());
        assertNull(almacen.leerRefresh());
        assertNull(UtilREST.getToken());
        assertNull(UtilREST.getRefreshToken());
    }

    // --- GP-107: un 401 en cadena abre Login una sola vez ------------------

    @Test
    public void seis_401_seguidos_avisan_una_sola_vez() {
        almacen.guardar("acceso-1", "refresco-1");
        int[] avisos = {0};
        UtilREST.setOnUnauthorizedListener(desactivada -> avisos[0]++);

        // La reproducción de GP-091: seis peticiones de la misma pantalla caen a la vez.
        for (int i = 0; i < 6; i++) UtilREST.notifyUnauthorized(false);

        assertEquals(1, avisos[0]);
        assertNull(UtilREST.getToken());
    }

    @Test
    public void tras_volver_a_entrar_un_401_nuevo_vuelve_a_avisar() {
        almacen.guardar("acceso-1", "refresco-1");
        int[] avisos = {0};
        UtilREST.setOnUnauthorizedListener(desactivada -> avisos[0]++);

        UtilREST.notifyUnauthorized(false);
        UtilREST.setToken("acceso-2");
        UtilREST.notifyUnauthorized(false);

        assertEquals(2, avisos[0]);
    }
}
