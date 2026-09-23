package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import es.pmdm.gymprofit.R;

// ============================================================
// RutinasVacioHeaderAdapter — adapter de un solo ítem con el estado vacío de
// "Mis rutinas": el aviso de que el usuario no tiene rutinas propias y el rótulo
// que presenta las predefinidas de debajo como sugerencia.
//
// Se concatena entre la tarjeta "+ Nueva rutina" y el listado, de modo que las
// predefinidas quedan DENTRO del estado vacío en lugar de colgar del rótulo
// "Mis rutinas", que es lo que las hacía parecer del usuario.
//
// Su visibilidad se cambia con setVisible(): un adapter de 0 ítems desaparece del
// ConcatAdapter sin tocar el resto de la lista.
// ============================================================
public class RutinasVacioHeaderAdapter extends RecyclerView.Adapter<RutinasVacioHeaderAdapter.VH> {

    private boolean visible = false;

    // Muestra u oculta el bloque. No hace nada si el estado no cambia, para no
    // disparar animaciones de lista en cada recarga.
    public void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        if (visible) notifyItemInserted(0); else notifyItemRemoved(0);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutinas_vacio, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Bloque estático: todo su contenido viene de recursos.
    }

    @Override
    public int getItemCount() { return visible ? 1 : 0; }

    static class VH extends RecyclerView.ViewHolder {
        VH(@NonNull View itemView) { super(itemView); }
    }
}
