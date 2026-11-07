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
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta;
import es.pmdm.gymprofit.ui.tarjetas.TextosTarjeta;
import es.pmdm.gymprofit.utils.CifraTarjeta;
import es.pmdm.gymprofit.utils.OrdenRutinas;

// ============================================================
// ElegirRutinaHoja — las dos hojas de elegir rutina, con un solo orden (OrdenRutinas, GP-241):
// la que toca hoy, el resto del ciclo del programa tras ella y las tuyas en el orden de «Mis
// rutinas». Las zonas y las últimas veces vienen con la lista de activas (GP-232): no se pide
// nada al abrirla.
//
//   · «¿Qué rutina hoy?» (Cambiar y Empezar otra de Hoy toca; GP-105, rehecha en GP-232): sin
//     títulos, «Toca hoy» en la de hoy, ▶ en cada fila y «Empezar sin rutina» al final.
//   · «¿Qué entrenaste?» (Apuntar entreno, GP-241, tablero Elegir): «De tu programa, …» y «Tus
//     rutinas», el check en la elegida y «Entreno libre» al final, tras una raya. Cada fila es un
//     botón de opción para TalkBack. Se elige con un toque y se cierra.
// Quien la abre decide qué hacer con la elegida (null = sin rutina).
// ============================================================
public final class ElegirRutinaHoja {

    /** La rutina elegida, o null si se eligió sin rutina. */
    public interface Eleccion { void elegida(@Nullable Rutina rutina); }

    private ElegirRutinaHoja() { }

    /**
     * «¿Qué rutina hoy?»: las rutinas en el orden de las hojas, con «Toca hoy» en la de hoy.
     *
     * @param orden las rutinas ya ordenadas (OrdenRutinas.ordenar).
     */
    public static void mostrar(Activity act, OrdenRutinas.Orden orden, Eleccion eleccion) {
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_elegir_rutina, null, false);
        LinearLayout lista = raiz.findViewById(R.id.listaRutinas);
        TextosTarjeta textos = new TextosTarjeta(act);
        EntradaTarjeta entrada = new EntradaTarjeta();

        for (Rutina r : orden.todas()) {
            boolean toca = orden.hoyId != null && orden.hoyId == r.getId();
            View f = fila(act, lista, r, toca, textos, entrada);
            f.findViewById(R.id.ivEmpezar).setVisibility(View.VISIBLE);
            f.setContentDescription(act.getString(R.string.empezar_rutina_a11y, r.getNombre())
                    + resto(act, r, toca, textos));
            f.setOnClickListener(v -> {
                hoja.dismiss();
                eleccion.elegida(r);
            });
            lista.addView(f);
        }
        lista.addView(sinRutina(act, lista, R.string.btn_empezar_sin_rutina, 0, () -> {
            hoja.dismiss();
            eleccion.elegida(null);
        }));

