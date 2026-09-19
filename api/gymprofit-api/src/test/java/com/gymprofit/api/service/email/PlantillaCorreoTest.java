package com.gymprofit.api.service.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// PlantillaCorreoTest — la envoltura común de los correos
//
// Lo que se comprueba aquí no es que el HTML sea bonito, que eso se mira a ojo, sino las
// tres cosas que se rompen en silencio y solo se descubren cuando ya han salido cientos
// de correos: que el dato protagonista llega al cuerpo, que lo que escribe el usuario no
// puede inyectar marcado, y que el pie comercial lleva enlace de baja y el transaccional
// no —que es lo único de este fichero con consecuencias legales—.
//
// Además se renderizan las tres formas de contenido, incluidas las dos que todavía no
// usa ningún correo: si AVISO o DATOS se rompen, quiero enterarme ahora y no el día que
// se escriba el resumen semanal.
// ============================================================
class PlantillaCorreoTest {

    private static final String BAJA = "https://gymprofit.app/baja?t=abc";

    // --- Contenido ----------------------------------------------------------

    /** Lo mínimo: el código tiene que aparecer en el HTML, o el correo no sirve para nada. */
    @Test
    void laFormaAccion_poneElCodigoEnElHtml() {
        String html = PlantillaCorreo.renderizar(
                "Código 483920",
                "Restablece tu contraseña",
                PlantillaCorreo.Accion.conCodigo("Escribe este código.", "TU CÓDIGO", "483920", "Caduca pronto."),
                "Si no lo has pedido, ignora este correo.",
                PlantillaCorreo.Pie.transaccional());

        assertThat(html).contains("483920").contains("TU CÓDIGO").contains("Restablece tu contraseña");
    }

    /**
     * El nombre de usuario lo elige el usuario y acaba dentro del HTML del correo. Sin
     * escapar, un nombre con etiquetas rompe la maquetación o cuela marcado ajeno en el
     * cuerpo; el enlace del ejemplo sería un enlace de verdad, pulsable, en un correo que
     * viene del remitente de GymProFit.
     */
    @Test
    void unNombreConHtml_saleEscapado() {
        String nombre = "<a href=\"http://malo\">pulsa</a> & 'co'";

        String html = PlantillaCorreo.renderizar(
                "Hola",
                "Restablece tu contraseña",
                PlantillaCorreo.Accion.conCodigo(
                        "Hola, " + PlantillaCorreo.escapar(nombre) + ".", "TU CÓDIGO", "483920", null),
                null,
                PlantillaCorreo.Pie.transaccional());

        assertThat(html).contains("&lt;a href=&quot;http://malo&quot;&gt;pulsa&lt;/a&gt; &amp; &#39;co&#39;");
        assertThat(html).doesNotContain("<a href=\"http://malo\"");
    }

    /** destacar() también escapa: si no, sería la puerta de atrás del escapado anterior. */
    @Test
    void destacar_escapaLoQueResalta() {
        assertThat(PlantillaCorreo.destacar("<b>15</b>"))
                .contains("&lt;b&gt;15&lt;/b&gt;")
                .doesNotContain("<b>15</b>");
    }

    /**
     * Un valor insertado no puede volver a pasar por la sustitución: si el nombre de usuario
     * fuese literalmente «{{PIE}}», con replace() encadenados acabaría convertido en el pie
     * del correo. Por eso la sustitución es de una sola pasada.
     */
    @Test
    void unValorQueParezcaUnMarcador_noSeVuelveASustituir() {
        String html = PlantillaCorreo.renderizar(
                "Hola",
                "{{PIE}}",
                PlantillaCorreo.Aviso.de("{{CONTENIDO}}"),
                null,
                PlantillaCorreo.Pie.transaccional());

        assertThat(html).contains("{{PIE}}").contains("{{CONTENIDO}}");
    }

    // --- Variantes de pie ---------------------------------------------------

    /**
     * El pie transaccional NO puede llevar baja: son los correos que el usuario ha pedido
     * —recuperación, verificación, borrado de cuenta— y darse de baja de ellos equivale a
     * quedarse sin poder recuperar la cuenta.
     */
    @Test
    void elPieTransaccional_noLlevaEnlaceDeBaja() {
        String html = PlantillaCorreo.renderizar(
                "Hola", "Restablece tu contraseña",
                PlantillaCorreo.Aviso.de("Texto."), null,
                PlantillaCorreo.Pie.transaccional());

        assertThat(html).doesNotContain("Darse de baja").doesNotContain(BAJA);
    }

