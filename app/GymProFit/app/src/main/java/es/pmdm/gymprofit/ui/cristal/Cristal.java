package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.xmlpull.v1.XmlPullParser;

import es.pmdm.gymprofit.R;

// ============================================================
// Cristal — el cristal estático de Liquid Glass (GP-285, DEC-070).
//
// Una superficie translúcida sobre el fondo ambiental: relleno en degradado a 160° (blanco
// 16 % → 5 % al 55 % → 9 % en oscuro; 62 % → 34 % → 48 % en claro), borde de 1 dp y un brillo
// de 1 dp por dentro del canto de arriba. En claro lleva además una línea más tenue por
// dentro del canto de abajo y un filo oscuro de medio dp en el borde: sobre un fondo claro, el
// cristal se despega por sus cantos (en oscuro esos dos colores son transparentes y no se
// pintan). No desenfoca nada: el fondo ya es difuso, así que es igual en todas las versiones
// de Android y no cuesta más que un degradado.
//
// Tipos:
//   · TARJETA      tarjetas, tiles y chips (el radio lo da cristalRadio; sin él, píldora).
//   · LENTE        la selección: la pestaña activa de la barra. En claro, con sombra.
//   · ORO          la fila de un récord: degradado dorado de lado a lado y brillo.
//   · ORO_FILA     la fila de la serie que batió un récord en la Sesión en vivo (lote 2): el
//                  mismo dorado sobre un surco oscuro (gp_cristal_oro_surco; en claro no hay),
//                  para que lo dorado que se escribe encima pase 4,5:1.
//   · VIVO         el tinte que el cristal vivo pinta sobre su desenfoque (CristalVivo).
//   · VIVO_OPACO   la superficie del cristal vivo cuando no desenfoca (nivel 3).
//   · VIVO_GRUESO  el tinte de la hoja del «+», con más cuerpo que el de la barra.
//   · MARCA        el botón principal y el «+» de la barra (lote 1c): NO es cristal, es el
//                  naranja de la marca macizo con acabado, igual en los tres niveles. De abajo
//                  arriba: una sombra suave del propio naranja por fuera, el relleno en degradado
//                  vertical muy suave (gp_marca_alto → _bajo, los dos a ΔE ≤ 15 de #FF6A00), un
//                  brillo radial pequeño en el tercio de arriba (en un botón ancho, una elipse
//                  centrada de en torno al 60 % de su ancho), el borde de 1,5 dp en degradado
//                  (claro arriba, tenue abajo) y el reflejo de arriba, una media luna que se apaga
//                  hacia abajo. Pulsado, el brillo y la media luna suben (ponerLuz, Hundir).
//   · CAMPO        el campo de una serie (kg, reps, tiempo) en la Sesión en vivo: el relleno
//                  gp_cristal_campo_* (en claro, el de las tarjetas; en oscuro, el surco liso) con
//                  el canto de un control (gp_cristal_barra_tenue, 3:1), sin brillos ni filo.
//   · CAMPO_FOCO   el mismo con foco: el canto de 2 dp en el naranja de los gráficos.
//
// La sombra del cristal estático en claro no la pinta este dibujo sino su tarjeta
// (TarjetaCristal), que es quien puede dibujar fuera de su caja; la de la lente sí, y por eso
// el padre de la lente no recorta a sus hijos.
//
// Se declara en res/drawable con <drawable class=…> (bg_cristal*.xml), como BurbujaProfi, o se
// crea desde código (CristalVivo). Los colores son los gp_cristal_* de colors.xml.
// ============================================================
public class Cristal extends Drawable {

    public static final int TARJETA = 0, LENTE = 1, ORO = 2, ORO_FILA = 3, VIVO = 4, VIVO_OPACO = 5, VIVO_GRUESO = 6;
    public static final int MARCA = 7;
    public static final int CAMPO = 8, CAMPO_FOCO = 9;

