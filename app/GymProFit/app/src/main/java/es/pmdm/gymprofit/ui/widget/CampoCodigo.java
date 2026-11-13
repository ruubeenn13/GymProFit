package es.pmdm.gymprofit.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.VerificacionCorreo;

// ============================================================
// CampoCodigo — el código de seis cifras en seis cajas (GP-045, diseño «Mira tu correo»).
//
// Por debajo es UN campo numérico (EditText) del ancho de las cajas, con el texto y el
// cursor transparentes: el que recibe el teclado, el autofill de código y lo que se
// pega, y el único que ve TalkBack («Código, 3 de 6 cifras»). Las seis cajas solo lo
// pintan, y no existen para la accesibilidad (DEC-019).
//
// Lo que se escribe o se pega se queda en sus cifras, seis como mucho: «123 456» entra
// como 123456. Al completar la sexta, avisa (se comprueba solo, no hay botón).
// En error, las seis cajas van en rojo; al tocar o escribir, se vacían.
// ============================================================
public class CampoCodigo extends FrameLayout {

    /** Lo que pasa cuando el código tiene sus seis cifras. */
    public interface AlCompletar {
        void completo(@NonNull String codigo);
    }

    private static final int CIFRAS = 6;

    private final TextView[] cajas = new TextView[CIFRAS];
    private final EditText campo;
    private boolean enError;
    @Nullable private AlCompletar alCompletar;

    public CampoCodigo(@NonNull Context context) {
        this(context, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    public CampoCodigo(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;

        LinearLayout fila = new LinearLayout(context);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        int alto = getResources().getDimensionPixelSize(R.dimen.codigo_caja_alto);
        int hueco = getResources().getDimensionPixelSize(R.dimen.espacio_8);
        for (int i = 0; i < CIFRAS; i++) {
            TextView caja = new TextView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, alto, 1f);
            if (i > 0) lp.setMarginStart(hueco);
            caja.setLayoutParams(lp);
            caja.setGravity(Gravity.CENTER);
            caja.setTypeface(ResourcesCompat.getFont(context, R.font.barlow_condensed_bold));
            caja.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_cifra_codigo));
            caja.setTextColor(ContextCompat.getColor(context, R.color.gp_on_surface));
            caja.setIncludeFontPadding(false);
            cajas[i] = caja;
            fila.addView(caja);
        }
        addView(fila, new LayoutParams(LayoutParams.MATCH_PARENT, alto));

        campo = new EditText(context);
        campo.setId(R.id.etCodigo);
        campo.setInputType(InputType.TYPE_CLASS_NUMBER);
        campo.setImeOptions(EditorInfo.IME_ACTION_DONE);
        campo.setFilters(new InputFilter[]{(fuente, ini, fin, destino, dIni, dFin) -> {
            // Solo cifras, y que el total no pase de seis.
            String cifras = VerificacionCorreo.soloCifras(fuente.subSequence(ini, fin));
            int cabe = CIFRAS - (destino.length() - (dFin - dIni));
            return cifras.length() > cabe ? cifras.substring(0, Math.max(0, cabe)) : cifras;
        }});
        campo.setBackground(null);
        campo.setTextColor(Color.TRANSPARENT);
        campo.setHintTextColor(Color.TRANSPARENT);
        campo.setCursorVisible(false);
        campo.setLongClickable(true);
        campo.setHint(R.string.verificar_correo_codigo_rotulo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            campo.setAutofillHints("smsOTPCode");
            campo.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
        }
        addView(campo, new LayoutParams(LayoutParams.MATCH_PARENT, alto));

        campo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                if (enError && e.length() > 0) quitarError();
                pintar();
                if (e.length() == CIFRAS && alCompletar != null) alCompletar.completo(e.toString());
            }
        });
        campo.setOnFocusChangeListener((v, foco) -> pintar());
        // Tras un error, tocar vacía las cajas para escribir otra vez.
        campo.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN && enError) limpiar();
            return false;
        });
        pintar();
    }

    /** Lo que se hace al completar las seis cifras. */
    public void setAlCompletar(@Nullable AlCompletar alCompletar) {
        this.alCompletar = alCompletar;
    }

    /** Las cifras escritas. */
    @NonNull
    public String getCodigo() {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    /** Pone un código (lo pegado), ya en sus cifras. Si son seis, se comprueba solo. */
    public void poner(@Nullable CharSequence texto) {
        campo.setText(VerificacionCorreo.soloCifras(texto));
        campo.setSelection(campo.length());
    }

    /** Vacía las cajas y quita el error. */
    public void limpiar() {
        enError = false;
        campo.setText(null);
        pintar();
    }

    /** Las seis cajas en rojo hasta que se toque o se escriba. */
    public void marcarError() {
        enError = true;
        pintar();
    }

    /** Deja el foco en el campo y abre el teclado. */
    public void enfocar() {
        campo.requestFocus();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(campo, 0);
    }

    /** El campo de verdad, para enganchar acciones del teclado. */
    @NonNull
    public EditText getCampo() {
        return campo;
    }

    private void quitarError() {
        enError = false;
    }

    private void pintar() {
        String t = getCodigo();
        int activa = campo.hasFocus() ? Math.min(t.length(), CIFRAS - 1) : -1;
        for (int i = 0; i < CIFRAS; i++) {
            cajas[i].setText(i < t.length() ? String.valueOf(t.charAt(i)) : "");
            int fondo = enError ? R.drawable.bg_caja_codigo_error
                    : (i == activa ? R.drawable.bg_caja_codigo_activa : R.drawable.bg_caja_codigo);
            cajas[i].setBackgroundResource(fondo);
            cajas[i].setTextColor(ContextCompat.getColor(getContext(),
                    enError ? R.color.gp_error : R.color.gp_on_surface));
        }
        // TalkBack: «Código, 3 de 6 cifras».
        ViewCompat.setStateDescription(campo,
                getResources().getString(R.string.verificar_correo_cifras_a11y, t.length(), CIFRAS));
    }
}
