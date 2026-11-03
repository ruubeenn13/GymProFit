package es.pmdm.gymprofit.ui.progreso;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.sesion.VolumenSemanal;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.widget.BarrasKilosView;
import es.pmdm.gymprofit.ui.widget.SelectorPildora;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.HistorialSesiones;
import es.pmdm.gymprofit.utils.KilosMovidos;
import es.pmdm.gymprofit.utils.ProgresoSemanal;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// KilosProgreso — «Kilos movidos» de Progreso (GP-216, lote 4, tablero 16): el selector
// 4 sem. · 8 sem. · Todo, la cifra de esta semana con su avance frente a la pasada y las barras
// de «Tu progreso» de Inicio (BarrasKilosView), una por semana.
//
// Los kilos salen de GET /sesiones/usuario/{id}/volumen-semanal. «Todo» empieza en la semana de
// la primera sesión, que se lee de la lista de sesiones que Progreso ya ha pedido (no se pide otra
// vez), hasta el tope de 104 semanas. Cambiar de período pide solo los kilos. Una cuenta sin
// ninguna sesión terminada ve el vacío y no el selector con barras a cero.
// ============================================================
final class KilosProgreso {

    /** Hasta este número de semanas TalkBack lee la cifra de cada una; con más, un resumen. */
    private static final int SEMANAS_LEIDAS = 8;

    private final Fragment fragment;
    private final View raiz;
    private final int usuarioId;
    private final VistaEstado estado;
    private final SelectorPildora selector;
    private final Runnable pedirSesiones;
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final Fuente<List<VolumenSemanal>> kilos = new Fuente<>();

    private int periodo = KilosMovidos.PERIODO_INICIAL;
    // El período y las semanas de lo que se ha pedido, para saber si lo que hay sirve.
    private int periodoPedido = -1;
    private int semanasPedidas;
    @Nullable private Fuente<List<SesionEntrenamiento>> sesiones;

