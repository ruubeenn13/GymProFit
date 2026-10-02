package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.utils.Cantidades;
import es.pmdm.gymprofit.utils.Categorias;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// AlimentoComidaAdapter — los alimentos de una comida (decisión 16, tablero 2, lote 1.6.2)
//
// Cada fila: el icono de su categoría en un cuadro de 44, el nombre en una línea, la
// cantidad debajo (Cantidades: «2 rebanadas (56 g)», «150 g») y a la derecha las kcal y
// la proteína. Tocarla abre la ficha; mantenerla pulsada abre su menú; para TalkBack lleva
// la acción «Quitar de la comida», que hace lo mismo que deslizarla.
// La primera vez que se pinta la lista, las filas entran en cascada (momento 16).
// ============================================================
public class AlimentoComidaAdapter extends RecyclerView.Adapter<AlimentoComidaAdapter.ViewHolder> {

    /** Lo que se puede hacer con una fila. */
    public interface Acciones {
        /** Tocarla: la ficha con su cantidad y «Actualizar». */
        void abrir(@NonNull AlimentoComida item);

        /** Mantenerla pulsada: su menú. */
        void menu(@NonNull AlimentoComida item, @NonNull View fila);

        /** Quitarla de la comida, sin preguntar (deslizar o la acción de TalkBack). */
        void quitar(@NonNull AlimentoComida item);
    }

    private final List<AlimentoComida> items;
    private final Acciones acciones;
    // Hasta qué posición entra en cascada: solo la primera vez que se pinta la lista.
    private int cascadaHasta = -1;

    public AlimentoComidaAdapter(@NonNull List<AlimentoComida> items, @NonNull Acciones acciones) {
        this.items = items;
        this.acciones = acciones;
    }

    /** La próxima vez que se pinte, las filas entran en cascada. */
    public void entrarEnCascada() {
        cascadaHasta = Math.min(items.size(), 14);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alimento_comida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AlimentoComida item = items.get(position);
        Context ctx = h.itemView.getContext();
        NumberFormat nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(ctx));
        nf.setMaximumFractionDigits(0);

        String nombre = item.getNombreAlimento();
        String cantidad = Cantidades.de(ctx, item);
        String kcal = nf.format(item.getCaloriasTotales());
        String prot = nf.format(Math.round(item.getProteinasTotales()));
        h.icono.setImageResource(Categorias.icono(item.getCategoriaAlimento()));
        h.nombre.setText(nombre);
        h.cantidad.setText(cantidad);
        h.kcal.setText(ctx.getString(R.string.comida_fila_kcal, kcal));
        h.proteina.setText(ctx.getString(R.string.comida_fila_prot, prot));
        h.itemView.setBackgroundResource(position == items.size() - 1
                ? R.drawable.bg_fila_comida_ultima : R.drawable.bg_fila_comida);
        h.itemView.setContentDescription(ctx.getString(R.string.comida_fila_a11y, nombre, cantidad, kcal, prot));

        h.itemView.setOnClickListener(v -> acciones.abrir(item));
        h.itemView.setOnLongClickListener(v -> {
            acciones.menu(item, v);
            return true;
        });
        ViewCompat.removeAccessibilityAction(h.itemView, h.accionQuitar);
        h.accionQuitar = ViewCompat.addAccessibilityAction(h.itemView, ctx.getString(R.string.comida_quitar),
                (v, args) -> {
                    acciones.quitar(item);
                    return true;
                });

        if (position < cascadaHasta) {
            Movimiento.entrarUna(h.itemView, position * Movimiento.CASCADA_ESCALON, Movimiento.CASCADA, 16);
            if (position >= cascadaHasta - 1) cascadaHasta = -1;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icono;
        final TextView nombre, cantidad, kcal, proteina;
        int accionQuitar = View.NO_ID;

        ViewHolder(View v) {
            super(v);
            icono = v.findViewById(R.id.ivCategoriaFila);
            nombre = v.findViewById(R.id.tvNombreAlimento);
            cantidad = v.findViewById(R.id.tvCantidadGramos);
            kcal = v.findViewById(R.id.tvCaloriasItem);
            proteina = v.findViewById(R.id.tvProteinaItem);
        }
    }
}
