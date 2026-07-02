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
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// AdminUsuarioAdapter — adapter del listado de usuarios en el panel de administración.
// Pinta cada usuario con su rol y estado (activo/inactivo), y expone un menú
// de acciones para activar/desactivar la cuenta o cambiar su rol.
// ============================================================
public class AdminUsuarioAdapter extends RecyclerView.Adapter<AdminUsuarioAdapter.ViewHolder> {

    // Callback hacia la Activity/Fragment para las acciones del menú contextual.
    public interface OnAccionListener {
        void onToggleActivo(Usuario usuario, int position);
        void onCambiarRol(Usuario usuario, int position);
    }

    private final List<Usuario> items;
    private final OnAccionListener listener;

    public AdminUsuarioAdapter(List<Usuario> items, OnAccionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_usuario_v2, parent, false);
        return new ViewHolder(view);
    }

    // Enlaza los datos del usuario con las vistas y configura el chip de estado
    // y el menú de acciones (activar-desactivar / cambiar rol).
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Usuario u = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvUsername.setText(u.getUsername());
        h.tvEmail.setText(u.getEmail().isEmpty() ? "—" : u.getEmail());

        String rol = u.getRol() != null ? u.getRol().replace("ROLE_", "") : "USER";
        h.chipRol.setText(rol);

        if (u.isActivo()) {
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
            Usuario current = items.get(pos);
            List<UIHelper.MenuAction> actions = new ArrayList<>();
            actions.add(new UIHelper.MenuAction(
                    current.isActivo() ? R.drawable.ic_visibility_off : R.drawable.ic_check,
                    current.isActivo() ? ctx.getString(R.string.admin_desactivar) : ctx.getString(R.string.admin_activar),
                    () -> listener.onToggleActivo(current, pos)));
            actions.add(new UIHelper.MenuAction(R.drawable.ic_perfil,
                    ctx.getString(R.string.admin_cambiar_rol_titulo),
                    () -> listener.onCambiarRol(current, pos)));
            UIHelper.mostrarMenuAnclado(ctx, v, null, actions);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // Actualiza un ítem concreto tras editar/cambiar estado y notifica el cambio.
    public void actualizarItem(int position, Usuario usuario) {
        items.set(position, usuario);
        notifyItemChanged(position);
    }

    // Referencias a las vistas de cada item del listado de usuarios.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvEmail;
        Chip chipRol, chipEstado;
        ImageView btnAcciones;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername  = itemView.findViewById(R.id.tvUsernameAdmin);
            tvEmail     = itemView.findViewById(R.id.tvEmailAdmin);
            chipRol     = itemView.findViewById(R.id.chipRolAdmin);
            chipEstado  = itemView.findViewById(R.id.chipEstadoAdmin);
            btnAcciones = itemView.findViewById(R.id.btnAccionesUsuario);
        }
    }
}
