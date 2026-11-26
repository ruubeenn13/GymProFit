package es.pmdm.gymprofit.ui.cristal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// BordeSistema — el degradado que desvanece el contenido junto a una barra del sistema
// (GP-285, DEC-070): el fondo base, macizo en la franja de la barra, y de ahí a transparente.
//
// Es el fondo de una vista sin más (bordeSuperior y bordeInferior en activity_main). Las
// medidas las da BordesSistema; aquí solo se pinta. El degradado se monta al cambiar de
// tamaño, de color o de tramo macizo, nunca al dibujar.
// ============================================================
public class BordeSistema extends Drawable {

    private final boolean arriba;
    private final Paint pintura = new Paint();
    @ColorInt private int color;
    private int macizo;

    /** @param arriba true para el de la barra de estado (macizo arriba); false, el de abajo. */
    public BordeSistema(boolean arriba) {
        this.arriba = arriba;
        // Degradado largo sobre casi negro: sin tramado se ven los escalones.
        pintura.setDither(true);
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
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float parte = Math.min(1f, macizo / (float) b.height());
        // De la punta transparente a la maciza: el de arriba, de abajo arriba.
        pintura.setShader(new LinearGradient(0f, arriba ? b.bottom : b.top, 0f, arriba ? b.top : b.bottom,
                BordesSistema.colores(color, parte), BordesSistema.paradas(parte), Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(getBounds(), pintura);
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
