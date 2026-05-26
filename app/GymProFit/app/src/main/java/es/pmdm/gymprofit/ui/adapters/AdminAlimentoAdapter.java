package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.utils.UIHelper;

public class AdminAlimentoAdapter extends RecyclerView.Adapter<AdminAlimentoAdapter.ViewHolder> {

    public interface OnAccionListener {
        void onToggleActivo(Alimento alimento, int position);
        void onEditar(Alimento alimento, int position);
    }

    private final List<Alimento> items;
    private final OnAccionListener listener;

    public AdminAlimentoAdapter(List<Alimento> items, OnAccionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_alimento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Alimento a = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvNombre.setText(a.getNombre());

        String cat = a.getCategoria();
        h.tvCategoria.setText((cat != null && !cat.isEmpty()) ? cat : "—");

        h.tvCalorias.setText(a.getCalorias() + " kcal/100g");

        if (a.isActivo()) {
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
            Alimento current = items.get(pos);
            List<UIHelper.MenuAction> actions = new ArrayList<>();
            actions.add(new UIHelper.MenuAction(R.drawable.ic_edit,
                    ctx.getString(R.string.admin_editar_alimento_titulo),
                    () -> listener.onEditar(current, pos)));
            actions.add(new UIHelper.MenuAction(
                    current.isActivo() ? R.drawable.ic_visibility_off : R.drawable.ic_check,
                    current.isActivo() ? ctx.getString(R.string.admin_desactivar) : ctx.getString(R.string.admin_activar),
                    () -> listener.onToggleActivo(current, pos)));
            UIHelper.mostrarBottomMenu(ctx, null, actions);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    /**
     * Actualiza un ítem concreto y notifica el cambio.
     */
    public void actualizarItem(int position, Alimento alimento) {
        items.set(position, alimento);
        notifyItemChanged(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCategoria, tvCalorias;
        Chip chipEstado;
        ImageView btnAcciones;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre    = itemView.findViewById(R.id.tvNombreAlimento);
            tvCategoria = itemView.findViewById(R.id.tvCategoriaAlimento);
            tvCalorias  = itemView.findViewById(R.id.tvCaloriasAlimento);
            chipEstado  = itemView.findViewById(R.id.chipEstadoAlimento);
            btnAcciones = itemView.findViewById(R.id.btnAccionesAlimento);
        }
    }
}
