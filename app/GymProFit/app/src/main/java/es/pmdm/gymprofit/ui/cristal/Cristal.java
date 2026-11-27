package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
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
//   · BRILLO       solo el brillo de arriba, para ponerlo encima de un botón relleno.
//   · VIVO         el tinte que el cristal vivo pinta sobre su desenfoque (CristalVivo).
//   · VIVO_OPACO   la superficie del cristal vivo cuando no desenfoca (nivel 3).
//   · VIVO_GRUESO  el tinte de la hoja del «+», con más cuerpo que el de la barra.
//   · VIVO_ACCION  el tinte naranja del «+» de la barra, con una línea de reflejo en su cuarto
//                  de arriba, como una cápsula de cristal.
//   · VIVO_ACCION_OPACO  el «+» cuando no desenfoca (nivel 3): el degradado radial macizo.
//
// La sombra del cristal estático en claro no la pinta este dibujo sino su tarjeta
// (TarjetaCristal), que es quien puede dibujar fuera de su caja; la de la lente sí, y por eso
// el padre de la lente no recorta a sus hijos.
//
// Se declara en res/drawable con <drawable class=…> (bg_cristal*.xml), como BurbujaProfi, o se
// crea desde código (CristalVivo). Los colores son los gp_cristal_* de colors.xml.
// ============================================================
public class Cristal extends Drawable {

    public static final int TARJETA = 0, LENTE = 1, ORO = 2, BRILLO = 3, VIVO = 4, VIVO_OPACO = 5, VIVO_GRUESO = 6;
    public static final int VIVO_ACCION = 7, VIVO_ACCION_OPACO = 8;

    /** El ángulo del degradado del relleno, como en CSS: 0 hacia arriba, 90 hacia la derecha. */
    static final float ANGULO = 160f;
    /** Dónde cae el tono medio del relleno. */
    static final float MEDIO = 0.55f;
    /** Hasta dónde baja la línea de reflejo del «+»: su cuarto de arriba. */
    static final float REFLEJO_ALTO = 0.25f;
    // El centro del degradado radial del «+» opaco, en fracción de su caja.
    private static final float RADIAL_X = 0.35f, RADIAL_Y = 0.30f;

    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint luz = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint luzBaja = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint reflejo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path forma = new Path(), bordeForma = new Path(), luzForma = new Path();
    private final Path luzBajaForma = new Path(), filoForma = new Path(), reflejoForma = new Path();
    private final RectF caja = new RectF();

    private int tipo = TARJETA;
    // Radio de las esquinas en px; negativo, la mitad del alto (píldora).
    private float radio = -1f;
    private float grosor = 1f, grosorFilo = 1f;
    private int alto, medio, bajo;
    private boolean pulsado;
    // La sombra de la lente (solo en claro): sus medidas en px y su color.
    private float sombraDifuminado, sombraBaja;
    private int sombraColor;
    @Nullable private SombraCristal sombra;
    // El «+»: su reflejo (color y cuánto se mete del canto, en px) y el radio de su degradado opaco.
    private int reflejoColor;
    private float reflejoDentro, radialRadio;

    /** Para <drawable class=…>: los atributos llegan en inflate(). */
    public Cristal() {
        relleno.setStyle(Paint.Style.FILL);
        trazo.setStyle(Paint.Style.STROKE);
        luz.setStyle(Paint.Style.FILL);
        luzBaja.setStyle(Paint.Style.FILL);
        filo.setStyle(Paint.Style.STROKE);
        reflejo.setStyle(Paint.Style.STROKE);
        reflejo.setStrokeCap(Paint.Cap.ROUND);
        relleno.setDither(true);
    }

    /**
     * Desde código.
     *
     * @param tipo  uno de TARJETA … VIVO_GRUESO.
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
            case ORO:
                a = R.color.gp_cristal_oro_alto;
                b = R.color.gp_cristal_oro_bajo;
                m = 0;
                borde = brilloBajo = canto = 0;
                brillo = R.color.gp_cristal_oro_brillo;
                break;
            case BRILLO:
                a = m = b = 0;
                borde = brilloBajo = canto = 0;
                brillo = R.color.gp_boton_brillo;
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
            case VIVO_ACCION:
                a = R.color.gp_mas_tinte_alto;
                b = R.color.gp_mas_tinte_bajo;
                m = 0;
                borde = R.color.gp_mas_borde;
                brillo = R.color.gp_mas_brillo;
                brilloBajo = canto = 0;
                reflejoColor = ResourcesCompat.getColor(r, R.color.gp_mas_reflejo, theme);
                reflejoDentro = r.getDimension(R.dimen.barra_mas_reflejo_dentro);
                reflejo.setStrokeWidth(r.getDimension(R.dimen.barra_mas_reflejo_grosor));
                break;
            case VIVO_ACCION_OPACO:
                // El «+» de antes de ser cristal: degradado radial y el borde del cristal, sin brillo.
                a = R.color.gp_mas_claro;
                b = R.color.gp_mas_oscuro;
                m = 0;
                borde = R.color.gp_cristal_brillo;
                brillo = brilloBajo = canto = 0;
                radialRadio = r.getDimension(R.dimen.barra_mas_degradado);
                // Su borde, en píxeles enteros, como el de la forma que lo pintaba antes: con el
                // grosor en décimas (2,75 px a 440 ppp) el canto salía en dos líneas.
                grosor = r.getDimensionPixelSize(R.dimen.cristal_borde);
                trazo.setStrokeWidth(grosor);
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

    /** Cambia el radio de las esquinas (px; negativo, píldora): el «+», mientras se pulsa. */
    void ponerRadio(float px) {
        if (radio == px) return;
        radio = px;
        onBoundsChange(getBounds());
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
        relleno.setShader(degradado());
        sombra = tipo == LENTE && sombraColor >>> 24 != 0 && !caja.isEmpty()
                ? SombraCristal.de(r, sombraDifuminado, sombraBaja) : null;
    }

