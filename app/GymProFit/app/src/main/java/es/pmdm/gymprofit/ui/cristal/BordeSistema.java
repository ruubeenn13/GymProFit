package es.pmdm.gymprofit.ui.cristal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// BordeSistema — el degradado que desvanece el contenido junto a una barra del sistema
// (GP-285, DEC-070): el fondo, macizo en la franja de la barra, y de ahí a transparente.
//
// Es el fondo de una vista sin más (bordeSuperior y bordeInferior en activity_main). Las
// medidas y la rampa las da BordesSistema; aquí solo se pinta.
//
// También es el velo que desvanece la lista junto al canto de un cristal vivo (el resumen y el
// descanso de la Sesión en vivo; CantoVivo): el mismo dibujo, a media pantalla. Como ahí la
// vista no está pegada a un canto del lienzo, se le dice dónde está (ponerOrigen) para que coja
// del fondo ambiental los píxeles de su sitio.
//
// «El fondo» es lo que la pestaña tiene detrás del contenido:
//
//   · Un color liso (gp_background) en las pestañas que aún no están en Liquid Glass. La rampa
//     es una imagen de 1 px de ancho, una fila por píxel de alto, que al dibujar se estira.
//   · El fondo ambiental en las que sí (Inicio): los mismos píxeles que FondoAmbiental pinta
//     en ese sitio, con el alfa de la rampa. Pintado en el color base liso, el borde tapaba
//     también el brillo que hay bajo la barra de estado y dejaba una banda oscura de lado a
//     lado, con su canto, justo donde el naranja es más vivo (lote 1b). El contenido se
//     desvanece hacia el fondo que hay, no hacia otro.
//
// Al cambiar de pestaña, el fondo ambiental aparece y se va con un fundido: mezcla() lleva el
// mismo paso aquí, para que el borde no cambie de golpe mientras el fondo aún se está yendo.
//
// No es un degradado de la GPU a propósito: la curva (el alfa al cuadrado) pedía ocho
// paradas, y ese sombreador, con tramado, tiraba el emulador del CI (SwiftShader) en cuanto
// se pintaba Inicio. Una imagen es lo más simple que se le puede pedir a una GPU y da la curva
// exacta. Se rehace al cambiar de tamaño, de fondo o de tramo macizo, nunca al dibujar.
// ============================================================
public class BordeSistema extends Drawable {

    private final boolean arriba;
    private final Paint pinturaFondo = new Paint();
    @ColorInt private int color;
    private int macizo;
    // Cuánto del macizo, por su extremo, vuelve a transparente (un velo a media pantalla), en px.
    private int entrada;
    @Nullable private Bitmap fondo;
    private float mezcla = 1f;
    // Dónde está el canto de arriba de la vista dentro del fondo, en px; -1, pegada a su canto
    // (el de arriba o el de abajo, según sea).
    private int origen = -1;
    // La rampa en el color liso (1 px de ancho) y la del fondo ambiental (a todo el ancho).
    @Nullable private Bitmap rampa, rampaFondo;

    /** @param arriba true para el de la barra de estado (macizo arriba); false, el de abajo. */
    public BordeSistema(boolean arriba) {
        this.arriba = arriba;
    }

    /**
     * El color del fondo que hay detrás y cuánto del borde, desde el canto de la pantalla, es macizo.
     *
     * @param color  el fondo liso de la pestaña, opaco.
     * @param macizo alto del tramo macizo, en px.
     */
    public void poner(@ColorInt int color, int macizo) {
        if (color == this.color && macizo == this.macizo) return;
        this.color = color;
        this.macizo = macizo;
        montar();
        invalidateSelf();
    }

    /**
     * Un velo a media pantalla no acaba en seco por el lado de su macizo: sus últimas filas
     * vuelven a transparente ({@link BordesSistema#alfas(int, int, int)}).
     *
     * @param px cuántas filas del macizo son esa rampa de vuelta.
     */
    public void ponerEntrada(int px) {
        if (px == entrada) return;
        entrada = px;
        montar();
        invalidateSelf();
    }

    /**
     * El fondo ambiental de la pantalla, para desvanecer hacia él y no hacia un color liso.
     *
     * @param fondo la imagen de {@link FondoAmbiental#imagen()}, que ocupa el lienzo entero (el
     *              borde está pegado a su canto de arriba o de abajo); null, sin fondo ambiental.
     */
    public void ponerFondo(@Nullable Bitmap fondo) {
        if (fondo == this.fondo) return;
        this.fondo = fondo;
        montar();
        invalidateSelf();
    }

    /**
     * Dónde está la vista dentro del lienzo, cuando no va pegada a su canto de arriba o de abajo
     * (el velo de un cristal vivo): de ahí salen los píxeles del fondo ambiental que enseña.
     *
     * @param y la y de su canto de arriba dentro del fondo, en px; negativo, pegada al canto.
     */
    public void ponerOrigen(int y) {
        int o = y < 0 ? -1 : y;
        if (o == origen) return;
        origen = o;
        montar();
        invalidateSelf();
    }

    /**
     * Cuánto del fondo ambiental se enseña sobre el color liso: 1, entero; 0, nada. Sigue al
     * fundido con el que el fondo ambiental aparece y se va al cambiar de pestaña.
     */
    public void mezcla(float parte) {
        float m = Math.max(0f, Math.min(1f, parte));
        if (m == mezcla) return;
        mezcla = m;
        pinturaFondo.setAlpha(Math.round(255f * m));
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        montar();
    }

    private void montar() {
        int ancho = getBounds().width(), alto = getBounds().height();
        rampa = null;
        rampaFondo = null;
        if (alto <= 0) return;
        int[] alfas = BordesSistema.alfas(alto, macizo, entrada);
        int rgb = color & 0x00FFFFFF;
        int[] filas = new int[alto];
        for (int y = 0; y < alto; y++) {
            // La fila 0 de la rampa es la punta transparente: en el de arriba va abajo.
            filas[arriba ? alto - 1 - y : y] = (alfas[y] << 24) | rgb;
        }
        rampa = Bitmap.createBitmap(filas, 1, alto, Bitmap.Config.ARGB_8888);

        // Con fondo ambiental: sus propios píxeles en este sitio, con el alfa de cada fila.
        if (fondo == null || ancho <= 0 || fondo.getWidth() != ancho || fondo.getHeight() < alto) return;
        int desde = origen >= 0 ? origen : arriba ? 0 : fondo.getHeight() - alto;
        // Fuera del fondo (un velo a medio colocar) no hay píxeles que coger: el color liso.
        if (desde + alto > fondo.getHeight()) return;
        int[] px = new int[ancho * alto];
        fondo.getPixels(px, 0, ancho, 0, desde, ancho, alto);
        for (int y = 0; y < alto; y++) {
            int alfa = filas[y] & 0xFF000000;
            for (int i = y * ancho, fin = i + ancho; i < fin; i++) px[i] = alfa | (px[i] & 0x00FFFFFF);
        }
        rampaFondo = Bitmap.createBitmap(px, ancho, alto, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        boolean conFondo = rampaFondo != null && mezcla > 0f;
        if (rampa != null && !(conFondo && mezcla >= 1f)) canvas.drawBitmap(rampa, null, getBounds(), null);
        if (conFondo) canvas.drawBitmap(rampaFondo, getBounds().left, getBounds().top, pinturaFondo);
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
        return PixelFormat.TRANSLUCENT;
    }
}
