package es.pmdm.gymprofit.ui.widget;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta;
import es.pmdm.gymprofit.ui.tarjetas.TextosTarjeta;

// ============================================================
// ElegirRutinaHoja — la hoja «¿Qué rutina hoy?» de «Cambiar» y «Empezar otra» de Hoy toca (GP-105;
// rehecha en GP-232): cada rutina con «Toca hoy» en la que elige HoyToca, sus zonas en frase,
// «La última vez, hace 6 días», las barras pequeñas de sus últimas veces y ▶; al final, «Empezar
// sin rutina». Las zonas y las últimas veces vienen con la lista de activas (GP-232): no se pide
// nada al abrirla. Quien la abre decide qué hacer con la elegida (null = sin rutina).
// ============================================================
public final class ElegirRutinaHoja {

    /** La rutina elegida, o null si se eligió empezar sin rutina. */
    public interface Eleccion { void elegida(@Nullable Rutina rutina); }

    private ElegirRutinaHoja() { }

    /**
     * @param tocaHoyId la rutina que elige HoyToca para hoy, o null si ninguna.
     */
    public static void mostrar(Activity act, List<Rutina> propias, @Nullable Integer tocaHoyId, Eleccion eleccion) {
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_elegir_rutina, null, false);
        LinearLayout lista = raiz.findViewById(R.id.listaRutinas);
        TextosTarjeta textos = new TextosTarjeta(act);
        EntradaTarjeta entrada = new EntradaTarjeta();

        // La que toca hoy, la primera, como en el tablero; las demás en su orden.
        List<Rutina> enOrden = new java.util.ArrayList<>(propias);
        for (int i = 0; i < enOrden.size(); i++) {
            if (tocaHoyId != null && tocaHoyId == enOrden.get(i).getId()) {
                enOrden.add(0, enOrden.remove(i));
                break;
            }
        }
        for (Rutina r : enOrden) {
            boolean toca = tocaHoyId != null && tocaHoyId == r.getId();
            lista.addView(fila(act, lista, r, toca, textos, entrada, () -> {
                hoja.dismiss();
                eleccion.elegida(r);
            }));
        }
        lista.addView(sinRutina(act, lista, () -> {
            hoja.dismiss();
            eleccion.elegida(null);
        }));

        hoja.setContentView(raiz);
        hoja.show();
    }

    private static View fila(Activity act, ViewGroup padre, Rutina r, boolean toca, TextosTarjeta textos,
                             EntradaTarjeta entrada, Runnable accion) {
        View f = LayoutInflater.from(act).inflate(R.layout.item_elegir_rutina, padre, false);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(r.getNombre());
        f.findViewById(R.id.tvTocaHoy).setVisibility(toca ? View.VISIBLE : View.GONE);
        String zonas = textos.zonas(r.getMusculos());
        texto(f.findViewById(R.id.tvSub), zonas);
        String ultima = textos.ultimaVezCorta(r.getUltimas());
        texto(f.findViewById(R.id.tvUltima), ultima);
        BarrasVecesView barras = f.findViewById(R.id.barrasElegir);
        // Las barras solo si la última vez tuvo kilos (CifraTarjeta).
        List<es.pmdm.gymprofit.model.sesion.VezRutina> u = r.getUltimas();
        boolean conKilos = !u.isEmpty() && es.pmdm.gymprofit.utils.CifraTarjeta.deVez(u.get(u.size() - 1)).conKilos();
        barras.setVeces(conKilos ? u : new java.util.ArrayList<>());
        entrada.pintar("r:" + r.getId(), null, 0, R.dimen.text_tarjeta_kg, textos, barras);
        f.findViewById(R.id.ivEmpezar).setVisibility(View.VISIBLE);

        StringBuilder a11y = new StringBuilder(act.getString(R.string.empezar_rutina_a11y, r.getNombre()));
        if (toca) a11y.append(act.getString(R.string.tarjeta_toca_hoy_a11y));
        a11y.append(". ");
        if (!zonas.isEmpty()) a11y.append(zonas).append(". ");
        a11y.append(ultima).append('.');
        f.setContentDescription(a11y);
        f.setOnClickListener(v -> accion.run());
        return f;
    }

    private static View sinRutina(Activity act, ViewGroup padre, Runnable accion) {
        View f = LayoutInflater.from(act).inflate(R.layout.item_elegir_rutina, padre, false);
        f.setMinimumHeight(act.getResources().getDimensionPixelSize(R.dimen.fila_alto_min));
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(R.string.btn_empezar_sin_rutina);
        ImageView icono = f.findViewById(R.id.ivIcono);
        icono.setImageResource(R.drawable.ic_ms_bolt);
        icono.setVisibility(View.VISIBLE);
        f.setContentDescription(act.getString(R.string.btn_empezar_sin_rutina));
        f.setOnClickListener(v -> accion.run());
        return f;
    }

    private static void texto(@NonNull TextView tv, @Nullable String s) {
        tv.setText(s);
        tv.setVisibility(s == null || s.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
