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
import android.animation.ValueAnimator;
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
import es.pmdm.gymprofit.envivo.LogicaDescanso;
import es.pmdm.gymprofit.envivo.PermisosDescanso;
import es.pmdm.gymprofit.envivo.TextosDescanso;
import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.ui.adapters.SesionEnVivoAdapter;
import es.pmdm.gymprofit.ui.cristal.BarraProgresoCristal;
import es.pmdm.gymprofit.ui.cristal.BordeSistema;
import es.pmdm.gymprofit.ui.cristal.BordesSistema;
import es.pmdm.gymprofit.ui.cristal.FondoAmbiental;
import es.pmdm.gymprofit.ui.cristal.Hundir;
import es.pmdm.gymprofit.ui.cristal.LienzoCristal;
import es.pmdm.gymprofit.ui.cristal.VentanaCristal;
import es.pmdm.gymprofit.ui.cristal.VigiaCristal;
import es.pmdm.gymprofit.ui.widget.TextoSinPartir;
import es.pmdm.gymprofit.utils.InputDialog;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.Movimiento;
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
//
// Es una pantalla Liquid Glass (GP-285, lote 2; DEC-070): el fondo ambiental de la sesión y
// la lista van en un LienzoCristal, y encima flotan, de cristal vivo, la cabecera, el resumen
// y el descanso. La lista pasa por debajo: esta pantalla coloca sus márgenes del sistema
// (InsetsUtils la deja) y le da a la lista el relleno de arriba y de abajo que hace falta.
// ============================================================
public class SesionEnVivoActivity extends AppCompatActivity implements SesionEnVivoAdapter.Acciones {

    // Aplica la escala de fuente global de la app.
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private SesionEnCursoRepositorio repo;
    private SesionEnVivoAdapter adapter;
    private TextView tvTitulo, tvSubtitulo;
    private TextView tvTiempo, tvSeries, tvVolumen;
    private View filaVolumen;
    private View filaResumen, cardGuardado, btnTerminar;
    private BarraProgresoCristal barraProgreso;
    private RecyclerView rv;

    // Liquid Glass (GP-285): el lienzo de detrás, su fondo, los bordes del sistema y lo que
    // flota arriba. El hueco del sistema de abajo es la barra de navegación o el teclado.
    @Nullable private LienzoCristal lienzo;
    @Nullable private VigiaCristal vigiaCristal;
    private FondoAmbiental fondo;
    private final BordeSistema bordeArriba = new BordeSistema(true), bordeAbajo = new BordeSistema(false);
    private View cabecera, filaCabecera, bordeSuperior, bordeInferior;
    // El hueco del sistema de abajo sin contar el teclado (lo que el borde de abajo tapa siempre).
    private int huecoSistema, huecoBarras;

    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable tic = new Runnable() {
        @Override public void run() {
            pintarReloj();
            comprobarCronometro();
            pintarDescanso(repo.actual());
            reloj.postDelayed(this, 1000);
        }
    };

    // Lo último que se pintó en la cabecera, para que las cifras sumen desde ahí (momento 24).
    private double volumenPintado = -1;
    private int seriesPintadas = -1;
    private int progresoPintado = -1;
    @Nullable private ValueAnimator contandoVolumen, contandoSeries;

    // El descanso (GP-013): el panel de abajo y el aviso de su fin; con la barra que se vacía y
    // sus momentos (GP-210, 26): sube al empezar, el número late a falta de 10 s y baja al acabar.
    private View zonaDescanso, panelDescanso, grupoCuentaAtras, avisoFin, btnDescansoMas;
    private TextView tvDescansoTiempo, tvDescansoDespues, tvFinTitulo, tvFinTexto;
    // La barra que se vacía (hasta GP-285, un aro).
    private BarraProgresoCristal barraDescanso;
    @Nullable private ValueAnimator vaciandoBarra;
    private long barraFinMs = -1, barraDuracionMs = -1;
    private long latidoDeFinMs = -1;
    private boolean bajandoPanel;
    // La serie a la que llevar la lista cuando el panel acabe de medirse; -1 si ninguna.
    private long irASerie = -1;

    @Nullable private BottomSheetDialog hojaCrono;
    @Nullable private TextView tvCronoTiempo;
    @Nullable private LinearProgressIndicator barraCrono;
    @Nullable private BottomSheetDialog hojaTerminar;
    // Se abrió el resumen: cuando la sesión desaparezca no hay que abrir Inicio.
    private boolean guardada;

    private ActivityResultLauncher<Intent> anadirEjercicios;

    // Sin red (GP-223): el aviso de arriba y quien vigila la red mientras la pantalla se ve.
    private View avisoSinRed;
    @Nullable private android.net.ConnectivityManager.NetworkCallback vigilaRed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        setContentView(R.layout.activity_sesion_en_vivo);

