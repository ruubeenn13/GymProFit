package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// ContrasteCristalTest — GP-285, DEC-070: las reglas de legibilidad de Liquid Glass, medidas.
//
// Regla 2 del diseño: cada par texto/superficie pasa 4,5:1 (3:1 en texto de 24 sp o más y en
// gráficos) sobre el punto MÁS DESFAVORABLE de lo que puede haber detrás: fondo + brillo +
// cristal compuestos. En oscuro ese punto es el más claro; en claro, el más oscuro. Aquí no se
// supone cuál es: se recorre el fondo entero (Ambiente.colorEn, la misma cuenta que pinta
// FondoAmbiental) en varios tamaños de pantalla y se compone cada capa de cristal encima
// con sus tres tonos (alto, medio y bajo).
//
// Regla 3: el cristal vivo pasa en los tres niveles. Detrás de la barra puede haber cualquier
// punto del fondo, con o sin una tarjeta de cristal, desenfocado (que promedia: nunca supera
// al punto extremo) y con la saturación subida, y además el velo del «+» cuando la hoja está
// abierta; encima va su tinte (niveles 1 y 2) o su superficie casi opaca (nivel 3).
//
// Lee values/colors.xml y values-night/colors.xml tal cual. Deja las cifras de cada par en
// build/reports/contraste-cristal.txt, de donde salen las de DESIGN.md.
// ============================================================
public class ContrasteCristalTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final String[] TEMAS = { "values-night", "values" };
    // Ancho × alto en dp: móvil pequeño, la maqueta, el emulador, uno estrecho y alto, una
    // tableta y un móvil tumbado.
    private static final float[][] PANTALLAS = { {360, 640}, {390, 844}, {411, 914}, {360, 800}, {600, 960}, {844, 390} };

    // Los tres tonos del relleno del cristal estático y los dos de cada cristal vivo.
    private static final String[] CRISTAL = { "gp_cristal_alto", "gp_cristal_medio", "gp_cristal_bajo" };
    // El tono medio del vivo solo existe en claro; en oscuro es transparente y no cuenta.
    private static final String[] VIVO = { "gp_vivo_alto", "gp_vivo_medio", "gp_vivo_bajo" };
    private static final String[] VIVO_OPACO = { "gp_vivo_opaco_alto", "gp_vivo_opaco_bajo" };
    // La hoja del «+», con más tinte que la barra (en oscuro, el mismo).
    private static final String[] HOJA = { "gp_vivo_hoja_alto", "gp_vivo_hoja_bajo" };
    private static final String[] ORO = { "gp_cristal_oro_alto", "gp_cristal_oro_bajo" };
    // El campo de una serie (kg, reps, tiempo): en claro, el cristal de las tarjetas; en oscuro,
    // el surco de siempre.
    private static final String[] CAMPO = { "gp_cristal_campo_alto", "gp_cristal_campo_medio", "gp_cristal_campo_bajo" };
    // El cristal vivo de la Sesión en vivo (el resumen y el descanso): el de la barra.
    private static final String[] VIVO_SESION = VIVO;
    // El botón de marca (el principal y el «+»): los dos extremos de su relleno macizo.
    private static final String[] MARCA_RELLENO = { "gp_marca_alto", "gp_marca_bajo" };

    private static final double TEXTO = 4.5, GRANDE = 3.0;

    // ── Regla 2: cristal estático ────────────────────────────────────────────

    @Test
    public void el_texto_sobre_el_fondo_ambiental_pasa_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> fondo = t.fondo();
            t.medir("sobre el fondo", "gp_cristal_texto", fondo, TEXTO, fallos);
            t.medir("sobre el fondo", "gp_cristal_texto_2", fondo, TEXTO, fallos);
            t.medir("sobre el fondo", "gp_cristal_enlace", fondo, TEXTO, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    @Test
    public void el_texto_sobre_el_cristal_estatico_pasa_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> cristal = t.capas(t.fondo(), CRISTAL);
            for (String texto : new String[]{ "gp_cristal_texto", "gp_cristal_texto_2", "gp_cristal_acento", "gp_cristal_exito",
                    "gp_cristal_oro" }) {
                t.medir("sobre el cristal", texto, cristal, TEXTO, fallos);
            }
            // Gráficos: iconos con color, barras de progreso y la última barra de la gráfica.
            for (String grafico : new String[]{ "gp_cristal_grafico", "gp_cristal_proteinas", "gp_cristal_agua", "gp_cristal_silueta" }) {
                t.medir("gráfico sobre el cristal", grafico, cristal, GRANDE, fallos);
            }
            // El chip y el botón secundario: cristal plano encima del de la tarjeta.
            Set<Integer> chip = t.capas(cristal, new String[]{ "gp_cristal_boton" });
            t.medir("sobre el chip", "gp_cristal_texto", chip, TEXTO, fallos);
            // La fila del récord: degradado dorado encima del cristal. Sus dos líneas van en el
            // texto principal (el secundario no llega) y la marca, a 26 sp, en su dorado.
            Set<Integer> oro = t.capas(cristal, ORO);
            t.medir("sobre el récord", "gp_cristal_texto", oro, TEXTO, fallos);
            t.medir("cifra sobre el récord", "gp_cristal_oro_cifra", oro, GRANDE, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // Lo que es translúcido y va sobre el cristal se mide ya compuesto: una barra apagada de una
    // gráfica contra el cristal que la rodea, y el relleno de una barra de progreso contra su pista.
    @Test
    public void las_graficas_sobre_el_cristal_pasan_3_a_1() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> cristal = t.capas(t.fondo(), CRISTAL);
            // Las barras apagadas de «esta vez frente a las anteriores» (BarrasVecesView).
            t.medirCompuesto("barra apagada", "gp_cristal_barra_tenue", cristal, GRANDE, fallos);
            // La gráfica de kilos (BarrasKilosView). Donde el tema le pone filo a las barras
            // (claro), la forma de cada barra se lee por él y es el filo el que tiene que pasar;
            // donde no (oscuro), pasan la semana en curso y las apagadas por sí solas.
            if (t.color("gp_cristal_kilos_filo") >>> 24 != 0) {
                t.medir("filo de la gráfica de kilos", "gp_cristal_kilos_filo", cristal, GRANDE, fallos);
            } else {
                t.medir("gráfica de kilos, esta semana", "gp_cristal_kilos", cristal, GRANDE, fallos);
                t.medirCompuesto("gráfica de kilos, las demás", "gp_cristal_kilos_tenue", cristal, GRANDE, fallos);
            }
            // El relleno de cada barra de «Tu día» contra su pista.
            Set<Integer> pistas = t.capas(cristal, new String[]{ "gp_cristal_pista" });
            for (String relleno : new String[]{ "gp_cristal_enlace", "gp_cristal_proteinas", "gp_cristal_agua" }) {
                t.medir("relleno contra su pista", relleno, pistas, GRANDE, fallos);
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // La silueta muscular sobre cristal, en claro: su cuerpo se ve contra el cristal (arriba) y la
    // zona trabajada, a pleno color, se distingue del cuerpo por su luminosidad y no solo por el
    // tono (DEC-019). Al oscurecer el cuerpo para que se viera, el naranja de la tarjeta opaca se
    // quedó a 1,2:1 de él (revisor de UI). En oscuro la silueta es la de antes del rediseño
    // (cuerpo claro y zona a 1,3:1): no entra en este lote y por eso no se mide aquí.
    @Test
    public void en_claro_la_zona_trabajada_de_la_silueta_se_distingue_del_cuerpo() throws IOException {
        List<String> fallos = new ArrayList<>();
        Tema t = new Tema("values");
        Set<Integer> cuerpo = new LinkedHashSet<>();
        cuerpo.add(t.color("gp_cristal_silueta"));
        t.medir("zona trabajada sobre el cuerpo", "gp_cristal_silueta_marca", cuerpo, GRANDE, fallos);
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // ── Regla 3: cristal vivo, en los tres niveles ───────────────────────────

    @Test
    public void la_barra_se_lee_con_desenfoque_y_sin_el() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            // Lo que puede quedar detrás, y eso mismo con la saturación del desenfoque.
            Set<Integer> detras = t.detrasDelVivo();
            Set<Integer> saturado = saturados(detras);

            String[][] barras = { VIVO, VIVO_OPACO }, hojas = { HOJA, VIVO_OPACO };
            String[] nombres = { "niveles 1 y 2", "nivel 3" };
            for (int n = 0; n < barras.length; n++) {
                Set<Integer> barra = t.capas(n == 0 ? saturado : detras, barras[n]);
                String donde = "barra, " + nombres[n];
                // Las pestañas sin elegir, y el texto y la acción («Volver») de la barra de la sesión.
                t.medir(donde, "gp_vivo_texto", barra, TEXTO, fallos);
                t.medir(donde, "gp_cristal_texto", barra, TEXTO, fallos);
                t.medir(donde, "gp_vivo_accion", barra, TEXTO, fallos);
                // La pestaña elegida: su icono y su etiqueta, sobre la lente.
                Set<Integer> lente = t.capas(barra, new String[]{ "gp_lente" });
                t.medir(donde + ", lente", "gp_vivo_acento", lente, TEXTO, fallos);
                // La hoja del «+»: sus títulos y sus textos secundarios.
                Set<Integer> hoja = t.capas(n == 0 ? saturado : detras, hojas[n]);
                t.medir("hoja del «+», " + nombres[n], "gp_cristal_texto", hoja, TEXTO, fallos);
                t.medir("hoja del «+», " + nombres[n], "gp_cristal_texto_2", hoja, TEXTO, fallos);
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // El botón de marca (DEC-070): el principal y el «+», macizos, iguales en los dos temas y en
    // los tres niveles. Su texto y su icono (casi negros) pasan 4,5:1 sobre todo su relleno, del
    // tono de arriba al de abajo, también donde el brillo y la media luna lo aclaran, en reposo y
    // pulsado del todo (algo claro encima solo sube el contraste de algo oscuro, pero se mide).
    // Y su relleno se queda a ΔE76 de 15 o menos de #FF6A00 de arriba abajo: es macizo, no
    // depende de lo que haya detrás. Pulsado no lleva capa encima (la de siempre, boton_capa, lo
    // dejaba a ΔE ~18): solo suben el brillo y la media luna, que acaban por encima de la mitad.
    @Test
    public void el_boton_de_marca_se_lee_y_sigue_en_la_marca() throws IOException {
        String barra = new String(Files.readAllBytes(RES.resolve("layout/view_barra_navegacion.xml")),
                StandardCharsets.UTF_8);
        int desde = barra.indexOf("android:id=\"@+id/btnAcciones\"");
        String mas = barra.substring(desde, barra.indexOf('>', desde));
        assertFalse("el «+» no lleva capa de pulsado encima: lo aleja de la marca",
                mas.contains("android:foreground"));
        assertTrue("el brillo acaba por encima de la mitad", Cristal.BRILLO_Y + Cristal.BRILLO_RADIO < 0.5f);
        assertTrue("la media luna acaba por encima de la mitad", Cristal.REFLEJO_ALTO < 0.5f);

        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> relleno = new LinkedHashSet<>();
            for (String tono : MARCA_RELLENO) relleno.add(t.color(tono));
            Set<Integer> superficie = new LinkedHashSet<>(relleno);
            float luz = 1f + Cristal.LUZ_PULSADO;
            for (String capa : new String[]{ "gp_marca_brillo", "gp_marca_reflejo" }) {
                int c = t.color(capa);
                int pulsada = (Math.min(255, Math.round((c >>> 24) * luz)) << 24) | (c & 0xFFFFFF);
                for (int r : relleno) {
                    superficie.add(Ambiente.sobre(c, r));
                    superficie.add(Ambiente.sobre(pulsada, r));
                }
            }
            t.medir("botón de marca", "gp_marca_texto", superficie, TEXTO, fallos);
            for (String tono : MARCA_RELLENO) {
                double d = deltaE(t.color(tono), MARCA);
                INFORME.append(String.format(Locale.ROOT, "%s · botón de marca, %s · ΔE76 %.1f%n", t.nombre, tono, d));
                if (d > 15) fallos.add(String.format(Locale.ROOT, "%s: %s queda a ΔE %.1f de #FF6A00", t.nombre, tono, d));
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // El botón secundario (BotonCristal): cristal estático en píldora, con los tokens de las
    // tarjetas, sobre cualquier punto del fondo (también el más claro); en el nivel 3, la
    // superficie opaca del cristal vivo. Su texto pasa 4,5:1 en los dos casos y los dos temas.
    @Test
    public void el_boton_secundario_se_lee_en_los_tres_niveles() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> cristal = t.capas(t.fondo(), CRISTAL);
            t.medir("botón secundario", "gp_cristal_texto", cristal, TEXTO, fallos);
            t.medir("botón secundario, lo más claro", "gp_cristal_texto", masClaro(cristal), TEXTO, fallos);
            t.medir("botón secundario, nivel 3", "gp_cristal_texto", t.capas(t.fondo(), VIVO_OPACO), TEXTO, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // El canto del botón secundario se distingue de la tarjeta de cristal sobre la que va (revisor
    // de UI, lote 1c: en claro, con el borde blanco del cristal, quedaba a 1,18:1 y el botón casi
    // no se veía). Su borde (gp_cristal_boton_borde; transparente, el del cristal) compuesto sobre
    // la tarjeta, frente a la tarjeta, en su peor punto: 1,4:1 o más. No es texto: lo que dice el
    // botón ya pasa 4,5:1; esto es que se vea su forma.
    @Test
    public void el_canto_del_boton_secundario_se_ve_sobre_la_tarjeta() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            int borde = t.color("gp_cristal_boton_borde");
            if (borde >>> 24 == 0) borde = t.color("gp_cristal_borde");
            double peor = Double.MAX_VALUE;
            for (int tarjeta : t.capas(t.fondo(), CRISTAL)) {
                peor = Math.min(peor, contraste(Ambiente.sobre(borde, tarjeta), tarjeta));
            }
            INFORME.append(String.format(Locale.ROOT, "%s · canto del botón secundario sobre la tarjeta · %.2f:1%n",
                    t.nombre, peor));
            if (peor < 1.4) fallos.add(String.format(Locale.ROOT,
                    "%s: el canto del botón secundario queda a %.2f:1 de la tarjeta", t.nombre, peor));
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // ── La Sesión en vivo (lote 2) ───────────────────────────────────────────

    // La Sesión en vivo tiene su propio fondo (naranja, dorado y azul: Ambiente.sesion) y sus
    // propias superficies: la tarjeta de un ejercicio, la fila de la serie en curso (la lente),
    // la fila dorada de un récord y el campo de kg o de repeticiones (un surco). Cada texto pasa
    // 4,5:1 sobre el peor punto de todo lo que puede tener debajo, y cada gráfico (el aro de una
    // serie sin marcar, el borde de un campo, el trofeo) 3:1.
    @Test
    public void la_sesion_en_vivo_se_lee_en_los_dos_temas() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> fondo = t.fondoSesion();
            // Sobre el fondo, sin cristal: el estado de la carga y «Descartar la sesión».
            t.medir("sesión, sobre el fondo", "gp_cristal_texto", fondo, TEXTO, fallos);
            t.medir("sesión, sobre el fondo", "gp_cristal_texto_2", fondo, TEXTO, fallos);
            t.medir("sesión, sobre el fondo", "gp_cristal_error", fondo, TEXTO, fallos);
            // La tarjeta de un ejercicio.
            Set<Integer> tarjeta = t.capas(fondo, CRISTAL);
            for (String texto : new String[]{ "gp_cristal_texto", "gp_cristal_texto_2", "gp_cristal_oro" }) {
                t.medir("sesión, tarjeta", texto, tarjeta, TEXTO, fallos);
            }
            for (String grafico : new String[]{ "gp_cristal_grafico", "gp_cristal_exito" }) {
                t.medir("sesión, gráfico en la tarjeta", grafico, tarjeta, GRANDE, fallos);
            }
            // El aro de una serie sin marcar, el borde de un campo y el de «Añadir serie».
            t.medirCompuesto("sesión, aro y borde de campo", "gp_cristal_barra_tenue", tarjeta, GRANDE, fallos);
            // La serie en curso: la lente. Lo que tenía hecho la última vez va en el texto
            // principal (el secundario no llega sobre la lente) y su número, sus kilos y su aro,
            // en el acento de la lente.
            Set<Integer> lente = t.capas(tarjeta, new String[]{ "gp_lente" });
            for (String texto : new String[]{ "gp_cristal_texto", "gp_cristal_lente_acento" }) {
                t.medir("sesión, serie en curso", texto, lente, TEXTO, fallos);
            }
            // El récord en dorado: el número de la serie y «¡Récord!» en la fila dorada, y el
            // título del aviso. Texto pequeño: 4,5:1 en los dos temas. La fila es el tinte dorado
            // del diseño sobre un surco oscuro (en claro no hay surco).
            Set<Integer> oro = t.capas(t.color("gp_cristal_oro_surco") >>> 24 == 0 ? tarjeta
                    : t.capas(tarjeta, new String[]{ "gp_cristal_oro_surco" }), ORO);
            t.medir("sesión, récord en dorado", "gp_cristal_oro_fila", oro, TEXTO, fallos);
            t.medir("sesión, texto sobre el récord", "gp_cristal_texto", oro, TEXTO, fallos);
            // Los botones de marcar: el check sobre el círculo de una serie hecha y el trofeo
            // sobre el círculo dorado de la del récord (gráficos: 3:1).
            t.medir("sesión, check de la serie hecha", "gp_cristal_exito",
                    t.capas(tarjeta, new String[]{ "gp_cristal_exito_fondo" }), GRANDE, fallos);
            t.medir("sesión, trofeo de la serie del récord", "gp_cristal_oro_fila",
                    t.capas(oro, new String[]{ "gp_cristal_oro_alto" }), GRANDE, fallos);
            // Los campos: un surco sobre la tarjeta, sobre la lente o sobre la fila dorada. Lo
            // escrito va en el texto principal y la pista (lo de la última vez), en el secundario;
            // en la fila dorada, donde el secundario no llega, la pista va también en el principal.
            Set<Integer> debajoDelCampo = new LinkedHashSet<>(tarjeta);
            debajoDelCampo.addAll(lente);
            String[] surco = CAMPO;
            Set<Integer> campo = t.capas(debajoDelCampo, surco);
            t.medir("sesión, campo", "gp_cristal_texto", campo, TEXTO, fallos);
            t.medir("sesión, pista del campo", "gp_cristal_texto_2", campo, TEXTO, fallos);
            t.medir("sesión, campo en el récord", "gp_cristal_texto", t.capas(oro, surco), TEXTO, fallos);
            // En la serie en curso, los kilos (escritos o propuestos) van en el acento de la lente.
            t.medir("sesión, kilos de la serie en curso", "gp_cristal_lente_acento", t.capas(lente, surco), TEXTO, fallos);
            // El borde del campo también contra lo de dentro: un control, 3:1.
            t.medirCompuesto("sesión, borde del campo por dentro", "gp_cristal_barra_tenue", campo, GRANDE, fallos);
            // Minimizar y «Terminar»: cristal estático sobre el fondo (nada pasa por detrás).
            t.medir("sesión, «Terminar» en cristal estático", "gp_vivo_accion", tarjeta, TEXTO, fallos);
            t.medir("sesión, minimizar en cristal estático", "gp_cristal_texto", tarjeta, GRANDE, fallos);
            // En el nivel 3 el botón secundario es la superficie opaca del cristal vivo.
            t.medir("sesión, «Terminar», nivel 3", "gp_vivo_accion", t.capas(fondo, VIVO_OPACO), TEXTO, fallos);
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // Los campos de kg y reps en claro (remate del lote 2): eran un surco gris (la pista de las
    // barras, tinta al 10 %) que sobre la tarjeta clara se leía como una caja gris opaca. Son
    // cristal estático con los tokens de las tarjetas: relleno translúcido claro y canto de
    // tinta. En oscuro no cambia: el surco de siempre.
    @Test
    public void los_campos_son_cristal_de_tarjeta_en_claro_y_el_surco_de_siempre_en_oscuro() throws IOException {
        Tema claro = new Tema("values"), oscuro = new Tema("values-night");
        for (int i = 0; i < CAMPO.length; i++) {
            int relleno = claro.color(CAMPO[i]);
            assertEquals("claro, " + CAMPO[i] + " es el de las tarjetas", claro.color(CRISTAL[i]), relleno);
            assertTrue("claro, " + CAMPO[i] + " es translúcido", relleno >>> 24 < 0xFF && relleno >>> 24 > 0);
            assertEquals("claro, " + CAMPO[i] + " es blanco, no tinta", 0xFFFFFF, relleno & 0xFFFFFF);
            assertEquals("oscuro, " + CAMPO[i] + " no se toca", oscuro.color("gp_cristal_pista"), oscuro.color(CAMPO[i]));
        }
        // El dibujo: lo pinta Cristal (tipo «campo»), con el borde de siempre y el de foco.
        String dibujo = new String(Files.readAllBytes(RES.resolve("drawable").resolve("bg_cristal_campo.xml")),
                StandardCharsets.UTF_8).replaceAll("(?s)<!--.*?-->", "");
        assertTrue("el campo lo pinta Cristal", dibujo.contains("app:cristalTipo=\"campo\""));
        assertTrue("con foco, su borde de foco", dibujo.contains("android:state_focused=\"true\"")
                && dibujo.contains("app:cristalTipo=\"campo_foco\""));
        assertFalse("ya no es el surco liso", dibujo.contains("gp_cristal_pista"));
    }

    // El cristal vivo de la Sesión en vivo: el resumen (tiempo, volumen y series) y el panel del
    // descanso. Detrás puede pasar el fondo, una tarjeta, la serie en curso o la fila dorada de
    // un récord. En los tres niveles: con el tinte sobre lo desenfocado (1 y 2) y con la
    // superficie opaca (3). Minimizar y «Terminar» ya no van aquí: son cristal estático (arriba).
    @Test
    public void el_cristal_vivo_de_la_sesion_se_lee_en_los_tres_niveles() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            Tema t = new Tema(tema);
            Set<Integer> detras = t.detrasDelVivoEnLaSesion();
            Set<Integer> saturado = saturados(detras);
            String[][] tintes = { VIVO_SESION, VIVO_OPACO };
            String[] nombres = { "niveles 1 y 2", "nivel 3" };
            for (int n = 0; n < tintes.length; n++) {
                Set<Integer> vivo = t.capas(n == 0 ? saturado : detras, tintes[n]);
                String donde = "sesión, cristal vivo, " + nombres[n];
                t.medir(donde, "gp_cristal_texto", vivo, TEXTO, fallos);
                t.medir(donde, "gp_cristal_texto_2", vivo, TEXTO, fallos);
                // «−15 s» y «+15 s»: cristal estático encima del panel, en los tres niveles.
                t.medir(donde + ", secundario", "gp_cristal_texto", t.capas(vivo, CRISTAL), TEXTO, fallos);
                // La barra que se vacía (y la de la sesión): su relleno contra su pista.
                Set<Integer> pista = t.capas(vivo, new String[]{ "gp_cristal_pista" });
                t.medir(donde + ", barra contra su pista", "gp_cristal_descanso_alto", pista, GRANDE, fallos);
                t.medir(donde + ", barra contra su pista", "gp_cristal_descanso_bajo", pista, GRANDE, fallos);
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    // ── La cuenta ───────────────────────────────────────────────────────────

    // El naranja de la marca, del que el botón de marca no se aleja más de ΔE 15.
    private static final int MARCA = 0xFFFF6A00;

    // ΔE76: la distancia en CIELAB (D65) entre dos colores opacos.
    static double deltaE(int a, int b) {
        double[] la = lab(a), lb = lab(b);
        return Math.sqrt(Math.pow(la[0] - lb[0], 2) + Math.pow(la[1] - lb[1], 2) + Math.pow(la[2] - lb[2], 2));
    }

    private static double[] lab(int color) {
        double r = lineal((color >> 16) & 0xFF), g = lineal((color >> 8) & 0xFF), b = lineal(color & 0xFF);
        double x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883;
        double fx = f(x), fy = f(y), fz = f(z);
        return new double[]{ 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz) };
    }

    private static double f(double t) {
        return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
    }

    // Que la distancia es la de CIELAB: de blanco a negro, 100; un color consigo mismo, 0.
    @Test
    public void la_cuenta_del_delta_e_es_la_de_cielab() {
        assertEquals(100.0, deltaE(0xFFFFFFFF, 0xFF000000), 0.01);
        assertEquals(0.0, deltaE(MARCA, MARCA), 0.0);
    }

    // Que la cuenta de este test es la de siempre: blanco sobre negro, 21:1, y el par que
    // PaletaContrasteTest ya tiene medido (#1A0E00 sobre #FF6A00, 6,61:1).
    @Test
    public void la_cuenta_del_contraste_es_la_de_wcag() {
        assertEquals(21.0, contraste(0xFFFFFFFF, 0xFF000000), 0.001);
        assertEquals(6.61, contraste(0xFF1A0E00, 0xFFFF6A00), 0.01);
        // Un 50 % de blanco sobre negro es gris medio, no otra cosa.
        assertEquals(0xFF808080, Ambiente.sobre(0x80FFFFFF, 0xFF000000));
    }

    // Un brillo se apaga al 70 % de su radio y en su centro tiene su alfa entero. Con los
    // números escritos: un radio de 400 se apaga a 280 del centro.
    @Test
    public void un_brillo_se_apaga_al_70_por_ciento_del_radio() {
        Ambiente.Brillo b = new Ambiente.Brillo(0.5f, 0.5f, 1f, 1f, 0x80FF0000);
        assertEquals(128 / 255f, Ambiente.alfaEn(b, 200, 200, 400, 400), 0.001f);
        assertEquals(0f, Ambiente.alfaEn(b, 200 + 280, 200, 400, 400), 0.001f);
        assertTrue(Ambiente.alfaEn(b, 200 + 276, 200, 400, 400) > 0f);
        assertEquals(64 / 255f, Ambiente.alfaEn(b, 200 + 140, 200, 400, 400), 0.002f);
        // Y más allá sigue apagado.
        assertEquals(0f, Ambiente.alfaEn(b, 200 + 300, 200, 400, 400), 0f);
    }

    // ── Ayudas ──────────────────────────────────────────────────────────────

    private static final StringBuilder INFORME = new StringBuilder();

    /** Los colores de un tema y las superficies que se componen con ellos. */
    private static final class Tema {
        final String nombre;
        final Map<String, String> crudo = new HashMap<>();

        Tema(String carpeta) throws IOException {
            nombre = carpeta.equals("values") ? "claro" : "oscuro";
            leer("values");
            if (!carpeta.equals("values")) leer(carpeta);
        }

        private void leer(String carpeta) throws IOException {
            String xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("colors.xml")), StandardCharsets.UTF_8);
            xml = xml.replaceAll("(?s)<!--.*?-->", "");
            Matcher m = Pattern.compile("<color name=\"([^\"]+)\">([^<]+)</color>").matcher(xml);
            while (m.find()) crudo.put(m.group(1), m.group(2).trim());
        }

        int color(String token) {
            String v = crudo.get(token);
            if (v == null) throw new AssertionError("no existe el color " + token + " en el tema " + nombre);
            if (v.startsWith("@color/")) return color(v.substring(7));
            long n = Long.parseLong(v.substring(1), 16);
            return (int) (v.length() == 7 ? 0xFF000000L | n : n);
        }

        /** Todos los colores que el fondo ambiental puede enseñar, en todas las pantallas. */
        Set<Integer> fondo() {
            Ambiente.Brillo[] brillos = Ambiente.inicio(color("gp_ambiente_brillo_1"),
                    color("gp_ambiente_brillo_2"), color("gp_ambiente_brillo_3"), color("gp_ambiente_brillo_4"));
            int base = color("gp_ambiente_base");
            Set<Integer> colores = new LinkedHashSet<>();
            for (float[] p : PANTALLAS) {
                for (float y = 0; y <= p[1]; y += 2) {
                    for (float x = 0; x <= p[0]; x += 2) {
                        colores.add(Ambiente.colorEn(x, y, p[0], p[1], base, brillos));
                    }
                }
            }
            return colores;
        }

        /** Lo mismo para el fondo de la Sesión en vivo (naranja, dorado y azul). */
        Set<Integer> fondoSesion() {
            Ambiente.Brillo[] brillos = Ambiente.sesion(color("gp_ambiente_sesion_1"),
                    color("gp_ambiente_sesion_2"), color("gp_ambiente_sesion_3"));
            int base = color("gp_ambiente_base");
            Set<Integer> colores = new LinkedHashSet<>();
            for (float[] p : PANTALLAS) {
                for (float y = 0; y <= p[1]; y += 2) {
                    for (float x = 0; x <= p[0]; x += 2) {
                        colores.add(Ambiente.colorEn(x, y, p[0], p[1], base, brillos));
                    }
                }
            }
            return colores;
        }

        /**
         * Lo que puede pasar por detrás del cristal vivo de la Sesión en vivo: su fondo, con o sin
         * la tarjeta de un ejercicio, y sobre la tarjeta la serie en curso o la fila de un récord.
         * Aquí no hay velo: el descanso no oscurece la lista.
         */
        Set<Integer> detrasDelVivoEnLaSesion() {
            Set<Integer> detras = new LinkedHashSet<>(fondoSesion());
            Set<Integer> tarjeta = capas(fondoSesion(), CRISTAL);
            detras.addAll(tarjeta);
            detras.addAll(capas(tarjeta, new String[]{ "gp_lente" }));
            detras.addAll(capas(tarjeta, ORO));
            return detras;
        }

        /**
         * Lo que puede quedar detrás de un cristal vivo: el fondo, con o sin una tarjeta encima,
         * y todo eso con el velo del «+»: con la hoja abierta, la hoja, la barra y el propio «+»
         * desenfocan el contenido ya oscurecido.
         */
        Set<Integer> detrasDelVivo() {
            Set<Integer> detras = new LinkedHashSet<>(fondo());
            detras.addAll(capas(fondo(), CRISTAL));
            detras.addAll(capas(detras, new String[]{ "gp_velo_cristal" }));
            return detras;
        }

        /**
         * Cada color de debajo con cada una de las capas compuesta encima. Una capa transparente
         * (un token que en ese tema no se pinta) no cuenta: no hay tal capa.
         */
        Set<Integer> capas(Set<Integer> debajo, String[] tokens) {
            Set<Integer> colores = new LinkedHashSet<>();
            for (String token : tokens) {
                int capa = color(token);
                if (capa >>> 24 == 0) continue;
                for (int c : debajo) colores.add(Ambiente.sobre(capa, c));
            }
            return colores;
        }

        /** Lo mismo para algo translúcido: se compone sobre cada superficie y se mide contra ella. */
        void medirCompuesto(String donde, String token, Set<Integer> superficies, double minimo, List<String> fallos) {
            int capa = color(token);
            double peor = Double.MAX_VALUE;
            int peorColor = 0;
            for (int s : superficies) {
                double c = contraste(Ambiente.sobre(capa, s), s);
                if (c < peor) {
                    peor = c;
                    peorColor = s;
                }
            }
            apuntar(donde, token, peor, minimo, peorColor, fallos);
        }

        /** El peor contraste del texto sobre esas superficies; si no llega, lo apunta. */
        void medir(String donde, String texto, Set<Integer> superficies, double minimo, List<String> fallos) {
            // Sin superficies no hay nada medido (una capa transparente no se compone): eso no
            // es pasar.
            if (superficies.isEmpty()) {
                fallos.add(nombre + " " + donde + ": no hay ninguna superficie que medir");
                return;
            }
            int t = color(texto);
            double peor = Double.MAX_VALUE;
            int peorColor = 0;
            for (int s : superficies) {
                double c = contraste(t, s);
                if (c < peor) {
                    peor = c;
                    peorColor = s;
                }
            }
            apuntar(donde, texto, peor, minimo, peorColor, fallos);
        }

        private void apuntar(String donde, String texto, double peor, double minimo, int peorColor, List<String> fallos) {
            String linea = String.format(Locale.ROOT, "%-6s %-30s %-26s %5.2f:1 (mínimo %.1f) sobre #%06X",
                    nombre, donde, texto, peor, minimo, peorColor & 0xFFFFFF);
            synchronized (INFORME) {
                INFORME.append(linea).append('\n');
                try {
                    Path salida = Paths.get("build", "reports", "contraste-cristal.txt");
                    Files.createDirectories(salida.getParent());
                    Files.write(salida, INFORME.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
            if (peor < minimo) fallos.add(linea);
        }
    }

    // Esos colores y, además, cada uno con la saturación que les sube el desenfoque.
    private static Set<Integer> saturados(Set<Integer> colores) {
        Set<Integer> todos = new LinkedHashSet<>(colores);
        for (int c : colores) todos.add(saturar(c, CristalVivo.SATURACION));
        return todos;
    }

    // El más luminoso de esos colores, solo.
    private static Set<Integer> masClaro(Set<Integer> colores) {
        int claro = 0;
        double mayor = -1;
        for (int c : colores) {
            if (luminancia(c) > mayor) {
                mayor = luminancia(c);
                claro = c;
            }
        }
        Set<Integer> uno = new LinkedHashSet<>();
        uno.add(claro);
        return uno;
    }

    // La matriz de saturación de Android (ColorMatrix.setSaturation), sobre un color opaco.
    private static int saturar(int color, float s) {
        float r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        float gris = 0.213f * r + 0.715f * g + 0.072f * b;
        return 0xFF000000 | (canal(gris + (r - gris) * s) << 16) | (canal(gris + (g - gris) * s) << 8)
                | canal(gris + (b - gris) * s);
    }

    private static int canal(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static double contraste(int a, int b) {
        double la = luminancia(a), lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(int color) {
        return 0.2126 * lineal((color >> 16) & 0xFF) + 0.7152 * lineal((color >> 8) & 0xFF)
                + 0.0722 * lineal(color & 0xFF);
    }

    private static double lineal(int canal) {
        double v = canal / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
