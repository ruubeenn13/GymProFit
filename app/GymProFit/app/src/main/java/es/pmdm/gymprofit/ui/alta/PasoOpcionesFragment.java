package es.pmdm.gymprofit.ui.alta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.NivelVisible;

// ============================================================
// PasoOpcionesFragment — las preguntas de una sola respuesta del alta (GP-103):
// tu objetivo, tu nivel y dónde entrenas, como sus tableros del lienzo.
//
//   · Objetivo: cuatro, con icono y lo que hace con tus calorías. Abre el capítulo 1.
//   · Nivel: tres, por el tiempo que llevas entrenando; sin icono, sigue el capítulo 1.
//     Experto ya no se ofrece: se enseña como Avanzado (NivelVisible).
//   · Dónde: gimnasio, mancuernas o peso corporal, con icono. Abre el capítulo 3.
//
// Elegir guarda en el borrador en el acto y enciende «Siguiente».
// ============================================================
public class PasoOpcionesFragment extends PasoAltaFragment {

    private static final String ARG_PASO = "paso";

    /** Una opción: el valor que se guarda, sus textos y sus iconos. */
    private static final class Opcion {
        final String valor;
        final int titulo, descripcion, contorno, relleno;

        Opcion(String valor, int titulo, int descripcion, int contorno, int relleno) {
            this.valor = valor;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.contorno = contorno;
            this.relleno = relleno;
        }
    }

    private final List<OpcionAlta> vistas = new ArrayList<>();
    private final List<Opcion> opciones = new ArrayList<>();

    /** La pantalla de una de las tres preguntas. */
    @NonNull
    public static PasoOpcionesFragment de(@NonNull AltaPasos.Paso paso) {
        PasoOpcionesFragment f = new PasoOpcionesFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_PASO, paso.ordinal());
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    protected AltaPasos.Paso paso() {
        return AltaPasos.Paso.de(requireArguments().getInt(ARG_PASO));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paso_opciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int icono, titulo, texto;
        switch (paso()) {
            case NIVEL:
                icono = 0;
                titulo = R.string.alta_nivel_titulo;
                texto = R.string.alta_nivel_texto;
                opciones.add(new Opcion("PRINCIPIANTE", R.string.nivel_principiante, R.string.alta_nivel_principiante_desc, 0, 0));
                opciones.add(new Opcion("INTERMEDIO", R.string.nivel_intermedio, R.string.alta_nivel_intermedio_desc, 0, 0));
                opciones.add(new Opcion("AVANZADO", R.string.nivel_avanzado, R.string.alta_nivel_avanzado_desc, 0, 0));
                break;
            case DONDE:
                icono = R.drawable.ic_ms_calendar_month_fill;
                titulo = R.string.alta_donde_titulo;
                texto = R.string.alta_donde_texto;
                opciones.add(new Opcion(Programa.GIMNASIO, R.string.equipamiento_gimnasio, R.string.alta_donde_gimnasio_desc,
                        R.drawable.ic_ms_fitness_center, R.drawable.ic_ms_fitness_center_fill));
                opciones.add(new Opcion(Programa.MANCUERNAS, R.string.equipamiento_mancuernas, R.string.alta_donde_mancuernas_desc,
                        R.drawable.ic_ms_home, R.drawable.ic_ms_home_fill));
                opciones.add(new Opcion(Programa.PESO_CORPORAL, R.string.equipamiento_peso_corporal, R.string.alta_donde_corporal_desc,
                        R.drawable.ic_ms_accessibility_new, R.drawable.ic_ms_accessibility_new_fill));
                break;
            default:
                icono = R.drawable.ic_ms_flag_fill;
                titulo = R.string.alta_objetivo_titulo;
                texto = R.string.alta_objetivo_texto;
                opciones.add(new Opcion(CalculadoraNutricional.OBJETIVO_PERDER_PESO, R.string.objetivo_perder_peso,
                        R.string.alta_objetivo_perder_desc, R.drawable.ic_ms_trending_down, R.drawable.ic_ms_trending_down_fill));
                opciones.add(new Opcion(CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR, R.string.objetivo_ganar_musculo,
                        R.string.alta_objetivo_musculo_desc, R.drawable.ic_ms_fitness_center, R.drawable.ic_ms_fitness_center_fill));
                opciones.add(new Opcion(CalculadoraNutricional.OBJETIVO_MANTENER_PESO, R.string.objetivo_mantener,
                        R.string.alta_objetivo_mantener_desc, R.drawable.ic_ms_balance, R.drawable.ic_ms_balance_fill));
                opciones.add(new Opcion(CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA, R.string.objetivo_fuerza,
                        R.string.alta_objetivo_fuerza_desc, R.drawable.ic_ms_bolt, R.drawable.ic_ms_bolt_fill));
        }

        LinearLayout grupo = view.findViewById(R.id.grupoOpciones);
        grupo.setContentDescription(getString(titulo));
        String guardado = guardado();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < opciones.size(); i++) {
            Opcion o = opciones.get(i);
            OpcionAlta v = new OpcionAlta(requireContext());
            v.setDatos(o.titulo, o.descripcion, o.contorno, o.relleno);
            v.setElegida(o.valor.equals(guardado), false);
            v.setOnClickListener(x -> elegir(o, v));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = Math.round(10 * d);
            grupo.addView(v, lp);
            vistas.add(v);
        }

        prepararSiguiente(view.findViewById(R.id.btnSiguientePaso));
        View[] entran = new View[vistas.size() + 1];
        for (int i = 0; i < vistas.size(); i++) entran[i] = vistas.get(i);
        entran[vistas.size()] = view.findViewById(R.id.btnSiguientePaso);
        cabecera(view, icono, titulo, texto, entran);
    }

    // Lo que hay en el borrador para esta pregunta; Experto cuenta como Avanzado.
    private String guardado() {
        switch (paso()) {
            case NIVEL: return NivelVisible.valor(prefs.getBorradorNivel());
            case DONDE: return prefs.getBorradorDonde();
            default:    return prefs.getBorradorObjetivo();
        }
    }

    private void elegir(@NonNull Opcion o, @NonNull OpcionAlta elegida) {
        switch (paso()) {
            case NIVEL: prefs.guardarBorradorNivel(o.valor); break;
            case DONDE: prefs.guardarBorradorDonde(o.valor); break;
            default:    prefs.guardarBorradorObjetivo(o.valor);
        }
        for (OpcionAlta v : vistas) v.setElegida(v == elegida, true);
        refrescarSiguiente();
    }
}
