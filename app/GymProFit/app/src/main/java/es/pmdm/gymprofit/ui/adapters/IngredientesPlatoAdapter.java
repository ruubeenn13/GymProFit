package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.plato.IngredienteElegido;
import es.pmdm.gymprofit.utils.AnadirRapido;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.Cantidades;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.IngredientesPlato;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// IngredientesPlatoAdapter — los ingredientes en la pantalla del plato (tablero 10, 1.6.6)
// Cada fila: el icono de su categoría, el nombre, la cantidad («300 g», «2 rebanadas
// (56 g)») y sus kcal. Tocarla abre su cantidad; quitarla es deslizarla o, con TalkBack,
// la acción «Quitar». La primera vez entran en cascada (momento 16, como en el tablero).
// ============================================================
public class IngredientesPlatoAdapter extends RecyclerView.Adapter<IngredientesPlatoAdapter.Fila> {

    /** Lo que se hace con una fila. */
    public interface Acciones {
        void abrir(int posicion);

        void quitar(int posicion);
    }

    private final IngredientesPlato ingredientes;
    private final Acciones acciones;
    // Hasta qué fila entra en cascada: solo al pintarse por primera vez.
    private int cascadaHasta = -1;

    public IngredientesPlatoAdapter(@NonNull IngredientesPlato ingredientes, @NonNull Acciones acciones) {
        this.ingredientes = ingredientes;
        this.acciones = acciones;
    }

    /** Las filas que hay ahora entran en cascada la próxima vez que se pinten. */
    public void cascada() {
        cascadaHasta = Math.min(ingredientes.cuantos(), 14);
    }

    @Override
    public int getItemCount() {
        return ingredientes.cuantos();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Fila(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingrediente_plato, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila h, int posicion) {
        Context ctx = h.itemView.getContext();
        IngredienteElegido i = ingredientes.lista().get(posicion);
        CantidadFicha c = i.cantidad();
        String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(ctx), FechaUtils.localeDeLaApp(ctx), c);
        String kcal = ctx.getString(R.string.nutricion_kcal_valor,
                NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx)).format(AnadirRapido.kcal(i.getAlimento(), c)));
        h.icono.setImageResource(BusquedaAlimentoAdapter.icono(i.getAlimento()));
        h.nombre.setText(i.getAlimento().getNombre());
        h.cantidad.setText(cantidad);
        h.kcal.setText(kcal);
        h.itemView.setContentDescription(ctx.getString(R.string.plato_ingrediente_a11y, i.getAlimento().getNombre(),
                cantidad, kcal));
        h.itemView.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) acciones.abrir(p);
        });
        // TalkBack no desliza: «Quitar» es una acción de la fila.
        ViewCompat.replaceAccessibilityAction(h.itemView, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                ctx.getString(R.string.plato_ingrediente_cambiar_a11y), null);
        ViewCompat.removeAccessibilityAction(h.itemView, h.accionQuitar);
        h.accionQuitar = ViewCompat.addAccessibilityAction(h.itemView, ctx.getString(R.string.plato_ingrediente_quitar_a11y), (v, a) -> {
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) acciones.quitar(p);
            return true;
        });
        if (posicion < cascadaHasta) {
            Movimiento.entrarUna(h.itemView, posicion * Movimiento.CASCADA_ESCALON, Movimiento.CASCADA, 16);
        } else {
            h.itemView.setAlpha(1f);
            h.itemView.setTranslationY(0f);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull Fila holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.getBindingAdapterPosition() >= cascadaHasta - 1) cascadaHasta = -1;
    }

    static final class Fila extends RecyclerView.ViewHolder {
        final ImageView icono;
        final TextView nombre;
        final TextView cantidad;
        final TextView kcal;
        // La acción de TalkBack «Quitar», para no repetirla al volver a pintar la fila.
        int accionQuitar = View.NO_ID;

        Fila(@NonNull View v) {
            super(v);
            icono = v.findViewById(R.id.ivIconoIngrediente);
            nombre = v.findViewById(R.id.tvNombreIngrediente);
            cantidad = v.findViewById(R.id.tvCantidadIngrediente);
            kcal = v.findViewById(R.id.tvKcalIngrediente);
        }
    }
}
