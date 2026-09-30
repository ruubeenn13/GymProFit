package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.envivo.CronometroSerie;
import es.pmdm.gymprofit.envivo.EmpezarSesion;
import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.ui.adapters.SesionEnVivoAdapter;
import es.pmdm.gymprofit.utils.InputDialog;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.Pauta;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.Valoracion;

// ============================================================
// SesionEnVivoActivity — la sesión en vivo (GP-012) con la última vez de cada ejercicio
// (GP-014).
//
// La pantalla no guarda nada: la sesión vive en SesionEnCursoRepositorio, que la escribe
// al fichero de la cuenta a cada cambio. Aquí solo se pinta lo que hay y se le pasa lo
// que toca el usuario. Por eso minimizar es cerrar la pantalla: la sesión sigue, con su
// reloj, en la barra de la app y en la notificación.
//
// El reloj se pinta cada segundo desde el inicio guardado, así que no se para aunque
// Android cierre la app: al volver, cuenta desde cuando se tocó «Empezar». El
// cronómetro de las series por tiempo, igual.
//
// Desde el primer intento de guardar, la sesión no se edita (lo decide el repositorio):
// la lista se vuelve de solo lectura y arriba sale el estado del guardado, con
// «Reintentar» y «Descartar la sesión» si falló.
// ============================================================
public class SesionEnVivoActivity extends AppCompatActivity implements SesionEnVivoAdapter.Acciones {

    // Aplica la escala de fuente global de la app.
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private SesionEnCursoRepositorio repo;
    private SesionEnVivoAdapter adapter;
    private MaterialToolbar toolbar;
    private TextView tvTiempo, tvSeries, tvEjercicios;
    private View filaResumen, cardGuardado, btnTerminar;
    private LinearProgressIndicator barraProgreso;
    private RecyclerView rv;

    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable tic = new Runnable() {
        @Override public void run() {
            pintarReloj();
            comprobarCronometro();
            reloj.postDelayed(this, 1000);
        }
    };

    @Nullable private BottomSheetDialog hojaCrono;
    @Nullable private TextView tvCronoTiempo;
    @Nullable private LinearProgressIndicator barraCrono;
    @Nullable private BottomSheetDialog hojaTerminar;
    // Se abrió el resumen: cuando la sesión desaparezca no hay que abrir Inicio.
    private boolean guardada;

