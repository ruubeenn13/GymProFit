package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// ============================================================
// PerfilCuentaTest — GP-111: el sexo y la actividad, entre el móvil y la API.
//
// En el móvil son claves globales y cerrarSesion() las conserva, así que en un móvil
// con dos cuentas pueden ser de la otra. PerfilCuenta decide de quién son, qué se trae
// de la API y qué se sube una vez. El almacén es un doble en memoria con las mismas
// reglas que PreferencesManager: una clave que no existe es null.
// ============================================================
public class PerfilCuentaTest {

    /** Almacén en memoria: lo mismo que guardan las preferencias. */
    private static final class Almacen implements PerfilCuenta.Almacen {
        String sexo, actividad, dueno;
        Set<String> onboarding = new HashSet<>();

        @Override public String getSexoGuardado() { return sexo; }
        @Override public String getActividadGuardada() { return actividad; }
        @Override public String getDuenoPerfil() { return dueno; }
        @Override public Set<String> getCuentasConOnboarding() { return onboarding; }
        @Override public void saveSexo(String s) { sexo = s; }
        @Override public void saveActividad(String a) { actividad = a; }
        @Override public void borrarSexo() { sexo = null; }
        @Override public void borrarActividad() { actividad = null; }
        @Override public void apuntarDuenoPerfil(String u) { dueno = u; }
    }

    private static Almacen almacen(String sexo, String actividad, String dueno, String... onboarding) {
        Almacen a = new Almacen();
        a.sexo = sexo;
        a.actividad = actividad;
        a.dueno = dueno;
        a.onboarding.addAll(Arrays.asList(onboarding));
        return a;
    }

    // ── De quién es el perfil local ────────────────────────────────────────

    @Test
    public void con_dueno_apuntado_manda_el_dueno() {
        assertEquals(PerfilCuenta.Propiedad.MIA,
                PerfilCuenta.propiedad("ana", Collections.singleton("luis"), "ana"));
        assertEquals(PerfilCuenta.Propiedad.AJENA,
                PerfilCuenta.propiedad("luis", Collections.singleton("ana"), "ana"));
    }

    @Test
    public void sin_dueno_es_suyo_solo_si_es_la_unica_cuenta_con_onboarding() {
        assertEquals(PerfilCuenta.Propiedad.MIA,
                PerfilCuenta.propiedad(null, Collections.singleton("ana"), "ana"));
        assertEquals(PerfilCuenta.Propiedad.DESCONOCIDA,
                PerfilCuenta.propiedad(null, new HashSet<>(Arrays.asList("ana", "luis")), "ana"));
        assertEquals(PerfilCuenta.Propiedad.DESCONOCIDA,
                PerfilCuenta.propiedad(null, Collections.singleton("luis"), "ana"));
        assertEquals(PerfilCuenta.Propiedad.DESCONOCIDA,
                PerfilCuenta.propiedad(null, Collections.emptySet(), "ana"));
    }

    @Test
    public void solo_se_usa_lo_que_no_es_de_otra_cuenta() {
        assertTrue(PerfilCuenta.usable(PerfilCuenta.Propiedad.MIA));
        // Sin saber de quién es, queda como hasta ahora: se usa lo que hay.
        assertTrue(PerfilCuenta.usable(PerfilCuenta.Propiedad.DESCONOCIDA));
        assertEquals(false, PerfilCuenta.usable(PerfilCuenta.Propiedad.AJENA));
    }

    // ── Cuenta que ya existía (instalación sin dueño apuntado) ─────────────

    @Test
    public void cuenta_antigua_unica_sube_lo_elegido_una_vez_y_se_apunta() {
        Almacen a = almacen("MUJER", "ACTIVO", null, "ana");

        Map<String, Object> subir = PerfilCuenta.alEntrar(a, "ana", null, null);

        assertEquals("MUJER", subir.get("sexo"));
        assertEquals("ACTIVO", subir.get("nivelActividad"));
        assertEquals("ana", a.dueno);
        // La segunda vez la API ya los tiene: se traen, no se vuelven a subir.
        assertTrue(PerfilCuenta.alEntrar(a, "ana", "MUJER", "ACTIVO").isEmpty());
    }

    @Test
    public void cuenta_antigua_que_nunca_eligio_no_sube_nada() {
        // La clave no existe: el HOMBRE / MODERADO de hoy es el valor por defecto, no una elección.
        Almacen a = almacen(null, null, null, "ana");

        assertTrue(PerfilCuenta.alEntrar(a, "ana", null, null).isEmpty());
    }

    @Test
    public void cuenta_antigua_con_otra_cuenta_en_el_movil_no_se_inventa() {
        Almacen a = almacen("MUJER", "ACTIVO", null, "ana", "luis");

        assertTrue(PerfilCuenta.alEntrar(a, "ana", null, null).isEmpty());
        assertNull("sin saber de quién es no se apunta dueño", a.dueno);
        assertEquals("y el móvil queda como estaba", "MUJER", a.sexo);
    }

    // ── Dos cuentas en el mismo móvil ──────────────────────────────────────

    @Test
    public void el_perfil_de_otra_cuenta_no_se_sube() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");

        assertTrue(PerfilCuenta.alEntrar(a, "ana", null, null).isEmpty());
        assertEquals("luis", a.dueno);
        assertEquals("el de la otra cuenta sigue intacto", "MUJER", a.sexo);
    }

    @Test
    public void lo_de_la_api_se_trae_y_el_perfil_pasa_a_ser_de_quien_entra() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");

        assertTrue(PerfilCuenta.alEntrar(a, "ana", "HOMBRE", "SEDENTARIO").isEmpty());

        assertEquals("HOMBRE", a.sexo);
        assertEquals("SEDENTARIO", a.actividad);
        assertEquals("ana", a.dueno);
    }

    @Test
    public void si_la_api_trae_solo_uno_el_otro_de_la_otra_cuenta_se_tira() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");

        PerfilCuenta.alEntrar(a, "ana", "HOMBRE", null);

        assertEquals("HOMBRE", a.sexo);
        assertNull("la actividad era de luis: ana no la ha elegido", a.actividad);
        assertEquals("ana", a.dueno);
    }

    @Test
    public void si_la_api_trae_solo_uno_el_otro_propio_se_sube() {
        Almacen a = almacen("MUJER", "ACTIVO", "ana", "ana");

        Map<String, Object> subir = PerfilCuenta.alEntrar(a, "ana", "MUJER", null);

        assertEquals(Collections.singletonMap("nivelActividad", "ACTIVO"), subir);
    }

    @Test
    public void con_dueno_propio_y_api_vacia_se_sube() {
        // P. ej. el onboarding terminó sin red: el PATCH falló y quedó solo en el móvil.
        Almacen a = almacen("MUJER", "LIGERO", "ana", "ana", "luis");

        Map<String, Object> subir = PerfilCuenta.alEntrar(a, "ana", null, null);

        assertEquals("MUJER", subir.get("sexo"));
        assertEquals("LIGERO", subir.get("nivelActividad"));
    }

    @Test
    public void un_valor_raro_de_la_api_no_se_guarda() {
        // Una API más nueva podría traer un valor que esta build no conoce.
        Almacen a = almacen("MUJER", "ACTIVO", "ana", "ana");

        assertTrue("ni se sube lo propio encima", PerfilCuenta.alEntrar(a, "ana", "OTRO", "MUCHISIMO").isEmpty());

        assertEquals("MUJER", a.sexo);
        assertEquals("ACTIVO", a.actividad);
    }
}
