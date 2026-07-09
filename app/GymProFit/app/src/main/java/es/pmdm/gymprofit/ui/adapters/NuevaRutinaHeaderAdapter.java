package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import es.pmdm.gymprofit.R;

// ============================================================
// NuevaRutinaHeaderAdapter — adapter de un solo ítem (la tarjeta "+ Nueva rutina")
// que se concatena ANTES del listado de rutinas mediante ConcatAdapter. Al pulsar la
// tarjeta invoca el callback de creación. Se usa en vez de un FAB para que el botón
// de crear no quede oculto tras la barra de navegación flotante.
// ============================================================
public class NuevaRutinaHeaderAdapter extends RecyclerView.Adapter<NuevaRutinaHeaderAdapter.VH> {

    public interface OnCrearClick { void onCrear(); }

    private final OnCrearClick callback;

    public NuevaRutinaHeaderAdapter(OnCrearClick callback) { this.callback = callback; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nueva_rutina, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.itemView.setOnClickListener(v -> { if (callback != null) callback.onCrear(); });
    }

    @Override
    public int getItemCount() { return 1; }

    static class VH extends RecyclerView.ViewHolder {
        VH(@NonNull View itemView) { super(itemView); }
    }
}
