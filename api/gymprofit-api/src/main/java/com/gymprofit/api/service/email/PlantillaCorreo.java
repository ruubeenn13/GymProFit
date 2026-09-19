package com.gymprofit.api.service.email;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// PlantillaCorreo — la envoltura visual común de los correos de GymProFit
//
// Hasta ahora el HTML se concatenaba dentro de EmailService. Con un correo salía a
// cuenta; con seis, no: cada uno volvería a copiar la cabecera, el pie y los colores, y
// el primer retoque de marca obligaría a tocarlos todos. Aquí la envoltura está una vez
// y cada correo aporta solo su bloque central.
//
// NO hay motor de plantillas a propósito. Thymeleaf o Freemarker añaden dependencia,
// escaneo de classpath y arranque a una API que ya tarda 185 s en levantar en Render,
// todo para seis correos que no cambian en caliente. Bloques de texto de Java bastan.
//
// ---- Por qué el HTML está maquetado con TABLAS -------------------------------------
// Outlook de escritorio renderiza el correo con el motor de Word, no con un motor web:
// no entiende flexbox ni grid, ignora border-radius y box-shadow, y se salta buena parte
// del posicionamiento CSS. Por eso todo va en tablas anidadas con estilos EN LÍNEA (no
// hay bloque <style>: Gmail lo conserva, pero otros clientes lo descartan), los anchos
// van además en el atributo width, y la profundidad del diseño se consigue con cambios
// de tono y líneas de 1 px —que sí sobreviven— y nunca con sombras. Las sombras están
// declaradas porque en los clientes que las entienden suman, pero el diseño se sostiene
// sin ellas. Las "ghost tables" entre comentarios condicionales [if mso] fijan el ancho
// en Outlook, que ignora max-width y si no estiraría la tarjeta a toda la ventana.
//
// Sin imágenes y sin fuentes cargadas: Gmail pasa las imágenes remotas por su proxy y
// muchos clientes las bloquean hasta que el usuario acepta, así que un diseño que
// dependa de ellas llega roto; las fuentes web solo las aplican Apple Mail y poco más.
// La marca es un cuadrado de color y texto, y se ve igual en todas partes.
//
// Ver DEC-026 en documentacion/PRODUCT-DECISIONS.md.
//
// ---- Escapado ----------------------------------------------------------------------
// Los párrafos que recibe esta clase son FRAGMENTOS DE HTML, no texto plano: así el
// llamante puede resaltar un dato con destacar(). La contrapartida es que todo valor que
// venga del usuario tiene que pasar por escapar() ANTES de llegar aquí. Los valores que
// esta clase sí trata como texto —el código, el número grande, las etiquetas, el
// preencabezado, el titular, la URL de baja— los escapa ella misma.
// ============================================================
final class PlantillaCorreo {

    // --- Tokens visuales (dirección «A3 · profundidad por capas») -----------

    /** Naranja de marca: barra de acento, código, botones y enlaces. */
    private static final String ACENTO = "#B83E00";
    /** Tinta de los titulares y de los énfasis dentro del cuerpo. */
    private static final String TINTA = "#1A1512";
    /** Gris del cuerpo de texto. */
    private static final String CUERPO = "#57504A";
    /** Gris de la nota pequeña bajo el protagonista. */
    private static final String NOTA = "#78706A";

    // El resto de tonos —fondo #E4DCD2, línea #F0EBE4, panel #FBF5F0 sobre borde #EDDACB,
    // franja #FAF7F3 y los grises #8A7E74, #6E655D y #6B6259— viven dentro de los bloques
    // de HTML de abajo, porque solo aparecen ahí y sacarlos a constantes obligaría a
    // trocear cada plantilla en concatenaciones y la dejaría ilegible.

    // El dorado #CBA135 NO aparece a propósito: queda reservado para los correos de
    // celebración (récords, rachas). Si se gasta ahora en un correo corriente, deja de
    // significar nada cuando haga falta.

    /** Pila del sistema. Ninguna fuente se descarga: ver la cabecera del fichero. */
    private static final String FUENTE = "-apple-system,'Segoe UI',Helvetica,Arial,sans-serif";
    /** Monoespaciada del sistema, para que las seis cifras del código tengan el mismo ancho. */
    private static final String MONO = "ui-monospace,SFMono-Regular,Menlo,Consolas,monospace";

