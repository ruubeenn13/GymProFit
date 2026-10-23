package es.pmdm.gymprofit.ui.inicio;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.sesion.VolumenSemanal;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.widget.BarrasKilosView;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ProgresoSemanal;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// InicioProgreso — «Tu progreso» de Inicio (GP-216, tablero 13): los kilos por semana de las
// últimas seis, con su «+12 %», y debajo la tarjeta del último récord.
//
// Los kilos salen de GET /sesiones/usuario/{id}/volumen-semanal (el servidor los suma; la lista
// de sesiones no trae kilos). La semana en curso se dibuja pero no cuenta para el porcentaje
// (ProgresoSemanal). La tarjeta del récord, pulsable, crece hasta la progresión de ese ejercicio.
// Los dos trozos cargan por su cuenta: un fallo de los kilos no esconde el récord, y la sección
// entera se oculta solo cuando no hay ni lo uno ni lo otro.
// ============================================================
public final class InicioProgreso {

    /** Las semanas que se dibujan, la actual incluida. */
    static final int SEMANAS = 6;

    /** Quien quiere los récords cuando llegan (los últimos entrenos cuentan los de cada sesión). */
    public interface AlTenerRecords { void listo(@Nullable Records records); }

    private final Fragment fragment;
    private final View raiz;
    private final PreferencesManager prefs;
    private final VistaEstado estado;
    private final AlTenerRecords alTenerRecords;
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);

    // El fallo de la petición de los kilos, para decirlo en el estado de la sección.
    private static final class Fallo {
        final int code;
        final String message;

        Fallo(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    private boolean kilosListos;
    private boolean hayKilos;
    @Nullable private Fallo fallo;
    private boolean hayRecord;
    private int generacionKilos;
    private int generacionRecord;

    public InicioProgreso(@NonNull Fragment fragment, @NonNull View raiz, @NonNull PreferencesManager prefs,
                          @NonNull Runnable abrirProgreso, @NonNull AlTenerRecords alTenerRecords) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.prefs = prefs;
        this.alTenerRecords = alTenerRecords;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoProgreso));
        raiz.findViewById(R.id.enlaceProgreso).setOnClickListener(v -> abrirProgreso.run());
    }

    /** Pide los kilos de las últimas semanas y el último récord. */
    public void cargar() {
        int uid = prefs.getUsuarioId();
        if (uid == -1) {
            raiz.findViewById(R.id.seccionProgreso).setVisibility(View.GONE);
            return;
        }
        cargarKilos(uid);
        cargarRecord();
    }

    // ── Kilos por semana ─────────────────────────────────────────────────────

    private void cargarKilos(int uid) {
        final int esta = ++generacionKilos;
        sesionApi.getVolumenSemanal(uid, primerLunesIso(), SEMANAS).enqueue(new ApiCallback<List<VolumenSemanal>>() {
            @Override
            public void onOk(List<VolumenSemanal> semanas) {
                if (esta != generacionKilos || !fragment.isAdded()) return;
                pintarKilos(semanas == null ? new ArrayList<>() : semanas);
            }

            @Override
            public void onFail(int code, String message) {
                if (esta != generacionKilos || !fragment.isAdded()) return;
                kilosListos = true;
                hayKilos = false;
                fallo = new Fallo(code, message);
                raiz.findViewById(R.id.bloqueKilos).setVisibility(View.GONE);
                actualizar();
            }
        });
    }

    // El lunes de hace cinco semanas a las 00:00, en la hora del móvil: la primera de las seis.
    static String primerLunesIso() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, -((dow + 5) % 7));
        cal.add(Calendar.WEEK_OF_YEAR, -(SEMANAS - 1));
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.getTime());
    }

    private void pintarKilos(List<VolumenSemanal> semanas) {
        kilosListos = true;
        fallo = null;
        double[] kilos = new double[semanas.size()];
        String[] etiquetas = new String[semanas.size()];
        for (int i = 0; i < kilos.length; i++) {
            kilos[i] = semanas.get(i).getVolumenKg();
            etiquetas[i] = fragment.getString(R.string.inicio_semana_corta, i + 1);
        }
        hayKilos = ProgresoSemanal.hayDatos(kilos);
        View bloque = raiz.findViewById(R.id.bloqueKilos);
        bloque.setVisibility(hayKilos ? View.VISIBLE : View.GONE);
        if (hayKilos) {
            BarrasKilosView barras = raiz.findViewById(R.id.barrasKilos);
            barras.setSemanas(kilos, etiquetas);
            NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(fragment.requireContext()));
            barras.setContentDescription(fragment.getString(R.string.inicio_kilos_a11y, valores(kilos, nf)));
            pintarVariacion(ProgresoSemanal.variacion(kilos, true));
        }
        actualizar();
    }

    private String valores(double[] kilos, NumberFormat nf) {
        List<String> partes = new ArrayList<>();
        for (double k : kilos) partes.add(nf.format(Math.round(k)));
        return android.text.TextUtils.join(", ", partes);
    }

    private void pintarVariacion(@Nullable ProgresoSemanal.Variacion v) {
        TextView tv = raiz.findViewById(R.id.tvKilosVariacion);
        if (v == null) {
            tv.setVisibility(View.GONE);
            return;
        }
        tv.setVisibility(View.VISIBLE);
        String signo = v.porcentaje > 0 ? "+" : v.porcentaje < 0 ? "\u2212" : "";
        tv.setText(fragment.getString(R.string.inicio_variacion, signo, Math.abs(v.porcentaje), v.semanas));
        tv.setTextColor(v.porcentaje > 0
                ? ContextCompat.getColor(fragment.requireContext(), R.color.gp_success)
                : MaterialColors.getColor(tv, com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    // ── Último récord ────────────────────────────────────────────────────────

    private void cargarRecord() {
        MaterialCardView card = raiz.findViewById(R.id.cardRecordHome);
        final int esta = ++generacionRecord;
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override
            public void onOk(Records r) {
                if (esta != generacionRecord || !fragment.isAdded()) return;
                alTenerRecords.listo(r);
                if (r == null || r.getRecords().isEmpty()) {
                    hayRecord = false;
                    card.setVisibility(View.GONE);
                } else {
                    pintarRecord(r.getRecords().get(0));
                }
                actualizar();
            }

            @Override
            public void onFail(int code, String message) {
                if (esta != generacionRecord || !fragment.isAdded()) return;
                // El récord es un extra de la sección: si falla se queda oculto y los kilos se
                // leen igual (o dicen su propio fallo). Queda rastro en el log.
                Log.w("GymProFit", "cargarRecord falló (" + code + "): " + message);
                alTenerRecords.listo(null);
                hayRecord = false;
                card.setVisibility(View.GONE);
                actualizar();
            }
        });
    }

    // «Aperturas en máquina», «Último récord · hoy» y «Antes: 82,5 kg × 10», con la marca a la derecha.
    private void pintarRecord(Record record) {
        hayRecord = true;
        MaterialCardView card = raiz.findViewById(R.id.cardRecordHome);
        card.setVisibility(View.VISIBLE);
        String nombre = record.nombre(FechaUtils.localeDeLaApp(fragment.requireContext()));
        // La marca («60 kg × 10») no se parte: con letra grande dejaba el «10» solo en una tercera línea.
        String marca = Marcas.texto(fragment.requireContext(), record).replace(' ', '\u00A0');
        ((TextView) raiz.findViewById(R.id.tvRecordEjercicio)).setText(nombre);
        ((TextView) raiz.findViewById(R.id.tvRecordMarca)).setText(marca);

        TextView sub = raiz.findViewById(R.id.tvRecordAntes);
        String cuando = TiempoRelativo.texto(fragment.requireContext(), record.getFecha());
        String anterior = Marcas.anteriorSolo(fragment.requireContext(), record);
        if (anterior != null) anterior = anterior.replace(' ', '\u00A0');
        String ultimo = cuando == null ? fragment.getString(R.string.ultimo_record)
                : fragment.getString(R.string.hoy_toca_separador, fragment.getString(R.string.ultimo_record), cuando);
        sub.setText(anterior == null ? ultimo : fragment.getString(R.string.inicio_dos_lineas, ultimo,
                fragment.getString(R.string.record_antes, anterior)));
        card.setContentDescription(nombre + ". " + sub.getText().toString().replace('\n', ' ') + ". " + marca);

        // La ficha espera el ejercicio entero en extras: se pide al pulsar.
        card.setOnClickListener(v ->
                ejercicioApi.getPorId(record.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
                    @Override
                    public void onOk(Ejercicio ejercicio) {
                        if (!fragment.isAdded() || ejercicio == null) return;
                        EjercicioNavHelper.abrir(fragment.requireActivity(), card, ejercicio);
                    }

                    @Override
                    public void onFail(int code, String message) {
                        if (!fragment.isAdded()) return;
                        UiFeedback.toastError(fragment.requireActivity(), code, message);
                    }
                }));
    }

    // ── La sección entera ────────────────────────────────────────────────────

    // La tarjeta se ve si hay kilos o récord; si no, el estado: el error con «Reintentar» o el vacío.
    private void actualizar() {
        if (!kilosListos) return;
        View tarjeta = raiz.findViewById(R.id.cardProgreso);
        if (hayKilos || hayRecord) {
            tarjeta.setVisibility(View.VISIBLE);
            if (fallo != null && !hayKilos) {
                // Hay récord pero los kilos fallaron: el fallo se dice sin tapar el récord.
                estado.error(VistaEstado.mensaje(fragment.requireContext(), R.string.inicio_progreso_error,
                        fallo.code, fallo.message), () -> cargarKilos(prefs.getUsuarioId()));
            } else {
                estado.oculto();
            }
            return;
        }
        tarjeta.setVisibility(View.GONE);
        if (fallo != null) {
            estado.error(VistaEstado.mensaje(fragment.requireContext(), R.string.inicio_progreso_error,
                    fallo.code, fallo.message), () -> cargarKilos(prefs.getUsuarioId()));
        } else {
            estado.vacio(R.string.inicio_progreso_vacio);
        }
    }
}
