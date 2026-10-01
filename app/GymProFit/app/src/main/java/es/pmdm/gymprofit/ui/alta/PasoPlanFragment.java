package es.pmdm.gymprofit.ui.alta;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.Recomendado;
import es.pmdm.gymprofit.model.programa.VistaPrevia;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.Enumeracion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.NivelVisible;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// PasoPlanFragment — «Tu plan», construido delante (GP-103, tablero 8 del lienzo).
//
//   · Cada día: calorías, macros y agua de CalculadoraNutricional, con lo contestado. Las
//     cifras cuentan y el anillo se dibuja (momento 7). De 14 a 17 años, «Perder grasa»
//     da las de mantenimiento y se dice aquí. Con «Prefiero no decirlo», sin calorías.
//   · Tu programa: GET /programas/recomendado y la vista previa con nivel, objetivo y
//     minutos, sin token. Es lo único que espera a la red, y tiene su carga y su error;
//     al llegar, sus rutinas entran de una en una y un brillo la cruza (momento 8).
//   · El aviso de salud y los términos de uso (https://gymprofit.app/terminos).
//
// «Guarda tu plan» se puede pulsar desde el primer momento, también sin programa: la
// cuenta se crea igual y el programa se ofrece después en Inicio.
// ============================================================
public class PasoPlanFragment extends PasoAltaFragment {

    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);
    private View raiz;
    private boolean animar;

    @NonNull
    @Override
    protected AltaPasos.Paso paso() {
        return AltaPasos.Paso.PLAN;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paso_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        raiz = view;
        // Al volver de «Guarda tu plan» o al girar, las cifras ya no cuentan otra vez.
        animar = savedInstanceState == null;
        AltaPasos.Respuestas r = prefs.getBorradorRespuestas();

        ((TextView) view.findViewById(R.id.tvAntetitulo)).setText(getString(R.string.alta_antetitulo,
                getString(R.string.alta_capitulo_plan), AltaPasos.Paso.PLAN.capitulo, AltaPasos.CAPITULOS));
        ((TextView) view.findViewById(R.id.tvTextoPaso)).setText(resumen(r));

        pintarCalorias(r);
        pintarAvisoSalud();

        MaterialButton boton = view.findViewById(R.id.btnGuardaPlan);
        boton.setText(alta().esCuentaExistente() ? R.string.alta_empezar : R.string.alta_guarda_tu_plan);
        boton.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(
                requireContext(), R.animator.toque));
        boton.setOnClickListener(v -> alta().siguiente());

        view.findViewById(R.id.btnReintentarPrograma).setOnClickListener(v -> cargarPrograma(r));
        cargarPrograma(r);

        if (animar) {
            Movimiento.entrar(0, view.findViewById(R.id.tvAntetitulo), view.findViewById(R.id.tvTituloPaso),
                    view.findViewById(R.id.tvTextoPaso));
            Movimiento.entrarUna(view.findViewById(R.id.cardCadaDia), 120, 400, 16);
            View carta = view.findViewById(R.id.cardPrograma);
            Movimiento.entrarUna(carta, 1150, Movimiento.CARTA, 28);
            Movimiento.entrarUna(view.findViewById(R.id.avisoSalud), 2000, Movimiento.ENTRA, 16);
            Movimiento.respirar(boton, 2400);
        }
    }

    // «Para ganar músculo, en el gimnasio, 3 días de 45 minutos.»
    private String resumen(@NonNull AltaPasos.Respuestas r) {
        String dias = getResources().getQuantityString(R.plurals.alta_plan_dias, r.dias, r.dias);
        return getString(R.string.alta_plan_resumen, getString(paraObjetivo(r.objetivo)),
                getString(dondeEnFrase(r.donde)), dias, r.minutos);
    }

    @StringRes
    private static int paraObjetivo(String objetivo) {
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO: return R.string.alta_para_perder;
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO: return R.string.alta_para_mantener;
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA: return R.string.alta_para_fuerza;
            default: return R.string.alta_para_musculo;
        }
    }

    @StringRes
    private static int dondeEnFrase(String donde) {
        switch (donde) {
            case Programa.MANCUERNAS: return R.string.alta_en_casa;
            case Programa.PESO_CORPORAL: return R.string.alta_con_tu_peso;
            default: return R.string.alta_en_el_gimnasio;
        }
    }

    // ── Cada día ────────────────────────────────────────────────────────────

    private void pintarCalorias(@NonNull AltaPasos.Respuestas r) {
        TextView nota = raiz.findViewById(R.id.tvNotaCalorias);
        View fila = raiz.findViewById(R.id.filaCalorias);
        if (!r.conCalorias()) {
            // «Prefiero no decirlo» (decisión 4): sin calorías, y dónde añadirlas.
            fila.setVisibility(View.GONE);
            nota.setText(R.string.alta_plan_sin_calorias);
            nota.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
            return;
        }
        double peso;
        try {
            peso = Double.parseDouble(r.peso);
        } catch (NumberFormatException e) {
            // AltaPasos ya exige un peso para llegar aquí con calorías; uno ilegible se
            // trata como «Prefiero no decirlo» antes que enseñar un número inventado.
            fila.setVisibility(View.GONE);
            nota.setText(R.string.alta_plan_sin_calorias);
            return;
        }
        ResultadoNutricional n = CalculadoraNutricional.calcular(peso, r.altura, r.edad,
                "HOMBRE".equals(r.sexo), r.actividad, r.objetivo);
        nota.setText(n.mantenimientoPorEdad ? getString(R.string.alta_plan_nota_menores) : getString(notaObjetivo(r.objetivo)));

        Locale idioma = FechaUtils.localeDeLaApp(requireContext());
        NumberFormat entero = NumberFormat.getIntegerInstance(idioma);
        NumberFormat litros = NumberFormat.getInstance(idioma);
        litros.setMinimumFractionDigits(1);
        litros.setMaximumFractionDigits(1);

        TextView kcal = raiz.findViewById(R.id.tvKcalPlan);
        TextView agua = raiz.findViewById(R.id.tvAguaPlan);
        TextView[] macros = {
                macro(R.id.macroProtPlan, R.string.alta_plan_proteina, R.color.gp_macro_proteinas),
                macro(R.id.macroCarbosPlan, R.string.alta_plan_carbohidratos, R.color.gp_macro_carbos),
                macro(R.id.macroGrasasPlan, R.string.alta_plan_grasa, R.color.gp_macro_grasas)
        };
        int[] gramos = {n.proteinas, n.carbohidratos, n.grasas};

        raiz.findViewById(R.id.marcoAnillo).setContentDescription(
                getString(R.string.alta_plan_kcal_a11y, entero.format(n.calorias)));
        raiz.findViewById(R.id.listaMacros).setContentDescription(getString(R.string.alta_plan_macros_a11y,
                n.proteinas, n.carbohidratos, n.grasas, litros.format(n.agua)));

        ((AnilloMacros) raiz.findViewById(R.id.anilloMacros)).mostrar(n.proteinas, n.carbohidratos, n.grasas, animar);
        Movimiento.Paso paso = f -> {
            kcal.setText(entero.format(Math.round(n.calorias * f)));
            for (int i = 0; i < 3; i++) {
                macros[i].setText(getString(R.string.alta_plan_gramos, Math.round(gramos[i] * f)));
            }
            agua.setText(getString(R.string.alta_plan_litros, litros.format(n.agua * f)));
        };
        if (animar) {
            paso.en(0f);
            Movimiento.contar(0, Movimiento.CIFRAS, paso);
            // La gota se llena (FILL de 0 a 1 en el lienzo): el relleno aparece sobre el contorno.
            View gota = raiz.findViewById(R.id.ivGota);
            gota.setAlpha(0f);
            gota.animate().alpha(1f).setStartDelay(1100).setDuration(700).start();
        } else {
            paso.en(1f);
        }
    }

    @StringRes
    private static int notaObjetivo(String objetivo) {
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO: return R.string.alta_plan_nota_perder;
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO: return R.string.alta_plan_nota_mantener;
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA: return R.string.alta_plan_nota_fuerza;
            default: return R.string.alta_plan_nota_musculo;
        }
    }

    // Un macro con su punto de color; devuelve dónde van los gramos.
    private TextView macro(int id, @StringRes int nombre, int colorRes) {
        View fila = raiz.findViewById(id);
        ((TextView) fila.findViewById(R.id.tvNombreMacro)).setText(nombre);
        GradientDrawable punto = new GradientDrawable();
        punto.setShape(GradientDrawable.OVAL);
        punto.setColor(ContextCompat.getColor(requireContext(), colorRes));
        fila.findViewById(R.id.puntoMacro).setBackground(punto);
        return fila.findViewById(R.id.tvValorMacro);
    }

    // ── Tu programa ─────────────────────────────────────────────────────────

    private void cargarPrograma(@NonNull AltaPasos.Respuestas r) {
        estadoPrograma(Estado.CARGANDO);
        String nivel = NivelVisible.valor(r.nivel);
        programaApi.recomendadoPara(r.donde, r.dias, nivel).enqueue(new ApiCallback<Recomendado>() {
            @Override
            public void onOk(Recomendado rec) {
                if (!isAdded()) return;
                if (rec == null || rec.getPrograma() == null) {
                    errorPrograma(404, null);
                    return;
                }
                Programa p = rec.getPrograma();
                prefs.guardarBorradorPrograma(p.getCodigo(), p.getNombre());
                programaApi.vistaPreviaPara(p.getCodigo(), r.minutos, nivel, r.objetivo)
                        .enqueue(new ApiCallback<VistaPrevia>() {
                            @Override
                            public void onOk(VistaPrevia vp) {
                                if (!isAdded()) return;
                                pintarPrograma(rec, vp, r);
                            }

                            @Override
                            public void onFail(int code, String message) {
                                if (!isAdded()) return;
                                errorPrograma(code, message);
                            }
                        });
            }

            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                errorPrograma(code, message);
            }
        });
    }

    private enum Estado { CARGANDO, ERROR, LISTO }

    private void estadoPrograma(@NonNull Estado e) {
        raiz.findViewById(R.id.cargaPrograma).setVisibility(e == Estado.CARGANDO ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.errorPrograma).setVisibility(e == Estado.ERROR ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.contenidoPrograma).setVisibility(e == Estado.LISTO ? View.VISIBLE : View.GONE);
    }

    // El error de la tarjeta, con «Reintentar». Con demasiadas peticiones (429, el cupo
    // de GP-148), cuánto esperar; sin red, que no hay red; si no, el genérico.
    private void errorPrograma(int code, @Nullable String message) {
        estadoPrograma(Estado.ERROR);
        TextView texto = raiz.findViewById(R.id.tvErrorPrograma);
        switch (es.pmdm.gymprofit.utils.ErrorAlta.programa(code)) {
            case ESPERA:
                texto.setText(es.pmdm.gymprofit.utils.UiFeedback.mensaje(requireContext(), code, message));
                break;
            case SIN_RED:
                texto.setText(R.string.alta_plan_programa_sin_red);
                break;
            default:
                texto.setText(R.string.alta_plan_programa_error);
        }
    }

    private void pintarPrograma(@NonNull Recomendado rec, @Nullable VistaPrevia vp, @NonNull AltaPasos.Respuestas r) {
        estadoPrograma(Estado.LISTO);
        Programa p = rec.getPrograma();
        Locale idioma = FechaUtils.localeDeLaApp(requireContext());
        ((TextView) raiz.findViewById(R.id.tvNombrePrograma)).setText(p.getNombre());
        ((TextView) raiz.findViewById(R.id.tvDetallePrograma)).setText(getString(R.string.alta_plan_detalle_programa,
                es.pmdm.gymprofit.ui.widget.TuProgramaVista.equipamiento(requireContext(), r.donde),
                getResources().getQuantityString(R.plurals.alta_plan_dias, r.dias, r.dias), r.minutos));
        TextView motivo = raiz.findViewById(R.id.tvMotivoPrograma);
        motivo.setText(rec.getMotivo());
        motivo.setVisibility(rec.getMotivo() == null || rec.getMotivo().isEmpty() ? View.GONE : View.VISIBLE);

        LinearLayout lista = raiz.findViewById(R.id.listaRutinasPlan);
        lista.removeAllViews();
        List<VistaPrevia.Rutina> rutinas = vp == null || vp.getRutinas() == null
                ? new ArrayList<>() : vp.getRutinas();
        List<View> filas = new ArrayList<>();
        for (VistaPrevia.Rutina rutina : rutinas) {
            View fila = getLayoutInflater().inflate(R.layout.item_rutina_plan, lista, false);
            ((TextView) fila.findViewById(R.id.tvNombreRutinaPlan)).setText(rutina.getNombre());
            String ejercicios = tresEjercicios(rutina, idioma);
            TextView tvEj = fila.findViewById(R.id.tvEjerciciosRutinaPlan);
            tvEj.setText(ejercicios);
            tvEj.setVisibility(ejercicios.isEmpty() ? View.GONE : View.VISIBLE);
            String minutos = getString(R.string.alta_minutos, rutina.getDuracionMinutos());
            ((TextView) fila.findViewById(R.id.tvMinutosRutinaPlan)).setText(minutos);
            fila.setContentDescription(getString(R.string.alta_plan_rutina_a11y, rutina.getNombre(),
                    rutina.getDuracionMinutos(), ejercicios));
            lista.addView(fila);
            filas.add(fila);
        }
        lista.setVisibility(rutinas.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) raiz.findViewById(R.id.tvDiasPrograma)).setText(diasDelPrograma(p, rutinas, r.dias, idioma));

        if (animar) {
            for (int i = 0; i < filas.size(); i++) {
                Movimiento.entrarUna(filas.get(i), 250 + i * Movimiento.FILA_ESCALON, Movimiento.FILA, 10);
            }
            Movimiento.brillar(raiz.findViewById(R.id.cardPrograma), 0x29FFB27A, 650, Movimiento.BRILLO);
        }
    }

    // «Sentadilla, press de banca y remo con barra»: los tres primeros.
    private String tresEjercicios(@NonNull VistaPrevia.Rutina rutina, @NonNull Locale idioma) {
        List<String> nombres = new ArrayList<>();
        if (rutina.getEjercicios() != null) {
            for (RutinaEjercicio e : rutina.getEjercicios()) {
                if (e.getNombreEjercicio() == null || e.getNombreEjercicio().isEmpty()) continue;
                nombres.add(nombres.isEmpty() ? e.getNombreEjercicio() : Enumeracion.enMedio(e.getNombreEjercicio(), idioma));
                if (nombres.size() == 3) break;
            }
        }
        return Enumeracion.unir(nombres, getString(R.string.enumeracion_y));
    }

    // «Lunes A, miércoles B y viernes C. En Entrenar las tienes todas, y en Inicio, la que toca.»
    private String diasDelPrograma(@NonNull Programa p, @NonNull List<VistaPrevia.Rutina> rutinas, int dias,
                                   @NonNull Locale idioma) {
        if (rutinas.isEmpty()) return getString(R.string.alta_plan_dias_sin_rutinas);
        String[] nombresDia = getResources().getStringArray(R.array.alta_dias_semana);
        int[] reparto = AltaPasos.repartirRutinas(dias, rutinas.size());
        List<String> partes = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            if (reparto[d] < 0) continue;
            partes.add(getString(R.string.alta_plan_dia_rutina, nombresDia[d],
                    AltaPasos.rotuloCorto(p.getNombre(), rutinas.get(reparto[d]).getNombre())));
        }
        String semana = Enumeracion.alPrincipio(Enumeracion.unir(partes, getString(R.string.enumeracion_y)), idioma);
        return getString(R.string.alta_plan_dias_texto, semana);
    }

    // ── Aviso de salud ──────────────────────────────────────────────────────

    // El aviso con «Términos de uso» pulsable dentro de la frase, como la privacidad del
    // registro: TalkBack lo anuncia como enlace y el resto no se vuelve un botón.
    private void pintarAvisoSalud() {
        TextView tv = raiz.findViewById(R.id.tvAvisoSalud);
        String enlace = getString(R.string.alta_terminos_enlace);
        String frase = getString(R.string.alta_plan_aviso_salud, enlace);
        SpannableString texto = new SpannableString(frase);
        int inicio = frase.indexOf(enlace);
        if (inicio >= 0) {
            texto.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    UIHelper.abrirUrl(requireContext(), getString(R.string.url_terminos));
                }
            }, inicio, inicio + enlace.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setLinkTextColor(ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorPrimary)));
        }
        tv.setText(texto);
    }

    private int color(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
