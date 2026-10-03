package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Favoritos;
import es.pmdm.gymprofit.model.alimento.Racion;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.utils.Anadidos;
import es.pmdm.gymprofit.utils.Categorias;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.GruposBusqueda;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BusquedaAlimentoAdapter — la lista de Añadir, de Buscar y de Favoritos (tableros 4, 5 y
// 8; lotes 1.6.1 y 1.6.3)
//
// Pinta lo que arma GruposBusqueda: cabeceras de sección (con «Ver todos» o cuántos),
// filas de alimento, la propuesta de favorito y, al final de una búsqueda, «¿No lo
// encuentras?». Las filas de un grupo hacen su tarjeta con los fondos bg_grupo_*
// (primera, media, última o sola) y una raya entre ellas.
// Cada fila lleva el icono de su categoría (sin categoría, el de producto), el nombre
// con la insignia azul si los datos están revisados y, debajo, lo que diga quien la usa
// (Acciones.detalle): la última cantidad, lo añadido o las kcal por 100 g. A la derecha,
// el «+» (decisión 1): tocarlo añade sin preguntar y lo vuelve ✓ (momento 15); tocar el
// ✓ lo quita. El resto de la fila abre la ficha.
// La primera lista entra en cascada (momento 16); al volver, ya está.
// Desde la 1.6.4, en «Todo», las comidas recientes con su «Copiar» (que pasa a «Copiada»,
// con su ✓, y ya no se toca), y en «Favoritos», bajo la propuesta y sin ningún favorito,
// el texto de la pestaña vacía (GP-184).
// ============================================================
public class BusquedaAlimentoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Lo que se puede hacer desde la lista. */
    public interface Acciones {
        void abrir(@NonNull Alimento alimento);

        /** ¿Tiene menú al mantenerlo pulsado? */
        boolean tieneOpciones(@NonNull Alimento alimento, @Nullable GruposBusqueda.Seccion seccion);

        void opciones(@NonNull Alimento alimento, @Nullable GruposBusqueda.Seccion seccion, @NonNull View fila);

        void escanear();

        void crear();

        /** El «+» o el ✓ de la fila. */
        void tocarMas(@NonNull Alimento alimento, @NonNull View boton, @NonNull ImageView icono);

        /** ¿Está la fila en ✓? */
        boolean marcado(@NonNull Alimento alimento);

        /** Lo que dice la fila debajo del nombre. */
        @NonNull
        String detalle(@NonNull Alimento alimento, @Nullable GruposBusqueda.Seccion seccion);

        /** Lo que dice TalkBack del «+» (qué, cuánto y a qué comida) o del ✓. */
        @NonNull
        String etiquetaMas(@NonNull Alimento alimento);

        /** «Ver todos» de Recientes. */
        void verTodos();

        void aceptarPropuesta(@NonNull Favoritos.Propuesta propuesta);

        void rechazarPropuesta(@NonNull Favoritos.Propuesta propuesta);

        /** «Copiar» de una comida reciente (1.6.4). */
        void copiar(@NonNull ComidaReciente comida);

        /** ¿Está ya copiada (o copiándose)? Entonces dice «Copiada» y no se toca. */
        boolean copiada(@NonNull ComidaReciente comida);

        /** «a la merienda», la comida de la etiqueta, para TalkBack de «Copiar». */
        @NonNull
        String aLaComida();
    }

    private static final int CABECERA = 0;
    private static final int ALIMENTO = 1;
    private static final int NO_LO_ENCUENTRAS = 2;
    private static final int CABECERA_EXTRA = 3;
    private static final int PROPUESTA = 4;
    private static final int COMIDA_RECIENTE = 5;
    private static final int SIN_FAVORITOS = 6;

    /** Payload: solo cambia el «+» de la fila (y su detalle). */
    private static final Object SOLO_MAS = new Object();

    private final Acciones acciones;
    private final List<GruposBusqueda.Elemento> elementos = new ArrayList<>();
    // Hasta qué posición entra en cascada: solo la primera vez que se pinta la lista.
    private int cascadaHasta = -1;
    // El alimento que acaba de entrar en favoritos, para resaltarlo una vez (momento 19).
    private int resaltarId = -1;

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

    /** La próxima vez que se pinte ese alimento, entra resaltado. */
    public void resaltar(int alimentoId) {
        resaltarId = alimentoId;
    }

    /** Repinta la fila de esa comida reciente («Copiar» o «Copiada»). */
    public void cambioComida(int comidaId) {
        for (int i = 0; i < elementos.size(); i++) {
            ComidaReciente c = elementos.get(i).comida;
            if (c != null && c.getId() == comidaId) notifyItemChanged(i);
        }
    }

    /** Repinta el «+» de las filas de ese alimento (Anadidos.clave, de cualquier comida). */
    public void cambioMas(@NonNull String clave) {
        String alimento = Anadidos.parteAlimento(clave);
        for (int i = 0; i < elementos.size(); i++) {
            Alimento a = elementos.get(i).alimento;
            if (elementos.get(i).tipo == GruposBusqueda.Tipo.ALIMENTO && a != null
                    && (alimento.equals(Anadidos.parteAlimento(Anadidos.clave("", a.getId(), a.getBarcode())))
                    || alimento.equals(Anadidos.parteAlimento(Anadidos.clave("", 0, a.getBarcode()))))) {
                notifyItemChanged(i, SOLO_MAS);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        GruposBusqueda.Elemento e = elementos.get(position);
        switch (e.tipo) {
            case CABECERA:
                return e.verTodos || e.cuantos >= 0 ? CABECERA_EXTRA : CABECERA;
            case NO_LO_ENCUENTRAS: return NO_LO_ENCUENTRAS;
            case PROPUESTA: return PROPUESTA;
            case COMIDA_RECIENTE: return COMIDA_RECIENTE;
            case SIN_FAVORITOS: return SIN_FAVORITOS;
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
        if (viewType == CABECERA_EXTRA) {
            View v = inflater.inflate(R.layout.item_seccion_extra, parent, false);
            v.findViewById(R.id.btnVerTodos).setOnClickListener(b -> acciones.verTodos());
            return new RecyclerView.ViewHolder(v) { };
        }
        if (viewType == NO_LO_ENCUENTRAS) {
            View v = inflater.inflate(R.layout.item_no_lo_encuentras, parent, false);
            v.findViewById(R.id.btnEscanealo).setOnClickListener(b -> acciones.escanear());
            v.findViewById(R.id.btnCrealo).setOnClickListener(b -> acciones.crear());
            return new RecyclerView.ViewHolder(v) { };
        }
        if (viewType == PROPUESTA) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_propuesta_favorito, parent, false)) { };
        }
        if (viewType == COMIDA_RECIENTE) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_comida_reciente, parent, false)) { };
        }
        if (viewType == SIN_FAVORITOS) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_sin_favoritos, parent, false)) { };
        }
        return new FilaHolder(inflater.inflate(R.layout.item_alimento_busqueda, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        GruposBusqueda.Elemento e = elementos.get(position);
        if (payloads.contains(SOLO_MAS) && holder instanceof FilaHolder && e.alimento != null) {
            ((FilaHolder) holder).pintarMas(e.alimento, e.seccion);
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GruposBusqueda.Elemento e = elementos.get(position);
        if (e.tipo == GruposBusqueda.Tipo.CABECERA) {
            pintarCabecera(holder.itemView, e);
        } else if (e.tipo == GruposBusqueda.Tipo.ALIMENTO && e.alimento != null) {
            ((FilaHolder) holder).pintar(e.alimento, e.seccion, fondo(position));
        } else if (e.tipo == GruposBusqueda.Tipo.PROPUESTA && e.propuesta != null) {
            pintarPropuesta(holder.itemView, e.propuesta);
        } else if (e.tipo == GruposBusqueda.Tipo.COMIDA_RECIENTE && e.comida != null) {
            pintarReciente(holder.itemView, e.comida, fondo(position));
        }
        if (position < cascadaHasta) {
            Movimiento.entrarUna(holder.itemView, position * Movimiento.CASCADA_ESCALON, Movimiento.CASCADA, 16);
        } else if (e.alimento != null && e.tipo == GruposBusqueda.Tipo.ALIMENTO && e.alimento.getId() == resaltarId) {
            resaltarId = -1;
            Movimiento.resaltar(holder.itemView, ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.gp_primary_container));
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

    private void pintarCabecera(@NonNull View v, @NonNull GruposBusqueda.Elemento e) {
        TextView titulo = v.findViewById(R.id.tvSeccion);
        titulo.setText(tituloSeccion(e.seccion));
        androidx.core.view.ViewCompat.setAccessibilityHeading(titulo, true);
        View verTodos = v.findViewById(R.id.btnVerTodos);
        if (verTodos != null) verTodos.setVisibility(e.verTodos ? View.VISIBLE : View.GONE);
        TextView cuantos = v.findViewById(R.id.tvCuantosSeccion);
        if (cuantos != null) {
            cuantos.setVisibility(e.cuantos >= 0 ? View.VISIBLE : View.GONE);
            if (e.cuantos >= 0) {
                cuantos.setText(v.getResources().getQuantityString(R.plurals.favoritos_cuantos, e.cuantos, e.cuantos));
            }
        }
    }

    private void pintarPropuesta(@NonNull View v, @NonNull Favoritos.Propuesta p) {
        Context ctx = v.getContext();
        Alimento a = p.getAlimento();
        ((ImageView) v.findViewById(R.id.ivIconoPropuesta)).setImageResource(icono(a));
        ((TextView) v.findViewById(R.id.tvTituloPropuesta)).setText(ctx.getString(R.string.propuesta_titulo, a.getNombre()));
        ((TextView) v.findViewById(R.id.tvVecesPropuesta)).setText(
                ctx.getResources().getQuantityString(R.plurals.propuesta_veces, p.getVeces(), p.getVeces()));
        View si = v.findViewById(R.id.btnAceptarPropuesta);
        si.setContentDescription(ctx.getString(R.string.propuesta_aceptar_a11y, a.getNombre()));
        si.setOnClickListener(b -> acciones.aceptarPropuesta(p));
        v.findViewById(R.id.btnRechazarPropuesta).setOnClickListener(b -> acciones.rechazarPropuesta(p));
    }

    // Una comida reciente: su icono, su nombre, lo que lleva y «Copiar» o «Copiada».
    private void pintarReciente(@NonNull View v, @NonNull ComidaReciente c, int fondo) {
        Context ctx = v.getContext();
        v.setBackgroundResource(fondo);
        v.findViewById(R.id.rayaReciente).setVisibility(fondo == R.drawable.bg_grupo_media
                || fondo == R.drawable.bg_grupo_primera ? View.VISIBLE : View.INVISIBLE);
        ((ImageView) v.findViewById(R.id.ivIconoReciente)).setImageResource(
                es.pmdm.gymprofit.ui.nutricion.ElegirComida.icono(c.getTipoComida()));
        String nombre = es.pmdm.gymprofit.ui.nutricion.CopiarComida.nombre(ctx, c);
        ((TextView) v.findViewById(R.id.tvNombreReciente)).setText(nombre);
        ((TextView) v.findViewById(R.id.tvDetalleReciente)).setText(
                es.pmdm.gymprofit.ui.nutricion.CopiarComida.detalle(ctx, c));
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx));
        v.findViewById(R.id.textosReciente).setContentDescription(ctx.getString(R.string.reciente_a11y, nombre,
                es.pmdm.gymprofit.utils.ComidasRecientes.nombres(c.getLineas()), nf.format(c.getKcal())));

        com.google.android.material.button.MaterialButton boton = v.findViewById(R.id.btnCopiarReciente);
        boolean copiada = acciones.copiada(c);
        int texto = copiada ? ContextCompat.getColor(ctx, R.color.gp_success) : color(ctx,
                com.google.android.material.R.attr.colorOnSurface);
        boton.setText(copiada ? R.string.reciente_copiada : R.string.reciente_copiar);
        boton.setIconResource(copiada ? R.drawable.ic_ms_check : R.drawable.ic_ms_content_copy);
        boton.setIconTint(ColorStateList.valueOf(texto));
        boton.setTextColor(texto);
        boton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx,
                copiada ? R.color.gp_success_container : R.color.gp_surface_2)));
        // Copiada no se vuelve a tocar: lo copiado se quita con el ✓ de cada alimento.
        boton.setEnabled(!copiada);
        boton.setContentDescription(copiada ? ctx.getString(R.string.reciente_copiada_a11y, nombre)
                : ctx.getString(R.string.reciente_copiar_a11y, nombre, acciones.aLaComida()));
        boton.setOnClickListener(copiada ? null : b -> acciones.copiar(c));
    }

    // Primera, media, última o sola dentro de su grupo, según los vecinos del mismo tipo.
    private int fondo(int pos) {
        GruposBusqueda.Tipo tipo = elementos.get(pos).tipo;
        boolean antes = pos > 0 && elementos.get(pos - 1).tipo == tipo;
        boolean despues = pos + 1 < elementos.size() && elementos.get(pos + 1).tipo == tipo;
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
            case FAVORITOS: return R.string.favoritos_seccion;
            case COMIDAS_RECIENTES: return R.string.seccion_comidas_recientes;
            default: return R.string.seccion_basicos;
        }
    }

    /** El icono de la fila: el de su categoría; sin categoría, el de producto (B2). */
    public static int icono(@NonNull Alimento a) {
        String clave = a.getCategoriaClave();
        return clave != null && !clave.trim().isEmpty() ? Categorias.icono(clave) : R.drawable.ic_ms_inventory_2;
    }

    /**
     * El detalle de siempre: «110 kcal por 100 g · 23 g de proteína» o, si es un
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

    /** Pone el «+» o el ✓, sin moverlo: el momento 15 lo anima quien lo toca. */
    public static void pintarMas(@NonNull View boton, @NonNull ImageView icono, boolean marcado) {
        Context ctx = boton.getContext();
        boton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx,
                marcado ? R.color.gp_success_container : R.color.gp_surface_2)));
        icono.setImageResource(marcado ? R.drawable.ic_ms_check : R.drawable.ic_ms_add);
        icono.setImageTintList(marcado ? ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.gp_success))
                : ColorStateList.valueOf(color(ctx, com.google.android.material.R.attr.colorOnSurface)));
    }

    private static int color(Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    final class FilaHolder extends RecyclerView.ViewHolder {
        private final ImageView icono;
        private final TextView nombre;
        private final TextView detalle;
        private final View raya;
        private final View mas;
        private final ImageView iconoMas;

        FilaHolder(@NonNull View v) {
            super(v);
            icono = v.findViewById(R.id.ivIconoFila);
            nombre = v.findViewById(R.id.tvNombreFila);
            detalle = v.findViewById(R.id.tvDetalleFila);
            raya = v.findViewById(R.id.rayaFila);
            mas = v.findViewById(R.id.btnMasFila);
            iconoMas = v.findViewById(R.id.ivMasFila);
        }

        void pintar(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion seccion, int fondo) {
            Context ctx = itemView.getContext();
            icono.setImageResource(icono(a));
            nombre.setText(es.pmdm.gymprofit.ui.nutricion.Insignia.nombre(ctx, a.getNombre(), a.isRevisado(), 18));
            itemView.setBackgroundResource(fondo);
            raya.setVisibility(fondo == R.drawable.bg_grupo_media || fondo == R.drawable.bg_grupo_primera
                    ? View.VISIBLE : View.INVISIBLE);
            itemView.setOnClickListener(v -> acciones.abrir(a));
            if (acciones.tieneOpciones(a, seccion)) {
                itemView.setOnLongClickListener(v -> {
                    acciones.opciones(a, seccion, v);
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
                itemView.setLongClickable(false);
            }
            mas.setOnClickListener(v -> acciones.tocarMas(a, mas, iconoMas));
            pintarMas(a, seccion);
        }

        // El «+» y lo que dice la fila, que cambia con él.
        void pintarMas(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion seccion) {
            Context ctx = itemView.getContext();
            BusquedaAlimentoAdapter.pintarMas(mas, iconoMas, acciones.marcado(a));
            mas.setContentDescription(acciones.etiquetaMas(a));
            String det = acciones.detalle(a, seccion);
            detalle.setText(det);
            itemView.setContentDescription(a.isRevisado()
                    ? ctx.getString(R.string.fila_revisada_a11y, a.getNombre(), det)
                    : ctx.getString(R.string.fila_a11y, a.getNombre(), det));
        }
    }
}
