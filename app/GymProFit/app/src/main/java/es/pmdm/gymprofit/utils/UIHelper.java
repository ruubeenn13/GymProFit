package es.pmdm.gymprofit.utils;

// MODIFICADO - bg_dialog eliminado, el fondo del diálogo se aplica por código
// para evitar el crash con ?attr en drawables
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import es.pmdm.gymprofit.R;

public class UIHelper {

    // TOASTS

    public static void mostrarToastInfo(Context context, String mensaje) {
        mostrarToast(context, mensaje, R.drawable.ic_info);
    }

    public static void mostrarToastExito(Context context, String mensaje) {
        mostrarToast(context, mensaje, R.drawable.ic_check);
    }

    public static void mostrarToastError(Context context, String mensaje) {
        mostrarToast(context, mensaje, R.drawable.ic_error);
    }

    private static void mostrarToast(Context context, String mensaje, int iconoRes) {
        View view = LayoutInflater.from(context).inflate(R.layout.toast_custom, null);
        ImageView ivIcono = view.findViewById(R.id.ivToastIcono);
        TextView tvMensaje = view.findViewById(R.id.tvToastMensaje);
        ivIcono.setImageResource(iconoRes);
        tvMensaje.setText(mensaje);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.show();
    }

    // DIÁLOGOS

    public static void mostrarDialogo(Context context, String titulo, String mensaje, Runnable onConfirmar) {
        mostrarDialogoConIcono(context, titulo, mensaje, -1, onConfirmar);
    }

    public static void mostrarDialogoConIcono(Context context, String titulo, String mensaje, int iconoRes, Runnable onConfirmar) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        LinearLayout root = dialog.findViewById(R.id.dialogRoot);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        int colorSurface = typedValue.data;

        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(dpToPx(context, 20));
        fondo.setColor(colorSurface);
        root.setBackground(fondo);

        TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitulo);
        TextView tvMensaje = dialog.findViewById(R.id.tvDialogMensaje);
        ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcono);
        MaterialButton btnCancelar = dialog.findViewById(R.id.btnDialogCancelar);
        MaterialButton btnConfirmar = dialog.findViewById(R.id.btnDialogConfirmar);

        tvTitulo.setText(titulo);
        tvMensaje.setText(mensaje);

        if (iconoRes != -1) {
            ivIcono.setVisibility(View.VISIBLE);
            ivIcono.setImageResource(iconoRes);
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirmar != null) onConfirmar.run();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            int ancho = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(ancho, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}