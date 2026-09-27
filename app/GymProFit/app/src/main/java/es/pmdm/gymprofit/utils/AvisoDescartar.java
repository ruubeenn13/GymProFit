package es.pmdm.gymprofit.utils;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;
import java.util.function.BooleanSupplier;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;

// ============================================================
// AvisoDescartar — GP-098: salir de un formulario ya no tira lo apuntado sin avisar.
//
// Al salir de Registrar sesión o del asistente o el editor de rutina, con atrás o con
// la flecha de la cabecera, si hay algo sin guardar se pregunta «¿Descartar lo que has
// apuntado?» (Descartar / Seguir editando). Sin cambios, sale directo, sin diálogo.
//
// Qué cuenta como «algo apuntado» lo decide cada pantalla; aquí están las piezas puras
// para decidirlo, que es lo que se prueba sin dispositivo.
// ============================================================
public final class AvisoDescartar {

    private AvisoDescartar() {}

    /**
     * Engancha el aviso a atrás (gesto y botón) y a la flecha de la cabecera.
     *
     * @param act        la pantalla del formulario.
     * @param cabecera   su cabecera, o null si no tiene flecha.
     * @param hayCambios dice, en el momento de salir, si hay algo sin guardar.
     */
    public static void instalar(AppCompatActivity act, Toolbar cabecera, BooleanSupplier hayCambios) {
        act.getOnBackPressedDispatcher().addCallback(act, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                salir(act, hayCambios);
            }
        });
        if (cabecera != null) cabecera.setNavigationOnClickListener(v -> salir(act, hayCambios));
    }

    // Sale directo si no hay nada que perder; si lo hay, pregunta antes.
    private static void salir(AppCompatActivity act, BooleanSupplier hayCambios) {
        if (!hayCambios.getAsBoolean()) {
            act.finish();
            return;
        }
        preguntar(act, act::finish);
    }

    /**
     * El mismo aviso en un diálogo con campos (GP-108): la cantidad al añadir un
     * alimento, una medida. Atrás, con algo escrito, pregunta antes de cerrar; tocar
     * fuera no lo cierra mientras haya algo escrito. «Cancelar» sigue cerrando sin
     * preguntar: ahí descartar es lo que se ha pedido.
     *
     * @param act        la pantalla que abre el diálogo.
     * @param dialogo    el diálogo ya creado.
     * @param hayCambios dice, al salir, si hay algo sin guardar.
     * @param campos     los campos del diálogo, para saber cuándo cambia.
     */
    public static void instalarEnDialogo(android.app.Activity act, androidx.appcompat.app.AlertDialog dialogo,
                                         BooleanSupplier hayCambios, android.widget.TextView... campos) {
        android.text.TextWatcher vigilante = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                dialogo.setCanceledOnTouchOutside(!hayCambios.getAsBoolean());
            }
        };
        for (android.widget.TextView campo : campos) campo.addTextChangedListener(vigilante);
        dialogo.setCanceledOnTouchOutside(!hayCambios.getAsBoolean());
        Runnable alVolver = () -> {
            if (!hayCambios.getAsBoolean()) dialogo.cancel();
            else preguntar(act, dialogo::dismiss);
        };
        // En Android 13+ el atrás de la ventana del diálogo va por su propio
        // OnBackInvokedDispatcher, y el de androidx no llegaba a él (comprobado en
        // API 36): se registra ahí, por delante del cierre por defecto del Dialog.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            dialogo.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, alVolver::run);
        } else {
            dialogo.getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    alVolver.run();
                }
            });
        }
    }

    // «¿Descartar lo que has apuntado?» (Descartar / Seguir editando).
    private static void preguntar(android.app.Activity act, Runnable descartar) {
        UIHelper.mostrarDialogoConIcono(act,
                act.getString(R.string.descartar_titulo),
                act.getString(R.string.descartar_mensaje),
                R.drawable.ic_ms_delete,
                act.getString(R.string.descartar_confirmar),
                act.getString(R.string.descartar_seguir),
                descartar);
    }

    /** Si alguno de los campos ya no tiene el texto con el que se abrió la pantalla. */
    public static boolean distintos(String[] iniciales, CharSequence... actuales) {
        for (int i = 0; i < actuales.length; i++) {
            if (distinto(i < iniciales.length ? iniciales[i] : null, actuales[i])) return true;
        }
        return false;
    }

    /** Si alguno de los textos lleva algo más que espacios. */
    public static boolean hayTexto(CharSequence... textos) {
        for (CharSequence t : textos) {
            if (t != null && t.toString().trim().length() > 0) return true;
        }
        return false;
    }

    /** Si el texto actual ya no es el que tenía el campo al abrir la pantalla. */
    public static boolean distinto(String inicial, CharSequence actual) {
        String a = actual == null ? "" : actual.toString().trim();
        String i = inicial == null ? "" : inicial.trim();
        return !a.equals(i);
    }

    /**
     * Si en Registrar sesión hay algo apuntado: duración, notas, valoración o alguna
     * serie tocada. Las series nacen con las repeticiones del plan ya puestas, así que
     * eso solo no cuenta; cuenta el peso, unas repeticiones distintas, marcarla hecha
     * o añadir o quitar series. Elegir otra rutina tampoco: no se ha tecleado nada.
     */
    public static boolean sesionConDatos(CharSequence duracion, CharSequence notas, float estrellas,
                                         List<EjercicioPesoAdapter.Item> items) {
        if (hayTexto(duracion, notas) || estrellas > 0f) return true;
        for (EjercicioPesoAdapter.Item item : items) {
            if (item.realizadas.size() != Math.max(1, item.series)) return true;
            String planReps = String.valueOf(item.repeticiones);
            for (EjercicioPesoAdapter.Serie s : item.realizadas) {
                if (s.completada || hayTexto(s.peso) || distinto(planReps, s.repeticiones)) return true;
            }
        }
        return false;
    }
}
