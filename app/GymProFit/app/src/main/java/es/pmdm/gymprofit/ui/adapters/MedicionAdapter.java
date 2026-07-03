package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// MedicionAdapter — adapter de RecyclerView para el historial de mediciones corporales.
// Muestra fecha, peso, IMC y (si están disponibles) grasa corporal/masa muscular
// de cada MedicionCorporal, ocultando las secciones sin dato, y expone acciones
// de editar/eliminar para el seguimiento de progreso físico del usuario.
// ============================================================
public class MedicionAdapter extends RecyclerView.Adapter<MedicionAdapter.ViewHolder> {

    // Callback invocado al pulsar el botón de eliminar una medición.
    public interface OnDeleteListener {
        void onDelete(MedicionCorporal medicion);
    }

    // Callback invocado al pulsar el botón de editar una medición.
    public interface OnEditListener {
        void onEdit(MedicionCorporal medicion);
    }

    private final List<MedicionCorporal> items;
    private final OnDeleteListener deleteListener;
    private final OnEditListener editListener;
    private final Context context;

    // Constructor: recibe contexto, lista de mediciones y listeners de edición/borrado.
    public MedicionAdapter(Context context, List<MedicionCorporal> items, OnDeleteListener deleteListener, OnEditListener editListener) {
        this.context = context;
        this.items = items;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    // Infla el layout de un ítem de medición y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicion, parent, false);
        return new ViewHolder(view);
    }

    // Rellena fecha, peso, IMC y datos extra (grasa/músculo) mostrando solo
    // las secciones con información disponible, y engancha editar/eliminar.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        MedicionCorporal m = items.get(position);

        h.tvFecha.setText(m.getFecha().isEmpty() ? "—" : FechaUtils.formatearFechaHora(m.getFecha()));
        h.tvPeso.setText(String.format(Locale.getDefault(), "%.1f kg", m.getPeso()));

        if (m.getImc() > 0) {
            h.tvImc.setText(String.format(Locale.getDefault(), "IMC: %.1f", m.getImc()));
            h.tvImc.setVisibility(View.VISIBLE);
        } else {
            h.tvImc.setVisibility(View.GONE);
        }

        boolean hayExtras = m.getGrasaCorporal() > 0 || m.getMasaMuscular() > 0;
        if (hayExtras) {
            h.llExtras.setVisibility(View.VISIBLE);
            h.tvGrasa.setText(m.getGrasaCorporal() > 0
                    ? String.format(Locale.getDefault(), "Grasa: %.1f%%", m.getGrasaCorporal()) : "");
            h.tvMusculo.setText(m.getMasaMuscular() > 0
                    ? String.format(Locale.getDefault(), "Músculo: %.1f kg", m.getMasaMuscular()) : "");
        } else {
            h.llExtras.setVisibility(View.GONE);
        }

        h.btnEditar.setOnClickListener(v -> editListener.onEdit(m));
        h.btnEliminar.setOnClickListener(v -> deleteListener.onDelete(m));
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ViewHolder con las referencias a las vistas de cada fila de medición.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvPeso, tvImc, tvGrasa, tvMusculo;
        ImageView btnEditar, btnEliminar;
        LinearLayout llExtras;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha     = itemView.findViewById(R.id.tvFechaMedicion);
            tvPeso      = itemView.findViewById(R.id.tvPesoMedicion);
            tvImc       = itemView.findViewById(R.id.tvImcMedicion);
            tvGrasa     = itemView.findViewById(R.id.tvGrasaMedicion);
            tvMusculo   = itemView.findViewById(R.id.tvMusculoMedicion);
            btnEditar   = itemView.findViewById(R.id.btnEditarMedicion);
            btnEliminar = itemView.findViewById(R.id.btnEliminarMedicion);
            llExtras    = itemView.findViewById(R.id.llExtras);
        }
    }
}
