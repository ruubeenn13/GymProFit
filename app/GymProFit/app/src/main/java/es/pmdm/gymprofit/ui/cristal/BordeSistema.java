package es.pmdm.gymprofit.ui.cristal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// BordeSistema — el degradado que desvanece el contenido junto a una barra del sistema
// (GP-285, DEC-070): el fondo base, macizo en la franja de la barra, y de ahí a transparente.
//
// Es el fondo de una vista sin más (bordeSuperior y bordeInferior en activity_main). Las
// medidas y la rampa las da BordesSistema; aquí solo se pinta.
//
// La rampa es una imagen de 1 px de ancho, una fila por píxel de alto, que al dibujar se
// estira a lo ancho. No es un degradado de la GPU a propósito: la curva (el alfa al cuadrado)
// pedía ocho paradas, y ese sombreador, con tramado, tiraba el emulador del CI (SwiftShader)
// en cuanto se pintaba Inicio. Una imagen estirada es lo más simple que se le puede pedir a
// una GPU, da la curva exacta y no necesita tramado: son 255 niveles en un centenar de filas.
// Se rehace al cambiar de tamaño, de color o de tramo macizo, nunca al dibujar.
// ============================================================
public class BordeSistema extends Drawable {

    private final boolean arriba;
    @ColorInt private int color;
    private int macizo;
    @Nullable private Bitmap rampa;

    /** @param arriba true para el de la barra de estado (macizo arriba); false, el de abajo. */
    public BordeSistema(boolean arriba) {
        this.arriba = arriba;
    }

    /**
     * El color del fondo que hay detrás y cuánto del borde, desde el canto de la pantalla, es macizo.
     *
     * @param color  el fondo base de la pestaña, opaco.
     * @param macizo alto del tramo macizo, en px.
     */
    public void poner(@ColorInt int color, int macizo) {
        if (color == this.color && macizo == this.macizo) return;
        this.color = color;
        this.macizo = macizo;
        montar();
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        montar();
    }

    private void montar() {
        int alto = getBounds().height();
        if (alto <= 0) {
            rampa = null;
            return;
        }
        int[] alfas = BordesSistema.alfas(alto, macizo);
        int rgb = color & 0x00FFFFFF;
        int[] filas = new int[alto];
        for (int y = 0; y < alto; y++) {
            // La fila 0 de la rampa es la punta transparente: en el de arriba va abajo.
            filas[arriba ? alto - 1 - y : y] = (alfas[y] << 24) | rgb;
        }
        rampa = Bitmap.createBitmap(filas, 1, alto, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (rampa != null) canvas.drawBitmap(rampa, null, getBounds(), null);
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