    // La forma metida hacia dentro: por donde pasa el centro de un trazo que no se sale del canto.
    private void anillo(@NonNull Path destino, float r, float dentro) {
        destino.rewind();
        destino.addRoundRect(new RectF(caja.left + dentro, caja.top + dentro, caja.right - dentro, caja.bottom - dentro),
                Math.max(0, r - dentro), Math.max(0, r - dentro), Path.Direction.CW);
    }

    // La línea de reflejo del «+»: el tramo del contorno, metido reflejoDentro, que queda en
    // el cuarto de arriba, apagándose hacia sus dos extremos (ahí llega transparente, así que
    // el corte no se ve).
    private void prepararReflejo(float r) {
        reflejoForma.rewind();
        reflejo.setShader(null);
        if (reflejoColor >>> 24 == 0 || caja.isEmpty()) return;
        float rr = Math.max(0f, r - reflejoDentro);
        // Lo que baja el corte desde lo alto de la línea, y la media cuerda de la línea a esa altura.
        float baja = Math.min(rr, Math.max(0f, caja.height() * REFLEJO_ALTO - reflejoDentro));
        float recto = Math.max(0f, caja.width() / 2f - reflejoDentro - rr);
        float media = recto + (float) Math.sqrt(Math.max(0f, rr * rr - (rr - baja) * (rr - baja)));
        if (media <= 0f) return;
        anillo(reflejoForma, r, reflejoDentro);
        int apagado = reflejoColor & 0x00FFFFFF;
        reflejo.setShader(new LinearGradient(caja.centerX() - media, 0, caja.centerX() + media, 0,
                new int[]{apagado, reflejoColor, apagado}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
    }

    // El degradado del relleno sobre la caja: a 160° con tono medio, de lado a lado (oro) o
    // radial (el «+» opaco).
    @Nullable
    private Shader degradado() {
        if (caja.isEmpty() || (alto == bajo && (medio == 0 || medio == alto))) {
            relleno.setColor(alto);
            return null;
        }
        if (tipo == ORO) {
            return new LinearGradient(caja.left, 0, caja.right, 0, alto, bajo, Shader.TileMode.CLAMP);
        }
        if (tipo == VIVO_ACCION_OPACO) {
            return new RadialGradient(caja.left + caja.width() * RADIAL_X, caja.top + caja.height() * RADIAL_Y,
                    Math.max(1f, radialRadio), alto, bajo, Shader.TileMode.CLAMP);
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
        if (caja.isEmpty()) return;
        if (tipo == BRILLO) {
            // Al pulsar, el botón cambia de forma (BotonExpresivo): el brillo se quita mientras.
            if (!pulsado) canvas.drawPath(luzForma, luz);
            return;
        }
        if (sombra != null) {
            canvas.save();
            canvas.translate(caja.left, caja.top);
            sombra.pintar(canvas, caja.width(), caja.height(), sombraColor);
            canvas.restore();
        }
        canvas.drawPath(forma, relleno);
        if (trazo.getColor() != 0) canvas.drawPath(bordeForma, trazo);
        if (filo.getColor() != 0) canvas.drawPath(filoForma, filo);
        if (luz.getColor() != 0) canvas.drawPath(luzForma, luz);
        if (luzBaja.getColor() != 0) canvas.drawPath(luzBajaForma, luzBaja);
        if (!reflejoForma.isEmpty()) {
            canvas.save();
            canvas.clipRect(caja.left, caja.top, caja.right, caja.top + caja.height() * REFLEJO_ALTO);
            canvas.drawPath(reflejoForma, reflejo);
            canvas.restore();
        }
    }

    @Override
    public boolean isStateful() {
        return tipo == BRILLO;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] estado) {
        if (tipo != BRILLO) return false;
        boolean ahora = false;
        for (int e : estado) if (e == android.R.attr.state_pressed) ahora = true;
        if (ahora == pulsado) return false;
        pulsado = ahora;
        invalidateSelf();
        return true;
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
        relleno.setAlpha(alpha);
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
