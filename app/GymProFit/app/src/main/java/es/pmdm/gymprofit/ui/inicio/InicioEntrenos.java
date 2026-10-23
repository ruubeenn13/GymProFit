package es.pmdm.gymprofit.ui.inicio;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.sesion.ResumenSesion;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.utils.ExtraEntreno;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.HistorialSesiones;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.Transiciones;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// InicioEntrenos — «Tus últimos entrenos» de Inicio (GP-216, tablero 13): un carrusel con los
// cinco últimos entrenos terminados, cada uno con cuándo, la rutina, los kilos y los minutos, y una
// línea de color con lo que tuvo (ExtraEntreno).
//
// La lista de sesiones no trae kilos ni la sesión anterior: salen de GET /sesiones/{id}/resumen,
// una sola vez por sesión (una sesión terminada no cambia, así que se guarda). Los récords de cada
// sesión salen de la lista de récords vigentes, por su sesión: cuenta los que siguen en pie, un
// récord que otro después superó ya no se cuenta. Cada tarjeta abre el resumen de esa sesión.
// ============================================================
public final class InicioEntrenos {

    /** Cuántos entrenos se enseñan. */
    private static final int MAXIMO = 5;

    private final Fragment fragment;
    private final View raiz;
    private final VistaEstado estado;
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    // Resúmenes ya pedidos, por sesión: una sesión terminada no cambia.
    private final Map<Integer, ResumenSesion> resumenes = new HashMap<>();
    private final Map<Integer, View> tarjetas = new HashMap<>();

    @Nullable private List<SesionEntrenamiento> sesiones;
    @Nullable private Records records;
    private boolean recordsListos;