    private ActivityResultLauncher<Intent> anadirEjercicios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_sesion_en_vivo);

        repo = SesionEnCursoRepositorio.get(this);
        repo.usarCuenta(prefs.getUsuarioId());
        if (!repo.hay()) {
            // Se llegó por una notificación vieja o la sesión ya se guardó.
            salir();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> salir());
        tvTiempo = findViewById(R.id.tvTiempo);
        tvSeries = findViewById(R.id.tvSeries);
        tvEjercicios = findViewById(R.id.tvEjercicios);
        filaResumen = findViewById(R.id.filaResumen);
        barraProgreso = findViewById(R.id.barraProgreso);
        for (int id : new int[]{R.id.tvRotuloTiempo, R.id.tvRotuloSeries, R.id.tvRotuloEjercicios}) {
            es.pmdm.gymprofit.utils.Rotulos.limitar(findViewById(id));
        }
        cardGuardado = findViewById(R.id.cardGuardado);
        btnTerminar = findViewById(R.id.btnTerminar);
        btnTerminar.setOnClickListener(v -> abrirTerminar());
        findViewById(R.id.btnReintentar).setOnClickListener(v -> repo.reintentar());
        findViewById(R.id.btnDescartarGuardado).setOnClickListener(v -> confirmarDescartar());

        // Atrás es minimizar: la sesión sigue.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { salir(); }
        });

        rv = findViewById(R.id.rvSesion);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SesionEnVivoAdapter(this, repo, this);
        adapter.setQuitar(this::quitarSerie);
        rv.setAdapter(adapter);
        // Las filas no parpadean al marcar: el cambio es el tinte, no un fundido.
        if (rv.getItemAnimator() instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        instalarDeslizar();

        anadirEjercicios = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> {
                    if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                        repo.anadirEjercicios(leerAnadidos(r.getData().getStringExtra("ejerciciosJson")));
                    }
                });

        repo.getSesion().observe(this, this::pintar);
        repo.getResultado().observe(this, evento -> {
            SesionEnCursoRepositorio.Resultado r = evento.tomar();
            if (r == null) return;
            if (r.ok()) irAlResumen(r.sesion, r.guardada);
            // El fallo lo pinta el estado de la sesión, en la tarjeta de arriba.
        });

        // Lo que falte por pedir: los ejercicios de la rutina, si Android cerró la app
        // mientras llegaban, y la última vez de los que aún no la tienen.
        repo.cargarEjercicios();
        repo.pedirUltimaVez();
    }

    @Override
    protected void onStart() {
        super.onStart();
        reloj.post(tic);
    }

    @Override
    protected void onStop() {
        reloj.removeCallbacks(tic);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (hojaCrono != null) hojaCrono.dismiss();
        if (hojaTerminar != null) hojaTerminar.dismiss();
        super.onDestroy();
    }

    // Minimizar. Si la sesión se abrió desde la notificación con la app cerrada, no hay
    // nada detrás: se abre Inicio para no salir de la app.
    private void salir() {
        if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ── Pintar ───────────────────────────────────────────────

    private void pintar(@Nullable SesionEnCurso s) {
        if (s == null) {
            // Se descartó (o se guardó y el resumen ya está abierto).
            if (!guardada) salir();
            return;
        }
        toolbar.setTitle(EmpezarSesion.nombre(this, s.rutinaNombre));
        toolbar.setSubtitle(s.programaNombre);

        LogicaSesion.Totales t = LogicaSesion.totales(s);
        tvSeries.setText(getString(R.string.envivo_de, t.seriesHechas, t.seriesTotales));
        tvEjercicios.setText(getString(R.string.envivo_de, t.ejerciciosHechos, t.ejerciciosTotales));
        barraProgreso.setProgress(t.seriesTotales == 0 ? 0 : Math.round(100f * t.seriesHechas / t.seriesTotales));
        pintarReloj();

        adapter.pintar(s);
        pintarGuardado(s);

        // Mientras corre un cronómetro la pantalla no se apaga; y su hoja se enseña
        // también al volver a la pantalla (sigue contando desde que empezó).
        if (s.cronometro != null) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mostrarCronometro(s);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (hojaCrono != null) {
                hojaCrono.dismiss();
                hojaCrono = null;
            }
        }
    }

    private void pintarReloj() {
        SesionEnCurso s = repo.actual();
        if (s == null) return;
        tvTiempo.setText(LogicaSesion.reloj(s.inicioMs, repo.ahora()));
        // TalkBack oye los minutos, no cada segundo: cambiar la descripción a cada tic
        // es un aviso de cambio por segundo que nadie quiere oír.
        LogicaSesion.Totales t = LogicaSesion.totales(s);
        String minutos = getString(R.string.duracion_min, LogicaSesion.minutosReloj(s.inicioMs, repo.ahora()));
        String descripcion = getString(R.string.envivo_resumen_a11y, minutos,
                t.seriesHechas, t.seriesTotales, t.ejerciciosHechos, t.ejerciciosTotales);
        if (!descripcion.contentEquals(filaResumen.getContentDescription() != null
                ? filaResumen.getContentDescription() : "")) {
            filaResumen.setContentDescription(descripcion);
        }
    }

    private void pintarGuardado(SesionEnCurso s) {
        boolean intentado = s.claveIdempotencia != null;
        cardGuardado.setVisibility(intentado ? View.VISIBLE : View.GONE);
        btnTerminar.setVisibility(intentado ? View.GONE : View.VISIBLE);
        if (!intentado) return;
        boolean guardando = s.guardado == SesionEnCurso.Guardado.GUARDANDO;
        TextView titulo = findViewById(R.id.tvGuardadoTitulo);
        titulo.setText(guardando ? R.string.envivo_guardando : R.string.envivo_error_titulo);
        findViewById(R.id.progresoGuardado).setVisibility(guardando ? View.VISIBLE : View.GONE);
        findViewById(R.id.ivGuardado).setVisibility(guardando ? View.GONE : View.VISIBLE);
        TextView texto = findViewById(R.id.tvGuardadoTexto);
        String motivo = s.codigoFallo > 0 ? UiFeedback.mensaje(this, s.codigoFallo, s.mensajeFallo) : null;
        texto.setText(motivo == null ? getString(R.string.envivo_error_texto)
                : motivo + "\n" + getString(R.string.envivo_error_texto));
        texto.setVisibility(guardando ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnReintentar).setVisibility(guardando ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnDescartarGuardado).setVisibility(guardando ? View.GONE : View.VISIBLE);
    }

    // ── Lista ────────────────────────────────────────────────

    @Override
    public void marcar(long serieId, int numero) {
        LogicaSesion.Marcado m = repo.marcar(serieId, getResources().getConfiguration().getLocales().get(0));
        if (m == null) return;
        switch (m) {
            case FALTAN_REPS: avisar(R.string.envivo_falta_reps); break;
            case FALTAN_SEGUNDOS: avisar(R.string.envivo_falta_segundos); break;
            case NO_VALIDO: avisar(R.string.envivo_no_valido); break;
            default: break;
        }
    }

    private void avisar(int texto) {
        Snackbar.make(rv, texto, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void anadirSerie(long ejercicioId) {
        repo.anadirSerie(ejercicioId);
    }

    @Override
    public void menuEjercicio(View ancla, long ejercicioId, String nombre) {
        List<UIHelper.MenuAction> acciones = new ArrayList<>();
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, getString(R.string.accion_quitar_ejercicio), true,
                () -> repo.quitarEjercicio(ejercicioId)));
        UIHelper.mostrarMenuAnclado(this, ancla, nombre, acciones);
    }

    // Quitar una serie deslizándola, a cualquier lado. Solo las series, y solo mientras
    // la sesión se puede editar. TalkBack lo hace desde las acciones de la fila.
    private void instalarDeslizar() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public int getSwipeDirs(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h) {
                return adapter.esSerieQuitable(h.getBindingAdapterPosition()) ? super.getSwipeDirs(r, h) : 0;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder a,
                                  @NonNull RecyclerView.ViewHolder b) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder h, int dir) {
                quitarSerie(h.getBindingAdapterPosition());
            }
        }).attachToRecyclerView(rv);
    }

    private void quitarSerie(int posicion) {
        if (!adapter.esSerieQuitable(posicion)) return;
        SesionEnCursoRepositorio.Quitada q = repo.quitarSerieDeshacible(adapter.idDe(posicion));
        if (q == null) {
            adapter.pintar(repo.actual());
            return;
        }
        Snackbar.make(rv, R.string.envivo_serie_quitada, Snackbar.LENGTH_LONG)
                .setAction(R.string.envivo_deshacer, v -> repo.reponer(q))
                .show();
    }

    @Override
    public void anadirEjercicio() {
        Intent i = new Intent(this, AnadirEjerciciosActivity.class);
        i.putExtra("editMode", true);
        i.putExtra(AnadirEjerciciosActivity.EXTRA_MODO_SESION, true);
        anadirEjercicios.launch(i);
    }

    // Lo que devuelve la biblioteca: [{ejercicioId, nombre, …}].
    private static List<SesionEnCursoRepositorio.Nuevo> leerAnadidos(@Nullable String json) {
        List<SesionEnCursoRepositorio.Nuevo> nuevos = new ArrayList<>();
        if (json == null) return nuevos;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                nuevos.add(new SesionEnCursoRepositorio.Nuevo(o.getInt("ejercicioId"), o.optString("nombre", null)));
            }
        } catch (JSONException e) {
            // La biblioteca lo escribe siempre bien; si no, no se añade nada y la sesión
            // sigue como estaba, que es lo seguro.
            return Collections.emptyList();
        }
        return nuevos;
    }

    @Override
    public void descartar() {
        confirmarDescartar();
    }

    private void confirmarDescartar() {
        UIHelper.mostrarDialogoConIcono(this, getString(R.string.envivo_descartar),
                getString(R.string.envivo_descartar_confirmar), R.drawable.ic_ms_delete,
                getString(R.string.envivo_descartar_si), null, () -> {
                    if (hojaTerminar != null) hojaTerminar.dismiss();
                    repo.descartar();
                });
    }

    @Override
    public void reintentarCarga() {
        repo.cargarEjercicios();
    }

    // ── Cronómetro ───────────────────────────────────────────

    @Override
    public void cronometro(long serieId) {
        repo.empezarCronometro(serieId);
    }

    private void mostrarCronometro(SesionEnCurso s) {
        SesionEnCurso.Ejercicio e = SesionEnCursoRepositorio.ejercicioDeSerie(s, s.cronometro.serieId);
        if (e == null) return;
        if (hojaCrono == null) {
            hojaCrono = new BottomSheetDialog(this);
            View v = getLayoutInflater().inflate(R.layout.dialog_cronometro, null, false);
            tvCronoTiempo = v.findViewById(R.id.tvCronoTiempo);
            barraCrono = v.findViewById(R.id.barraCrono);
            v.findViewById(R.id.btnParar).setOnClickListener(b -> repo.pararYApuntar());
            v.findViewById(R.id.btnCancelarCrono).setOnClickListener(b -> repo.cancelarCronometro());
            hojaCrono.setContentView(v);
            // Solo se cierra con sus dos botones: un toque fuera no puede tirar la serie.
            hojaCrono.setCancelable(false);
            hojaCrono.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            hojaCrono.show();
        }
        int numero = e.series.indexOf(serieDe(e, s.cronometro.serieId)) + 1;
        String nombre = e.nombre != null ? e.nombre : getString(R.string.ejercicio_sin_nombre, e.ejercicioId);
        ((TextView) hojaCrono.findViewById(R.id.tvCronoTitulo))
                .setText(getString(R.string.envivo_crono_titulo, numero, nombre));
        Integer min = LogicaSesion.minimoPauta(e), max = LogicaSesion.maximoPauta(e);
        TextView pauta = hojaCrono.findViewById(R.id.tvCronoPauta);
        if (min == null) {
            pauta.setText(R.string.envivo_crono_sin_pauta);
        } else {
            String cantidad = Pauta.cantidad(Pauta.Formatos.de(this), min, max, min, "SEGUNDOS");
            pauta.setText(getString(R.string.envivo_crono_pauta, Pauta.lado(Pauta.Formatos.de(this), cantidad, e.porLado)));
        }
        pintarCronometro();
    }

    @Nullable
    private static SesionEnCurso.Serie serieDe(SesionEnCurso.Ejercicio e, long id) {
        for (SesionEnCurso.Serie s : e.series) if (s.id == id) return s;
        return null;
    }

    private void pintarCronometro() {
        SesionEnCurso s = repo.actual();
        if (s == null || s.cronometro == null || tvCronoTiempo == null || barraCrono == null) return;
        int seg = CronometroSerie.segundos(s.cronometro, repo.ahora());
        tvCronoTiempo.setText(LogicaSesion.tiempo(seg));
        SesionEnCurso.Ejercicio e = SesionEnCursoRepositorio.ejercicioDeSerie(s, s.cronometro.serieId);
        Integer max = e != null ? LogicaSesion.maximoPauta(e) : null;
        barraCrono.setVisibility(max == null ? View.GONE : View.VISIBLE);
        if (max != null) barraCrono.setProgress(Math.min(100, Math.round(100f * seg / max)));
    }

    private void comprobarCronometro() {
        CronometroSerie.Aviso a = repo.comprobarCronometro();
        switch (a) {
            case VIBRAR_CORTA: vibrar(new long[]{0, 180}); break;
            case VIBRAR_DOBLE: vibrar(new long[]{0, 180, 140, 180}); break;
            case PARAR: avisar(R.string.envivo_crono_parado); break;
            default: break;
        }
        pintarCronometro();
    }

    private void vibrar(long[] patron) {
        Vibrator v;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = getSystemService(VibratorManager.class);
            v = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(patron, -1));
        } else {
            v.vibrate(patron, -1);
        }
    }

    // ── Terminar ─────────────────────────────────────────────

    private void abrirTerminar() {
        SesionEnCurso s = repo.actual();
        if (s == null || !repo.editable()) return;
        if (hojaTerminar != null && hojaTerminar.isShowing()) return;
        hojaTerminar = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_terminar_sesion, null, false);
        hojaTerminar.setContentView(v);
        hojaTerminar.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        hojaTerminar.getBehavior().setSkipCollapsed(true);

        LogicaSesion.Totales t = LogicaSesion.totales(s);
        boolean hayAlgo = t.seriesHechas > 0;

        pintarDuracion(v);
        v.findViewById(R.id.btnAjustar).setOnClickListener(b -> ajustarDuracion(v));

        ((TextView) v.findViewById(R.id.tvSeriesHechas)).setText(getResources().getQuantityString(
                R.plurals.envivo_series_hechas_de, t.seriesTotales, t.seriesHechas, t.seriesTotales));
        v.findViewById(R.id.grupoSinMarcar).setVisibility(hayAlgo && t.sinMarcar() > 0 ? View.VISIBLE : View.GONE);
        ((TextView) v.findViewById(R.id.tvSinMarcar)).setText(getResources().getQuantityString(
                R.plurals.envivo_sin_marcar, t.sinMarcar(), t.sinMarcar()));
        v.findViewById(R.id.btnRevisar).setOnClickListener(b -> {
            hojaTerminar.dismiss();
            int pos = adapter.primeraSinMarcar();
            if (pos >= 0) rv.smoothScrollToPosition(pos);
        });

        v.findViewById(R.id.tvNadaMarcado).setVisibility(hayAlgo ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.grupoGuardar).setVisibility(hayAlgo ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btnDescartarHoja).setVisibility(hayAlgo ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.btnDescartarHoja).setOnClickListener(b -> confirmarDescartar());
        v.findViewById(R.id.btnSeguir).setOnClickListener(b -> hojaTerminar.dismiss());

        configurarValoracion(v, s);
        EditText etNotas = v.findViewById(R.id.etNotas);
        etNotas.setText(s.notas);
        etNotas.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) { repo.escribirNotas(e.toString()); }
        });

        v.findViewById(R.id.btnGuardar).setOnClickListener(b -> {
            if (repo.guardar() == SesionEnCursoRepositorio.Guardar.ENVIADO) {
                hojaTerminar.dismiss();
                rv.scrollToPosition(0);
            }
        });
        hojaTerminar.show();
    }

    // «43 min · desde las 18:02», con la hora en el formato del móvil.
    private void pintarDuracion(View hoja) {
        SesionEnCurso s = repo.actual();
        if (s == null) return;
        String hora = DateFormat.getTimeFormat(this).format(new Date(s.inicioMs));
        String texto = getString(R.string.envivo_duracion_valor, repo.minutosPropuestos(), hora);
        ((TextView) hoja.findViewById(R.id.tvDuracion)).setText(texto);
        hoja.findViewById(R.id.grupoDuracion).setContentDescription(getString(R.string.envivo_duracion) + ": " + texto);
    }

    private void ajustarDuracion(View hoja) {
        InputDialog.entero(this, getString(R.string.envivo_ajustar_titulo), getString(R.string.envivo_ajustar_rango),
                String.valueOf(repo.minutosPropuestos()), valor -> {
                    Integer min = Numeros.entero(valor, 1, LogicaSesion.MAX_MINUTOS);
                    if (min == null) {
                        UIHelper.mostrarToastError(this, getString(R.string.sesiones_duracion_invalida));
                        return;
                    }
                    repo.ajustarDuracion(min);
                    pintarDuracion(hoja);
                });
    }

    // Opcional y arranca sin valorar (GP-077), como en Registrar sesión.
    private void configurarValoracion(View hoja, SesionEnCurso s) {
        RatingBar rating = hoja.findViewById(R.id.ratingBar);
        TextView estado = hoja.findViewById(R.id.tvEstadoValoracion);
        View quitar = hoja.findViewById(R.id.btnQuitarValoracion);
        rating.setRating(s.valoracion != null ? s.valoracion : 0f);
        Runnable pintarValoracion = () -> {
            Integer valor = Valoracion.paraEnviar(rating.getRating());
            if (valor == null) {
                estado.setText(R.string.sesiones_sin_valorar);
                rating.setContentDescription(getString(R.string.sesiones_valoracion_desc_sin));
                ViewCompat.setStateDescription(rating, getString(R.string.sesiones_sin_valorar));
                quitar.setVisibility(View.INVISIBLE);
            } else {
                estado.setText(getString(R.string.sesiones_valoracion_estado, valor));
                rating.setContentDescription(getString(R.string.sesiones_valoracion_desc));
                ViewCompat.setStateDescription(rating, getString(R.string.sesiones_valoracion_a11y, valor));
                quitar.setVisibility(View.VISIBLE);
            }
        };
        rating.setOnRatingBarChangeListener((bar, estrellas, delUsuario) -> {
            repo.valorar(Valoracion.paraEnviar(estrellas));
            pintarValoracion.run();
        });
        quitar.setOnClickListener(b -> {
            rating.setRating(0f);
            rating.requestFocus();
        });
        pintarValoracion.run();
    }

    // ── Guardada ─────────────────────────────────────────────

    // El resumen de siempre, como tras Registrar sesión.
    private void irAlResumen(SesionEntrenamiento sesion, SesionEnCurso guardadaEnCurso) {
        guardada = true;
        UIHelper.mostrarToastExito(this, getString(R.string.sesiones_exito));
        ArrayList<String> nuevosLogros = new ArrayList<>();
        if (sesion.getNuevosLogros() != null) nuevosLogros.addAll(sesion.getNuevosLogros());
        Intent intent = new Intent(this, ResumenSesionActivity.class);
        intent.putExtra("sesionId", sesion.getId());
        intent.putExtra("rutinaNombre", guardadaEnCurso.rutinaNombre != null ? guardadaEnCurso.rutinaNombre : "");
        intent.putExtra(ResumenSesionActivity.EXTRA_ENTRENAMIENTO_LIBRE, guardadaEnCurso.rutinaId == null);
        intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
        intent.putExtra(ResumenSesionActivity.EXTRA_RECORDS, new ArrayList<>(sesion.getRecordsBatidos()));
        intent.putExtra(ResumenSesionActivity.EXTRA_PRIMERAS_MARCAS, sesion.getPrimerasMarcas().size());
        if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
        startActivity(intent);
        finish();
    }
}
