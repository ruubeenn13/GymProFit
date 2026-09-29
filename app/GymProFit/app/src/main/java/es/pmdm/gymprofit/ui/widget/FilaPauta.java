package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.utils.Pauta;

// ============================================================
// FilaPauta — una fila item_ejercicio_pauta: el ejercicio y su pauta (lote 1.2.1).
//
// «Press de banca · 3 × 6–10», con el cronómetro si va por tiempo y, si se pide, la
// etiqueta «Básico» y las notas con el descanso. TalkBack lee la fila entera de una vez.
// ============================================================
public final class FilaPauta {

    private FilaPauta() { }

    /**
     * Crea la fila de un ejercicio.
     *
     * @param completa con la etiqueta «Básico», las notas y el descanso (el detalle de un
     *                 programa); sin ello, solo nombre y pauta («Hoy toca»).
     */
    public static View crear(Context ctx, ViewGroup padre, RutinaEjercicio re, boolean completa) {
        View fila = LayoutInflater.from(ctx).inflate(R.layout.item_ejercicio_pauta, padre, false);
        String nombre = re.getNombreEjercicio() != null && !re.getNombreEjercicio().isEmpty()
                ? re.getNombreEjercicio() : ctx.getString(R.string.ejercicio_sin_nombre, re.getEjercicioId());
        String pauta = Pauta.texto(ctx, re);

        ((TextView) fila.findViewById(R.id.tvEjercicio)).setText(nombre);
        TextView tvPauta = fila.findViewById(R.id.tvPauta);
        tvPauta.setText(pauta);
        tvPauta.setCompoundDrawablesRelativeWithIntrinsicBounds(re.esPorTiempo() ? R.drawable.ic_ms_timer : 0, 0, 0, 0);
        escalarIcono(ctx, tvPauta);

        List<String> a11y = new ArrayList<>();
        a11y.add(nombre);
        a11y.add(pauta);
        if (re.esPorTiempo()) a11y.add(ctx.getString(R.string.pauta_por_tiempo_a11y));

        if (completa) {
            fila.findViewById(R.id.tvBasico).setVisibility(re.esBasico() ? View.VISIBLE : View.GONE);
            if (re.esBasico()) a11y.add(ctx.getString(R.string.pauta_basico));
            List<String> extra = new ArrayList<>();
            String descanso = Pauta.descanso(ctx, re.getTiempoDescanso());
            if (descanso != null) extra.add(descanso);
            if (re.getNotas() != null && !re.getNotas().trim().isEmpty()) extra.add(re.getNotas().trim());
            if (!extra.isEmpty()) {
                String texto = String.join(ctx.getString(R.string.separador_punto), extra);
                TextView tvNota = fila.findViewById(R.id.tvNotaEjercicio);
                tvNota.setText(texto);
                tvNota.setVisibility(View.VISIBLE);
                a11y.add(texto);
            }
        }
        fila.setContentDescription(String.join(". ", a11y));
        return fila;
    }

    // El cronómetro, a 18 dp como el texto de al lado.
    static void escalarIcono(Context ctx, TextView tv) {
        android.graphics.drawable.Drawable d = tv.getCompoundDrawablesRelative()[0];
        if (d == null) return;
        int px = Math.round(18 * ctx.getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, px, px);
        tv.setCompoundDrawablesRelative(d, null, null, null);
    }
}
