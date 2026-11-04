package es.pmdm.gymprofit.arranque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import es.pmdm.gymprofit.arranque.DecisionArranque.Destino;
import es.pmdm.gymprofit.arranque.DecisionArranque.Estado;
import es.pmdm.gymprofit.arranque.DecisionArranque.Renovacion;

// ============================================================
// DecisionArranqueTest — GP-223, punto 5: adónde lleva el arranque en cada caso.
//
// Cada test es un caso de la ficha: sin sesión, alta a medias, sesión vigente,
// caducada, sin red, sesión en vivo sin red, servidor que no responde, 401 y
// cuenta desactivada. La caducidad sale del campo exp del token, sin verificar la
// firma: solo decide si hay que renovar.
// ============================================================
public class DecisionArranqueTest {

    private static final long AHORA = 1_800_000_000_000L;
    private static final long MINUTO = 60_000L;

    /** Una sesión con el alta hecha, red y el token vigente 20 minutos más. */
    private static Estado conSesion() {
        return new Estado().sesion(true).onboarding(true).refresh(true).red(true)
                .caducaMs(AHORA + 20 * MINUTO).ahoraMs(AHORA);
    }

    @Test
    public void sinSesionVaALaBienvenida() {
        assertEquals(Destino.BIENVENIDA, DecisionArranque.decidir(new Estado().red(true).ahoraMs(AHORA)));
    }

    @Test
    public void sinSesionYSinRedTambienVaALaBienvenida() {
        // La bienvenida y el cuestionario no necesitan la API: «Sin conexión» es de quien ya tiene cuenta.
        assertEquals(Destino.BIENVENIDA, DecisionArranque.decidir(new Estado().red(false).ahoraMs(AHORA)));
    }

    @Test
    public void sinSesionConElAltaAMediasVuelveAlAlta() {
        assertEquals(Destino.ALTA,
                DecisionArranque.decidir(new Estado().altaEmpezada(true).red(true).ahoraMs(AHORA)));
    }

    @Test
    public void conSesionSinOnboardingVaAlAltaDeLaCuenta() {
        assertEquals(Destino.ALTA_CUENTA, DecisionArranque.decidir(conSesion().onboarding(false)));
    }

    @Test
    public void conElTokenVigenteVaAInicioSinLlamarALaApi() {
        assertEquals(Destino.INICIO, DecisionArranque.decidir(conSesion()));
    }

    @Test
    public void conElTokenCaducadoRenueva() {
        assertEquals(Destino.RENOVAR, DecisionArranque.decidir(conSesion().caducaMs(AHORA - MINUTO)));
    }

    @Test
    public void unTokenQueCaducaEnSegundosTambienSeRenueva() {
        // Si caduca mientras Inicio pide sus datos, lo renovaría el autenticador a la vez:
        // con un margen, se renueva una vez y aquí.
        assertEquals(Destino.RENOVAR, DecisionArranque.decidir(conSesion().caducaMs(AHORA + 10_000)));
        assertEquals(Destino.INICIO, DecisionArranque.decidir(
                conSesion().caducaMs(AHORA + DecisionArranque.MARGEN_CADUCIDAD_MS + 1)));
    }

    @Test
    public void unTokenSinCaducidadLegibleSeRenueva() {
        assertEquals(Destino.RENOVAR, DecisionArranque.decidir(conSesion().caducaMs(null)));
    }

    @Test
    public void caducadoYSinRefreshVaAEntrar() {
        assertEquals(Destino.ENTRAR, DecisionArranque.decidir(conSesion().caducaMs(AHORA - MINUTO).refresh(false)));
    }

    @Test
    public void sinRedEsSinConexionAunqueElTokenSigaVigente() {
        assertEquals(Destino.SIN_CONEXION, DecisionArranque.decidir(conSesion().red(false)));
        assertEquals(Destino.SIN_CONEXION, DecisionArranque.decidir(conSesion().red(false).caducaMs(AHORA - MINUTO)));
    }

    @Test
    public void sinRedConUnaSesionEnVivoAbreLaSesion() {
        assertEquals(Destino.SESION_EN_VIVO, DecisionArranque.decidir(conSesion().red(false).sesionEnVivo(true)));
    }

