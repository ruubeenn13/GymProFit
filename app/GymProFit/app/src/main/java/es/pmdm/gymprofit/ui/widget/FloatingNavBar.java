package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// FloatingNavBar — barra de navegación flotante con efecto LIQUID GLASS real
// (estilo Fitia). Los 5 destinos muestran SIEMPRE icono + etiqueta; el activo
// va en color de marca (naranja). Sobre él hay una "lente" de vidrio que, en
// API 33+ (RuntimeShader AGSL), MAGNIFICA y REFRACTA los iconos de debajo con
// un borde cromático (arcoíris) y cuerpo escarchado; en versiones anteriores
// cae a una gota translúcida. Al ARRASTRAR, la lente persigue al dedo con
// inercia suave y gravedad magnética hacia cada opción; al soltar cae con
// rebote y navega. Al cambiar de pestaña la lente "viaja" por el menú.
// Arquitectura solo-Activities.
// ============================================================
public class FloatingNavBar extends FrameLayout {

    private static final int[] ICONOS = {
            R.drawable.ic_home, R.drawable.ic_rutinas, R.drawable.ic_ejercicios,
            R.drawable.ic_nutricion, R.drawable.ic_perfil
    };
    private static final int[] LABELS = {
            R.string.nav_home, R.string.nav_rutinas, R.string.nav_ejercicios,
            R.string.nav_nutricion, R.string.nav_perfil
    };
    private static final int N = 5;
    private static final float SUAVIZADO = 0.16f;

    // Shader AGSL: lente de vidrio (refracción + aberración cromática + escarcha).
    private static final String AGSL =
            "uniform shader content;\n" +
            "uniform float2 uCenter;\n" +
            "uniform float2 uRadius;\n" +
            "half4 main(float2 coord) {\n" +
            "  float2 d = coord - uCenter;\n" +
            "  float2 n = d / uRadius;\n" +
            "  float r = length(n);\n" +
            "  if (r >= 1.0) { return content.eval(coord); }\n" +
            "  float mag = 0.74 + 0.26 * r;\n" +               // refracción: magnifica hacia el centro
            "  float2 src = uCenter + d * mag;\n" +
            "  float rim = smoothstep(0.72, 1.0, r);\n" +
            "  float ca = rim * 9.0;\n" +                      // aberración cromática en el borde
            "  float2 dir = d / max(length(d), 0.001);\n" +
            "  half4 c;\n" +
            "  c.r = content.eval(src - dir * ca).r;\n" +
            "  c.g = content.eval(src).g;\n" +
            "  c.b = content.eval(src + dir * ca).b;\n" +
            "  c.a = 1.0;\n" +
            "  c.rgb = mix(c.rgb, half3(1.0), 0.16);\n" +      // cuerpo escarchado (blanco translúcido)
            "  c.rgb += rim * half3(0.22, 0.10, 0.28);\n" +    // tinte cromático del borde (dispersión)
            "  float ring = smoothstep(0.90, 1.0, r);\n" +
            "  c.rgb += ring * 0.32;\n" +                      // aro brillante del cristal
            "  return c;\n" +
            "}\n";

    public interface OnTabSelectedListener { void onSelected(int index); }

    private final ImageView[] iconos = new ImageView[N];
    private final TextView[] labels = new TextView[N];
    private View burbuja;                 // gota de reserva (API < 33)
    private RuntimeShader lente;          // lente de vidrio (API 33+)
    private boolean usarLente;
    private OnTabSelectedListener listener;

    private int activo = 0;
    private boolean arrastrando = false;
    private float objetivoCentro = -1f;
    private float actualCentro = -1f;
    private float estiramiento = 1f;      // deformación líquida (1 = redonda)
    private ValueAnimator ticker;
    private ValueAnimator viaje;

    private int colorMarca, textoActivo, textoInactivo;

