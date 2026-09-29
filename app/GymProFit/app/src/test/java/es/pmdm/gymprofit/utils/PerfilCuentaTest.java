package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.utils.PerfilCuenta.Campo;

// ============================================================
// PerfilCuentaTest — el perfil entre el móvil y la API: el sexo y la actividad
// (GP-111) y el peso, la altura, la edad, el objetivo y el nivel (GP-129).
//
// En el móvil son claves globales y cerrarSesion() las conserva, así que en un móvil
// con dos cuentas pueden ser de la otra. PerfilCuenta decide de quién son, qué se trae
// de la API y qué se sube una vez. El almacén es un doble en memoria con las mismas
// reglas que PreferencesManager: una clave que no existe es null, y el peso y la
// altura se guardan como float.
// ============================================================
public class PerfilCuentaTest {

    /** Almacén en memoria: lo mismo que guardan las preferencias. */
    private static final class Almacen implements PerfilCuenta.Almacen {
        final Map<Campo, String> valores = new EnumMap<>(Campo.class);
        String dueno;
        Set<String> onboarding = new HashSet<>();

        @Override public String leer(Campo c) { return valores.get(c); }
        @Override public void guardar(Campo c, String v) {
            // Como PreferencesManager: el peso y la altura pasan por un float.
            boolean decimal = c == Campo.PESO || c == Campo.ALTURA;
            valores.put(c, decimal ? String.valueOf(Float.parseFloat(v)) : v);
        }
        @Override public void borrar(Campo c) { valores.remove(c); }
        @Override public String getDuenoPerfil() { return dueno; }
        @Override public Set<String> getCuentasConOnboarding() { return onboarding; }
        @Override public void apuntarDuenoPerfil(String u) { dueno = u; }
    }

    private static Almacen almacen(String sexo, String actividad, String dueno, String... onboarding) {
        Almacen a = new Almacen();
        if (sexo != null) a.valores.put(Campo.SEXO, sexo);
        if (actividad != null) a.valores.put(Campo.ACTIVIDAD, actividad);
        a.dueno = dueno;
        a.onboarding.addAll(Arrays.asList(onboarding));
        return a;
    }

    /** Perfil de la API con solo el sexo y la actividad (lo de GP-111). */
    private static Map<Campo, String> api(String sexo, String actividad) {
        Map<Campo, String> m = new EnumMap<>(Campo.class);
        if (sexo != null) m.put(Campo.SEXO, sexo);
        if (actividad != null) m.put(Campo.ACTIVIDAD, actividad);
        return m;
    }

    /** Perfil completo de la API, tal como llega en UsuarioDTO. */
    private static Map<Campo, String> apiCompleta() {
        Map<Campo, String> m = api("MUJER", "ACTIVO");
        m.put(Campo.PESO, "62.50");
        m.put(Campo.ALTURA, "168.0");
        m.put(Campo.EDAD, "28");
        m.put(Campo.OBJETIVO, "PERDER_PESO");
        m.put(Campo.NIVEL, "PRINCIPIANTE");
        return m;
    }

    private static Map<Campo, String> vacia() {
        return new EnumMap<>(Campo.class);
    }

    private static Map<String, Object> subir(Almacen a, String usuario, Map<Campo, String> api) {
        return PerfilCuenta.alEntrar(a, usuario, api).subir;
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
        assertFalse(PerfilCuenta.usable(PerfilCuenta.Propiedad.AJENA));
    }

    // ── GP-111: cuenta que ya existía (instalación sin dueño apuntado) ─────

    @Test
    public void cuenta_antigua_unica_sube_lo_elegido_una_vez_y_se_apunta() {
        Almacen a = almacen("MUJER", "ACTIVO", null, "ana");

        Map<String, Object> subir = subir(a, "ana", vacia());

        assertEquals("MUJER", subir.get("sexo"));
        assertEquals("ACTIVO", subir.get("nivelActividad"));
        assertEquals("ana", a.dueno);
        // La segunda vez la API ya los tiene: se traen, no se vuelven a subir.
        assertTrue(subir(a, "ana", api("MUJER", "ACTIVO")).isEmpty());
    }