    /** El ángulo del degradado del relleno, como en CSS: 0 hacia arriba, 90 hacia la derecha. */
    static final float ANGULO = 160f;
    /** Dónde cae el tono medio del relleno. */
    static final float MEDIO = 0.55f;
    /** Hasta dónde baja la media luna del reflejo de un botón de marca: su cuarto de arriba. */
    static final float REFLEJO_ALTO = 0.25f;
    // El brillo radial de un botón de marca: su centro y su radio, en fracción de su caja (el
    // radio, del lado corto). Pequeño y solo en el tercio de arriba: más grande, aclaraba todo
    // el botón y lo alejaba del naranja de la marca.
    static final float BRILLO_X = 0.32f, BRILLO_Y = 0.14f, BRILLO_RADIO = 0.19f;
    // En un botón ancho (el principal, «Empezar»), el brillo es una elipse centrada que ocupa en
    // torno al 60 % de su ancho (BRILLO_ANCHO es su semieje, en fracción del ancho), con el mismo
    // alto y la misma altura que el del «+»: con el centro del redondo se leía como un punto a
    // la izquierda (propietario, 27-11). Ancho es más de ANCHO_DESDE veces su alto.
    static final float BRILLO_ANCHO = 0.30f, ANCHO_DESDE = 1.5f;
    // Cuánto del brillo y de la media luna se pinta otra vez encima con el botón pulsado del todo.
    static final float LUZ_PULSADO = 0.75f;

    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    // El surco de la fila dorada: lo que oscurece la tarjeta bajo el tinte.
    private final Paint surco = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint luz = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint luzBaja = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint reflejo = new Paint(Paint.ANTI_ALIAS_FLAG);
    // El botón de marca: su brillo radial.
    private final Paint brilloRadial = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path forma = new Path(), bordeForma = new Path(), luzForma = new Path();
    private final Path luzBajaForma = new Path(), filoForma = new Path(), reflejoForma = new Path();
    private final RectF caja = new RectF();

    private int tipo = TARJETA;
    // Radio de las esquinas en px; negativo, la mitad del alto (píldora).
    private float radio = -1f;
    private float grosor = 1f, grosorFilo = 1f;
    private int alto, medio, bajo;
    // La sombra de la lente (solo en claro): sus medidas en px y su color.
    private float sombraDifuminado, sombraBaja;
    private int sombraColor;
    @Nullable private SombraCristal sombra;
    // El botón de marca: su reflejo (color y cuánto se mete del canto, en px), el brillo radial
    // y los dos extremos del degradado de su borde.
    private int reflejoColor, brilloRadialColor, bordeAlto, bordeBajo;
    private float reflejoDentro;
    // El botón de marca: lo pulsado que está, de 0 a 1 (ponerLuz).
    private float luzPulsado;
    // Lo que se ve del dibujo entero, de 0 a 255 (setAlpha).
    private int alfa = 255;

    /** Para <drawable class=…>: los atributos llegan en inflate(). */
    public Cristal() {
        relleno.setStyle(Paint.Style.FILL);
        // Un pincel nace negro opaco: el surco solo existe en la fila dorada (ORO_FILA).
        surco.setStyle(Paint.Style.FILL);
        surco.setColor(0);
        trazo.setStyle(Paint.Style.STROKE);
        luz.setStyle(Paint.Style.FILL);
        luzBaja.setStyle(Paint.Style.FILL);
        filo.setStyle(Paint.Style.STROKE);
        reflejo.setStyle(Paint.Style.FILL);
        reflejo.setDither(true);
        brilloRadial.setStyle(Paint.Style.FILL);
        brilloRadial.setDither(true);
        relleno.setDither(true);
    }

