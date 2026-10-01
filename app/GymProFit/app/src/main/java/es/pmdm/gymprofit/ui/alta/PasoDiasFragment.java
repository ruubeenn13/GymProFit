package es.pmdm.gymprofit.ui.alta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.AltaPasos;

// ============================================================
// PasoDiasFragment — «Días y tiempo» (GP-103, tablero 7 del lienzo).
//
// Dos selectores con píldora (momento 6 de DEC-039): de 2 a 6 días por semana, con la
// semana de ejemplo que se enciende día a día, y 30, 45, 60 o 75+ minutos por sesión,
// los que acepta la API al seguir un programa. Sin elegir, no hay píldora: «Ver mi plan»
// se enciende con los dos.
// ============================================================
public class PasoDiasFragment extends PasoAltaFragment {

    private SemanaEjemplo semana;
    private View filaEjemplo;

    @NonNull
    @Override
    protected AltaPasos.Paso paso() {
        return AltaPasos.Paso.DIAS;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paso_dias, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        semana = view.findViewById(R.id.semanaEjemplo);
        filaEjemplo = view.findViewById(R.id.filaEjemplo);

        SelectorPildora dias = view.findViewById(R.id.selectorDias);
        int n = AltaPasos.DIAS_MAX - AltaPasos.DIAS_MIN + 1;
        String[] textosDias = new String[n];
        String[] nombresDias = new String[n];
        for (int i = 0; i < n; i++) {
            int d = AltaPasos.DIAS_MIN + i;
            textosDias[i] = String.valueOf(d);
            nombresDias[i] = getResources().getQuantityString(R.plurals.alta_dias_a11y, d, d);
        }
        dias.setOpciones(textosDias, nombresDias, 56, true);
        dias.setContentDescription(getString(R.string.alta_dias_etiqueta));
        int diasGuardados = prefs.getBorradorDias();
        if (diasGuardados >= AltaPasos.DIAS_MIN && diasGuardados <= AltaPasos.DIAS_MAX) {
            dias.setElegida(diasGuardados - AltaPasos.DIAS_MIN, false);
            pintarSemana(diasGuardados, false);
        }
        dias.setAlElegir(i -> {
            int d = AltaPasos.DIAS_MIN + i;
            prefs.guardarBorradorDias(d);
            pintarSemana(d, true);
            refrescarSiguiente();
        });

        SelectorPildora minutos = view.findViewById(R.id.selectorMinutos);
        int m = AltaPasos.MINUTOS.length;
        String[] textosMin = new String[m];
        String[] nombresMin = new String[m];
        int guardados = -1;
        for (int i = 0; i < m; i++) {
            int min = AltaPasos.MINUTOS[i];
            boolean ultimo = i == m - 1;
            textosMin[i] = getString(ultimo ? R.string.alta_minutos_o_mas : R.string.alta_minutos, min);
            nombresMin[i] = getString(ultimo ? R.string.alta_minutos_o_mas_a11y : R.string.alta_minutos_a11y, min);
            if (min == prefs.getBorradorMinutos()) guardados = i;
        }
        minutos.setOpciones(textosMin, nombresMin, 52, false);
        minutos.setContentDescription(getString(R.string.alta_tiempo_etiqueta));
        if (guardados >= 0) minutos.setElegida(guardados, false);
        minutos.setAlElegir(i -> {
            prefs.guardarBorradorMinutos(AltaPasos.MINUTOS[i]);
            refrescarSiguiente();
        });

        View siguiente = view.findViewById(R.id.btnSiguientePaso);
        prepararSiguiente(view.findViewById(R.id.btnSiguientePaso));
        cabecera(view, 0, R.string.alta_dias_titulo, R.string.alta_dias_texto,
                view.findViewById(R.id.tvEtiquetaDias), dias, filaEjemplo,
                view.findViewById(R.id.tvEtiquetaTiempo), minutos, siguiente);
    }

    // La semana de ejemplo y su frase para TalkBack.
    private void pintarSemana(int dias, boolean animar) {
        filaEjemplo.setVisibility(View.VISIBLE);
        String[] frases = getResources().getStringArray(R.array.alta_ejemplo_dias);
        semana.mostrar(dias, animar, frases[dias - AltaPasos.DIAS_MIN]);
    }
}
