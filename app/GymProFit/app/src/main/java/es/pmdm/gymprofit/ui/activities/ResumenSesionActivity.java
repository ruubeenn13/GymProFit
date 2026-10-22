package es.pmdm.gymprofit.ui.activities;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.sesion.ResumenSesion;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.nutricion.ParaRecuperarSeccion;
import es.pmdm.gymprofit.utils.ComparacionEntreno;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ResumenSesionActivity — el resumen del entreno (GP-210 paso 1, GP-023, GP-011).
//
// Tablero 3 «Resumen» de documentacion/diseno/2026-10-22-firma-visual/, que sustituye a
// «Sesión completada» del tablero 11 de nutrición. La recompensa del final del entreno, de
// arriba abajo (momento 28 de DEC-039):
//   1. Los kilos que cuentan desde cero (CIFRAS) y tres fichas: la variación frente a la
//      sesión anterior de la misma rutina, la duración y las series.
//   2. La tarjeta dorada de récords, con la marca anterior (momento 12, regla de GP-088).
//   3. «Frente a la última vez», por ejercicio, en cascada (CASCADA). La primera vez de un
//      ejercicio no lleva porcentaje (GP-014: «Primera vez: es tu punto de partida»).
//   4. El logro conseguido, con el medallón dorado. Sin Profi: llega en el paso 5.
//   5. «Para recuperar» (ParaRecuperarSeccion): la proteína del día y tres ideas con su «+».
// Abajo, «Hecho». «Compartir» no se dibuja hasta la tarjeta del paso 4.
//
// Los kilos, las series y las comparaciones salen del servidor (GET /sesiones/{id}/resumen,
// con la definición de volumen de GP-011) y sirven también las sesiones del historial; los
// récords y los logros nuevos solo los trae el guardado, así que al abrir una sesión vieja
// esas dos tarjetas no aparecen. Cargando y error, con VistaEstado.
// ============================================================
public class ResumenSesionActivity extends AppCompatActivity {

    /** Récords que batió la sesión recién guardada (ArrayList de Record), GP-088. */
    public static final String EXTRA_RECORDS = "recordsBatidos";
    /** Cuántos ejercicios se hacían por primera vez en la sesión recién guardada. */
    public static final String EXTRA_PRIMERAS_MARCAS = "primerasMarcas";

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private PreferencesManager prefsManager;
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final LogroApi logroApi = ApiClient.service(LogroApi.class);

    private View scroll;
    private VistaEstado estado;
    private View layoutCelebracion;
    private TextView tvVolumenHero;
    private ChipGroup fichas;