    public FloatingNavBar(Context c) { this(c, null); }
    public FloatingNavBar(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        colorMarca    = attr(com.google.android.material.R.attr.colorPrimary);
        int onSurface = attr(com.google.android.material.R.attr.colorOnSurface);
        textoActivo   = colorMarca;                 // el destino activo va en NARANJA (como Fitia en amarillo)
        textoInactivo = conAlfa(onSurface, 0xC2);

        // Barra: vidrio naranja muy translúcido con reflejo blanco superior
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ conAlfa(0xFFFFFF, 0x1E), conAlfa(colorMarca, 0x22), conAlfa(colorMarca, 0x2E) });
        bg.setCornerRadius(dp(32));
        bg.setStroke((int) dp(1), conAlfa(0xFFFFFF, 0x38));
        setBackground(bg);
        setElevation(dp(10));

        // ¿Soporta la lente de vidrio? (RuntimeShader es API 33+)
        usarLente = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        if (usarLente) {
            try { lente = new RuntimeShader(AGSL); }
            catch (Throwable t) { usarLente = false; }
        }

        // Gota de reserva (solo se ve en API < 33): vidrio escarchado
        burbuja = new View(getContext());
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ conAlfa(0xFFFFFF, 0x4A), conAlfa(0xFFFFFF, 0x1E), conAlfa(0xFFFFFF, 0x30) });
        glass.setCornerRadius(dp(28));
        glass.setStroke((int) dp(1), conAlfa(0xFFFFFF, 0x66));
        burbuja.setBackground(glass);
        burbuja.setVisibility(usarLente ? INVISIBLE : VISIBLE);
        addView(burbuja, new LayoutParams((int) dp(64), (int) dp(58), Gravity.CENTER_VERTICAL));

        // Fila de 5 destinos por encima
        LinearLayout fila = new LinearLayout(getContext());
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setWeightSum(N);
        addView(fila, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        for (int i = 0; i < N; i++) {
            LinearLayout celda = new LinearLayout(getContext());
            celda.setOrientation(LinearLayout.VERTICAL);
            celda.setGravity(Gravity.CENTER);
            celda.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

            ImageView icono = new ImageView(getContext());
            icono.setImageResource(ICONOS[i]);
            icono.setLayoutParams(new LinearLayout.LayoutParams((int) dp(24), (int) dp(24)));
            iconos[i] = icono;
            celda.addView(icono);

            TextView label = new TextView(getContext());
            label.setText(LABELS[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            label.setLetterSpacing(0.02f);
            label.setPadding(0, (int) dp(3), 0, 0);
            label.setSingleLine(true);
            label.setIncludeFontPadding(false);
            label.setGravity(Gravity.CENTER);
            android.graphics.Typeface tf = ResourcesCompat.getFont(getContext(), R.font.barlow_condensed);
            label.setTypeface(tf != null ? tf : android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            labels[i] = label;
            celda.addView(label);

            fila.addView(celda);
        }

        resaltar(0);
    }

    public void setActive(int index) {
        activo = clamp(index);
        resaltar(activo);
        post(() -> { colocar(activo); render(); });
    }

    // Coloca la lente en `desde` y la hace viajar suavemente hasta `hasta`.
    public void setActiveFrom(int desde, int hasta) {
        activo = clamp(hasta);
        if (desde == hasta) { setActive(hasta); return; }
        post(() -> {
            if (getWidth() == 0) { colocar(hasta); render(); return; }
            colocar(desde);
            resaltar(hasta);
            float destino = centro(hasta);
            if (viaje != null) viaje.cancel();
            viaje = ValueAnimator.ofFloat(actualCentro, destino);
            viaje.setDuration(Math.max(320, Math.abs(hasta - desde) * 140L));
            viaje.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            viaje.addUpdateListener(a -> {
                float prev = actualCentro;
                actualCentro = (float) a.getAnimatedValue();
                estiramiento = clampF(1f + Math.abs(actualCentro - prev) / dp(26f), 1f, 1.13f);
                render();
            });
            viaje.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator an) { rebotar(activo); }
            });
            viaje.start();
        });
    }

    public void setOnTabSelectedListener(OnTabSelectedListener l) { this.listener = l; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (!arrastrando) { colocar(activo); render(); }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                arrastrando = true;
                if (viaje != null) viaje.cancel();
                objetivoCentro = objetivoGravedad(e.getX());
                iniciarTicker();
                return true;
            case MotionEvent.ACTION_MOVE:
                objetivoCentro = objetivoGravedad(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                arrastrando = false;
                pararTicker();
                int destino = indiceEnCentro(objetivoGravedad(e.getX()));
                boolean cambia = destino != activo;
                activo = destino;
                resaltar(activo);
                if (cambia && listener != null) {
                    listener.onSelected(activo);   // navega; la lente "viaja" en la pantalla destino
                } else {
                    rebotar(activo);               // misma opción: asienta con rebote
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    // Bucle por frames: la lente persigue el objetivo con inercia y se deforma
    // según lo que se retrasa (líquido). Corre solo durante el arrastre.
    private void iniciarTicker() {
        if (ticker != null) ticker.cancel();
        ticker = ValueAnimator.ofFloat(0f, 1f);
        ticker.setDuration(600000);
        ticker.setRepeatCount(ValueAnimator.INFINITE);
        ticker.addUpdateListener(a -> frame());
        ticker.start();
    }

    private void pararTicker() { if (ticker != null) { ticker.cancel(); ticker = null; } }

    private void frame() {
        if (getWidth() == 0 || objetivoCentro < 0) return;
        if (actualCentro < 0) actualCentro = objetivoCentro;
        float retraso = objetivoCentro - actualCentro;
        actualCentro += retraso * SUAVIZADO;
        estiramiento = clampF(1f + Math.abs(retraso) / dp(170f), 1f, 1.13f);
        render();
        resaltar(indiceEnCentro(actualCentro));
    }

    // Dibuja la lente/gota en su posición y deformación actuales.
    private void render() {
        if (getWidth() == 0 || actualCentro < 0) return;
        if (usarLente && lente != null) {
            float rx = (getWidth() / (float) N) * 0.60f * estiramiento;
            float ry = getHeight() * 0.40f / estiramiento;
            float cx = clampF(actualCentro, rx, getWidth() - rx);
            lente.setFloatUniform("uCenter", cx, getHeight() / 2f);
            lente.setFloatUniform("uRadius", rx, ry);
            setRenderEffect(RenderEffect.createRuntimeShaderEffect(lente, "content"));
        } else {
            int w = ((LayoutParams) burbuja.getLayoutParams()).width;
            burbuja.setTranslationX(clampF(actualCentro - w / 2f, 0, getWidth() - w));
            burbuja.setScaleX(estiramiento);
            burbuja.setScaleY(1f / estiramiento);
        }
    }

    private float objetivoGravedad(float x) {
        float celda = getWidth() / (float) N;
        int cercano = clamp(Math.round((x - celda / 2f) / celda));
        float centroCercano = celda * cercano + celda / 2f;
        float dist = Math.abs(x - centroCercano);
        float k = 0.35f + 0.5f * (1f - Math.min(1f, dist / (celda / 2f)));
        return clampF(x * (1f - k) + centroCercano * k, celda / 2f, getWidth() - celda / 2f);
    }

    private int indiceEnCentro(float centro) {
        if (getWidth() == 0) return activo;
        return clamp((int) (centro / (getWidth() / (float) N)));
    }

    private float centro(int i) { return (getWidth() / (float) N) * i + (getWidth() / (float) N) / 2f; }

    // Asienta la lente en la celda i con rebote elástico recuperando su forma.
    private void rebotar(int i) {
        if (getWidth() == 0) { colocar(i); render(); return; }
        float destino = centro(i);
        ValueAnimator a = ValueAnimator.ofFloat(actualCentro, destino);
        a.setDuration(340);
        a.setInterpolator(new OvershootInterpolator(2.2f));
        a.addUpdateListener(v -> {
            actualCentro = (float) v.getAnimatedValue();
            estiramiento += (1f - estiramiento) * 0.25f;
            render();
        });
        a.start();
    }

    private void colocar(int i) {
        if (getWidth() == 0) return;
        actualCentro = objetivoCentro = centro(i);
        estiramiento = 1f;
    }

    // Resalta la opción `sel` (icono + etiqueta en naranja); el resto atenuado.
    private void resaltar(int sel) {
        for (int i = 0; i < N; i++) {
            int color = i == sel ? textoActivo : textoInactivo;
            ImageViewCompat.setImageTintList(iconos[i], ColorStateList.valueOf(color));
            labels[i].setTextColor(color);
            labels[i].setVisibility(VISIBLE);
        }
    }

    private float clampF(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private int clamp(int i) { return Math.max(0, Math.min(N - 1, i)); }
    private int conAlfa(int rgb, int alfa) { return (rgb & 0x00FFFFFF) | (alfa << 24); }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    private int attr(int attrRes) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrRes, tv, true)) return tv.data;
        return Color.GRAY;
    }
}
