package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// BibliotecaCabeceraAdapter — la primera pieza de la lista de la Biblioteca (GP-216, tablero 29):
// «Los que más haces» en un carrusel y el rótulo de la lista con cuántos ejercicios hay. Es una
// sola vista dentro del mismo RecyclerView, así se desplaza con las filas. «Los que más haces»
// solo se enseña si hay algo y mientras no se filtra ni se busca: con un filtro puesto, lo que
// se pide es la lista, no un atajo.
// ============================================================
public class BibliotecaCabeceraAdapter extends RecyclerView.Adapter<BibliotecaCabeceraAdapter.ViewHolder> {

    /** Una tarjeta de «Los que más haces». */
    public static final class MasHaces {
        public final Ejercicio ejercicio;
        /** La marca en dorado («80 kg × 10»), o null si el ejercicio no tiene récord. */
        @Nullable public final String marca;
        /** En cuántas sesiones sale; es lo que se enseña cuando no hay marca. */
        public final long veces;

        public MasHaces(Ejercicio ejercicio, @Nullable String marca, long veces) {
            this.ejercicio = ejercicio;
            this.marca = marca;
            this.veces = veces;
        }
    }

    /** Lo que pasa al tocar una tarjeta: el ejercicio y la propia tarjeta (para la transición). */
    public interface AlTocar {
        void abrir(Ejercicio ejercicio, View tarjeta);
    }

    private final AlTocar alTocar;
    private final List<MasHaces> masHaces = new ArrayList<>();
    private boolean filtrando;
    private boolean buscando;
    private long cuantos = -1;
    private ViewHolder holder;

    public BibliotecaCabeceraAdapter(AlTocar alTocar) {
        this.alTocar = alTocar;
    }

    /** Las tarjetas de «Los que más haces»; vacía las quita. */
    public void setMasHaces(List<MasHaces> nuevas) {
        masHaces.clear();
        masHaces.addAll(nuevas);
        refrescar();
    }

    /** Si hay búsqueda o grupo puesto: entonces «Los que más haces» se esconde. */
    public void setFiltrando(boolean filtrando) {
        this.filtrando = filtrando;
        refrescar();
    }

    /** Si hay texto en el buscador: el rótulo de la lista pasa de «De la A a la Z» a «Resultados». */
    public void setBuscando(boolean buscando) {
        this.buscando = buscando;
        refrescar();
    }

    /** Cuántos ejercicios hay con el filtro actual, o -1 si aún no se sabe. */
    public void setCuantos(long cuantos) {
        this.cuantos = cuantos;
        refrescar();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_biblioteca_cabecera, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        holder = h;
        refrescar();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    private void refrescar() {
        if (holder == null) return;
        Context ctx = holder.itemView.getContext();
        boolean ver = !filtrando && !masHaces.isEmpty();
        holder.bloqueMasHaces.setVisibility(ver ? View.VISIBLE : View.GONE);
        if (ver) pintarCarrusel(ctx);
        holder.tvTituloLista.setText(buscando ? R.string.biblioteca_resultados : R.string.biblioteca_lista);
        if (cuantos < 0) {
            holder.tvCuantos.setText("");
        } else {
            String n = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx)).format(cuantos);
            holder.tvCuantos.setText(ctx.getResources().getQuantityString(
                    R.plurals.biblioteca_cuantos, (int) Math.min(cuantos, Integer.MAX_VALUE), n));
        }
    }

    private void pintarCarrusel(Context ctx) {
        LinearLayout lista = holder.listaMasHaces;
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        float dp = ctx.getResources().getDisplayMetrics().density;
        for (int i = 0; i < masHaces.size(); i++) {
            MasHaces m = masHaces.get(i);
            View tarjeta = inflater.inflate(R.layout.item_mas_haces, lista, false);
            ((TextView) tarjeta.findViewById(R.id.tvMasHacesNombre)).setText(m.ejercicio.getNombre());
            TextView marca = tarjeta.findViewById(R.id.tvMasHacesMarca);
            String texto = m.marca != null ? m.marca : ctx.getResources().getQuantityString(
                    R.plurals.biblioteca_veces, (int) Math.min(m.veces, Integer.MAX_VALUE), (int) m.veces);
            marca.setText(texto);
            // Sin récord, las veces van en el color de apoyo: el dorado es solo de lo conseguido.
            if (m.marca == null) marca.setTextColor(com.google.android.material.color.MaterialColors
                    .getColor(marca, com.google.android.material.R.attr.colorOnSurfaceVariant));
            EjercicioAdapter.pintarFoto((ImageView) tarjeta.findViewById(R.id.ivMasHaces), m.ejercicio);
            tarjeta.setContentDescription(ctx.getString(R.string.biblioteca_mas_haces_a11y,
                    m.ejercicio.getNombre(), texto));
            tarjeta.setOnClickListener(v -> alTocar.abrir(m.ejercicio, v));
            if (i > 0) ((LinearLayout.LayoutParams) tarjeta.getLayoutParams()).setMarginStart(Math.round(12 * dp));
            lista.addView(tarjeta);
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final View bloqueMasHaces;
        final LinearLayout listaMasHaces;
        final TextView tvCuantos, tvTituloLista;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            bloqueMasHaces = itemView.findViewById(R.id.bloqueMasHaces);
            listaMasHaces = itemView.findViewById(R.id.listaMasHaces);
            tvCuantos = itemView.findViewById(R.id.tvCuantos);
            tvTituloLista = itemView.findViewById(R.id.tvTituloLista);
        }
    }
}
