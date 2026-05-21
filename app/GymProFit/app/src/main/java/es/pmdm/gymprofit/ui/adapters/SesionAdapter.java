package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

public class SesionAdapter extends RecyclerView.Adapter<SesionAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(SesionEntrenamiento sesion);
    }

    public interface OnClickListener {
        void onClick(SesionEntrenamiento sesion);
    }

    private final List<SesionEntrenamiento> items;
    private final Map<Integer, String> rutinaNombres;
    private final OnDeleteListener deleteListener;
    private final OnClickListener clickListener;
    private final Context context;

    public SesionAdapter(Context context, List<SesionEntrenamiento> items,
                         Map<Integer, String> rutinaNombres,
                         OnDeleteListener deleteListener,
                         OnClickListener clickListener) {
        this.context = context;
        this.items = items;
        this.rutinaNombres = rutinaNombres;
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sesion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SesionEntrenamiento s = items.get(position);

        h.tvFecha.setText(s.getFechaInicio().isEmpty() ? "—" : s.getFechaInicio());

        String rutinaNombre = rutinaNombres.get(s.getRutinaId());
        h.tvRutina.setText(rutinaNombre != null ? rutinaNombre
                : context.getString(R.string.sesiones_sin_rutina));

        h.tvDuracion.setText(context.getString(R.string.sesiones_min, s.getDuracionMinutos()));
        h.tvCalorias.setText(context.getString(R.string.sesiones_kcal, s.getCaloriasQuemadas()));

        h.btnEliminar.setOnClickListener(v -> deleteListener.onDelete(s));
        if (clickListener != null) {
            h.itemView.setOnClickListener(v -> clickListener.onClick(s));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvRutina, tvDuracion, tvCalorias;
        ImageView btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha    = itemView.findViewById(R.id.tvFechaSesion);
            tvRutina   = itemView.findViewById(R.id.tvRutinaSesion);
            tvDuracion = itemView.findViewById(R.id.tvDuracionSesion);
            tvCalorias = itemView.findViewById(R.id.tvCaloriasSesion);
            btnEliminar = itemView.findViewById(R.id.btnEliminarSesion);
        }
    }
}
