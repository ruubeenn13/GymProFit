package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.Logro;

// ============================================================
// LogroAdapter — adapter de RecyclerView para el listado de logros del usuario.
// Muestra todos los Logro disponibles con su icono, nombre y descripción,
// atenuando visualmente (alpha + sin check) los que aún no están en el
// conjunto de IDs desbloqueados por el usuario en la app de gamificación.
// ============================================================
public class LogroAdapter extends RecyclerView.Adapter<LogroAdapter.ViewHolder> {

    private final List<Logro> items;
    private final Set<Integer> desbloqueados;

    // Constructor: recibe todos los logros y el conjunto de IDs desbloqueados.
    public LogroAdapter(List<Logro> items, Set<Integer> desbloqueados) {
        this.items = items;
        this.desbloqueados = desbloqueados;
    }

    // Infla el layout de un ítem de logro y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_logro, parent, false);
        return new ViewHolder(view);
    }

    // Rellena icono, nombre y descripción, y aplica el estado visual
    // (desbloqueado/bloqueado) según pertenezca o no al set de desbloqueados.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Logro logro = items.get(position);
        boolean desbloqueado = desbloqueados.contains(logro.getId());

        String icono = logro.getIcono();
        h.tvIcono.setText((icono != null && !icono.isEmpty()) ? icono : "🏆");

        h.tvNombre.setText(logro.getNombre());
        h.tvDesc.setText(logro.getDescripcion());

        if (desbloqueado) {
            h.ivCheck.setVisibility(View.VISIBLE);
            h.itemView.setAlpha(1f);
        } else {
            h.ivCheck.setVisibility(View.GONE);
            h.itemView.setAlpha(0.5f);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ViewHolder con las referencias a las vistas de cada fila de logro.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcono, tvNombre, tvDesc;
        ImageView ivCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcono  = itemView.findViewById(R.id.tvIconoLogro);
            tvNombre = itemView.findViewById(R.id.tvNombreLogro);
            tvDesc   = itemView.findViewById(R.id.tvDescLogro);
            ivCheck  = itemView.findViewById(R.id.ivDesbloqueado);
        }
    }
}
