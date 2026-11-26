package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// FondoAmbiental — el fondo de las pantallas Liquid Glass (GP-285, DEC-070).
//
// La base (#08090B en oscuro) con tres brillos radiales difusos encima. Es estático: los
// degradados se montan una vez por tamaño y pintarlo son cuatro rectángulos; no se mueve con
// el scroll ni se recalcula. El contenido, que sí se mueve, pasa por encima.
//
// La geometría es la de Ambiente, que es también la que ContrasteCristalTest recorre para
// medir el texto: lo que se pinta y lo que se mide salen del mismo sitio.
// ============================================================
public class FondoAmbiental extends Drawable {

    private final Paint base = new Paint();
    private final Ambiente.Brillo[] brillos;
    private final Paint[] pinturas;

    /** El fondo de Inicio, con los colores del tema de ese contexto. */
    public FondoAmbiental(@NonNull Context context) {
        base.setColor(ContextCompat.getColor(context, R.color.gp_ambiente_base));
        brillos = Ambiente.inicio(
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_1),
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_2),
                ContextCompat.getColor(context, R.color.gp_ambiente_brillo_3));
        pinturas = new Paint[brillos.length];
        for (int i = 0; i < pinturas.length; i++) {
            pinturas[i] = new Paint();
            // Degradados largos sobre casi negro: sin tramado se ven los escalones.
            pinturas[i].setDither(true);
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        if (b.isEmpty()) return;
        float ancho = b.width(), alto = b.height(), lado = Math.min(ancho, alto);
        for (int i = 0; i < brillos.length; i++) {
            Ambiente.Brillo br = brillos[i];
            // Un círculo de radio 1 estirado a la elipse: el mismo color de punta a punta, con
            // el alfa bajando en línea recta hasta 0 en Ambiente.FIN.
            int transparente = br.color & 0x00FFFFFF;
            RadialGradient g = new RadialGradient(0f, 0f, 1f,
                    new int[]{br.color, transparente, transparente},
                    new float[]{0f, Ambiente.FIN, 1f}, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            m.setScale(br.rx * lado, br.ry * lado);
            m.postTranslate(b.left + br.cx * ancho, b.top + br.cy * alto);
            g.setLocalMatrix(m);
            pinturas[i].setShader(g);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        canvas.drawRect(b, base);
        for (Paint p : pinturas) canvas.drawRect(b, p);
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
