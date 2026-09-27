package es.pmdm.gymprofit.ui.widget;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;

// ============================================================
// ElegirRutinaHoja — la hoja de «Cambiar» y «Empezar otra» de Hoy toca (GP-105):
// las rutinas propias y «Empezar sin rutina». Quien la abre decide qué hacer con la
// elegida (null = sin rutina).
// ============================================================
public final class ElegirRutinaHoja {

    /** La rutina elegida, o null si se eligió empezar sin rutina. */
    public interface Eleccion { void elegida(@Nullable Rutina rutina); }

    private ElegirRutinaHoja() { }

    public static void mostrar(Activity act, List<Rutina> propias, Eleccion eleccion) {
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_elegir_rutina, null, false);
        LinearLayout lista = raiz.findViewById(R.id.listaRutinas);

        for (Rutina r : propias) {
            String sub = act.getResources().getQuantityString(
                    R.plurals.rutina_num_ejercicios, r.getNumEjercicios(), r.getNumEjercicios());
            lista.addView(fila(act, lista, r.getNombre(), sub, R.drawable.ic_ms_play_arrow, () -> {
                hoja.dismiss();
                eleccion.elegida(r);
            }));
        }
        lista.addView(fila(act, lista, act.getString(R.string.btn_empezar_sin_rutina), null,
                R.drawable.ic_ms_bolt, () -> {
                    hoja.dismiss();
                    eleccion.elegida(null);
                }));

        hoja.setContentView(raiz);
        hoja.show();
    }

    private static View fila(Activity act, ViewGroup padre, String titulo, @Nullable String sub,
                             int icono, Runnable accion) {
        View f = LayoutInflater.from(act).inflate(R.layout.item_elegir_rutina, padre, false);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        TextView tvSub = f.findViewById(R.id.tvSub);
        tvSub.setText(sub);
        tvSub.setVisibility(sub == null ? View.GONE : View.VISIBLE);
        ((android.widget.ImageView) f.findViewById(R.id.ivIcono)).setImageResource(icono);
        f.setContentDescription(sub == null ? titulo : titulo + ". " + sub);
        f.setOnClickListener(v -> accion.run());
        return f;
    }
}
