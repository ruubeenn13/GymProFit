package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.utils.NivelColor;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// EjercicioAdapter — adapter de RecyclerView para el catálogo de ejercicios (GP-216, tablero 29).
// Cada fila: foto del ejercicio, nombre, «Grupo · Material» y el nivel con su punto de color.
// Sin imagen se cae a la silueta con la zona trabajada: nunca un bloque en blanco. Las filas
// forman una sola tarjeta (primera, media, última o sola). El filtrado (texto/grupo) se hace en
// el SERVIDOR vía /ejercicios/buscar (paginado); el adapter solo pinta la lista recibida y
// soporta append de páginas para el scroll infinito.
// ============================================================
public class EjercicioAdapter extends RecyclerView.Adapter<EjercicioAdapter.ViewHolder> {

    // Callback invocado al pulsar un ejercicio de la lista.
    public interface OnClickListener {
        void onClick(Ejercicio ejercicio);
    }

    // Callback con la fila pulsada, para que quien la use pueda abrir la ficha desde ella
    // (la tarjeta crece hasta la pantalla).
    public interface OnClickFilaListener {
        void onClick(Ejercicio ejercicio, View fila);
    }

    private final List<Ejercicio> ejercicios;
    private final OnClickFilaListener clickListener;

    // Constructor sin listener de click.
    public EjercicioAdapter(List<Ejercicio> ejercicios) {
        this(ejercicios, (OnClickFilaListener) null);
    }

    // Constructor con el click sin la fila.
    public EjercicioAdapter(List<Ejercicio> ejercicios, OnClickListener clickListener) {
        this(ejercicios, clickListener == null ? null : (e, fila) -> clickListener.onClick(e));
    }

    // Constructor principal: guarda la lista mostrada (copia defensiva).
    public EjercicioAdapter(List<Ejercicio> ejercicios, OnClickFilaListener clickListener) {
        this.ejercicios = new ArrayList<>(ejercicios);
        this.clickListener = clickListener;
    }

    // Infla el layout de un ítem de ejercicio y crea su ViewHolder.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio, parent, false);
        return new ViewHolder(view);
    }

    // Rellena las vistas de una fila con los datos del ejercicio.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context ctx = holder.itemView.getContext();
        Ejercicio ejercicio = ejercicios.get(position);
        holder.tvNombre.setText(ejercicio.getNombre());
        pintarFoto(holder.ivIcono, ejercicio);

        String grupo = UIHelper.traducirGrupoMuscular(ctx, ejercicio.getGrupoMuscular());
        String equipo = ejercicio.getEquipoNecesario();
        holder.tvDetalle.setText(equipo == null || equipo.trim().isEmpty()
                ? grupo : ctx.getString(R.string.biblioteca_detalle_fila, grupo, equipo.trim()));

        String nivel = UIHelper.traducirNivel(ctx, ejercicio.getDificultad());
        holder.tvNivel.setText(nivel);
        holder.puntoNivel.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(ctx, NivelColor.de(ejercicio.getDificultad()))));

        holder.itemView.setBackgroundResource(fondo(position));
        holder.itemView.setContentDescription(ctx.getString(R.string.biblioteca_fila_a11y,
                ejercicio.getNombre(), holder.tvDetalle.getText(), nivel));
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onClick(ejercicio, v));
        }
    }

    /**
     * La foto del ejercicio, o la silueta de la zona si no tiene: se ve mientras Glide
     * descarga y se queda si no hay imagen o falla. Reutilizado por «Los que más haces».
     */
    public static void pintarFoto(ImageView iv, Ejercicio ejercicio) {
        int silueta = UIHelper.cuerpoMuscular(ejercicio.getMusculoPrimario(), ejercicio.getGrupoMuscular());
        String imagen = ejercicio.getImagenUrl();
        if (imagen == null || imagen.isBlank()) {
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageResource(silueta);
        } else {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(iv.getContext()).load(imagen).placeholder(silueta).error(silueta).into(iv);
        }
    }

    // Primera, media, última o sola dentro de la tarjeta de la lista.
    private int fondo(int pos) {
        boolean antes = pos > 0;
        boolean despues = pos + 1 < ejercicios.size();
        if (antes && despues) return R.drawable.bg_grupo_media;
        if (antes) return R.drawable.bg_grupo_ultima;
        if (despues) return R.drawable.bg_grupo_primera;
        return R.drawable.bg_grupo_solo;
    }

    @Override
    public int getItemCount() {
        return ejercicios.size();
    }

    // Reemplaza la lista completa (nueva búsqueda / primera página).
    public void setEjercicios(List<Ejercicio> nuevos) {
        ejercicios.clear();
        ejercicios.addAll(nuevos);
        notifyDataSetChanged();
    }

    // Añade una página adicional al final (scroll infinito). La que era la última deja de
    // serlo, así que se vuelve a pintar para que cierre la tarjeta donde toca.
    public void addEjercicios(List<Ejercicio> nuevos) {
        if (nuevos == null || nuevos.isEmpty()) return;
        int desde = ejercicios.size();
        ejercicios.addAll(nuevos);
        if (desde > 0) notifyItemChanged(desde - 1);
        notifyItemRangeInserted(desde, nuevos.size());
    }

    // ViewHolder con las referencias a las vistas de cada fila de ejercicio.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcono;
        final TextView tvNombre, tvDetalle, tvNivel;
        final View puntoNivel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono = itemView.findViewById(R.id.ivIconoEjercicio);
            tvNombre = itemView.findViewById(R.id.tvNombreEjercicio);
            tvDetalle = itemView.findViewById(R.id.tvDetalleEjercicio);
            tvNivel = itemView.findViewById(R.id.tvNivelEjercicio);
            puntoNivel = itemView.findViewById(R.id.puntoNivel);
        }
    }
}
