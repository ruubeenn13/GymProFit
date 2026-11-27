package es.pmdm.gymprofit.ui.cristal;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import es.pmdm.gymprofit.R;

// ============================================================
// FondoAmbiental — el fondo de las pantallas Liquid Glass (GP-285, DEC-070).
//
// La base (#08090B en oscuro, #F2EDE8 en claro) con los brillos radiales difusos encima. Es
// estático: se calcula una vez por tamaño, a resolución completa y con tramado propio
// (Ambiente.pintar), y dibujarlo es pegar una imagen opaca; no se mueve con el scroll ni se
// recalcula. El contenido, que sí se mueve, pasa por encima.
//
// No son degradados de la GPU a propósito (lote 1b): cada versión de Android los trama a su
// manera (en Android 7, un damero de grano grueso) y solo saben bajar en línea recta, que
// dejaba a la vista el canto de cada brillo. La imagen es la misma, píxel a píxel, en todas.
//
// Calcularla es una pantalla entera de píxeles (medido en el emulador: de 0,4 a 0,6 s en la
// compilación de pruebas): se hace en un hilo aparte. Mientras, se ve el color base, y los
// brillos aparecen con un fundido corto en cuanto están; al cambiar de tamaño (un giro) se
// sigue viendo la imagen anterior, estirada, hasta que llega la nueva.
//
// La geometría es la de Ambiente, que es también la que ContrasteCristalTest recorre para
// medir el texto: lo que se pinta y lo que se mide salen del mismo sitio.
//
// Los bordes del sistema (BordeSistema) desvanecen el contenido hacia este mismo fondo: le
// piden la imagen con imagen() y cuánto se ve con aparicion(), y alCambiar() les avisa.
// ============================================================
public class FondoAmbiental extends Drawable {

    // Filas que se pintan de una vez: así no hace falta un segundo búfer del tamaño de la pantalla.
    private static final int TANDA = 64;
    // Un solo hilo para todos los fondos: si llegan dos tamaños seguidos, van en orden.
    private static final Executor HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fondo-ambiental");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private final int base;
    private final Ambiente.Brillo[] brillos;
    private final long fundido;
    private final Handler interfaz = new Handler(Looper.getMainLooper());
    private final Paint pintura = new Paint(Paint.FILTER_BITMAP_FLAG);
    @Nullable private Bitmap imagen;
    @Nullable private Runnable alCambiar;
    @Nullable private ValueAnimator animacion;
    // El último tamaño pedido: una imagen que llega para otro ya no vale.
    private int pedido;
    private float aparicion;

    /** El fondo de Inicio, con los colores del tema de ese contexto. */
    public FondoAmbiental(@NonNull Context context) {
        base = ContextCompat.getColor(context, R.color.gp_ambiente_base);
        brillos = Ambiente.inicio(
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_1),
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_2),
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_3),
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_4));
        fundido = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    /** El fondo ya pintado; null hasta que está calculado. Puede ser de un tamaño anterior. */
    @Nullable
    public Bitmap imagen() {
        return imagen;
    }

    /** Cuánto de la imagen se ve sobre el color base: de 0 (aún no está) a 1 (entera). */
    public float aparicion() {
        return aparicion;
    }

    /** A quién avisar cuando hay imagen nueva y en cada paso de su fundido. */
    public void alCambiar(@Nullable Runnable aviso) {
        alCambiar = aviso;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        if (b.isEmpty()) return;
        final int ancho = b.width(), alto = b.height();
        if (imagen != null && imagen.getWidth() == ancho && imagen.getHeight() == alto) return;
        final int esta = ++pedido;
        HILO.execute(() -> {
            Bitmap nueva = calcular(ancho, alto, base, brillos);
            interfaz.post(() -> {
                if (esta == pedido) estrenar(nueva);
            });
        });
    }

    // En el hilo aparte: no toca nada de esta instancia.
    @NonNull
    private static Bitmap calcular(int ancho, int alto, int base, Ambiente.Brillo[] brillos) {
        Bitmap nueva = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
        nueva.setHasAlpha(false);
        int[] px = new int[ancho * TANDA];
        for (int y = 0; y < alto; y += TANDA) {
            int filas = Math.min(TANDA, alto - y);
            Ambiente.pintar(px, ancho, alto, y, filas, base, brillos);
            nueva.setPixels(px, 0, ancho, 0, y, ancho, filas);
        }
        return nueva;
    }

    // De vuelta en el hilo de la interfaz: la primera imagen aparece con un fundido; la de un
    // cambio de tamaño sustituye a la anterior sin más.
    private void estrenar(@NonNull Bitmap nueva) {
        boolean primera = imagen == null;
        imagen = nueva;
        if (animacion != null) animacion.cancel();
        if (primera && fundido > 0 && animacionesEncendidas()) {
            animacion = ValueAnimator.ofFloat(0f, 1f).setDuration(fundido);
            animacion.addUpdateListener(a -> poner((float) a.getAnimatedValue()));
            animacion.start();
        } else {
            poner(1f);
        }
    }

    // Con las animaciones del sistema apagadas (o «quitar animaciones» de accesibilidad), sin fundido.
    private static boolean animacionesEncendidas() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled();
    }

    private void poner(float parte) {
        aparicion = parte;
        pintura.setAlpha(Math.round(255f * parte));
        invalidateSelf();
        if (alCambiar != null) alCambiar.run();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (imagen == null || aparicion < 1f) canvas.drawColor(base);
        if (imagen == null || aparicion <= 0f) return;
        if (imagen.getWidth() == b.width() && imagen.getHeight() == b.height()) {
            // Píxel a píxel: el filtro no hace nada y el tramado llega intacto.
            canvas.drawBitmap(imagen, b.left, b.top, pintura);
        } else {
            // La de un tamaño anterior, estirada, mientras se calcula la nueva.
            canvas.drawBitmap(imagen, null, b, pintura);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // No se usa: quien lo quiere translúcido cambia el alfa de su vista.
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter filtro) {
        // No se tiñe.
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
