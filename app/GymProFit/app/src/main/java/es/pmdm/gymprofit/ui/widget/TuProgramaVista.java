package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.utils.CuandoTocaRutina;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Musculos;
import es.pmdm.gymprofit.utils.Siglas;
import es.pmdm.gymprofit.utils.TuPrograma;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.VistaEstado;
import es.pmdm.gymprofit.utils.Zonas;
import es.pmdm.gymprofit.utils.ZonasDelDia;

// ============================================================
// TuProgramaVista — pinta view_tu_programa, arriba de Entrenar (GP-074, lote 1.2.1; rehecha en
// GP-216, tablero 14).
//
// No decide nada: lo que toca y lo que va hecho lo calcula la API y lo reparte TuPrograma. Aquí
// solo se pinta —la tarjeta con «día N de M» y la tira del ciclo, el bloque «Hoy» y las rutinas
// del programa con sus músculos y cuándo tocan— y se pasan los toques a quien la usa (Acciones).
// Los músculos salen del catálogo; hasta que llega (setCatalogo) las filas salen sin ellos.
// ============================================================
public class TuProgramaVista {

    /** Lo que hace la pantalla con cada toque. */
    public interface Acciones {
        void elegirPrograma();
        void empezar(Rutina rutina);
        void abrirRutina(Rutina rutina, View tarjeta);
        void menuRutina(View ancla, Rutina rutina);
        void menuPrograma(View ancla, ProgramaQueSigue seguido);
        void volverASeguir(ProgramaQueSigue seguido);
        void dejar();
    }

    private final Context ctx;
    private final View raiz;
    private final Acciones acciones;
    private final VistaEstado estado;

