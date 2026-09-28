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

        public Item(int ejercicioId, String nombre, int series, int repeticiones) {
            this.ejercicioId = ejercicioId;
            this.nombre = nombre;
            this.series = series;
            this.repeticiones = repeticiones;

            // Se arranca con tantas series como pedía la rutina, con sus
            // repeticiones ya puestas: lo normal es cumplir el plan, así que el
            // usuario solo teclea el peso y corrige lo que se salga.
            for (int i = 1; i <= Math.max(1, series); i++) {
                realizadas.add(new Serie(i, String.valueOf(repeticiones)));
            }
        }
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
        holder.tvSeriesReps.setText(holder.itemView.getContext().getString(R.string.series_por_reps, item.series, item.repeticiones));

        // Se vacía y se vuelve a montar: el ViewHolder se recicla y un ejercicio
        // puede tener un número de series distinto del que tenía el anterior.
        holder.contenedor.removeAllViews();
        for (int i = 0; i < item.realizadas.size(); i++) {
            holder.contenedor.addView(crearFilaSerie(holder.contenedor, holder, item.realizadas.get(i), i));
        }
    }

    /**
     * Monta la fila de una serie y la deja enganchada al modelo.
     *
     * @param padre   contenedor al que se añadirá, solo para inflar con sus reglas.
     * @param holder  la tarjeta del ejercicio, para saber su posición al escribir.
     * @param serie   la serie que se pinta; se lee de ella, no se escribe.
     * @param indice  qué serie del ejercicio es.
     * @return la fila lista para añadir.
     */
    private View crearFilaSerie(ViewGroup padre, ViewHolder holder, Serie serie, int indice) {
        View fila = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_serie, padre, false);

        ((TextView) fila.findViewById(R.id.tvNumeroSerie)).setText(String.valueOf(serie.numero));

        TextInputEditText etPeso = fila.findViewById(R.id.etPesoSerie);
        TextInputEditText etReps = fila.findViewById(R.id.etRepsSerie);
        ImageView ivCheck = fila.findViewById(R.id.ivSerieCompletada);

        etPeso.setText(serie.peso);
        etReps.setText(serie.repeticiones);

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
        TextView tvNombre, tvSeriesReps;
        LinearLayout contenedor;

        ViewHolder(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombreEjercicioPeso);
            tvSeriesReps = v.findViewById(R.id.tvSeriesRepsPeso);
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
