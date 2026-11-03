package es.pmdm.gymprofit.ui.widget;

import android.app.Activity;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.imageview.ShapeableImageView;

import es.pmdm.gymprofit.R;

// ============================================================
// FocoVisible — el foco del teclado o del DPAD se ve igual en toda la app (GP-227): un anillo de
// 2 dp, POR FUERA de lo que tiene el foco y separado 2 dp de su borde. Pulsado, la capa del 10 %
// la ponen los fondos (color/tarjeta_capa).
//
// Una sola regla para todo lo pulsable, y no un fondo por pieza: los botones de Material solo
// cambiaban su capa de estado (1,13 a 1,19:1) y lo demás llevaba el gris del sistema (revisor,
// GP-228). Y por fuera porque por dentro no sirve: pisaba el texto de las filas sin aire, quedaba a
// 1,4:1 dentro de un botón relleno y no se distinguía del borde de una tarjeta oscura. Por fuera
// lo que tiene al lado es lo que hay detrás de la vista, así que su color es el colorOnSurface
// del padre (dentro de la tarjeta protagonista, gp_on_hero; fuera, gp_on_surface).
//
// El anillo va en la capa de encima de la ventana (no lo recorta ningún padre) y sigue a la vista
// en cada fotograma mientras tiene el foco (se desplaza con su lista). No llevan anillo:
//   - lo que se desplaza (scrolls, listas): no es un control, el foco se ve en lo que tiene dentro;
//   - los campos de texto: su borde de foco, el de la app;
//   - lo marcado con R.id.tag_anillo_propio (la pestaña de la barra, en su píldora, y las opciones
//     del selector, que dibuja el suyo).
// Un icono suelto sin relleno (ImageView o ImageButton con un fondo que solo es la capa de pulsado:
// «Volver», papeleras, el calendario de un campo) no tiene texto que pisar, y por fuera el anillo se
// salía de la pantalla o cruzaba el borde de su campo (revisor, tercera pasada): ahí va por dentro,
// 2 dp hacia dentro de su borde. Con relleno (el «+» de la barra) va por fuera: por dentro quedaba a
// 2,3:1 sobre el naranja (cuarta pasada). El avatar (ShapeableImageView, sobre una foto), por fuera.
// Al ganar el foco, lo enfocado se desplaza a la vista con el anillo entero: un carrusel a sangre lo
// dejaba pegado al borde de la pantalla, con un lado del anillo fuera. Se pide también tras el
// siguiente layout: con el teclado abierto, el scroll aplazaba su desplazamiento y la petición
// llegaba antes (quinta pasada). Y el anillo se recorta a lo que se ve de las listas que lo
// contienen, solo en el eje en que se desplaza cada una: alrededor de una fila medio escondida se
// pintaba encima de la cabecera (cuarta pasada), y recortar también el otro eje dejaba sin arriba ni
// abajo las tarjetas de los carruseles (quinta pasada).
// En modo táctil no hay anillo: al tocar la pantalla se quita.
// ============================================================
public final class FocoVisible implements ViewTreeObserver.OnGlobalFocusChangeListener,
        ViewTreeObserver.OnTouchModeChangeListener, ViewTreeObserver.OnPreDrawListener {

    /** Un botón cuadrado de hasta este lado (dp) es redondo: el anillo es un círculo. */
    private static final int LADO_REDONDO_DP = 64;
    /** Lo que se separa el anillo del borde de la vista (dp). */
    private static final int HUECO_DP = 2;

    private final ViewGroup decor;
    private final GradientDrawable anillo = new GradientDrawable();
    private final Recortado capa = new Recortado(anillo);
    private final android.graphics.Rect enDecor = new android.graphics.Rect();
    private final android.graphics.Rect donde = new android.graphics.Rect();
    @Nullable private View conAnillo;

    private FocoVisible(ViewGroup decor) {
        this.decor = decor;
        anillo.setColor(0);
    }

    /** Escucha el foco de la ventana de la pantalla (una vez por ventana). */
    public static void instalar(@NonNull Activity a) {
        View d = a.getWindow().getDecorView();
        if (!(d instanceof ViewGroup) || d.getTag(R.id.tag_foco_tarjeta) != null) return;
        FocoVisible f = new FocoVisible((ViewGroup) d);
        d.setTag(R.id.tag_foco_tarjeta, f);
        d.getViewTreeObserver().addOnGlobalFocusChangeListener(f);
        d.getViewTreeObserver().addOnTouchModeChangeListener(f);
    }

    @Override
    public void onGlobalFocusChanged(@Nullable View antes, @Nullable View ahora) {
        quitar();
        if (ahora == null || ahora.isInTouchMode()) return;
        // Un carrusel no es una parada: con algo enfocable dentro, deja de serlo y el foco pasa a lo
        // primero que tiene (HorizontalScrollView se hace enfocable en su constructor; el tabulador se
        // paraba en él sin enseñar nada, revisor, GP-228).
        if (ahora instanceof HorizontalScrollView) {
            java.util.ArrayList<View> dentro = new java.util.ArrayList<>();
            ((ViewGroup) ahora).addFocusables(dentro, View.FOCUS_FORWARD);
            dentro.remove(ahora);
            if (!dentro.isEmpty()) {
                ahora.setFocusable(false);
                dentro.get(0).requestFocus();
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ahora.setDefaultFocusHighlightEnabled(false);
        if (!llevaAnillo(ahora)) return;
        conAnillo = ahora;
        int ancho = ahora.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco);
        anillo.setStroke(ancho, colorAlrededor(ahora));
        donde.setEmpty();
        capa.setBounds(0, 0, decor.getWidth(), decor.getHeight());
        decor.getOverlay().add(capa);
        decor.getViewTreeObserver().addOnPreDrawListener(this);
        colocar();
        // Que se vea entera con su anillo (por dentro, la vista entera): el scroll que la contiene se
        // desplaza lo que haga falta, ahora y otra vez tras el siguiente layout.
        final View enfocada = ahora;
        mostrar(enfocada);
        enfocada.post(() -> {
            if (enfocada == conAnillo) mostrar(enfocada);
        });
    }

    @Override
    public void onTouchModeChanged(boolean tactil) {
        if (tactil) quitar();
    }

    /** Recoloca el anillo en cada fotograma: la vista se desplaza con su lista. */
    @Override
    public boolean onPreDraw() {
        if (conAnillo == null) return true;
        if (!conAnillo.isAttachedToWindow() || !conAnillo.isShown()) {
            quitar();
            return true;
        }
        colocar();
        return true;
    }

    /** Si una vista lleva el anillo (lógica sin dibujo). */
    static boolean llevaAnillo(@NonNull View v) {
        return !(v instanceof ScrollView || v instanceof NestedScrollView || v instanceof HorizontalScrollView
                || v instanceof RecyclerView || v instanceof EditText
                || v.getTag(R.id.tag_anillo_propio) != null);
    }

    private void quitar() {
        if (conAnillo == null) return;
        decor.getOverlay().remove(capa);
        decor.getViewTreeObserver().removeOnPreDrawListener(this);
        conAnillo = null;
    }

    /** Si el anillo va por dentro: un icono suelto sin relleno, que no tiene texto que pisar. */
    static boolean porDentro(@NonNull View v) {
        return v instanceof ImageView && !(v instanceof ShapeableImageView) && sinRelleno(v.getBackground());
    }

    // Un fondo que no pinta nada en reposo: ninguno, o una capa de pulsado sin más capas que su máscara.
    private static boolean sinRelleno(@Nullable android.graphics.drawable.Drawable fondo) {
        if (fondo == null) return true;
        if (!(fondo instanceof android.graphics.drawable.RippleDrawable)) return false;
        android.graphics.drawable.RippleDrawable r = (android.graphics.drawable.RippleDrawable) fondo;
        for (int i = 0; i < r.getNumberOfLayers(); i++) {
            if (r.getId(i) != android.R.id.mask) return false;
        }
        return true;
    }

    private static void mostrar(View v) {
        int m = Math.max(0, fuera(v));
        v.requestRectangleOnScreen(new android.graphics.Rect(-m, -m, v.getWidth() + m, v.getHeight() + m), true);
        mostrarEnCarrusel(v, m);
    }

    // HorizontalScrollView no se desplaza más allá del borde de su contenido aunque tenga relleno: la
    // última tarjeta de un carrusel a sangre se quedaba pegada al borde de la pantalla. Se desplaza a
    // mano lo que falte para ver el anillo entero (scrollBy sí llega hasta el relleno).
    private static void mostrarEnCarrusel(View v, int fuera) {
        for (ViewParent p = v.getParent(); p instanceof View; p = p.getParent()) {
            if (!(p instanceof HorizontalScrollView)) continue;
            HorizontalScrollView h = (HorizontalScrollView) p;
            android.graphics.Rect r = new android.graphics.Rect(0, 0, v.getWidth(), v.getHeight());
            h.offsetDescendantRectToMyCoords(v, r);
            int izq = h.getScrollX(), der = izq + h.getWidth();
            if (r.right + fuera > der) h.scrollBy(r.right + fuera - der, 0);
            else if (r.left - fuera < izq) h.scrollBy(r.left - fuera - izq, 0);
            return;
        }
    }

    // Lo que el anillo sale del borde de la vista: hueco y trazo por fuera; por dentro, hacia dentro.
    private static int fuera(View v) {
        float d = v.getResources().getDisplayMetrics().density;
        int ancho = v.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco);
        return porDentro(v) ? -Math.round(HUECO_DP * d) : Math.round(HUECO_DP * d) + ancho;
    }

    // El anillo alrededor de la vista: separado HUECO_DP de su borde y con su forma.
    private void colocar() {
        View v = conAnillo;
        if (v == null) return;
        float d = v.getResources().getDisplayMetrics().density;
        int fuera = fuera(v);
        // Sin transformaciones: una vista girada (la flecha de «Periodo anterior», rotation 180)
        // daba con getLocationInWindow su esquina opuesta y el anillo salía desplazado su tamaño.
        enDecor.set(0, 0, v.getWidth(), v.getHeight());
        try {
            decor.offsetDescendantRectToMyCoords(v, enDecor);
        } catch (IllegalArgumentException e) {
            quitar();   // ya no cuelga de esta ventana: no hay dónde ponerlo
            return;
        }
        int x = enDecor.left, y = enDecor.top;
        capa.recortarA(visible(v));
        // Solo si se ha movido: invalidar en cada fotograma redibujaría la ventana sin parar.
        if (donde.left == x - fuera && donde.top == y - fuera && donde.width() == v.getWidth() + 2 * fuera
                && donde.height() == v.getHeight() + 2 * fuera) return;
        donde.set(x - fuera, y - fuera, x + v.getWidth() + fuera, y + v.getHeight() + fuera);
        anillo.setBounds(donde);
        anillo.setCornerRadius(Math.max(0f, radio(v, d) + fuera));
        decor.invalidate();
    }

    // El radio del borde de la vista: el de su tarjeta o botón, un círculo si es un botón redondo,
    // y el de los chips si no.
    private static float radio(View v, float d) {
        int w = v.getWidth(), h = v.getHeight();
        if (v instanceof MaterialCardView) return ((MaterialCardView) v).getRadius();
        if (v instanceof MaterialButton) {
            float r = ((MaterialButton) v).getShapeAppearanceModel().getTopLeftCornerSize()
                    .getCornerSize(new RectF(0, 0, w, h));
            return Math.min(r, Math.min(w, h) / 2f);
        }
        boolean cuadrado = Math.abs(w - h) <= 4 * d;
        if (cuadrado && w <= LADO_REDONDO_DP * d) return Math.min(w, h) / 2f;
        return Math.min(v.getResources().getDimension(R.dimen.radio_chip), Math.min(w, h) / 2f);
    }

    // Lo que se ve de la vista en la ventana (en coordenadas de la ventana): cada lista o scroll que la
    // contiene recorta solo en el eje en que se desplaza. Sin ninguno, la ventana entera.
    private android.graphics.Rect visible(View v) {
        android.graphics.Rect r = new android.graphics.Rect(0, 0, decor.getWidth(), decor.getHeight());
        for (ViewParent p = v.getParent(); p instanceof View && p != decor; p = p.getParent()) {
            View a = (View) p;
            int eje = eje(a);
            if (eje == 0) continue;
            android.graphics.Rect suyo = new android.graphics.Rect(a.getScrollX(), a.getScrollY(),
                    a.getScrollX() + a.getWidth(), a.getScrollY() + a.getHeight());
            try {
                decor.offsetDescendantRectToMyCoords(a, suyo);
            } catch (IllegalArgumentException e) {
                continue;
            }
            int[] z = recortar(new int[]{ r.left, r.top, r.right, r.bottom },
                    new int[]{ suyo.left, suyo.top, suyo.right, suyo.bottom }, eje);
            r.set(z[0], z[1], z[2], z[3]);
        }
        return r;
    }

    static final int EJE_X = 1, EJE_Y = 2;

    /**
     * Recorta la zona {@code r} (izquierda, arriba, derecha, abajo) a la de un contenedor, solo en el
     * eje en que este se desplaza: un carrusel recorta los lados, una lista vertical arriba y abajo.
     * Sin solaparse, una zona vacía (todo a 0).
     */
    static int[] recortar(int[] r, int[] suyo, int eje) {
        int[] z = r.clone();
        if (eje == EJE_X) {
            z[0] = Math.max(z[0], suyo[0]);
            z[2] = Math.min(z[2], suyo[2]);
        } else if (eje == EJE_Y) {
            z[1] = Math.max(z[1], suyo[1]);
            z[3] = Math.min(z[3], suyo[3]);
        }
        return z[0] < z[2] && z[1] < z[3] ? z : new int[4];
    }

    /** En qué eje se desplaza un contenedor: EJE_X, EJE_Y o 0 si no se desplaza. */
    static int eje(View a) {
        if (a instanceof HorizontalScrollView) return EJE_X;
        if (a instanceof ScrollView || a instanceof NestedScrollView) return EJE_Y;
        if (a instanceof RecyclerView) {
            RecyclerView.LayoutManager lm = ((RecyclerView) a).getLayoutManager();
            return lm != null && lm.canScrollHorizontally() ? EJE_X : EJE_Y;
        }
        return 0;
    }

    /** El anillo recortado a una zona: lo que asoma de una lista no se pinta sobre la cabecera. */
    private static final class Recortado extends android.graphics.drawable.Drawable {
        private final GradientDrawable dentro;
        private final android.graphics.Rect zona = new android.graphics.Rect();

        Recortado(GradientDrawable dentro) {
            this.dentro = dentro;
        }

        void recortarA(android.graphics.Rect r) {
            if (!zona.equals(r)) {
                zona.set(r);
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull android.graphics.Canvas c) {
            if (zona.isEmpty()) return;
            int guardado = c.save();
            c.clipRect(zona);
            dentro.draw(c);
            c.restoreToCount(guardado);
        }

        @Override
        public void setAlpha(int alfa) {
            dentro.setAlpha(alfa);
        }

        @Override
        public void setColorFilter(@Nullable android.graphics.ColorFilter f) {
            dentro.setColorFilter(f);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    // El color del texto sobre lo que hay detrás de la vista: el colorOnSurface del padre.
    private static int colorAlrededor(View v) {
        ViewParent p = v.getParent();
        View fondo = p instanceof View ? (View) p : v;
        return MaterialColors.getColor(fondo, com.google.android.material.R.attr.colorOnSurface);
    }
}
