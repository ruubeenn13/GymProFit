package es.pmdm.gymprofit.arranque;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.Trace;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.List;

import es.pmdm.gymprofit.BuildConfig;
import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.arranque.DecisionArranque.Destino;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.PerfilRemoto;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.activities.AltaActivity;
import es.pmdm.gymprofit.ui.activities.BienvenidaActivity;
import es.pmdm.gymprofit.ui.activities.LoginActivity;
import es.pmdm.gymprofit.ui.activities.SesionEnVivoActivity;
import es.pmdm.gymprofit.utils.AnimUtils;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// PantallaArranque — el arranque de la app (GP-223): una sola pantalla de carga.
//
// La monta MainActivity cuando la abre el icono. El splash del sistema (API SplashScreen,
// con la G que entra) se queda lo justo, y debajo está esta pantalla con la misma G en el
// mismo sitio: cuando el splash se quita, no se nota. Aquí se decide adónde ir
// (DecisionArranque), se llama a la API solo para renovar la sesión, se dice qué pasa con
// los tiempos de PlanArranque y se sale con la misma salida vaya a donde vaya.
//
//   · Inicio: las pestañas se montan debajo en cuanto se sabe, y al salir el fondo se abre
//     sobre ellas y entran en cascada. Sin saltar de Activity.
//   · Otra pantalla (bienvenida, alta, Entrar, sesión en vivo): la G sale sobre el fondo
//     oscuro y la pantalla nueva llega con un fundido sobre él. Nunca se ve el fondo de
//     MainActivity, así que no hay destello ni fotograma en blanco con la app en claro.
//   · Sin red o sin respuesta: la G sube y sale el error con «Reintentar», que vuelve a
//     decidir desde el principio.
//
// La pantalla se infla con la configuración de noche y ThemeOverlay.GymProFit.Arranque: es
// oscura también con la app en claro, como el splash y el icono.
// ============================================================
public final class PantallaArranque implements PlanArranque.Pantalla {

    /** Extra de las pantallas que llegan desde aquí: llegan con fundido y su propia entrada. */
    public static final String EXTRA_DESDE_ARRANQUE = "desde_arranque";
    /**
     * Solo en depuración: renueva siempre, esperando estos ms antes de pedir, para ver
     * «Conectando…» (más de 3000) o «No conseguimos conectar» (más de 15000) sin tocar el
     * servidor. Con adb: {@code am start -n com.gymprofit.app/es.pmdm.gymprofit.ui.activities.MainActivity
     * -a android.intent.action.MAIN -c android.intent.category.LAUNCHER --el depuracion_lento_ms 5000}.
     */
    public static final String EXTRA_LENTO_DEPURACION = "depuracion_lento_ms";

    /** Lo que la pantalla pide a MainActivity. */
    public interface Anfitrion {
        /** Ya se sabe que se va a Inicio: montar las pestañas debajo, que vayan cargando. */
        void montarInicio();
        /** Las piezas de Inicio que entran en cascada al salir, en orden de lectura. */
        @NonNull List<View> piezasDeInicio();
        /** La pantalla de carga ya no está: lo que espera a Inicio a la vista (los avisos). */
        void trasArranque();
    }

    // Lo más que la salida hacia Inicio espera a que el hilo principal respire, y qué es un
    // fotograma a tiempo (dos de 60 Hz).
    private static final long ESPERA_FLUIDEZ_MS = 1000;
    private static final long FOTOGRAMA_FLUIDO_NS = 34_000_000L;

    private final AppCompatActivity act;
    private final Anfitrion anfitrion;
    private final PreferencesManager prefs;
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable tic = this::alTic;
    private final Runnable ticTraspaso = this::alTicTraspaso;
    private final long lentoDepuracion;
    private final boolean quieto;

    private ViewGroup raiz;
    private VistaG g;
    private TextView tvConectando, tvErrorTitulo, tvErrorTexto;
    private View bloqueError, huecoGError, btnReintentar;
    private FondoArranque fondo;
    @Nullable private WindowInsetsControllerCompat barras;
    private boolean barrasClarasAntes, navegacionClaraAntes, contrasteAntes;