    public InicioEntrenos(@NonNull Fragment fragment, @NonNull View raiz, @NonNull Runnable abrirHistorial) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoEntrenos));
        raiz.findViewById(R.id.enlaceEntrenos).setOnClickListener(v -> abrirHistorial.run());
    }

    /** Las sesiones del usuario, en cualquier orden. */
    public void setSesiones(@Nullable List<SesionEntrenamiento> sesiones) {
        this.sesiones = sesiones;
        pintar();
    }

    /** Los récords vigentes (o null si no llegaron): con ellos se cuenta lo de cada sesión. */
    public void setRecords(@Nullable Records records) {
        this.records = records;
        this.recordsListos = true;
        pintar();
    }

    /** Las sesiones no llegaron: se dice dentro de la sección. */
    public void fallo(int code, @Nullable String message, @NonNull Runnable alReintentar) {
        raiz.findViewById(R.id.scrollEntrenos).setVisibility(View.GONE);
        estado.error(VistaEstado.mensaje(fragment.requireContext(), R.string.inicio_entrenos_error, code, message),
                alReintentar);
    }

    /** Los récords batidos en esa sesión: los vigentes que salieron de ella. */
    public static int recordsDe(@Nullable Records records, int sesionId) {
        if (records == null) return 0;
        int n = 0;
        for (Record r : records.getRecords()) {
            if (r.getSesionId() != null && r.getSesionId() == sesionId) n++;
        }
        return n;
    }

    // ── Pintar ───────────────────────────────────────────────────────────────

    private void pintar() {
        if (!fragment.isAdded() || sesiones == null) return;
        List<SesionEntrenamiento> ultimas = new ArrayList<>();
        for (SesionEntrenamiento s : HistorialSesiones.masRecientePrimero(sesiones)) {
            if (s.isCompletada()) ultimas.add(s);
            if (ultimas.size() == MAXIMO) break;
        }
        View scroll = raiz.findViewById(R.id.scrollEntrenos);
        if (ultimas.isEmpty()) {
            scroll.setVisibility(View.GONE);
            estado.vacio(R.string.inicio_entrenos_vacio);
            return;
        }
        estado.oculto();
        scroll.setVisibility(View.VISIBLE);

        LinearLayout lista = raiz.findViewById(R.id.listaEntrenos);
        lista.removeAllViews();
        tarjetas.clear();
        LayoutInflater inflater = LayoutInflater.from(fragment.requireContext());
        for (SesionEntrenamiento s : ultimas) {
            View tarjeta = inflater.inflate(R.layout.item_ultimo_entreno, lista, false);
            tarjetas.put(s.getId(), tarjeta);
            lista.addView(tarjeta);
            rellenar(tarjeta, s);
            if (!resumenes.containsKey(s.getId())) pedirResumen(s);
        }
    }

    private void rellenar(View tarjeta, SesionEntrenamiento s) {
        Locale idioma = FechaUtils.localeDeLaApp(fragment.requireContext());
        String cuando = cuando(s.getFechaInicio(), idioma);
        String rutina = s.getRutinaNombre() != null ? s.getRutinaNombre() : fragment.getString(R.string.hecho_hoy_libre);
        ((TextView) tarjeta.findViewById(R.id.tvEntrenoCuando)).setText(cuando);
        ((TextView) tarjeta.findViewById(R.id.tvEntrenoRutina)).setText(rutina);
        String minutos = fragment.getString(R.string.duracion_min, s.getDuracionMinutos());
        ((TextView) tarjeta.findViewById(R.id.tvEntrenoMin)).setText(minutos);

        TextView tvKilos = tarjeta.findViewById(R.id.tvEntrenoKilos);
        TextView tvExtra = tarjeta.findViewById(R.id.tvEntrenoExtra);
        ResumenSesion resumen = resumenes.get(s.getId());
        String kilos = null;
        String extra = null;
        if (resumen != null && resumen.getVolumenKg() > 0) {
            kilos = fragment.getString(R.string.inicio_kilos_n, java.text.NumberFormat.getIntegerInstance(
                    FechaUtils.localeDeLaApp(fragment.requireContext())).format(Math.round(resumen.getVolumenKg())));
            tvKilos.setText(kilos);
        } else {
            tvKilos.setText(null);
        }
        tvKilos.setVisibility(kilos == null ? View.GONE : View.VISIBLE);

        // La línea de color espera los dos datos: sin ellos diría «Sin récords» sin saberlo.
        if (resumen != null && recordsListos) {
            ResumenSesion.SesionAnterior ant = resumen.getAnterior();
            ExtraEntreno e = ExtraEntreno.de(recordsDe(records, s.getId()), resumen.getVolumenKg(),
                    ant == null ? 0 : ant.getVolumenKg());
            extra = pintarExtra(tvExtra, e);
        } else {
            tvExtra.setText(null);
            tvExtra.setVisibility(View.GONE);
        }

        tarjeta.setContentDescription(cuando + ". " + rutina + ". " + (kilos == null ? "" : kilos + ". ")
                + minutos + (extra == null ? "" : ". " + extra));
        tarjeta.setOnClickListener(v -> abrirResumen(s, v));
    }

    // La línea de color de un entreno: dorado con récords, verde si sube, neutro si no.
    private String pintarExtra(TextView tv, ExtraEntreno e) {
        String texto;
        int color;
        switch (e.tipo) {
            case RECORDS:
                texto = fragment.getResources().getQuantityString(R.plurals.inicio_entreno_records, e.n, e.n);
                color = ContextCompat.getColor(fragment.requireContext(), R.color.gp_gold_text);
                break;
            case SUBE:
                texto = fragment.getString(R.string.inicio_entreno_sube, e.n);
                color = ContextCompat.getColor(fragment.requireContext(), R.color.gp_success);
                break;
            case BAJA:
                texto = fragment.getString(R.string.inicio_entreno_baja, e.n);
                color = MaterialColors.getColor(tv, com.google.android.material.R.attr.colorOnSurfaceVariant);
                break;
            default:
                texto = fragment.getString(R.string.inicio_entreno_sin_records);
                color = MaterialColors.getColor(tv, com.google.android.material.R.attr.colorOnSurfaceVariant);
                break;
        }
        tv.setText(texto);
        tv.setTextColor(color);
        tv.setVisibility(View.VISIBLE);
        return texto;
    }

    private void pedirResumen(SesionEntrenamiento s) {
        sesionApi.resumen(s.getId()).enqueue(new ApiCallback<ResumenSesion>() {
            @Override
            public void onOk(ResumenSesion r) {
                if (r == null || !fragment.isAdded()) return;
                resumenes.put(s.getId(), r);
                View tarjeta = tarjetas.get(s.getId());
                if (tarjeta != null) rellenar(tarjeta, s);
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: sin el resumen la tarjeta se queda con su rutina y sus minutos,
                // que ya dicen lo esencial, y abrirla pide el resumen entero y sí enseña su fallo.
            }
        });
    }

    private void abrirResumen(SesionEntrenamiento s, View tarjeta) {
        Intent intent = new Intent(fragment.requireContext(), ResumenSesionActivity.class);
        intent.putExtra("sesionId", s.getId());
        Transiciones.abrirTarjeta(fragment.requireActivity(), tarjeta, intent);
    }

    // «Hoy», «Ayer · martes», «Lunes», «Jueves pasado» o, pasada una semana de la semana, «12 oct».
    private String cuando(@Nullable String iso, Locale idioma) {
        int dias = TiempoRelativo.diasEntre(iso, TiempoRelativo.hoy());
        if (dias < 0 || iso == null || iso.length() < 10) return "";
        Calendar c = Calendar.getInstance();
        c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1,
                Integer.parseInt(iso.substring(8, 10)));
        Date dia = c.getTime();
        String diaSemana = mayuscula(new SimpleDateFormat("EEEE", idioma).format(dia), idioma);
        if (dias == 0) return fragment.getString(R.string.tiempo_hoy_mayuscula);
        if (dias == 1) return fragment.getString(R.string.hoy_toca_separador,
                fragment.getString(R.string.tiempo_ayer_mayuscula), diaSemana.toLowerCase(idioma));
        if (dias < 7) return diaSemana;
        if (dias < 14) return fragment.getString(R.string.inicio_dia_pasado, diaSemana.toLowerCase(idioma));
        return mayuscula(new SimpleDateFormat(fragment.getString(R.string.inicio_fecha_corta_patron), idioma).format(dia), idioma);
    }

    private static String mayuscula(String s, Locale idioma) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(idioma) + s.substring(1);
    }

    /** Para quien necesita saber cuántos récords tuvo la última sesión terminada de la lista. */
    public static int recordsDeLaUltima(@Nullable List<SesionEntrenamiento> sesiones, @Nullable Records records) {
        for (SesionEntrenamiento s : HistorialSesiones.masRecientePrimero(sesiones)) {
            if (s.isCompletada()) return recordsDe(records, s.getId());
        }
        return 0;
    }
}
