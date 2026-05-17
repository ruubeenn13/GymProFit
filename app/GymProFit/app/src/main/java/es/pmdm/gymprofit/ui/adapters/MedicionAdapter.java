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

public class MedicionAdapter extends RecyclerView.Adapter<MedicionAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(MedicionCorporal medicion);
    }

    private final List<MedicionCorporal> items;
    private final OnDeleteListener deleteListener;
    private final Context context;

    public MedicionAdapter(Context context, List<MedicionCorporal> items, OnDeleteListener deleteListener) {
        this.context = context;
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        MedicionCorporal m = items.get(position);

        h.tvFecha.setText(m.getFecha().isEmpty() ? "—" : m.getFecha());
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

        h.btnEliminar.setOnClickListener(v -> deleteListener.onDelete(m));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvPeso, tvImc, tvGrasa, tvMusculo;
        ImageView btnEliminar;
        LinearLayout llExtras;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha    = itemView.findViewById(R.id.tvFechaMedicion);
            tvPeso     = itemView.findViewById(R.id.tvPesoMedicion);
            tvImc      = itemView.findViewById(R.id.tvImcMedicion);
            tvGrasa    = itemView.findViewById(R.id.tvGrasaMedicion);
            tvMusculo  = itemView.findViewById(R.id.tvMusculoMedicion);
            btnEliminar = itemView.findViewById(R.id.btnEliminarMedicion);
            llExtras   = itemView.findViewById(R.id.llExtras);
        }
    }
}
