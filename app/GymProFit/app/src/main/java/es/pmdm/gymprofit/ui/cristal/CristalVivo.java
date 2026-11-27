package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import es.pmdm.gymprofit.R;

// ============================================================
// CristalVivo — el cristal con contenido que se mueve por debajo (GP-285, DEC-070).
//
// Un FrameLayout que pinta, detrás de sus hijos, lo que el LienzoCristal de la pantalla tiene
// en su mismo sitio, según el nivel del móvil (NivelCristal):
//
//   1 · desenfocado (σ 22 dp, saturación 180 %) y refractado en una banda junto al borde,
//       como una lente, con un RuntimeShader AGSL; encima, el tinte translúcido.
//   2 · desenfocado, sin refracción; encima, el tinte.
//   3 · nada de lo de detrás: una superficie opaca (al 98 % lo de detrás se entreveía, nítido).
//
// Con cristalGrueso el tinte tiene más cuerpo: la hoja del «+», que lleva texto secundario.
// Con cristalAccion es el «+» de la barra: el tinte es el naranja de la acción principal, con
// su reflejo, y en el nivel 3 queda como antes de ser cristal, macizo y sin sombra.
//
// En los tres lleva el borde y el brillo superior del cristal, y con cristalSombra una sombra
// suave por fuera. Lo que hay dentro (pestañas, textos) es legible en los tres niveles: sus
// colores pasan 4,5:1 sin contar con el desenfoque (ContrasteCristalTest).
//
// El coste está en no hacer nada por fotograma: el efecto (RenderEffect) se crea una vez por
// tamaño y nivel, nunca al dibujar, y la vista no se invalida al desplazarse el contenido: el
// nodo del lienzo ya apunta a lo que se mueve. Solo se vuelve a grabar si ella cambia de sitio.
// (La lente de la barra anterior creaba un RenderEffect por fotograma y por eso se retiró.)
//
// La encuentra sola: el LienzoCristal de su ventana con id lienzoCristal. Sin él, o dibujada
// en un lienzo de software, se queda en el nivel 3.
// ============================================================
public class CristalVivo extends FrameLayout implements ViewTreeObserver.OnPreDrawListener {

    /** Saturación de lo desenfocado: el «saturate(180%)» de la maqueta. */
    public static final float SATURACION = 1.8f;

    /**
     * El «radio» que hay que pedirle a RenderEffect para desenfocar con una desviación dada.
     *
     * <p>La maqueta (CSS, {@code blur(22px)}) da la desviación σ, en dp: aquí llega ya en px,
     * convertida por la densidad de la pantalla (cristal_desenfoque es un dimen en dp). Android
     * saca σ de su radio con σ = radio · 0,57735 + 0,5: esta es la cuenta al revés. El lienzo
     * se desenfoca a tamaño real, sin reducirlo antes, así que no hay nada más que compensar.
     *
     * @param sigmaPx la desviación del desenfoque, en px.
     */
    public static float radioDesenfoque(float sigmaPx) {
        return Math.max(1f, (sigmaPx - 0.5f) / 0.57735f);
    }
    // La sombra se pinta de una imagen pequeña estirada: es difusa y no se nota.
    private static final int ESCALA_SOMBRA = 4;
    // Fotogramas que espera a que el lienzo tenga algo grabado antes de dejar de repintarse.
    private static final int ESPERAS_MAXIMAS = 5;

    private final Cristal tinte, opaco;
    private final Path forma = new Path();
    private final RectF caja = new RectF();
    private final Paint pinturaSombra = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final int[] aqui = new int[2], alli = new int[2];
    private final float radio, sigma, sombraRadio, sombraBaja;
    private final boolean conSombra, accion;

    @Nullable private LienzoCristal lienzo;
    @NonNull private NivelCristal nivel = NivelCristal.OPACO;
    // Dónde está esta vista dentro del lienzo, en px.
    private int dx, dy;
    private int esperas;
    @Nullable private Bitmap sombra;
    // El nodo desenfocado; solo existe en Android 12 o más.
    @Nullable private final Desenfoque desenfoque;

    public CristalVivo(@NonNull Context context) {
        this(context, null);
    }

