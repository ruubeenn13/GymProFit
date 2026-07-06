package es.pmdm.gymprofit.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.util.WeakHashMap;

import es.pmdm.gymprofit.R;

// ============================================================
// LoadingDialog — spinner de carga modal reutilizable (overlay).
// Se muestra ANTES de un enqueue de red y se oculta en onOk/onFail, sin tocar
// el layout de cada pantalla (es un Dialog independiente). Una instancia por
// Activity, guardada en un WeakHashMap para no fugar el contexto. Cancelable=false:
// el usuario no puede cerrarlo tocando fuera (bloquea la interacción mientras carga).
// Patrón calcado de UIHelper: fondo redondeado colorSurface aplicado por código.
// ============================================================
public final class LoadingDialog {

    private LoadingDialog() {}

    // Un diálogo vivo por Activity; WeakHashMap evita retener la Activity destruida.
    private static final WeakHashMap<Activity, Dialog> DIALOGOS = new WeakHashMap<>();

    // Muestra el spinner con el mensaje genérico "Cargando…".
    public static void show(Activity activity) {
        show(activity, activity != null ? activity.getString(R.string.feedback_cargando) : "");
    }

    // Variante para cold-start de Render: mensaje "Activando el servidor…".
    public static void showColdStart(Activity activity) {
        show(activity, activity != null ? activity.getString(R.string.feedback_cargando_servidor) : "");
    }

    // Crea (o reutiliza) el diálogo de la Activity y lo muestra con el mensaje dado.
    public static void show(Activity activity, String mensaje) {
        if (!esUsable(activity)) return;

        // Si ya hay uno visible para esta Activity, solo actualiza el mensaje.
        Dialog existente = DIALOGOS.get(activity);
        if (existente != null && existente.isShowing()) {
            TextView tv = existente.findViewById(R.id.tvLoadingMensaje);
            if (tv != null) tv.setText(mensaje);
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_loading);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        // Fondo redondeado con el color de superficie del tema (claro/oscuro).
        View root = dialog.findViewById(R.id.dialogRoot);
        if (root != null) {
            TypedValue tv = new TypedValue();
            activity.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurface, tv, true);
            GradientDrawable fondo = new GradientDrawable();
            fondo.setShape(GradientDrawable.RECTANGLE);
            fondo.setCornerRadius(dpToPx(activity, 20));
            fondo.setColor(tv.data);
            root.setBackground(fondo);
        }

        TextView tvMensaje = dialog.findViewById(R.id.tvLoadingMensaje);
        if (tvMensaje != null) tvMensaje.setText(mensaje);

        DIALOGOS.put(activity, dialog);
        dialog.show();
    }

    // Oculta y descarta el diálogo de la Activity si existe. Seguro llamar varias veces.
    public static void hide(Activity activity) {
        if (activity == null) return;
        Dialog dialog = DIALOGOS.remove(activity);
        if (dialog != null && dialog.isShowing() && esUsable(activity)) {
            dialog.dismiss();
        }
    }

    // La Activity debe existir y no estar terminando/destruida antes de tocar su ventana.
    private static boolean esUsable(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    // Convierte dp a píxeles según la densidad de pantalla.
    private static int dpToPx(Activity activity, int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }
}