    private PlanArranque plan;
    // Cuándo se quita la copia del splash, y la copia misma mientras sigue debajo.
    private TraspasoSplash traspaso;
    @Nullable private SplashScreenViewProvider copia;
    @Nullable private ApiClient.Renovacion renovacion;
    @Nullable private Destino destino;
    private boolean sesionEnVivo;
    // El splash del sistema ya no está (o nunca estuvo); y una salida que lo esperaba.
    private boolean splashFuera;
    private boolean salidaPendiente;
    private boolean montarPendiente;
    // Dónde y cómo de grande puso la G el traspaso (casada con el icono de la copia).
    private float baseX, baseY;
    // Inicio ya montado (sus pestañas existen), y «Conectando…» que esperaba a verse.
    private boolean montado;
    private boolean conectandoPendiente;
    @Nullable private CharSequence tituloAntes;
    private final java.util.List<View> apartadas = new java.util.ArrayList<>();
    private boolean terminada;

    /**
     * Si esta apertura de MainActivity es un arranque: la abre el icono (o un acceso del
     * sistema con MAIN y LAUNCHER) y no es una recreación. Desde dentro de la app,
     * MainActivity se abre sin esa categoría y va directa a sus pestañas, como siempre.
     */
    public static boolean esArranque(@NonNull Intent intent, @Nullable android.os.Bundle estado) {
        return estado == null && Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER);
    }

    public PantallaArranque(@NonNull AppCompatActivity act, @NonNull Anfitrion anfitrion) {
        this.act = act;
        this.anfitrion = anfitrion;
        this.prefs = new PreferencesManager(act);
        this.quieto = Movimiento.quieto(act);
        this.lentoDepuracion = BuildConfig.DEBUG
                ? act.getIntent().getLongExtra(EXTRA_LENTO_DEPURACION, 0L) : 0L;
    }

    /**
     * Pone la pantalla de carga encima de todo, ata el splash del sistema al plan y decide.
     * MainActivity la llama tras setContentView; el splash ya está instalado.
     */
    public void iniciar(@NonNull SplashScreen splash) {
        long ahora = SystemClock.uptimeMillis();
        // Desde que se tocó el icono: el arranque del proceso si es de esta apertura (en frío),
        // o este momento si el proceso ya vivía (en caliente).
        long proceso = Process.getStartUptimeMillis();
        long tocado = ahora - proceso < 5000 ? proceso : ahora;

        inflar();
        plan = PlanArranque.alTocar(tocado, quieto, this);
        splash.setKeepOnScreenCondition(() -> plan.mantenerSplash(SystemClock.uptimeMillis()));
        traspaso = TraspasoSplash.nuevo(
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                new TraspasoSplash.Pantalla() {
                    @Override public void quitarCopia() { quitarCopiaDelSplash(); }
                    @Override public void splashFuera() { alQuitarseSplash(); }
                });
        // El traspaso, a una réplica propia (decisión del propietario tras el revisor). Sin esta
        // escucha, el sistema funde su ventana con la de la app y la G se atenúa en cada
        // arranque. Con ella, Android pasa el splash a una copia; su icono es una superficie
        // del sistema por encima de la ventana, y al quitar la copia la shell lo suelta en el
        // acto, sin esperar a ningún fotograma de la app. Aquí la pantalla de carga se pone
        // encima de la copia con su G estática (el último fotograma del AVD, con su mismo
        // viewport) en el sitio del icono, y la copia se queda debajo hasta que el fotograma
        // que pinta la réplica en su sitio se ha entregado y ha pasado otro (TraspasoSplash).
        // Quitarla antes dejaba a la vista la copia sin icono si ese fotograma tardaba.
        splash.setOnExitAnimationListener(proveedor -> {
            Trace.beginSection("gp:traspaso.encima");
            copia = proveedor;
            // En la traza (Perfetto), lo que vive la copia debajo de la réplica.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Trace.beginAsyncSection("gp:copia", 0);
            // La librería puede haber puesto ya las barras del tema: con la app en claro, la de
            // navegación salía clara sobre la pantalla oscura.
            barrasOscuras();
            pasarAEncima(proveedor.getIconView());
            traspaso.encima(SystemClock.uptimeMillis());
            programarTicTraspaso();
            Trace.endSection();
        });
        decidir();
    }

    // La copia del splash fuera, cuando TraspasoSplash lo dice.
    private void quitarCopiaDelSplash() {
        if (copia == null) return;
        Trace.beginSection("gp:traspaso.quitarCopia");
        copia.remove();
        copia = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Trace.endAsyncSection("gp:copia", 0);
        Trace.endSection();
        // Al quitarse, la librería vuelve a poner las barras del tema.
        barrasOscuras();
    }

    private void programarTicTraspaso() {
        reloj.removeCallbacks(ticTraspaso);
        if (terminada || traspaso == null) return;
        long siguiente = traspaso.siguienteTic();
        if (siguiente >= 0) reloj.postAtTime(ticTraspaso, siguiente);
    }

    private void alTicTraspaso() {
        traspaso.tic(SystemClock.uptimeMillis());
        programarTicTraspaso();
    }

    // El fotograma que pinta la réplica en su sitio se ha entregado: se avisa, y del primero
    // que venga después también.
    private void alEntregarseReplica() {
        Trace.beginSection("gp:traspaso.entregado");
        traspaso.entregado(SystemClock.uptimeMillis());
        Trace.endSection();
        android.view.Choreographer.getInstance().postFrameCallback(t -> {
            if (act.isDestroyed()) return;
            Trace.beginSection("gp:traspaso.fotograma");
            traspaso.fotograma(SystemClock.uptimeMillis());
            programarTicTraspaso();
            Trace.endSection();
        });
    }

    /**
     * Pone la pantalla de carga encima de todo (en la decoración de la ventana, por encima de
     * la copia del splash y del contenido) y casa su G con el icono de la copia: el mismo
     * centro en pantalla. El tamaño es el del icono del splash (288 dp, la caja de la G
     * estática): en Android 12 o más, {@code iconView} mide solo el círculo de 192 dp y el
     * icono se pinta a 288 a su alrededor; escalar a sus límites dejaba la G al 67 % unos
     * fotogramas (medido a cámara lenta). Sin icono, o de tamaño 0, la G se queda donde la
     * pone su layout: en el centro de la ventana.
     */
    private void pasarAEncima(@Nullable View icono) {
        ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
        ViewGroup padre = (ViewGroup) raiz.getParent();
        if (padre != decor) {
            if (padre != null) padre.removeView(raiz);
            decor.addView(raiz, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            // Fuera del contenido, lo que se aparta de TalkBack es el contenido entero.
            View contenido = act.findViewById(android.R.id.content);
            for (View v : apartadas) {
                v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                if (v instanceof ViewGroup) ((ViewGroup) v).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            }
            apartadas.clear();
            contenido.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            if (contenido instanceof ViewGroup) {
                ((ViewGroup) contenido).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            }
            apartadas.add(contenido);
        } else {
            raiz.bringToFront();
        }
        boolean conIcono = icono != null && icono.getWidth() > 0 && icono.getHeight() > 0;
        int[] enPantalla = new int[2];
        if (conIcono) icono.getLocationOnScreen(enPantalla);
        float cx = conIcono ? enPantalla[0] + icono.getWidth() / 2f : 0f;
        float cy = conIcono ? enPantalla[1] + icono.getHeight() / 2f : 0f;
        // La réplica aún no se ha medido en su sitio nuevo: se casa justo antes de pintarla, y
        // se pide aviso cuando se entregue ese mismo fotograma (Android 10 o más; antes, la
        // copia ya se quita en el acto y no hace falta).
        OneShotPreDrawListener.add(raiz, () -> {
            if (conIcono) {
                int[] g0 = new int[2];
                g.getLocationOnScreen(g0);
                // Sin traslación ni escala: la posición del layout.
                float gx = g0[0] - g.getTranslationX() + g.getWidth() / 2f;
                float gy = g0[1] - g.getTranslationY() + g.getHeight() / 2f;
                baseX = cx - gx;
                baseY = cy - gy;
                g.setTranslationX(baseX);
                g.setTranslationY(baseY);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                raiz.getViewTreeObserver().registerFrameCommitCallback(this::alEntregarseReplica);
            }
        });
    }

    /** MainActivity se destruye: se deja de esperar a la API y al reloj. */
    public void destruir() {
        terminada = true;
        reloj.removeCallbacksAndMessages(null);
        if (renovacion != null) renovacion.cancelar();
    }

    // ── Montar ───────────────────────────────────────────────────────────────

    private void inflar() {
        Configuration noche = new Configuration(act.getResources().getConfiguration());
        noche.uiMode = (noche.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
        // El tema de la app entero (en noche resuelve al oscuro) y encima el overlay. Solo el
        // overlay no basta: en Android 7 el tema base no se copia a unos recursos con otra
        // configuración, y los componentes de Material se quedaban sin tema (se cerraba).
        ContextThemeWrapper oscuro = new ContextThemeWrapper(act, R.style.Theme_GymProFit);
        oscuro.applyOverrideConfiguration(noche);
        oscuro.getTheme().applyStyle(R.style.ThemeOverlay_GymProFit_Arranque, true);

        ViewGroup contenido = act.findViewById(android.R.id.content);
        raiz = (ViewGroup) LayoutInflater.from(oscuro).inflate(R.layout.view_arranque, contenido, false);
        fondo = new FondoArranque(ContextCompat.getColor(oscuro, R.color.gp_background));
        raiz.setBackground(fondo);
        contenido.addView(raiz);
        // Lo de debajo (las pestañas, la barra) no existe para TalkBack ni para el teclado
        // mientras está la pantalla de carga: TalkBack se iba a la pestaña «Inicio», que no se
        // ve (revisor). Se devuelve al salir hacia Inicio.
        for (int i = 0; i < contenido.getChildCount(); i++) {
            View v = contenido.getChildAt(i);
            if (v == raiz) continue;
            v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            if (v instanceof ViewGroup) ((ViewGroup) v).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            apartadas.add(v);
        }
        // Se traga los toques sin ser un control: pulsable, pedía un nombre (revisor).
        raiz.setOnTouchListener((v, e) -> true);
        // TalkBack dice el título de la ventana al abrirla: «Cargando GymProFit».
        tituloAntes = act.getTitle();
        act.setTitle(R.string.arranque_cargando);

        g = raiz.findViewById(R.id.vistaG);
        tvConectando = raiz.findViewById(R.id.tvConectando);
        bloqueError = raiz.findViewById(R.id.bloqueError);
        huecoGError = raiz.findViewById(R.id.huecoGError);
        tvErrorTitulo = raiz.findViewById(R.id.tvErrorTitulo);
        tvErrorTexto = raiz.findViewById(R.id.tvErrorTexto);
        btnReintentar = raiz.findViewById(R.id.btnReintentar);
        btnReintentar.setOnClickListener(v -> reintentar());

        // La G, centrada en la ventana entera: sin padding. Solo el botón se aparta de la barra.
        int abajo = act.getResources().getDimensionPixelSize(R.dimen.arranque_boton_abajo);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) btnReintentar.getLayoutParams();
            lp.bottomMargin = abajo + b.bottom;
            btnReintentar.setLayoutParams(lp);
            return insets;
        });

        // Iconos de las barras claros sobre el fondo oscuro, aunque la app esté en claro.
        barras = WindowCompat.getInsetsController(act.getWindow(), act.getWindow().getDecorView());
        contrasteAntes = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q
                || act.getWindow().isNavigationBarContrastEnforced();
        barrasClarasAntes = barras.isAppearanceLightStatusBars();
        navegacionClaraAntes = barras.isAppearanceLightNavigationBars();
        barrasOscuras();
    }

    // Iconos claros en las barras del sistema, y sin el velo de contraste (claro en el tema claro).
    private void barrasOscuras() {
        if (barras == null) return;
        barras.setAppearanceLightStatusBars(false);
        barras.setAppearanceLightNavigationBars(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            act.getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    // ── Decidir ──────────────────────────────────────────────────────────────

    private void decidir() {
        String token = prefs.getToken();
        String refresh = prefs.getRefreshToken();
        String usuario = prefs.getUsername();
        boolean conSesion = prefs.haySesion();
        sesionEnVivo = conSesion && haySesionEnVivo();
        DecisionArranque.Estado e = new DecisionArranque.Estado()
                .sesion(conSesion)
                .onboarding(usuario != null && !usuario.isEmpty() && prefs.isOnboardingCompletadoParaUsuario(usuario))
                .altaEmpezada(prefs.altaEmpezada())
                .refresh(refresh != null && !refresh.isEmpty())
                .red(hayRed())
                .sesionEnVivo(sesionEnVivo)
                // En depuración con el servidor lento forzado, siempre se renueva.
                .caducaMs(lentoDepuracion > 0 ? null : DecisionArranque.caducidadMs(token))
                .ahoraMs(System.currentTimeMillis());
        Destino d = DecisionArranque.decidir(e);
        if (d != Destino.RENOVAR) {
            llegar(d);
            return;
        }
        renovacion = ApiClient.renovarAlArrancar(lentoDepuracion, (codigo, cuerpo) -> {
            if (terminada) return;
            llegar(DecisionArranque.trasRenovar(DecisionArranque.clasificar(codigo, cuerpo), sesionEnVivo));
        });
        programarTic();
    }

    private void llegar(@NonNull Destino d) {
        destino = d;
        if (d == Destino.INICIO || d == Destino.ALTA_CUENTA) {
            // El perfil de la API se cruza con el del móvil sin esperar (GP-111), ya con la
            // sesión renovada: antes, una petición con el token caducado lo habría renovado
            // a la vez que el arranque.
            PerfilRemoto.alArrancar(act);
        }
        if (d == Destino.INICIO) {
            // Con el splash aún encima, Inicio se monta en cuanto se vaya: montar las pestañas
            // tiene el hilo principal ocupado cientos de ms, y si coincide con el traspaso del
            // splash se ve un hueco sin la G (medido en el emulador). Debajo de la G quieta no
            // se nota, y la salida espera a que Inicio respire (cuandoHayaFluidez).
            if (splashFuera) montar();
            else montarPendiente = true;
        }
        plan.decidido(SystemClock.uptimeMillis());
        programarTic();
    }

    private void montar() {
        Trace.beginSection("gp:arranque.montarInicio");
        anfitrion.montarInicio();
        montado = true;
        Trace.endSection();
    }

    // Montar Inicio cuando ya se haya dibujado un fotograma sin el splash: el que quita su
    // copia. Si se montaba en el mismo paso, ese fotograma se retrasaba lo que tarda el
    // montaje y lo último a la vista era la copia sin icono: la G desaparecía (revisor).
    private void montarTrasUnFotograma() {
        android.view.Choreographer.getInstance().postFrameCallback(t -> reloj.post(() -> {
            if (!terminada) montar();
        }));
    }

    private boolean hayRed() {
        ConnectivityManager cm = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        Network red = cm.getActiveNetwork();
        NetworkCapabilities c = red != null ? cm.getNetworkCapabilities(red) : null;
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private boolean haySesionEnVivo() {
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(act);
        repo.usarCuenta(prefs.getUsuarioId());
        return repo.hay();
    }

    // ── Reloj ────────────────────────────────────────────────────────────────

    private void programarTic() {
        reloj.removeCallbacks(tic);
        if (terminada || plan == null) return;
        long siguiente = plan.siguienteTic(SystemClock.uptimeMillis());
        if (siguiente >= 0) reloj.postAtTime(tic, siguiente);
    }

    private void alTic() {
        if (terminada) return;
        plan.tic(SystemClock.uptimeMillis());
        programarTic();
    }

    // ── Lo que pide el plan ──────────────────────────────────────────────────

    @Override
    public void empezarBrillo() {
        g.empezarBrillo();
    }

    @Override
    public void mostrarConectando() {
        // Con el splash aún encima (un móvil lento), TalkBack diría «Conectando» antes que
        // «Cargando GymProFit», y no se vería: espera a que se vaya (revisor).
        if (!splashFuera) {
            conectandoPendiente = true;
            return;
        }
        tvConectando.setText(R.string.arranque_conectando);
        // TalkBack dice «Conectando», sin los puntos suspensivos; es región viva.
        tvConectando.setContentDescription(act.getString(R.string.arranque_conectando_a11y));
        Movimiento.aparecerConectando(tvConectando);
    }

    @Override
    public void agotado() {
        if (renovacion != null) renovacion.cancelar();
        destino = Destino.NO_CONECTA;
        salir();
    }

    @Override
    public void salir() {
        if (!splashFuera) {
            // El splash del sistema se va ahora; la salida, cuando su copia se haya quitado. Si
            // no avisa (no llegó a enseñarlo), se sale igual al rato (TraspasoSplash).
            salidaPendiente = true;
            traspaso.esperar(SystemClock.uptimeMillis());
            programarTicTraspaso();
            return;
        }
        ejecutarSalida();
    }

    private void alQuitarseSplash() {
        if (splashFuera) return;
        splashFuera = true;
        if (montarPendiente) {
            montarPendiente = false;
            montarTrasUnFotograma();
        }
        if (conectandoPendiente) {
            conectandoPendiente = false;
            mostrarConectando();
        }
        if (salidaPendiente) {
            salidaPendiente = false;
            // Tras un fotograma con la réplica ya pintada en su sitio: si la salida empezaba en
            // el mismo paso, su primer fotograma no tenía aún la G casada.
            android.view.Choreographer.getInstance().postFrameCallback(t -> reloj.post(this::ejecutarSalida));
        }
    }

    // ── Salir ────────────────────────────────────────────────────────────────

    private void ejecutarSalida() {
        if (terminada || destino == null) return;
        Trace.beginSection("gp:arranque.salida");
        try {
            ejecutarSalidaA(destino);
        } finally {
            Trace.endSection();
        }
    }

    private void ejecutarSalidaA(@NonNull Destino destino) {
        switch (destino) {
            case SIN_CONEXION:
            case NO_CONECTA:
                mostrarError(destino);
                return;
            case INICIO:
                // Inicio acaba de montar sus pestañas: hasta que el hilo principal no respira,
                // un fotograma dura más que la salida entera y no se vería. También sin
                // animaciones: sin esperar se veía Inicio vacío (revisor).
                cuandoHayaFluidez(this::salirAInicio);
                return;
            default:
                salirA(destino);
        }
    }

    /**
     * Ejecuta {@code accion} cuando lleguen dos fotogramas seguidos a tiempo, o al cabo de
     * ESPERA_FLUIDEZ_MS. Medido en el emulador: al montar Inicio hubo fotogramas de 700 a
     * 875 ms, y la salida de 300 ms cabía entera en uno. En un móvil que no se atasca, se
     * cumple en dos fotogramas (unos 33 ms).
     */
    private void cuandoHayaFluidez(@NonNull Runnable accion) {
        long limite = SystemClock.uptimeMillis() + ESPERA_FLUIDEZ_MS;
        android.view.Choreographer.getInstance().postFrameCallback(new android.view.Choreographer.FrameCallback() {
            long anterior = -1;
            int seguidos = 0;

            @Override
            public void doFrame(long ahoraNanos) {
                if (terminada) return;
                // Hasta que Inicio no esté montado, no cuenta: aún falta lo pesado.
                if (montado && anterior > 0 && ahoraNanos - anterior < FOTOGRAMA_FLUIDO_NS) seguidos++;
                else seguidos = 0;
                anterior = ahoraNanos;
                if (montado && (seguidos >= 2 || SystemClock.uptimeMillis() >= limite)) {
                    accion.run();
                } else {
                    android.view.Choreographer.getInstance().postFrameCallback(this);
                }
            }
        });
    }

    private void salirAInicio() {
        if (terminada) return;
        terminada = true;
        g.pararBrillo();
        tvConectando.setVisibility(View.INVISIBLE);
        List<View> piezas = anfitrion.piezasDeInicio();
        Movimiento.sacarG(g, fondo::setApertura, () -> {
            ViewGroup padre = (ViewGroup) raiz.getParent();
            if (padre != null) padre.removeView(raiz);
            for (View v : apartadas) {
                v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                if (v instanceof ViewGroup) ((ViewGroup) v).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            }
            act.setTitle(tituloAntes);
            if (barras != null) {
                barras.setAppearanceLightStatusBars(barrasClarasAntes);
                barras.setAppearanceLightNavigationBars(navegacionClaraAntes);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    act.getWindow().setNavigationBarContrastEnforced(contrasteAntes);
                }
            }
            anfitrion.trasArranque();
        });
        Movimiento.cascada(piezas);
    }

    private void salirA(@NonNull Destino d) {
        terminada = true;
        reloj.removeCallbacksAndMessages(null);
        g.pararBrillo();
        tvConectando.setVisibility(View.INVISIBLE);
        Intent i = intencion(d);
        Movimiento.sacarG(g, null, () -> {
            act.startActivity(i);
            // En Android 14 o más el fundido lo monta GymProFitApp en la pantalla que llega.
            if (i.getBooleanExtra(EXTRA_DESDE_ARRANQUE, false)
                    && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                AnimUtils.fundidoDesdeArranque(act);
            }
            act.finish();
        });
    }

    @NonNull
    private Intent intencion(@NonNull Destino d) {
        switch (d) {
            case BIENVENIDA:
                return new Intent(act, BienvenidaActivity.class);
            case ALTA:
                return new Intent(act, AltaActivity.class);
            case ALTA_CUENTA:
                return AltaActivity.paraCuentaExistente(act);
            case SESION_EN_VIVO:
                return new Intent(act, SesionEnVivoActivity.class).putExtra(EXTRA_DESDE_ARRANQUE, true);
            case ENTRAR_DESACTIVADA:
            case ENTRAR:
            default:
                // La sesión ya no vale: fuera los tokens. El correo o usuario se queda, aparte,
                // para que Entrar salga ya escrito.
                UtilREST.clearToken();
                prefs.cerrarSesion();
                AvatarUtils.olvidar();
                Intent login = new Intent(act, LoginActivity.class).putExtra(EXTRA_DESDE_ARRANQUE, true);
                if (d == Destino.ENTRAR_DESACTIVADA) {
                    login.putExtra(LoginActivity.EXTRA_CUENTA_DESACTIVADA, true);
                } else {
                    login.putExtra(LoginActivity.EXTRA_SESION_CERRADA, true);
                }
                return login;
        }
    }

    // ── Errores ──────────────────────────────────────────────────────────────

    private void mostrarError(@NonNull Destino tipo) {
        boolean sinRed = tipo == Destino.SIN_CONEXION;
        g.pararBrillo();
        tvConectando.setVisibility(View.INVISIBLE);
        tvErrorTitulo.setText(sinRed ? R.string.arranque_sin_conexion_titulo : R.string.arranque_no_conecta_titulo);
        tvErrorTexto.setText(sinRed ? R.string.arranque_sin_conexion_texto : R.string.arranque_no_conecta_texto);
        bloqueError.setVisibility(View.VISIBLE);
        btnReintentar.setVisibility(View.VISIBLE);
        // La ventana se llama como el error: si TalkBack vuelve a ella, no dice «Cargando».
        act.setTitle(tvErrorTitulo.getText());
        g.setContentDescription(null);
        g.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // Cuando el bloque ya está medido, la G sube a su hueco de 72 dp.
        OneShotPreDrawListener.add(bloqueError, () -> {
            float centroG = g.getTop() + g.getHeight() / 2f;
            float centroHueco = bloqueError.getTop() + huecoGError.getTop() + huecoGError.getHeight() / 2f;
            Movimiento.moverG(g, 0f, centroHueco - centroG, (float) Movimiento.G_ERROR_DP / Movimiento.G_DP);
            // Cuando la G ya ha subido: si no, al subir pasaba por encima del título (revisor).
            Movimiento.entrar(quieto ? 0 : Movimiento.G_SUBE, tvErrorTitulo, tvErrorTexto, btnReintentar);
            enfocar(tvErrorTitulo);
        });
    }

    private void reintentar() {
        bloqueError.setVisibility(View.GONE);
        btnReintentar.setVisibility(View.GONE);
        g.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        g.setContentDescription(act.getString(R.string.arranque_cargando));
        act.setTitle(R.string.arranque_cargando);
        Movimiento.moverG(g, baseX, baseY, 1f);
        enfocar(g);
        destino = null;
        renovacion = null;
        plan = PlanArranque.reintento(SystemClock.uptimeMillis(), quieto, this);
        decidir();
    }

    // El foco de TalkBack a la vista, cuando ya está en pantalla. Sin TalkBack no hace nada.
    private static void enfocar(@NonNull View v) {
        v.post(() -> v.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null));
    }
}