        repo = SesionEnCursoRepositorio.get(this);
        repo.usarCuenta(prefs.getUsuarioId());
        if (!repo.hay()) {
            // Se llegó por una notificación vieja o la sesión ya se guardó.
            salir();
            return;
        }

        prepararCristal();
        View btnMinimizar = findViewById(R.id.btnMinimizar);
        btnMinimizar.setOnClickListener(v -> salir());
        androidx.appcompat.widget.TooltipCompat.setTooltipText(btnMinimizar, btnMinimizar.getContentDescription());
        tvTitulo = findViewById(R.id.tvTituloSesion);
        tvSubtitulo = findViewById(R.id.tvSubtituloSesion);
        ViewCompat.setAccessibilityHeading(tvTitulo, true);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvSeries = findViewById(R.id.tvSeries);
        tvVolumen = findViewById(R.id.tvVolumen);
        filaVolumen = findViewById(R.id.filaVolumen);
        filaResumen = findViewById(R.id.filaResumen);
        barraProgreso = findViewById(R.id.barraProgreso);
        barraProgreso.setColoresRelleno(
                androidx.core.content.ContextCompat.getColor(this, R.color.gp_cristal_descanso_alto),
                androidx.core.content.ContextCompat.getColor(this, R.color.gp_cristal_descanso_bajo));
        // Las tres cifras, al mismo tamaño: si con letra grande una tiene que encoger para no
        // partirse, encogen las tres (GP-288).
        TextoSinPartir.igualar((TextoSinPartir) tvTiempo, (TextoSinPartir) tvVolumen, (TextoSinPartir) tvSeries);
        cardGuardado = findViewById(R.id.cardGuardado);
        btnTerminar = findViewById(R.id.btnTerminar);
        btnTerminar.setOnClickListener(v -> abrirTerminar());
        // Minimizar y «Terminar» son cristal vivo, no botones: a TalkBack se le dice que lo son,
        // y responden al dedo como los demás botones de Liquid Glass (se hunden, DEC-070).
        for (View boton : new View[]{btnMinimizar, btnTerminar}) {
            comoBoton(boton);
            Hundir.alPulsar(boton);
        }
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
        // La tarjeta de cristal de cada ejercicio, de una pieza, detrás de sus filas.
        rv.addItemDecoration(SesionEnVivoAdapter.tarjetas(rv.getContext()));
        // Las filas no parpadean al marcar: el cambio es el tinte, no un fundido.
        if (rv.getItemAnimator() instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        instalarDeslizar();
        configurarDescanso();
        aplicarInsets();

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

        avisoSinRed = findViewById(R.id.avisoSinRed);
        // El aviso de siempre, sin su fondo opaco: aquí va dentro de su cristal vivo.
        ((android.view.ViewGroup) avisoSinRed).getChildAt(0).setBackground(null);
        ((android.widget.ImageView) avisoSinRed.findViewById(R.id.ivAviso)).setImageResource(R.drawable.ic_ms_cloud_off);
        ((TextView) avisoSinRed.findViewById(R.id.tvAviso)).setText(R.string.envivo_sin_red);
        boolean desdeArranque = savedInstanceState == null && getIntent().getBooleanExtra(
                es.pmdm.gymprofit.arranque.PantallaArranque.EXTRA_DESDE_ARRANQUE, false);
        pintarRed(hayRed(), desdeArranque);
        if (desdeArranque) {
            // Desde el arranque sin red (GP-223): la sesión sube 40 dp mientras aparece.
            View contenido = ((android.view.ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
            // Solo el desplazamiento: la aparición la hace el fundido de la ventana.
            Movimiento.subirSinFundido(java.util.Collections.singletonList(contenido), Movimiento.SESION_SUBE_DP);
        }

        // Lo que falte por pedir: los ejercicios de la rutina, si Android cerró la app
        // mientras llegaban, y la última vez de los que aún no la tienen.
        repo.cargarEjercicios();
        repo.pedirUltimaVez();
        repo.pedirMarcas();
    }

    @Override
    protected void onStart() {
        super.onStart();
        reloj.post(tic);
        // Los nombres y las notas, en el idioma de la app si cambió con la sesión abierta (GP-251).
        repo.traducir();
        vigilarRed();
    }

    @Override
    protected void onStop() {
        reloj.removeCallbacks(tic);
        dejarDeVigilarRed();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // El nivel del cristal puede haber cambiado (alto contraste, o el vigía bajó el techo).
        if (lienzo != null) lienzo.revisarNivel();
        if (vigiaCristal != null) vigiaCristal.empezar();
    }

    @Override
    protected void onPause() {
        if (vigiaCristal != null) vigiaCristal.parar();
        super.onPause();
    }

    // ── Liquid Glass (GP-285, lote 2) ────────────────────────

    // El lienzo con su fondo ambiental (el de la sesión, con el dorado), los bordes del sistema,
    // que desvanecen la lista hacia ese mismo fondo, y el vigía del rendimiento del cristal vivo.
    private void prepararCristal() {
        lienzo = findViewById(R.id.lienzoCristal);
        fondo = FondoAmbiental.sesion(this);
        // Cuando hay imagen nueva (se calcula en un hilo aparte) los bordes la cogen, y siguen
        // el fundido con el que aparece.
        fondo.alCambiar(() -> {
            bordeArriba.ponerFondo(fondo.imagen());
            bordeAbajo.ponerFondo(fondo.imagen());
            bordeArriba.mezcla(fondo.aparicion());
            bordeAbajo.mezcla(fondo.aparicion());
        });
        findViewById(R.id.fondoAmbiental).setBackground(fondo);
        bordeArriba.mezcla(0f);
        bordeAbajo.mezcla(0f);
        findViewById(R.id.bordeSuperior).setBackground(bordeArriba);
        findViewById(R.id.bordeInferior).setBackground(bordeAbajo);
        vigiaCristal = new VigiaCristal(this, lienzo);
        cabecera = findViewById(R.id.cabeceraSesion);
        filaCabecera = findViewById(R.id.filaCabecera);
        bordeSuperior = findViewById(R.id.bordeSuperior);
        // La lista deja libre arriba lo que mida lo que flota (la cabecera, el resumen y, si
        // salen, el aviso de sin red y el estado del guardado), y el borde de arriba la
        // desvanece antes de la fila del título.
        cabecera.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> v.post(() -> {
            ajustarBordeSuperior();
            ajustarHuecos();
        }));
    }

    // Esta pantalla gestiona sus márgenes del sistema, como la navegación principal: a los
    // lados, la raíz; arriba, la cabecera (el lienzo, con el fondo ambiental, sigue bajo la
    // barra de estado); abajo, el descanso, que flota por encima del hueco de gestos o de
    // botones, o del teclado. Y los dos bordes del sistema, macizos donde están los iconos.
    private void aplicarInsets() {
        VentanaCristal.barrasTransparentes(this);
        View raiz = findViewById(R.id.raizSesion);
        bordeInferior = findViewById(R.id.bordeInferior);
        int arribaBase = getResources().getDimensionPixelSize(R.dimen.espacio_8);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            androidx.core.graphics.Insets barras = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets muesca = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            int teclado = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            int arriba = Math.max(barras.top, muesca.top);
            int abajo = Math.max(barras.bottom, muesca.bottom);
            v.setPadding(Math.max(barras.left, muesca.left), 0, Math.max(barras.right, muesca.right), 0);
            cabecera.setPadding(cabecera.getPaddingLeft(), arriba + arribaBase,
                    cabecera.getPaddingRight(), cabecera.getPaddingBottom());
            // El teclado sustituye a la barra de navegación cuando está abierto: manda el mayor.
            huecoSistema = Math.max(abajo, teclado);
            zonaDescanso.setPadding(0, 0, 0, huecoSistema);
            huecoBarras = abajo;
            // Los bordes dependen de dónde queden la cabecera y el descanso: al colocarse.
            ajustarHuecos();
            return insets;
        });
        ViewCompat.requestApplyInsets(raiz);
    }

    // El borde de arriba es macizo hasta donde empieza el primer cristal de debajo del título
    // (la barra de estado, la cabecera y el hueco que la separa del resumen: el título va suelto
    // sobre el fondo y la lista no puede pasarle por detrás, ni asomar a medias justo debajo,
    // donde con letra grande se leía como una línea más del título) y de ahí se desvanece, como
    // el de la navegación principal bajo la barra de estado. La lista sí pasa por detrás del
    // resumen, que es cristal vivo: aparece desenfocada bajo su canto de arriba.
    private void ajustarBordeSuperior() {
        if (cabecera == null || filaCabecera.getHeight() == 0) return;
        int macizo = cabecera.getTop() + filaCabecera.getBottom();
        android.view.ViewGroup pila = (android.view.ViewGroup) cabecera;
        for (int i = pila.indexOfChild(filaCabecera) + 1; i < pila.getChildCount(); i++) {
            View pieza = pila.getChildAt(i);
            if (pieza.getVisibility() != View.VISIBLE) continue;
            macizo = cabecera.getTop() + pieza.getTop();
            break;
        }
        ponerAlto(bordeSuperior, BordesSistema.altoSuperior(macizo,
                getResources().getDimensionPixelSize(R.dimen.borde_sistema_desvanecido)));
        bordeArriba.poner(androidx.core.content.ContextCompat.getColor(this, R.color.gp_ambiente_base), macizo);
    }

    // El borde de abajo es macizo en la franja de la barra del sistema y de ahí se desvanece.
    // Con el descanso abierto (o el aviso de su fin) sube hasta el pie del panel: en los 22 dp
    // que lo separan del borde, la línea de la lista que pasaba por debajo se leía como una
    // línea más del panel, justo tras «Siguiente: …» (revisor de UI). La lista sigue pasando,
    // desenfocada, por detrás del panel.
    private void ajustarBordeInferior(boolean conDescanso) {
        if (bordeInferior == null) return;
        int macizo = BordesSistema.macizoInferior(huecoBarras);
        if (conDescanso) {
            macizo = zonaDescanso.getPaddingBottom()
                    + getResources().getDimensionPixelSize(R.dimen.envivo_descanso_abajo);
        }
        ponerAlto(bordeInferior, macizo + getResources().getDimensionPixelSize(R.dimen.borde_sistema_desvanecido));
        bordeAbajo.poner(androidx.core.content.ContextCompat.getColor(this, R.color.gp_ambiente_base), macizo);
    }

    private static void ponerAlto(View v, int alto) {
        android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp.height == alto) return;
        lp.height = alto;
        v.setLayoutParams(lp);
    }

    // El relleno de la lista: arriba, lo que mide lo que flota; abajo, lo que ocupe el descanso
    // (con letra grande crece) o, sin él, el hueco de siempre, y nunca menos que el hueco del
    // sistema (el teclado), para que la última serie no quede tapada.
    private void ajustarHuecos() {
        if (rv == null || cabecera == null || zonaDescanso == null) return;
        int hueco = getResources().getDimensionPixelSize(R.dimen.envivo_hueco_descanso);
        int margen = getResources().getDimensionPixelSize(R.dimen.espacio_16);
        boolean conDescanso = panelDescanso.getVisibility() == View.VISIBLE || avisoFin.getVisibility() == View.VISIBLE;
        int alto = conDescanso ? zonaDescanso.getHeight() : 0;
        int abajo = Math.max(Math.max(hueco, alto), huecoSistema + margen);
        int arriba = cabecera.getHeight();
        ajustarBordeInferior(conDescanso);
        if (arriba > 0 && (rv.getPaddingTop() != arriba || rv.getPaddingBottom() != abajo)) {
            // Si la lista está arriba del todo, sigue arriba: su primer ejercicio, bajo la cabecera.
            boolean enCabeza = !rv.canScrollVertically(-1);
            rv.setPadding(rv.getPaddingLeft(), arriba, rv.getPaddingRight(), abajo);
            if (enCabeza && rv.getLayoutManager() instanceof LinearLayoutManager) {
                ((LinearLayoutManager) rv.getLayoutManager()).scrollToPositionWithOffset(0, 0);
            }
        }
        // Al empezar el descanso, la serie que toca queda a la vista sobre el panel
        // (con letra grande el panel ocupa media pantalla y la tapaba).
        if (irASerie != -1 && alto > 0) {
            int pos = adapter.posicionSerie(irASerie);
            irASerie = -1;
            if (pos >= 0) rv.smoothScrollToPosition(pos);
        }
    }

    // Una vista de cristal que hace de botón: TalkBack la anuncia como tal.
    private static void comoBoton(View vista) {
        ViewCompat.setAccessibilityDelegate(vista, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull androidx.core.view.accessibility.AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
    }

    // «4.820 kg», «9 de 18»: la cifra y, más pequeño y en el texto secundario, su apoyo, en una
    // sola línea (el espacio duro los une: el texto no se parte entre ellos).
    private CharSequence cifraCon(String cifra, String apoyo) {
        android.text.SpannableStringBuilder t = new android.text.SpannableStringBuilder(cifra);
        t.append('\u00A0');
        int desde = t.length();
        t.append(apoyo.replace(' ', '\u00A0'));
        t.setSpan(new android.text.style.RelativeSizeSpan(0.6f), desde, t.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        t.setSpan(new android.text.style.ForegroundColorSpan(com.google.android.material.color.MaterialColors.getColor(
                        filaResumen, com.google.android.material.R.attr.colorOnSurfaceVariant)),
                desde, t.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return t;
    }

    // ── Sin red (GP-223) ─────────────────────────────────────

    private boolean hayRed() {
        android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
        if (cm == null) return true;
        android.net.Network red = cm.getActiveNetwork();
        android.net.NetworkCapabilities c = red != null ? cm.getNetworkCapabilities(red) : null;
        return c != null && c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // Mientras se ve la pantalla: el aviso sale al perder la red y se quita al volver.
    private void vigilarRed() {
        android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
        if (cm == null || vigilaRed != null) return;
        vigilaRed = new android.net.ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull android.net.Network red) { reloj.post(() -> pintarRed(hayRed(), false)); }
            @Override public void onLost(@NonNull android.net.Network red) { reloj.post(() -> pintarRed(hayRed(), false)); }
            @Override public void onCapabilitiesChanged(@NonNull android.net.Network red,
                                                        @NonNull android.net.NetworkCapabilities c) {
                reloj.post(() -> pintarRed(hayRed(), false));
            }
        };
        cm.registerDefaultNetworkCallback(vigilaRed);
        pintarRed(hayRed(), false);
    }

    private void dejarDeVigilarRed() {
        android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
        if (cm != null && vigilaRed != null) cm.unregisterNetworkCallback(vigilaRed);
        vigilaRed = null;
    }

    // El aviso, a la vista solo sin red. Al aparecer baja 12 dp (también al llegar del arranque).
    private void pintarRed(boolean conRed, boolean alLlegar) {
        if (avisoSinRed == null) return;
        boolean visible = avisoSinRed.getVisibility() == View.VISIBLE;
        if (conRed) {
            avisoSinRed.setVisibility(View.GONE);
        } else if (!visible || alLlegar) {
            avisoSinRed.setVisibility(View.VISIBLE);
            Movimiento.entrarUna(avisoSinRed, 0, Movimiento.ENTRA, -Movimiento.AVISO_BAJA_DP);
        }
    }

    @Override
    protected void onDestroy() {
        if (contandoVolumen != null) contandoVolumen.cancel();
        if (contandoSeries != null) contandoSeries.cancel();
        if (vaciandoBarra != null) vaciandoBarra.cancel();
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
        tvTitulo.setText(EmpezarSesion.nombre(this, s.rutinaNombre));
        tvSubtitulo.setText(s.programaNombre);
        tvSubtitulo.setVisibility(s.programaNombre == null || s.programaNombre.isEmpty() ? View.GONE : View.VISIBLE);

        pintarCifras(s);
        pintarReloj();

        adapter.pintar(s);
        pintarGuardado(s);
        pintarDescanso(s);

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

    // Volumen, series y barra de la cabecera. La primera vez se pintan tal cual; después, cada
    // cambio (marcar o desmarcar una serie) suma desde la cifra de antes (momento 24).
    private void pintarCifras(SesionEnCurso s) {
        LogicaSesion.Totales t = LogicaSesion.totales(s);
        double volumen = LogicaSesion.volumen(s);
        int progreso = t.seriesTotales == 0 ? 0 : Math.round(100f * t.seriesHechas / t.seriesTotales);
        String deTotal = getString(R.string.envivo_de_total, t.seriesTotales);
        String kg = getString(R.string.envivo_unidad_kg);
        boolean primera = seriesPintadas == -1;
        if (contandoVolumen != null) contandoVolumen.cancel();
        if (contandoSeries != null) contandoSeries.cancel();
        // Una rutina a peso corporal no lleva contador de volumen en ningún momento, con la regla de
        // Apuntar entreno (DEC-060, GP-253): sin kilos marcados ni en la última vez, solo las series.
        filaVolumen.setVisibility(LogicaSesion.volumenVisible(s) ? View.VISIBLE : View.GONE);
        if (primera || volumen == volumenPintado) {
            tvVolumen.setText(cifraCon(formatoVolumen(volumen), kg));
        } else {
            contandoVolumen = Movimiento.sumar(volumenPintado, volumen,
                    v -> tvVolumen.setText(cifraCon(formatoVolumen(v), kg)));
        }
        // «de 18» se repinta siempre: el total cambia al añadir o quitar una serie.
        if (primera || t.seriesHechas == seriesPintadas) {
            tvSeries.setText(cifraCon(String.valueOf(t.seriesHechas), deTotal));
        } else {
            contandoSeries = Movimiento.sumar(seriesPintadas, t.seriesHechas,
                    v -> tvSeries.setText(cifraCon(String.valueOf(Math.round(v)), deTotal)));
        }
        // La barra avanza (o retrocede) en BARRA ms con la curva estándar (momento 24); con las
        // animaciones quitadas, salta.
        barraProgreso.setProgreso(progreso, !primera && progreso != progresoPintado);
        volumenPintado = volumen;
        seriesPintadas = t.seriesHechas;
        progresoPintado = progreso;
    }

    // «4210» o «12.345», con el separador de miles de la lengua del móvil.
    private String formatoVolumen(double kilos) {
        return java.text.NumberFormat.getIntegerInstance(getResources().getConfiguration().getLocales().get(0))
                .format(Math.round(kilos));
    }

    private void pintarReloj() {
        SesionEnCurso s = repo.actual();
        if (s == null) return;
        tvTiempo.setText(LogicaSesion.reloj(s.inicioMs, repo.ahora()));
        // TalkBack oye los minutos, no cada segundo: cambiar la descripción a cada tic
        // es un aviso de cambio por segundo que nadie quiere oír.
        LogicaSesion.Totales t = LogicaSesion.totales(s);
        String minutos = getString(R.string.duracion_min, LogicaSesion.minutosReloj(s.inicioMs, repo.ahora()));
        double volumen = LogicaSesion.volumen(s);
        String descripcion = LogicaSesion.volumenVisible(s)
                ? getString(R.string.envivo_resumen_a11y, minutos, formatoVolumen(volumen), t.seriesHechas, t.seriesTotales)
                : getString(R.string.envivo_resumen_sin_kilos_a11y, minutos, t.seriesHechas, t.seriesTotales);
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

    // ── Descanso (GP-013) ────────────────────────────────────

    private void configurarDescanso() {
        zonaDescanso = findViewById(R.id.zonaDescanso);
        panelDescanso = findViewById(R.id.panelDescanso);
        grupoCuentaAtras = findViewById(R.id.grupoCuentaAtras);
        avisoFin = findViewById(R.id.avisoFinDescanso);
        tvDescansoTiempo = findViewById(R.id.tvDescansoTiempo);
        tvDescansoDespues = findViewById(R.id.tvDescansoDespues);
        tvFinTitulo = findViewById(R.id.tvFinTitulo);
        tvFinTexto = findViewById(R.id.tvFinTexto);
        barraDescanso = findViewById(R.id.barraDescanso);
        barraDescanso.setColoresRelleno(
                androidx.core.content.ContextCompat.getColor(this, R.color.gp_cristal_descanso_alto),
                androidx.core.content.ContextCompat.getColor(this, R.color.gp_cristal_descanso_bajo));
        btnDescansoMas = findViewById(R.id.btnDescansoMas);
        findViewById(R.id.btnDescansoMenos).setOnClickListener(v -> repo.ajustarDescanso(-LogicaDescanso.PASO_SEGUNDOS));
        btnDescansoMas.setOnClickListener(v -> repo.ajustarDescanso(LogicaDescanso.PASO_SEGUNDOS));
        findViewById(R.id.btnDescansoSaltar).setOnClickListener(v -> repo.saltarDescanso());
        findViewById(R.id.btnCerrarFin).setOnClickListener(v -> repo.cerrarFinDescanso());

        // TalkBack lee lo que queda al llegar al panel o al tocarlo: la descripción se
        // calcula en ese momento. Cambiarla a cada segundo sería un aviso por segundo.
        ViewCompat.setAccessibilityDelegate(grupoCuentaAtras, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull androidx.core.view.accessibility.AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                SesionEnCurso s = repo.actual();
                if (s != null && s.descanso != null) info.setContentDescription(descripcionDescanso(s));
            }
        });

        // En el recorrido de TalkBack el descanso va antes que la lista, aunque esté
        // abajo: es lo que corre. Cuenta atrás, −15 s, +15 s, Saltar; y el aviso del fin.
        grupoCuentaAtras.setAccessibilityTraversalBefore(R.id.btnDescansoMenos);
        findViewById(R.id.btnDescansoMenos).setAccessibilityTraversalBefore(R.id.btnDescansoMas);
        btnDescansoMas.setAccessibilityTraversalBefore(R.id.btnDescansoSaltar);
        findViewById(R.id.btnDescansoSaltar).setAccessibilityTraversalBefore(R.id.rvSesion);
        findViewById(R.id.grupoFinDescanso).setAccessibilityTraversalBefore(R.id.btnCerrarFin);
        findViewById(R.id.btnCerrarFin).setAccessibilityTraversalBefore(R.id.rvSesion);

        // La lista deja libre debajo de su final lo que ocupe el panel (con letra grande
        // crece), para que la última serie no quede tapada (ajustarHuecos).
        zonaDescanso.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> rv.post(this::ajustarHuecos));
    }

    // Cada segundo y a cada cambio: la cuenta atrás, la barra, la serie que viene y el aviso del
    // fin. El panel sube al empezar el descanso y baja al acabar o saltarlo (momento 26).
    private void pintarDescanso(@Nullable SesionEnCurso s) {
        if (panelDescanso == null) return;
        SesionEnCurso.Descanso d = s != null ? s.descanso : null;
        SesionEnCursoRepositorio.FinDescanso finDescanso = d == null ? repo.finDescanso() : null;
        boolean visible = panelDescanso.getVisibility() == View.VISIBLE && !bajandoPanel;
        avisoFin.setVisibility(finDescanso != null ? View.VISIBLE : View.GONE);
        if (finDescanso != null) ajustarBordeInferior(true);
        if (d != null) {
            bajandoPanel = false;
            panelDescanso.animate().cancel();
            panelDescanso.setTranslationY(0f);
            if (panelDescanso.getVisibility() != View.VISIBLE) {
                panelDescanso.setVisibility(View.VISIBLE);
                // El borde de abajo sube con el panel en este mismo fotograma: esperando a que
                // la zona se recoloque llegaba uno tarde y, con las animaciones quitadas, se
                // veía un instante la lista bajo el panel (revisor de UI).
                ajustarBordeInferior(true);
                Movimiento.subirPanel(panelDescanso);
                LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(s);
                if (sig != null) irASerie = sig.serieId;
            }
            long ahora = repo.ahora();
            tvDescansoTiempo.setText(LogicaSesion.tiempo(LogicaDescanso.restanteSegundos(d, ahora)));
            tvDescansoDespues.setText(TextosDescanso.siguiente(this, s));
            pintarBarraDescanso(d, ahora);
            btnDescansoMas.setEnabled(LogicaDescanso.puedeSumar(d, ahora));
            if (LogicaDescanso.restanteSegundos(d, ahora) == 10 && latidoDeFinMs != d.finMs) {
                latidoDeFinMs = d.finMs;
                Movimiento.latirNumero(tvDescansoTiempo);
            }
        } else {
            latidoDeFinMs = -1;
            barraFinMs = -1;
            if (vaciandoBarra != null) vaciandoBarra.cancel();
            if (panelDescanso.getVisibility() == View.VISIBLE && !bajandoPanel) {
                bajandoPanel = true;
                Movimiento.bajarPanel(panelDescanso, () -> bajandoPanel = false);
            }
        }
        if (finDescanso != null) {
            tvFinTitulo.setText(TextosDescanso.tituloFin(this, finDescanso));
            tvFinTexto.setText(getString(R.string.envivo_fin_texto,
                    TextosDescanso.ejercicio(this, finDescanso.ejercicio, finDescanso.ejercicioId)));
        }
    }

    // La barra se vacía en lineal hasta el final del descanso (momento 26; hasta GP-285 era un
    // aro: cambia el dibujo, no la duración ni la curva). Se lanza una vez por descanso y de
    // nuevo si cambia (±15 s); con las animaciones quitadas baja a saltos de un segundo, que es
    // el estado de cada instante y no un salto al final. Mil pasos, para que baje sin saltos.
    private void pintarBarraDescanso(SesionEnCurso.Descanso d, long ahora) {
        long queda = LogicaDescanso.restanteMs(d, ahora);
        int ahoraProgreso = d.duracionMs <= 0 ? 0 : (int) (1000L * queda / d.duracionMs);
        if (Movimiento.quieto(this)) {
            barraDescanso.setFraccion(ahoraProgreso / 1000f);
            return;
        }
        if (barraFinMs == d.finMs && barraDuracionMs == d.duracionMs) return;
        barraFinMs = d.finMs;
        barraDuracionMs = d.duracionMs;
        if (vaciandoBarra != null) vaciandoBarra.cancel();
        barraDescanso.setFraccion(ahoraProgreso / 1000f);
        if (queda <= 0) return;
        vaciandoBarra = ValueAnimator.ofInt(ahoraProgreso, 0);
        vaciandoBarra.setDuration(queda);
        vaciandoBarra.setInterpolator(Movimiento.LINEAL);
        vaciandoBarra.addUpdateListener(a -> barraDescanso.setFraccion((int) a.getAnimatedValue() / 1000f));
        vaciandoBarra.start();
    }

    // La primera vez que empieza un descanso sin lo necesario para avisar con la pantalla
    // apagada, la hoja lo explica y lo pide. Solo una vez: después queda en Ajustes.
    private void quizaPedirPermiso() {
        SesionEnCurso s = repo.actual();
        if (s == null || s.descanso == null) return;
        PreferencesManager prefs = new PreferencesManager(this);
        PermisosDescanso.Falta falta = PermisosDescanso.falta(this);
        if (falta == PermisosDescanso.Falta.NADA || prefs.getHojaPermisoDescansoVista()) return;
        prefs.saveHojaPermisoDescansoVista();

        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_permiso_descanso, null, false);
        ((TextView) v.findViewById(R.id.tvPermisoTexto)).setText(falta == PermisosDescanso.Falta.ALARMAS
                ? R.string.descanso_permiso_texto_alarmas : R.string.descanso_permiso_texto_notificaciones);
        v.findViewById(R.id.btnDarPermiso).setOnClickListener(b -> {
            hoja.dismiss();
            PermisosDescanso.activar(this);
        });
        v.findViewById(R.id.btnAhoraNo).setOnClickListener(b -> hoja.dismiss());
        hoja.setContentView(v);
        hoja.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        hoja.getBehavior().setSkipCollapsed(true);
        hoja.show();
    }

    // «Descanso: quedan 1 minuto y 42 segundos. Después: Hip thrust con barra, serie 3».
    private String descripcionDescanso(SesionEnCurso s) {
        return getString(R.string.envivo_descanso_a11y,
                TextosDescanso.duracionHablada(this, LogicaDescanso.restanteSegundos(s.descanso, repo.ahora())),
                TextosDescanso.siguiente(this, s));
    }

    // ── Lista ────────────────────────────────────────────────

    @Override
    public void marcar(long serieId, int numero) {
        LogicaSesion.Marcado m = repo.marcar(serieId, getResources().getConfiguration().getLocales().get(0));
        if (m == null) return;
        switch (m) {
            case FALTAN_REPS:
            case FALTAN_SEGUNDOS:
                // No se marca: el foco va al campo que falta y se dice qué escribir.
                SesionEnVivoAdapter.enfocarSerie(rv, serieId);
                avisar(m == LogicaSesion.Marcado.FALTAN_REPS
                        ? R.string.envivo_falta_reps : R.string.envivo_falta_segundos);
                break;
            case NO_VALIDO: avisar(R.string.envivo_no_valido); break;
            case HECHA:
                animarMarcada(serieId);
                quizaPedirPermiso();
                break;
            default: break;
        }
    }

    // Antes de que la lista se repinte: la serie marcada se anima (momento 24) y, si bate el
    // récord del ejercicio, también su fila y su aviso (momento 25).
    private void animarMarcada(long serieId) {
        SesionEnCursoRepositorio.RecordBatido r = repo.tomarRecord();
        adapter.animarMarcada(serieId, r != null ? r.ejercicioId : -1);
    }

    private void avisar(int texto) {
        aviso(texto, Snackbar.LENGTH_LONG).show();
    }

    // Un aviso al pie de la lista. La ventana ya se encoge con el teclado (adjustResize), y Material
    // sumaba además el alto del teclado como margen: con él abierto, el aviso subía hasta debajo de
    // la cabecera y tapaba el ejercicio y su ⋮ (revisor, lote 12). Sin ese margen se queda al pie.
    private Snackbar aviso(int texto, int duracion) {
        Snackbar sb = Snackbar.make(rv, texto, duracion);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(sb.getView(),
                (v, insets) -> androidx.core.view.WindowInsetsCompat.CONSUMED);
        return sb;
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
        aviso(R.string.envivo_serie_quitada, Snackbar.LENGTH_LONG)
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
                nuevos.add(new SesionEnCursoRepositorio.Nuevo(o.getInt("ejercicioId"), o.optString("nombre", null),
                        o.has("medida") ? o.optString("medida") : null));
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
            v.findViewById(R.id.btnParar).setOnClickListener(b -> {
                SesionEnCurso antes = repo.actual();
                long serieCrono = antes != null && antes.cronometro != null ? antes.cronometro.serieId : -1;
                if (repo.pararYApuntar()) {
                    animarMarcada(serieCrono);
                    quizaPedirPermiso();
                }
            });
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
        SesionEnCurso antes = repo.actual();
        long serieCrono = antes != null && antes.cronometro != null ? antes.cronometro.serieId : -1;
        CronometroSerie.Aviso a = repo.comprobarCronometro();
        switch (a) {
            case VIBRAR_CORTA: vibrar(new long[]{0, 180}); break;
            case VIBRAR_DOBLE: vibrar(new long[]{0, 180, 140, 180}); break;
            case PARAR:
                // A la hora no se apunta nada (una serie de una hora casi seguro es un
                // olvido): se dice, y el foco va al tiempo de esa serie para escribirlo.
                if (serieCrono != -1) SesionEnVivoAdapter.enfocarSerie(rv, serieCrono);
                avisar(R.string.envivo_crono_parado);
                break;
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
        intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
        intent.putExtra(ResumenSesionActivity.EXTRA_RECORDS, new ArrayList<>(sesion.getRecordsBatidos()));
        intent.putExtra(ResumenSesionActivity.EXTRA_PRIMERAS_MARCAS,
                Marcas.ejerciciosDistintos(sesion.getPrimerasMarcas()));
        if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
        startActivity(intent);
        finish();
    }
}