    /** Marcadores {{...}} que sustituye render(). */
    private static final Pattern MARCADOR = Pattern.compile("\\{\\{(\\w+)}}");

    private PlantillaCorreo() {
    }

    // ========================================================================
    // Variantes de pie
    // ========================================================================

    /**
     * Pie del correo, fuera de la tarjeta. Hay dos variantes y la diferencia no es estética:
     * <ul>
     *   <li><b>Transaccional</b> — sin enlace de baja. Es el correo que el usuario ha
     *       provocado con una acción suya: recuperación, verificación, borrado de cuenta,
     *       avisos de seguridad. No se puede dar de baja de esto sin quedarse sin cuenta.</li>
     *   <li><b>Comercial</b> — con enlace de baja obligatorio. El resumen semanal y los
     *       avisos de récord son marketing a efectos legales aunque no lo parezcan: los
     *       manda el producto por iniciativa propia, y el usuario tiene que poder cortarlos.</li>
     * </ul>
     *
     * @param conBaja si el pie lleva enlace para darse de baja.
     * @param urlBaja destino de ese enlace; se ignora en la variante transaccional.
     */
    record Pie(boolean conBaja, String urlBaja) {

        /** Pie sin enlace de baja, para los correos que el usuario ha pedido. */
        static Pie transaccional() {
            return new Pie(false, null);
        }

        /** Pie con enlace de baja, para todo lo que el producto manda por iniciativa propia. */
        static Pie comercial(String urlBaja) {
            return new Pie(true, urlBaja);
        }
    }

    // ========================================================================
    // Formas de contenido
    // ========================================================================

    /**
     * Bloque central de un correo. Hay tres formas y cada correo elige una; la envoltura
     * —barra de acento, cabecera, franja de cierre y pie— es la misma en las tres.
     */
    sealed interface Contenido permits Accion, Aviso, Datos {

        /** @return el HTML del bloque, listo para incrustar en la zona de contenido. */
        String html();
    }

    /**
     * Forma <b>ACCIÓN</b>: un párrafo, un protagonista único y una nota pequeña debajo.
     * El protagonista es el código de un solo uso o un botón, nunca los dos: si hay dos
     * cosas que hacer, no hay ninguna que destaque.
     *
     * @param parrafo    fragmento HTML de introducción (el llamante ya lo ha escapado).
     * @param etiqueta   versalitas sobre el código, p. ej. «TU CÓDIGO»; puede ser nula.
     * @param codigo     el código a mostrar, o nulo si el protagonista es un botón.
     * @param textoBoton texto del botón, o nulo si el protagonista es un código.
     * @param urlBoton   destino del botón.
     * @param nota       fragmento HTML de la nota pequeña bajo el protagonista; puede ser nulo.
     */
    record Accion(String parrafo, String etiqueta, String codigo,
                  String textoBoton, String urlBoton, String nota) implements Contenido {

        /** Acción cuyo protagonista es un código de un solo uso. */
        static Accion conCodigo(String parrafo, String etiqueta, String codigo, String nota) {
            return new Accion(parrafo, etiqueta, codigo, null, null, nota);
        }

        /** Acción cuyo protagonista es un botón. */
        static Accion conBoton(String parrafo, String textoBoton, String urlBoton, String nota) {
            return new Accion(parrafo, null, null, textoBoton, urlBoton, nota);
        }

        @Override
        public String html() {
            String protagonista = codigo != null
                    ? panelDeCodigo(etiqueta, codigo)
                    : boton(textoBoton, urlBoton);
            return parrafoHtml(parrafo) + protagonista + notaPequena(nota);
        }
    }

    /**
     * Forma <b>AVISO</b>: solo texto, sin nada que pulsar. Es la de «tu contraseña ha
     * cambiado»: informa de algo que ya ha pasado, y meterle un botón enseñaría al usuario
     * a pulsar justo en el correo que un atacante querría que pulsara.
     *
     * @param parrafos fragmentos HTML, uno por párrafo (el llamante ya los ha escapado).
     */
    record Aviso(List<String> parrafos) implements Contenido {

        /** Aviso a partir de sus párrafos. */
        static Aviso de(String... parrafos) {
            return new Aviso(List.of(parrafos));
        }

