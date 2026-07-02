package es.pmdm.gymprofit.ui.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import es.pmdm.gymprofit.R;

// ============================================================
// EjercicioPesoAdapter — adapter de RecyclerView para registrar peso por ejercicio.
// Cada fila muestra un ejercicio de la sesión (nombre, series x repeticiones)
// junto a un campo editable donde el usuario introduce el peso usado; el
// texto tecleado se guarda directamente en el Item mediante un TextWatcher.
// ============================================================
public class EjercicioPesoAdapter extends RecyclerView.Adapter<EjercicioPesoAdapter.ViewHolder> {

    // Modelo local de fila: datos del ejercicio y el peso introducido por el usuario.
    public static class Item {
        public final int ejercicioId;
        public final String nombre;
        public final int series;
        public final int repeticiones;
        public String peso = "";

        public Item(int ejercicioId, String nombre, int series, int repeticiones) {
            this.ejercicioId = ejercicioId;
            this.nombre = nombre;
            this.series = series;
            this.repeticiones = repeticiones;
        }
    }

    private final List<Item> items;

    // Constructor: recibe la lista de ítems (ejercicio + peso) a mostrar.
    public EjercicioPesoAdapter(List<Item> items) {
        this.items = items;
    }

    // Infla el layout de un ítem ejercicio-peso y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_peso, parent, false);
        return new ViewHolder(v);
    }

    // Rellena nombre y series/repeticiones, y reengancha el TextWatcher del peso
    // evitando duplicar listeners al reciclarse la vista.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvNombre.setText(item.nombre);
        holder.tvSeriesReps.setText(item.series + " × " + item.repeticiones);

        if (holder.watcher != null) holder.etPeso.removeTextChangedListener(holder.watcher);
        holder.etPeso.setText(item.peso);
        holder.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { item.peso = s.toString().trim(); }
        };
        holder.etPeso.addTextChangedListener(holder.watcher);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // Devuelve la lista de ítems con los pesos introducidos por el usuario.
    public List<Item> getItems() { return items; }

    // ViewHolder con las referencias a las vistas de cada fila y su TextWatcher activo.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvSeriesReps;
        TextInputEditText etPeso;
        TextWatcher watcher;

        ViewHolder(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombreEjercicioPeso);
            tvSeriesReps = v.findViewById(R.id.tvSeriesRepsPeso);
            etPeso = v.findViewById(R.id.etPeso);
        }
    }
}
