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
// ============================================================
public class EjercicioPesoAdapter extends RecyclerView.Adapter<EjercicioPesoAdapter.ViewHolder> {

    /** Una serie concreta, tal y como la va rellenando el usuario. */
    public static class Serie {
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
    public static class Item {
        public final int ejercicioId;
        public final String nombre;
        /** Series que pedía la rutina. Se usa solo para la cabecera y la precarga. */
        public final int series;
        /** Repeticiones que pedía la rutina. */
        public final int repeticiones;
        /** Lo realmente hecho, una entrada por serie. */
        public final List<Serie> realizadas = new ArrayList<>();

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

    public EjercicioPesoAdapter(List<Item> items) {
        this.items = items;
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
        holder.tvNombre.setText(item.nombre);
        holder.tvSeriesReps.setText(item.series + " × " + item.repeticiones);

        // Se vacía y se vuelve a montar: el ViewHolder se recicla y un ejercicio
        // puede tener un número de series distinto del que tenía el anterior.
        holder.contenedor.removeAllViews();
        for (Serie serie : item.realizadas) {
            holder.contenedor.addView(crearFilaSerie(holder.contenedor, serie));
        }
    }

    /**
     * Monta la fila de una serie y la deja enganchada al modelo.
     *
     * @param padre contenedor al que se añadirá, solo para inflar con sus reglas.
     * @param serie la serie que representa; se escribe directamente sobre ella.
     * @return la fila lista para añadir.
     */
    private View crearFilaSerie(ViewGroup padre, Serie serie) {
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
        // Escribir un peso MARCA la serie como hecha. Si hubiera que pulsar además
        // el check, quien rellenara sus cuatro series sin tocarlo guardaría "0
        // series completadas" y cualquier estadística construida encima mentiría.
        // El check queda para lo contrario: desmarcar una serie que se apuntó pero
        // no se llegó a terminar.
        etPeso.addTextChangedListener(new EscribeEn(valor -> {
            serie.peso = valor;
            if (!valor.isEmpty() && !serie.completada) {
                serie.completada = true;
                pintarCompletada(ivCheck, true);
            }
        }));
        etReps.addTextChangedListener(new EscribeEn(valor -> serie.repeticiones = valor));

        pintarCompletada(ivCheck, serie.completada);
        ivCheck.setOnClickListener(v -> {
            serie.completada = !serie.completada;
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
