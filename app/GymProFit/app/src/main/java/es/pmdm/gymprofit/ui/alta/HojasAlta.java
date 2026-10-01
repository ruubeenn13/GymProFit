package es.pmdm.gymprofit.ui.alta;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// HojasAlta — las hojas de «Sobre ti» (GP-103): sexo, edad, altura, peso y actividad.
//
// Hojas de Material que suben desde abajo, con el fondo velado. Cada una guarda al
// pulsar «Listo», no antes: cerrarla con atrás o tocando fuera no cambia nada.
//
// La de actividad es la del lienzo, con lo que cuenta cada opción (decisión 5: el factor
// de la fórmula ya incluye el ejercicio). Las de números usan el selector del sistema,
// que TalkBack lee y mueve sin nada más.
// ============================================================
public final class HojasAlta {

    private HojasAlta() {}

    /** Recibe lo elegido al pulsar «Listo». */
    public interface Elegido<T> { void listo(T valor); }

    /** Valores de actividad, en el orden de la hoja. */
    public static final String[] ACTIVIDADES = {
            CalculadoraNutricional.ACTIVIDAD_SEDENTARIO, CalculadoraNutricional.ACTIVIDAD_LIGERO,
            CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.ACTIVIDAD_ACTIVO
    };
    static final int[] ACTIVIDAD_TITULOS = {R.string.onboarding_sedentario, R.string.onboarding_ligero,
            R.string.onboarding_moderado, R.string.onboarding_activo};
    static final int[] ACTIVIDAD_DESCRIPCIONES = {R.string.actividad_sedentario_desc, R.string.actividad_ligero_desc,
            R.string.actividad_moderado_desc, R.string.actividad_activo_desc};

    /** «¿Cuánto te mueves?» */
    public static void actividad(@NonNull Context ctx, @NonNull String actual, @NonNull Elegido<String> alElegir) {
        opciones(ctx, R.string.alta_actividad_titulo, R.string.alta_actividad_texto,
                ACTIVIDADES, ACTIVIDAD_TITULOS, ACTIVIDAD_DESCRIPCIONES, actual, alElegir);
    }

    /** «¿Cuál es tu sexo?», para la fórmula de las calorías. */
    public static void sexo(@NonNull Context ctx, @NonNull String actual, @NonNull Elegido<String> alElegir) {
        opciones(ctx, R.string.alta_sexo_titulo, R.string.alta_sexo_texto,
                new String[]{"MUJER", "HOMBRE"},
                new int[]{R.string.onboarding_mujer, R.string.onboarding_hombre},
                new int[]{0, 0}, actual, alElegir);
    }

    private static void opciones(@NonNull Context ctx, @StringRes int titulo, @StringRes int texto,
                                 @NonNull String[] valores, @NonNull int[] titulos, @NonNull int[] descripciones,
                                 @NonNull String actual, @NonNull Elegido<String> alElegir) {
        BottomSheetDialog hoja = new BottomSheetDialog(ctx);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_alta_opciones, null, false);
        TextView tvTitulo = v.findViewById(R.id.tvTituloHoja);
        tvTitulo.setText(titulo);
        TextView tvTexto = v.findViewById(R.id.tvTextoHoja);
        tvTexto.setText(texto);
        tvTexto.setVisibility(View.VISIBLE);