        @Override
        public String html() {
            StringBuilder html = new StringBuilder();
            for (String texto : parrafos) {
                html.append(parrafoHtml(texto));
            }
            return html.toString();
        }
    }

    /**
     * Forma <b>DATOS</b>: un número grande, su etiqueta y una comparación. Es la del
     * resumen semanal, donde lo que importa es la cifra y si ha subido o bajado, no el texto.
     *
     * @param parrafo     fragmento HTML de introducción; puede ser nulo.
     * @param numero      la cifra protagonista, ya formateada para el idioma del destinatario.
     * @param etiqueta    qué mide esa cifra, en versalitas.
     * @param comparacion contra qué se compara, p. ej. «+12 % respecto a la semana pasada».
     */
    record Datos(String parrafo, String numero, String etiqueta, String comparacion) implements Contenido {

        @Override
        public String html() {
            return parrafoHtml(parrafo) + render(PANEL_DATOS, Map.of(
                    "NUMERO", escapar(numero),
                    "ETIQUETA", escapar(etiqueta),
                    "COMPARACION", escapar(comparacion)));
        }
    }

    // ========================================================================
    // Render
    // ========================================================================

    /**
     * Monta el correo completo: envoltura común más el bloque central que le toque.
     *
     * @param preheader    texto oculto que el cliente de correo enseña en la bandeja junto al
     *                     asunto. Si no se pone, ahí acaba la primera línea visible del HTML.
     * @param titular      dice el propósito del correo; no saluda.
     * @param contenido    el bloque central, en una de las tres formas.
     * @param franjaCierre fragmento HTML de la franja al pie de la tarjeta: qué hacer si esto
     *                     no lo has pedido tú. Puede ser nulo.
     * @param pie          variante de pie, que decide si hay enlace de baja.
     * @return el documento HTML completo.
     */
    static String renderizar(String preheader, String titular, Contenido contenido,
                             String franjaCierre, Pie pie) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("FUENTE", FUENTE);
        valores.put("PREHEADER", escapar(preheader));
        valores.put("TITULAR", escapar(titular));
        valores.put("CONTENIDO", contenido.html());
        valores.put("FRANJA", franja(franjaCierre));
        valores.put("PIE", pieHtml(pie));
        return render(ENVOLTURA, valores);
    }

    /**
     * Resalta un dato dentro de un párrafo: misma tinta que el titular y peso 600.
     * Escapa lo que recibe, así que sirve igual para un dato que venga del usuario.
     *
     * @param texto texto plano a resaltar.
     * @return el fragmento HTML resaltado.
     */
    static String destacar(String texto) {
        return "<span style=\"color:" + TINTA + ";font-weight:600;\">" + escapar(texto) + "</span>";
    }

    /**
     * Escapa un valor que va a acabar dentro del HTML del correo.
     * <p>
     * El nombre de usuario lo elige el usuario: sin esto, un nombre con &lt; o &gt; rompe la
     * maquetación del correo o mete marcado ajeno en el cuerpo.
     *
     * @param texto valor en crudo, posiblemente nulo.
     * @return el mismo valor con los cinco caracteres significativos de HTML escapados.
     */
    static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    // --- Piezas ------------------------------------------------------------

    /** Un párrafo del cuerpo. Devuelve cadena vacía si no hay texto, para no dejar huecos. */
    private static String parrafoHtml(String html) {
        return StringUtils.hasText(html)
                ? "<p style=\"margin:0 0 24px;font-family:" + FUENTE + ";font-size:16px;"
                  + "line-height:25px;color:" + CUERPO + ";\">" + html + "</p>"
                : "";
    }

    /** La nota pequeña bajo el protagonista. */
    private static String notaPequena(String html) {
        return StringUtils.hasText(html)
                ? "<p style=\"margin:22px 0 0;font-family:" + FUENTE + ";font-size:14px;"
                  + "line-height:21px;color:" + NOTA + ";\">" + html + "</p>"
                : "";
    }

    /** Panel del código: el único sitio del correo donde el naranja ocupa tamaño grande. */
    private static String panelDeCodigo(String etiqueta, String codigo) {
        return render(PANEL_CODIGO, Map.of(
                "ETIQUETA", escapar(etiqueta),
                "CODIGO", escapar(codigo)));
    }

    /**
     * Botón «a prueba de balas»: el color va en el atributo bgcolor de una celda además de
     * en el CSS, porque Outlook no pinta el fondo de un enlace con display:inline-block. El
     * radio lo ignora y sale un rectángulo, que es aceptable; lo que no sería aceptable es
     * un botón sin fondo, es decir, invisible.
     */
    private static String boton(String texto, String url) {
        return render(BOTON, Map.of("TEXTO", escapar(texto), "URL", escapar(url)));
    }

    /** Franja de cierre, pegada al borde inferior de la tarjeta. */
    private static String franja(String html) {
        return StringUtils.hasText(html) ? render(FRANJA, Map.of("TEXTO", html)) : "";
    }

    /** Pie, ya fuera de la tarjeta. El enlace de baja solo aparece en la variante comercial. */
    private static String pieHtml(Pie pie) {
        String base = "GymProFit · Este correo se ha enviado automáticamente. No respondas a este mensaje.";
        if (!pie.conBaja()) {
            return base;
        }
        return base + "<br><a href=\"" + escapar(pie.urlBaja()) + "\" style=\"color:" + ACENTO
               + ";text-decoration:underline;\">Darse de baja de estos correos</a>";
    }

    /**
     * Sustituye los marcadores {{CLAVE}} de una plantilla en una sola pasada.
     * <p>
     * En una pasada y no con replace() encadenados a propósito: encadenando, un valor
     * insertado que contuviera «{{PIE}}» lo volvería a ver la sustitución siguiente.
     */
    private static String render(String plantilla, Map<String, String> valores) {
        Matcher marcadores = MARCADOR.matcher(plantilla);
        StringBuilder salida = new StringBuilder();
        while (marcadores.find()) {
            String valor = valores.getOrDefault(marcadores.group(1), "");
            marcadores.appendReplacement(salida, Matcher.quoteReplacement(valor));
        }
        marcadores.appendTail(salida);
        return salida.toString();
    }

    // ========================================================================
    // HTML
    // ========================================================================

    /**
     * La envoltura: barra de acento, cabecera con la marca, zona de contenido, franja de
     * cierre y pie. Tres tablas encajadas —fondo a todo el ancho, columna de 600 px,
     * tarjeta de 520 px— porque centrar con márgenes automáticos no funciona en Outlook.
     * <p>
     * El cuadrado naranja de la cabecera es el hueco reservado al logotipo real: cuando lo
     * haya, sustituye a esa celda sin mover nada más.
     */
    private static final String ENVOLTURA = """
            <!DOCTYPE html>
            <html lang="es" xmlns:o="urn:schemas-microsoft-com:office:office">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light only">
            <title>{{TITULAR}}</title>
            <!--[if mso]><xml><o:OfficeDocumentSettings><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]-->
            </head>
            <body style="margin:0;padding:0;width:100%;background-color:#E4DCD2;">
            <div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;color:#E4DCD2;">{{PREHEADER}}</div>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;background-color:#E4DCD2;">
            <tr><td align="center" style="padding:36px 12px 40px;">
            <!--[if mso]><table role="presentation" width="600" align="center" cellpadding="0" cellspacing="0" border="0"><tr><td><![endif]-->
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;">
            <tr><td align="center" style="padding:0;">
            <!--[if mso]><table role="presentation" width="520" align="center" cellpadding="0" cellspacing="0" border="0"><tr><td><![endif]-->
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:520px;background-color:#FFFFFF;border-radius:6px;box-shadow:0 1px 1px rgba(26,21,18,.06),0 6px 18px rgba(26,21,18,.10);">
            <tr><td height="4" bgcolor="#B83E00" style="height:4px;line-height:4px;font-size:0;background-color:#B83E00;border-radius:6px 6px 0 0;">&nbsp;</td></tr>
            <tr><td style="padding:30px 40px 26px;border-bottom:1px solid #F0EBE4;">
            <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
            <td width="22" style="width:22px;padding:0;">
            <table role="presentation" width="22" cellpadding="0" cellspacing="0" border="0" style="width:22px;"><tr>
            <td height="22" bgcolor="#B83E00" style="width:22px;height:22px;line-height:22px;font-size:0;background-color:#B83E00;border-radius:5px;">&nbsp;</td>
            </tr></table>
            </td>
            <td style="padding-left:10px;font-family:{{FUENTE}};font-size:15px;line-height:22px;font-weight:700;letter-spacing:1.4px;color:#1A1512;">GYMPROFIT</td>
            </tr></table>
            </td></tr>
            <tr><td style="padding:34px 40px 32px;">
            <h1 style="margin:0 0 16px;font-family:{{FUENTE}};font-size:30px;line-height:36px;font-weight:700;letter-spacing:-0.4px;color:#1A1512;">{{TITULAR}}</h1>
            {{CONTENIDO}}
            </td></tr>
            {{FRANJA}}
            </table>
            <!--[if mso]></td></tr></table><![endif]-->
            </td></tr>
            <tr><td align="center" style="padding:24px 16px 0;font-family:{{FUENTE}};font-size:13px;line-height:20px;color:#6B6259;text-align:center;">{{PIE}}</td></tr>
            </table>
            <!--[if mso]></td></tr></table><![endif]-->
            </td></tr>
            </table>
            </body>
            </html>
            """;

    /**
     * Panel del código de un solo uso.
     * <p>
     * El text-indent compensa el letter-spacing: el espaciado se añade también DETRÁS de la
     * última cifra, así que sin empujar el bloque 9 px a la derecha el código se ve
     * descentrado dentro del panel aunque la celda esté centrada.
     */
    private static final String PANEL_CODIGO = """
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;background-color:#FBF5F0;border:1px solid #EDDACB;border-radius:6px;box-shadow:inset 0 1px 0 rgba(255,255,255,.9);">
            <tr><td align="center" style="padding:28px 20px;">
            <div style="font-family:FUENTE_;font-size:12px;line-height:16px;font-weight:600;letter-spacing:1.6px;color:#8A7E74;">{{ETIQUETA}}</div>
            <div style="padding-top:14px;font-family:MONO_;font-size:46px;line-height:50px;mso-line-height-rule:exactly;font-weight:700;letter-spacing:9px;text-indent:9px;color:#B83E00;">{{CODIGO}}</div>
            </td></tr>
            </table>
            """.replace("FUENTE_", FUENTE).replace("MONO_", MONO);

    /** Panel del número grande del resumen. Mismo envase que el código, otra tinta. */
    private static final String PANEL_DATOS = """
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;background-color:#FBF5F0;border:1px solid #EDDACB;border-radius:6px;box-shadow:inset 0 1px 0 rgba(255,255,255,.9);">
            <tr><td align="center" style="padding:28px 20px;">
            <div style="font-family:FUENTE_;font-size:46px;line-height:50px;mso-line-height-rule:exactly;font-weight:700;letter-spacing:-1px;color:#1A1512;">{{NUMERO}}</div>
            <div style="padding-top:10px;font-family:FUENTE_;font-size:12px;line-height:16px;font-weight:600;letter-spacing:1.6px;color:#8A7E74;">{{ETIQUETA}}</div>
            <div style="padding-top:12px;font-family:FUENTE_;font-size:14px;line-height:21px;color:#78706A;">{{COMPARACION}}</div>
            </td></tr>
            </table>
            """.replace("FUENTE_", FUENTE);

    /** Botón del protagonista cuando la acción no es teclear un código. */
    private static final String BOTON = """
            <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto;">
            <tr><td align="center" bgcolor="#B83E00" style="background-color:#B83E00;border-radius:6px;">
            <a href="{{URL}}" style="display:inline-block;padding:15px 30px;font-family:FUENTE_;font-size:16px;line-height:20px;font-weight:600;color:#FFFFFF;text-decoration:none;">{{TEXTO}}</a>
            </td></tr>
            </table>
            """.replace("FUENTE_", FUENTE);

    /** Franja de cierre, con su línea de 1 px y las esquinas inferiores de la tarjeta. */
    private static final String FRANJA = """
            <tr><td style="padding:22px 40px;background-color:#FAF7F3;border-top:1px solid #F0EBE4;border-radius:0 0 6px 6px;font-family:FUENTE_;font-size:14px;line-height:22px;color:#6E655D;">{{TEXTO}}</td></tr>
            """.replace("FUENTE_", FUENTE);
}