    /**
     * El comercial sí, y esto no es una preferencia de diseño: el resumen semanal y los
     * avisos de récord los manda el producto por iniciativa propia, así que son marketing
     * y el enlace de baja es obligatorio aunque el correo hable de sentadillas.
     */
    @Test
    void elPieComercial_llevaEnlaceDeBaja() {
        String html = PlantillaCorreo.renderizar(
                "Tu semana", "Tu semana en GymProFit",
                PlantillaCorreo.Aviso.de("Texto."), null,
                PlantillaCorreo.Pie.comercial(BAJA));

        assertThat(html).contains("Darse de baja").contains("href=\"" + BAJA + "\"");
    }

    // --- Las tres formas sobre la misma envoltura ---------------------------

    /**
     * Las tres formas tienen que entrar en la envoltura sin romperla: misma cabecera, misma
     * marca, mismo pie y el documento cerrado. Dos de ellas todavía no las usa nadie, que es
     * justo el motivo de probarlas: el día que se escriba el resumen semanal no quiero
     * descubrir entonces que DATOS nunca se renderizó.
     */
    @Test
    void lasTresFormas_renderizanSinRomperLaEnvoltura() {
        PlantillaCorreo.Contenido[] formas = {
                PlantillaCorreo.Accion.conCodigo("Escribe esto.", "TU CÓDIGO", "483920", "Caduca pronto."),
                PlantillaCorreo.Accion.conBoton("Confirma tu correo.", "Verificar", "https://gymprofit.app/v/1", null),
                PlantillaCorreo.Aviso.de("Tu contraseña ha cambiado.", "Si no has sido tú, escríbenos."),
                new PlantillaCorreo.Datos("Esta es tu semana.", "7.400", "KCAL QUEMADAS", "+12 % que la semana pasada")
        };

        for (PlantillaCorreo.Contenido forma : formas) {
            String html = PlantillaCorreo.renderizar(
                    "Preencabezado", "Un titular", forma, "Franja de cierre.",
                    PlantillaCorreo.Pie.transaccional());

            assertThat(html).startsWith("<!DOCTYPE html>").endsWith("</html>\n");
            assertThat(html).contains("GYMPROFIT").contains("Un titular").contains("Franja de cierre.");
            // Envoltura intacta: la etiqueta de apertura del cuerpo y su cierre, una vez cada una.
            assertThat(html).containsOnlyOnce("</body>").containsOnlyOnce("</html>");
            // El diseño no depende de que cargue nada: ni imágenes ni fuentes remotas.
            assertThat(html).doesNotContain("<img").doesNotContain("@import").doesNotContain("fonts.googleapis");
            // Maquetación con tablas, no con flex ni grid, porque Outlook usa el motor de Word.
            assertThat(html).doesNotContain("display:flex").doesNotContain("display:grid");
        }
    }

    /** El preencabezado va oculto y al principio: es lo que se lee en la bandeja, no en el correo. */
    @Test
    void elPreencabezado_vaOcultoYAntesDelContenido() {
        String html = PlantillaCorreo.renderizar(
                "Código 483920, válido 15 minutos.", "Restablece tu contraseña",
                PlantillaCorreo.Aviso.de("Texto."), null,
                PlantillaCorreo.Pie.transaccional());

        int preencabezado = html.indexOf("Código 483920, válido 15 minutos.");
        assertThat(preencabezado).isPositive();
        assertThat(preencabezado).isLessThan(html.indexOf("GYMPROFIT"));
        assertThat(html.substring(0, preencabezado)).contains("display:none");
    }

    /** La forma AVISO no puede tener nada que pulsar: es su razón de existir. */
    @Test
    void laFormaAviso_noTieneNadaQuePulsar() {
        String html = PlantillaCorreo.renderizar(
                "Tu contraseña ha cambiado", "Tu contraseña ha cambiado",
                PlantillaCorreo.Aviso.de("Acabas de cambiar tu contraseña.", "Si no has sido tú, escríbenos."),
                "Si no reconoces este cambio, responde a este correo.",
                PlantillaCorreo.Pie.transaccional());

        assertThat(html).doesNotContain("<a ");
        assertThat(html).contains("Acabas de cambiar tu contraseña.").contains("Si no has sido tú, escríbenos.");
    }
}
