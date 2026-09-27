package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.ui.fragments.EntrenarFragment;
import es.pmdm.gymprofit.ui.fragments.InicioFragment;
import es.pmdm.gymprofit.ui.fragments.NutricionFragment;
import es.pmdm.gymprofit.ui.fragments.ProgresoFragment;
import es.pmdm.gymprofit.ui.widget.BarraNavegacion;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.NavTabs;
import es.pmdm.gymprofit.utils.TiempoRelativo;

// ============================================================
// MainActivity — la navegación principal (GP-105).
//
// Cuatro pestañas —Inicio, Entrenar, Nutrición y Progreso— en un ViewPager2 que ya
// no se desliza de lado: solo se cambia tocando la barra, y el cambio es un fundido
// corto, no un desplazamiento. El «+» central de la barra abre la hoja «¿Qué quieres
// hacer?» sobre la pestaña que haya.
//
// Atrás, como en la 1.0.2 (GP-097): desde cualquier pestaña vuelve a Inicio y desde
// Inicio sale; con la hoja abierta, la cierra.
//
// Guarda lo poco que la hoja necesita saber de las pestañas —qué rutina toca hoy y
// el último peso— para enseñarlo solo cuando ya está cargado: la hoja no pide nada.
// ============================================================
public class MainActivity extends BaseActivity {

    // Clave para recordar la pestaña activa al recrearse la Activity (cambio de tema/idioma).
    private static final String KEY_TAB = "gpf_tab_activa";

    private ViewPager2 pager;
    private BarraNavegacion barra;
    private View velo, hoja;
    private boolean hojaAbierta = false;

    // Lo que ya se sabe para los subtítulos de la hoja. null = todavía no.
    @Nullable private Rutina hoyToca;
    @Nullable private String ultimoPeso;

    private final MedicionApi medicionApi = ApiClient.service(MedicionApi.class);

