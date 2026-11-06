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
import es.pmdm.gymprofit.ui.tarjetas.TextosTarjeta;
import es.pmdm.gymprofit.ui.widget.BarrasVecesView;
import es.pmdm.gymprofit.utils.FrasesTarjeta;
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
//   · Historial: las dos últimas sesiones terminadas en filas compactas, con cuándo, kilos,
//     récords y las barras de sus últimas veces, de la primera página de /sesiones/historial
//     (GP-232): ya no se piden los kilos de cada fila.
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
    // Kilos de cada sesión ya pedidos.
    // Las filas del Historial entran una sola vez (GP-232).
    private final es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta entrada = new es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta();

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
            // Dorado solo el más reciente, como en la lista de Récords: el mismo récord no puede ir
            // celebrado en una y apagado en la otra (revisor, GP-225; DEC-018).
            boolean dorado = i == 0;
            View fila = dorado ? fila(lista, R.drawable.ic_ms_trophy_fill, R.drawable.bg_fila_icono_oro, R.color.gp_gold)
                    : fila(lista, R.drawable.ic_ms_trophy, R.drawable.bg_fila_icono, R.color.gp_text_secondary);
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
            if (!dorado) valor.setTextColor(ContextCompat.getColor(ctx, R.color.gp_on_surface));
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

    void pintarHistorial(@NonNull Fuente<es.pmdm.gymprofit.model.sesion.PaginaHistorial> fuente,
                         @NonNull Runnable reintentar) {
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
        List<es.pmdm.gymprofit.model.sesion.PaginaHistorial.Sesion> todas =
                fuente.dato() == null ? new java.util.ArrayList<>() : fuente.dato().sesiones();
        if (todas.isEmpty()) {
            card.setVisibility(View.GONE);
            enlace.setVisibility(View.GONE);
            estadoHistorial.vacio(R.string.progreso_historial_vacio);
            return;
        }
        estadoHistorial.oculto();
        card.setVisibility(View.VISIBLE);
        enlace.setVisibility(View.VISIBLE);
        rellenarHistorial(todas.subList(0, Math.min(SESIONES, todas.size())));
    }

    // Filas compactas (GP-232): la rutina, «Ayer, 13.640 kg y 1 récord» y las barras pequeñas.
    private void rellenarHistorial(List<es.pmdm.gymprofit.model.sesion.PaginaHistorial.Sesion> ultimas) {
        if (!fragment.isAdded()) return;
        Context ctx = raiz.getContext();
        LinearLayout lista = raiz.findViewById(R.id.listaHistorial);
        lista.removeAllViews();
        TextosTarjeta textos = new TextosTarjeta(ctx);
        for (int i = 0; i < ultimas.size(); i++) {
            es.pmdm.gymprofit.model.sesion.PaginaHistorial.Sesion s = ultimas.get(i);
            if (i > 0) separadorSesion(lista);
            View fila = LayoutInflater.from(ctx).inflate(R.layout.item_sesion_fila, lista, false);
            lista.addView(fila);
            String rutina = textos.titulo(s);
            String cuando = textos.cuandoRelativo(s.getFechaInicio());
            String kilos = s.getEjercicios() == 0 ? ctx.getString(R.string.tarjeta_sin_ejercicios)
                    : ctx.getString(R.string.inicio_kilos_n, textos.numero(s.getKilos()));
            String antes = cuando.isEmpty() ? kilos : ctx.getString(R.string.tarjeta_cuando, cuando, kilos);
            FrasesTarjeta.Linea l = FrasesTarjeta.conRecords(antes, ctx.getString(R.string.enumeracion_y),
                    s.getRecords(), textos.frases());
            ((TextView) fila.findViewById(R.id.tvTituloFila)).setText(rutina);
            textoOculto(fila.findViewById(R.id.tvApoyoFila), null);
            TextView apoyo = fila.findViewById(R.id.tvApoyoFila);
            apoyo.setText(textos.conOro(l));
            apoyo.setVisibility(View.VISIBLE);
            BarrasVecesView barras = fila.findViewById(R.id.barrasFila);
            barras.setVeces(s.esLibre() ? new java.util.ArrayList<>() : s.getBarras());
            entrada.pintar("s:" + s.getId(), null, 0, R.dimen.text_tarjeta_kg, textos, barras);

            StringBuilder a11y = new StringBuilder(rutina);
            String cuandoA11y = textos.cuandoA11y(s.getFechaInicio());
            if (!cuandoA11y.isEmpty()) a11y.append(", ").append(cuandoA11y);
            a11y.append(". ").append(s.getEjercicios() == 0 ? kilos : textos.kilosA11y(s.getKilos()));
            if (s.getRecords() > 0) a11y.append(ctx.getString(R.string.enumeracion_y))
                    .append(FrasesTarjeta.records(s.getRecords(), textos.frases()));
            a11y.append('.');
            String b = s.esLibre() ? null : textos.barrasA11y(s.getBarras());
            if (b != null) a11y.append(' ').append(b).append('.');
            a11y.append(' ').append(ctx.getString(R.string.tarjeta_abre_resumen_a11y)).append('.');
            fila.setContentDescription(a11y);
            fila.setOnClickListener(v -> {
                Intent intent = new Intent(ctx, ResumenSesionActivity.class);
                intent.putExtra("sesionId", s.getId());
                Transiciones.abrirTarjeta(fragment.requireActivity(), v, intent);
            });
        }
    }

    // Entre filas de sesión, la raya desde el texto (16 dp): estas filas no llevan icono.
    private static void separadorSesion(LinearLayout lista) {
        View sep = new View(lista.getContext(), null, 0, R.style.Widget_GymProFit_Separador);
        ViewGroup.MarginLayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                lista.getResources().getDimensionPixelSize(R.dimen.separador_alto));
        lp.setMarginStart(lista.getResources().getDimensionPixelSize(R.dimen.fila_padding_horizontal));
        sep.setLayoutParams(lp);
        sep.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        lista.addView(sep);
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
