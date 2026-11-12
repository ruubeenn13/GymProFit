package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.envivo.CabeceraEntreno;
import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.RegistroSesionRepositorioApi;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.Carga;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.EstadoGuardado;
import es.pmdm.gymprofit.ui.widget.ElegirRutinaHoja;
import es.pmdm.gymprofit.ui.widget.HojaDuracion;
import es.pmdm.gymprofit.ui.widget.HojaElegir;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.Caras;
import es.pmdm.gymprofit.utils.CifraTarjeta;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.MarcaSerie;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// RegistrarSesionActivity — «Apuntar entreno» (GP-241; antes Registrar sesión, GP-012), desde el
// «+»: un entrenamiento ya hecho. Diseño en documentacion/diseno/2026-11-06-entreno-y-objetivo/
// (tablero Entreno-B).
//
//   · La cabecera, en la tarjeta protagonista: la fecha (Hoy de entrada; Hoy, Ayer u Otro día,
//     nunca futuro), la rutina que elige HoyToca con su porqué y «Cambiar» (la hoja «¿Qué
//     entrenaste?»), tres cuadros que suman al marcar (duración, kilos y series), la última vez
//     de esa rutina en cascada y el récord en dorado en cuanto una serie marcada lo bate.
//   · Los ejercicios, con las filas de la sesión en vivo (item_envivo_serie), la última vez de
//     pista y «Todo como la última vez». Los que no se han empezado, plegados.
//   · «¿Qué tal fue?», cinco caras de 1 a 5 (sin tocar, NULL), y la nota tras el lápiz.
//   · El pie fijo: lo que se va a guardar, en una frase, y «Guardar entreno».
//
// GUARDADO (GP-006). Todo va en POST /sesiones/completa, la pantalla NO se cierra hasta recibir el
// éxito y, si falla, se ofrece reintentar con lo marcado intacto y la misma clave.
//
// BORRADOR (GP-016). La pantalla no guarda nada: el borrador, la clave y el guardado en marcha
// viven en RegistrarSesionViewModel, que los escribe en su SavedStateHandle. Girar, «No conservar
// actividades» o que Android mate la app no pierden nada. Aquí solo se pinta ese estado y se le
// pasa cada cambio; la red tampoco se llama desde aquí, sino desde el repositorio.
// ============================================================
public class RegistrarSesionActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    /**
     * Id de la rutina con la que se ha entrado, o -1 si se abrió sin rutina (desde el «+»: entonces
     * se pone la que toca hoy). Solo cuenta la primera vez: al restaurar manda lo que eligió el usuario.
     */
    public static final String EXTRA_RUTINA_ID = "rutinaId";

    private RegistrarSesionViewModel vm;
    private Locale idioma;
    private CabeceraEntreno.Textos textos;
    private LogicaSesion.Textos textosSesion;

    private TextView tvFecha, tvRutina, tvPorque, tvDuracion, tvKilos, tvSeries, tvUltimaVez, tvRecordHoy, tvPie,
            tvCaraElegida;
    private View btnFecha, cuadroDuracion, cuadroKilos, cuadroSeries, btnCambiarRutina, btnComoUltima,
            btnAnadirEjercicios, tilNota;
    private ImageButton btnNota;
    private EditText etNota;
    private LinearLayout listaEjercicios, grupoCaras;
    private VistaEstado estadoEjercicios;
    private final List<ImageButton> caras = new ArrayList<>();

    // Las filas de serie que hay en pantalla, por id de serie, y la forma de la lista: si cambia
    // (otra rutina, plegar, añadir) se rehace; si no, cada fila se repinta en su sitio.
    private final Map<Long, FilaSerie> filas = new HashMap<>();
    @Nullable private String estructura;
    // Las cifras que se ven, para contar desde ellas (Movimiento.contar) y no desde cero.
    private long kilosVistos = -1;
    private int seriesVistas = -1;
    private boolean recordVisto;
    // Las series que eran récord en el último repintado: la que lo pasa a ser ahora se dora.
    private Set<Long> seriesRecord = new HashSet<>();
    // Si ya se pintó una vez: hasta entonces nada se anima como recién conseguido.
    private boolean pintado;
    // El aviso de fallo abierto, para no apilar otro y cerrarlo al destruir la pantalla.
    private Dialog avisoFallo;
    private ActivityResultLauncher<Intent> anadirEjercicios;

    // Inicializa la pantalla: monta las vistas, crea (o recupera) el ViewModel y se suscribe a sus
    // cambios.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefsManager = new PreferencesManager(this);
        setContentView(R.layout.activity_registrar_sesion);
        idioma = FechaUtils.localeDeLaApp(this);
        textos = textos();
        textosSesion = textosSesion();

        tvFecha = findViewById(R.id.tvFecha);
        tvRutina = findViewById(R.id.tvRutina);
        tvPorque = findViewById(R.id.tvPorque);
        tvDuracion = findViewById(R.id.tvDuracion);
        tvKilos = findViewById(R.id.tvKilos);
        tvSeries = findViewById(R.id.tvSeries);
        tvUltimaVez = findViewById(R.id.tvUltimaVez);
        tvRecordHoy = findViewById(R.id.tvRecordHoy);
        tvPie = findViewById(R.id.tvPie);
        tvCaraElegida = findViewById(R.id.tvCaraElegida);
        btnFecha = findViewById(R.id.btnFecha);
        cuadroDuracion = findViewById(R.id.cuadroDuracion);
        cuadroKilos = findViewById(R.id.cuadroKilos);
        cuadroSeries = findViewById(R.id.cuadroSeries);
        btnCambiarRutina = findViewById(R.id.btnCambiarRutina);
        btnComoUltima = findViewById(R.id.btnComoUltima);
        btnAnadirEjercicios = findViewById(R.id.btnAnadirEjercicios);
        tilNota = findViewById(R.id.tilNota);
        btnNota = findViewById(R.id.btnNota);
        etNota = findViewById(R.id.etNota);
        listaEjercicios = findViewById(R.id.listaEjercicios);
        grupoCaras = findViewById(R.id.grupoCaras);
        estadoEjercicios = new VistaEstado(findViewById(R.id.estadoEjercicios));

        int usuarioId = prefsManager.getUsuarioId();
        vm = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory(this, null) {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                                                     @NonNull SavedStateHandle handle) {
                return (T) new RegistrarSesionViewModel(handle, new RegistroSesionRepositorioApi(usuarioId));
            }
        }).get(RegistrarSesionViewModel.class);

        int rutinaDeEntrada = getIntent().getIntExtra(EXTRA_RUTINA_ID, -1);
        vm.iniciar(rutinaDeEntrada != -1 ? rutinaDeEntrada : null);

        // Salir con algo apuntado pregunta antes de tirarlo (GP-098).
        AvisoDescartar.instalar(this, findViewById(R.id.toolbar), vm::hayDatos);

        anadirEjercicios = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
            vm.anadirEjercicios(leerAnadidos(r.getData().getStringExtra("ejerciciosJson")));
        });

        btnFecha.setOnClickListener(v -> elegirFecha());
        btnCambiarRutina.setOnClickListener(v -> elegirRutina());
        cuadroDuracion.setOnClickListener(v -> HojaDuracion.mostrar(this, vm::getDuracion, vm::sumarDuracion,
                RegistrarSesionViewModel.DURACION_PASO, RegistrarSesionViewModel.DURACION_MIN,
                RegistrarSesionViewModel.DURACION_MAX));
        btnComoUltima.setOnClickListener(v -> {
            Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
            vm.comoLaUltimaVez(idioma);
        });
        btnAnadirEjercicios.setOnClickListener(v -> {
            Intent i = new Intent(this, AnadirEjerciciosActivity.class);
            i.putExtra("editMode", true);
            i.putExtra(AnadirEjerciciosActivity.EXTRA_MODO_SESION, true);
            anadirEjercicios.launch(i);
        });
        findViewById(R.id.btnGuardar).setOnClickListener(v -> vm.guardar());
        // Con el teclado abierto el pie se aparta: le quitaba media pantalla a las filas que se
        // están escribiendo, y un toque que iba a una fila caía en «Guardar entreno».
        View pie = findViewById(R.id.piePantalla);
        View raiz = findViewById(android.R.id.content);
        raiz.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            androidx.core.view.WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(raiz);
            boolean teclado = insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime());
            int visible = teclado ? View.GONE : View.VISIBLE;
            if (pie.getVisibility() != visible) pie.setVisibility(visible);
        });
        ((es.pmdm.gymprofit.ui.widget.FilaQueBaja) findViewById(R.id.filaEjercicios)).setCentrados(true);
        montarCaras();
        montarNota();

        vm.getCambios().observe(this, n -> pintar());
        vm.getDatos().observe(this, c -> pintar());
        vm.getCargaEjercicios().observe(this, c -> pintar());
        vm.getEstadoGuardado().observe(this, this::pintarGuardado);
    }

    // ── Pintar ────────────────────────────────────────────────

    private void pintar() {
        pintarCabecera();
        pintarEjercicios();
        pintarCaras();
        CabeceraEntreno c = vm.cabecera();
        tvPie.setText(c.pie(textos, idioma));
    }

    private void pintarCabecera() {
        String fecha = textoFecha(vm.getFecha());
        tvFecha.setText(fecha);
        btnFecha.setContentDescription(getString(R.string.apuntar_fecha_a11y, fecha));

        boolean decidida = vm.rutinaDecidida();
        Integer rutinaId = vm.getRutinaId();
        String nombre = rutinaId == null ? getString(R.string.apuntar_libre) : vm.nombreRutina();
        tvRutina.setVisibility(decidida ? View.VISIBLE : View.INVISIBLE);
        tvRutina.setText(nombre != null ? nombre : "");
        btnCambiarRutina.setVisibility(decidida && vm.hayRutinas() ? View.VISIBLE : View.GONE);
        btnCambiarRutina.setContentDescription(getString(R.string.apuntar_cambiar_rutina_a11y, nombre != null ? nombre : ""));

        String porque;
        if (!decidida) porque = getString(R.string.apuntar_cargando);
        else if (rutinaId == null) porque = getString(R.string.apuntar_libre_sub);
        else {
            switch (vm.porque()) {
                case PROGRAMA: porque = getString(R.string.apuntar_porque_programa); break;
                case MAS_TIEMPO: porque = getString(R.string.apuntar_porque_mas_tiempo); break;
                default: porque = null;
            }
        }
        tvPorque.setText(porque);
        tvPorque.setVisibility(porque == null ? View.GONE : View.VISIBLE);

        CabeceraEntreno c = vm.cabecera();
        int minutos = vm.getDuracion();
        tvDuracion.setText(conUnidad(String.valueOf(minutos), getString(R.string.apuntar_unidad_min)));
        cuadroDuracion.setContentDescription(getResources().getQuantityString(R.plurals.apuntar_duracion_a11y, minutos, minutos));

        // Sin peso, ni en la última vez ni en lo marcado, no hay cuadro de kilos: los otros dos se
        // reparten el ancho (DEC-060: ningún «0 kg» fijo).
        cuadroKilos.setVisibility(c.cuadroKilos ? View.VISIBLE : View.GONE);
        long kilos = Math.round(c.kilos);
        contar(tvKilos, kilosVistos, kilos, n -> conUnidad(CifraTarjeta.numero(n, idioma), getString(R.string.apuntar_unidad_kg)));
        kilosVistos = kilos;
        String kilosTexto = CifraTarjeta.numero(kilos, idioma);
        cuadroKilos.setContentDescription(getResources().getQuantityString(R.plurals.apuntar_volumen_a11y,
                (int) Math.min(Integer.MAX_VALUE, kilos), kilosTexto));

        String de = c.conRutina ? getString(R.string.apuntar_unidad_de, c.previstas) : "";
        int marcadas = c.marcadas;
        contar(tvSeries, seriesVistas, marcadas, n -> conUnidad(String.valueOf(n), de));
        seriesVistas = marcadas;
        cuadroSeries.setContentDescription(c.conRutina
                ? getResources().getQuantityString(R.plurals.apuntar_series_de_a11y, marcadas, marcadas, c.previstas)
                : getString(R.string.apuntar_series) + ", "
                        + getResources().getQuantityString(R.plurals.apuntar_series_a11y, marcadas, marcadas));

        String ultima = CabeceraEntreno.ultimaVez(vm.ultimaVez(), textos, idioma);
        tvUltimaVez.setText(ultima);
        tvUltimaVez.setVisibility(ultima == null ? View.GONE : View.VISIBLE);

        List<CabeceraEntreno.RecordHoy> records = CabeceraEntreno.records(vm.getBorrador().ejercicios, textos);
        String record = CabeceraEntreno.frase(records, textos, idioma);
        tvRecordHoy.setText(record);
        boolean hayRecord = record != null;
        tvRecordHoy.setVisibility(hayRecord ? View.VISIBLE : View.GONE);
        if (hayRecord && !recordVisto && pintado) Movimiento.entrar(0, tvRecordHoy);
        recordVisto = hayRecord;
    }

    // «55 min», «2.740 kg», «5 de 21»: la cifra en condensada y lo de detrás en Barlow pequeño.
    private CharSequence conUnidad(String cifra, String unidad) {
        SpannableString s = new SpannableString(cifra + unidad);
        if (unidad.isEmpty()) return s;
        int desde = cifra.length();
        s.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_caption)), desde, s.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.gp_on_hero_variant)), desde, s.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Typeface barlow = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.barlow);
        if (barlow != null) s.setSpan(new Fuente(barlow), desde, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private interface Cifra { CharSequence de(long n); }

    // La cifra cuenta desde la que se veía hasta la nueva (Movimiento.contar); la primera vez y con
    // «Quitar animaciones», se pone sin contar.
    private void contar(TextView tv, long antes, long ahora, Cifra cifra) {
        // Una cuenta en marcha se para antes: si no, al acabar pisaría con su texto viejo (el
        // «0 de 0» de antes de llegar los ejercicios) lo que se pinta ahora.
        Object enMarcha = tv.getTag(R.id.tvSeries);
        if (enMarcha instanceof android.animation.ValueAnimator) ((android.animation.ValueAnimator) enMarcha).cancel();
        tv.setTag(R.id.tvSeries, null);
        if (antes < 0 || antes == ahora || Movimiento.quieto(this)) {
            tv.setText(cifra.de(ahora));
            return;
        }
        tv.setTag(R.id.tvSeries, Movimiento.contar(0, Movimiento.CIFRA,
                f -> tv.setText(cifra.de(Math.round(antes + (ahora - antes) * f)))));
    }

    // ── Los ejercicios ────────────────────────────────────────

    private void pintarEjercicios() {
        Carga datos = vm.getDatos().getValue();
        Carga ejercicios = vm.getCargaEjercicios().getValue();
        SesionEnCurso s = vm.getBorrador();
        boolean libre = vm.rutinaDecidida() && vm.getRutinaId() == null;

        if (datos == Carga.ERROR) {
            estadoEjercicios.error(getString(R.string.apuntar_error_rutinas), vm::reintentar);
        } else if (!vm.rutinaDecidida() || ejercicios == Carga.CARGANDO) {
            estadoEjercicios.cargando();
        } else if (ejercicios == Carga.ERROR) {
            estadoEjercicios.error(getString(R.string.apuntar_error_ejercicios), vm::reintentar);
        } else if (libre && s.ejercicios.isEmpty()) {
            estadoEjercicios.vacio(R.drawable.ic_ms_fitness_center, R.string.apuntar_libre_vacio);
        } else {
            estadoEjercicios.oculto();
        }
        btnAnadirEjercicios.setVisibility(libre && datos != Carga.ERROR ? View.VISIBLE : View.GONE);
        btnComoUltima.setVisibility(vm.hayUltimaVez() ? View.VISIBLE
                : vm.tuvoUltimaVez() ? View.INVISIBLE : View.GONE);

        String forma = forma(s);
        if (!forma.equals(estructura)) {
            estructura = forma;
            rehacerLista(s);
        }
        Set<Long> recordAhora = new HashSet<>();
        for (CabeceraEntreno.RecordHoy r : CabeceraEntreno.records(s.ejercicios, textos)) recordAhora.add(r.serieId);
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            for (int i = 0; i < e.series.size(); i++) {
                FilaSerie f = filas.get(e.series.get(i).id);
                // Al entrar (o tras girar) los récords que ya había no se celebran otra vez.
                if (f != null) f.pintar(e, i, recordAhora.contains(f.serieId), pintado && !seriesRecord.contains(f.serieId));
            }
        }
        seriesRecord = recordAhora;
        pintado = true;
    }

    // La forma de la lista: qué ejercicios, abiertos o plegados, y qué series tiene cada uno.
    private String forma(SesionEnCurso s) {
        StringBuilder sb = new StringBuilder();
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            sb.append(e.id).append(vm.abierto(e) ? 'A' : 'P').append(RegistrarSesionViewModel.empezado(e) ? 'E' : '-')
                    .append(LogicaSesion.porTiempo(e) ? 'T' : 'R');
            for (SesionEnCurso.Serie serie : e.series) sb.append(',').append(serie.id);
            sb.append(';');
        }
        return sb.toString();
    }

    private void rehacerLista(SesionEnCurso s) {
        listaEjercicios.removeAllViews();
        filas.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout grupo = null;
        for (int i = 0; i < s.ejercicios.size(); i++) {
            SesionEnCurso.Ejercicio e = s.ejercicios.get(i);
            String nombre = e.nombre != null ? e.nombre : getString(R.string.ejercicio_sin_nombre, i + 1);
            if (vm.abierto(e)) {
                grupo = null;
                listaEjercicios.addView(tarjetaAbierta(inflater, e, nombre));
            } else {
                if (grupo == null) {
                    grupo = new LinearLayout(this);
                    grupo.setOrientation(LinearLayout.VERTICAL);
                    grupo.setBackgroundResource(R.drawable.bg_tarjeta_filas);
                    grupo.setClipToOutline(true);
                    int borde = getResources().getDimensionPixelSize(R.dimen.tarjeta_borde);
                    grupo.setPadding(borde, borde, borde, borde);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = getResources().getDimensionPixelSize(R.dimen.espacio_12);
                    listaEjercicios.addView(grupo, lp);
                } else {
                    grupo.addView(raya());
                }
                grupo.addView(filaPlegada(inflater, grupo, e, nombre));
            }
        }
    }

    private View tarjetaAbierta(LayoutInflater inflater, SesionEnCurso.Ejercicio e, String nombre) {
        View tarjeta = inflater.inflate(R.layout.item_apuntar_ejercicio, listaEjercicios, false);
        ((TextView) tarjeta.findViewById(R.id.tvNombreEjercicio)).setText(nombre);
        String pauta = pauta(e, true);
        TextView tvPauta = tarjeta.findViewById(R.id.tvPautaEjercicio);
        tvPauta.setText(pauta);
        tvPauta.setVisibility(pauta == null ? View.GONE : View.VISIBLE);

        // Sin empezar se puede volver a plegar; empezado, se queda abierto.
        View cabecera = tarjeta.findViewById(R.id.cabeceraEjercicio);
        boolean plegable = !RegistrarSesionViewModel.empezado(e);
        tarjeta.findViewById(R.id.ivPliegue).setVisibility(plegable ? View.VISIBLE : View.GONE);
        String a11y = pauta == null ? nombre : nombre + ". " + pauta;
        cabecera.setContentDescription(a11y);
        if (plegable) {
            cabecera.setOnClickListener(v -> vm.alternarPliegue(e.id));
            plegado(cabecera, false);
        } else {
            cabecera.setClickable(false);
            cabecera.setBackground(null);
            ViewCompat.setAccessibilityHeading(cabecera, true);
        }

        boolean tiempo = LogicaSesion.porTiempo(e);
        TextView rotuloKg = tarjeta.findViewById(R.id.tvRotuloKg);
        rotuloKg.setText(tiempo ? R.string.envivo_col_tiempo : R.string.envivo_col_kg);
        ((LinearLayout.LayoutParams) rotuloKg.getLayoutParams()).weight = tiempo ? 2f : 1f;
        tarjeta.findViewById(R.id.tvRotuloReps).setVisibility(tiempo ? View.GONE : View.VISIBLE);

        LinearLayout contenedor = tarjeta.findViewById(R.id.contenedorSeries);
        for (SesionEnCurso.Serie serie : e.series) {
            View fila = inflater.inflate(R.layout.item_envivo_serie, contenedor, false);
            // La fila es la de la sesión en vivo, dentro de una tarjeta entera: sin sus márgenes ni
            // su pieza de tarjeta partida.
            fila.setBackground(null);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fila.getLayoutParams();
            lp.setMarginStart(0);
            lp.setMarginEnd(0);
            contenedor.addView(fila, lp);
            filas.put(serie.id, new FilaSerie(fila, serie.id));
        }
        return tarjeta;
    }

    private View filaPlegada(LayoutInflater inflater, ViewGroup padre, SesionEnCurso.Ejercicio e, String nombre) {
        View f = inflater.inflate(R.layout.item_apuntar_plegado, padre, false);
        ((TextView) f.findViewById(R.id.tvNombrePlegado)).setText(nombre);
        String corta = pauta(e, false);
        TextView tvPauta = f.findViewById(R.id.tvPautaPlegado);
        tvPauta.setText(corta);
        tvPauta.setVisibility(corta == null ? View.GONE : View.VISIBLE);
        String larga = pauta(e, true);
        f.setContentDescription(larga == null ? nombre + ". " + getString(R.string.apuntar_sin_empezar)
                : getString(R.string.apuntar_abrir_ejercicio_a11y, nombre, larga));
        f.setOnClickListener(v -> vm.alternarPliegue(e.id));
        plegado(f, true);
        return f;
    }

    // TalkBack: el estado («Plegado», «Desplegado») y la acción de abrir o plegar.
    private void plegado(View v, boolean plegado) {
        ViewCompat.setStateDescription(v, getString(plegado ? R.string.apuntar_plegado : R.string.apuntar_desplegado));
        ViewCompat.setAccessibilityDelegate(v, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(plegado ? AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND
                        : AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE);
            }

            @Override
            public boolean performAccessibilityAction(@NonNull View host, int action, @Nullable Bundle args) {
                if (action == AccessibilityNodeInfoCompat.ACTION_EXPAND || action == AccessibilityNodeInfoCompat.ACTION_COLLAPSE) {
                    return host.performClick();
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    private View raya() {
        View v = new View(this);
        v.setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, 0));
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.separador_alto));
        lp.setMarginStart(getResources().getDimensionPixelSize(R.dimen.fila_padding_horizontal));
        v.setLayoutParams(lp);
        return v;
    }

    /**
     * La pauta en frase: «3 series de 6 a 10 repeticiones» (larga) o «3 series de 6 a 10» (corta,
     * la de los plegados). Por tiempo, «de 30 a 60 segundos»; por lado, «por pierna».
     *
     * @return null sin pauta (un ejercicio añadido a un entreno libre).
     */
    @Nullable
    private String pauta(SesionEnCurso.Ejercicio e, boolean larga) {
        if (e.seriesPauta <= 0) return null;
        Integer min = LogicaSesion.minimoPauta(e), max = LogicaSesion.maximoPauta(e);
        if (min == null) return null;
        boolean rango = max != null && !max.equals(min);
        String cifra = rango ? getString(R.string.apuntar_pauta_rango, min, max) : String.valueOf(min);
        String cantidad = cifra;
        if (larga) {
            int cuantas = rango ? max : min;
            cantidad = getResources().getQuantityString(LogicaSesion.porTiempo(e)
                    ? R.plurals.apuntar_pauta_segundos : R.plurals.apuntar_pauta_reps, cuantas, cifra);
            if (e.porLado != null) {
                switch (e.porLado) {
                    case "PIERNA": cantidad = getString(R.string.apuntar_pauta_por_pierna, cantidad); break;
                    case "BRAZO": cantidad = getString(R.string.apuntar_pauta_por_brazo, cantidad); break;
                    case "LADO": cantidad = getString(R.string.apuntar_pauta_por_lado, cantidad); break;
                    default: break;
                }
            }
        }
        return getResources().getQuantityString(R.plurals.apuntar_pauta_series, e.seriesPauta, e.seriesPauta, cantidad);
    }

    // ── Una fila de serie ─────────────────────────────────────

    /** Las vistas de una serie (item_envivo_serie) y lo que se escribe en ellas. */
    private final class FilaSerie {
        final View raiz, fila;
        final TextView tvNumero, tvAnterior;
        final EditText etKg, etReps, etTiempo;
        final ImageButton btnMarcar;
        final long serieId;
        boolean pintando;

        FilaSerie(View raiz, long serieId) {
            this.raiz = raiz;
            this.serieId = serieId;
            fila = raiz.findViewById(R.id.filaSerie);
            tvNumero = raiz.findViewById(R.id.tvNumero);
            tvAnterior = raiz.findViewById(R.id.tvAnterior);
            etKg = raiz.findViewById(R.id.etKg);
            etReps = raiz.findViewById(R.id.etReps);
            etTiempo = raiz.findViewById(R.id.etTiempo);
            btnMarcar = raiz.findViewById(R.id.btnMarcar);
            raiz.findViewById(R.id.btnCronometro).setVisibility(View.GONE);
            // Lo escrito va al ViewModel; estas vistas no guardan su estado aparte (manda el borrador).
            for (EditText et : new EditText[]{etKg, etReps, etTiempo}) et.setSaveEnabled(false);
            etKg.addTextChangedListener(alCambiar(etKg, t -> vm.escribirPeso(serieId, t)));
            etReps.addTextChangedListener(alCambiar(etReps, t -> vm.escribirRepeticiones(serieId, t)));
            etTiempo.addTextChangedListener(alCambiar(etTiempo, t -> vm.escribirSegundos(serieId, t)));
            btnMarcar.setOnClickListener(v -> marcar(this));
        }

        private TextWatcher alCambiar(EditText et, java.util.function.Consumer<String> destino) {
            return new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    if (!pintando) destino.accept(s.toString());
                    Object nombre = et.getTag(R.id.etKg);
                    ViewCompat.setStateDescription(et, s.length() > 0 && nombre != null ? nombre.toString() : null);
                }
            };
        }

        /**
         * Pinta la serie.
         *
         * @param recordNuevo si es récord y en el repintado anterior no lo era: entonces se dora.
         */
        void pintar(SesionEnCurso.Ejercicio e, int indice, boolean record, boolean recordNuevo) {
            SesionEnCurso.Serie s = e.series.get(indice);
            int numero = indice + 1;
            boolean tiempo = LogicaSesion.porTiempo(e);
            pintando = true;
            tvNumero.setText(String.valueOf(numero));
            tvAnterior.setText(LogicaSesion.textoAnterior(e, numero, textosSesion, idioma));
            etKg.setVisibility(tiempo ? View.GONE : View.VISIBLE);
            etReps.setVisibility(tiempo ? View.GONE : View.VISIBLE);
            etTiempo.setVisibility(tiempo ? View.VISIBLE : View.GONE);
            String pistaPeso = LogicaSesion.pistaPeso(e, numero, idioma);
            String pistaReps = LogicaSesion.pistaReps(e, numero, textosSesion);
            String pistaSeg = LogicaSesion.pistaSegundos(e, numero, textosSesion);
            poner(etKg, s.peso, pistaPeso);
            poner(etReps, s.repeticiones, pistaReps);
            poner(etTiempo, s.segundos, pistaSeg);
            rotular(etKg, getString(R.string.envivo_kg_a11y, numero), pistaPeso);
            rotular(etReps, getString(R.string.envivo_reps_a11y, numero), pistaReps);
            rotular(etTiempo, getString(R.string.envivo_tiempo_a11y, numero), pistaSeg);
            pintando = false;

            // La fila del récord, en dorado; al pasar a serlo, se dora con el momento 25.
            int oroFila = ContextCompat.getColor(RegistrarSesionActivity.this, R.color.gp_gold_fila);
            if (record && recordNuevo) {
                // dorarFila deja el color final con «Quitar animaciones»; la vibración se queda.
                Movimiento.dorarFila(fila, MaterialColors.getColor(fila, com.google.android.material.R.attr.colorSurface),
                        ContextCompat.getColor(RegistrarSesionActivity.this, R.color.gp_gold_container), oroFila);
                Movimiento.vibrar(fila, Movimiento.Vibracion.EXITO);
            } else {
                fila.setBackgroundColor(record ? oroFila : android.graphics.Color.TRANSPARENT);
            }

            // El botón: verde con su check si está hecha, dorado con el trofeo si es el récord y gris
            // si no; el estado lo dice también para TalkBack, no solo el color.
            btnMarcar.setImageResource(record ? R.drawable.ic_ms_trophy_fill : R.drawable.ic_ms_check);
            // Sin marcar, un aro sin relleno (bg_check_pendiente, gp_stroke); marcada, el círculo lleno:
            // la diferencia es de forma, no solo de color (DEC-019).
            // El récord es el círculo teñido de dorado con su trofeo; hecha y sin hacer, las formas de
            // MarcaSerie (círculo con borde y aro), sin tinte (GP-252).
            if (record) {
                btnMarcar.setBackgroundResource(R.drawable.bg_check);
                btnMarcar.setBackgroundTintList(ColorStateList.valueOf(fondo(s.hecha, true)));
            } else {
                btnMarcar.setBackgroundTintList(null);
                btnMarcar.setBackgroundResource(MarcaSerie.fondo(s.hecha, false));
            }
            btnMarcar.setImageTintList(ColorStateList.valueOf(record
                    ? MaterialColors.getColor(btnMarcar, com.google.android.material.R.attr.colorOnPrimary)
                    : s.hecha ? ContextCompat.getColor(RegistrarSesionActivity.this, R.color.gp_success)
                    : MaterialColors.getColor(btnMarcar, com.google.android.material.R.attr.colorOnSurfaceVariant)));
            String estado = getString(MarcaSerie.estado(s.hecha));
            ViewCompat.setStateDescription(btnMarcar, record ? estado + ", " + getString(R.string.apuntar_serie_record) : estado);
            btnMarcar.setContentDescription(getString(s.hecha ? R.string.apuntar_serie_apuntada_a11y
                    : R.string.apuntar_apuntar_serie_a11y, numero));

            // TalkBack: «Serie 2. Anterior: 57,5 × 12», donde entra en la fila.
            tvNumero.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            SesionEnCurso.SerieAnterior a = LogicaSesion.anterior(e, numero);
            String nombre = getString(R.string.envivo_serie_a11y, numero);
            tvNumero.setContentDescription(a == null ? nombre
                    : nombre + ". " + getString(R.string.envivo_anterior_a11y, tvAnterior.getText()));
        }

        private void poner(EditText et, String valor, String pista) {
            if (!et.getText().toString().equals(valor)) et.setText(valor);
            et.setHint(pista);
        }

        // El nombre del campo no va como descripción: taparía el valor. Vacío, va como pista del nodo
        // («Kilos, serie 1, 57,5»); con valor, como estado, que TalkBack lee tras el valor.
        private void rotular(EditText et, String nombre, String pista) {
            et.setTag(R.id.etKg, nombre);
            ViewCompat.setStateDescription(et, et.getText().length() > 0 ? nombre : null);
            String texto = pista == null || pista.isEmpty() ? nombre : nombre + ", " + pista;
            ViewCompat.setAccessibilityDelegate(et, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setHintText(texto);
                }
            });
        }
    }

    private int fondo(boolean hecha, boolean record) {
        if (record) return ContextCompat.getColor(this, R.color.gp_gold);
        if (hecha) return ContextCompat.getColor(this, R.color.gp_success_container);
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, 0);
    }

    // Marcar: el ViewModel decide (LogicaSesion.marcar); aquí se dice lo que falta y se anima.
    private void marcar(FilaSerie f) {
        SesionEnCurso.Serie antes = serie(f.serieId);
        boolean estabaHecha = antes != null && antes.hecha;
        // Lo que hay encima puede cambiar de alto al marcar (entra el récord en la cabecera, se va
        // «Todo como la última vez») o la lista rehacerse: la fila tocada se queda bajo el dedo.
        int yAntes = yEnPantalla(f.raiz);
        LogicaSesion.Marcado m = vm.marcar(f.serieId, idioma);
        anclar(f.serieId, yAntes);
        switch (m) {
            case FALTAN_REPS:
            case FALTAN_SEGUNDOS:
                // No se marca: el foco va al campo que falta y se dice qué escribir.
                EditText campo = m == LogicaSesion.Marcado.FALTAN_REPS ? f.etReps : f.etTiempo;
                campo.requestFocus();
                avisar(m == LogicaSesion.Marcado.FALTAN_REPS ? R.string.envivo_falta_reps : R.string.envivo_falta_segundos);
                break;
            case NO_VALIDO:
                avisar(R.string.envivo_no_valido);
                break;
            case HECHA:
                if (!estabaHecha) {
                    // El círculo crece al marcar (momento 24), también el del récord, que va a dorado;
                    // ese ya vibra al dorarse la fila, así que aquí no vibra otra vez.
                    boolean record = seriesRecord.contains(f.serieId);
                    FilaSerie ahora = filas.get(f.serieId);
                    ImageButton boton = ahora != null ? ahora.btnMarcar : f.btnMarcar;
                    if (record) {
                        boton.setBackgroundResource(R.drawable.bg_check);
                        Movimiento.marcarSerie(boton, fondo(false, false), fondo(true, true), false);
                    } else {
                        boton.setBackgroundTintList(null);
                        boton.setBackgroundResource(MarcaSerie.fondo(true, false));
                        Movimiento.crecerMarca(boton, true);
                    }
                }
                break;
            default:
                break;
        }
    }

    private static int yEnPantalla(View v) {
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);
        return xy[1];
    }

    // Tras el siguiente layout, desplaza lo justo para que la fila de la serie vuelva a su altura.
    private void anclar(long serieId, int yAntes) {
        androidx.core.widget.NestedScrollView scroll = findViewById(R.id.scrollApuntar);
        scroll.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                FilaSerie f = filas.get(serieId);
                if (f == null || !f.raiz.isAttachedToWindow()) return true;
                int salto = yEnPantalla(f.raiz) - yAntes;
                if (salto == 0) return true;
                scroll.scrollBy(0, salto);
                return false;
            }
        });
    }

    @Nullable
    private SesionEnCurso.Serie serie(long id) {
        for (SesionEnCurso.Ejercicio e : vm.getBorrador().ejercicios) {
            for (SesionEnCurso.Serie s : e.series) if (s.id == id) return s;
        }
        return null;
    }

    private void avisar(int texto) {
        Snackbar.make(listaEjercicios, texto, Snackbar.LENGTH_LONG).show();
    }

    // ── La fecha ──────────────────────────────────────────────

    // «Hoy, viernes 6», «Ayer, jueves 5», «Martes 3 de noviembre».
    private String textoFecha(String dia) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(dia.substring(0, 4)), Integer.parseInt(dia.substring(5, 7)) - 1,
                Integer.parseInt(dia.substring(8, 10)));
        if (dia.equals(vm.hoy())) return getString(R.string.apuntar_fecha_hoy, formato(R.string.apuntar_fecha_patron_dia, c));
        if (dia.equals(vm.ayer())) return getString(R.string.apuntar_fecha_ayer, formato(R.string.apuntar_fecha_patron_dia, c));
        boolean esteAnio = dia.substring(0, 4).equals(vm.hoy().substring(0, 4));
        String texto = formato(esteAnio ? R.string.apuntar_fecha_patron_otro : R.string.apuntar_fecha_patron_otro_anio, c);
        return texto.substring(0, 1).toUpperCase(idioma) + texto.substring(1);
    }

    private String formato(int patron, Calendar c) {
        return new SimpleDateFormat(getString(patron), idioma).format(c.getTime());
    }

    // Hoy, Ayer u Otro día, como botones de opción; Otro día abre el calendario, sin días futuros.
    private void elegirFecha() {
        String fecha = vm.getFecha();
        int elegida = fecha.equals(vm.hoy()) ? 0 : fecha.equals(vm.ayer()) ? 1 : 2;
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        opciones.add(HojaElegir.Opcion.de(getString(R.string.tiempo_hoy_mayuscula)));
        opciones.add(HojaElegir.Opcion.de(getString(R.string.tiempo_ayer_mayuscula)));
        opciones.add(HojaElegir.Opcion.de(getString(R.string.apuntar_fecha_otro_dia)));
        HojaElegir.mostrar(this, R.string.apuntar_fecha_titulo, opciones, elegida, 0, i -> {
            if (i == 0) vm.setFecha(vm.hoy());
            else if (i == 1) vm.setFecha(vm.ayer());
            else abrirCalendario();
        });
    }

    private void abrirCalendario() {
        long hoyUtc = com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds();
        com.google.android.material.datepicker.MaterialDatePicker<Long> picker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.apuntar_fecha_titulo)
                        .setSelection(utcDe(vm.getFecha()))
                        .setCalendarConstraints(new com.google.android.material.datepicker.CalendarConstraints.Builder()
                                .setEnd(hoyUtc)
                                .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())
                                .build())
                        .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            vm.setFecha(f.format(new java.util.Date(sel)));
        });
        picker.show(getSupportFragmentManager(), "fecha");
    }

    // El día elegido, en medianoche UTC, que es como lo cuenta el calendario.
    private static long utcDe(String dia) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Integer.parseInt(dia.substring(0, 4)), Integer.parseInt(dia.substring(5, 7)) - 1,
                Integer.parseInt(dia.substring(8, 10)));
        return c.getTimeInMillis();
    }

    // ── La rutina ─────────────────────────────────────────────

    private void elegirRutina() {
        ElegirRutinaHoja.mostrarParaApuntar(this, vm.orden(), vm.getRutinaId(), r -> {
            Integer id = r != null ? r.getId() : null;
            if (java.util.Objects.equals(id, vm.getRutinaId())) return;
            int marcadas = vm.marcadas();
            if (marcadas == 0) {
                vm.elegirRutina(id);
                return;
            }
            // Con series marcadas, cambiar las quita: se pregunta antes.
            UIHelper.mostrarDialogoConIcono(this, getString(R.string.apuntar_cambiar_titulo),
                    getResources().getQuantityString(R.plurals.apuntar_cambiar_mensaje, marcadas, marcadas),
                    R.drawable.ic_ms_autorenew, getString(R.string.apuntar_cambiar_si), getString(R.string.dialog_cancelar),
                    () -> vm.elegirRutina(id), null);
        });
    }

    // Lo que devuelve la biblioteca: [{ejercicioId, nombre, …}].
    private static List<SesionEnCursoRepositorio.Nuevo> leerAnadidos(@Nullable String json) {
        List<SesionEnCursoRepositorio.Nuevo> nuevos = new ArrayList<>();
        if (json == null) return nuevos;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                nuevos.add(new SesionEnCursoRepositorio.Nuevo(o.getInt("ejercicioId"), o.optString("nombre", null),
                        o.has("medida") ? o.optString("medida") : null));
            }
        } catch (JSONException e) {
            // La biblioteca lo escribe siempre bien; si no, no se añade nada y el entreno sigue como
            // estaba, que es lo seguro.
            return Collections.emptyList();
        }
        return nuevos;
    }

    // ── Las caras y la nota ───────────────────────────────────

    // Cinco caras de 48 dp, botones de opción con su nombre; el grupo dice cuántas hay.
    private void montarCaras() {
        int lado = getResources().getDimensionPixelSize(R.dimen.zona_tactil);
        int hueco = getResources().getDimensionPixelSize(R.dimen.espacio_4);
        ViewCompat.setAccessibilityDelegate(grupoCaras, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, Caras.NUMERO, false,
                        AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE));
            }
        });
        grupoCaras.setContentDescription(getString(R.string.apuntar_que_tal));
        for (int valor = 1; valor <= Caras.NUMERO; valor++) {
            ImageButton cara = new ImageButton(this);
            cara.setImageResource(Caras.icono(valor));
            cara.setScaleType(ImageView.ScaleType.CENTER);
            cara.setForeground(ContextCompat.getDrawable(this, R.drawable.fondo_pulsable_redondo));
            cara.setContentDescription(getString(Caras.nombre(valor)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, lado);
            if (valor > 1) lp.setMarginStart(hueco);
            final int este = valor;
            cara.setOnClickListener(v -> {
                Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
                vm.tocarCara(este);
            });
            grupoCaras.addView(cara, lp);
            caras.add(cara);
        }
    }

    private void pintarCaras() {
        Integer valor = vm.getValoracion();
        int nombre = Caras.nombre(valor);
        tvCaraElegida.setText(nombre == 0 ? "" : getString(nombre));
        for (int i = 0; i < caras.size(); i++) {
            ImageButton cara = caras.get(i);
            boolean marcada = valor != null && valor == i + 1;
            cara.setBackgroundResource(marcada ? R.drawable.bg_cara_elegida : R.drawable.bg_cara);
            cara.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(cara, marcada
                    ? com.google.android.material.R.attr.colorOnPrimary : com.google.android.material.R.attr.colorOnSurface)));
            final int posicion = i;
            ViewCompat.setAccessibilityDelegate(cara, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName("android.widget.RadioButton");
                    info.setCheckable(true);
                    info.setChecked(marcada);
                    info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                            0, 1, posicion, 1, false, marcada));
                }
            });
        }
    }

    // La nota, plegada tras el lápiz; con algo escrito sale abierta.
    private void montarNota() {
        etNota.setSaveEnabled(false);
        etNota.setText(vm.getNotas());
        etNota.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { vm.setNotas(s.toString()); }
        });
        mostrarNota(!vm.getNotas().isEmpty(), false);
        btnNota.setOnClickListener(v -> mostrarNota(tilNota.getVisibility() != View.VISIBLE, true));
    }

    private void mostrarNota(boolean abierta, boolean enfocar) {
        tilNota.setVisibility(abierta ? View.VISIBLE : View.GONE);
        ViewCompat.setStateDescription(btnNota, getString(abierta ? R.string.apuntar_desplegado : R.string.apuntar_plegado));
        if (abierta && enfocar) {
            etNota.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etNota, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ── Guardar ───────────────────────────────────────────────

    /**
     * Pinta el guardado. Tras girar, la pantalla nueva recibe el estado en curso: si sigue
     * guardando, vuelve el «Cargando…»; si falló y nadie cerró el aviso, vuelve el aviso; y el
     * éxito abre el resumen una sola vez, porque se marca como abierto.
     */
    private void pintarGuardado(EstadoGuardado estado) {
        switch (estado.fase) {
            case GUARDANDO:
                LoadingDialog.show(this);
                break;
            case FALLO:
                LoadingDialog.hide(this);
                ofrecerReintento(estado);
                break;
            case EXITO:
                LoadingDialog.hide(this);
                vm.resumenAbierto();
                UIHelper.mostrarToastExito(this, getString(R.string.sesiones_exito));
                setResult(RESULT_OK);
                irAlResumen(estado.sesion);
                finish();
                break;
            default:
                LoadingDialog.hide(this);
                break;
        }
    }

    /**
     * Avisa de que NO se ha guardado —o de que no se sabe— y ofrece repetir el envío. No se cierra
     * la pantalla, no se borra nada de lo marcado y no se cambia la clave.
     */
    private void ofrecerReintento(EstadoGuardado estado) {
        if (avisoFallo != null && avisoFallo.isShowing()) return;

        String titulo;
        String mensaje;
        if (estado.interrumpido) {
            titulo = getString(R.string.sesiones_error_interrumpido_titulo);
            mensaje = getString(R.string.sesiones_error_interrumpido);
        } else {
            titulo = getString(R.string.sesiones_error_guardar_titulo);
            String motivo = estado.codigo == 0
                    ? getString(R.string.sesiones_error_guardar)
                    : UiFeedback.mensaje(this, estado.codigo, estado.mensaje);
            mensaje = motivo + "\n\n" + getString(R.string.sesiones_error_guardar_ayuda);
        }

        avisoFallo = UIHelper.mostrarDialogoConIcono(this, titulo, mensaje, R.drawable.ic_ms_error,
                getString(R.string.sesiones_error_reintentar),
                getString(R.string.sesiones_error_ahora_no),
                () -> {
                    avisoFallo = null;
                    vm.guardar();
                },
                () -> {
                    avisoFallo = null;
                    vm.descartarFallo();
                });
    }

    // Al volver de segundo plano con el guardado aún en marcha, el «Cargando…» vuelve: el LiveData
    // no repite un estado que ya entregó.
    @Override
    protected void onStart() {
        super.onStart();
        EstadoGuardado estado = vm.getEstadoGuardado().getValue();
        if (estado != null && estado.fase == RegistrarSesionViewModel.Fase.GUARDANDO) {
            LoadingDialog.show(this);
        }
    }

    // El «Cargando…» se quita al dejar de verse: cerrado a tiempo no queda colgado de una pantalla
    // destruida al girar. onStart lo vuelve a poner si sigue guardando.
    @Override
    protected void onStop() {
        LoadingDialog.hide(this);
        super.onStop();
    }

    // El aviso de fallo se cierra sin contar como «Ahora no»: la pantalla nueva lo vuelve a enseñar,
    // porque en el ViewModel sigue abierto.
    @Override
    protected void onDestroy() {
        if (avisoFallo != null) {
            avisoFallo.dismiss();
            avisoFallo = null;
        }
        super.onDestroy();
    }

    // Abre el resumen de la sesión recién guardada.
    private void irAlResumen(SesionEntrenamiento sesion) {
        ArrayList<String> nuevosLogros = new ArrayList<>();
        if (sesion.getNuevosLogros() != null) nuevosLogros.addAll(sesion.getNuevosLogros());

        Intent intent = new Intent(this, ResumenSesionActivity.class);
        intent.putExtra("sesionId", sesion.getId());
        intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
        intent.putExtra(ResumenSesionActivity.EXTRA_RECORDS, new ArrayList<>(sesion.getRecordsBatidos()));
        intent.putExtra(ResumenSesionActivity.EXTRA_PRIMERAS_MARCAS,
                Marcas.ejerciciosDistintos(sesion.getPrimerasMarcas()));
        startActivity(intent);
    }

    // ── Textos ────────────────────────────────────────────────

    private CabeceraEntreno.Textos textos() {
        return new CabeceraEntreno.Textos() {
            @NonNull @Override public String pieConKilos(int series, @NonNull String kilos) {
                return getResources().getQuantityString(R.plurals.apuntar_pie_con_kilos, series, series, kilos);
            }
            @NonNull @Override public String pieSinKilos(int series) {
                return getResources().getQuantityString(R.plurals.apuntar_pie_sin_kilos, series, series);
            }
            @NonNull @Override public String pieVacio() { return getString(R.string.apuntar_pie_vacio); }
            @NonNull @Override public String ultimaKilos(@NonNull String kilos) { return getString(R.string.apuntar_ultima_kilos, kilos); }
            @NonNull @Override public String ultimaSeries(int series) {
                return getResources().getQuantityString(R.plurals.apuntar_ultima_series, series, series);
            }
            @NonNull @Override public String ultimaSinSeries() { return getString(R.string.apuntar_ultima_sin_series); }
            @NonNull @Override public String recordUno(@NonNull String ejercicio, @NonNull String marca) {
                return getString(R.string.apuntar_record_uno, ejercicio, marca);
            }
            @NonNull @Override public String recordVarios(int n, @NonNull String ejercicios) {
                return getResources().getQuantityString(R.plurals.apuntar_records, n, n, ejercicios);
            }
            @NonNull @Override public String y() { return getString(R.string.enumeracion_y); }
            @NonNull @Override public String ySerie() { return getString(R.string.tarjeta_y_serie); }
            @NonNull @Override public String marca(@NonNull SesionEnCurso.Marca m) { return textoMarca(m); }
            @NonNull @Override public String sinNombre(int posicion) { return getString(R.string.ejercicio_sin_nombre, posicion); }
        };
    }

    // «72,5 kg × 8», «15 reps» o «1:15», como «Tu récord» de la sesión en vivo.
    private String textoMarca(SesionEnCurso.Marca m) {
        if (Record.TIPO_TIEMPO.equals(m.tipo)) return Marcas.tiempo(m.segundos);
        if (Record.TIPO_PESO.equals(m.tipo)) {
            double kilos;
            try {
                kilos = m.peso != null ? Double.parseDouble(m.peso) : 0;
            } catch (NumberFormatException e) {
                kilos = 0;
            }
            return sinPartir(getString(R.string.record_peso_por_reps, Marcas.kilos(this, kilos), m.repeticiones));
        }
        return sinPartir(getResources().getQuantityString(R.plurals.record_repeticiones, m.repeticiones, m.repeticiones));
    }

    // Una marca no se parte entre líneas: «95 kg × 10» va en una pieza.
    private static String sinPartir(String marca) {
        return marca.replace(' ', '\u00A0');
    }

    private LogicaSesion.Textos textosSesion() {
        return new LogicaSesion.Textos() {
            @Override public String pesoPorReps(String kilos, int reps) { return getString(R.string.envivo_peso_por_reps, kilos, reps); }
            @Override public String reps(int reps) { return getResources().getQuantityString(R.plurals.envivo_reps, reps, reps); }
            @Override public String rango(int min, int max) { return getString(R.string.pauta_rango, min, max); }
            @Override public String segundos(String numero) { return getString(R.string.pauta_segundos, numero); }
            @Override public String nada() { return getString(R.string.envivo_nada); }
        };
    }

    // El «min», el «kg» o el «de 21» en Barlow regular dentro de una cifra condensada.
    private static final class Fuente extends android.text.style.MetricAffectingSpan {
        private final Typeface tf;

        Fuente(Typeface tf) { this.tf = tf; }

        @Override public void updateDrawState(android.text.TextPaint p) { p.setTypeface(tf); }

        @Override public void updateMeasureState(@NonNull android.text.TextPaint p) { p.setTypeface(tf); }
    }
}