        hoja.setContentView(raiz);
        hoja.show();
    }

    /**
     * «¿Qué entrenaste?»: las del programa y las tuyas con su título, el check en la elegida y
     * «Entreno libre» al final.
     *
     * @param orden     las rutinas ya ordenadas (OrdenRutinas.ordenar).
     * @param elegidaId la rutina elegida ahora, o null si es un entreno libre.
     */
    public static void mostrarParaApuntar(Activity act, OrdenRutinas.Orden orden, @Nullable Integer elegidaId,
                                          Eleccion eleccion) {
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_elegir_rutina, null, false);
        ((TextView) raiz.findViewById(R.id.tvTituloHojaRutina)).setText(R.string.elegir_que_entrenaste);
        View cerrar = raiz.findViewById(R.id.btnCerrarHojaRutina);
        cerrar.setVisibility(View.VISIBLE);
        cerrar.setOnClickListener(v -> hoja.dismiss());
        LinearLayout lista = raiz.findViewById(R.id.listaRutinas);
        TextosTarjeta textos = new TextosTarjeta(act);
        EntradaTarjeta entrada = new EntradaTarjeta();

        if (!orden.programa.isEmpty()) {
            lista.addView(titulo(act, lista, orden.nombrePrograma != null
                    ? act.getString(R.string.elegir_de_tu_programa, orden.nombrePrograma)
                    : act.getString(R.string.elegir_de_tu_programa_sin_nombre), false));
            opciones(act, lista, orden.programa, orden.hoyId, elegidaId, textos, entrada, hoja, eleccion);
        }
        if (!orden.propias.isEmpty()) {
            lista.addView(titulo(act, lista, act.getString(R.string.elegir_tus_rutinas), !orden.programa.isEmpty()));
            opciones(act, lista, orden.propias, orden.hoyId, elegidaId, textos, entrada, hoja, eleccion);
        }

        if (!orden.programa.isEmpty() || !orden.propias.isEmpty()) lista.addView(raya(act, lista));
        View libre = sinRutina(act, lista, R.string.apuntar_libre, R.string.apuntar_libre_sub, () -> {
            hoja.dismiss();
            eleccion.elegida(null);
        });
        apuntarEstilo(act, libre, elegidaId == null);
        ImageView rayo = libre.findViewById(R.id.ivIcono);
        int lado = act.getResources().getDimensionPixelSize(R.dimen.fila_icono_circulo);
        int dentro = (lado - act.getResources().getDimensionPixelSize(R.dimen.fila_icono)) / 2;
        rayo.getLayoutParams().width = lado;
        rayo.getLayoutParams().height = lado;
        rayo.setPadding(dentro, dentro, dentro, dentro);
        rayo.setBackgroundResource(R.drawable.bg_icono_libre);
        rayo.setImageTintList(android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors
                .getColor(rayo, com.google.android.material.R.attr.colorOnPrimaryContainer)));
        lista.addView(libre);

        hoja.setContentView(raiz);
        hoja.show();
    }

    // Las filas de una sección de «¿Qué entrenaste?».
    private static void opciones(Activity act, LinearLayout lista, List<Rutina> rutinas, @Nullable Integer hoyId,
                                 @Nullable Integer elegidaId, TextosTarjeta textos, EntradaTarjeta entrada,
                                 BottomSheetDialog hoja, Eleccion eleccion) {
        for (Rutina r : rutinas) {
            boolean toca = hoyId != null && hoyId == r.getId();
            boolean elegida = elegidaId != null && elegidaId == r.getId();
            View f = fila(act, lista, r, toca, textos, entrada);
            ImageView check = f.findViewById(R.id.ivEmpezar);
            check.setImageResource(R.drawable.ic_ms_check_circle_fill);
            check.setVisibility(elegida ? View.VISIBLE : View.INVISIBLE);
            f.setContentDescription(r.getNombre() + resto(act, r, toca, textos));
            apuntarEstilo(act, f, elegida);
            f.setOnClickListener(v -> {
                hoja.dismiss();
                eleccion.elegida(r);
            });
            lista.addView(f);
        }
    }

    // Una fila de rutina: nombre, «Toca hoy», zonas, la última vez y sus barras pequeñas.
    private static View fila(Activity act, ViewGroup padre, Rutina r, boolean toca, TextosTarjeta textos,
                             EntradaTarjeta entrada) {
        View f = LayoutInflater.from(act).inflate(R.layout.item_elegir_rutina, padre, false);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(r.getNombre());
        f.findViewById(R.id.tvTocaHoy).setVisibility(toca ? View.VISIBLE : View.GONE);
        texto(f.findViewById(R.id.tvSub), textos.zonas(r.getMusculos()));
        texto(f.findViewById(R.id.tvUltima), textos.ultimaVezCorta(r.getUltimas()));
        BarrasVecesView barras = f.findViewById(R.id.barrasElegir);
        // Las barras solo si la última vez tuvo kilos (CifraTarjeta).
        List<VezRutina> u = r.getUltimas();
        boolean conKilos = !u.isEmpty() && CifraTarjeta.deVez(u.get(u.size() - 1)).conKilos();
        barras.setVeces(conKilos ? u : new java.util.ArrayList<>());
        entrada.pintar("r:" + r.getId(), null, 0, R.dimen.text_tarjeta_kg, textos, barras);
        return f;
    }

    // «, la que toca hoy. Pecho, espalda y brazos. La última vez, hace 6 días.»
    private static String resto(Activity act, Rutina r, boolean toca, TextosTarjeta textos) {
        StringBuilder a11y = new StringBuilder();
        if (toca) a11y.append(act.getString(R.string.tarjeta_toca_hoy_a11y));
        a11y.append(". ");
        String zonas = textos.zonas(r.getMusculos());
        if (!zonas.isEmpty()) a11y.append(zonas).append(". ");
        a11y.append(textos.ultimaVezCorta(r.getUltimas())).append('.');
        return a11y.toString();
    }

    // «Empezar sin rutina» o «Entreno libre»: el rayo delante y, en el de apuntar, su explicación.
    private static View sinRutina(Activity act, ViewGroup padre, int titulo, int sub, Runnable accion) {
        View f = LayoutInflater.from(act).inflate(R.layout.item_elegir_rutina, padre, false);
        f.setMinimumHeight(act.getResources().getDimensionPixelSize(R.dimen.fila_alto_min));
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        if (sub != 0) texto(f.findViewById(R.id.tvSub), act.getString(sub));
        ImageView icono = f.findViewById(R.id.ivIcono);
        icono.setImageResource(R.drawable.ic_ms_bolt);
        icono.setVisibility(View.VISIBLE);
        f.setContentDescription(sub != 0 ? act.getString(titulo) + ". " + act.getString(sub) : act.getString(titulo));
        f.setOnClickListener(v -> accion.run());
        return f;
    }

    // En «¿Qué entrenaste?» cada fila es un botón de opción, con 8 dp a los lados para que la
    // elegida enseñe su fondo redondeado.
    private static void apuntarEstilo(Activity act, View f, boolean elegida) {
        int lado = act.getResources().getDimensionPixelSize(R.dimen.espacio_8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(lado);
        lp.setMarginEnd(lado);
        f.setLayoutParams(lp);
        f.setPaddingRelative(act.getResources().getDimensionPixelSize(R.dimen.espacio_12), f.getPaddingTop(),
                act.getResources().getDimensionPixelSize(R.dimen.espacio_12), f.getPaddingBottom());
        if (elegida) f.setBackgroundResource(R.drawable.bg_fila_elegida);
        ViewCompat.setAccessibilityDelegate(f, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.RadioButton");
                info.setCheckable(true);
                info.setChecked(elegida);
            }
        });
    }

    private static View titulo(Activity act, ViewGroup padre, String texto, boolean conAire) {
        TextView tv = new TextView(act);
        tv.setText(texto);
        tv.setTextAppearance(R.style.TextAppearance_GymProFit_Apoyo_Pequeno);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(act, R.color.gp_text_secondary));
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        ViewCompat.setAccessibilityHeading(tv, true);
        int lado = act.getResources().getDimensionPixelSize(R.dimen.margen_pantalla);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(lado);
        lp.setMarginEnd(lado);
        lp.topMargin = act.getResources().getDimensionPixelSize(conAire ? R.dimen.espacio_16 : R.dimen.espacio_8);
        lp.bottomMargin = act.getResources().getDimensionPixelSize(R.dimen.espacio_4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static View raya(Activity act, ViewGroup padre) {
        View v = new View(act);
        v.setBackgroundColor(androidx.core.content.ContextCompat.getColor(act, R.color.gp_divider));
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int lado = act.getResources().getDimensionPixelSize(R.dimen.margen_pantalla);
        int arriba = act.getResources().getDimensionPixelSize(R.dimen.espacio_12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                act.getResources().getDimensionPixelSize(R.dimen.separador_alto));
        lp.setMarginStart(lado);
        lp.setMarginEnd(lado);
        lp.topMargin = arriba;
        lp.bottomMargin = arriba;
        v.setLayoutParams(lp);
        return v;
    }

    private static void texto(@NonNull TextView tv, @Nullable String s) {
        tv.setText(s);
        tv.setVisibility(s == null || s.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
