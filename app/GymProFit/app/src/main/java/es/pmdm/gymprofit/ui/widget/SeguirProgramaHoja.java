package es.pmdm.gymprofit.ui.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.VistaPrevia;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.TuPrograma;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// SeguirProgramaHoja — seguir un programa, o cambiar el tiempo del que sigue (lote 1.2.1).
//
// «¿Cuánto tiempo tienes por sesión?» con 30, 45, 60 y 75+: la primera vez 60, y al
// cambiar el tiempo el que tenía. Cada elección pide a la API la vista previa (la app no
// aplica ninguna regla) y enseña cada rutina con sus minutos y lo que se queda fuera. Si
// ya sigue otro programa, o este mismo, lo dice antes de pulsar: sus rutinas salen de
// Entrenar, o se rehacen y se pierden sus cambios; las sesiones y los récords se quedan.
// ============================================================
public final class SeguirProgramaHoja {

    private SeguirProgramaHoja() { }

    private static final int[] MINUTOS = {30, 45, 60, 75};

    /**
     * @param codigo   programa que se va a seguir.
     * @param nombre   su nombre, para los avisos.
     * @param seguido  el que sigue ahora, o null.
     * @param alSeguir se llama cuando la API lo ha creado.
     */
    public static void mostrar(FragmentActivity act, String codigo, String nombre,
                               @Nullable ProgramaQueSigue seguido, Runnable alSeguir) {
        ProgramaApi api = ApiClient.service(ProgramaApi.class);
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_seguir_programa, null, false);
        hoja.setContentView(raiz);
        hoja.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        hoja.getBehavior().setSkipCollapsed(true);

        boolean mismo = seguido != null && seguido.getPrograma() != null
                && codigo.equals(seguido.getPrograma().getCodigo());
        ChipGroup chips = raiz.findViewById(R.id.chipsMinutos);
        Map<Integer, Integer> minutosDe = new HashMap<>();
        int[] ids = {R.id.chip30, R.id.chip45, R.id.chip60, R.id.chip75};
        for (int i = 0; i < ids.length; i++) {
            minutosDe.put(ids[i], MINUTOS[i]);
            if (MINUTOS[i] != 75) {
                ((Chip) raiz.findViewById(ids[i])).setText(act.getString(R.string.seguir_chip_min, MINUTOS[i]));
            }
        }
        int inicial = mismo ? seguido.getMinutos() : 60;
        for (int id : ids) {
            if (minutosDe.get(id) == inicial) chips.check(id);
        }

        TextView aviso = raiz.findViewById(R.id.tvAvisoSeguir);
        View boton = raiz.findViewById(R.id.btnConfirmarSeguir);
        VistaEstado estado = new VistaEstado(raiz.findViewById(R.id.estadoVistaPrevia));
        // Solo cuenta la respuesta de la última elección: cambiar rápido de chip no mezcla.
        int[] pedida = {0};

        Runnable[] cargar = new Runnable[1];
        cargar[0] = () -> {
            int minutos = minutosDe.get(chips.getCheckedChipId());
            pintarAviso(act, aviso, boton, mismo, seguido, nombre, minutos);
            int esta = ++pedida[0];
            estado.cargando();
            ((LinearLayout) raiz.findViewById(R.id.listaVistaPrevia)).removeAllViews();
            raiz.findViewById(R.id.tvAjustes).setVisibility(View.GONE);
            boton.setEnabled(false);
            api.vistaPrevia(codigo, minutos).enqueue(new ApiCallback<VistaPrevia>() {
                @Override public void onOk(VistaPrevia vista) {
                    if (esta != pedida[0] || !hoja.isShowing()) return;
                    estado.oculto();
                    pintarVista(act, raiz, vista);
                    boton.setEnabled(true);
                }
                @Override public void onFail(int code, String message) {
                    if (esta != pedida[0] || !hoja.isShowing()) return;
                    estado.error(VistaEstado.mensaje(act, R.string.seguir_error_vista, code, message), cargar[0]);
                }
            });
        };
        chips.setOnCheckedStateChangeListener((g, marcados) -> cargar[0].run());