    /**
     * Desde código.
     *
     * @param tipo  uno de TARJETA … CAMPO_FOCO.
     * @param radio radio de las esquinas en px; negativo, píldora.
     */
    public Cristal(@NonNull Context context, int tipo, float radio) {
        this();
        this.tipo = tipo;
        this.radio = radio;
        leerColores(context.getResources(), context.getTheme());
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
                        @Nullable Resources.Theme theme) throws java.io.IOException,
            org.xmlpull.v1.XmlPullParserException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = theme != null ? theme.obtainStyledAttributes(attrs, R.styleable.Cristal, 0, 0)
                : r.obtainAttributes(attrs, R.styleable.Cristal);
        tipo = a.getInt(R.styleable.Cristal_cristalTipo, TARJETA);
        radio = a.getDimension(R.styleable.Cristal_cristalRadio, -1f);
        a.recycle();
        leerColores(r, theme);
    }

    private void leerColores(@NonNull Resources r, @Nullable Resources.Theme theme) {
        grosor = r.getDimension(R.dimen.cristal_borde);
        grosorFilo = r.getDimension(R.dimen.cristal_filo);
        trazo.setStrokeWidth(grosor);
        filo.setStrokeWidth(grosorFilo);
        int borde = R.color.gp_cristal_borde, brillo = R.color.gp_cristal_brillo;
        // La línea de abajo y el filo son del cristal entero (tarjetas y cristal vivo).
        int brilloBajo = R.color.gp_cristal_brillo_bajo, canto = R.color.gp_cristal_filo;
        int a = R.color.gp_cristal_alto, m = R.color.gp_cristal_medio, b = R.color.gp_cristal_bajo;
        switch (tipo) {
            case LENTE:
                a = m = b = R.color.gp_lente;
                borde = R.color.gp_lente_resplandor;
                brillo = R.color.gp_lente_brillo;
                brilloBajo = canto = 0;
                sombraColor = ResourcesCompat.getColor(r, R.color.gp_lente_sombra, theme);
                sombraDifuminado = r.getDimension(R.dimen.lente_sombra_difuminado);
                sombraBaja = r.getDimension(R.dimen.lente_sombra_baja);
                break;
            case ORO_FILA:
                surco.setColor(ResourcesCompat.getColor(r, R.color.gp_cristal_oro_surco, theme));
                // Sigue: el tinte es el del récord.
            case ORO:
                a = R.color.gp_cristal_oro_alto;
                b = R.color.gp_cristal_oro_bajo;
                m = 0;
                borde = brilloBajo = canto = 0;
                brillo = R.color.gp_cristal_oro_brillo;
                break;
            case VIVO:
                a = R.color.gp_vivo_alto;
                m = R.color.gp_vivo_medio;
                b = R.color.gp_vivo_bajo;
                break;
            case VIVO_GRUESO:
                a = R.color.gp_vivo_hoja_alto;
                b = R.color.gp_vivo_hoja_bajo;
                m = 0;
                break;
            case VIVO_OPACO:
                a = R.color.gp_vivo_opaco_alto;
                b = R.color.gp_vivo_opaco_bajo;
                m = 0;
                break;
            case MARCA:
                a = R.color.gp_marca_alto;
                b = R.color.gp_marca_bajo;
                m = 0;
                // El borde lo pinta un degradado (bordeAlto → bordeBajo, prepararMarca).
                borde = R.color.gp_marca_borde_alto;
                brillo = brilloBajo = canto = 0;
                brilloRadialColor = ResourcesCompat.getColor(r, R.color.gp_marca_brillo, theme);
                bordeAlto = ResourcesCompat.getColor(r, R.color.gp_marca_borde_alto, theme);
                bordeBajo = ResourcesCompat.getColor(r, R.color.gp_marca_borde_bajo, theme);
                reflejoColor = ResourcesCompat.getColor(r, R.color.gp_marca_reflejo, theme);
                reflejoDentro = r.getDimension(R.dimen.marca_reflejo_dentro);
                grosor = r.getDimension(R.dimen.marca_borde);
                trazo.setStrokeWidth(grosor);
                sombraColor = ResourcesCompat.getColor(r, R.color.gp_marca_sombra, theme);
                sombraDifuminado = r.getDimension(R.dimen.marca_sombra_difuminado);
                sombraBaja = r.getDimension(R.dimen.marca_sombra_baja);
                break;
            case CAMPO_FOCO:
                borde = R.color.gp_cristal_grafico;
                grosor = r.getDimension(R.dimen.cristal_campo_foco);
                trazo.setStrokeWidth(grosor);
                // Sigue: el relleno es el del campo.
            case CAMPO:
                a = R.color.gp_cristal_campo_alto;
                m = R.color.gp_cristal_campo_medio;
                b = R.color.gp_cristal_campo_bajo;
                if (tipo == CAMPO) borde = R.color.gp_cristal_barra_tenue;
                // Un canto de tinta y nada más: el brillo blanco de arriba lo taparía.
                brillo = brilloBajo = canto = 0;
                break;
            default:
                break;
        }
        alto = a == 0 ? 0 : ResourcesCompat.getColor(r, a, theme);
        bajo = b == 0 ? 0 : ResourcesCompat.getColor(r, b, theme);
        // Sin tono medio propio (o con uno transparente), el degradado va recto del alto al bajo.
        medio = m == 0 ? 0 : ResourcesCompat.getColor(r, m, theme);
        trazo.setColor(borde == 0 ? 0 : ResourcesCompat.getColor(r, borde, theme));
        luz.setColor(brillo == 0 ? 0 : ResourcesCompat.getColor(r, brillo, theme));
        luzBaja.setColor(brilloBajo == 0 ? 0 : ResourcesCompat.getColor(r, brilloBajo, theme));
        filo.setColor(canto == 0 ? 0 : ResourcesCompat.getColor(r, canto, theme));
    }

    /** El radio de las esquinas para un alto dado: el pedido o, sin él, la mitad del alto. */
    float radioPara(float altoPx) {
        return radio < 0 ? altoPx / 2f : Math.min(radio, altoPx / 2f);
    }

    /**
     * Otro color para el borde (el botón secundario en claro, BotonCristal). Uno transparente no
     * cambia nada: se queda el del cristal.
     */
    public void ponerBorde(@androidx.annotation.ColorInt int color) {
        if (color >>> 24 == 0) return;
        trazo.setColor(color);
        invalidateSelf();
    }

    /**
     * La luz de un botón de marca pulsado (lote 1c): de 0, en reposo, a 1, pulsado del todo. El brillo y la
     * media luna se pintan otra vez encima con LUZ_PULSADO de su alfa por esa fracción: el brillo
     * pasa del 20 % al 32 % y la media luna del 40 % al 58 %. Los dos acaban por encima de la
     * mitad del botón, así que el naranja de abajo no cambia (ContrasteCristalTest). En los
     * demás cristales no hace nada.
     */
    void ponerLuz(float fraccion) {
        if (tipo != MARCA || luzPulsado == fraccion) return;
        luzPulsado = fraccion;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        caja.set(b);
        float r = radioPara(caja.height());
        forma.rewind();
        forma.addRoundRect(caja, r, r, Path.Direction.CW);

        // El borde va por dentro del canto: medio grosor hacia dentro.
        anillo(bordeForma, r, grosor / 2f);
        // El filo, en el mismo canto: medio dp, por fuera del borde claro.
        anillo(filoForma, r, grosorFilo / 2f);

        // El brillo de arriba: lo que queda de la forma al quitarle ella misma un grosor más
        // abajo (el «inset 0 1px 0» de la maqueta), así sigue las esquinas. La línea de abajo,
        // lo mismo hacia arriba.
        Path corrida = new Path(forma);
        corrida.offset(0, grosor);
        luzForma.rewind();
        luzForma.op(forma, corrida, Path.Op.DIFFERENCE);
        corrida.set(forma);
        corrida.offset(0, -grosor);
        luzBajaForma.rewind();
        luzBajaForma.op(forma, corrida, Path.Op.DIFFERENCE);

        prepararReflejo(r);
        prepararMarca();
        relleno.setShader(degradado());
        sombra = (tipo == LENTE || tipo == MARCA) && sombraColor >>> 24 != 0 && !caja.isEmpty()
                ? SombraCristal.de(r, sombraDifuminado, sombraBaja) : null;
    }

    // La forma metida hacia dentro: por donde pasa el centro de un trazo que no se sale del canto.
    private void anillo(@NonNull Path destino, float r, float dentro) {
        destino.rewind();
        destino.addRoundRect(new RectF(caja.left + dentro, caja.top + dentro, caja.right - dentro, caja.bottom - dentro),
                Math.max(0, r - dentro), Math.max(0, r - dentro), Path.Direction.CW);
    }

    // La media luna del reflejo de un botón de marca: la forma metida reflejoDentro, menos ella misma bajada
    // hasta el cuarto de arriba. Queda ancha arriba y en punta hacia los lados, y se apaga de
    // arriba abajo (llega transparente a su canto de abajo, que así no se ve). Acaba antes de
    // donde empieza el icono (CristalTest).
    private void prepararReflejo(float r) {
        reflejoForma.rewind();
        reflejo.setShader(null);
        if (reflejoColor >>> 24 == 0 || caja.isEmpty()) return;
        float baja = caja.height() * REFLEJO_ALTO - reflejoDentro;
        if (baja <= 0f) return;
        Path dentro = new Path();
        anillo(dentro, r, reflejoDentro);
        Path bajada = new Path(dentro);
        bajada.offset(0, baja);
        reflejoForma.op(dentro, bajada, Path.Op.DIFFERENCE);
        float arriba = caja.top + reflejoDentro;
        reflejo.setShader(new LinearGradient(0, arriba, 0, arriba + baja,
                reflejoColor, reflejoColor & 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    // El brillo radial y el borde en degradado de un botón de marca: dependen de la caja.
    private void prepararMarca() {
        if (tipo != MARCA || caja.isEmpty()) return;
        // Muy difuminado: del 20 % en su centro a nada en su radio, con un tramo intermedio al
        // 40 % de su alfa para que no se lea como un disco. Tres paradas, no más (DEC-070).
        float[] b = brilloMarca(caja.width(), caja.height());
        float cx = caja.left + b[0], cy = caja.top + b[1];
        int alfaMedio = Math.round((brilloRadialColor >>> 24) * 0.4f);
        int medio = (alfaMedio << 24) | (brilloRadialColor & 0x00FFFFFF);
        RadialGradient radial = new RadialGradient(cx, cy, b[2],
                new int[]{brilloRadialColor, medio, brilloRadialColor & 0x00FFFFFF},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
        // La elipse: el círculo de radio horizontal, aplastado en vertical sobre su centro.
        if (b[3] != b[2]) {
            Matrix aplastar = new Matrix();
            aplastar.setScale(1f, b[3] / b[2], cx, cy);
            radial.setLocalMatrix(aplastar);
        }
        brilloRadial.setShader(radial);
        trazo.setShader(new LinearGradient(0, caja.top, 0, caja.bottom, bordeAlto, bordeBajo,
                Shader.TileMode.CLAMP));
    }

    /**
     * Dónde va el brillo radial de un botón de marca y cuánto mide, en px dentro de su caja:
     * {centro x, centro y, semieje horizontal, semieje vertical}. En el «+» (redondo), un círculo
     * pequeño arriba a la izquierda; en un botón ancho, una elipse centrada de en torno al 60 %
     * de su ancho, con la misma altura y el mismo alto.
     */
    static float[] brilloMarca(float ancho, float alto) {
        float vertical = Math.max(1f, Math.min(ancho, alto) * BRILLO_RADIO);
        if (ancho > alto * ANCHO_DESDE) {
            return new float[]{ancho * 0.5f, alto * BRILLO_Y, Math.max(vertical, ancho * BRILLO_ANCHO), vertical};
        }
        return new float[]{ancho * BRILLO_X, alto * BRILLO_Y, vertical, vertical};
    }

    // El degradado del relleno sobre la caja: a 160° con tono medio, de lado a lado (oro) o
    // vertical (un botón de marca).
    @Nullable
    private Shader degradado() {
        if (caja.isEmpty() || (alto == bajo && (medio == 0 || medio == alto))) {
            relleno.setColor(alto);
            return null;
        }
        if (tipo == ORO || tipo == ORO_FILA) {
            return new LinearGradient(caja.left, 0, caja.right, 0, alto, bajo, Shader.TileMode.CLAMP);
        }
        if (tipo == MARCA) {
            return new LinearGradient(0, caja.top, 0, caja.bottom, alto, bajo, Shader.TileMode.CLAMP);
        }
        // Como en CSS: la línea pasa por el centro y mide |ancho·sen| + |alto·cos|.
        double rad = Math.toRadians(ANGULO);
        float sx = (float) Math.sin(rad), sy = (float) -Math.cos(rad);
        float largo = Math.abs(caja.width() * sx) + Math.abs(caja.height() * sy);
        float cx = caja.centerX(), cy = caja.centerY();
        float x0 = cx - sx * largo / 2f, y0 = cy - sy * largo / 2f;
        float x1 = cx + sx * largo / 2f, y1 = cy + sy * largo / 2f;
        return medio == 0
                ? new LinearGradient(x0, y0, x1, y1, alto, bajo, Shader.TileMode.CLAMP)
                : new LinearGradient(x0, y0, x1, y1, new int[]{alto, medio, bajo},
                new float[]{0f, MEDIO, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (caja.isEmpty() || alfa == 0) return;
        // A medias (la fila dorada que aparece, momento 25) se pinta en una capa: el dibujo
        // entero, con su borde y su brillo, y no solo el relleno.
        int capa = alfa < 255 ? canvas.saveLayerAlpha(caja, alfa) : -1;
        pintar(canvas);
        if (capa != -1) canvas.restoreToCount(capa);
    }

    private void pintar(@NonNull Canvas canvas) {
        if (sombra != null) {
            canvas.save();
            canvas.translate(caja.left, caja.top);
            sombra.pintar(canvas, caja.width(), caja.height(), sombraColor);
            canvas.restore();
        }
        if (tipo == MARCA) {
            pintarMarca(canvas);
            return;
        }
        if (surco.getColor() >>> 24 != 0) canvas.drawPath(forma, surco);
        canvas.drawPath(forma, relleno);
        if (trazo.getColor() != 0) canvas.drawPath(bordeForma, trazo);
        if (filo.getColor() != 0) canvas.drawPath(filoForma, filo);
        if (luz.getColor() != 0) canvas.drawPath(luzForma, luz);
        if (luzBaja.getColor() != 0) canvas.drawPath(luzBajaForma, luzBaja);
    }

    // Un botón de marca, de abajo arriba (la sombra ya está): relleno, brillo radial, borde y
    // reflejo. Pulsado, el brillo y el reflejo otra vez encima, con la fracción de luz que toque.
    private void pintarMarca(@NonNull Canvas canvas) {
        canvas.drawPath(forma, relleno);
        canvas.drawPath(forma, brilloRadial);
        canvas.drawPath(bordeForma, trazo);
        if (!reflejoForma.isEmpty()) canvas.drawPath(reflejoForma, reflejo);
        int alfa = Math.round(255 * LUZ_PULSADO * luzPulsado);
        if (alfa == 0) return;
        brilloRadial.setAlpha(alfa);
        canvas.drawPath(forma, brilloRadial);
        brilloRadial.setAlpha(255);
        if (!reflejoForma.isEmpty()) {
            reflejo.setAlpha(alfa);
            canvas.drawPath(reflejoForma, reflejo);
            reflejo.setAlpha(255);
        }
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        Rect b = getBounds();
        outline.setRoundRect(b, radioPara(b.height()));
        // Translúcido: sin sombra de elevación (se vería a través).
        outline.setAlpha(0f);
    }

    @Override
    public void setAlpha(int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        if (a == alfa) return;
        alfa = a;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alfa;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter filtro) {
        relleno.setColorFilter(filtro);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