    private int sesionId;
    private SesionEntrenamiento sesion;
    private ResumenSesion resumen;
    private ArrayList<String> nuevosLogros = new ArrayList<>();
    // Se llegó desde el registro de la sesión (no desde el historial): lanza las notificaciones.
    private boolean fromRegistrar = false;
    private int turno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_resumen_sesion);

        sesionId = getIntent().getIntExtra("sesionId", -1);
        if (sesionId == -1) { finish(); return; }

        ArrayList<String> extras = getIntent().getStringArrayListExtra("nuevosLogros");
        if (extras != null) {
            nuevosLogros = extras;
            fromRegistrar = true;
        }

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnHechoResumen).setOnClickListener(v -> finish());
        scroll = findViewById(R.id.scrollResumen);
        estado = new VistaEstado(findViewById(R.id.estadoResumen));
        layoutCelebracion = findViewById(R.id.layoutCelebracion);
        tvVolumenHero = findViewById(R.id.tvVolumenHero);
        fichas = findViewById(R.id.fichasResumen);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.tvUltimaVezTitulo), true);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.tvRecordsTituloResumen), true);

        cargar();
    }

    // ── Carga ───────────────────────────────────────────────────────────────

    // La sesión (duración, día) y su resumen (kilos, series, comparaciones): hacen falta las
    // dos, y si una falla se dice y se puede reintentar. Los logros se piden aparte, después.
    private void cargar() {
        int miTurno = ++turno;
        sesion = null;
        resumen = null;
        scroll.setVisibility(View.INVISIBLE);
        estado.cargando();
        AtomicInteger pendientes = new AtomicInteger(2);
        boolean[] fallo = {false};
        String[] porque = {null};

        sesionApi.getPorId(sesionId).enqueue(new ApiCallback<SesionEntrenamiento>() {
            @Override public void onOk(SesionEntrenamiento body) {
                sesion = body;
                llego(miTurno, pendientes, fallo, porque);
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                porque[0] = VistaEstado.mensaje(ResumenSesionActivity.this, R.string.resumen_error, code, message).toString();
                llego(miTurno, pendientes, fallo, porque);
            }
        });
        sesionApi.resumen(sesionId).enqueue(new ApiCallback<ResumenSesion>() {
            @Override public void onOk(ResumenSesion body) {
                resumen = body;
                llego(miTurno, pendientes, fallo, porque);
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                porque[0] = VistaEstado.mensaje(ResumenSesionActivity.this, R.string.resumen_error, code, message).toString();
                llego(miTurno, pendientes, fallo, porque);
            }
        });
    }

    private void llego(int miTurno, AtomicInteger pendientes, boolean[] fallo, String[] porque) {
        if (miTurno != turno || isDestroyed()) return;
        if (pendientes.decrementAndGet() > 0) return;
        if (fallo[0] || sesion == null || resumen == null) {
            estado.error(porque[0] != null ? porque[0] : getString(R.string.resumen_error), this::cargar);
            return;
        }
        mostrarContenido();
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    private void mostrarContenido() {
        estado.oculto();
        scroll.setVisibility(View.VISIBLE);
        pintarCabecera();
        pintarRecords();
        pintarUltimaVez();
        pintarLogro();
        arrancarParaRecuperar();

        if (fromRegistrar) {
            NotificationHelper.notificarSesionCompletada(this, sesion.getDuracionMinutos());
            if (!nuevosLogros.isEmpty()) {
                NotificationHelper.notificarLogrosDesbloqueados(this, nuevosLogros);
            }
        }
    }

    // Los kilos desde cero y las tres fichas. Sin pesos que contar el número enorme celebraría
    // un cero: se dice y se siguen enseñando la duración y las series.
    private void pintarCabecera() {
        double kilos = resumen.getVolumenKg();
        TextView sinKilos = findViewById(R.id.tvSinKilos);
        if (kilos <= 0) {
            layoutCelebracion.setVisibility(View.GONE);
            sinKilos.setVisibility(View.VISIBLE);
        } else {
            sinKilos.setVisibility(View.GONE);
            layoutCelebracion.setVisibility(View.VISIBLE);
            String total = formatoMiles(Math.round(kilos));
            // Una sola frase para TalkBack: el número no se anuncia contando.
            layoutCelebracion.setContentDescription(getString(R.string.resumen_a11y_kilos, total));
            ViewCompat.setImportantForAccessibility(layoutCelebracion, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            layoutCelebracion.setFocusable(true);
            for (int i = 0; i < ((LinearLayout) layoutCelebracion).getChildCount(); i++) {
                ((LinearLayout) layoutCelebracion).getChildAt(i)
                        .setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            tvVolumenHero.setText(formatoMiles(0));
            Movimiento.entrar(0, layoutCelebracion);
            // Momento 28: los kilos cuentan desde cero (CIFRAS).
            Movimiento.contar(0, Movimiento.CIFRAS,
                    f -> tvVolumenHero.setText(formatoMiles(Math.round(kilos * f))));
        }

        fichas.removeAllViews();
        ComparacionEntreno.Variacion v = ComparacionEntreno.variacionDeSesion(resumen);
        if (v != null) anadirFichaVariacion(v);
        anadirFicha(R.drawable.ic_ms_timer, getString(R.string.sesiones_min, sesion.getDuracionMinutos()), false);
        int series = resumen.getSeries();
        if (series > 0) {
            anadirFicha(R.drawable.ic_ms_fitness_center,
                    getResources().getQuantityString(R.plurals.resumen_series_chip, series, series), false);
        }
    }

    // «+6 % que el jueves», en verde si sube. Si baja o es igual no se pinta de rojo: una
    // sesión más floja no es un fallo, y el porcentaje ya lo dice.
    private void anadirFichaVariacion(ComparacionEntreno.Variacion v) {
        String cuando = cuandoFue(resumen.getAnterior().getFecha());
        switch (v.tendencia) {
            case SUBE:
                anadirFicha(R.drawable.ic_ms_trending_up, getString(R.string.resumen_vs_sube, v.porcentaje, cuando), true);
                break;
            case BAJA:
                anadirFicha(R.drawable.ic_ms_trending_down, getString(R.string.resumen_vs_baja, -v.porcentaje, cuando), false);
                break;
            default:
                anadirFicha(R.drawable.ic_ms_check, getString(R.string.resumen_vs_igual, cuando), false);
        }
    }

    private void anadirFicha(@DrawableRes int icono, String texto, boolean verde) {
        TextView ficha = (TextView) LayoutInflater.from(this).inflate(R.layout.item_ficha_resumen, fichas, false);
        ficha.setText(texto);
        ficha.setBackgroundResource(verde ? R.drawable.bg_ficha_resumen_verde : R.drawable.bg_ficha_resumen);
        Drawable d = ContextCompat.getDrawable(this, icono);
        if (d != null) {
            d = d.mutate();
            int px = (int) (18 * getResources().getDisplayMetrics().density);
            d.setBounds(0, 0, px, px);
            d.setTint(verde ? ContextCompat.getColor(this, R.color.gp_success) : color(com.google.android.material.R.attr.colorOnSurfaceVariant));
            ficha.setCompoundDrawablesRelative(d, null, null, null);
        }
        if (verde) ficha.setTextColor(ContextCompat.getColor(this, R.color.gp_success));
        fichas.addView(ficha);
    }

    // «el jueves» si la sesión anterior fue hace menos de una semana; si no, «la última vez».
    private String cuandoFue(@Nullable String isoAnterior) {
        Locale idioma = FechaUtils.localeDeLaApp(this);
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date antes = iso.parse(isoAnterior);
            Date ahora = sesion.getFechaInicio() != null ? iso.parse(sesion.getFechaInicio()) : new Date();
            if (antes != null && ahora != null && ahora.getTime() - antes.getTime() < 7L * 24 * 3600 * 1000) {
                return getString(R.string.resumen_vs_dia, new SimpleDateFormat("EEEE", idioma).format(antes));
            }
        } catch (ParseException | NullPointerException e) {
            // Una fecha que no se entiende solo quita el nombre del día: «la última vez» dice lo mismo.
        }
        return getString(R.string.resumen_vs_ultima);
    }

    /**
     * Pinta los récords que batió la sesión y cuántas primeras marcas dejó (GP-088).
     *
     * <p>Los trae el guardado, así que solo existen al llegar desde el registro; al abrir el
     * resumen de una sesión vieja la tarjeta no aparece. Tampoco aparece si no hay nada: una
     * sección de récords vacía es un reproche.
     */
    @SuppressWarnings("unchecked")
    private void pintarRecords() {
        java.io.Serializable extra = getIntent().getSerializableExtra(EXTRA_RECORDS);
        List<Record> records = extra instanceof List ? (List<Record>) extra : new ArrayList<>();
        int primeras = getIntent().getIntExtra(EXTRA_PRIMERAS_MARCAS, 0);
        MaterialCardView tarjeta = findViewById(R.id.layoutRecordsResumen);
        if (records.isEmpty() && primeras == 0) {
            tarjeta.setVisibility(View.GONE);
            return;
        }
        tarjeta.setVisibility(View.VISIBLE);
        findViewById(R.id.cabeceraRecordsResumen).setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.tvRecordsTituloResumen)).setText(
                getResources().getQuantityString(R.plurals.resumen_records_titulo, records.size(), records.size()));

        LinearLayout contenedor = findViewById(R.id.contenedorRecordsResumen);
        contenedor.removeAllViews();
        contenedor.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        Locale idioma = FechaUtils.localeDeLaApp(this);
        int oro = ContextCompat.getColor(this, R.color.gp_gold);
        int oroTexto = ContextCompat.getColor(this, R.color.gp_gold_text);
        int claro = ContextCompat.getColor(this, R.color.gp_on_primary_container);
        int orden = 0;
        for (int i = 0; i < records.size(); i++) {
            Record r = records.get(i);
            View fila = getLayoutInflater().inflate(R.layout.item_record, contenedor, false);
            // El trofeo dorado de cada récord batido, que celebra al entrar (GP-104, momento 12).
            ImageView trofeo = fila.findViewById(R.id.ivTrofeoRecord);
            trofeo.setImageResource(R.drawable.ic_ms_trophy_fill);
            trofeo.setImageTintList(ColorStateList.valueOf(oro));
            String nombre = r.nombre(idioma);
            String marca = Marcas.texto(this, r);
            String antes = Marcas.anterior(this, r);
            ((TextView) fila.findViewById(R.id.tvNombreRecord)).setText(nombre);
            TextView tvMarca = fila.findViewById(R.id.tvMarcaRecord);
            tvMarca.setText(marca);
            tvMarca.setTextColor(oroTexto);
            // Por tiempo (GP-125), el cronómetro delante de «1:05».
            Marcas.iconoTiempo(tvMarca, r);
            TextView tvDetalle = fila.findViewById(R.id.tvDetalleRecord);
            tvDetalle.setText(antes);
            tvDetalle.setVisibility(antes == null ? View.GONE : View.VISIBLE);
            View raya = fila.findViewById(R.id.divRecord);
            raya.setBackgroundColor(ColorUtils.setAlphaComponent(oro, 77));
            raya.setVisibility(i == records.size() - 1 ? View.GONE : View.VISIBLE);
            // Aquí la fila solo informa: no lleva a ningún sitio.
            fila.setClickable(false);
            fila.setFocusable(true);
            fila.setContentDescription(getString(R.string.record_a11y, nombre, marca, antes != null ? antes : ""));
            contenedor.addView(fila);
            // Uno detrás de otro, para que cada récord se vea celebrar (momento 12).
            Movimiento.celebrarRecord((android.view.ViewGroup) fila, trofeo, oro, claro, 400L + 300L * orden++);
        }

        TextView tvPrimeras = findViewById(R.id.tvPrimerasMarcasResumen);
        if (primeras > 0) {
            tvPrimeras.setText(getResources().getQuantityString(R.plurals.resumen_primeras_marcas, primeras, primeras));
            tvPrimeras.setVisibility(View.VISIBLE);
        } else {
            tvPrimeras.setVisibility(View.GONE);
        }
        Movimiento.entrarUna(tarjeta, 150, Movimiento.ENTRA, 16);
    }

    // «Frente a la última vez»: una fila por ejercicio que mueve kilos, en cascada. La primera
    // vez de un ejercicio no tiene porcentaje, sino «Primera vez: es tu punto de partida».
    private void pintarUltimaVez() {
        MaterialCardView tarjeta = findViewById(R.id.cardUltimaVez);
        List<ComparacionEntreno.Fila> filas = ComparacionEntreno.filas(resumen);
        LinearLayout contenedor = findViewById(R.id.contenedorUltimaVez);
        contenedor.removeAllViews();
        if (filas.isEmpty()) {
            tarjeta.setVisibility(View.GONE);
            return;
        }
        tarjeta.setVisibility(View.VISIBLE);
        for (int i = 0; i < filas.size(); i++) {
            ComparacionEntreno.Fila f = filas.get(i);
            View fila = getLayoutInflater().inflate(R.layout.item_ultima_vez, contenedor, false);
            ((TextView) fila.findViewById(R.id.tvNombreUltimaVez)).setText(f.nombre);
            String kilos = getString(R.string.resumen_kilos_fila, formatoMiles(Math.round(f.kilos)));
            ((TextView) fila.findViewById(R.id.tvKilosUltimaVez)).setText(kilos);
            TextView tvPct = fila.findViewById(R.id.tvPctUltimaVez);
            String pct;
            String pctHablado;
            if (f.primeraVez()) {
                fila.findViewById(R.id.tvPrimeraVez).setVisibility(View.VISIBLE);
                tvPct.setVisibility(View.GONE);
                pct = null;
                pctHablado = getString(R.string.envivo_primera_vez);
            } else {
                switch (f.variacion.tendencia) {
                    case SUBE:
                        pct = getString(R.string.resumen_pct_sube, f.variacion.porcentaje);
                        pctHablado = getString(R.string.resumen_pct_sube_a11y, f.variacion.porcentaje);
                        tvPct.setTextColor(ContextCompat.getColor(this, R.color.gp_success));
                        break;
                    case BAJA:
                        // Neutro, no rojo: bajar un día no es un fallo.
                        pct = getString(R.string.resumen_pct_baja, -f.variacion.porcentaje);
                        pctHablado = getString(R.string.resumen_pct_baja_a11y, -f.variacion.porcentaje);
                        tvPct.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
                        break;
                    default:
                        pct = getString(R.string.resumen_pct_igual);
                        pctHablado = getString(R.string.resumen_pct_igual_a11y);
                        tvPct.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
                }
                tvPct.setText(pct);
            }
            fila.setContentDescription(getString(R.string.resumen_fila_a11y, f.nombre, kilos, pctHablado));
            fila.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            fila.setFocusable(true);
            fila.findViewById(R.id.rayaUltimaVez).setVisibility(i == filas.size() - 1 ? View.GONE : View.VISIBLE);
            contenedor.addView(fila);
            // Momento 28: la comparación en cascada, detrás de los récords.
            Movimiento.entrarUna(fila, 700 + (long) i * Movimiento.CASCADA_ESCALON, Movimiento.CASCADA, 16);
        }
    }

    // El logro conseguido, con el medallón dorado. Sale solo con los logros nuevos que trae el
    // guardado (no los que ya se tenían). Su descripción llega después, de la lista de logros.
    private void pintarLogro() {
        MaterialCardView tarjeta = findViewById(R.id.cardLogroResumen);
        if (nuevosLogros.isEmpty()) {
            tarjeta.setVisibility(View.GONE);
            return;
        }
        tarjeta.setVisibility(View.VISIBLE);
        String nombre = nuevosLogros.get(0);
        TextView tvNombre = findViewById(R.id.tvLogroNombreResumen);
        TextView tvDetalle = findViewById(R.id.tvLogroDetalleResumen);
        tvNombre.setText(nombre);
        if (nuevosLogros.size() > 1) {
            int mas = nuevosLogros.size() - 1;
            tvDetalle.setText(getResources().getQuantityString(R.plurals.resumen_logro_mas, mas, mas));
            tvDetalle.setVisibility(View.VISIBLE);
        } else {
            tvDetalle.setVisibility(View.GONE);
        }
        findViewById(R.id.filaLogroResumen).setContentDescription(getString(R.string.resumen_logro_a11y, nombre));
        Movimiento.entrarUna(tarjeta, 900, Movimiento.ENTRA, 16);
        View medallon = findViewById(R.id.ivMedallonLogro);
        if (!Movimiento.quieto(this)) {
            medallon.setAlpha(0f);
            medallon.postDelayed(() -> {
                if (!isDestroyed()) {
                    Movimiento.saltar(medallon, Movimiento.TROFEO);
                    Movimiento.vibrar(medallon, Movimiento.Vibracion.EXITO);
                }
            }, 900);
        }
        if (nuevosLogros.size() == 1) pedirDescripcionDelLogro(nombre);
    }

    private void pedirDescripcionDelLogro(String nombre) {
        logroApi.getProgreso().enqueue(new ApiCallback<List<LogroProgreso>>() {
            @Override public void onOk(List<LogroProgreso> lista) {
                if (isDestroyed() || lista == null) return;
                for (LogroProgreso l : lista) {
                    if (nombre.equals(l.getNombre()) && l.getDescripcion() != null && !l.getDescripcion().isEmpty()) {
                        TextView tvDetalle = findViewById(R.id.tvLogroDetalleResumen);
                        tvDetalle.setText(l.getDescripcion());
                        tvDetalle.setVisibility(View.VISIBLE);
                        return;
                    }
                }
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: la descripción es un extra y la tarjeta ya dice el nombre
                // del logro, que es lo que importa. No hay nada que reintentar aquí.
            }
        });
    }

    // «Para recuperar» solo tiene sentido el día de la sesión: la proteína es la de hoy. Una
    // sesión vieja abierta desde el historial no la enseña.
    private void arrancarParaRecuperar() {
        View tarjeta = findViewById(R.id.cardParaRecuperar);
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String dia = sesion.getFechaInicio();
        if (dia == null || !dia.startsWith(hoy)) {
            tarjeta.setVisibility(View.GONE);
            return;
        }
        new ParaRecuperarSeccion(this, (MaterialCardView) tarjeta, prefsManager).cargar();
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private String formatoMiles(long valor) {
        return String.format(FechaUtils.localeDeLaApp(this), "%,d", valor);
    }

    private int color(int atributo) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(atributo, tv, true);
        return tv.data;
    }
}
