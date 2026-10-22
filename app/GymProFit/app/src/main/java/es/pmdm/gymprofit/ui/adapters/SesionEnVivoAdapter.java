package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.envivo.LogicaDescanso;
import es.pmdm.gymprofit.envivo.LogicaRecordVivo;
import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.Pauta;
import es.pmdm.gymprofit.utils.TiempoRelativo;

// ============================================================
// SesionEnVivoAdapter — la lista de la sesión en vivo (GP-012), plana.
//
// Cada ejercicio es una tarjeta partida en piezas: su cabecera, una fila por serie y
// un pie con «Añadir serie». Es plana y no una tarjeta con filas dentro porque cada
// serie tiene que ser un elemento de la lista para quitarla deslizándola. Al final va
// el estado de la carga con «Añadir ejercicio» y «Descartar la sesión».
//
// La lista se reconstruye con cada cambio de la sesión, pero con DiffUtil sobre una
// FOTO de lo que se pinta: solo se repinta lo que cambió, y el campo en el que se está
// escribiendo no pierde el foco al marcar otra serie. Teclear no repinta nada: el texto
// va al repositorio, que lo escribe al fichero.
// ============================================================
public class SesionEnVivoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Lo que la pantalla hace por la lista. */
    public interface Acciones {
        void marcar(long serieId, int numero);
        void cronometro(long serieId);
        void menuEjercicio(View ancla, long ejercicioId, String nombre);
        void anadirSerie(long ejercicioId);
        void anadirEjercicio();
        void descartar();
        void reintentarCarga();
    }

    static final int CABECERA = 0, SERIE = 1, PIE = 2, FINAL = 3;

    /** Foto inmutable de una fila, para comparar con DiffUtil. */
    static final class Fila {
        final int tipo;
        final long id;
        // Cabecera
        String nombre, pauta, notas, ultimaVez;
        boolean porTiempo, menu;
        /** «Tu récord: …» o «Primera vez: …» (GP-210); null si aún no se sabe. */
        String record;
        /** Si la línea del récord va en dorado (hay marca) o neutra (primera vez). */
        boolean recordDorado;
        /** El aviso «¡Récord!» de este ejercicio, o null si no lo hay (GP-210). */
        String avisoTitulo, avisoAntes, avisoA11y;
        // Serie
        int numero;
        String anterior, anteriorA11y, peso, reps, segundos, pistaPeso, pistaReps, pistaSegundos;
        boolean hecha, editable;
        /** La que toca tras el descanso: se resalta mientras dura (GP-013). */
        boolean siguiente;
        /** La primera sin marcar de la sesión: su botón va en el color principal (GP-210). */
        boolean toca;
        /** La serie que batió el récord del ejercicio: su fila va en dorado (GP-210). */
        boolean oro;
        // Pie
        boolean puedeAnadir;
        // Final
        SesionEnCurso.Carga carga;
        boolean vacia;

        Fila(int tipo, long id) {
            this.tipo = tipo;
            this.id = id;
        }

        boolean mismoContenido(Fila o) {
            return tipo == o.tipo && id == o.id && Objects.equals(nombre, o.nombre)
                    && Objects.equals(pauta, o.pauta) && Objects.equals(notas, o.notas)
                    && Objects.equals(ultimaVez, o.ultimaVez) && porTiempo == o.porTiempo && menu == o.menu
                    && Objects.equals(record, o.record) && recordDorado == o.recordDorado
                    && Objects.equals(avisoTitulo, o.avisoTitulo) && Objects.equals(avisoAntes, o.avisoAntes)
                    && Objects.equals(avisoA11y, o.avisoA11y) && toca == o.toca && oro == o.oro
                    && numero == o.numero && Objects.equals(anterior, o.anterior)
                    && Objects.equals(peso, o.peso) && Objects.equals(reps, o.reps)
                    && Objects.equals(segundos, o.segundos) && Objects.equals(pistaPeso, o.pistaPeso)
                    && Objects.equals(pistaReps, o.pistaReps) && Objects.equals(pistaSegundos, o.pistaSegundos)
                    && hecha == o.hecha && editable == o.editable && siguiente == o.siguiente
                    && puedeAnadir == o.puedeAnadir
                    && carga == o.carga && vacia == o.vacia;
        }
    }

    private final Context ctx;
    private final SesionEnCursoRepositorio repo;
    private final Acciones acciones;
    private List<Fila> filas = new ArrayList<>();

    // Lo que anima el próximo repintado (GP-210): la serie recién marcada y, si bate el
    // récord, su fila dorada y el aviso de su ejercicio. Se piden desde la pantalla justo
    // antes de que la lista se repinte y se agotan en cuanto se pinta la fila marcada.
    private long marcadaSerieId = -1;
    private boolean marcadaEsRecord;
    private long recordSerieId = -1;
    private long recordEjercicioId = -1;

    public SesionEnVivoAdapter(@NonNull Context ctx, @NonNull SesionEnCursoRepositorio repo,
                               @NonNull Acciones acciones) {
        this.ctx = ctx;
        this.repo = repo;
        this.acciones = acciones;
        setHasStableIds(true);
    }

    // ── Construir la foto ────────────────────────────────────

    /** Textos de LogicaSesion, sacados de strings.xml. */
    private LogicaSesion.Textos textos() {
        return new LogicaSesion.Textos() {
            @Override public String pesoPorReps(String kilos, int reps) {
                return ctx.getString(R.string.envivo_peso_por_reps, kilos, reps);
            }
            @Override public String reps(int reps) {
                return ctx.getResources().getQuantityString(R.plurals.envivo_reps, reps, reps);
            }
            @Override public String rango(int min, int max) { return ctx.getString(R.string.pauta_rango, min, max); }
            @Override public String segundos(String numero) { return ctx.getString(R.string.pauta_segundos, numero); }
            @Override public String nada() { return ctx.getString(R.string.envivo_nada); }
        };
    }

    private Locale locale() {
        return ctx.getResources().getConfiguration().getLocales().get(0);
    }

    /** Pinta la sesión: solo cambia lo que cambió. */
    public void pintar(@Nullable SesionEnCurso s) {
        List<Fila> nuevas = new ArrayList<>();
        if (s != null) {
            boolean editable = s.claveIdempotencia == null;
            // Durante el descanso se resalta la serie que viene después.
            LogicaDescanso.Siguiente sig = s.descanso != null ? LogicaDescanso.siguiente(s) : null;
            long siguienteId = sig != null ? sig.serieId : Long.MIN_VALUE;
            LogicaSesion.Textos t = textos();
            Locale locale = locale();
            boolean tocaDada = false;
            for (SesionEnCurso.Ejercicio e : s.ejercicios) {
                nuevas.add(cabecera(e, editable));
                for (int i = 0; i < e.series.size(); i++) {
                    Fila f = serie(e, i, editable, t, locale);
                    f.siguiente = f.id == siguienteId;
                    // La primera sin marcar de toda la sesión es la que toca.
                    f.toca = !tocaDada && !f.hecha;
                    tocaDada |= f.toca;
                    f.oro = f.hecha && e.recordSerieId != null && e.recordSerieId == f.id;
                    nuevas.add(f);
                }
                Fila pie = new Fila(PIE, -e.id);
                pie.editable = editable;
                pie.puedeAnadir = e.series.size() < SesionEnCurso.MAX_SERIES;
                nuevas.add(pie);
            }
            Fila fin = new Fila(FINAL, Long.MIN_VALUE);
            fin.carga = s.carga;
            fin.vacia = s.ejercicios.isEmpty();
            fin.editable = editable;
            nuevas.add(fin);
        }
        List<Fila> viejas = filas;
        // Sin ejercicios pintados todavía (solo la fila final, mientras cargaban), se pinta
        // de cero: con DiffUtil la lista se anclaba a esa fila final y, al llegar los
        // ejercicios encima, se abría por el final en vez de por el primero.
        if (viejas.size() <= 1) {
            filas = nuevas;
            notifyDataSetChanged();
            return;
        }
        DiffUtil.DiffResult d = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return viejas.size(); }
            @Override public int getNewListSize() { return nuevas.size(); }
            @Override public boolean areItemsTheSame(int a, int b) {
                return viejas.get(a).tipo == nuevas.get(b).tipo && viejas.get(a).id == nuevas.get(b).id;
            }
            @Override public boolean areContentsTheSame(int a, int b) {
                return viejas.get(a).mismoContenido(nuevas.get(b));
            }
        });
        filas = nuevas;
        d.dispatchUpdatesTo(this);
    }

    private Fila cabecera(SesionEnCurso.Ejercicio e, boolean editable) {
        Fila f = new Fila(CABECERA, e.id);
        f.nombre = e.nombre != null ? e.nombre : ctx.getString(R.string.ejercicio_sin_nombre, e.ejercicioId);
        f.porTiempo = LogicaSesion.porTiempo(e);
        f.menu = editable;
        if (e.deRutina) {
            f.pauta = Pauta.texto(Pauta.Formatos.de(ctx), e.seriesPauta, e.minimo, e.maximo,
                    e.repeticionesPauta, e.medida, e.porLado);
            List<String> extra = new ArrayList<>();
            String descanso = Pauta.descanso(ctx, e.descanso);
            if (descanso != null) extra.add(descanso);
            if (e.notasPauta != null && !e.notasPauta.trim().isEmpty()) extra.add(e.notasPauta.trim());
            f.notas = String.join(ctx.getString(R.string.separador_punto), extra);
        }
        if (!LogicaSesion.primeraVez(e) && e.anteriorCargado && e.anteriorFecha != null) {
            String cuando = TiempoRelativo.texto(ctx, e.anteriorFecha);
            f.ultimaVez = cuando != null ? ctx.getString(R.string.envivo_ultima_vez, cuando) : null;
        }
        recordDe(e, f);
        return f;
    }

    // «Tu récord» y su aviso (GP-210). Sin historial: «Primera vez». Sin saberlo aún (no ha
    // llegado la respuesta o no hubo red): ninguna de las dos, que es lo honesto.
    private void recordDe(SesionEnCurso.Ejercicio e, Fila f) {
        SesionEnCurso.Marca m = LogicaRecordVivo.mostrada(e);
        if (m != null) {
            String marca = textoMarca(m);
            f.record = ctx.getString(LogicaRecordVivo.esDeHoy(e)
                    ? R.string.envivo_record_tu_hoy : R.string.envivo_record_tu, marca);
            f.recordDorado = true;
        } else if (LogicaSesion.primeraVez(e)) {
            f.record = ctx.getString(R.string.envivo_primera_vez);
        }
        if (e.recordSerieId == null || e.marca == null) return;
        for (SesionEnCurso.Serie s : e.series) {
            if (s.id != e.recordSerieId || !s.hecha) continue;
            SesionEnCurso.Marca nueva = LogicaRecordVivo.deSerie(e, s);
            if (nueva == null) return;
            String antes = textoMarca(e.marca);
            f.avisoTitulo = ctx.getString(R.string.envivo_record_aviso, textoMarca(nueva));
            f.avisoAntes = ctx.getString(R.string.envivo_record_aviso_antes, antes);
            f.avisoA11y = ctx.getString(R.string.envivo_record_aviso_a11y, textoMarca(nueva), antes);
            return;
        }
    }

    // «85 kg × 7», «12 reps» o «1:05», como en el resumen (Marcas).
    private String textoMarca(SesionEnCurso.Marca m) {
        if (Record.TIPO_TIEMPO.equals(m.tipo)) return Marcas.tiempo(m.segundos);
        if (Record.TIPO_PESO.equals(m.tipo)) {
            double kilos;
            try {
                kilos = m.peso != null ? Double.parseDouble(m.peso) : 0;
            } catch (NumberFormatException e) {
                kilos = 0;
            }
            return ctx.getString(R.string.record_peso_por_reps, Marcas.kilos(ctx, kilos), m.repeticiones);
        }
        return ctx.getResources().getQuantityString(R.plurals.record_repeticiones, m.repeticiones, m.repeticiones);
    }

    /**
     * La pantalla acaba de marcar una serie: al repintarla se anima (momento 24) y, si bate el
     * récord del ejercicio, se dora su fila y baja el aviso (momento 25). Se llama justo
     * después de marcar, antes de que la lista se repinte.
     *
     * @param serieId   la serie marcada.
     * @param ejercicio el ejercicio de un récord recién batido, o -1 si no lo hubo.
     */
    public void animarMarcada(long serieId, long ejercicio) {
        marcadaSerieId = serieId;
        marcadaEsRecord = ejercicio != -1;
        recordSerieId = ejercicio != -1 ? serieId : -1;
        recordEjercicioId = ejercicio;
    }

    private Fila serie(SesionEnCurso.Ejercicio e, int i, boolean editable, LogicaSesion.Textos t, Locale locale) {
        SesionEnCurso.Serie s = e.series.get(i);
        Fila f = new Fila(SERIE, s.id);
        f.numero = i + 1;
        f.porTiempo = LogicaSesion.porTiempo(e);
        // Sin preguntar todavía no se sabe: la columna se queda en blanco, no dice «—».
        f.anterior = e.anteriorCargado ? LogicaSesion.textoAnterior(e, f.numero, t, locale) : "";
        f.anteriorA11y = !e.anteriorCargado ? null
                : LogicaSesion.anterior(e, f.numero) == null ? ctx.getString(R.string.envivo_nada_a11y) : f.anterior;
        f.peso = s.peso;
        f.reps = s.repeticiones;
        f.segundos = s.segundos;
        f.pistaPeso = LogicaSesion.pistaPeso(e, f.numero, locale);
        f.pistaReps = LogicaSesion.pistaReps(e, f.numero, t);
        f.pistaSegundos = LogicaSesion.pistaSegundos(e, f.numero, t);
        f.hecha = s.hecha;
        f.editable = editable;
        return f;
    }

    // El color de fondo del botón de marcar según su estado.
    private int fondoBoton(boolean hecha, boolean toca) {
        if (hecha) return ContextCompat.getColor(ctx, R.color.gp_success_container);
        if (toca) return MaterialColors.getColor(ctx, androidx.appcompat.R.attr.colorPrimary, 0);
        return MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceVariant, 0);
    }

    /** Posición de la cabecera del ejercicio de una serie sin marcar, para «Revisar». */
    public int primeraSinMarcar() {
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).tipo == SERIE && !filas.get(i).hecha) return i;
        }
        return -1;
    }

    /** Si la fila es una serie que se puede quitar deslizándola. */
    public boolean esSerieQuitable(int posicion) {
        return posicion >= 0 && posicion < filas.size()
                && filas.get(posicion).tipo == SERIE && filas.get(posicion).editable;
    }

    public long idDe(int posicion) { return filas.get(posicion).id; }

    // ── RecyclerView ─────────────────────────────────────────

    @Override public int getItemCount() { return filas.size(); }

    @Override public int getItemViewType(int position) { return filas.get(position).tipo; }

    /**
     * Pone el foco en el campo que falta de la serie (repeticiones o tiempo) y abre el
     * teclado. Devuelve el campo, o null si la fila no está en pantalla.
     */
    @Nullable
    public static EditText enfocarSerie(@NonNull RecyclerView rv, long serieId) {
        RecyclerView.ViewHolder vh = rv.findViewHolderForItemId(SERIE * 1_000_000_000_000L + serieId);
        if (!(vh instanceof SesionEnVivoAdapter.SerieVH)) return null;
        SerieVH serie = (SerieVH) vh;
        EditText campo = serie.etTiempo.getVisibility() == View.VISIBLE ? serie.etTiempo : serie.etReps;
        if (!campo.isEnabled()) return null;
        campo.requestFocus();
        InputMethodManager imm = (InputMethodManager) campo.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(campo, InputMethodManager.SHOW_IMPLICIT);
        return campo;
    }

    @Override public long getItemId(int position) {
        Fila f = filas.get(position);
        return f.tipo * 1_000_000_000_000L + f.id;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int tipo) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (tipo) {
            case CABECERA: return new CabeceraVH(inf.inflate(R.layout.item_envivo_ejercicio, parent, false));
            case SERIE:    return new SerieVH(inf.inflate(R.layout.item_envivo_serie, parent, false));
            case PIE:      return new PieVH(inf.inflate(R.layout.item_envivo_pie, parent, false));
            default:       return new FinalVH(inf.inflate(R.layout.item_envivo_final, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        Fila f = filas.get(position);
        if (h instanceof CabeceraVH) ((CabeceraVH) h).pintar(f);
        else if (h instanceof SerieVH) ((SerieVH) h).pintar(f);
        else if (h instanceof PieVH) ((PieVH) h).pintar(f);
        else ((FinalVH) h).pintar(f);
    }

    private static void texto(TextView tv, @Nullable String t) {
        tv.setText(t);
        tv.setVisibility(t == null || t.isEmpty() ? View.GONE : View.VISIBLE);
    }

    final class CabeceraVH extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvPauta, tvNotas, tvUltimaVez, tvRotuloKg, tvRotuloReps, tvRecord;
        final TextView tvRecordTitulo, tvRecordAntes;
        final View grupo, avisoRecord, marcoTrofeo;
        final ImageButton btnMenu;

        CabeceraVH(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvPauta = v.findViewById(R.id.tvPauta);
            tvNotas = v.findViewById(R.id.tvNotas);
            tvUltimaVez = v.findViewById(R.id.tvUltimaVez);
            tvRecord = v.findViewById(R.id.tvRecord);
            avisoRecord = v.findViewById(R.id.avisoRecord);
            marcoTrofeo = v.findViewById(R.id.marcoTrofeo);
            tvRecordTitulo = v.findViewById(R.id.tvRecordTitulo);
            tvRecordAntes = v.findViewById(R.id.tvRecordAntes);
            tvRotuloKg = v.findViewById(R.id.tvRotuloKg);
            tvRotuloReps = v.findViewById(R.id.tvRotuloReps);
            grupo = v.findViewById(R.id.grupoCabecera);
            btnMenu = v.findViewById(R.id.btnMenuEjercicio);
            for (int id : new int[]{R.id.tvRotuloSerie, R.id.tvRotuloAnterior, R.id.tvRotuloKg, R.id.tvRotuloReps}) {
                es.pmdm.gymprofit.utils.Rotulos.limitar(v.findViewById(id));
            }
        }

        void pintar(Fila f) {
            tvNombre.setText(f.nombre);
            texto(tvPauta, f.pauta);
            texto(tvNotas, f.notas);
            texto(tvUltimaVez, f.ultimaVez);
            pintarRecord(f);
            // Por tiempo, una columna «Tiempo» donde van kg y reps.
            tvRotuloKg.setText(f.porTiempo ? R.string.envivo_col_tiempo : R.string.envivo_col_kg);
            android.widget.LinearLayout.LayoutParams lp =
                    (android.widget.LinearLayout.LayoutParams) tvRotuloKg.getLayoutParams();
            lp.weight = f.porTiempo ? 2f : 1f;
            tvRotuloKg.setLayoutParams(lp);
            tvRotuloReps.setVisibility(f.porTiempo ? View.GONE : View.VISIBLE);

            List<String> a11y = new ArrayList<>();
            a11y.add(f.nombre);
            if (f.pauta != null) a11y.add(f.pauta);
            if (f.notas != null && !f.notas.isEmpty()) a11y.add(f.notas);
            if (f.record != null) a11y.add(f.record);
            if (f.ultimaVez != null) a11y.add(f.ultimaVez);
            grupo.setContentDescription(String.join(". ", a11y));
            ViewCompat.setAccessibilityHeading(grupo, true);

            btnMenu.setVisibility(f.menu ? View.VISIBLE : View.GONE);
            btnMenu.setContentDescription(ctx.getString(R.string.envivo_menu_ejercicio, f.nombre));
            btnMenu.setOnClickListener(v -> acciones.menuEjercicio(v, f.id, f.nombre));
        }

        // «Tu récord: …» (con su trofeo dorado) o «Primera vez»; y el aviso «¡Récord!», que
        // baja con el trofeo, las chispas y el brillo la vez que se bate (momento 25).
        private void pintarRecord(Fila f) {
            texto(tvRecord, f.record);
            tvRecord.setTextColor(f.recordDorado
                    ? ContextCompat.getColor(ctx, R.color.gp_gold_text)
                    : MaterialColors.getColor(tvRecord, androidx.appcompat.R.attr.colorPrimary));
            tvRecord.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    f.recordDorado ? R.drawable.ic_ms_trophy : 0, 0, 0, 0);

            boolean hayAviso = f.avisoTitulo != null;
            boolean yaVisible = avisoRecord.getVisibility() == View.VISIBLE;
            avisoRecord.setVisibility(hayAviso ? View.VISIBLE : View.GONE);
            if (!hayAviso) return;
            tvRecordTitulo.setText(f.avisoTitulo);
            tvRecordAntes.setText(f.avisoAntes);
            avisoRecord.setContentDescription(f.avisoA11y);
            if (f.id == recordEjercicioId && !yaVisible) {
                recordEjercicioId = -1;
                int oro = ContextCompat.getColor(ctx, R.color.gp_gold);
                Movimiento.bajarAviso(avisoRecord);
                Movimiento.celebrarRecord((android.view.ViewGroup) avisoRecord, marcoTrofeo, oro, oro, 120);
            }
        }
    }

    final class SerieVH extends RecyclerView.ViewHolder {
        final View fila;
        final TextView tvNumero, tvAnterior;
        final EditText etKg, etReps, etTiempo;
        final ImageButton btnCronometro, btnMarcar;
        long serieId = -1;
        boolean pintando;
        int accionQuitar = View.NO_ID;

        SerieVH(View v) {
            super(v);
            fila = v.findViewById(R.id.filaSerie);
            tvNumero = v.findViewById(R.id.tvNumero);
            tvAnterior = v.findViewById(R.id.tvAnterior);
            etKg = v.findViewById(R.id.etKg);
            etReps = v.findViewById(R.id.etReps);
            etTiempo = v.findViewById(R.id.etTiempo);
            btnCronometro = v.findViewById(R.id.btnCronometro);
            btnMarcar = v.findViewById(R.id.btnMarcar);
            // Lo escrito va al repositorio; el estado de estas vistas no se guarda aparte
            // (manda el fichero de la sesión, no lo que Android restaure).
            for (EditText et : new EditText[]{etKg, etReps, etTiempo}) et.setSaveEnabled(false);
            etKg.addTextChangedListener(alCambiar(etKg, t -> repo.escribirPeso(serieId, t)));
            etReps.addTextChangedListener(alCambiar(etReps, t -> repo.escribirRepeticiones(serieId, t)));
            etTiempo.addTextChangedListener(alCambiar(etTiempo, t -> repo.escribirSegundos(serieId, t)));
        }

        private TextWatcher alCambiar(EditText et, java.util.function.Consumer<String> destino) {
            return new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    if (!pintando && serieId != -1) destino.accept(s.toString());
                    Object nombre = et.getTag(R.id.etKg);
                    ViewCompat.setStateDescription(et, s.length() > 0 && nombre != null ? nombre.toString() : null);
                }
            };
        }

        // El nombre del campo no va como descripción: en un campo de texto la descripción
        // tapa el valor y TalkBack dejaría de leer lo escrito. Vacío, va como pista del
        // nodo («Kilos de la serie 1, 57,5»); con valor, TalkBack no lee la pista, y va
        // como estado, que sí se lee tras el valor («60, Kilos de la serie 1»).
        private void rotular(EditText et, String nombre, String pista) {
            // El nombre se guarda en la vista para que el TextWatcher lo ponga al escribir.
            et.setTag(R.id.etKg, nombre);
            ViewCompat.setStateDescription(et, et.getText().length() > 0 ? nombre : null);
            String texto = pista == null || pista.isEmpty() ? nombre : nombre + ", " + pista;
            ViewCompat.setAccessibilityDelegate(et, new androidx.core.view.AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setHintText(texto);
                }
            });
        }

        private void poner(EditText et, String valor, String pista, boolean editable) {
            if (!et.getText().toString().equals(valor)) et.setText(valor);
            et.setHint(pista);
            et.setEnabled(editable);
        }

        void pintar(Fila f) {
            pintando = true;
            serieId = f.id;
            tvNumero.setText(String.valueOf(f.numero));
            tvAnterior.setText(f.anterior);

            etKg.setVisibility(f.porTiempo ? View.GONE : View.VISIBLE);
            etReps.setVisibility(f.porTiempo ? View.GONE : View.VISIBLE);
            etTiempo.setVisibility(f.porTiempo ? View.VISIBLE : View.GONE);
            btnCronometro.setVisibility(f.porTiempo && f.editable ? View.VISIBLE : View.GONE);
            poner(etKg, f.peso, f.pistaPeso, f.editable);
            poner(etReps, f.reps, f.pistaReps, f.editable);
            poner(etTiempo, f.segundos, f.pistaSegundos, f.editable);
            // Después de poner el texto: el estado depende de si hay valor.
            rotular(etKg, ctx.getString(R.string.envivo_kg_a11y, f.numero), f.pistaPeso);
            rotular(etReps, ctx.getString(R.string.envivo_reps_a11y, f.numero), f.pistaReps);
            rotular(etTiempo, ctx.getString(R.string.envivo_tiempo_a11y, f.numero), f.pistaSegundos);
            pintando = false;

            // La fila: dorada si es la del récord (momento 25), con el borde del color principal
            // si es la siguiente tras el descanso (GP-013), sin relleno en las demás.
            boolean marcadaAhora = f.hecha && f.id == marcadaSerieId;
            if (f.oro) {
                int oroFila = ContextCompat.getColor(ctx, R.color.gp_gold_fila);
                if (marcadaAhora && marcadaEsRecord) {
                    Movimiento.dorarFila(fila, MaterialColors.getColor(fila, com.google.android.material.R.attr.colorSurface),
                            ContextCompat.getColor(ctx, R.color.gp_gold_container), oroFila);
                } else {
                    fila.setBackgroundColor(oroFila);
                }
            } else if (f.siguiente && !f.hecha) {
                fila.setBackgroundResource(R.drawable.bg_serie_siguiente);
            } else {
                fila.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            // El botón: verde si está hecha, del color principal si es la que toca y gris si no
            // (el check lo dice también con su estado, no solo con el color).
            int fondo = fondoBoton(f.hecha, f.toca);
            int tinte = MaterialColors.getColor(btnMarcar, f.hecha
                    ? androidx.appcompat.R.attr.colorPrimary : f.toca ? com.google.android.material.R.attr.colorOnPrimary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant);
            btnMarcar.setImageResource(R.drawable.ic_ms_check);
            btnMarcar.setImageTintList(ColorStateList.valueOf(f.hecha ? ContextCompat.getColor(ctx, R.color.gp_success) : tinte));
            if (marcadaAhora) {
                int antes = fondoBoton(false, true);
                Movimiento.marcarSerie(btnMarcar, antes, fondo, !marcadaEsRecord);
            } else {
                btnMarcar.setBackgroundTintList(ColorStateList.valueOf(fondo));
            }
            if (f.id == marcadaSerieId) {
                // Agotado: la cabecera (más arriba) ya se pintó en esta pasada.
                marcadaSerieId = -1;
                marcadaEsRecord = false;
                recordSerieId = -1;
                recordEjercicioId = -1;
            }
            ViewCompat.setStateDescription(btnMarcar,
                    ctx.getString(f.hecha ? R.string.envivo_hecha : R.string.envivo_sin_hacer));
            btnMarcar.setContentDescription(ctx.getString(R.string.envivo_marcar, f.numero));
            btnMarcar.setEnabled(f.editable);
            btnMarcar.setOnClickListener(v -> acciones.marcar(f.id, f.numero));

            btnCronometro.setContentDescription(ctx.getString(R.string.envivo_cronometro, f.numero));
            btnCronometro.setOnClickListener(v -> acciones.cronometro(f.id));

            // TalkBack: «Serie 2. Anterior: 57,5 × 12» en el número, que es donde entra
            // en la fila; y «Quitar serie» como acción, que es lo que sustituye al gesto.
            String nombre = ctx.getString(f.siguiente && !f.hecha
                    ? R.string.envivo_serie_siguiente_a11y : R.string.envivo_serie_a11y, f.numero);
            tvNumero.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            tvNumero.setContentDescription(f.anteriorA11y == null ? nombre
                    : nombre + ". " + ctx.getString(R.string.envivo_anterior_a11y, f.anteriorA11y));
            if (accionQuitar != View.NO_ID) ViewCompat.removeAccessibilityAction(tvNumero, accionQuitar);
            accionQuitar = View.NO_ID;
            if (f.editable) {
                accionQuitar = ViewCompat.addAccessibilityAction(tvNumero,
                        ctx.getString(R.string.envivo_quitar_serie), (vista, args) -> {
                            int pos = getBindingAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) quitar.quitar(pos);
                            return true;
                        });
            }
        }
    }

    /** Quien quita una serie (la pantalla, que ofrece deshacer). */
    public interface Quitar { void quitar(int posicion); }

    private Quitar quitar = p -> { };

    public void setQuitar(@NonNull Quitar q) { quitar = q; }

    final class PieVH extends RecyclerView.ViewHolder {
        final MaterialButton btn;

        PieVH(View v) {
            super(v);
            btn = v.findViewById(R.id.btnAnadirSerie);
        }

        void pintar(Fila f) {
            btn.setVisibility(f.editable ? View.VISIBLE : View.INVISIBLE);
            btn.setEnabled(f.puedeAnadir);
            btn.setText(f.puedeAnadir ? ctx.getString(R.string.envivo_anadir_serie)
                    : ctx.getString(R.string.envivo_max_series, SesionEnCurso.MAX_SERIES));
            btn.setOnClickListener(v -> acciones.anadirSerie(-f.id));
        }
    }

    final class FinalVH extends RecyclerView.ViewHolder {
        final View grupoEstado, progreso, btnReintentar, btnAnadir, btnDescartar;
        final TextView tvEstado;

        FinalVH(View v) {
            super(v);
            grupoEstado = v.findViewById(R.id.grupoEstado);
            progreso = v.findViewById(R.id.progresoCarga);
            tvEstado = v.findViewById(R.id.tvEstado);
            btnReintentar = v.findViewById(R.id.btnReintentarCarga);
            btnAnadir = v.findViewById(R.id.btnAnadirEjercicio);
            btnDescartar = v.findViewById(R.id.btnDescartar);
        }

        void pintar(Fila f) {
            boolean cargando = f.carga == SesionEnCurso.Carga.CARGANDO;
            boolean error = f.carga == SesionEnCurso.Carga.ERROR;
            grupoEstado.setVisibility(cargando || error || f.vacia ? View.VISIBLE : View.GONE);
            progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
            btnReintentar.setVisibility(error ? View.VISIBLE : View.GONE);
            tvEstado.setText(cargando ? R.string.envivo_cargando
                    : error ? R.string.envivo_error_carga : R.string.envivo_vacia);
            btnReintentar.setOnClickListener(v -> acciones.reintentarCarga());
            btnAnadir.setVisibility(f.editable ? View.VISIBLE : View.GONE);
            btnDescartar.setVisibility(f.editable ? View.VISIBLE : View.GONE);
            btnAnadir.setOnClickListener(v -> acciones.anadirEjercicio());
            btnDescartar.setOnClickListener(v -> acciones.descartar());
        }
    }
}
