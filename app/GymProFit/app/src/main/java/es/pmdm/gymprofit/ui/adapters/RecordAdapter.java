package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.Zonas;

// ============================================================
// RecordAdapter — récords vigentes agrupados por zona del cuerpo (GP-088).
//
// Las zonas salen de Zonas, en su orden; lo que no cae en ninguna (cuello, un
// ejercicio sin músculo conocido) va al final en «Otros». Dentro de cada zona se
// respeta el orden en que llegan de la API, del récord más reciente al más antiguo.
// ============================================================
public class RecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Qué hacer al pulsar un récord: abrir la ficha de su ejercicio. */
    public interface OnRecordClick { void onClick(Record record); }

    private static final int TIPO_SECCION = 0;
    private static final int TIPO_RECORD = 1;

    // Cada fila es un rótulo de zona (Integer: id del título) o un Record.
    private final List<Object> filas;
    private final OnRecordClick listener;
    // El récord más reciente (el primero que da la API): trofeo y marca en dorado.
    private final Record masReciente;

    public RecordAdapter(List<Record> records, OnRecordClick listener) {
        this.filas = agrupar(records);
        this.listener = listener;
        this.masReciente = records.isEmpty() ? null : records.get(0);
    }

    /**
     * Intercala los rótulos de zona con sus récords.
     *
     * @return filas en orden: el id del título de cada zona con récords, seguido de
     *         sus récords; al final, «Otros» si hace falta.
     */
    public static List<Object> agrupar(List<Record> records) {
        Map<Zonas.Zona, List<Record>> porZona = new LinkedHashMap<>();
        for (Zonas.Zona z : Zonas.TODAS) porZona.put(z, new ArrayList<>());
        List<Record> otros = new ArrayList<>();
        for (Record r : records) {
            Zonas.Zona z = Zonas.deMusculo(r.getMusculo());
            if (z == null) otros.add(r);
            else porZona.get(z).add(r);
        }

        List<Object> filas = new ArrayList<>();
        for (Map.Entry<Zonas.Zona, List<Record>> e : porZona.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            filas.add(e.getKey().titulo);
            filas.addAll(e.getValue());
        }
        if (!otros.isEmpty()) {
            filas.add(R.string.zona_titulo_otros);
            filas.addAll(otros);
        }
        return filas;
    }

    @Override
    public int getItemViewType(int position) {
        return filas.get(position) instanceof Record ? TIPO_RECORD : TIPO_SECCION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_SECCION) {
            return new SeccionHolder(inf.inflate(R.layout.item_record_seccion, parent, false));
        }
        return new RecordHolder(inf.inflate(R.layout.item_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object fila = filas.get(position);
        if (holder instanceof SeccionHolder) {
            TextView tv = ((SeccionHolder) holder).tvSeccion;
            tv.setText((Integer) fila);
            ViewCompat.setAccessibilityHeading(tv, true);
            return;
        }

        Record r = (Record) fila;
        RecordHolder h = (RecordHolder) holder;
        Context ctx = h.itemView.getContext();
        Locale idioma = FechaUtils.localeDeLaApp(ctx);

        String nombre = r.nombre(idioma);
        String marca = Marcas.texto(ctx, r);
        h.tvNombre.setText(nombre);
        h.tvMarca.setText(marca);

        // «hace 2 días · antes 82,5 kg × 10»; lo que falte, no se escribe (GP-105).
        String cuando = TiempoRelativo.texto(ctx, r.getFecha());
        String anterior = Marcas.anteriorSolo(ctx, r);
        String detalle = cuando == null ? (anterior == null ? "" : ctx.getString(R.string.record_antes, anterior))
                : anterior == null ? cuando : ctx.getString(R.string.record_hace_antes, cuando, anterior);
        h.tvDetalle.setText(detalle);
        h.tvDetalle.setVisibility(detalle.isEmpty() ? View.GONE : View.VISIBLE);

        // Dorado solo el último récord: lo conseguido, no todo (DEC-018).
        boolean dorado = r == masReciente;
        h.ivTrofeo.setImageResource(dorado ? R.drawable.ic_ms_trophy_fill : R.drawable.ic_ms_trophy);
        h.ivTrofeo.setImageTintList(android.content.res.ColorStateList.valueOf(dorado
                ? androidx.core.content.ContextCompat.getColor(ctx, R.color.gp_gold)
                : color(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant)));
        h.tvMarca.setTextColor(dorado ? androidx.core.content.ContextCompat.getColor(ctx, R.color.gp_gold_text)
                : color(ctx, com.google.android.material.R.attr.colorOnSurface));
        // Por tiempo (GP-125), el cronómetro delante de «1:05».
        Marcas.iconoTiempo(h.tvMarca, r);

        // Las filas de una zona forman una tarjeta: primera, intermedia o última.
        boolean primera = !(position > 0 && filas.get(position - 1) instanceof Record);
        boolean ultima = !(position + 1 < filas.size() && filas.get(position + 1) instanceof Record);
        h.itemView.setBackgroundResource(primera && ultima ? R.drawable.bg_grupo_solo
                : primera ? R.drawable.bg_grupo_primera : ultima ? R.drawable.bg_grupo_ultima : R.drawable.bg_grupo_media);
        h.divider.setVisibility(ultima ? View.GONE : View.VISIBLE);

        h.itemView.setContentDescription(ctx.getString(R.string.record_a11y, nombre, marca, detalle));
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(r); });
    }

    @Override
    public int getItemCount() { return filas.size(); }

    static class SeccionHolder extends RecyclerView.ViewHolder {
        final TextView tvSeccion;
        SeccionHolder(View v) { super(v); tvSeccion = v.findViewById(R.id.tvSeccionRecord); }
    }

    private static int color(Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    static class RecordHolder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvMarca, tvDetalle;
        final android.widget.ImageView ivTrofeo;
        final View divider;
        RecordHolder(View v) {
            super(v);
            ivTrofeo = v.findViewById(R.id.ivTrofeoRecord);
            divider = v.findViewById(R.id.divRecord);
            tvNombre = v.findViewById(R.id.tvNombreRecord);
            tvMarca = v.findViewById(R.id.tvMarcaRecord);
            tvDetalle = v.findViewById(R.id.tvDetalleRecord);
        }
    }
}