    // Atrás fuera de Inicio vuelve a Inicio (GP-097). En Inicio queda desactivado para
    // que atrás salga de la app con la animación del sistema, que se puede cancelar.
    private final OnBackPressedCallback atrasAInicio = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            irATab(NavTabs.INICIO);
        }
    };

    // Con la hoja abierta, atrás la cierra. Se registra después y por eso manda.
    private final OnBackPressedCallback atrasCierraHoja = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            cerrarAcciones();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        barra = findViewById(R.id.barra);
        velo  = findViewById(R.id.veloAcciones);
        hoja  = findViewById(R.id.hojaAcciones);

        aplicarInsets();

        pager.setAdapter(new TabsAdapter(this));
        pager.setUserInputEnabled(false);
        // Las cuatro vivas: cambiar de pestaña no re-infla; cada una recarga al volver.
        pager.setOffscreenPageLimit(NavTabs.TOTAL - 1);
        // Cada pestaña que se crea fuera de la vista nace ya apartada del foco.
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull androidx.fragment.app.FragmentManager fm,
                                                      @NonNull Fragment f, @NonNull View v,
                                                      @Nullable Bundle estado) {
                        pager.post(() -> ocultarPestanasNoVisibles(pager.getCurrentItem()));
                    }
                }, false);

        barra.setOyente(new BarraNavegacion.Oyente() {
            @Override public void onPestana(int pestana) {
                cerrarAcciones();
                irATab(pestana);
            }
            @Override public void onAcciones() {
                if (hojaAbierta) cerrarAcciones(); else abrirAcciones();
            }
        });

        velo.setOnClickListener(v -> cerrarAcciones());
        findViewById(R.id.accionEntrenar).setOnClickListener(v -> accionEntrenar());
        findViewById(R.id.accionComida).setOnClickListener(v -> accionComida());
        findViewById(R.id.accionPeso).setOnClickListener(v -> accionPeso());

        int tabPorDefecto = getIntent().getIntExtra(NavTabs.EXTRA_TAB, NavTabs.INICIO);
        int tab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_TAB, tabPorDefecto) : tabPorDefecto;
        pager.setCurrentItem(tab, false);
        pintarPestana(tab);
        getOnBackPressedDispatcher().addCallback(this, atrasAInicio);
        getOnBackPressedDispatcher().addCallback(this, atrasCierraHoja);

        if (savedInstanceState == null) aplicarDestino(getIntent());
        cargarUltimoPeso();
        pedirPermisoNotificaciones();
    }

    // Las notificaciones abren esta misma pantalla en una pestaña concreta.
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        cerrarAcciones();
        if (intent.hasExtra(NavTabs.EXTRA_TAB)) {
            irATab(intent.getIntExtra(NavTabs.EXTRA_TAB, NavTabs.INICIO));
        }
        aplicarDestino(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pager != null) outState.putInt(KEY_TAB, pager.getCurrentItem());
    }

    // Esta pantalla gestiona sus márgenes del sistema (InsetsUtils la deja): arriba y a
    // los lados, la raíz; abajo, la barra, que pinta su fondo bajo la barra de gestos.
    private void aplicarInsets() {
        View raiz = findViewById(R.id.raizMain);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets muesca = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(Math.max(barras.left, muesca.left), Math.max(barras.top, muesca.top),
                    Math.max(barras.right, muesca.right), 0);
            barra.aplicarInsetInferior(Math.max(barras.bottom, muesca.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(raiz);
    }

    // ── Pestañas ─────────────────────────────────────────────────────────────

    /**
     * Cambia de pestaña con un fundido corto. Con «Quitar animaciones» la duración del
     * sistema es 0 y el cambio es inmediato.
     */
    public void irATab(int index) {
        if (pager == null || index < 0 || index >= NavTabs.TOTAL) return;
        if (index != pager.getCurrentItem()) {
            pager.animate().cancel();
            pager.setAlpha(0f);
            pager.setCurrentItem(index, false);
            pager.animate().alpha(1f)
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .start();
        }
        pintarPestana(index);
    }

    private void pintarPestana(int index) {
        barra.setActiva(index);
        atrasAInicio.setEnabled(NavTabs.atrasVuelveAInicio(index));
        pager.post(() -> ocultarPestanasNoVisibles(index));
    }

    // Las cuatro pestañas siguen vivas en el pager: sin esto el foco de teclado (Tab) y
    // el de accesibilidad entraban en las que no se ven. Con -1 se apartan todas (la
    // hoja del «+» abierta tapa la que se ve).
    private void ocultarPestanasNoVisibles(int visible) {
        for (int i = 0; i < NavTabs.TOTAL; i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag("f" + i);
            View v = f != null ? f.getView() : null;
            if (v == null) continue;
            boolean esta = i == visible;
            ((android.view.ViewGroup) v).setDescendantFocusability(esta
                    ? android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS : android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            v.setImportantForAccessibility(esta ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
    }

    // Lleva a la sección de Progreso que pida el intent (notificaciones).
    private void aplicarDestino(Intent intent) {
        if (!intent.hasExtra(NavTabs.EXTRA_SECCION_PROGRESO)) return;
        int seccion = intent.getIntExtra(NavTabs.EXTRA_SECCION_PROGRESO, ProgresoFragment.RECORDS);
        irATab(NavTabs.PROGRESO);
        pager.post(() -> {
            ProgresoFragment p = progreso();
            if (p != null) p.mostrarSeccion(seccion, false);
        });
    }

    @Nullable
    private ProgresoFragment progreso() {
        // FragmentStateAdapter etiqueta cada página como «f» + su id, que es la posición.
        Fragment f = getSupportFragmentManager().findFragmentByTag("f" + NavTabs.PROGRESO);
        return f instanceof ProgresoFragment ? (ProgresoFragment) f : null;
    }

    // ── Hoja del «+» ──────────────────────────────────────────────────────────

    private void abrirAcciones() {
        if (hojaAbierta) return;
        hojaAbierta = true;
        pintarSubtitulos();

        long dur = getResources().getInteger(android.R.integer.config_shortAnimTime);
        velo.animate().cancel();
        velo.setAlpha(0f);
        velo.setVisibility(View.VISIBLE);
        velo.animate().alpha(1f).setDuration(dur).start();

        hoja.animate().cancel();
        hoja.setVisibility(View.VISIBLE);
        hoja.post(() -> {
            hoja.setTranslationY(hoja.getHeight() + getResources().getDisplayMetrics().density * 16);
            hoja.animate().translationY(0f).setDuration(dur)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        });

        barra.setAccionesAbiertas(true);
        atrasCierraHoja.setEnabled(true);
        // Lo de detrás deja de existir para TalkBack mientras la hoja está abierta.
        ocultarPestanasNoVisibles(-1);
        View titulo = findViewById(R.id.tvTituloAcciones);
        titulo.post(() -> titulo.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null));
    }

    /** Cierra la hoja del «+» si está abierta. */
    public void cerrarAcciones() {
        if (!hojaAbierta) return;
        hojaAbierta = false;

        long dur = getResources().getInteger(android.R.integer.config_shortAnimTime);
        velo.animate().cancel();
        velo.animate().alpha(0f).setDuration(dur)
                .withEndAction(() -> velo.setVisibility(View.GONE)).start();
        hoja.animate().cancel();
        hoja.animate().translationY(hoja.getHeight() + getResources().getDisplayMetrics().density * 16)
                .setDuration(dur)
                .withEndAction(() -> hoja.setVisibility(View.GONE)).start();

        barra.setAccionesAbiertas(false);
        atrasCierraHoja.setEnabled(false);
        ocultarPestanasNoVisibles(pager.getCurrentItem());
    }

    // Subtítulos de la hoja: solo lo que ya se sabe; lo demás, oculto.
    private void pintarSubtitulos() {
        android.widget.TextView subEntrenar = findViewById(R.id.tvSubEntrenar);
        android.widget.TextView subComida = findViewById(R.id.tvSubComida);
        android.widget.TextView subPeso = findViewById(R.id.tvSubPeso);

        mostrarSub(subEntrenar, hoyToca != null
                ? getString(R.string.accion_entrenar_sub, hoyToca.getNombre()) : null);
        mostrarSub(subComida, getString(R.string.accion_comida_sub,
                getString(ComidaQueToca.enFrase(ComidaQueToca.ahora()))));
        mostrarSub(subPeso, ultimoPeso);

        // TalkBack lee cada fila de una vez: título y, si lo hay, subtítulo.
        describir(R.id.accionEntrenar, R.string.accion_entrenar, subEntrenar);
        describir(R.id.accionComida, R.string.accion_comida, subComida);
        describir(R.id.accionPeso, R.string.accion_peso, subPeso);
    }

    private static void mostrarSub(android.widget.TextView tv, @Nullable String texto) {
        tv.setText(texto);
        tv.setVisibility(texto == null ? View.GONE : View.VISIBLE);
    }

    private void describir(int filaId, int tituloId, android.widget.TextView sub) {
        String titulo = getString(tituloId);
        findViewById(filaId).setContentDescription(sub.getVisibility() == View.VISIBLE
                ? titulo + ". " + sub.getText() : titulo);
    }

    /** Inicio avisa de qué rutina toca hoy (o null si no hay rutinas propias). */
    public void publicarHoyToca(@Nullable Rutina rutina) {
        hoyToca = rutina;
    }

    /** Medidas avisa de la última medición con peso, para la hoja. */
    public void publicarUltimoPeso(@Nullable MedicionCorporal m) {
        if (m == null || m.getPeso() <= 0) {
            ultimoPeso = null;
            return;
        }
        String cuando = TiempoRelativo.texto(this, m.getFecha());
        String kilos = getString(R.string.unidad_kg_decimal, m.getPeso());
        ultimoPeso = cuando == null ? null : getString(R.string.accion_peso_sub, kilos, cuando);
    }

    // La última medición se pide una vez al entrar, en silencio: si no llega, la fila
    // de la hoja sale sin subtítulo, que es exactamente lo que se quiere.
    private void cargarUltimoPeso() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1 || prefsManager.isGuest()) return;
        medicionApi.getOrdenadas(uid).enqueue(new ApiCallback<List<MedicionCorporal>>() {
            @Override
            public void onOk(List<MedicionCorporal> lista) {
                if (isFinishing()) return;
                for (MedicionCorporal m : lista != null ? lista : java.util.Collections.<MedicionCorporal>emptyList()) {
                    if (m.getPeso() > 0) { publicarUltimoPeso(m); return; }
                }
            }
            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito (GP-017): es solo el subtítulo de un atajo y sin él
                // la fila se entiende igual. Un fallo de red ya lo avisará la pestaña que
                // el usuario tenga delante.
            }
        });
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    // Empezar entrenamiento → registrar sesión con la rutina de «Hoy toca» ya elegida.
    private void accionEntrenar() {
        cerrarAcciones();
        if (!verificarAccesoRegistrado()) return;
        Intent i = new Intent(this, RegistrarSesionActivity.class);
        if (hoyToca != null) i.putExtra(RegistrarSesionActivity.EXTRA_RUTINA_ID, hoyToca.getId());
        startActivity(i);
    }

    // Registrar comida → añadir alimento, hoy, en la comida que toca por la hora.
    private void accionComida() {
        cerrarAcciones();
        if (!verificarAccesoRegistrado()) return;
        Intent i = new Intent(this, AnadirAlimentoActivity.class);
        i.putExtra("tipoComida", ComidaQueToca.ahora());
        i.putExtra("comidaId", -1);
        i.putExtra("fecha", TiempoRelativo.hoy());
        startActivity(i);
    }

    // Anotar peso o medidas → Progreso › Medidas con el diálogo del peso abierto.
    private void accionPeso() {
        cerrarAcciones();
        if (!verificarAccesoRegistrado()) return;
        irATab(NavTabs.PROGRESO);
        pager.post(() -> {
            ProgresoFragment p = progreso();
            if (p != null) p.mostrarSeccion(ProgresoFragment.MEDIDAS, true);
        });
    }

    // Solicita el permiso de notificaciones (Android 13+) una sola vez al entrar.
    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
    }

    // Adapter del pager: instancia el fragment de cada pestaña.
    private static final class TabsAdapter extends FragmentStateAdapter {
        TabsAdapter(FragmentActivity fa) { super(fa); }

        @Override public int getItemCount() { return NavTabs.TOTAL; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case NavTabs.ENTRENAR:  return new EntrenarFragment();
                case NavTabs.NUTRICION: return new NutricionFragment();
                case NavTabs.PROGRESO:  return new ProgresoFragment();
                default:                return new InicioFragment();
            }
        }
    }
}
