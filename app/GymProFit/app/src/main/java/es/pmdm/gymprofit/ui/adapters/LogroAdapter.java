package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.Logro;

// ============================================================
// LogroAdapter — adapter de RecyclerView para el listado de logros del usuario.
// Muestra todos los Logro disponibles con su icono, nombre y descripción,
// atenuando visualmente (alpha + sin check) los que aún no están en el
// conjunto de IDs desbloqueados por el usuario en la app de gamificación.
//
// El icono sale del TIPO del logro (ver ICONOS_POR_TIPO). Antes salía del campo
// "icono" del modelo, que la API no envía — no existe en LogroDTO ni columna en
// la tabla —, así que siempre era null y los SEIS logros acababan con el mismo
// emoji de trofeo escrito como literal de respaldo. El tipo sí viaja en el DTO
// (enum TipoLogro) y es estable, así que no hace falta migración para
// distinguirlos.
// ============================================================
public class LogroAdapter extends RecyclerView.Adapter<LogroAdapter.ViewHolder> {

    // Un icono por tipo de logro. Las claves son los valores del enum TipoLogro de
    // la API tal y como llegan serializados. Un tipo que no esté aquí —porque el
    // catálogo crezca en el servidor y la app no se haya actualizado— cae en
    // ic_logro, una insignia neutra que no promete un logro concreto.
    private static final Map<String, Integer> ICONOS_POR_TIPO = new HashMap<>();
    static {
        ICONOS_POR_TIPO.put("PRIMERA_SESION",    R.drawable.ic_logro_primera_sesion);
        ICONOS_POR_TIPO.put("CONSTANCIA",        R.drawable.ic_logro_constancia);
        ICONOS_POR_TIPO.put("DEDICADO",          R.drawable.ic_logro_dedicado);
        ICONOS_POR_TIPO.put("CENTENARIO",        R.drawable.ic_logro_centenario);
        ICONOS_POR_TIPO.put("OBJETIVO_CUMPLIDO", R.drawable.ic_logro_objetivo_cumplido);
        ICONOS_POR_TIPO.put("MAQUINA",           R.drawable.ic_logro_maquina);
    }

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

        h.ivIcono.setImageResource(iconoDe(logro.getTipo()));

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

    // Icono del tipo, o la insignia neutra si el tipo es desconocido o no viene.
    @DrawableRes
    private static int iconoDe(String tipo) {
        Integer icono = tipo != null ? ICONOS_POR_TIPO.get(tipo) : null;
        return icono != null ? icono : R.drawable.ic_logro;
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ViewHolder con las referencias a las vistas de cada fila de logro.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDesc;
        ImageView ivIcono, ivCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono  = itemView.findViewById(R.id.ivIconoLogro);
            tvNombre = itemView.findViewById(R.id.tvNombreLogro);
            tvDesc   = itemView.findViewById(R.id.tvDescLogro);
            ivCheck  = itemView.findViewById(R.id.ivDesbloqueado);
        }
    }
}
