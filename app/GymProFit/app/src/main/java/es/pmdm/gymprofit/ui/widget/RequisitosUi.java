package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.RequisitosClave;

// ============================================================
// RequisitosUi — pinta los requisitos que se marcan solos al escribir (GP-216, tablero 20): la
// lista de requisitos (view_requisito) y la barra de fortaleza (view_fortaleza) de la contraseña
// nueva y, con el mismo patrón, los del correo nuevo.
//
// Cumplido = el check en verde y el texto en verde; sin cumplir = el círculo vacío y el texto
// secundario. El estado no vive solo en el color: el icono cambia de forma y TalkBack lee «cumplido»
// o «pendiente» detrás del texto (DEC-019). Las reglas están en RequisitosClave.
// ============================================================
public final class RequisitosUi {

    private RequisitosUi() { }

    /** Pone el texto de un requisito (una vez) y su estado. */
    public static void requisito(@NonNull View fila, @StringRes int texto, boolean cumplido) {
        Context ctx = fila.getContext();
        TextView tv = fila.findViewById(R.id.tvRequisito);
        ImageView icono = fila.findViewById(R.id.ivRequisito);
        tv.setText(texto);
        int verde = ContextCompat.getColor(ctx, R.color.gp_success);
        int gris = MaterialColors.getColor(fila, com.google.android.material.R.attr.colorOnSurfaceVariant);
        icono.setImageResource(cumplido ? R.drawable.ic_ms_check_circle_fill : R.drawable.ic_ms_radio_button_unchecked);
        icono.setImageTintList(ColorStateList.valueOf(cumplido ? verde : gris));
        tv.setTextColor(cumplido ? verde : gris);
        fila.setContentDescription(ctx.getString(cumplido ? R.string.requisito_cumplido_a11y : R.string.requisito_pendiente_a11y,
                ctx.getString(texto)));
        fila.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** Los requisitos de la contraseña nueva. */
    public static void clave(@NonNull View raiz, @NonNull RequisitosClave r, boolean vacia) {
        requisito(raiz.findViewById(R.id.reqLongitud), R.string.clave_req_longitud, r.longitud);
        requisito(raiz.findViewById(R.id.reqDistinta), R.string.clave_req_distinta, r.distinta && !vacia);
        View bytes = raiz.findViewById(R.id.reqCabe);
        // El límite de 72 bytes solo se nombra cuando se pasa: casi nadie llega, y si se pasa, se dice por qué.
        bytes.setVisibility(r.cabe ? View.GONE : View.VISIBLE);
        if (!r.cabe) requisito(bytes, R.string.clave_req_cabe, false);
        requisito(raiz.findViewById(R.id.reqFrase), R.string.clave_req_frase, r.frase);
        fortaleza(raiz.findViewById(R.id.fortaleza), r, vacia);
    }

    // La barra: cuatro tramos y su palabra.
    private static void fortaleza(View barra, RequisitosClave r, boolean vacia) {
        Context ctx = barra.getContext();
        LinearLayout tramos = barra.findViewById(R.id.tramosFortaleza);
        int llenos = vacia ? 0 : r.segmentos;
        int verde = ContextCompat.getColor(ctx, R.color.gp_success);
        int pista = ContextCompat.getColor(ctx, R.color.gp_meter_track);
        int aviso = MaterialColors.getColor(barra, androidx.appcompat.R.attr.colorPrimary);
        for (int i = 0; i < tramos.getChildCount(); i++) {
            View t = tramos.getChildAt(i);
            int color = i >= llenos ? pista : (r.aceptable() ? verde : aviso);
            t.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        TextView palabra = barra.findViewById(R.id.tvFortaleza);
        if (vacia) {
            palabra.setText(null);
            return;
        }
        int texto;
        if (!r.cabe) texto = R.string.clave_fuerza_larga;
        else if (!r.longitud) texto = R.string.clave_fuerza_corta;
        else if (r.segmentos >= 4) texto = R.string.clave_fuerza_segura;
        else if (r.segmentos == 3) texto = R.string.clave_fuerza_buena;
        else texto = R.string.clave_fuerza_justa;
        palabra.setText(texto);
        palabra.setTextColor(r.aceptable() ? verde : MaterialColors.getColor(barra,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    /** Los requisitos del correo nuevo. */
    public static void correo(@NonNull View raiz, @NonNull RequisitosClave.CorreoNuevo r, boolean vacio) {
        requisito(raiz.findViewById(R.id.reqForma), R.string.correo_req_forma, r.forma);
        requisito(raiz.findViewById(R.id.reqDistinto), R.string.correo_req_distinto, r.distinto && !vacio);
    }
}
