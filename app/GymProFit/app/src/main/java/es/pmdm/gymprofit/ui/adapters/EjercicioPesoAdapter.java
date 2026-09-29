package es.pmdm.gymprofit.ui.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;

// ============================================================
// EjercicioPesoAdapter — registro de una sesión, SERIE A SERIE.
//
// Antes cada fila tenía un único campo de peso para el ejercicio entero, así que
// solo se podía guardar un número: quien hiciera 4×8 subiendo carga tenía que
// elegir uno y mentir. Y sin dato por serie no hay progresión de carga, ni
// récords personales, ni volumen levantado — que es justamente para lo que
// existe una app de entrenamiento.
//
// Cada fila es ahora un ejercicio con una línea por serie: peso, repeticiones
// REALES y marca de completada. Las repeticiones se precargan con las que pedía
// la rutina, pero son editables: fallar la última serie es información.
//
// GP-016. Lo tecleado ya no vive aquí: los Item y las Serie son el borrador de
// RegistrarSesionViewModel, que los guarda en su SavedStateHandle para que girar o que
// Android mate la app no los pierda. El adaptador los pinta y cada cambio se lo pasa
// al Editor, que es quien los escribe. Por eso son Serializable.
// ============================================================
public class EjercicioPesoAdapter extends RecyclerView.Adapter<EjercicioPesoAdapter.ViewHolder> {

    /**
     * Quien escribe en el borrador. El adaptador no toca los campos: avisa y luego
     * repinta lo que haya quedado (escribir un peso, por ejemplo, marca la serie hecha).
     * Los índices son los del ejercicio en la lista y de la serie dentro de él.
     */
    public interface Editor {
        void peso(int ejercicio, int serie, String valor);
        void repeticiones(int ejercicio, int serie, String valor);
        void alternarHecha(int ejercicio, int serie);
        /** Segundos de una serie por tiempo (GP-125). */
        void segundos(int ejercicio, int serie, String valor);
    }

    /**
     * Una serie concreta, tal y como la va rellenando el usuario. El peso y las
     * repeticiones son el TEXTO tecleado, no números: «72,» a medio escribir se
     * conserva tal cual, y se convierte solo al guardar.
     */
    public static class Serie implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final int numero;
        public String peso = "";
        public String repeticiones = "";
        /** Segundos, en las series por tiempo (GP-125); vacío en las demás. */
        public String segundos = "";
        public boolean completada = false;