    @Test
    public void cuenta_antigua_que_nunca_eligio_no_sube_nada() {
        // La clave no existe: el HOMBRE / MODERADO de hoy es el valor por defecto, no una elección.
        Almacen a = almacen(null, null, null, "ana");

        assertTrue(subir(a, "ana", vacia()).isEmpty());
    }

    @Test
    public void cuenta_antigua_con_otra_cuenta_en_el_movil_no_se_inventa() {
        Almacen a = almacen("MUJER", "ACTIVO", null, "ana", "luis");

        assertTrue(subir(a, "ana", vacia()).isEmpty());
        assertNull("sin saber de quién es no se apunta dueño", a.dueno);
        assertEquals("y el móvil queda como estaba", "MUJER", a.leer(Campo.SEXO));
    }

    // ── GP-111: dos cuentas en el mismo móvil ──────────────────────────────

    @Test
    public void el_perfil_de_otra_cuenta_no_se_sube() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");

        assertTrue(subir(a, "ana", vacia()).isEmpty());
        assertEquals("luis", a.dueno);
        assertEquals("el de la otra cuenta sigue intacto", "MUJER", a.leer(Campo.SEXO));
    }

    @Test
    public void lo_de_la_api_se_trae_y_el_perfil_pasa_a_ser_de_quien_entra() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");

        assertTrue(subir(a, "ana", api("HOMBRE", "SEDENTARIO")).isEmpty());

        assertEquals("HOMBRE", a.leer(Campo.SEXO));
        assertEquals("SEDENTARIO", a.leer(Campo.ACTIVIDAD));
        assertEquals("ana", a.dueno);
    }

    @Test
    public void si_la_api_trae_solo_uno_el_otro_de_la_otra_cuenta_se_tira() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");

        PerfilCuenta.alEntrar(a, "ana", api("HOMBRE", null));

        assertEquals("HOMBRE", a.leer(Campo.SEXO));
        assertNull("la actividad era de luis: ana no la ha elegido", a.leer(Campo.ACTIVIDAD));
        assertEquals("ana", a.dueno);
    }

    @Test
    public void si_la_api_trae_solo_uno_el_otro_propio_se_sube() {
        Almacen a = almacen("MUJER", "ACTIVO", "ana", "ana");

        Map<String, Object> subir = subir(a, "ana", api("MUJER", null));

        assertEquals(Collections.singletonMap("nivelActividad", "ACTIVO"), subir);
    }

    @Test
    public void con_dueno_propio_y_api_vacia_se_sube() {
        // P. ej. el onboarding terminó sin red: el PATCH falló y quedó solo en el móvil.
        Almacen a = almacen("MUJER", "LIGERO", "ana", "ana", "luis");

        Map<String, Object> subir = subir(a, "ana", vacia());

        assertEquals("MUJER", subir.get("sexo"));
        assertEquals("LIGERO", subir.get("nivelActividad"));
    }

    @Test
    public void un_valor_raro_de_la_api_no_se_guarda() {
        // Una API más nueva podría traer un valor que esta build no conoce.
        Almacen a = almacen("MUJER", "ACTIVO", "ana", "ana");

        assertTrue("ni se sube lo propio encima", subir(a, "ana", api("OTRO", "MUCHISIMO")).isEmpty());
        assertEquals("MUJER", a.leer(Campo.SEXO));
        assertEquals("ACTIVO", a.leer(Campo.ACTIVIDAD));
    }

    // ── GP-129: peso, altura, edad, objetivo y nivel ───────────────────────

    @Test
    public void la_cuenta_que_reinstala_recupera_el_perfil_entero() {
        // Móvil recién instalado (o con los datos borrados): nada guardado, ni dueño.
        Almacen a = almacen(null, null, null);

        PerfilCuenta.Resultado r = PerfilCuenta.alEntrar(a, "ana", apiCompleta());

        assertTrue(r.subir.isEmpty());
        assertTrue("hay que recalcular el objetivo nutricional", r.cambiado);
        assertEquals("62.5", a.leer(Campo.PESO));
        assertEquals("168.0", a.leer(Campo.ALTURA));
        assertEquals("28", a.leer(Campo.EDAD));
        assertEquals("PERDER_PESO", a.leer(Campo.OBJETIVO));
        assertEquals("PRINCIPIANTE", a.leer(Campo.NIVEL));
        assertEquals("ana", a.dueno);
    }

    @Test
    public void lo_de_la_api_pisa_lo_del_movil() {
        Almacen a = almacen("MUJER", "ACTIVO", "ana", "ana");
        a.guardar(Campo.PESO, "70");
        a.guardar(Campo.OBJETIVO, "MANTENER_PESO");

        PerfilCuenta.Resultado r = PerfilCuenta.alEntrar(a, "ana", apiCompleta());

        assertTrue(r.cambiado);
        assertEquals("62.5", a.leer(Campo.PESO));
        assertEquals("PERDER_PESO", a.leer(Campo.OBJETIVO));
    }

    @Test
    public void si_ya_estaba_igual_no_hay_nada_que_recalcular() {
        Almacen a = almacen(null, null, null);
        PerfilCuenta.alEntrar(a, "ana", apiCompleta());

        assertFalse(PerfilCuenta.alEntrar(a, "ana", apiCompleta()).cambiado);
    }

    @Test
    public void lo_que_el_movil_tiene_y_la_api_no_se_sube_una_vez() {
        // El PATCH del onboarding falló: la app lo dio por guardado en local, sin reintento.
        Almacen a = almacen("MUJER", "ACTIVO", "ana", "ana");
        a.guardar(Campo.PESO, "62.5");
        a.guardar(Campo.ALTURA, "168");
        a.guardar(Campo.EDAD, "28");
        a.guardar(Campo.OBJETIVO, "PERDER_PESO");
        a.guardar(Campo.NIVEL, "PRINCIPIANTE");

        PerfilCuenta.Resultado r = PerfilCuenta.alEntrar(a, "ana", vacia());

        assertEquals(new BigDecimal("62.5"), r.subir.get("peso"));
        assertEquals(new BigDecimal("168.0"), r.subir.get("altura"));
        assertEquals(28, r.subir.get("edad"));
        assertEquals("PERDER_PESO", r.subir.get("objetivo"));
        assertEquals("PRINCIPIANTE", r.subir.get("nivelExperiencia"));
        assertEquals(7, r.subir.size());
        assertFalse("no ha cambiado nada en el móvil", r.cambiado);

        // Subido, la API ya los tiene: la vez siguiente no se sube nada.
        assertTrue(PerfilCuenta.alEntrar(a, "ana", apiCompleta()).subir.isEmpty());
    }

    @Test
    public void un_valor_por_defecto_nunca_se_sube() {
        // Sin la clave, el 70 kg que da getPeso() es el valor por defecto: no se sube.
        Almacen a = almacen("MUJER", null, "ana", "ana");

        assertEquals(Collections.singletonMap("sexo", (Object) "MUJER"), subir(a, "ana", vacia()));
    }

    @Test
    public void con_dos_cuentas_el_peso_de_la_otra_ni_se_sube_ni_se_queda() {
        Almacen a = almacen("MUJER", "ACTIVO", "luis", "ana", "luis");
        a.guardar(Campo.PESO, "90");
        a.guardar(Campo.EDAD, "51");

        // ana, sin nada en la API: no se sube nada de luis.
        assertTrue(subir(a, "ana", vacia()).isEmpty());

        // ana con solo el objetivo en la API: se trae, y lo demás de luis se tira.
        Map<Campo, String> soloObjetivo = vacia();
        soloObjetivo.put(Campo.OBJETIVO, "MEJORAR_FUERZA");
        PerfilCuenta.alEntrar(a, "ana", soloObjetivo);
        assertEquals("MEJORAR_FUERZA", a.leer(Campo.OBJETIVO));
        assertNull(a.leer(Campo.PESO));
        assertNull(a.leer(Campo.EDAD));
        assertEquals("ana", a.dueno);
    }

    @Test
    public void un_objetivo_o_un_nivel_que_esta_build_no_conoce_se_deja_estar() {
        Almacen a = almacen(null, null, "ana", "ana");
        a.guardar(Campo.OBJETIVO, "PERDER_PESO");
        a.guardar(Campo.NIVEL, "INTERMEDIO");
        Map<Campo, String> nueva = vacia();
        nueva.put(Campo.OBJETIVO, "RECOMPOSICION");
        nueva.put(Campo.NIVEL, "LEYENDA");

        PerfilCuenta.Resultado r = PerfilCuenta.alEntrar(a, "ana", nueva);

        assertEquals("PERDER_PESO", a.leer(Campo.OBJETIVO));
        assertEquals("INTERMEDIO", a.leer(Campo.NIVEL));
        assertTrue("ni se pisa lo de la API con lo del móvil", r.subir.isEmpty());
        assertFalse(r.cambiado);
    }

    @Test
    public void un_numero_fuera_de_rango_de_la_api_se_deja_estar() {
        Almacen a = almacen(null, null, "ana", "ana");
        a.guardar(Campo.ALTURA, "170");
        Map<Campo, String> rara = vacia();
        rara.put(Campo.ALTURA, "1.75");   // en metros: esta build la guarda en centímetros
        rara.put(Campo.PESO, "no-es-un-numero");

        PerfilCuenta.Resultado r = PerfilCuenta.alEntrar(a, "ana", rara);

        assertEquals("170.0", a.leer(Campo.ALTURA));
        assertNull(a.leer(Campo.PESO));
        assertTrue(r.subir.isEmpty());
    }

    @Test
    public void del_usuario_de_la_api_sale_cada_campo_y_un_cero_es_que_no_lo_tiene() {
        Usuario u = new Usuario();
        u.setPeso("62.50");
        u.setObjetivo("PERDER_PESO");
        u.setNivelExperiencia("");
        // altura y edad a 0: así llega un null de la API al modelo, que usa primitivos.

        Map<Campo, String> m = PerfilCuenta.deUsuario(u);

        assertEquals("62.50", m.get(Campo.PESO));
        assertEquals("PERDER_PESO", m.get(Campo.OBJETIVO));
        assertEquals(2, m.size());

        u.setAltura(168);
        u.setEdad(28);
        m = PerfilCuenta.deUsuario(u);
        assertEquals("168.0", m.get(Campo.ALTURA));
        assertEquals("28", m.get(Campo.EDAD));
    }

    @Test
    public void el_onboarding_no_guarda_como_elegido_lo_que_se_dejo_en_blanco() {
        // La edad es opcional: en blanco llega 0, y el 25 del cálculo es un valor por
        // defecto que no puede quedar guardado como si el usuario lo hubiera dicho.
        Almacen a = almacen(null, null, "ana", "ana");
        a.guardar(Campo.EDAD, "40");

        PerfilCuenta.guardarOpcional(a, Campo.EDAD, "0");
        PerfilCuenta.guardarOpcional(a, Campo.PESO, "62.5");
        PerfilCuenta.guardarOpcional(a, Campo.ALTURA, null);

        assertNull("la edad de antes tampoco vale: el onboarding define el perfil", a.leer(Campo.EDAD));
        assertEquals("62.5", a.leer(Campo.PESO));
        assertNull(a.leer(Campo.ALTURA));
        assertTrue(subir(a, "ana", vacia()).containsKey("peso"));
        assertFalse(subir(a, "ana", vacia()).containsKey("edad"));
    }
}
