package es.pmdm.gymprofit.ui.progreso;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.utils.AvisosCuenta;
import es.pmdm.gymprofit.utils.CambioPeso;
import es.pmdm.gymprofit.utils.EjeFechas;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.InputDialog;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.MedicionHoy;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.PuntosGrafica;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// CuerpoProgreso — «Tu cuerpo» de Progreso (GP-216, lote 4, tablero 16): el último peso, cuánto
// ha cambiado en las últimas 8 semanas (CambioPeso), su línea, el objetivo y, solo si el aviso de
// progreso está encendido, que te avisamos si pasa un mes sin pesarte.
//
// El aviso es el de la API (RecordatorioNotificacionesTask: la última medición de hace 30 días o
// más, como mucho uno al mes): no tiene día fijo, así que no se inventa uno («el día 1»). Sin el
// permiso de notificaciones del móvil no llegaría, y tampoco se promete.
//
// «Apuntar peso» abre la hoja del peso aquí mismo y guarda con la regla de Medidas (MedicionHoy:
// una medición por día). La tarjeta abre Medidas, con todo lo demás.
// ============================================================
final class CuerpoProgreso {

    /** Quien recarga las mediciones tras apuntar el peso. */
    interface AlGuardar { void guardado(); }

    private final Fragment fragment;
    private final View raiz;
    private final PreferencesManager prefs;
    private final VistaEstado estado;
    private final AlGuardar alGuardar;
    private final MedicionApi medicionApi = ApiClient.service(MedicionApi.class);
    @Nullable private MedicionCorporal ultima;
    private boolean medicionesListas;

