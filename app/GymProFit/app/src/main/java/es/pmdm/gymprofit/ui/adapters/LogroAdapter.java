package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// LogroAdapter — lista de logros con su estado y su progreso (GP-079).
//
// Cada fila lleva un medallón de 48 dp con un Material Symbol:
//   · bloqueado  → medallón gp_surface_2, glifo en CONTORNO con gp_text_secondary,
//                  y debajo la barra y «3 de 7 sesiones» si el logro tiene pasos;
//   · conseguido → medallón gp_gold_container, glifo RELLENO con gp_gold, y
//                  debajo «Conseguido · 23 sept 2026» en gp_gold_text.
// Lo conseguido es ORO y lo que está en curso es MARCA (DEC-018): por eso la
// barra de progreso sigue en naranja. La primera versión de GP-079 puso el
// conseguido en naranja y chocaba con esa decisión.
// El glifo pasa 3:1 contra su medallón en los dos temas (el mínimo es 3,10:1,
// conseguido en claro). Nada se atenúa con alpha: antes toda la tarjeta
// bloqueada iba a setAlpha(0.5f) y el texto bajaba a unos 2,2:1.
//
// Los datos vienen de GET /logros/progreso, que ya cruza catálogo y obtenidos.
// ============================================================
public class LogroAdapter extends RecyclerView.Adapter<LogroAdapter.ViewHolder> {

    /** Par de glifos de un tipo: contorno para bloqueado, relleno para conseguido. */
    private static final class Glifos {
        @DrawableRes final int contorno;
        @DrawableRes final int relleno;
        Glifos(@DrawableRes int contorno, @DrawableRes int relleno) {
            this.contorno = contorno;
            this.relleno = relleno;
        }
    }

    // Un par de glifos por tipo. Las claves son el enum TipoLogro de la API tal y
    // como llega serializado. Un tipo que la app no conozca —el catálogo puede
    // crecer en el servidor antes que la app— cae en el trofeo genérico.
    private static final Map<String, Glifos> GLIFOS_POR_TIPO = new HashMap<>();
    private static final Glifos GLIFOS_DESCONOCIDO = new Glifos(R.drawable.ic_ms_trophy, R.drawable.ic_ms_trophy_fill);
    static {
        GLIFOS_POR_TIPO.put("PRIMERA_SESION",    new Glifos(R.drawable.ic_ms_flag, R.drawable.ic_ms_flag_fill));
        GLIFOS_POR_TIPO.put("CONSTANCIA",        new Glifos(R.drawable.ic_ms_event_available, R.drawable.ic_ms_event_available_fill));
        GLIFOS_POR_TIPO.put("DEDICADO",          new Glifos(R.drawable.ic_ms_local_fire_department, R.drawable.ic_ms_local_fire_department_fill));
        GLIFOS_POR_TIPO.put("CENTENARIO",        new Glifos(R.drawable.ic_ms_workspace_premium, R.drawable.ic_ms_workspace_premium_fill));
        GLIFOS_POR_TIPO.put("OBJETIVO_CUMPLIDO", new Glifos(R.drawable.ic_ms_target, R.drawable.ic_ms_target_fill));
        GLIFOS_POR_TIPO.put("MAQUINA",           new Glifos(R.drawable.ic_ms_crown, R.drawable.ic_ms_crown_fill));
    }

    private final List<LogroProgreso> items;