    public CristalVivo(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CristalVivo);
        radio = a.getDimension(R.styleable.CristalVivo_cristalRadio, -1f);
        conSombra = a.getBoolean(R.styleable.CristalVivo_cristalSombra, false);
        boolean grueso = a.getBoolean(R.styleable.CristalVivo_cristalGrueso, false);
        accion = a.getBoolean(R.styleable.CristalVivo_cristalAccion, false);
        a.recycle();
        sigma = getResources().getDimension(R.dimen.cristal_desenfoque);
        sombraRadio = getResources().getDimension(R.dimen.cristal_sombra_radio);
        sombraBaja = getResources().getDimension(R.dimen.cristal_sombra_baja);
        pinturaSombra.setColor(ContextCompat.getColor(context, R.color.gp_vivo_sombra));
        tinte = new Cristal(context, accion ? Cristal.VIVO_ACCION : grueso ? Cristal.VIVO_GRUESO : Cristal.VIVO, radio);
        opaco = new Cristal(context, accion ? Cristal.VIVO_ACCION_OPACO : Cristal.VIVO_OPACO, radio);
        desenfoque = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? new Desenfoque() : null;
        setWillNotDraw(false);
    }

    // ── El lienzo y el nivel ────────────────────────────────────────────────

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        View candidato = getRootView().findViewById(R.id.lienzoCristal);
        lienzo = candidato instanceof LienzoCristal ? (LienzoCristal) candidato : null;
        if (lienzo != null) {
            lienzo.apuntar(this);
            nivel = lienzo.nivel();
        } else {
            nivel = NivelCristal.OPACO;
        }
        prepararEfecto();
        getViewTreeObserver().addOnPreDrawListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        if (lienzo != null) lienzo.borrar(this);
        lienzo = null;
        super.onDetachedFromWindow();
    }

    /** El lienzo avisa: el nivel de este móvil ha cambiado. */
    void alCambiarNivel(@NonNull NivelCristal nuevo) {
        nivel = nuevo;
        prepararEfecto();
        invalidate();
    }

    /** El nivel con el que pinta (para pruebas y para el medidor). */
    @NonNull
    public NivelCristal nivel() {
        return nivel;
    }

    // Si se mueve (la cascada del arranque, la hoja que sube), lo de detrás es otro trozo.
    @Override
    public boolean onPreDraw() {
        if (lienzo == null || nivel == NivelCristal.OPACO || !isShown()) return true;
        getLocationInWindow(aqui);
        lienzo.getLocationInWindow(alli);
        int x = aqui[0] - alli[0], y = aqui[1] - alli[1];
        if (x != dx || y != dy) {
            dx = x;
            dy = y;
            invalidate();
        }
        return true;
    }

    // ── Medidas ─────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int antesW, int antesH) {
        super.onSizeChanged(w, h, antesW, antesH);
        caja.set(0, 0, w, h);
        float r = tinte.radioPara(h);
        forma.rewind();
        forma.addRoundRect(caja, r, r, Path.Direction.CW);
        tinte.setBounds(0, 0, w, h);
        opaco.setBounds(0, 0, w, h);
        sombra = conSombra && w > 0 && h > 0 ? crearSombra(w, h, r) : null;
        prepararEfecto();
    }

    /** El radio de sus esquinas ahora, en px. */
    public float radioEsquinas() {
        return tinte.radioPara(getHeight());
    }

    /**
     * Cambia el radio de las esquinas (px): el «+», que cambia de forma mientras se pulsa
     * (BotonExpresivo). Con refracción rehace el efecto, que lleva el radio dentro: pasa en los
     * pocos fotogramas de un toque, nunca al desplazar el contenido.
     */
    public void ponerRadioEsquinas(float px) {
        tinte.ponerRadio(px);
        opaco.ponerRadio(px);
        float r = radioEsquinas();
        forma.rewind();
        forma.addRoundRect(caja, r, r, Path.Direction.CW);
        if (nivel == NivelCristal.REFRACCION) prepararEfecto();
        invalidate();
    }

    // El efecto del nodo, una vez por tamaño y nivel.
    private void prepararEfecto() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || desenfoque == null) return;
        if (nivel == NivelCristal.OPACO || getWidth() == 0 || getHeight() == 0) return;
        desenfoque.preparar(getResources(), getWidth(), getHeight(), tinte.radioPara(getHeight()), sigma,
                nivel == NivelCristal.REFRACCION);
    }

    // La silueta difuminada, en gris de un canal y a un cuarto de tamaño.
    @NonNull
    private Bitmap crearSombra(int w, int h, float r) {
        float margen = sombraRadio * 2f;
        int bw = Math.max(1, Math.round((w + margen * 2f) / ESCALA_SOMBRA));
        int bh = Math.max(1, Math.round((h + margen * 2f) / ESCALA_SOMBRA));
        Bitmap b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setMaskFilter(new BlurMaskFilter(sombraRadio / ESCALA_SOMBRA, BlurMaskFilter.Blur.NORMAL));
        c.scale(1f / ESCALA_SOMBRA, 1f / ESCALA_SOMBRA);
        c.drawRoundRect(margen, margen, margen + w, margen + h, r, r, p);
        return b;
    }

    // ── Dibujo ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // Solo desenfoca en Android 12 o más, con un nivel que lo pida, en un lienzo acelerado
        // y con el lienzo de detrás ya grabado. Si no, la superficie opaca.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || nivel == NivelCristal.OPACO
                || desenfoque == null || lienzo == null || !canvas.isHardwareAccelerated()) {
            // El «+» opaco es el de antes de ser cristal, que no llevaba sombra.
            if (!accion) pintarSombra(canvas);
            opaco.draw(canvas);
            return;
        }
        pintarSombra(canvas);
        LienzoCristal.Captura captura = lienzo.captura();
        if (captura != null && captura.lista()) {
            desenfoque.pintar(canvas, captura, dx, dy, forma);
            tinte.draw(canvas);
            esperas = 0;
        } else {
            opaco.draw(canvas);
            // El lienzo aún no ha grabado su primer fotograma: se repinta cuando lo tenga. Con
            // tope, para no quedarse repintando si no llega a grabar.
            if (captura != null && esperas++ < ESPERAS_MAXIMAS) invalidate();
        }
    }

    // La sombra solo por fuera de la forma: a través del cristal se vería sucia. Antes de
    // Android 8 no se recorta: allí «todo menos la forma» parte de la caja de la vista, y la
    // sombra, que cae fuera de ella, salía cortada en recto (dos orejas grises en las esquinas
    // de la barra). Tampoco hace falta: en esas versiones el cristal es siempre el opaco
    // (nivel 3) y tapa lo que queda debajo.
    private void pintarSombra(@NonNull Canvas canvas) {
        if (sombra == null) return;
        float margen = sombraRadio * 2f;
        canvas.save();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(forma);
        }
        canvas.translate(-margen, -margen + sombraBaja);
        canvas.scale(ESCALA_SOMBRA, ESCALA_SOMBRA);
        canvas.drawBitmap(sombra, 0f, 0f, pinturaSombra);
        canvas.restore();
    }

    // ── Android 12 o más ────────────────────────────────────────────────────

    /** El trozo del lienzo que queda detrás, desenfocado. Clase aparte: API 31. */
    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Desenfoque {
        private final RenderNode nodo = new RenderNode("CristalVivo");

        // Se llama al cambiar de tamaño o de nivel, nunca al dibujar.
        void preparar(@NonNull Resources res, int w, int h, float radio, float sigmaPx, boolean refraccion) {
            nodo.setPosition(0, 0, w, h);
            float r = radioDesenfoque(sigmaPx);
            ColorMatrix saturacion = new ColorMatrix();
            saturacion.setSaturation(SATURACION);
            RenderEffect efecto = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(saturacion),
                    RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP));
            if (refraccion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                efecto = Refraccion.sobre(res, efecto, w, h, radio);
            }
            nodo.setRenderEffect(efecto);
        }

        void pintar(@NonNull Canvas canvas, @NonNull LienzoCristal.Captura captura, int dx, int dy,
                    @NonNull Path forma) {
            RecordingCanvas grabacion = nodo.beginRecording();
            try {
                grabacion.translate(-dx, -dy);
                grabacion.drawRenderNode(captura.nodo);
            } finally {
                nodo.endRecording();
            }
            canvas.save();
            canvas.clipPath(forma);
            canvas.drawRenderNode(nodo);
            canvas.restore();
        }
    }

    /** La lente del borde. Clase aparte: RuntimeShader es de API 33. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static final class Refraccion {

        // El programa está en res/raw/cristal_refraccion.agsl; la banda que refracta y lo que
        // desplaza como mucho, en dimens.xml.
        @NonNull
        static RenderEffect sobre(@NonNull Resources res, @NonNull RenderEffect dentro, int w, int h, float radio) {
            RuntimeShader lente = new RuntimeShader(programa(res));
            lente.setFloatUniform("tam", w, h);
            lente.setFloatUniform("radio", radio);
            lente.setFloatUniform("banda", res.getDimension(R.dimen.cristal_lente_banda));
            lente.setFloatUniform("fuerza", res.getDimension(R.dimen.cristal_lente_fuerza));
            return RenderEffect.createChainEffect(
                    RenderEffect.createRuntimeShaderEffect(lente, "contenido"), dentro);
        }

        // El texto del programa. Es un recurso de la app: si no se pudiera leer, la app está rota
        // y que se sepa.
        @NonNull
        private static String programa(@NonNull Resources res) {
            try (InputStream entrada = res.openRawResource(R.raw.cristal_refraccion);
                 ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
                byte[] trozo = new byte[2048];
                for (int n; (n = entrada.read(trozo)) > 0; ) salida.write(trozo, 0, n);
                return salida.toString("UTF-8");
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer cristal_refraccion.agsl", e);
            }
        }
    }
}