    CuerpoProgreso(@NonNull Fragment fragment, @NonNull View raiz, @NonNull PreferencesManager prefs,
                   @NonNull Runnable abrirMedidas, @NonNull AlGuardar alGuardar) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.prefs = prefs;
        this.alGuardar = alGuardar;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoCuerpo));
        View card = raiz.findViewById(R.id.cardCuerpo);
        card.setOnClickListener(v -> abrirMedidas.run());
        androidx.core.view.ViewCompat.replaceAccessibilityAction(card,
                androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                fragment.getString(R.string.progreso_ver_medidas), null);
        raiz.findViewById(R.id.enlaceApuntarPeso).setOnClickListener(v -> apuntarPeso());
        prepararLinea(raiz.findViewById(R.id.chartPesoProgreso));
    }

    /** La última medición, para la hoja del «+» (null si no hay). */
    @Nullable MedicionCorporal ultima() { return ultima; }

    void pintar(@NonNull Fuente<List<MedicionCorporal>> mediciones, @NonNull Fuente<Usuario> usuario,
                @NonNull Runnable reintentar) {
        Context ctx = raiz.getContext();
        View card = raiz.findViewById(R.id.cardCuerpo);
        View enlace = raiz.findViewById(R.id.enlaceApuntarPeso);
        if (!mediciones.lista()) {
            // Sin saber si hoy ya hay medición, apuntar podría duplicar el día.
            enlace.setVisibility(medicionesListas ? View.VISIBLE : View.GONE);
            if (card.getVisibility() != View.VISIBLE) estado.cargando();
            return;
        }
        if (mediciones.fallo()) {
            medicionesListas = false;
            card.setVisibility(View.GONE);
            enlace.setVisibility(View.GONE);
            estado.error(VistaEstado.mensaje(ctx, R.string.progreso_cuerpo_error, mediciones.code(), mediciones.mensaje()),
                    reintentar);
            return;
        }
        medicionesListas = true;
        enlace.setVisibility(View.VISIBLE);
        List<MedicionCorporal> lista = mediciones.dato() == null ? new ArrayList<>() : mediciones.dato();
        ultima = lista.isEmpty() ? null : lista.get(0);
        List<CambioPeso.Punto> puntos = new ArrayList<>();
        for (MedicionCorporal m : lista) puntos.add(new CambioPeso.Punto(m.getFecha(), m.getPeso()));
        CambioPeso.Resultado r = CambioPeso.de(puntos);
        if (r == null) {
            card.setVisibility(View.GONE);
            estado.vacio(R.drawable.ic_ms_monitor_weight, R.string.progreso_cuerpo_vacio);
            return;
        }
        estado.oculto();
        card.setVisibility(View.VISIBLE);

        Locale idioma = FechaUtils.localeDeLaApp(ctx);
        NumberFormat uno = NumberFormat.getNumberInstance(idioma);
        uno.setMinimumFractionDigits(1);
        uno.setMaximumFractionDigits(1);
        String peso = uno.format(r.ultimo.peso);
        ((TextView) raiz.findViewById(R.id.tvPesoProgreso)).setText(peso);
        List<String> partes = new ArrayList<>();
        partes.add(ctx.getString(R.string.progreso_peso_a11y, peso));

        TextView tvCambio = raiz.findViewById(R.id.tvCambioPeso);
        if (r.cambio != null) {
            String signo = r.cambio > 0 ? "+" : r.cambio < 0 ? "−" : "";
            String cambio = ctx.getResources().getQuantityString(R.plurals.progreso_cambio_peso, r.semanas,
                    signo + uno.format(Math.abs(r.cambio)), r.semanas);
            tvCambio.setText(cambio);
            tvCambio.setVisibility(View.VISIBLE);
            partes.add(cambio);
        } else {
            tvCambio.setVisibility(View.GONE);
        }

        // Con un día de pesajes, el punto con su valor y su fecha y la pista; con dos o más, la
        // línea (GP-229: con un pesaje no salía nada).
        LineChart linea = raiz.findViewById(R.id.chartPesoProgreso);
        TextView pista = raiz.findViewById(R.id.tvPistaCuerpo);
        List<EjeFechas.Punto> dias = porDia(r.puntos);
        PuntosGrafica.Que que = PuntosGrafica.de(dias.size());
        pista.setVisibility(que == PuntosGrafica.Que.UN_PUNTO ? View.VISIBLE : View.GONE);
        if (que == PuntosGrafica.Que.LINEA) {
            pintarLinea(linea, dias, null);
            partes.add(ctx.getResources().getQuantityString(R.plurals.progreso_linea_peso_a11y, r.semanas,
                    uno.format(r.puntos.get(0).peso), peso, r.semanas));
        } else if (que == PuntosGrafica.Que.UN_PUNTO) {
            String fecha = EjeFechas.etiqueta(dias.get(0).x, idioma, ctx.getString(R.string.inicio_fecha_corta_patron));
            pintarLinea(linea, dias, ctx.getString(R.string.unidad_kg_decimal, (double) dias.get(0).y));
            partes.add(ctx.getString(R.string.progreso_un_peso_a11y, peso, fecha));
            partes.add(pista.getText().toString());
        } else {
            linea.setVisibility(View.GONE);
        }

        TextView tvObjetivo = raiz.findViewById(R.id.tvObjetivoCuerpo);
        String objetivo = usuario.ok() && usuario.dato() != null && usuario.dato().getObjetivo() != null
                ? usuario.dato().getObjetivo() : prefs.getObjetivo();
        if (objetivo != null && !objetivo.isEmpty()) {
            String texto = ctx.getString(R.string.progreso_objetivo, UIHelper.traducirObjetivo(ctx, objetivo));
            tvObjetivo.setText(texto);
            tvObjetivo.setVisibility(View.VISIBLE);
            partes.add(texto);
        } else {
            tvObjetivo.setVisibility(View.GONE);
        }

        // Solo con el aviso encendido en la cuenta y el permiso del móvil: si no, no llegaría.
        boolean aviso = usuario.ok() && usuario.dato() != null
                && AvisosCuenta.Estado.de(null, null, usuario.dato().getAvisosProgreso()).progreso
                && NotificationManagerCompat.from(ctx).areNotificationsEnabled();
        raiz.findViewById(R.id.tvAvisoPeso).setVisibility(aviso ? View.VISIBLE : View.GONE);
        if (aviso) partes.add(ctx.getString(R.string.progreso_aviso_peso));
        card.setContentDescription(android.text.TextUtils.join(". ", partes));
    }

    // ── La línea ─────────────────────────────────────────────────────────────

    // Una línea quieta, sin ejes ni toques: la tarjeta entera es la que se pulsa (abre Medidas) y
    // lo que dice la línea va en la descripción de la tarjeta (DEC-019).
    private static void prepararLinea(LineChart c) {
        ChartStyler.styleLine(c, null);
        c.setTouchEnabled(false);
        c.getXAxis().setEnabled(false);
        c.getAxisLeft().setEnabled(false);
        c.getAxisRight().setEnabled(false);
        // Aire de 6 dp: los puntos de los extremos salían cortados por la mitad.
        c.setExtraOffsets(6, 6, 6, 6);
        c.setMinOffset(0);
        c.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    // Por fecha y no por orden (GP-228): dos pesajes separados un día no salen igual de lejos
    // que dos separados un mes. Varios el mismo día darían una línea vertical: se queda el
    // último del día (EjeFechas).
    private static List<EjeFechas.Punto> porDia(List<CambioPeso.Punto> puntos) {
        List<String> fechas = new ArrayList<>();
        List<Float> pesos = new ArrayList<>();
        for (CambioPeso.Punto p : puntos) {
            fechas.add(p.fecha);
            pesos.add((float) p.peso);
        }
        return EjeFechas.porDia(fechas, pesos, EjeFechas.DelDia.ULTIMO);
    }

    // La línea, o con un solo día el punto con su valor encima y su fecha debajo (unPunto no
    // nulo): sin ejes en la línea, que las semanas ya las dice el texto.
    private static void pintarLinea(LineChart c, List<EjeFechas.Punto> dias, @Nullable String unPunto) {
        List<Entry> e = new ArrayList<>();
        for (EjeFechas.Punto p : dias) e.add(new Entry(p.x, p.y));
        LineDataSet ds = new LineDataSet(e, null);
        ChartStyler.styleLineDataSet(ds, c.getContext());
        ds.setDrawFilled(false);
        ds.setHighlightEnabled(false);
        ds.setMode(LineDataSet.Mode.LINEAR);
        int azul = androidx.core.content.ContextCompat.getColor(c.getContext(), R.color.gp_linea_peso);
        ds.setColor(azul);
        ds.setCircleColor(azul);
        boolean solo = unPunto != null;
        if (solo) PuntosGrafica.unPunto(ds, c.getContext(), unPunto);
        c.getXAxis().setEnabled(solo);
        if (solo) {
            Locale idioma = FechaUtils.localeDeLaApp(c.getContext());
            String patron = c.getContext().getString(R.string.inicio_fecha_corta_patron);
            c.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                    return EjeFechas.llevaEtiqueta(value, dias) ? EjeFechas.etiqueta(value, idioma, patron) : "";
                }
            });
        }
        c.setData(new LineData(ds));
        c.setVisibility(View.VISIBLE);
        c.invalidate();
    }

    // ── Apuntar el peso ──────────────────────────────────────────────────────

    private void apuntarPeso() {
        if (!fragment.isAdded() || !medicionesListas) return;
        String inicial = ultima != null && ultima.getPeso() > 0
                ? String.format(FechaUtils.localeDeLaApp(fragment.requireContext()), "%.1f", ultima.getPeso()) : null;
        InputDialog.numerico(fragment.requireActivity(), fragment.getString(R.string.perfil_peso),
                fragment.getString(R.string.dialogo_nuevo_valor), inicial, fragment.getString(R.string.progreso_unidad_kg),
                valor -> guardar(valor));
    }

    private void guardar(String valor) {
        if (!fragment.isAdded() || valor.isEmpty()) return;
        // Los mismos límites que Medidas: un dedo torpe no deja 750 kg para siempre en la gráfica.
        BigDecimal peso = Numeros.exacto(valor, 30, 300);
        if (peso == null) {
            UIHelper.mostrarToastError(fragment.requireActivity(), fragment.getString(R.string.error_peso_invalido));
            return;
        }
        ApiCallback<MedicionCorporal> cb = new ApiCallback<MedicionCorporal>() {
            @Override
            public void onOk(MedicionCorporal m) {
                if (!fragment.isAdded()) return;
                LoadingDialog.hide(fragment.requireActivity());
                alGuardar.guardado();
            }

            @Override
            public void onFail(int code, String message) {
                if (!fragment.isAdded()) return;
                LoadingDialog.hide(fragment.requireActivity());
                UiFeedback.toastError(fragment.requireActivity(), code, message);
            }
        };
        LoadingDialog.show(fragment.requireActivity());
        if (MedicionHoy.esDeHoy(ultima, TiempoRelativo.hoy())) {
            Map<String, Object> body = new HashMap<>();
            body.put("peso", peso);
            medicionApi.patch(ultima.getId(), body).enqueue(cb);
        } else {
            Map<String, Object> body = MedicionHoy.copiaDe(ultima, prefs.getUsuarioId());
            body.put("peso", peso);
            medicionApi.crear(body).enqueue(cb);
        }
    }
}