    @Nullable private ProgramaQueSigue ultimo;
    @Nullable private Map<Integer, Ejercicio> catalogo;

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
        ultimo = null;
        estado.oculto();
        mostrar(true, false);
    }

    /** El catálogo de ejercicios, con el que se ponen los músculos de cada rutina. */
    public void setCatalogo(@Nullable Map<Integer, Ejercicio> catalogo) {
        this.catalogo = catalogo;
        if (ultimo != null) pintar(ultimo);
    }

    /** Sigue uno: su tarjeta, el bloque de hoy y las rutinas con cuándo toca cada una. */
    public void pintar(ProgramaQueSigue seguido) {
        ultimo = seguido;
        estado.oculto();
        mostrar(false, true);
        Programa p = seguido.getPrograma();

        ((TextView) raiz.findViewById(R.id.tvProgramaNombre)).setText(p.getNombre());
        String nivel = p.getNivel() != null
                ? UIHelper.traducirNivel(ctx, p.getNivel()).toLowerCase(FechaUtils.localeDeLaApp(ctx)) : "";
        String resumen = nivel.isEmpty()
                ? ctx.getString(R.string.tu_programa_resumen, equipamiento(ctx, p.getEquipamiento()),
                        ctx.getString(R.string.duracion_min, seguido.getMinutos()))
                : ctx.getString(R.string.tu_programa_resumen_largo, equipamiento(ctx, p.getEquipamiento()),
                        ctx.getString(R.string.duracion_min, seguido.getMinutos()), nivel);
        ((TextView) raiz.findViewById(R.id.tvProgramaResumen)).setText(resumen);
        raiz.findViewById(R.id.cabeceraPrograma).setContentDescription(p.getNombre() + ". " + resumen);
        ImageButton menu = raiz.findViewById(R.id.btnMenuPrograma);
        menu.setVisibility(View.VISIBLE);
        menu.setOnClickListener(v -> acciones.menuPrograma(v, seguido));

        pintarCiclo(seguido);

        RutinaConEjercicios hoy = TuPrograma.hoy(seguido);
        raiz.findViewById(R.id.bloqueHoy).setVisibility(hoy != null ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.bloqueBorradas).setVisibility(hoy == null ? View.VISIBLE : View.GONE);
        if (hoy != null) {
            pintarHoy(hoy);
        } else {
            raiz.findViewById(R.id.btnVolverASeguir).setOnClickListener(v -> acciones.volverASeguir(seguido));
            raiz.findViewById(R.id.btnDejarPrograma).setOnClickListener(v -> acciones.dejar());
        }
        pintarRutinas(seguido);
    }

    // Visibilidad de las dos tarjetas y de lo que cuelga del programa.
    private void mostrar(boolean elegir, boolean programa) {
        raiz.findViewById(R.id.cardElegirPrograma).setVisibility(elegir ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.cardPrograma).setVisibility(programa ? View.VISIBLE : View.GONE);
        if (!programa) {
            raiz.findViewById(R.id.btnMenuPrograma).setVisibility(View.GONE);
            raiz.findViewById(R.id.bloqueHoy).setVisibility(View.GONE);
            raiz.findViewById(R.id.bloqueBorradas).setVisibility(View.GONE);
            raiz.findViewById(R.id.tvDespues).setVisibility(View.GONE);
            raiz.findViewById(R.id.cardRutinasPrograma).setVisibility(View.GONE);
            ((LinearLayout) raiz.findViewById(R.id.listaRestoPrograma)).removeAllViews();
        }
    }

    // La rutina de la sesión en curso (GP-012), o null: su bloque y su fila dicen «En curso» y
    // «Volver». Lo fija quien pinta, antes de pintar.
    @Nullable private Integer rutinaEnCurso;

    /** La rutina de la sesión en curso, o null si no hay (o es sin rutina). */
    public void setRutinaEnCurso(@Nullable Integer rutinaId) {
        rutinaEnCurso = rutinaId;
    }

    private boolean enCurso(Rutina r) {
        return rutinaEnCurso != null && rutinaEnCurso == r.getId();
    }

    // ── Hoy ─────────────────────────────────────────────────────────────────

    // El bloque naranja: «Hoy · jueves», la rutina y el botón de empezar. Tocarlo abre la rutina.
    private void pintarHoy(RutinaConEjercicios hoy) {
        boolean enCurso = enCurso(hoy);
        String dia = new java.text.SimpleDateFormat("EEEE", FechaUtils.localeDeLaApp(ctx)).format(new java.util.Date());
        ((TextView) raiz.findViewById(R.id.tvHoyEtiqueta)).setText(enCurso ? ctx.getString(R.string.envivo_en_curso)
                : ctx.getString(R.string.tu_programa_hoy_dia, dia));
        ((TextView) raiz.findViewById(R.id.tvHoyNombre)).setText(hoy.getNombre());
        String musculos = musculos(hoy);
        String resumen = resumen(ctx, hoy);
        ((TextView) raiz.findViewById(R.id.tvHoySub)).setText(musculos.isEmpty() ? resumen
                : ctx.getString(R.string.rutina_resumen, resumen, musculos));

        View empezar = raiz.findViewById(R.id.btnEmpezarHoy);
        empezar.setContentDescription(enCurso ? ctx.getString(R.string.envivo_volver_rutina_a11y, hoy.getNombre())
                : ctx.getString(R.string.empezar_rutina_a11y, hoy.getNombre()));
        empezar.setOnClickListener(v -> acciones.empezar(hoy));
        View bloque = raiz.findViewById(R.id.bloqueHoy);
        bloque.setOnClickListener(v -> acciones.abrirRutina(hoy, v));
        bloque.setOnLongClickListener(v -> {
            acciones.menuRutina(v, hoy);
            return true;
        });
    }

    // ── Rutinas del programa ────────────────────────────────────────────────

    // Todas las rutinas del programa en el orden en que tocan, con sus músculos y cuándo toca cada una.
    private void pintarRutinas(ProgramaQueSigue seguido) {
        List<RutinaConEjercicios> rutinas = seguido.getRutinas() != null ? seguido.getRutinas() : new ArrayList<>();
        boolean hay = TuPrograma.hoy(seguido) != null && !rutinas.isEmpty();
        raiz.findViewById(R.id.tvDespues).setVisibility(hay ? View.VISIBLE : View.GONE);
        raiz.findViewById(R.id.cardRutinasPrograma).setVisibility(hay ? View.VISIBLE : View.GONE);
        LinearLayout lista = raiz.findViewById(R.id.listaRestoPrograma);
        lista.removeAllViews();
        if (!hay) return;
        LayoutInflater inflater = LayoutInflater.from(ctx);
        int primario = color(androidx.appcompat.R.attr.colorPrimary);
        int sobrePrimario = color(com.google.android.material.R.attr.colorOnPrimary);
        for (int i = 0; i < rutinas.size(); i++) {
            RutinaConEjercicios r = rutinas.get(i);
            View fila = inflater.inflate(R.layout.item_rutina_fila, lista, false);
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(r.getNombre());
            String musculos = musculos(r);
            String resumen = resumen(ctx, r);
            TextView tvMusculos = fila.findViewById(R.id.tvMusculos);
            tvMusculos.setText(musculos.isEmpty() ? resumen : musculos);
            TextView tvResumen = fila.findViewById(R.id.tvResumen);
            tvResumen.setText(resumen);
            tvResumen.setVisibility(musculos.isEmpty() ? View.GONE : View.VISIBLE);

            boolean enCurso = enCurso(r);
            CuandoTocaRutina c = CuandoTocaRutina.de(i);
            String cuando = enCurso ? ctx.getString(R.string.envivo_en_curso) : cuando(c);
            TextView tvCuando = fila.findViewById(R.id.tvCuando);
            tvCuando.setText(cuando);
            // El de hoy, en el naranja de la marca; los demás, apagados.
            boolean toca = c.tipo == CuandoTocaRutina.Tipo.HOY || enCurso;
            tvCuando.setTextColor(toca ? primario : color(com.google.android.material.R.attr.colorOnSurfaceVariant));
            View fondo = fila.findViewById(R.id.fondoIconoRutina);
            fondo.setBackgroundTintList(toca ? ColorStateList.valueOf(primario) : null);
            ((android.widget.ImageView) fila.findViewById(R.id.ivRutinaIcono)).setImageTintList(
                    toca ? ColorStateList.valueOf(sobrePrimario)
                            : ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnSurface)));

            fila.setContentDescription(r.getNombre() + ". " + (musculos.isEmpty() ? "" : musculos + ". ") + resumen
                    + ". " + cuando);
            fila.setOnClickListener(v -> acciones.abrirRutina(r, v));
            fila.setOnLongClickListener(v -> {
                acciones.menuRutina(v, r);
                return true;
            });
            lista.addView(fila);
            if (i < rutinas.size() - 1) lista.addView(raya());
        }
    }

    private String cuando(CuandoTocaRutina c) {
        switch (c.tipo) {
            case HOY: return ctx.getString(R.string.tiempo_hoy_mayuscula);
            case SIGUIENTE: return ctx.getString(R.string.tu_programa_siguiente);
            default: return ctx.getResources().getQuantityString(R.plurals.tu_programa_en_n, c.n, c.n);
        }
    }

    // Los músculos que trabaja, de más a menos series: «Pecho · Espalda · Hombros» (hasta tres).
    private String musculos(RutinaConEjercicios r) {
        if (catalogo == null || r.getEjercicios() == null) return "";
        List<ZonasDelDia.Ejercicio> lista = new ArrayList<>();
        for (RutinaEjercicio re : r.getEjercicios()) {
            Ejercicio e = catalogo.get(re.getEjercicioId());
            lista.add(new ZonasDelDia.Ejercicio(e == null ? null
                    : Musculos.normalizar(e.getMusculoPrimario(), e.getGrupoMuscular()), re.getSeries()));
        }
        List<String> nombres = new ArrayList<>();
        for (Zonas.Zona z : ZonasDelDia.de(lista).zonas) {
            if (nombres.size() < 3) nombres.add(ctx.getString(z.titulo));
        }
        return android.text.TextUtils.join(" · ", nombres);
    }

    private View raya() {
        View v = new View(ctx);
        v.setBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant));
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(dp)));
        lp.setMarginStart(Math.round(80 * dp));
        v.setLayoutParams(lp);
        return v;
    }

    // ── Ciclo ───────────────────────────────────────────────────────────────

    // «Día 3 de 4», las hechas, una barra con un tramo por día y la tira de siglas.
    private void pintarCiclo(ProgramaQueSigue seguido) {
        List<TuPrograma.EstadoDia> dias = TuPrograma.barra(seguido);
        List<ProgramaQueSigue.DiaCiclo> ciclo = seguido.getCiclo() != null ? seguido.getCiclo() : new ArrayList<>();
        int hechas = TuPrograma.hechas(seguido);
        int total = dias.size();

        // Qué día del ciclo toca: el lugar de la posición de hoy en la lista, contando desde 1.
        int diaHoy = 0;
        Integer posHoy = seguido.getPosicionHoy();
        for (int i = 0; i < ciclo.size(); i++) {
            if (posHoy != null && ciclo.get(i).getPosicion() == posHoy) { diaHoy = i + 1; break; }
        }
        TextView tvDia = raiz.findViewById(R.id.tvProgramaDia);
        tvDia.setText(diaHoy > 0 ? ctx.getString(R.string.tu_programa_dia_de, diaHoy, total) : "");
        tvDia.setVisibility(diaHoy > 0 ? View.VISIBLE : View.GONE);
        ((TextView) raiz.findViewById(R.id.tvProgramaHechas)).setText(
                ctx.getResources().getQuantityString(R.plurals.tu_programa_hechas_corto, hechas, hechas));

        pintarBarra(seguido, dias, hechas, diaHoy);
        pintarTira(ciclo, dias, hechas, diaHoy);
    }

    // Un tramo por día: hecha (relleno), hoy (borde), pendiente o borrada (apagada).
    private void pintarBarra(ProgramaQueSigue seguido, List<TuPrograma.EstadoDia> dias, int hechas, int diaHoy) {
        LinearLayout barra = raiz.findViewById(R.id.barraCiclo);
        barra.removeAllViews();
        float dp = ctx.getResources().getDisplayMetrics().density;
        int primario = color(androidx.appcompat.R.attr.colorPrimary);
        int apagado = color(com.google.android.material.R.attr.colorOutline);
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
                    fondo.setColor(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorSurface)));
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
        String hechasTxt = ctx.getResources().getQuantityString(R.plurals.tu_programa_hechas, hechas, hechas);
        barra.setContentDescription(ctx.getString(R.string.tu_programa_ciclo_a11y, dias.size(), hechasTxt, diaHoy));
    }

    // Un círculo por día con las siglas de su rutina: lleno si va hecha, con borde si es hoy, apagado si
    // falta o está borrada. La tira entera la lee TalkBack de una vez, día por día.
    private void pintarTira(List<ProgramaQueSigue.DiaCiclo> ciclo, List<TuPrograma.EstadoDia> dias,
                            int hechas, int diaHoy) {
        LinearLayout tira = raiz.findViewById(R.id.tiraCiclo);
        tira.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        float dp = ctx.getResources().getDisplayMetrics().density;
        int primario = color(androidx.appcompat.R.attr.colorPrimary);
        int sobrePrimario = color(com.google.android.material.R.attr.colorOnPrimary);
        int contenedor = color(com.google.android.material.R.attr.colorPrimaryContainer);
        int sobreContenedor = color(com.google.android.material.R.attr.colorOnPrimaryContainer);
        int neutro = color(com.google.android.material.R.attr.colorSurfaceVariant);
        int apoyo = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        StringBuilder voz = new StringBuilder();
        for (int i = 0; i < ciclo.size() && i < dias.size(); i++) {
            ProgramaQueSigue.DiaCiclo d = ciclo.get(i);
            View v = inflater.inflate(R.layout.item_dia_programa, tira, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            v.setLayoutParams(lp);
            ((TextView) v.findViewById(R.id.tvDiaNumero)).setText(String.valueOf(i + 1));
            TextView siglas = v.findViewById(R.id.tvDiaSiglas);
            siglas.setText(Siglas.de(d.getRutinaNombre()));
            GradientDrawable fondo = new GradientDrawable();
            fondo.setShape(GradientDrawable.OVAL);
            int estadoTxt;
            switch (dias.get(i)) {
                case HECHA:
                    fondo.setColor(primario);
                    siglas.setTextColor(sobrePrimario);
                    estadoTxt = R.string.tu_programa_estado_hecha;
                    break;
                case HOY:
                    fondo.setColor(contenedor);
                    fondo.setStroke(Math.round(2 * dp), primario);
                    siglas.setTextColor(sobreContenedor);
                    estadoTxt = R.string.tu_programa_estado_hoy;
                    break;
                case BORRADA:
                    fondo.setColor(neutro);
                    siglas.setTextColor(apoyo);
                    siglas.setAlpha(0.4f);
                    estadoTxt = R.string.tu_programa_estado_borrada;
                    break;
                default:
                    fondo.setColor(neutro);
                    siglas.setTextColor(apoyo);
                    estadoTxt = R.string.tu_programa_estado_pendiente;
            }
            siglas.setBackground(fondo);
            tira.addView(v);
            if (voz.length() > 0) voz.append(". ");
            voz.append(ctx.getString(R.string.tu_programa_dia_siglas_a11y, i + 1,
                    d.getRutinaNombre() != null ? d.getRutinaNombre() : "", ctx.getString(estadoTxt)));
        }
        tira.setContentDescription(voz.toString());
        tira.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** «5 ejercicios · 45 min». */
    public static String resumen(Context ctx, Rutina r) {
        return ctx.getString(R.string.rutina_resumen,
                ctx.getResources().getQuantityString(R.plurals.rutina_num_ejercicios, r.getNumEjercicios(),
                        r.getNumEjercicios()),
                ctx.getString(R.string.duracion_min, r.getDuracionMinutos()));
    }

    /** El color de la franja de un programa según su material (gimnasio, mancuernas, peso corporal). */
    @androidx.annotation.ColorRes
    public static int colorDe(@Nullable String equipamiento) {
        if (Programa.MANCUERNAS.equals(equipamiento)) return R.color.gp_programa_mancuernas;
        if (Programa.PESO_CORPORAL.equals(equipamiento)) return R.color.gp_programa_corporal;
        return R.color.gp_programa_gimnasio;
    }

    /** El nombre del material de un programa en el idioma de la app. */
    public static String equipamiento(Context ctx, @Nullable String codigo) {
        if (Programa.MANCUERNAS.equals(codigo)) return ctx.getString(R.string.equipamiento_mancuernas);
        if (Programa.PESO_CORPORAL.equals(codigo)) return ctx.getString(R.string.equipamiento_peso_corporal);
        return ctx.getString(R.string.equipamiento_gimnasio);
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
