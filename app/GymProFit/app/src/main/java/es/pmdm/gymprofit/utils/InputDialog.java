package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import es.pmdm.gymprofit.R;

// ============================================================
// InputDialog — diálogo de introducción de datos ÚNICO y estilizado.
// Reemplaza los diálogos construidos a mano (AlertDialog.Builder + TextInputLayout
// programático) que se veían pobres. Usa MaterialAlertDialogBuilder (diálogo
// redondeado del tema) + un TextInputLayout outlined (dialog_input.xml). Un único
// punto de estilo para todos los formularios emergentes de la app.
// ============================================================
public final class InputDialog {

    private InputDialog() { }

    // Callback con el texto introducido al pulsar Guardar.
    public interface OnConfirm { void onConfirm(String valor); }

    // Diálogo numérico decimal.
    public static void numerico(Context ctx, String titulo, String hint, String valorInicial,
                                String sufijo, OnConfirm cb) {
        mostrar(ctx, titulo, hint, valorInicial, sufijo,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, false, cb);
    }

    // Diálogo de texto (multilínea).
    public static void texto(Context ctx, String titulo, String hint, String valorInicial, OnConfirm cb) {
        mostrar(ctx, titulo, hint, valorInicial, null,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, true, cb);
    }

    // Construye y muestra el diálogo estilizado.
    private static void mostrar(Context ctx, String titulo, String hint, String valorInicial,
                                String sufijo, int inputType, boolean multilinea, OnConfirm cb) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_input, null);
        TextInputLayout til = v.findViewById(R.id.tilInput);
        TextInputEditText et = v.findViewById(R.id.etInput);

        til.setHint(hint);
        if (sufijo != null) til.setSuffixText(sufijo);
        et.setInputType(inputType);
        if (multilinea) et.setSingleLine(false);
        if (valorInicial != null && !valorInicial.isEmpty()) {
            et.setText(valorInicial);
            et.setSelection(et.getText() != null ? et.getText().length() : 0);
        }

        new MaterialAlertDialogBuilder(ctx)
                .setTitle(titulo)
                .setView(v)
                .setPositiveButton(R.string.medicion_guardar, (d, w) ->
                        cb.onConfirm(et.getText() != null ? et.getText().toString().trim() : ""))
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }
}
