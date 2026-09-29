package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

// ============================================================
// CampoContrasena — campo de contraseña que no expone lo escrito mientras está oculto
// (GP-131).
//
// Mientras la contraseña está oculta, el nodo de accesibilidad lleva el texto
// enmascarado, un punto por carácter, igual que un campo de contraseña de Android sin
// Material. Con el ojo abierto lleva el texto real, que ya se ve en pantalla. La pista,
// el error y lo demás que Material pone en el nodo no se tocan.
//
// Se corrige después de todo lo demás: onInitializeAccessibilityNodeInfo de la vista
// es quien llama al delegado de TextInputLayout, así que al volver de super el nodo ya
// está completo y solo queda cambiar el texto.
// ============================================================
public class CampoContrasena extends TextInputEditText {

    /** El mismo punto que dibuja {@link PasswordTransformationMethod}. */
    private static final char PUNTO = '•';

    public CampoContrasena(@NonNull Context context) {
        super(context);
    }

    public CampoContrasena(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CampoContrasena(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (!oculta()) return;
        Editable escrito = getText();
        info.setText(enmascarar(info.getText(), escrito == null ? "" : escrito.toString()));
    }

    /** La contraseña está oculta mientras el campo la dibuja con puntos. */
    public boolean oculta() {
        return getTransformationMethod() instanceof PasswordTransformationMethod;
    }

    /**
     * El texto del nodo con la contraseña cambiada por puntos.
     *
     * <p>Material pone el texto escrito al principio del nodo; por debajo de Android 8
     * le añade detrás «, pista». Se cambia solo ese principio, y lo que venga detrás se
     * conserva. Si el nodo no empieza por lo escrito (campo vacío: lleva la pista), se
     * devuelve tal cual.
     *
     * @param delNodo el texto que ya lleva el nodo.
     * @param escrito la contraseña escrita.
     * @return el texto que tiene que llevar el nodo.
     */
    @Nullable
    static CharSequence enmascarar(@Nullable CharSequence delNodo, @NonNull String escrito) {
        if (delNodo == null || escrito.isEmpty()) return delNodo;
        String texto = delNodo.toString();
        if (!texto.startsWith(escrito)) return delNodo;
        StringBuilder puntos = new StringBuilder(escrito.length());
        for (int i = 0; i < escrito.length(); i++) puntos.append(PUNTO);
        return puntos.append(texto.substring(escrito.length())).toString();
    }
}
