package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.utils.TuPrograma;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// TuProgramaVista — pinta view_tu_programa, arriba de Entrenar (GP-074, lote 1.2.1).
//
// No decide nada: lo que toca y lo que va hecho lo calcula la API y lo reparte
// TuPrograma. Aquí solo se pinta y se pasan los toques a quien la usa (Acciones).
// ============================================================
public class TuProgramaVista {

    /** Lo que hace la pantalla con cada toque. */
    public interface Acciones {
        void elegirPrograma();
        void empezar(Rutina rutina);
        void abrirRutina(Rutina rutina);
        void menuRutina(View ancla, Rutina rutina);
        void menuPrograma(View ancla, ProgramaQueSigue seguido);
        void volverASeguir(ProgramaQueSigue seguido);
        void dejar();
    }

    private final Context ctx;
    private final View raiz;
    private final Acciones acciones;
    private final VistaEstado estado;

    public TuProgramaVista(View raiz, Acciones acciones) {
        this.ctx = raiz.getContext();
        this.raiz = raiz;
        this.acciones = acciones;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoPrograma));
        raiz.findViewById(R.id.cardElegirPrograma).setOnClickListener(v -> acciones.elegirPrograma());
    }

    public void cargando() {
        mostrar(false, false);
        estado.cargando();
    }

    public void error(CharSequence mensaje, Runnable reintentar) {
        mostrar(false, false);
        estado.error(mensaje, reintentar);
    }

    /** No sigue ningún programa: la tarjeta que lleva a Programas. */
    public void sinPrograma() {
        estado.oculto();
        mostrar(true, false);
    }

    /** Sigue uno: su tarjeta y el resto de sus rutinas. */
    public void pintar(ProgramaQueSigue seguido) {
        estado.oculto();
        mostrar(false, true);
        Programa p = seguido.getPrograma();

        ((TextView) raiz.findViewById(R.id.tvProgramaNombre)).setText(p.getNombre());
        String resumen = ctx.getString(R.string.tu_programa_resumen, equipamiento(ctx, p.getEquipamiento()),
                ctx.getString(R.string.duracion_min, seguido.getMinutos()));
        ((TextView) raiz.findViewById(R.id.tvProgramaResumen)).setText(resumen);
        raiz.findViewById(R.id.cabeceraPrograma).setContentDescription(p.getNombre() + ". " + resumen);
        View menu = raiz.findViewById(R.id.btnMenuPrograma);
        menu.setOnClickListener(v -> acciones.menuPrograma(v, seguido));

        pintarBarra(seguido);

        RutinaConEjercicios hoy = TuPrograma.hoy(seguido);
        raiz.findViewById(R.id.bloqueHoy).setVisibility(hoy != null ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.bloqueBorradas).setVisibility(hoy == null ? View.VISIBLE : View.GONE);
        if (hoy != null) {
            pintarHoy(hoy);
        } else {
            raiz.findViewById(R.id.btnVolverASeguir).setOnClickListener(v -> acciones.volverASeguir(seguido));
            raiz.findViewById(R.id.btnDejarPrograma).setOnClickListener(v -> acciones.dejar());
        }
        pintarResto(TuPrograma.resto(seguido));
    }

    // Visibilidad de las dos tarjetas y del resto.
    private void mostrar(boolean elegir, boolean programa) {
        raiz.findViewById(R.id.cardElegirPrograma).setVisibility(elegir ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.cardPrograma).setVisibility(programa ? View.VISIBLE : View.GONE);
        if (!programa) {
            raiz.findViewById(R.id.tvDespues).setVisibility(View.GONE);
            ((LinearLayout) raiz.findViewById(R.id.listaRestoPrograma)).removeAllViews();
        }
    }

    // La rutina de la sesión en curso (GP-012), o null: su tarjeta dice «En curso» y
    // «Volver». Lo fija quien pinta, antes de pintar.
    @androidx.annotation.Nullable private Integer rutinaEnCurso;

    /** La rutina de la sesión en curso, o null si no hay (o es sin rutina). */
    public void setRutinaEnCurso(@androidx.annotation.Nullable Integer rutinaId) {
        rutinaEnCurso = rutinaId;
    }

    private boolean enCurso(Rutina r) {
        return rutinaEnCurso != null && rutinaEnCurso == r.getId();
    }

    private void pintarHoy(RutinaConEjercicios hoy) {
        ((TextView) raiz.findViewById(R.id.tvHoyNombre)).setText(hoy.getNombre());
        ((TextView) raiz.findViewById(R.id.tvHoySub)).setText(resumen(ctx, hoy));

        LinearLayout lista = raiz.findViewById(R.id.listaEjerciciosHoy);
        lista.removeAllViews();
        List<RutinaEjercicio> ejercicios = hoy.getEjercicios() != null ? hoy.getEjercicios() : new ArrayList<>();
        for (RutinaEjercicio re : ejercicios) lista.addView(FilaPauta.crear(ctx, lista, re, false));

        com.google.android.material.button.MaterialButton empezar = raiz.findViewById(R.id.btnEmpezarHoy);
        boolean enCurso = enCurso(hoy);
        ((TextView) raiz.findViewById(R.id.tvHoyEtiqueta)).setText(enCurso ? R.string.envivo_en_curso : R.string.hoy_toca);
        empezar.setText(enCurso ? R.string.envivo_volver : R.string.btn_empezar);
        empezar.setContentDescription(enCurso ? ctx.getString(R.string.envivo_volver_rutina_a11y, hoy.getNombre())
                : ctx.getString(R.string.empezar_rutina_a11y, hoy.getNombre()));
        empezar.setOnClickListener(v -> acciones.empezar(hoy));
    }

    // El resto, con la misma tarjeta que «Mis rutinas».
    private void pintarResto(List<RutinaConEjercicios> resto) {
        LinearLayout lista = raiz.findViewById(R.id.listaRestoPrograma);
        lista.removeAllViews();
        raiz.findViewById(R.id.tvDespues).setVisibility(resto.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (RutinaConEjercicios r : resto) {
            View card = inflater.inflate(R.layout.item_rutina_entrenar, lista, false);
            String resumen = resumen(ctx, r);
            ((TextView) card.findViewById(R.id.tvNombre)).setText(r.getNombre());
            ((TextView) card.findViewById(R.id.tvResumen)).setText(resumen);
            com.google.android.material.button.MaterialButton boton = card.findViewById(R.id.btnEmpezarContorno);
            boolean enCurso = enCurso(r);
            TextView etiqueta = card.findViewById(R.id.tvTocaHoy);
            etiqueta.setText(R.string.envivo_en_curso);
            etiqueta.setVisibility(enCurso ? View.VISIBLE : View.GONE);
            boton.setText(enCurso ? R.string.envivo_volver : R.string.btn_empezar);
            boton.setContentDescription(enCurso ? ctx.getString(R.string.envivo_volver_rutina_a11y, r.getNombre())
                    : ctx.getString(R.string.empezar_rutina_a11y, r.getNombre()));
            boton.setOnClickListener(v -> acciones.empezar(r));
            card.setContentDescription(r.getNombre() + ". " + resumen
                    + (enCurso ? ". " + ctx.getString(R.string.envivo_en_curso) : ""));
            card.setOnClickListener(v -> acciones.abrirRutina(r));
            card.setOnLongClickListener(v -> {
                acciones.menuRutina(v, r);
                return true;
            });
            lista.addView(card);
        }
    }

    // Un tramo por día: hecha (relleno), hoy (borde), pendiente o borrada (apagada).
    private void pintarBarra(ProgramaQueSigue seguido) {
        LinearLayout barra = raiz.findViewById(R.id.barraCiclo);
        barra.removeAllViews();
        List<TuPrograma.EstadoDia> dias = TuPrograma.barra(seguido);
        float dp = ctx.getResources().getDisplayMetrics().density;
        int primario = color(ctx, androidx.appcompat.R.attr.colorPrimary);
        int apagado = color(ctx, com.google.android.material.R.attr.colorOutline);
        for (int i = 0; i < dias.size(); i++) {
            View tramo = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.setMarginStart(Math.round(4 * dp));
            GradientDrawable fondo = new GradientDrawable();
            fondo.setCornerRadius(4 * dp);
            switch (dias.get(i)) {
                case HECHA:
                    fondo.setColor(primario);
                    break;
                case HOY:
                    fondo.setColor(ColorStateList.valueOf(color(ctx, com.google.android.material.R.attr.colorSurface)));
                    fondo.setStroke(Math.round(2 * dp), primario);
                    break;
                case BORRADA:
                    fondo.setColor(apagado);
                    tramo.setAlpha(0.3f);
                    break;
                default:
                    fondo.setColor(apagado);
            }
            tramo.setBackground(fondo);
            tramo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            barra.addView(tramo, lp);
        }
        int hechas = TuPrograma.hechas(seguido);
        String hechasTxt = ctx.getResources().getQuantityString(R.plurals.tu_programa_hechas, hechas, hechas);
        Integer hoy = seguido.getPosicionHoy();
        barra.setContentDescription(ctx.getString(R.string.tu_programa_ciclo_a11y, dias.size(), hechasTxt,
                hoy != null ? hoy : 0));
    }

    /** «5 ejercicios · 45 min». */
    public static String resumen(Context ctx, Rutina r) {
        return ctx.getString(R.string.rutina_resumen,
                ctx.getResources().getQuantityString(R.plurals.rutina_num_ejercicios, r.getNumEjercicios(),
                        r.getNumEjercicios()),
                ctx.getString(R.string.duracion_min, r.getDuracionMinutos()));
    }

    /** El nombre del material de un programa en el idioma de la app. */
    public static String equipamiento(Context ctx, @Nullable String codigo) {
        if (Programa.MANCUERNAS.equals(codigo)) return ctx.getString(R.string.equipamiento_mancuernas);
        if (Programa.PESO_CORPORAL.equals(codigo)) return ctx.getString(R.string.equipamiento_peso_corporal);
        return ctx.getString(R.string.equipamiento_gimnasio);
    }

    static int color(Context ctx, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