    KilosProgreso(@NonNull Fragment fragment, @NonNull View raiz, int usuarioId, int periodoInicial,
                  @NonNull Runnable pedirSesiones) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.usuarioId = usuarioId;
        this.pedirSesiones = pedirSesiones;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoKilos));
        this.selector = raiz.findViewById(R.id.selectorKilos);
        this.periodo = periodoInicial;
        selector.setElegida(periodo, false);
        selector.setAlElegir(i -> {
            periodo = i;
            pedir();
        });
    }

    /** El período elegido (índice de {@link KilosMovidos#PERIODOS}), para guardarlo al recrearse. */
    int periodo() { return periodo; }

    /** Pide los kilos del período elegido (al abrir Progreso, al cambiar de período, al reintentar). */
    void pedir() {
        periodoPedido = -1;
        pintar();
    }

    /** Han cambiado las sesiones: «Todo» puede saber ya desde cuándo, y el vacío, si lo es. */
    void setSesiones(@NonNull Fuente<List<SesionEntrenamiento>> sesiones) {
        this.sesiones = sesiones;
        pintar();
    }

    private void pintar() {
        if (!fragment.isAdded()) return;
        boolean todo = KilosMovidos.PERIODOS[periodo] == 0;
        // «Todo» necesita la primera sesión: sin la lista, espera o dice el fallo de la lista.
        if (todo && (sesiones == null || !sesiones.lista())) {
            mostrarEstado();
            estado.cargando();
            return;
        }
        if (todo && sesiones.fallo()) {
            mostrarEstado();
            estado.error(VistaEstado.mensaje(fragment.requireContext(), R.string.progreso_kilos_error,
                    sesiones.code(), sesiones.mensaje()), pedirSesiones);
            return;
        }
        if (cuentaNueva()) {
            pintarVacioCuentaNueva();
            return;
        }
        selector.setVisibility(View.VISIBLE);

        String hoy = TiempoRelativo.hoy();
        int semanas = KilosMovidos.semanas(KilosMovidos.PERIODOS[periodo],
                todo ? HistorialSesiones.primeraFecha(sesiones.dato()) : null, hoy);
        if (periodoPedido != periodo || semanasPedidas != semanas) {
            periodoPedido = periodo;
            semanasPedidas = semanas;
            if (raiz.findViewById(R.id.bloqueKilosProgreso).getVisibility() != View.VISIBLE) estado.cargando();
            kilos.pedir(sesionApi.getVolumenSemanal(usuarioId, KilosMovidos.desdeIso(hoy, semanas), semanas),
                    fragment, this::pintarKilos);
            return;
        }
        pintarKilos();
    }

    // Una cuenta sin entrenos: el vacío, sin selector ni barras a cero.
    private boolean cuentaNueva() {
        return sesiones != null && sesiones.ok() && HistorialSesiones.terminadas(sesiones.dato()).isEmpty();
    }

    private void pintarVacioCuentaNueva() {
        selector.setVisibility(View.GONE);
        mostrarEstado();
        estado.vacio(R.string.progreso_kilos_vacio);
    }

    private void mostrarEstado() {
        raiz.findViewById(R.id.bloqueKilosProgreso).setVisibility(View.GONE);
    }

    private void pintarKilos() {
        if (!fragment.isAdded() || !kilos.lista()) return;
        // Los kilos se piden a la vez que las sesiones: si llegan después y la cuenta no tiene
        // entrenos, no pintan ocho semanas a cero encima del vacío (lo vio el release, lote 4).
        if (cuentaNueva()) {
            pintarVacioCuentaNueva();
            return;
        }
        Context ctx = fragment.requireContext();
        if (kilos.fallo()) {
            mostrarEstado();
            estado.error(VistaEstado.mensaje(ctx, R.string.progreso_kilos_error, kilos.code(), kilos.mensaje()),
                    this::pedir);
            return;
        }
        List<VolumenSemanal> semanas = kilos.dato() == null ? new ArrayList<>() : kilos.dato();
        if (semanas.isEmpty()) {
            mostrarEstado();
            estado.vacio(R.string.progreso_kilos_vacio);
            return;
        }
        estado.oculto();
        raiz.findViewById(R.id.bloqueKilosProgreso).setVisibility(View.VISIBLE);

        Locale idioma = FechaUtils.localeDeLaApp(ctx);
        NumberFormat nf = NumberFormat.getIntegerInstance(idioma);
        double[] k = new double[semanas.size()];
        String[] etiquetas = new String[semanas.size()];
        SimpleDateFormat corta = new SimpleDateFormat(ctx.getString(R.string.inicio_fecha_corta_patron), idioma);
        for (int i = 0; i < k.length; i++) {
            k[i] = semanas.get(i).getVolumenKg();
            etiquetas[i] = etiqueta(semanas.get(i).getInicio(), corta);
        }
        BarrasKilosView barras = raiz.findViewById(R.id.barrasKilosProgreso);
        barras.setSemanas(k, etiquetas);
        barras.setContentDescription(descripcion(ctx, k, nf));

        double esta = k[k.length - 1];
        String cifra = nf.format(Math.round(esta));
        ((TextView) raiz.findViewById(R.id.tvKilosSemana)).setText(cifra);
        String avance = pintarAvance(k.length > 1 ? KilosMovidos.avance(esta, k[k.length - 2]) : null);
        String nombre = ctx.getString(R.string.progreso_kilos_cifra_a11y, cifra);
        raiz.findViewById(R.id.filaKilosSemana).setContentDescription(avance == null ? nombre
                : ctx.getString(R.string.hoy_toca_separador, nombre, avance));
    }

    // «el 62 % de la pasada» en apoyo o «+12 %» en verde; devuelve lo que se dice a TalkBack.
    @Nullable
    private String pintarAvance(@Nullable KilosMovidos.Avance a) {
        TextView tv = raiz.findViewById(R.id.tvKilosAvance);
        Context ctx = tv.getContext();
        if (a == null) {
            tv.setVisibility(View.GONE);
            return null;
        }
        tv.setVisibility(View.VISIBLE);
        if (a.supera) {
            tv.setText(ctx.getString(R.string.progreso_kilos_sube, a.porcentaje));
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.gp_success));
            return ctx.getString(R.string.progreso_kilos_sube_a11y, a.porcentaje);
        }
        tv.setText(ctx.getString(R.string.progreso_kilos_de_la_pasada, a.porcentaje));
        tv.setTextColor(MaterialColors.getColor(tv, com.google.android.material.R.attr.colorOnSurfaceVariant));
        return ctx.getString(R.string.progreso_kilos_de_la_pasada_a11y, a.porcentaje);
    }

    // «14 sep»: el lunes de esa semana, como lo manda la API («2026-09-14T00:00:00»).
    @Nullable
    private static String etiqueta(@Nullable String inicio, SimpleDateFormat formato) {
        if (inicio == null || inicio.length() < 10) return null;
        try {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(inicio.substring(0, 4)), Integer.parseInt(inicio.substring(5, 7)) - 1,
                    Integer.parseInt(inicio.substring(8, 10)));
            return formato.format(c.getTime());
        } catch (NumberFormatException e) {
            return null;   // sin etiqueta esa barra; la cifra sigue en la descripción
        }
    }

    // Con pocas semanas, la cifra de cada una; con muchas, cuántas son, la mayor y la de esta.
    private static String descripcion(Context ctx, double[] k, NumberFormat nf) {
        if (k.length <= SEMANAS_LEIDAS) {
            List<String> partes = new ArrayList<>();
            for (double v : k) partes.add(nf.format(Math.round(v)));
            return ctx.getString(R.string.inicio_kilos_a11y, android.text.TextUtils.join(", ", partes));
        }
        double max = 0;
        for (double v : k) max = Math.max(max, v);
        return ctx.getString(R.string.progreso_kilos_resumen_a11y, k.length,
                ctx.getString(R.string.inicio_kilos_n, nf.format(Math.round(max))),
                ctx.getString(R.string.inicio_kilos_n, nf.format(Math.round(k[k.length - 1]))));
    }
}
