package es.pmdm.gymprofit.ui.widget;

import android.content.Intent;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.activities.DetalleRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EditarRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EditarRutinaAdminActivity;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// MenuRutina — abrir el detalle de una rutina y su menú de pulsación larga, tal como
// estaban en la pestaña Rutinas (GP-105 lo reparte entre Entrenar y Plantillas):
//   · rutina propia: editar o eliminar;
//   · plantilla (solo ADMIN): editar o activar/desactivar.
// ============================================================
public final class MenuRutina {

    private MenuRutina() { }

    /** Abre el detalle de la rutina con sus datos en extras. */
    public static void abrirDetalle(FragmentActivity act, ActivityResultLauncher<Intent> launcher, Rutina rutina) {
        Intent intent = new Intent(act, DetalleRutinaActivity.class);
        intent.putExtra("rutinaId",      rutina.getId());
        intent.putExtra("nombre",        rutina.getNombre());
        intent.putExtra("descripcion",   rutina.getDescripcion());
        intent.putExtra("nivel",         rutina.getNivel());
        intent.putExtra("duracion",      rutina.getDuracionMinutos());
        intent.putExtra("numEjercicios", rutina.getNumEjercicios());
        intent.putExtra("predefinida",   rutina.isPredefinida());
        intent.putExtra("usuarioId",     rutina.getUsuarioId());
        // De qué programa es, si es de uno (lote 1.2.1); null en las propias.
        intent.putExtra(DetalleRutinaActivity.EXTRA_PROGRAMA_NOMBRE, rutina.getProgramaNombre());
        launcher.launch(intent);
    }

    /**
     * Menú contextual de la rutina.
     *
     * @param editar   launcher de las pantallas de edición.
     * @param alCambiar se llama cuando la rutina se ha borrado o activado/desactivado.
     */
    public static void mostrar(FragmentActivity act, View ancla, Rutina rutina,
                               ActivityResultLauncher<Intent> editar, Runnable alCambiar) {
        List<UIHelper.MenuAction> acciones = new ArrayList<>();

        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit, act.getString(R.string.rutinas_editar), () -> {
            Intent intent;
            if (rutina.isPredefinida()) {
                intent = new Intent(act, EditarRutinaAdminActivity.class);
                intent.putExtra("id",              rutina.getId());
                intent.putExtra("nombre",          rutina.getNombre());
                intent.putExtra("descripcion",     rutina.getDescripcion());
                intent.putExtra("nivel",           rutina.getNivel());
                intent.putExtra("duracionMinutos", rutina.getDuracionMinutos());
                intent.putExtra("categoria",       rutina.getCategoria());
                intent.putExtra("diasSemana",      rutina.getDiasSemana());
            } else {
                intent = new Intent(act, EditarRutinaActivity.class);
                intent.putExtra("rutinaId",    rutina.getId());
                intent.putExtra("nombre",      rutina.getNombre());
                intent.putExtra("descripcion", rutina.getDescripcion());
                intent.putExtra("nivel",       rutina.getNivel());
                intent.putExtra("duracion",    rutina.getDuracionMinutos());
            }
            editar.launch(intent);
        }));

        if (rutina.isPredefinida()) {
            int icono = rutina.isActiva() ? R.drawable.ic_ms_visibility_off : R.drawable.ic_ms_check;
            String texto = act.getString(rutina.isActiva() ? R.string.rutinas_desactivar : R.string.rutinas_activar);
            acciones.add(new UIHelper.MenuAction(icono, texto, () -> alternarActiva(act, rutina, alCambiar)));
        } else {
            acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, act.getString(R.string.rutinas_eliminar), true,
                    () -> UIHelper.mostrarDialogoConIcono(act,
                            act.getString(R.string.rutinas_eliminar),
                            act.getString(R.string.rutinas_confirmar_eliminar),
                            R.drawable.ic_ms_delete,
                            () -> eliminar(act, rutina, alCambiar))));
        }

        UIHelper.mostrarMenuAnclado(act, ancla, rutina.getNombre(), acciones);
    }

    // Activa o desactiva una plantilla (solo ADMIN).
    private static void alternarActiva(FragmentActivity act, Rutina rutina, Runnable alCambiar) {
        RutinaApi api = ApiClient.service(RutinaApi.class);
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override public void onOk(Void body) {
                UIHelper.mostrarToastExito(act, act.getString(R.string.admin_exito_toggle_rutina));
                alCambiar.run();
            }
            @Override public void onFail(int code, String message) {
                UIHelper.mostrarToastError(act, act.getString(R.string.error_conexion));
            }
        };
        if (rutina.isActiva()) api.eliminar(rutina.getId()).enqueue(cb);
        else api.activar(rutina.getId()).enqueue(cb);
    }

    // Elimina una rutina propia.
    private static void eliminar(FragmentActivity act, Rutina rutina, Runnable alCambiar) {
        ApiClient.service(RutinaApi.class).eliminar(rutina.getId()).enqueue(new ApiCallback<Void>() {
            @Override public void onOk(Void body) {
                UIHelper.mostrarToastExito(act, act.getString(R.string.rutinas_eliminada_exito));
                alCambiar.run();
            }
            @Override public void onFail(int code, String message) {
                UIHelper.mostrarToastError(act, act.getString(R.string.error_conexion));
            }
        });
    }
}
