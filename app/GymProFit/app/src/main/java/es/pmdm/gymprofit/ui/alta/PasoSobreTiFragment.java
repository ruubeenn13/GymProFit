package es.pmdm.gymprofit.ui.alta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.ReglasEdad;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// PasoSobreTiFragment — «Sobre ti», en una pantalla (GP-103, tablero 4 del lienzo).
//
// Cinco filas —sexo, edad, altura, peso y actividad— que abren su hoja (HojasAlta). Lo
// elegido va al borrador al pulsar «Listo» de la hoja. La edad va de 14 a 100, el mínimo
// de la política (DEC-038).
//
// «Siguiente», con las cinco. «Prefiero no decirlo» borra lo que hubiera de «Sobre ti»
// y sigue: el plan sale sin calorías, que se añaden luego en el perfil (decisión 4).
// ============================================================
public class PasoSobreTiFragment extends PasoAltaFragment {

    /** Altura y edad con que empiezan sus hojas si aún no hay nada. */
    private static final int ALTURA_INICIAL = 170;
    private static final int EDAD_INICIAL = 30;
    private static final int ALTURA_MIN = 100;
    private static final int ALTURA_MAX = 250;

    private View filaSexo, filaEdad, filaAltura, filaPeso, filaActividad;

    @NonNull
    @Override
    protected AltaPasos.Paso paso() {
        return AltaPasos.Paso.SOBRE_TI;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paso_sobre_ti, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        filaSexo = view.findViewById(R.id.filaSexo);
        filaEdad = view.findViewById(R.id.filaEdad);
        filaAltura = view.findViewById(R.id.filaAltura);
        filaPeso = view.findViewById(R.id.filaPeso);
        filaActividad = view.findViewById(R.id.filaActividad);
        ((TextView) filaActividad.findViewById(R.id.tvSubFila)).setText(R.string.alta_actividad_sub);
        filaActividad.findViewById(R.id.tvSubFila).setVisibility(View.VISIBLE);
        view.findViewById(R.id.cardDatos).setContentDescription(getString(R.string.alta_sobre_ti_datos_a11y));

        filaSexo.setOnClickListener(v -> HojasAlta.sexo(requireContext(), prefs.getBorradorSexoElegido(), s -> {
            prefs.guardarBorradorSexo(s);
            alCambiar();
        }));
        filaEdad.setOnClickListener(v -> HojasAlta.numero(requireContext(), R.string.alta_edad_titulo,
                ReglasEdad.MINIMA, ReglasEdad.MAXIMA, prefs.getBorradorEdad(), EDAD_INICIAL, "", e -> {
                    prefs.guardarBorradorEdad(e);
                    alCambiar();
                }));
        filaAltura.setOnClickListener(v -> HojasAlta.numero(requireContext(), R.string.alta_altura_titulo,
                ALTURA_MIN, ALTURA_MAX, (int) Math.round(prefs.getBorradorAltura()), ALTURA_INICIAL,
                getString(R.string.alta_unidad_cm), a -> {
                    prefs.guardarBorradorAltura(a);
                    alCambiar();
                }));
        filaPeso.setOnClickListener(v -> HojasAlta.peso(requireContext(), prefs.getBorradorPeso(), p -> {
            prefs.guardarBorradorPeso(p);
            alCambiar();
        }));
        filaActividad.setOnClickListener(v -> HojasAlta.actividad(requireContext(), prefs.getBorradorActividad(), a -> {
            prefs.guardarBorradorActividad(a);
            alCambiar();
        }));

        MaterialButton noDecir = view.findViewById(R.id.btnPrefieroNoDecirlo);
        noDecir.setOnClickListener(v -> {
            prefs.borrarBorradorSobreTi();
            prefs.guardarBorradorSinDatos(true);
            alta().siguiente();
        });

        pintarFilas();
        prepararSiguiente(view.findViewById(R.id.btnSiguientePaso));
        View siguiente = view.findViewById(R.id.btnSiguientePaso);
        siguiente.setOnClickListener(v -> {
            if (!AltaPasos.puedeSeguir(paso(), prefs.getBorradorRespuestas())) return;
            prefs.guardarBorradorSinDatos(false);
            alta().siguiente();
        });
        cabecera(view, R.drawable.ic_ms_person_fill, R.string.alta_sobre_ti_titulo, R.string.alta_sobre_ti_texto,
                view.findViewById(R.id.cardDatos), siguiente, noDecir);
        Movimiento.entrar(260, filaSexo, filaEdad, filaAltura, filaPeso, filaActividad);
    }

    private void alCambiar() {
        if (!isAdded()) return;
        pintarFilas();
        refrescarSiguiente();
    }

    private void pintarFilas() {
        NumberFormat nf = NumberFormat.getInstance(FechaUtils.localeDeLaApp(requireContext()));
        nf.setMaximumFractionDigits(1);

        String sexo = prefs.getBorradorSexoElegido();
        fila(filaSexo, R.string.alta_sexo, sexo.isEmpty() ? null
                : getString("MUJER".equals(sexo) ? R.string.onboarding_mujer : R.string.onboarding_hombre));

        int edad = prefs.getBorradorEdad();
        fila(filaEdad, R.string.alta_edad, ReglasEdad.leer(String.valueOf(edad)) == null ? null
                : getResources().getQuantityString(R.plurals.alta_edad_valor, edad, edad));

        double altura = prefs.getBorradorAltura();
        fila(filaAltura, R.string.alta_altura, altura <= 0 ? null
                : getString(R.string.alta_altura_valor, (int) Math.round(altura)));

        String peso = prefs.getBorradorPeso();
        String pesoTexto = null;
        try {
            if (!peso.isEmpty()) pesoTexto = getString(R.string.alta_peso_valor, nf.format(Double.parseDouble(peso)));
        } catch (NumberFormatException e) {
            // Un peso ilegible en el borrador cuenta como sin contestar: se vuelve a pedir.
            pesoTexto = null;
        }
        fila(filaPeso, R.string.alta_peso, pesoTexto);

        String actividad = prefs.getBorradorActividad();
        fila(filaActividad, R.string.alta_actividad, actividad.isEmpty() ? null
                : UIHelper.traducirActividad(requireContext(), actividad));
    }

    // El valor a la derecha, o «Elegir» en naranja si falta; y la frase para TalkBack.
    private void fila(@NonNull View fila, @StringRes int nombre, @Nullable String valor) {
        ((TextView) fila.findViewById(R.id.tvTituloFila)).setText(nombre);
        TextView tvValor = fila.findViewById(R.id.tvValorFila);
        boolean hay = valor != null;
        tvValor.setText(hay ? valor : getString(R.string.alta_elegir));
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(hay ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : androidx.appcompat.R.attr.colorPrimary, tv, true);
        tvValor.setTextColor(tv.data);
        TextView sub = fila.findViewById(R.id.tvSubFila);
        String nombreTexto = getString(nombre);
        if (sub.getVisibility() == View.VISIBLE) nombreTexto = nombreTexto + ", " + sub.getText();
        fila.setContentDescription(hay ? getString(R.string.alta_fila_a11y, nombreTexto, valor)
                : getString(R.string.alta_fila_sin_elegir_a11y, nombreTexto));
    }
}
