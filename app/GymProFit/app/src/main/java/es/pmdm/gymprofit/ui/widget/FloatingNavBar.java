package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// FloatingNavBar — barra de navegación flotante estilo FITIA / iOS "liquid glass".
// La barra es una cápsula OSCURA translúcida (deja ver tenue el contenido de
// detrás). Sobre el destino activo hay una BURBUJA DE CRISTAL circular que, en
// API 33+ (RuntimeShader AGSL), MAGNIFICA y REFRACTA los iconos que tiene debajo
// con aberración cromática en el borde (arcoíris) + brillo especular + aro
// luminoso — igual que la burbuja de Fitia. Al cambiar de pestaña la burbuja
// "viaja" con inercia y rebote jelly; al arrastrar persigue al dedo con imán a
// cada opción. El icono/etiqueta del activo va en color de marca (naranja).
// En API < 33 cae a una gota translúcida sencilla. Funciona en claro y oscuro.
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

    // Shader AGSL: lente de cristal. Dentro del radio de la burbuja magnifica el
    // contenido (refracción), separa los canales RGB en el borde (aberración
    // cromática = arcoíris), añade una escarcha leve, un realce de cuerpo, un brillo
    // especular arriba-izquierda y un aro luminoso. Fuera del radio deja el contenido
    // intacto. NO tiñe de color (el naranja solo va en el icono/texto activo).
    private static final String AGSL =
            "uniform shader content;\n" +
            "uniform float2 uCenter;\n" +
            "uniform float2 uRadius;\n" +
            "uniform half3 uRim;\n" +
            "uniform float uDark;\n" +                        // 1 = tema oscuro, 0 = claro
            "half4 main(float2 coord) {\n" +
            "  float2 d = coord - uCenter;\n" +
            "  float2 n = d / uRadius;\n" +
            "  float r = length(n);\n" +
            "  if (r >= 1.0) { return content.eval(coord); }\n" +
            "  float mag = 0.88 + 0.12 * r * r;\n" +           // refracción SUAVE (la label cabe dentro)
            "  float2 src = uCenter + d * mag;\n" +
            "  float rim = smoothstep(0.60, 1.0, r);\n" +
            "  float ca = rim * 11.0;\n" +                     // aberración cromática (arcoíris) en el borde
            "  float2 dir = d / max(length(d), 0.001);\n" +
            "  half4 c;\n" +
            "  c.r = content.eval(src - dir * ca).r;\n" +
            "  c.g = content.eval(src).g;\n" +
            "  c.b = content.eval(src + dir * ca).b;\n" +
            "  c.a = 1.0;\n" +
            "  c.rgb = mix(c.rgb, half3(1.0), 0.05 * uDark);\n" +          // escarcha (solo en oscuro)
            "  c.rgb += smoothstep(1.0, 0.0, r) * 0.09 * uDark;\n" +       // realce claro (solo en oscuro)
            "  c.rgb -= (1.0 - uDark) * smoothstep(1.0, 0.55, r) * 0.06;\n" + // en CLARO: cristal algo más oscuro hacia el borde
            // brillo especular (reflejo arriba-izquierda): fuerte en oscuro, suave en claro
            "  float2 sp = (coord - (uCenter - uRadius * float2(0.34, 0.48))) / uRadius;\n" +
            "  c.rgb += smoothstep(0.62, 0.0, length(sp)) * mix(0.16, 0.38, uDark);\n" +
            // aro del borde: en OSCURO blanco luminoso; en CLARO oscuro con tinte de marca
            // (para que la burbuja NO sea blanco-sobre-blanco).
            "  float ring = smoothstep(0.82, 0.99, r) * (1.0 - smoothstep(0.985, 1.0, r));\n" +
            "  half3 edgeDark = half3(0.20) + uRim * 0.22;\n" +   // aro tenue y cálido (no blanco llamativo)
            "  half3 edgeLight = uRim * 0.15 - half3(0.34);\n" +
            "  c.rgb += ring * mix(edgeLight, edgeDark, uDark);\n" +
            "  return c;\n" +
            "}\n";

    public interface OnTabSelectedListener { void onSelected(int index); }

    private final ImageView[] iconos = new ImageView[N];
    private final TextView[] labels = new TextView[N];
    private View burbuja;                 // gota de reserva (API < 33)
    private RuntimeShader lente;          // lente de cristal (API 33+)
    private boolean usarLente;
    private OnTabSelectedListener listener;

    private int activo = 0;
    private boolean arrastrando = false;
    private boolean movido = false;
    private float downX = 0f;
    private float dragAnchorCentro = -1f;
    private int touchSlop;
    private float objetivoCentro = -1f;
    private float actualCentro = -1f;
    private float estiramiento = 1f;
    private ValueAnimator ticker;
    private ValueAnimator viaje;

    private float radioCapsula;
    private int insetX;                   // padding horizontal interno: aparta los iconos
                                          // extremos del semicírculo de la cápsula para que
                                          // la burbuja quepa centrada sin rozar el borde.
    private int colorMarca, textoActivo, textoInactivo;
    private boolean esOscuro;

    public FloatingNavBar(Context c) { this(c, null); }
    public FloatingNavBar(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        touchSlop     = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        insetX        = (int) dp(12);
        colorMarca    = attr(com.google.android.material.R.attr.colorPrimary);
        int onSurface = attr(com.google.android.material.R.attr.colorOnSurface);
        textoActivo   = colorMarca;                 // el destino activo va en NARANJA (como Fitia en amarillo)
        textoInactivo = conAlfa(onSurface, 0xD8);
        esOscuro = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        // Barra: cápsula OSCURA (o clara en tema claro) translúcida. Al solaparse con el
        // contenido, la translucidez deja ver TENUE lo que hay detrás (como Fitia). Es
        // casi opaca para que la burbuja refracte iconos brillantes sobre fondo oscuro.
        int baseArriba, baseAbajo, borde;
        if (esOscuro) {
            baseArriba = conAlfa(0x24262B, 0xD8);   // gris muy oscuro translúcido (cresta)
            baseAbajo  = conAlfa(0x121316, 0xE6);   // casi negro (profundidad)
            borde      = conAlfa(0xFFFFFF, 0x30);
        } else {
            baseArriba = conAlfa(0xFFFFFF, 0xE0);   // blanco translúcido
            baseAbajo  = conAlfa(0xF0F1F4, 0xEC);
            borde      = conAlfa(0x000000, 0x1A);
        }
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ baseArriba, baseAbajo });
        bg.setStroke((int) dp(1), borde);
        setBackground(bg);
        setElevation(dp(10));
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radioCapsula);
            }
        });

        // ¿Soporta la lente de cristal? (RuntimeShader es API 33+)
        usarLente = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try { lente = new RuntimeShader(AGSL); }
            catch (Throwable t) { usarLente = false; }
        }

        // Gota de reserva (solo se ve en API < 33): vidrio escarchado translúcido.
        burbuja = new View(getContext());
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ conAlfa(0xFFFFFF, 0x4A), conAlfa(0xFFFFFF, 0x22) });
        glass.setCornerRadius(dp(30));
        glass.setStroke((int) dp(1), conAlfa(0xFFFFFF, 0x70));
        burbuja.setBackground(glass);
        burbuja.setVisibility(usarLente ? INVISIBLE : VISIBLE);
        addView(burbuja, new LayoutParams((int) dp(66), (int) dp(60), Gravity.CENTER_VERTICAL));

        // Fila de 5 destinos por encima
        LinearLayout fila = new LinearLayout(getContext());
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setWeightSum(N);
        fila.setPadding(insetX, 0, insetX, 0);   // aparta los iconos de los extremos redondeados
        fila.setClipToPadding(false);
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
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
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

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------
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
            viaje.setDuration(Math.max(300, Math.abs(hasta - desde) * 130L));
            viaje.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            viaje.addUpdateListener(a -> {
                float prev = actualCentro;
                actualCentro = (float) a.getAnimatedValue();
                estiramiento = clampF(1f + Math.abs(actualCentro - prev) / dp(22f), 1f, 1.22f);
                render();
            });
            viaje.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator an) { rebotar(activo); }
            });
            viaje.start();
        });
    }

    // Sincroniza la burbuja con el scroll del ViewPager2 (viaje continuo en swipe).
    public void followScroll(float pos) {
        int idx = clamp(Math.round(pos));
        activo = idx;
        if (getWidth() == 0) { post(() -> followScroll(pos)); return; }
        if (viaje != null) viaje.cancel();
        float celda = cellW();
        actualCentro = objetivoCentro = insetX + celda * pos + celda / 2f;
        estiramiento = 1f;
        render();
        resaltar(idx);
    }

    // Asienta la burbuja en la pestaña seleccionada (fin del desplazamiento).
    public void setActiveIndex(int index) {
        activo = clamp(index);
        resaltar(activo);
        post(() -> { colocar(activo); render(); });
    }

    public void setOnTabSelectedListener(OnTabSelectedListener l) { this.listener = l; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        radioCapsula = h / 2f;
        invalidateOutline();
        if (!arrastrando) { colocar(activo); render(); }
    }

    // ------------------------------------------------------------------
    // Interacción táctil (arrastre/tap)
    // ------------------------------------------------------------------
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                movido = false;
                if (viaje != null) viaje.cancel();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!movido && Math.abs(e.getX() - downX) > touchSlop) {
                    movido = true;
                    arrastrando = true;
                    dragAnchorCentro = actualCentro;
                    objetivoCentro = actualCentro;
                    iniciarTicker();
                }
                if (movido) {
                    objetivoCentro = objetivoGravedad(dragAnchorCentro + (e.getX() - downX));
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (movido) {
                    arrastrando = false;
                    pararTicker();
                    int destino = indiceEnCentro(objetivoCentro);
                    boolean cambia = destino != activo;
                    activo = destino;
                    resaltar(activo);
                    rebotar(activo);
                    if (cambia && listener != null) listener.onSelected(activo);
                } else {
                    int destino = indiceEnCentro(e.getX());
                    if (destino != activo) {
                        int from = activo;
                        setActiveFrom(from, destino);
                        if (listener != null) listener.onSelected(destino);
                    }
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && lente != null) {
            aplicarLente();
        } else {
            int w = ((LayoutParams) burbuja.getLayoutParams()).width;
            burbuja.setTranslationX(clampF(actualCentro - w / 2f, 0, getWidth() - w));
            burbuja.setScaleX(estiramiento);
            burbuja.setScaleY(1f / estiramiento);
        }
    }

    // Actualiza la lente de cristal (radios de la burbuja, ~círculo con jelly) y la
    // aplica como RenderEffect que refracta el contenido de la barra. Solo API 33+.
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void aplicarLente() {
        // Burbuja que ENGLOBA la opción activa (icono + etiqueta), centrada en la celda.
        // Ligeramente más ancha que alta (poco estirada) para contener la etiqueta sin
        // recortarla; el zoom suave del shader ayuda a que quepa. Como en Fitia.
        float cellW = cellW();
        float rx = cellW * 0.52f * estiramiento;
        float ry = getHeight() * 0.48f / estiramiento;
        // Centrada en la opción: se clampa a los CENTROS de las celdas extremas. Gracias
        // al inset interno, esos centros caen en la zona RECTA de la cápsula, así que la
        // burbuja queda centrada sobre el icono sin rozar el semicírculo del extremo.
        float cx = clampF(actualCentro, insetX + cellW / 2f, getWidth() - insetX - cellW / 2f);
        lente.setFloatUniform("uCenter", cx, getHeight() / 2f);
        lente.setFloatUniform("uRadius", rx, ry);
        // Tinte MUY leve del aro con el color de marca (el resto del cristal es neutro).
        lente.setFloatUniform("uRim",
                Color.red(colorMarca) / 255f, Color.green(colorMarca) / 255f, Color.blue(colorMarca) / 255f);
        lente.setFloatUniform("uDark", esOscuro ? 1f : 0f);
        setRenderEffect(RenderEffect.createRuntimeShaderEffect(lente, "content"));
    }

    private float cellW() { return (getWidth() - 2f * insetX) / N; }

    private float objetivoGravedad(float x) {
        float celda = cellW();
        int cercano = clamp(Math.round((x - insetX - celda / 2f) / celda));
        float centroCercano = insetX + celda * cercano + celda / 2f;
        float dist = Math.abs(x - centroCercano);
        float k = 0.35f + 0.5f * (1f - Math.min(1f, dist / (celda / 2f)));
        return clampF(x * (1f - k) + centroCercano * k, insetX + celda / 2f, getWidth() - insetX - celda / 2f);
    }

    private int indiceEnCentro(float centro) {
        if (getWidth() == 0) return activo;
        return clamp((int) ((centro - insetX) / cellW()));
    }

    private float centro(int i) { float c = cellW(); return insetX + c * i + c / 2f; }

    // Al llegar a la celda i: rebote de posición (overshoot) + jelly (aplasta/estira).
    private void rebotar(int i) {
        if (getWidth() == 0) { colocar(i); render(); return; }
        final float inicio = actualCentro;
        final float destino = centro(i);
        final float estiraInicial = estiramiento;
        final OvershootInterpolator pos = new OvershootInterpolator(2.2f);
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(480);
        a.addUpdateListener(v -> {
            float t = (float) v.getAnimatedValue();
            actualCentro = inicio + (destino - inicio) * pos.getInterpolation(t);
            float wobble = 0.20f * (float) Math.sin(t * Math.PI * 2.3) * (1f - t);
            estiramiento = (1f + wobble) + (estiraInicial - 1f) * Math.max(0f, 1f - t * 3f);
            render();
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator an) {
                estiramiento = 1f; render();
            }
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
