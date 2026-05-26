package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.utils.UIHelper;

public class AdminEjercicioAdapter extends RecyclerView.Adapter<AdminEjercicioAdapter.ViewHolder> {

    public interface OnAccionListener {
        void onToggleActivo(Ejercicio ejercicio, int position);
        void onEditar(Ejercicio ejercicio, int position);
    }

    private final List<Ejercicio> items;
    private final OnAccionListener listener;

    public AdminEjercicioAdapter(List<Ejercicio> items, OnAccionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_ejercicio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Ejercicio e = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvNombre.setText(e.getNombre());
        String desc = e.getDescripcion();
        h.tvDescripcion.setText((desc != null && !desc.isEmpty()) ? desc : "—");

        String grupo = e.getGrupoMuscular();
        h.chipGrupo.setText((grupo != null && !grupo.isEmpty()) ? grupo : "—");

        String dif = e.getDificultad();
        h.chipDificultad.setText((dif != null && !dif.isEmpty()) ? dif : "—");

        if (e.isActivo()) {
            h.chipEstado.setText(ctx.getString(R.string.admin_estado_activo));
            h.chipEstado.setChipBackgroundColorResource(R.color.green_chip);
            h.chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.green_chip_text));
        } else {
            h.chipEstado.setText(ctx.getString(R.string.admin_estado_inactivo));
            h.chipEstado.setChipBackgroundColorResource(R.color.red_chip);
            h.chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.red_chip_text));
        }

        h.btnAcciones.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Ejercicio current = items.get(pos);
            List<UIHelper.MenuAction> actions = new ArrayList<>();
            actions.add(new UIHelper.MenuAction(R.drawable.ic_edit,
                    ctx.getString(R.string.admin_editar_ejercicio_titulo),
                    () -> listener.onEditar(current, pos)));
            actions.add(new UIHelper.MenuAction(
                    current.isActivo() ? R.drawable.ic_visibility_off : R.drawable.ic_check,
                    current.isActivo() ? ctx.getString(R.string.admin_desactivar) : ctx.getString(R.string.admin_activar),
                    () -> listener.onToggleActivo(current, pos)));
            UIHelper.mostrarMenuAnclado(ctx, v, null, actions);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void actualizarItem(int position, Ejercicio ejercicio) {
        items.set(position, ejercicio);
        notifyItemChanged(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion;
        Chip chipGrupo, chipDificultad, chipEstado;
        ImageView btnAcciones;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre      = itemView.findViewById(R.id.tvNombreEjercicio);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionEjercicio);
            chipGrupo     = itemView.findViewById(R.id.chipGrupoEjercicio);
            chipDificultad= itemView.findViewById(R.id.chipDificultadEjercicio);
            chipEstado    = itemView.findViewById(R.id.chipEstadoEjercicio);
            btnAcciones   = itemView.findViewById(R.id.btnAccionesEjercicio);
        }
    }
}