        public Serie(int numero, String repeticiones) {
            this.numero = numero;
            this.repeticiones = repeticiones;
        }
    }

    /** Un ejercicio de la sesión, con lo que pedía la rutina y lo realmente hecho. */
    public static class Item implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final int ejercicioId;
        /** Nombre del catálogo; null si no vino, y entonces se pinta «Ejercicio N». */
        public final String nombre;
        /** Series que pedía la rutina. Se usa solo para la cabecera y la precarga. */
        public final int series;
        /** Repeticiones que pedía la rutina. */
        public final int repeticiones;
        /** Lo realmente hecho, una entrada por serie. */
        public final ArrayList<Serie> realizadas = new ArrayList<>();
        // La pauta de la rutina, si la trae (GP-074/GP-125): la medida (REPETICIONES o
        // SEGUNDOS), el rango y si es por lado. Nulos en las rutinas de siempre.
        public String medida;
        public Integer minimo;
        public Integer maximo;
        public String porLado;

        public Item(int ejercicioId, String nombre, int series, int repeticiones) {
            this(ejercicioId, nombre, series, repeticiones, null, null, null, null);
        }

        public Item(int ejercicioId, String nombre, int series, int repeticiones, String medida,
                    Integer minimo, Integer maximo, String porLado) {
            this.ejercicioId = ejercicioId;
            this.nombre = nombre;
            this.series = series;
            this.repeticiones = repeticiones;
            this.medida = medida;
            this.minimo = minimo;
            this.maximo = maximo;
            this.porLado = porLado;

            // Se arranca con tantas series como pedía la rutina, con sus
            // repeticiones ya puestas: lo normal es cumplir el plan, así que el
            // usuario solo teclea el peso y corrige lo que se salga. Por tiempo no se
            // precarga nada: cada serie pide sus segundos, y en blanco es que no se hizo.
            for (int i = 1; i <= Math.max(1, series); i++) {
                realizadas.add(new Serie(i, porTiempo() ? "" : String.valueOf(repeticiones)));
            }
        }

        /** Si las series se miden en segundos (plancha…): solo piden los segundos. */
        public boolean porTiempo() { return "SEGUNDOS".equals(medida); }
    }

    private final List<Item> items;
    private final Editor editor;

    /**
     * @param items  la lista que se pinta; quien la da la actualiza y avisa.
     * @param editor adónde va cada cambio que teclea el usuario.
     */
    public EjercicioPesoAdapter(List<Item> items, Editor editor) {
        this.items = items;
        this.editor = editor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_peso, parent, false);
        return new ViewHolder(v);
    }

    // Pinta la cabecera y reconstruye las filas de serie.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvNombre.setText(item.nombre != null ? item.nombre
                : holder.itemView.getContext().getString(R.string.ejercicio_sin_nombre, position + 1));
        // Lo que pedía la rutina, con su pauta: «3 × 8–12», «2 × 30–60 s» con el cronómetro.
        android.content.Context ctx = holder.itemView.getContext();
        es.pmdm.gymprofit.utils.Pauta.Formatos formatos = es.pmdm.gymprofit.utils.Pauta.Formatos.de(ctx);
        holder.tvSeriesReps.setText(es.pmdm.gymprofit.utils.Pauta.texto(formatos, item.series, item.minimo,
                item.maximo, item.repeticiones, item.medida, item.porLado));
        holder.tvSeriesReps.setCompoundDrawablesRelativeWithIntrinsicBounds(
                item.porTiempo() ? R.drawable.ic_ms_timer : 0, 0, 0, 0);
        // Por lado, la columna lo dice: «reps por pierna», «segundos por lado».
        String columna = columna(ctx, item);
        holder.tvColumna.setText(columna);
        holder.tvColumna.setVisibility(columna == null ? View.GONE : View.VISIBLE);

        // Se vacía y se vuelve a montar: el ViewHolder se recicla y un ejercicio
        // puede tener un número de series distinto del que tenía el anterior.
        holder.contenedor.removeAllViews();
        for (int i = 0; i < item.realizadas.size(); i++) {
            holder.contenedor.addView(crearFilaSerie(holder.contenedor, holder, item, item.realizadas.get(i), i));
        }
    }

    /**
     * Monta la fila de una serie y la deja enganchada al modelo.
     *
     * @param padre   contenedor al que se añadirá, solo para inflar con sus reglas.
     * @param holder  la tarjeta del ejercicio, para saber su posición al escribir.
     * @param item    el ejercicio, para saber si va por tiempo y su rango.
     * @param serie   la serie que se pinta; se lee de ella, no se escribe.
     * @param indice  qué serie del ejercicio es.
     * @return la fila lista para añadir.
     */
    private View crearFilaSerie(ViewGroup padre, ViewHolder holder, Item item, Serie serie, int indice) {
        View fila = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_serie, padre, false);

        ((TextView) fila.findViewById(R.id.tvNumeroSerie)).setText(String.valueOf(serie.numero));

        TextInputEditText etPeso = fila.findViewById(R.id.etPesoSerie);
        TextInputEditText etReps = fila.findViewById(R.id.etRepsSerie);
        TextInputEditText etSegundos = fila.findViewById(R.id.etSegundosSerie);
        ImageView ivCheck = fila.findViewById(R.id.ivSerieCompletada);

        // Por tiempo (GP-125): solo los segundos, sin peso ni repeticiones. En las demás,
        // el rango de la rutina de pista en las repeticiones.
        boolean porTiempo = item.porTiempo();
        fila.findViewById(R.id.tilPesoSerie).setVisibility(porTiempo ? View.GONE : View.VISIBLE);
        fila.findViewById(R.id.tvPorSerie).setVisibility(porTiempo ? View.GONE : View.VISIBLE);
        fila.findViewById(R.id.tilRepsSerie).setVisibility(porTiempo ? View.GONE : View.VISIBLE);
        fila.findViewById(R.id.tilSegundosSerie).setVisibility(porTiempo ? View.VISIBLE : View.GONE);
        es.pmdm.gymprofit.utils.Pauta.Formatos f = es.pmdm.gymprofit.utils.Pauta.Formatos.de(padre.getContext());
        if (porTiempo) {
            ((com.google.android.material.textfield.TextInputLayout) fila.findViewById(R.id.tilSegundosSerie))
                    // Solo el rango («30–60 s»): el cronómetro ya dice que son segundos, y a
                    // letra grande «Segundos · 30–60 s» se cortaba.
                    .setHint(es.pmdm.gymprofit.utils.Pauta.cantidad(f, item.minimo, item.maximo, item.repeticiones,
                            item.medida));
        } else if (item.minimo != null && item.maximo != null && !item.minimo.equals(item.maximo)) {
            ((com.google.android.material.textfield.TextInputLayout) fila.findViewById(R.id.tilRepsSerie))
                    .setHint(es.pmdm.gymprofit.utils.Pauta.cantidad(f, item.minimo, item.maximo, item.repeticiones,
                            item.medida));
        }

        etPeso.setText(serie.peso);
        etReps.setText(serie.repeticiones);
        etSegundos.setText(serie.segundos);
        etSegundos.addTextChangedListener(new EscribeEn(valor -> {
            editor.segundos(holder.getBindingAdapterPosition(), indice, valor);
            pintarCompletada(ivCheck, serie.completada);
        }));

        // Las filas se crean nuevas en cada bind, así que no hay listeners viejos
        // que quitar: cada TextWatcher vive lo que vive su vista.
        //
        // Escribir un peso marca la serie como hecha: esa regla vive en el ViewModel,
        // aquí solo se repinta el check con lo que haya quedado.
        etPeso.addTextChangedListener(new EscribeEn(valor -> {
            editor.peso(holder.getBindingAdapterPosition(), indice, valor);
            pintarCompletada(ivCheck, serie.completada);
        }));
        etReps.addTextChangedListener(new EscribeEn(valor ->
                editor.repeticiones(holder.getBindingAdapterPosition(), indice, valor)));

        pintarCompletada(ivCheck, serie.completada);
        ivCheck.setOnClickListener(v -> {
            editor.alternarHecha(holder.getBindingAdapterPosition(), indice);
            pintarCompletada(ivCheck, serie.completada);
        });

        return fila;
    }

    // «Reps por pierna», «Segundos por lado»…; null si el ejercicio no es por lado.
    static String columna(android.content.Context ctx, Item item) {
        if (item.porLado == null) return null;
        int id;
        switch (item.porLado) {
            case "PIERNA": id = item.porTiempo() ? R.string.registro_segundos_por_pierna : R.string.registro_reps_por_pierna; break;
            case "BRAZO":  id = item.porTiempo() ? R.string.registro_segundos_por_brazo : R.string.registro_reps_por_brazo; break;
            case "LADO":   id = item.porTiempo() ? R.string.registro_segundos_por_lado : R.string.registro_reps_por_lado; break;
            default: return null;
        }
        return ctx.getString(id);
    }

    // Una serie marcada se ve al 100% y en el naranja de marca; sin marcar, apagada.
    private void pintarCompletada(ImageView check, boolean completada) {
        check.setAlpha(completada ? 1f : 0.35f);
        check.setImageTintList(androidx.core.content.ContextCompat.getColorStateList(
                check.getContext(),
                completada ? R.color.gp_success : R.color.gp_text_secondary));
    }

    @Override
    public int getItemCount() { return items.size(); }

    /** Los ejercicios con lo que el usuario haya rellenado. */
    public List<Item> getItems() { return items; }

    // ViewHolder: cabecera y contenedor de las filas de serie.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvSeriesReps, tvColumna;
        LinearLayout contenedor;

        ViewHolder(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombreEjercicioPeso);
            tvSeriesReps = v.findViewById(R.id.tvSeriesRepsPeso);
            tvColumna = v.findViewById(R.id.tvColumnaSeries);
            contenedor = v.findViewById(R.id.contenedorSeries);
        }
    }

    // TextWatcher mínimo: solo interesa el texto final, ya recortado.
    private static class EscribeEn implements TextWatcher {
        interface Destino { void set(String valor); }

        private final Destino destino;

        EscribeEn(Destino destino) { this.destino = destino; }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { destino.set(s.toString().trim()); }
    }
}