    /**
     * @param items logros a mostrar, en el orden en que se pintan
     */
    public LogroAdapter(List<LogroProgreso> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_logro, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Context ctx = h.itemView.getContext();
        LogroProgreso logro = items.get(position);
        Glifos glifos = glifosDe(logro.getTipo());

        h.tvNombre.setText(logro.getNombre());
        h.tvDesc.setText(logro.getDescripcion());

        String estado;
        if (logro.isConseguido()) {
            h.medallon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.gp_gold_container)));
            h.ivIcono.setImageResource(glifos.relleno);
            h.ivIcono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.gp_gold)));

            String fecha = FechaUtils.formatearFechaMedia(logro.getFechaObtenido(), FechaUtils.localeDeLaApp(ctx));
            h.tvConseguido.setText(fecha != null
                    ? ctx.getString(R.string.logro_conseguido_fecha, fecha)
                    : ctx.getString(R.string.logro_conseguido));
            h.tvConseguido.setVisibility(View.VISIBLE);
            h.layoutProgreso.setVisibility(View.GONE);
            estado = fecha != null
                    ? ctx.getString(R.string.logro_a11y_conseguido, fecha)
                    : ctx.getString(R.string.logro_conseguido);
        } else {
            h.medallon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.gp_surface_2)));
            h.ivIcono.setImageResource(glifos.contorno);
            h.ivIcono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.gp_text_secondary)));
            h.tvConseguido.setVisibility(View.GONE);

            if (logro.tienePasos()) {
                String progreso = textoProgreso(ctx, logro);
                h.pb.setMax(logro.getUmbral());
                h.pb.setProgressCompat(logro.getProgreso(), false);
                h.tvProgreso.setText(progreso);
                h.layoutProgreso.setVisibility(View.VISIBLE);
                estado = ctx.getString(R.string.logro_a11y_bloqueado_progreso, progreso);
            } else {
                // Umbral 1: «0 de 1» no aporta nada; la descripción ya dice qué hacer.
                h.layoutProgreso.setVisibility(View.GONE);
                estado = ctx.getString(R.string.logro_bloqueado);
            }
        }

        h.itemView.setContentDescription(ctx.getString(R.string.logro_a11y,
                logro.getNombre(), logro.getDescripcion(), estado));
    }

    /**
     * «3 de 7 sesiones». El plural concuerda con el total, que es el que lleva el
     * sustantivo: «1 de 7 sesiones», no «1 de 7 sesión».
     */
    static String textoProgreso(Context ctx, LogroProgreso logro) {
        return ctx.getResources().getQuantityString(pluralDe(logro.getMetrica()),
                logro.getUmbral(), logro.getProgreso(), logro.getUmbral());
    }

    // Palabra de la métrica; una métrica nueva que la app no conozca se cuenta como «pasos».
    @PluralsRes
    private static int pluralDe(String metrica) {
        if ("SESIONES_COMPLETADAS".equals(metrica)) return R.plurals.logro_progreso_sesiones;
        if ("EJERCICIOS_REALIZADOS".equals(metrica)) return R.plurals.logro_progreso_ejercicios;
        if ("OBJETIVOS_COMPLETADOS".equals(metrica)) return R.plurals.logro_progreso_objetivos;
        return R.plurals.logro_progreso_pasos;
    }

    // Glifos del tipo, o el trofeo si el tipo es desconocido o no viene.
    private static Glifos glifosDe(String tipo) {
        Glifos g = tipo != null ? GLIFOS_POR_TIPO.get(tipo) : null;
        return g != null ? g : GLIFOS_DESCONOCIDO;
    }

    // Idioma de la interfaz (el elegido en la app, no necesariamente el del sistema).

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View medallon, layoutProgreso;
        final ImageView ivIcono;
        final TextView tvNombre, tvDesc, tvProgreso, tvConseguido;
        final LinearProgressIndicator pb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            medallon       = itemView.findViewById(R.id.medallonLogro);
            ivIcono        = itemView.findViewById(R.id.ivIconoLogro);
            tvNombre       = itemView.findViewById(R.id.tvNombreLogro);
            tvDesc         = itemView.findViewById(R.id.tvDescLogro);
            layoutProgreso = itemView.findViewById(R.id.layoutProgresoLogro);
            pb             = itemView.findViewById(R.id.pbLogro);
            tvProgreso     = itemView.findViewById(R.id.tvProgresoLogro);
            tvConseguido   = itemView.findViewById(R.id.tvConseguidoLogro);
        }
    }
}
