package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
// FloatingNavBar — barra de navegación inferior flotante de vidrio naranja
// translúcido (estilo Fitia). Toda la barra es un cristal naranja semi-
// transparente; los 5 destinos muestran SIEMPRE icono + etiqueta centrada.
// Detrás del activo hay una "gota" de vidrio más brillante. Al ARRASTRAR, la
// gota persigue al dedo con INERCIA SUAVE (no salta), es atraída por gravedad
// hacia el centro de cada opción y se DEFORMA como líquido según cuánto se
// retrasa respecto al objetivo (gooey). Al soltar cae con rebote elástico
// sobre la opción más cercana y navega. Arquitectura solo-Activities.
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
    // Suavizado del seguimiento (fracción hacia el objetivo por frame): menor = más suave
    private static final float SUAVIZADO = 0.16f;

    public interface OnTabSelectedListener { void onSelected(int index); }

    private final ImageView[] iconos = new ImageView[N];
    private final TextView[] labels = new TextView[N];
    private View burbuja;
    private OnTabSelectedListener listener;

    private int activo = 0;
    private boolean arrastrando = false;
    private float objetivoCentro = -1f;  // a dónde quiere ir la gota (dedo + gravedad)
    private float actualCentro = -1f;     // dónde está realmente (persigue al objetivo)
    private ValueAnimator ticker;

    private int colorMarca, textoActivo, textoInactivo;

    public FloatingNavBar(Context c) { this(c, null); }
    public FloatingNavBar(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        colorMarca    = attr(com.google.android.material.R.attr.colorPrimary);
        textoActivo   = attr(com.google.android.material.R.attr.colorOnSurface);
        textoInactivo = conAlfa(textoActivo, 0xB8);

        // ── TODA la barra: vidrio naranja MUY TRANSLÚCIDO (deja ver el fondo),
        //    con brillo blanco superior de reflejo. Color saturado pero con
        //    poco alfa = efecto liquid glass ──
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ conAlfa(0xFFFFFF, 0x22), conAlfa(colorMarca, 0x24), conAlfa(colorMarca, 0x30) });
        bg.setCornerRadius(dp(32));
        bg.setStroke((int) dp(1), conAlfa(0xFFFFFF, 0x3E));
        setBackground(bg);
        setElevation(dp(10));

        // ── Gota de vidrio (destino activo): también translúcida, un punto más
        //    densa que la barra → vidrio sobre vidrio (liquid glass entre ambos) ──
        burbuja = new View(getContext());
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ conAlfa(0xFFFFFF, 0x3C), conAlfa(colorMarca, 0x59), conAlfa(colorMarca, 0x76) });
        glass.setCornerRadius(dp(28));
        glass.setStroke((int) dp(1), conAlfa(0xFFFFFF, 0x66));
        burbuja.setBackground(glass);
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
        post(() -> colocarBurbuja(activo));
    }

    // Coloca la gota en `desde` y la hace VIAJAR suavemente hasta `hasta`
    // (recorriendo el menú al cambiar de pantalla). La pantalla ya está
    // cargada: este viaje es solo visual. Reutiliza la persecución con inercia
    // + deformación del ticker, y al llegar asienta con rebote.
    public void setActiveFrom(int desde, int hasta) {
        activo = clamp(hasta);
        if (desde == hasta) { setActive(hasta); return; }
        post(() -> {
            if (getWidth() == 0) { colocarBurbuja(hasta); return; }
            colocarBurbuja(desde);          // arranca en la opción anterior
            iniciarTicker();
            float celda = getWidth() / (float) N;
            float destino = celda * clamp(hasta) + celda / 2f;
            ValueAnimator viaje = ValueAnimator.ofFloat(objetivoCentro, destino);
            viaje.setDuration(Math.max(300, Math.abs(hasta - desde) * 130L));
            viaje.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            viaje.addUpdateListener(a -> objetivoCentro = (float) a.getAnimatedValue());
            viaje.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator an) {
                    pararTicker();
                    snapBurbuja(activo);
                }
            });
            viaje.start();
        });
    }

    public void setOnTabSelectedListener(OnTabSelectedListener l) { this.listener = l; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (!arrastrando) colocarBurbuja(activo);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                arrastrando = true;
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
                // El destino es la opción bajo el DEDO al soltar (no la posición
                // de la gota, que en un toque aún no ha terminado de viajar).
                int destino = indiceEnCentro(objetivoGravedad(e.getX()));
                boolean cambia = destino != activo;
                activo = destino;
                resaltar(activo);
                if (cambia && listener != null) {
                    // Navegamos: NO animamos la gota aquí (esta pantalla se va);
                    // la burbuja "viaja" ya en la pantalla destino (setActiveFrom),
                    // evitando que el recorrido se pare y reinicie.
                    listener.onSelected(activo);
                } else {
                    snapBurbuja(activo); // misma opción: asienta con rebote
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    // Bucle por frames: la gota persigue el objetivo con inercia (suave) y se
    // deforma según lo que se retrasa (líquido). Corre solo durante el arrastre.
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
        actualCentro += retraso * SUAVIZADO;      // inercia suave hacia el objetivo

        ajustarAnchoBurbuja();
        int w = ((LayoutParams) burbuja.getLayoutParams()).width;
        burbuja.setTranslationX(clampF(actualCentro - w / 2f, 0, getWidth() - w));

        // Deformación gooey MUY SUTIL (apenas se ovala): estira poco en X según
        // cuánto se retrasa del objetivo.
        float estira = clampF(1f + Math.abs(retraso) / dp(170f), 1f, 1.13f);
        burbuja.setScaleX(1.04f * estira);
        burbuja.setScaleY(1.04f / estira);

        resaltar(indiceEnCentro(actualCentro));
    }

    // Objetivo con gravedad: mezcla la posición del dedo con el centro de la
    // opción más cercana (atracción más fuerte cuanto más cerca del centro).
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

    // Cae sobre la celda i con rebote elástico y recupera su forma redonda.
    private void snapBurbuja(int i) {
        if (getWidth() == 0) return;
        ajustarAnchoBurbuja();
        int w = ((LayoutParams) burbuja.getLayoutParams()).width;
        float celda = getWidth() / (float) N;
        float destino = celda * i + (celda - w) / 2f;
        actualCentro = celda * i + celda / 2f;
        objetivoCentro = actualCentro;
        burbuja.animate().translationX(destino).scaleX(1f).scaleY(1f)
                .setDuration(360).setInterpolator(new OvershootInterpolator(2.4f)).start();
    }

    private void colocarBurbuja(int i) {
        if (getWidth() == 0) return;
        ajustarAnchoBurbuja();
        int w = ((LayoutParams) burbuja.getLayoutParams()).width;
        float celda = getWidth() / (float) N;
        actualCentro = objetivoCentro = celda * i + celda / 2f;
        burbuja.setScaleX(1f); burbuja.setScaleY(1f);
        burbuja.setTranslationX(celda * i + (celda - w) / 2f);
    }

    private void ajustarAnchoBurbuja() {
        int ancho = (int) (getWidth() / (float) N - dp(10));
        LayoutParams lp = (LayoutParams) burbuja.getLayoutParams();
        if (lp.width != ancho && ancho > 0) { lp.width = ancho; burbuja.setLayoutParams(lp); }
    }

    // Resalta la opción `sel` (icono + etiqueta a contraste pleno); el resto
    // atenuado. TODAS las etiquetas SIEMPRE visibles y centradas bajo su icono.
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
