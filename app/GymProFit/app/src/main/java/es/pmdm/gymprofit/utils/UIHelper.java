package es.pmdm.gymprofit.utils;

// bg_dialog eliminado; fondo del diálogo aplicado por código para evitar crash con ?attr en drawables
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.PopupWindow;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import es.pmdm.gymprofit.R;

// ============================================================
// UIHelper — utilidades estáticas de UI compartidas por toda la app.
// Ofrece toasts personalizados con icono, diálogos de confirmación con fondo
// redondeado y un menú anclado (popup) tipo "acciones", evitando duplicar
// este código de bajo nivel en cada Activity.
// ============================================================
public class UIHelper {

    // TOASTS

    // Muestra un toast personalizado con icono informativo.
    public static void mostrarToastInfo(Context context, String mensaje) {
        mostrarToast(context, mensaje, R.drawable.ic_info);
    }

    // Muestra un toast personalizado con icono de éxito.
    public static void mostrarToastExito(Context context, String mensaje) {
        mostrarToast(context, mensaje, R.drawable.ic_check);
    }

    // Muestra un toast personalizado con icono de error.
    public static void mostrarToastError(Context context, String mensaje) {
        mostrarToast(context, mensaje, R.drawable.ic_error);
    }

    // Infla el layout toast_custom, rellena icono/mensaje y lo muestra en pantalla.
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

    // Muestra un diálogo de confirmación simple (sin icono) con botones cancelar/confirmar.
    public static void mostrarDialogo(Context context, String titulo, String mensaje, Runnable onConfirmar) {
        mostrarDialogoConIcono(context, titulo, mensaje, -1, onConfirmar);
    }

    // Muestra un diálogo de confirmación con icono opcional, fondo redondeado (colorSurface)
    // y ancho ajustado al 90% de la pantalla. Ejecuta onConfirmar solo si se pulsa confirmar.
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

    /**
     * Prepara un Dialog con el layout indicado, fondo redondeado colorSurface y sin título de sistema.
     * El caller configura los botones y llama a dialog.show() + ajustarAnchoDialogo().
     */
    public static Dialog prepararDialogoFormulario(Context context, View dialogView) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        LinearLayout root = dialogView.findViewById(R.id.dialogRoot);
        if (root != null) {
            TypedValue tv = new TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurface, tv, true);
            GradientDrawable fondo = new GradientDrawable();
            fondo.setShape(GradientDrawable.RECTANGLE);
            fondo.setCornerRadius(dpToPx(context, 20));
            fondo.setColor(tv.data);
            root.setBackground(fondo);
        }

        return dialog;
    }

    /** Muestra el dialog y ajusta su ancho al 90% de la pantalla. */
    public static void mostrarDialogoFormulario(Context context, Dialog dialog) {
        dialog.show();
        if (dialog.getWindow() != null) {
            int ancho = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(ancho, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // MENÚ ANCLADO

    public static final class MenuAction {
        public final int iconRes;
        public final String label;
        public final boolean destructive;
        public final Runnable action;

        public MenuAction(int iconRes, String label, boolean destructive, Runnable action) {
            this.iconRes = iconRes;
            this.label = label;
            this.destructive = destructive;
            this.action = action;
        }

        public MenuAction(int iconRes, String label, Runnable action) {
            this(iconRes, label, false, action);
        }

        public MenuAction(String label, Runnable action) {
            this(-1, label, false, action);
        }
    }

    /**
     * Muestra un PopupWindow anclado al view indicado, alineado a su borde derecho.
     * Aparece debajo del anchor; si no hay espacio, PopupWindow lo invierte automáticamente.
     *
     * @param anchor Vista que dispara el menú (botón 3 puntitos o itemView del long-press).
     * @param title  Título opcional (null para omitir).
     * @param actions Lista de acciones a mostrar.
     */
    public static void mostrarMenuAnclado(Context context, View anchor, String title, List<MenuAction> actions) {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_menu, null);

        TextView tvTitle = root.findViewById(R.id.tvMenuTitle);
        View divider = root.findViewById(R.id.dividerMenuTitle);
        LinearLayout llItems = root.findViewById(R.id.llMenuItems);

        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }

        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
        int colorNormal = tv.data;
        tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, tv, true);
        int colorDestructive = tv.data;
        tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, tv, true);
        int colorDivider = tv.data;

        final PopupWindow[] popupRef = new PopupWindow[1];

        boolean separadorAnadido = false;
        for (MenuAction a : actions) {
            if (a.destructive && !separadorAnadido) {
                View sep = new View(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(context, 1));
                lp.setMargins(dpToPx(context, 16), dpToPx(context, 4),
                        dpToPx(context, 16), dpToPx(context, 4));
                sep.setLayoutParams(lp);
                sep.setBackgroundColor(colorDivider);
                llItems.addView(sep);
                separadorAnadido = true;
            }

            View row = LayoutInflater.from(context).inflate(R.layout.item_menu_bottom, llItems, false);
            ImageView icon = row.findViewById(R.id.ivMenuItemIcon);
            TextView label = row.findViewById(R.id.tvMenuItemLabel);

            int textColor = a.destructive ? colorDestructive : colorNormal;
            label.setText(a.label);
            label.setTextColor(textColor);

            if (a.iconRes != -1) {
                icon.setImageResource(a.iconRes);
                icon.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
            } else {
                icon.setVisibility(View.GONE);
            }

            row.setOnClickListener(v -> {
                if (popupRef[0] != null) popupRef[0].dismiss();
                if (a.action != null) a.action.run();
            });

            llItems.addView(row);
        }

        tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(context, 12));
        bg.setColor(tv.data);
        root.setBackground(bg);

        root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int popupWidth = Math.min(
                Math.max(root.getMeasuredWidth(), dpToPx(context, 180)),
                (int) (screenWidth * 0.9));

        PopupWindow popup = new PopupWindow(
                root, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(dpToPx(context, 8));
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setAnimationStyle(androidx.appcompat.R.style.Animation_AppCompat_DropDownUp);
        popupRef[0] = popup;

        int[] anchorLoc = new int[2];
        anchor.getLocationOnScreen(anchorLoc);
        int xOffset = anchor.getWidth() - popupWidth;
        // Clamp so popup never goes off-screen left
        if (anchorLoc[0] + xOffset < 0) xOffset = -anchorLoc[0];
        popup.showAsDropDown(anchor, xOffset, 0);
    }

    // Convierte dp a píxeles según la densidad de pantalla del contexto.
    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}