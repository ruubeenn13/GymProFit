package es.pmdm.gymprofit.ui.progreso;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.ui.fragments.ProgresoFragment;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.FilaRecord;
import es.pmdm.gymprofit.utils.HistorialSesiones;
import es.pmdm.gymprofit.utils.LogrosProgreso;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.Transiciones;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VacioRecords;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ListasProgreso — «Récords recientes», «Logros» e «Historial» de Progreso (GP-216, lote 4,
// tablero 16): un trozo de cada lista y su «Ver todo», que abre la lista completa en su pantalla.
//
//   · Récords: los tres más recientes con la marca anterior, que RecordDTO ya trae; cada fila
//     abre la ficha del ejercicio, como la tarjeta del récord de Inicio. «Ver los N».
//   · Logros: los dos últimos conseguidos y los dos más cerca de conseguirse (LogrosProgreso).
//   · Historial: las dos últimas sesiones terminadas, con cuándo, minutos y kilos; los kilos se
//     piden por sesión (la lista no los trae) y se guardan: una sesión terminada no cambia.
//
// Cada una tiene su estado: cargando, error con «Reintentar» y vacío de cuenta nueva.
// ============================================================
final class ListasProgreso {

    private static final int RECORDS = 3;
    private static final int SESIONES = 2;

    private final Fragment fragment;
    private final View raiz;
    private final IntConsumer abrirSeccion;
    private final VistaEstado estadoRecords, estadoLogros, estadoHistorial;
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    // Kilos de cada sesión ya pedidos.
    private final Map<Integer, Double> kilosSesion = new HashMap<>();
    @Nullable private List<SesionEntrenamiento> ultimas;

    ListasProgreso(@NonNull Fragment fragment, @NonNull View raiz, @NonNull IntConsumer abrirSeccion) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.abrirSeccion = abrirSeccion;
        estadoRecords = new VistaEstado(raiz.findViewById(R.id.estadoRecords));
        estadoLogros = new VistaEstado(raiz.findViewById(R.id.estadoLogros));
        estadoHistorial = new VistaEstado(raiz.findViewById(R.id.estadoHistorial));
        raiz.findViewById(R.id.enlaceRecords).setOnClickListener(v -> abrirSeccion.accept(ProgresoFragment.RECORDS));
        raiz.findViewById(R.id.enlaceLogros).setOnClickListener(v -> abrirSeccion.accept(ProgresoFragment.LOGROS));
        raiz.findViewById(R.id.enlaceHistorial).setOnClickListener(v -> abrirSeccion.accept(ProgresoFragment.HISTORIAL));
    }

    // ── Récords ──────────────────────────────────────────────────────────────

    void pintarRecords(@NonNull Fuente<Records> fuente, @NonNull Fuente<List<SesionEntrenamiento>> sesiones,
                       @NonNull Runnable reintentar) {
        Context ctx = raiz.getContext();
        View card = raiz.findViewById(R.id.cardRecords);
        TextView enlace = raiz.findViewById(R.id.enlaceRecords);
        if (!fuente.lista()) {
            if (card.getVisibility() != View.VISIBLE) estadoRecords.cargando();
            return;
        }
        if (fuente.fallo()) {
            card.setVisibility(View.GONE);
            enlace.setVisibility(View.GONE);
            estadoRecords.error(VistaEstado.mensaje(ctx, R.string.progreso_records_error, fuente.code(), fuente.mensaje()),
                    reintentar);
            return;
        }
        List<Record> todos = fuente.dato() == null ? java.util.Collections.emptyList() : fuente.dato().getRecords();
        if (todos.isEmpty()) {
            card.setVisibility(View.GONE);
            enlace.setVisibility(View.GONE);
            // Sin entrenos y con entrenos sin récords no es lo mismo (GP-229).
            VacioRecords.Que que = VacioRecords.de(sesiones.lista(), sesiones.fallo(),
                    sesiones.dato() == null ? 0 : sesiones.dato().size());
            if (que == VacioRecords.Que.CARGANDO) estadoRecords.cargando();
            else estadoRecords.vacio(VacioRecords.texto(que));
            return;
        }
        estadoRecords.oculto();
        card.setVisibility(View.VISIBLE);
        enlace.setVisibility(View.VISIBLE);
        enlace.setText(ctx.getResources().getQuantityString(R.plurals.progreso_ver_records, todos.size(), todos.size()));

        LinearLayout lista = raiz.findViewById(R.id.listaRecords);
        lista.removeAllViews();
        Locale idioma = FechaUtils.localeDeLaApp(ctx);
        // La API los manda del más reciente al más antiguo.
        for (int i = 0; i < Math.min(RECORDS, todos.size()); i++) {
            Record r = todos.get(i);
            if (i > 0) separador(lista);
            View fila = fila(lista, R.drawable.ic_ms_trophy_fill, R.drawable.bg_fila_icono_oro, R.color.gp_gold);
            String nombre = r.nombre(idioma);
            String anterior = Marcas.anteriorSolo(ctx, r);
            String apoyo = FilaRecord.apoyo(TiempoRelativo.texto(ctx, r.getFecha()),
                    anterior == null ? null : anterior.replace(' ', ' '),
                    ctx.getString(R.string.record_hace_antes), idioma);
            // La marca no se parte: con letra grande dejaba el «10» solo en otra línea.
            String marca = Marcas.texto(ctx, r).replace(' ', ' ');
            ((TextView) fila.findViewById(R.id.tvTituloFila)).setText(nombre);
            textoOculto(fila.findViewById(R.id.tvApoyoFila), apoyo);
            TextView valor = fila.findViewById(R.id.tvValorFila);
            valor.setText(marca);
            Marcas.iconoTiempo(valor, r);
            valor.setVisibility(View.VISIBLE);
            // Un récord por tiempo dice que es un tiempo: el cronómetro solo se ve (igual que la lista).
            fila.setContentDescription(FilaRecord.dicho(nombre, apoyo, marca, r.esDeTiempo(),
                    ctx.getString(R.string.progreso_marca_tiempo_a11y)));
            fila.setOnClickListener(v -> abrirEjercicio(r, v));
        }
    }

    // La ficha espera el ejercicio entero en extras: se pide al pulsar, como en Inicio.
    private void abrirEjercicio(Record r, View fila) {
        ejercicioApi.getPorId(r.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
            @Override
            public void onOk(Ejercicio ejercicio) {
                if (!fragment.isAdded() || ejercicio == null) return;
                EjercicioNavHelper.abrir(fragment.requireActivity(), fila, ejercicio);
            }

            @Override
            public void onFail(int code, String message) {
                if (fragment.isAdded()) UiFeedback.toastError(fragment.requireActivity(), code, message);
            }
        });
    }

    // ── Logros ───────────────────────────────────────────────────────────────

    void pintarLogros(@NonNull Fuente<List<LogroProgreso>> fuente, @NonNull Runnable reintentar) {
        Context ctx = raiz.getContext();
        View scroll = raiz.findViewById(R.id.scrollLogros);
        if (!fuente.lista()) {
            if (scroll.getVisibility() != View.VISIBLE) estadoLogros.cargando();
            return;
        }
        if (fuente.fallo()) {
            scroll.setVisibility(View.GONE);
            estadoLogros.error(VistaEstado.mensaje(ctx, R.string.progreso_logros_error, fuente.code(), fuente.mensaje()),
                    reintentar);
            return;
        }
        List<LogroProgreso> elegidos = LogrosProgreso.elegir(fuente.dato());
        if (elegidos.isEmpty()) {
            scroll.setVisibility(View.GONE);
            estadoLogros.vacio(R.string.progreso_logros_vacio);
            return;
        }
        estadoLogros.oculto();
        scroll.setVisibility(View.VISIBLE);
        LinearLayout lista = raiz.findViewById(R.id.listaLogros);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (LogroProgreso l : elegidos) {
            View item = inflater.inflate(R.layout.item_logro_carrusel, lista, false);
            boolean si = l.isConseguido();
            item.findViewById(R.id.medallonLogro).setBackgroundResource(
                    si ? R.drawable.bg_medallon_conseguido : R.drawable.bg_medallon_pendiente);
            ImageView icono = item.findViewById(R.id.ivLogro);
            icono.setImageResource(LogroAdapter.glifo(l.getTipo(), si));
            icono.setImageTintList(ColorStateList.valueOf(si ? ContextCompat.getColor(ctx, R.color.gp_gold)
                    : MaterialColors.getColor(icono, com.google.android.material.R.attr.colorOnSurfaceVariant)));
            ((TextView) item.findViewById(R.id.tvNombreLogro)).setText(l.getNombre());
            String estado = si ? ctx.getString(R.string.logro_conseguido) : LogroAdapter.textoProgreso(ctx, l);
            TextView tvEstado = item.findViewById(R.id.tvEstadoLogro);
            tvEstado.setText(estado);
            tvEstado.setTextColor(MaterialColors.getColor(tvEstado, com.google.android.material.R.attr.colorOnSurfaceVariant));
            item.setContentDescription(ctx.getString(R.string.logro_a11y, l.getNombre(), l.getDescripcion(),
                    si ? estado : ctx.getString(R.string.logro_a11y_bloqueado_progreso, estado)));
            item.setOnClickListener(v -> abrirSeccion.accept(ProgresoFragment.LOGROS));
            lista.addView(item);
        }
    }

    // ── Historial ────────────────────────────────────────────────────────────

    void pintarHistorial(@NonNull Fuente<List<SesionEntrenamiento>> fuente, @NonNull Runnable reintentar) {
        Context ctx = raiz.getContext();
        View card = raiz.findViewById(R.id.cardHistorial);
        View enlace = raiz.findViewById(R.id.enlaceHistorial);
        if (!fuente.lista()) {
            if (card.getVisibility() != View.VISIBLE) estadoHistorial.cargando();
            return;
        }
        if (fuente.fallo()) {
            card.setVisibility(View.GONE);
            estadoHistorial.error(VistaEstado.mensaje(ctx, R.string.inicio_entrenos_error, fuente.code(), fuente.mensaje()),
                    reintentar);
            return;
        }
        List<SesionEntrenamiento> terminadas = HistorialSesiones.terminadas(fuente.dato());
        if (terminadas.isEmpty()) {
            card.setVisibility(View.GONE);
            enlace.setVisibility(View.GONE);
            estadoHistorial.vacio(R.string.progreso_historial_vacio);
            return;
        }
        estadoHistorial.oculto();
        card.setVisibility(View.VISIBLE);
        enlace.setVisibility(View.VISIBLE);
        ultimas = terminadas.subList(0, Math.min(SESIONES, terminadas.size()));
        rellenarHistorial();
        for (SesionEntrenamiento s : ultimas) {
            if (!kilosSesion.containsKey(s.getId())) pedirKilos(s.getId());
        }
    }

    private void rellenarHistorial() {
        if (ultimas == null || !fragment.isAdded()) return;
        Context ctx = raiz.getContext();
        LinearLayout lista = raiz.findViewById(R.id.listaHistorial);
        lista.removeAllViews();
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx));
        for (int i = 0; i < ultimas.size(); i++) {
            SesionEntrenamiento s = ultimas.get(i);
            if (i > 0) separador(lista);
            View fila = fila(lista, R.drawable.ic_ms_check, R.drawable.bg_fila_icono_hecho, R.color.gp_success);
            String rutina = s.getRutinaNombre() != null ? s.getRutinaNombre() : ctx.getString(R.string.hecho_hoy_libre);
            String apoyo = ctx.getString(R.string.duracion_min, s.getDuracionMinutos());
            String cuando = FechaUtils.diaRelativo(ctx, s.getFechaInicio());
            if (!cuando.isEmpty()) apoyo = ctx.getString(R.string.hoy_toca_separador, cuando, apoyo);
            Double kilos = kilosSesion.get(s.getId());
            if (kilos != null && kilos > 0) {
                apoyo = ctx.getString(R.string.hoy_toca_separador, apoyo,
                        ctx.getString(R.string.inicio_kilos_n, nf.format(Math.round(kilos))));
            }
            ((TextView) fila.findViewById(R.id.tvTituloFila)).setText(rutina);
            textoOculto(fila.findViewById(R.id.tvApoyoFila), apoyo);
            fila.findViewById(R.id.ivFlechaFila).setVisibility(View.VISIBLE);
            fila.setContentDescription(rutina + ". " + apoyo);
            fila.setOnClickListener(v -> {
                Intent intent = new Intent(ctx, ResumenSesionActivity.class);
                intent.putExtra("sesionId", s.getId());
                Transiciones.abrirTarjeta(fragment.requireActivity(), v, intent);
            });
        }
    }

    private void pedirKilos(int sesionId) {
        sesionApi.getVolumenSesion(sesionId).enqueue(new ApiCallback<Map<String, Double>>() {
            @Override
            public void onOk(Map<String, Double> r) {
                if (r == null || !fragment.isAdded()) return;
                Double v = r.get("volumenKg");
                kilosSesion.put(sesionId, v == null ? 0d : v);
                rellenarHistorial();
            }

            @Override
            public void onFail(int code, String message) {
                // Sin los kilos la fila se queda con cuándo y los minutos, que dicen lo esencial; el
                // resumen de la sesión, al abrirla, sí enseña su fallo. Queda rastro en el log.
                Log.w("GymProFit", "kilos de la sesión " + sesionId + " (" + code + "): " + message);
            }
        });
    }

    // ── Piezas ───────────────────────────────────────────────────────────────

    private View fila(LinearLayout lista, @DrawableRes int icono, @DrawableRes int fondo, int color) {
        View fila = LayoutInflater.from(lista.getContext()).inflate(R.layout.item_fila_progreso, lista, false);
        ImageView iv = fila.findViewById(R.id.ivIconoFila);
        iv.setImageResource(icono);
        iv.setBackgroundResource(fondo);
        iv.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(lista.getContext(), color)));
        lista.addView(fila);
        return fila;
    }

    private static void separador(LinearLayout lista) {
        View sep = new View(lista.getContext(), null, 0, R.style.Widget_GymProFit_Separador);
        ViewGroup.MarginLayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                lista.getResources().getDimensionPixelSize(R.dimen.separador_alto));
        lp.setMarginStart(lista.getResources().getDimensionPixelSize(R.dimen.fila_sangria_separador));
        sep.setLayoutParams(lp);
        sep.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        lista.addView(sep);
    }

    private static void textoOculto(TextView tv, @Nullable String texto) {
        tv.setText(texto);
        tv.setVisibility(texto == null || texto.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