        boton.setOnClickListener(v -> {
            int minutos = minutosDe.get(chips.getCheckedChipId());
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("minutos", minutos);
            LoadingDialog.show(act);
            api.seguir(codigo, cuerpo).enqueue(new ApiCallback<Void>() {
                @Override public void onOk(Void body) {
                    LoadingDialog.hide(act);
                    hoja.dismiss();
                    UIHelper.mostrarToastExito(act, act.getString(R.string.seguir_hecho, nombre));
                    alSeguir.run();
                }
                @Override public void onFail(int code, String message) {
                    // La hoja sigue abierta con lo elegido: se puede volver a pulsar.
                    LoadingDialog.hide(act);
                    UiFeedback.toastError(act, code, message);
                }
            });
        });

        hoja.show();
        cargar[0].run();
    }

    // El aviso si ya sigue uno, y el texto del botón.
    private static void pintarAviso(FragmentActivity act, TextView aviso, View boton, boolean mismo,
                                    @Nullable ProgramaQueSigue seguido, String nombre, int minutos) {
        String texto = null;
        String minutosTxt = act.getString(R.string.duracion_min, minutos);
        if (mismo) {
            texto = act.getString(R.string.seguir_mismo, nombre, minutosTxt);
        } else if (seguido != null && seguido.getPrograma() != null) {
            texto = act.getString(R.string.seguir_ya_sigues_otro, seguido.getPrograma().getNombre());
        }
        aviso.setText(texto);
        aviso.setVisibility(texto == null ? View.GONE : View.VISIBLE);
        ((TextView) boton).setText(mismo ? act.getString(R.string.seguir_boton_mismo, minutosTxt)
                : act.getString(R.string.seguir_boton));
    }

    // Cada rutina con sus minutos y lo que cambia, y el ajuste del perfil.
    private static void pintarVista(FragmentActivity act, View raiz, VistaPrevia vista) {
        LinearLayout lista = raiz.findViewById(R.id.listaVistaPrevia);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(act);
        if (vista.getRutinas() != null) {
            for (VistaPrevia.Rutina r : vista.getRutinas()) {
                View fila = inflater.inflate(R.layout.item_vista_previa_rutina, lista, false);
                String titulo = act.getString(R.string.seguir_rutina_linea, r.getNombre(),
                        act.getString(R.string.duracion_min, r.getDuracionMinutos()));
                String detalle = detalle(act, r);
                ((TextView) fila.findViewById(R.id.tvVistaNombre)).setText(titulo);
                ((TextView) fila.findViewById(R.id.tvVistaDetalle)).setText(detalle);
                fila.setContentDescription(titulo + ". " + detalle);
                lista.addView(fila);
            }
        }

        List<String> ajustes = new ArrayList<>();
        if (vista.getAjustes() != null) {
            if (vista.getAjustes().contains(VistaPrevia.AJUSTE_AVANZADO)) {
                ajustes.add(act.getString(R.string.seguir_ajuste_avanzado));
            }
            if (vista.getAjustes().contains(VistaPrevia.AJUSTE_FUERZA)) {
                ajustes.add(act.getString(R.string.seguir_ajuste_fuerza));
            }
        }
        TextView tvAjustes = raiz.findViewById(R.id.tvAjustes);
        tvAjustes.setText(String.join(" ", ajustes));
        tvAjustes.setVisibility(ajustes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** «Sin A ni B · Básicos a 2 series», o «Completa» si no cambia nada. */
    static String detalle(FragmentActivity act, VistaPrevia.Rutina r) {
        List<String> partes = new ArrayList<>();
        if (r.getQuitados() != null && !r.getQuitados().isEmpty()) {
            partes.add(act.getString(R.string.seguir_sin, TuPrograma.enumerar(r.getQuitados(),
                    act.getString(R.string.seguir_separador), act.getString(R.string.seguir_conector))));
        }
        if (r.getSeriesBasicos() != null) {
            partes.add(act.getResources().getQuantityString(R.plurals.seguir_basicos, r.getSeriesBasicos(),
                    r.getSeriesBasicos()));
        }
        return partes.isEmpty() ? act.getString(R.string.seguir_completa)
                : String.join(act.getString(R.string.separador_punto), partes);
    }
}
