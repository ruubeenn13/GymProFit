package es.pmdm.gymprofit.utils;

import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import es.pmdm.gymprofit.R;

// ============================================================
// CamposMacro — leer los macros de crear y editar alimento (GP-142).
//
// Un macro vacío es 0; uno que no se entiende marca su campo con el error y el
// formulario no se guarda. Antes se guardaba un 0 sin decir nada.
// ============================================================
public final class CamposMacro {

    private CamposMacro() { }

    /**
     * Lee los campos en orden y marca los que no se entienden.
     *
     * @return los valores, o null si alguno no se entiende (el primero queda con el foco).
     */
    @Nullable
    public static double[] leer(@NonNull android.widget.EditText... campos) {
        double[] valores = new double[campos.length];
        android.widget.EditText primero = null;
        for (int i = 0; i < campos.length; i++) {
            android.widget.EditText et = campos[i];
            Double v = Numeros.macro(et.getText() != null ? et.getText().toString() : null);
            TextInputLayout til = contenedor(et);
            String error = v == null ? et.getContext().getString(R.string.error_numero_invalido) : null;
            if (til != null) til.setError(error);
            else et.setError(error);
            if (v == null) {
                if (primero == null) primero = et;
            } else {
                valores[i] = v;
            }
        }
        if (primero != null) {
            primero.requestFocus();
            return null;
        }
        return valores;
    }

    @Nullable
    private static TextInputLayout contenedor(View v) {
        ViewParent p = v.getParent();
        while (p != null) {
            if (p instanceof TextInputLayout) return (TextInputLayout) p;
            p = p.getParent();
        }
        return null;
    }
}
