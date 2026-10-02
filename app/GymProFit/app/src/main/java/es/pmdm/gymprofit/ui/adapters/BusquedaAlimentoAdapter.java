package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.GruposBusqueda;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BusquedaAlimentoAdapter — la lista de Añadir y de Buscar (tableros 4 y 5, lote 1.6.1)
//
// Pinta lo que arma GruposBusqueda: cabeceras de sección, filas de alimento y, al final
// de una búsqueda, «¿No lo encuentras?». Las filas de un grupo hacen su tarjeta con los
// fondos bg_grupo_* (primera, media, última o sola) y una raya entre ellas.
// Cada fila dice el nombre, la insignia azul si los datos están revisados y, debajo,
// las kcal por 100 g y la proteína o, si es un producto, la marca y su primera ración.
// La primera lista entra en cascada (momento 16); al volver, ya está.
// ============================================================
public class BusquedaAlimentoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Lo que se puede hacer desde la lista. */
    public interface Acciones {
        void abrir(@NonNull Alimento alimento);

        /** ¿Tiene menú al mantenerlo pulsado? Los propios (y, para ADMIN, el catálogo). */
        boolean tieneOpciones(@NonNull Alimento alimento);

        void opciones(@NonNull Alimento alimento, @NonNull View fila);

        void escanear();

        void crear();
    }

    private static final int CABECERA = 0;
    private static final int ALIMENTO = 1;
    private static final int NO_LO_ENCUENTRAS = 2;

    private final Acciones acciones;
    private final List<GruposBusqueda.Elemento> elementos = new ArrayList<>();
    // Hasta qué posición entra en cascada: solo la primera vez que se pinta la lista.
    private int cascadaHasta = -1;

    public BusquedaAlimentoAdapter(@NonNull Acciones acciones) {
        this.acciones = acciones;
    }

    /**
     * Cambia la lista.
     *
     * @param nuevos  lo que pinta.
     * @param cascada true si entra en cascada (la primera vez de la pantalla).
     */
    public void poner(@NonNull List<GruposBusqueda.Elemento> nuevos, boolean cascada) {
        elementos.clear();
        elementos.addAll(nuevos);
        cascadaHasta = cascada ? Math.min(nuevos.size(), 14) : -1;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        switch (elementos.get(position).tipo) {
            case CABECERA: return CABECERA;
            case NO_LO_ENCUENTRAS: return NO_LO_ENCUENTRAS;
            default: return ALIMENTO;
        }
    }

    @Override
    public int getItemCount() {
        return elementos.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == CABECERA) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_seccion_busqueda, parent, false)) { };
        }
        if (viewType == NO_LO_ENCUENTRAS) {
            View v = inflater.inflate(R.layout.item_no_lo_encuentras, parent, false);
            v.findViewById(R.id.btnEscanealo).setOnClickListener(b -> acciones.escanear());
            v.findViewById(R.id.btnCrealo).setOnClickListener(b -> acciones.crear());
            return new RecyclerView.ViewHolder(v) { };
        }
        return new FilaHolder(inflater.inflate(R.layout.item_alimento_busqueda, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GruposBusqueda.Elemento e = elementos.get(position);
        if (e.tipo == GruposBusqueda.Tipo.CABECERA) {
            ((TextView) holder.itemView).setText(tituloSeccion(e.seccion));
        } else if (e.tipo == GruposBusqueda.Tipo.ALIMENTO && e.alimento != null) {
            ((FilaHolder) holder).pintar(e.alimento, e.seccion, fondo(position));
        }
        if (position < cascadaHasta) {
            Movimiento.entrarUna(holder.itemView, position * Movimiento.CASCADA_ESCALON, Movimiento.CASCADA, 16);
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationY(0f);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Lo que ya entró no vuelve a entrar al hacer scroll.
        if (holder.getBindingAdapterPosition() >= cascadaHasta - 1) cascadaHasta = -1;
    }

    // Primera, media, última o sola dentro de su grupo, según los vecinos.
    private int fondo(int pos) {
        boolean antes = pos > 0 && elementos.get(pos - 1).tipo == GruposBusqueda.Tipo.ALIMENTO;
        boolean despues = pos + 1 < elementos.size() && elementos.get(pos + 1).tipo == GruposBusqueda.Tipo.ALIMENTO;
        if (antes && despues) return R.drawable.bg_grupo_media;
        if (antes) return R.drawable.bg_grupo_ultima;
        if (despues) return R.drawable.bg_grupo_primera;
        return R.drawable.bg_grupo_solo;
    }

    private static int tituloSeccion(@Nullable GruposBusqueda.Seccion s) {
        if (s == null) return R.string.seccion_basicos;
        switch (s) {
            case RECIENTES: return R.string.seccion_recientes;
            case HABITUALES: return R.string.seccion_habituales;
            case TUYO: return R.string.seccion_tuyo;
            case PRODUCTOS: return R.string.seccion_productos;
            default: return R.string.seccion_basicos;
        }
    }

    /**
     * El detalle de una fila: «110 kcal por 100 g · 23 g de proteína» o, si es un
     * producto, «Marca · 1 envase (200 g)».
     */
    @NonNull
    public static String detalle(@NonNull Context ctx, @NonNull Alimento a) {
        NumberFormat nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(ctx));
        nf.setMaximumFractionDigits(1);
        if (a.esProducto()) {
            String marca = a.getMarca() != null && !a.getMarca().trim().isEmpty()
                    ? a.getMarca().trim() : ctx.getString(R.string.fila_producto_envasado);
            List<Racion> r = a.getRaciones();
            String racion = r.isEmpty() ? ctx.getString(R.string.fila_por_100)
                    : es.pmdm.gymprofit.utils.Cantidades.de(ctx, r.get(0).getNombre(), r.get(0).getUnidad(),
                    r.get(0).getUnidadPlural(), 1.0, r.get(0).getGramos());
            return ctx.getString(R.string.fila_producto, marca, racion);
        }
        return ctx.getString(R.string.fila_kcal_proteina, nf.format(a.getCalorias()), nf.format(a.getProteinas()));
    }

    final class FilaHolder extends RecyclerView.ViewHolder {
        private final ImageView icono;
        private final TextView nombre;
        private final TextView detalle;
        private final View raya;

        FilaHolder(@NonNull View v) {
            super(v);
            icono = v.findViewById(R.id.ivIconoFila);
            nombre = v.findViewById(R.id.tvNombreFila);
            detalle = v.findViewById(R.id.tvDetalleFila);
            raya = v.findViewById(R.id.rayaFila);
        }

        void pintar(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion seccion, int fondo) {
            Context ctx = itemView.getContext();
            boolean tuyo = seccion == GruposBusqueda.Seccion.TUYO || seccion == GruposBusqueda.Seccion.RECIENTES;
            icono.setImageResource(tuyo ? R.drawable.ic_ms_history
                    : a.esProducto() ? R.drawable.ic_ms_inventory_2 : R.drawable.ic_ms_restaurant);
            nombre.setText(es.pmdm.gymprofit.ui.nutricion.Insignia.nombre(ctx, a.getNombre(), a.isRevisado(), 18));
            String det = detalle(ctx, a);
            detalle.setText(det);
            itemView.setBackgroundResource(fondo);
            raya.setVisibility(fondo == R.drawable.bg_grupo_media || fondo == R.drawable.bg_grupo_primera
                    ? View.VISIBLE : View.INVISIBLE);
            itemView.setContentDescription(a.isRevisado()
                    ? ctx.getString(R.string.fila_revisada_a11y, a.getNombre(), det)
                    : ctx.getString(R.string.fila_a11y, a.getNombre(), det));
            itemView.setOnClickListener(v -> acciones.abrir(a));
            if (acciones.tieneOpciones(a)) {
                itemView.setOnLongClickListener(v -> {
                    acciones.opciones(a, v);
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
                itemView.setLongClickable(false);
            }
        }
    }
}
