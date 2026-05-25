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

public class EjercicioPesoAdapter extends RecyclerView.Adapter<EjercicioPesoAdapter.ViewHolder> {

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

    public List<Item> getItems() { return items; }

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
