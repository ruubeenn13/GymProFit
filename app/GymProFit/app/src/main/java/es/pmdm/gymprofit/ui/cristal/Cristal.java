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
// 16 % → 5 % al 55 % → 9 % en oscuro), borde de 1 dp y un brillo de 1 dp por dentro del canto
// de arriba. No desenfoca nada: el fondo ya es difuso, así que es igual en todas las versiones
// de Android y no cuesta más que un degradado.
//
// Tipos:
//   · TARJETA     tarjetas, tiles y chips (el radio lo da cristalRadio; sin él, píldora).
//   · LENTE       la selección: la pestaña activa de la barra.
//   · ORO         la fila de un récord: degradado dorado de lado a lado y brillo.
//   · BRILLO      solo el brillo de arriba, para ponerlo encima de un botón relleno.
//   · VIVO        el tinte que el cristal vivo pinta sobre su desenfoque (CristalVivo).
//   · VIVO_OPACO  la superficie del cristal vivo cuando no desenfoca (nivel 3).
//
// Se declara en res/drawable con <drawable class=…> (bg_cristal*.xml), como BurbujaProfi, o se
// crea desde código (CristalVivo). Los colores son los gp_cristal_* de colors.xml.
// ============================================================
public class Cristal extends Drawable {

    public static final int TARJETA = 0, LENTE = 1, ORO = 2, BRILLO = 3, VIVO = 4, VIVO_OPACO = 5;

    /** El ángulo del degradado del relleno, como en CSS: 0 hacia arriba, 90 hacia la derecha. */
    static final float ANGULO = 160f;
    /** Dónde cae el tono medio del relleno. */
    static final float MEDIO = 0.55f;

    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint luz = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path forma = new Path(), bordeForma = new Path(), luzForma = new Path();
    private final RectF caja = new RectF();

    private int tipo = TARJETA;
    // Radio de las esquinas en px; negativo, la mitad del alto (píldora).
    private float radio = -1f;
    private float grosor = 1f;
    private int alto, medio, bajo;
    private boolean pulsado;

    /** Para <drawable class=…>: los atributos llegan en inflate(). */
    public Cristal() {
        relleno.setStyle(Paint.Style.FILL);
        trazo.setStyle(Paint.Style.STROKE);
        luz.setStyle(Paint.Style.FILL);
        relleno.setDither(true);
    }

    /**
     * Desde código.
     *
     * @param tipo  uno de TARJETA … VIVO_OPACO.
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
        trazo.setStrokeWidth(grosor);
        int borde = R.color.gp_cristal_borde, brillo = R.color.gp_cristal_brillo;
        int a = R.color.gp_cristal_alto, m = R.color.gp_cristal_medio, b = R.color.gp_cristal_bajo;
        switch (tipo) {
            case LENTE:
                a = m = b = R.color.gp_lente;
                borde = R.color.gp_lente_resplandor;
                brillo = R.color.gp_lente_brillo;
                break;
            case ORO:
                a = R.color.gp_cristal_oro_alto;
                b = R.color.gp_cristal_oro_bajo;
                m = 0;
                borde = 0;
                brillo = R.color.gp_cristal_oro_brillo;
                break;
            case BRILLO:
                a = m = b = 0;
                borde = 0;
                brillo = R.color.gp_boton_brillo;
                break;
            case VIVO:
                a = R.color.gp_vivo_alto;
                b = R.color.gp_vivo_bajo;
                m = 0;
                break;
            case VIVO_OPACO:
                a = R.color.gp_vivo_opaco_alto;
                b = R.color.gp_vivo_opaco_bajo;
                m = 0;
                break;
            default:
                break;
        }
        alto = a == 0 ? 0 : ResourcesCompat.getColor(r, a, theme);
        bajo = b == 0 ? 0 : ResourcesCompat.getColor(r, b, theme);
        // Sin tono medio propio, el degradado va recto del alto al bajo.
        medio = m == 0 ? 0 : ResourcesCompat.getColor(r, m, theme);
        trazo.setColor(borde == 0 ? 0 : ResourcesCompat.getColor(r, borde, theme));
        luz.setColor(ResourcesCompat.getColor(r, brillo, theme));
    }

    /** El radio de las esquinas para un alto dado: el pedido o, sin él, la mitad del alto. */
    float radioPara(float altoPx) {
        return radio < 0 ? altoPx / 2f : Math.min(radio, altoPx / 2f);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        caja.set(b);
        float r = radioPara(caja.height());
        forma.rewind();
        forma.addRoundRect(caja, r, r, Path.Direction.CW);

        // El borde va por dentro del canto: medio grosor hacia dentro.
        float m = grosor / 2f;
        RectF dentro = new RectF(caja.left + m, caja.top + m, caja.right - m, caja.bottom - m);
        bordeForma.rewind();
        bordeForma.addRoundRect(dentro, Math.max(0, r - m), Math.max(0, r - m), Path.Direction.CW);

        // El brillo de arriba: lo que queda de la forma al quitarle ella misma un grosor más
        // abajo (el «inset 0 1px 0» de la maqueta), así sigue las esquinas.
        Path bajada = new Path(forma);
        bajada.offset(0, grosor);
        luzForma.rewind();
        luzForma.op(forma, bajada, Path.Op.DIFFERENCE);

        relleno.setShader(degradado());
    }

    // El degradado del relleno sobre la caja: a 160° con tono medio, o de lado a lado (oro).
    @Nullable
    private Shader degradado() {
        if (caja.isEmpty() || alto == bajo) {
            relleno.setColor(alto);
            return null;
        }
        if (tipo == ORO) {
            return new LinearGradient(caja.left, 0, caja.right, 0, alto, bajo, Shader.TileMode.CLAMP);
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
        canvas.drawPath(forma, relleno);
        if (trazo.getColor() != 0) canvas.drawPath(bordeForma, trazo);
        canvas.drawPath(luzForma, luz);
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
