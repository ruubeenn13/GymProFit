package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.text.TextUtils;
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
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// SesionAdapter — adaptador RecyclerView para el listado de sesiones de entrenamiento.
// Muestra fecha, rutina asociada, duración y calorías quemadas de cada sesión.
// Notifica clicks de eliminación y de selección de item mediante listeners.
// ============================================================
public class SesionAdapter extends RecyclerView.Adapter<SesionAdapter.ViewHolder> {

    // Callback invocado al pulsar el icono de eliminar una sesión.
    public interface OnDeleteListener {
        void onDelete(SesionEntrenamiento sesion);
    }

    // Callback invocado al pulsar sobre el item completo de la sesión.
    public interface OnClickListener {
        void onClick(SesionEntrenamiento sesion, View tarjeta);
    }

    private final List<SesionEntrenamiento> items;
    private final Map<Integer, String> rutinaNombres;
    private final OnDeleteListener deleteListener;
    private final OnClickListener clickListener;
    private final Context context;

    // Constructor: recibe la lista de sesiones, el mapa de nombres de rutina (id -> nombre)
    // y los listeners de eliminación/click.
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

    // Infla el layout item_sesion y crea el ViewHolder correspondiente.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sesion, parent, false);
        return new ViewHolder(view);
    }

    // Vincula los datos de la sesión en la posición dada con las vistas del item.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SesionEntrenamiento s = items.get(position);

        java.util.Locale idioma = FechaUtils.localeDeLaApp(context);
        String fecha = FechaUtils.formatearFechaHoraMedia(s.getFechaInicio(), idioma);
        h.tvFecha.setText(fecha == null ? context.getString(R.string.sin_valor) : fecha);

        // Tres casos distintos, no dos (GP-057):
        //   rutinaId null            → entrenamiento libre, un estado válido del modelo
        //   rutinaId con nombre      → la rutina, por su nombre
        //   rutinaId sin nombre      → la rutina existe pero no está entre las activas
        //                              del usuario (archivada), así que no hay nombre
        if (s.esEntrenamientoLibre()) {
            h.tvRutina.setText(context.getString(R.string.sesiones_entrenamiento_libre));
        } else {
            String rutinaNombre = rutinaNombres.get(s.getRutinaId());
            h.tvRutina.setText(rutinaNombre != null ? rutinaNombre
                    : context.getString(R.string.sesiones_sin_rutina));
        }

        h.tvDuracion.setText(context.getString(R.string.sesiones_min, s.getDuracionMinutos()));

        // TalkBack lee la tarjeta de una vez: fecha, rutina y duración (DEC-019). La papelera
        // sigue aparte, con su propio nombre.
        h.itemView.setContentDescription(TextUtils.join(", ",
                new CharSequence[]{h.tvFecha.getText(), h.tvRutina.getText(), h.tvDuracion.getText()}));
        // Cada papelera dice qué sesión borra (GP-227): «Borrar Full body B del 30 de septiembre».
        String dia = FechaUtils.fechaCorta(s.getFechaInicio(), idioma,
                context.getString(R.string.sesiones_eliminar_fecha_patron));
        h.btnEliminar.setContentDescription(dia == null
                ? context.getString(R.string.sesiones_eliminar_a11y_sin_fecha, h.tvRutina.getText())
                : context.getString(R.string.sesiones_eliminar_a11y, h.tvRutina.getText(), dia));
        h.btnEliminar.setOnClickListener(v -> deleteListener.onDelete(s));
        if (clickListener != null) {
            h.itemView.setOnClickListener(v -> clickListener.onClick(s, v));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // Contiene las referencias a las vistas de un item de sesión.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvRutina, tvDuracion;
        ImageView btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha    = itemView.findViewById(R.id.tvFechaSesion);
            tvRutina   = itemView.findViewById(R.id.tvRutinaSesion);
            tvDuracion = itemView.findViewById(R.id.tvDuracionSesion);
            btnEliminar = itemView.findViewById(R.id.btnEliminarSesion);
        }
    }
}