    @Test
    public void conRedYUnaSesionEnVivoVaAInicio() {
        // Inicio ya enseña la sesión en su barra, como hasta ahora.
        assertEquals(Destino.INICIO, DecisionArranque.decidir(conSesion().sesionEnVivo(true)));
    }

    @Test
    public void laRenovacionBuenaLlevaAInicio() {
        assertEquals(Destino.INICIO, DecisionArranque.trasRenovar(Renovacion.OK, false));
    }

    @Test
    public void un401AlRenovarLlevaAEntrar() {
        assertEquals(Destino.ENTRAR, DecisionArranque.trasRenovar(Renovacion.NO_AUTORIZADO, false));
        assertEquals(Destino.ENTRAR, DecisionArranque.trasRenovar(Renovacion.NO_AUTORIZADO, true));
    }

    @Test
    public void laCuentaDesactivadaLlevaAEntrarConSuAviso() {
        assertEquals(Destino.ENTRAR_DESACTIVADA, DecisionArranque.trasRenovar(Renovacion.CUENTA_DESACTIVADA, false));
    }

    @Test
    public void unServidorQueNoRespondeEsNoConseguimosConectar() {
        assertEquals(Destino.NO_CONECTA, DecisionArranque.trasRenovar(Renovacion.SIN_RESPUESTA, false));
    }

    @Test
    public void unServidorQueNoRespondeConUnaSesionEnVivoAbreLaSesion() {
        // El entreno en curso no depende de la API: se guarda en el móvil.
        assertEquals(Destino.SESION_EN_VIVO, DecisionArranque.trasRenovar(Renovacion.SIN_RESPUESTA, true));
    }

    @Test
    public void laRespuestaDeRefreshSeClasifica() {
        assertEquals(Renovacion.OK, DecisionArranque.clasificar(200, "{}"));
        assertEquals(Renovacion.NO_AUTORIZADO, DecisionArranque.clasificar(401, "{\"message\":\"Refresh token inválido\"}"));
        assertEquals(Renovacion.CUENTA_DESACTIVADA,
                DecisionArranque.clasificar(401, "{\"cause\":\"CUENTA_DESACTIVADA\"}"));
        // Sin Accept, la API puede responder el error en XML: el código está igual.
        assertEquals(Renovacion.CUENTA_DESACTIVADA,
                DecisionArranque.clasificar(401, "<Response><cause>CUENTA_DESACTIVADA</cause></Response>"));
        assertEquals(Renovacion.SIN_RESPUESTA, DecisionArranque.clasificar(-1, null));
        assertEquals(Renovacion.SIN_RESPUESTA, DecisionArranque.clasificar(500, ""));
        assertEquals(Renovacion.SIN_RESPUESTA, DecisionArranque.clasificar(503, ""));
        assertEquals(Renovacion.SIN_RESPUESTA, DecisionArranque.clasificar(429, ""));
    }

    @Test
    public void laCaducidadSaleDelCampoExpDelToken() {
        // Cabecera y cuerpo en base64url sin relleno; la firma no se mira.
        String token = jwt("{\"sub\":\"ana\",\"exp\":1800001234,\"iat\":1799999434}");
        assertEquals(Long.valueOf(1_800_001_234_000L), DecisionArranque.caducidadMs(token));
    }

    @Test
    public void laCaducidadLeeElAlfabetoUrlYLaFaltaDeRelleno() {
        // «ñ» y «?» dan bytes que en base64url salen con «-» o «_»; con 1 y 2 de relleno.
        for (String sub : new String[]{"ñ?>", "ab", "abc", "abcd"}) {
            String token = jwt("{\"sub\":\"" + sub + "\",\"exp\":42}");
            assertEquals(Long.valueOf(42_000L), DecisionArranque.caducidadMs(token));
        }
    }

    @Test
    public void unTokenMalFormadoNoTieneCaducidad() {
        assertNull(DecisionArranque.caducidadMs(null));
        assertNull(DecisionArranque.caducidadMs(""));
        assertNull(DecisionArranque.caducidadMs("no-es-un-jwt"));
        assertNull(DecisionArranque.caducidadMs("a.%%%.c"));
        assertNull(DecisionArranque.caducidadMs(jwt("{\"sub\":\"ana\"}")));
    }

    private static String jwt(String cuerpo) {
        return url("{\"alg\":\"HS512\"}") + "." + url(cuerpo) + ".firma";
    }

    private static String url(String json) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
