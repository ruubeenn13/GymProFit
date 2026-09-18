package es.pmdm.gymprofit.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import es.pmdm.gymprofit.R;

// ============================================================
// InsetsUtils — coloca el contenido de cada pantalla fuera de las barras del
// sistema (estado, navegación, muesca y teclado).
//
// Con targetSdk 36 Android 15+ dibuja SIEMPRE de borde a borde y ya no respeta
// los colores de barra del tema: si nadie gestiona los insets, la primera línea
// de cada pantalla queda debajo del reloj y la última debajo de la barra de
// gestos. Afecta a las 42 pantallas.
//
// Se aplica desde GymProFitApp con un ActivityLifecycleCallbacks y no desde
// BaseActivity a propósito: solo 12 de las 35 Activities extienden BaseActivity,
// así que por herencia se quedarían 23 pantallas sin arreglar. Es el mismo sitio
// desde el que ya se aplican las transiciones y el distintivo de pruebas.
// ============================================================
public final class InsetsUtils {

    private InsetsUtils() { }

    // Pantallas que NO deben separarse de las barras: ocupan la pantalla completa a
    // propósito y colocan su propio contenido centrado. Padding aquí solo las
    // descuadraría.
    private static final Set<String> A_PANTALLA_COMPLETA = new HashSet<>(Arrays.asList(
            "SplashActivity"));

    /**
     * Desactiva el ajuste automático de la ventana y reserva, en la raíz del
     * contenido, el espacio que ocupan las barras del sistema.
     *
     * <p>Se desactiva el ajuste en todas las versiones de Android, no solo en las
     * que fuerzan el borde a borde, para que la app se comporte igual en un móvil
     * con Android 7 que en uno con Android 15 y no haya que mantener dos casos.
     *
     * <p>El padding original del layout se respeta: los insets se SUMAN a él, y se
     * guarda una copia la primera vez para no acumular en sucesivas aplicaciones
     * (rotación, teclado que entra y sale, cambio de tema).
     *
     * @param actividad la Activity recién creada.
     */
    public static void aplicar(@NonNull Activity actividad) {
        Window ventana = actividad.getWindow();
        if (ventana == null) return;

        WindowCompat.setDecorFitsSystemWindows(ventana, false);
        ajustarContrasteDeBarras(actividad, ventana);

        if (A_PANTALLA_COMPLETA.contains(actividad.getClass().getSimpleName())) return;

        View raiz = raizDeContenido(actividad);
        if (raiz == null) return;

        final int[] base = paddingOriginal(raiz);

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (vista, insets) -> {
            // La muesca importa en horizontal: en vertical va dentro de la barra de
            // estado, pero al girar el móvil se come un lateral que systemBars() no
            // cubre. Se toma el máximo de las dos para no quedarse corto.
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets muesca = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            // El teclado sustituye a la barra de navegación cuando está abierto, así
            // que abajo manda el mayor de los dos y nunca se suman.
            int teclado = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            vista.setPadding(
                    base[0] + Math.max(barras.left, muesca.left),
                    base[1] + Math.max(barras.top, muesca.top),
                    base[2] + Math.max(barras.right, muesca.right),
                    base[3] + Math.max(Math.max(barras.bottom, muesca.bottom), teclado));

            // No se consumen: los hijos que quieran pintar bajo las barras (la barra
            // flotante de navegación) siguen recibiéndolos.
            return insets;
        });

        ViewCompat.requestApplyInsets(raiz);
    }

    /**
     * Pone los iconos de las barras del sistema en claro u oscuro según el tema,
     * para que se vean sobre el fondo de la app ahora que las barras son
     * transparentes.
     */
    private static void ajustarContrasteDeBarras(@NonNull Activity actividad, @NonNull Window ventana) {
        int modo = actividad.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean esOscuro = modo == Configuration.UI_MODE_NIGHT_YES;

        WindowInsetsControllerCompat control =
                WindowCompat.getInsetsController(ventana, ventana.getDecorView());
        // Iconos oscuros sobre fondo claro y al revés.
        control.setAppearanceLightStatusBars(!esOscuro);
        control.setAppearanceLightNavigationBars(!esOscuro);
    }

    /**
     * Devuelve la vista que hay que separar de las barras: la raíz que infló la
     * Activity con setContentView, no el contenedor del sistema.
     *
     * @return la raíz del layout, o {@code null} si la Activity aún no tiene contenido.
     */
    private static View raizDeContenido(@NonNull Activity actividad) {
        View contenido = actividad.findViewById(android.R.id.content);
        if (!(contenido instanceof ViewGroup)) return contenido;

        ViewGroup grupo = (ViewGroup) contenido;
        return grupo.getChildCount() > 0 ? grupo.getChildAt(0) : grupo;
    }

    /**
     * Padding que el layout traía escrito, guardado en la propia vista la primera
     * vez que se consulta.
     *
     * <p>Sin esto, cada nueva aplicación de insets (abrir el teclado, girar la
     * pantalla) sumaría otra vez sobre el padding ya sumado y el contenido se iría
     * hundiendo hacia el centro.
     *
     * @return array {izquierda, arriba, derecha, abajo} en píxeles.
     */
    private static int[] paddingOriginal(@NonNull View vista) {
        Object guardado = vista.getTag(R.id.tag_padding_original);
        if (guardado instanceof int[]) return (int[]) guardado;

        int[] original = {
                vista.getPaddingLeft(), vista.getPaddingTop(),
                vista.getPaddingRight(), vista.getPaddingBottom()};
        vista.setTag(R.id.tag_padding_original, original);
        return original;
    }
}
