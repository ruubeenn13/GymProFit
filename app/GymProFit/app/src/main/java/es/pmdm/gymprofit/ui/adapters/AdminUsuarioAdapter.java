package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;

public class AdminUsuarioAdapter extends RecyclerView.Adapter<AdminUsuarioAdapter.ViewHolder> {

    private final List<Usuario> items;

    public AdminUsuarioAdapter(List<Usuario> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_usuario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Usuario u = items.get(position);
        h.tvUsername.setText(u.getUsername());
        h.tvEmail.setText(u.getEmail().isEmpty() ? "—" : u.getEmail());
        String rol = u.getRol() != null ? u.getRol().replace("ROLE_", "") : "USER";
        h.chipRol.setText(rol);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvEmail;
        Chip chipRol;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsernameAdmin);
            tvEmail    = itemView.findViewById(R.id.tvEmailAdmin);
            chipRol    = itemView.findViewById(R.id.chipRolAdmin);
        }
    }
}