        LinearLayout grupo = v.findViewById(R.id.grupoHoja);
        grupo.setContentDescription(ctx.getString(titulo));
        List<OpcionAlta> vistas = new ArrayList<>();
        String[] elegido = {actual};
        float d = ctx.getResources().getDisplayMetrics().density;
        for (int i = 0; i < valores.length; i++) {
            OpcionAlta o = new OpcionAlta(ctx);
            o.setEnHoja(true);
            o.setDatos(titulos[i], descripciones[i], 0, 0);
            o.setElegida(valores[i].equals(actual), false);
            String valor = valores[i];
            o.setOnClickListener(x -> {
                elegido[0] = valor;
                for (OpcionAlta otra : vistas) otra.setElegida(otra == o, true);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = Math.round(8 * d);
            grupo.addView(o, lp);
            vistas.add(o);
        }
        v.findViewById(R.id.btnListoHoja).setOnClickListener(x -> {
            // Sin elegir nada, «Listo» solo cierra.
            if (!elegido[0].isEmpty()) alElegir.listo(elegido[0]);
            hoja.dismiss();
        });
        mostrar(hoja, v, true);
        View[] entran = new View[vistas.size() + 2];
        entran[0] = tvTitulo;
        entran[1] = tvTexto;
        for (int i = 0; i < vistas.size(); i++) entran[i + 2] = vistas.get(i);
        Movimiento.entrar(180, entran);
    }

    /**
     * Un número entero: edad o altura.
     *
     * @param actual el contestado, o 0 para empezar en {@code inicial}.
     * @param unidad lo que se ve al lado («cm»), o "" para la edad.
     */
    public static void numero(@NonNull Context ctx, @StringRes int titulo, int min, int max, int actual,
                              int inicial, @NonNull String unidad, @NonNull Elegido<Integer> alElegir) {
        BottomSheetDialog hoja = new BottomSheetDialog(ctx);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_alta_numero, null, false);
        ((TextView) v.findViewById(R.id.tvTituloHoja)).setText(titulo);
        ((TextView) v.findViewById(R.id.tvUnidadHoja)).setText(unidad);
        NumberPicker np = v.findViewById(R.id.npEntero);
        np.setMinValue(min);
        np.setMaxValue(max);
        np.setValue(actual >= min && actual <= max ? actual : inicial);
        np.setWrapSelectorWheel(false);
        np.setContentDescription(ctx.getString(titulo));
        v.findViewById(R.id.btnListoHoja).setOnClickListener(x -> {
            alElegir.listo(np.getValue());
            hoja.dismiss();
        });
        mostrar(hoja, v, false);
    }

    /**
     * El peso, en kilos con un decimal.
     *
     * @param actual el contestado, con punto decimal, o "".
     * @param alElegir recibe el peso con punto decimal («62.5»).
     */
    public static void peso(@NonNull Context ctx, @NonNull String actual, @NonNull Elegido<String> alElegir) {
        BottomSheetDialog hoja = new BottomSheetDialog(ctx);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_alta_numero, null, false);
        ((TextView) v.findViewById(R.id.tvTituloHoja)).setText(R.string.alta_peso_titulo);
        ((TextView) v.findViewById(R.id.tvUnidadHoja)).setText(R.string.alta_unidad_kg);
        NumberPicker kilos = v.findViewById(R.id.npEntero);
        NumberPicker decimas = v.findViewById(R.id.npDecimal);
        decimas.setVisibility(View.VISIBLE);
        kilos.setMinValue(PESO_MIN);
        kilos.setMaxValue(PESO_MAX);
        kilos.setWrapSelectorWheel(false);
        char coma = DecimalFormatSymbols.getInstance(FechaUtils.localeDeLaApp(ctx)).getDecimalSeparator();
        String[] decimales = new String[10];
        for (int i = 0; i < 10; i++) decimales[i] = coma + String.valueOf(i);
        decimas.setMinValue(0);
        decimas.setMaxValue(9);
        decimas.setDisplayedValues(decimales);
        int kg = 70, dec = 0;
        try {
            if (!actual.isEmpty()) {
                double p = Double.parseDouble(actual);
                kg = (int) Math.floor(p);
                dec = (int) Math.round((p - kg) * 10);
                if (dec == 10) { kg++; dec = 0; }
            }
        } catch (NumberFormatException e) {
            // Un borrador ilegible empieza en 70: lo que importa es lo que se elija ahora.
            kg = 70;
            dec = 0;
        }
        kilos.setValue(Math.max(PESO_MIN, Math.min(PESO_MAX, kg)));
        decimas.setValue(dec);
        kilos.setContentDescription(ctx.getString(R.string.alta_peso_kilos_a11y));
        decimas.setContentDescription(ctx.getString(R.string.alta_peso_decimas_a11y));
        v.findViewById(R.id.btnListoHoja).setOnClickListener(x -> {
            alElegir.listo(kilos.getValue() + "." + decimas.getValue());
            hoja.dismiss();
        });
        mostrar(hoja, v, false);
    }

    /** El peso que se puede elegir: el mismo rango que Editar perfil y el onboarding de antes. */
    public static final int PESO_MIN = 30;
    public static final int PESO_MAX = 300;

    // Abierta del todo; las de números no se arrastran, para que el selector se mueva sin
    // cerrar la hoja.
    private static void mostrar(@NonNull BottomSheetDialog hoja, @NonNull View contenido, boolean arrastrable) {
        hoja.setContentView(contenido);
        BottomSheetBehavior<?> b = hoja.getBehavior();
        b.setSkipCollapsed(true);
        b.setState(BottomSheetBehavior.STATE_EXPANDED);
        b.setDraggable(arrastrable);
        hoja.show();
    }
}
