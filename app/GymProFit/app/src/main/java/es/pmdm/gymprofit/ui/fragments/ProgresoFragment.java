package es.pmdm.gymprofit.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.activities.AjustesActivity;
import es.pmdm.gymprofit.ui.widget.FotoPerfil;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.LogrosVisibles;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// ProgresoFragment — pestaña Progreso (GP-105), según 04-progreso.png.
//
// Cabecera con el engranaje, que abre Ajustes (donde está todo lo que era la pestaña
// Perfil y el menú de tres puntos). Debajo, el perfil —avatar (la foto si la hay;
// tocarlo la cambia), nombre y «@usuario · nivel · objetivo»—, tres cifras (sesiones
// de /estadisticas, récords de /records y logros conseguidos de /logros/progreso) y
// las pestañas Récords · Logros · Medidas · Historial, con lo que eran sus pantallas.
//
// Las secciones son fragments hijos que se muestran y se ocultan; la oculta queda en
// STARTED, así que solo pide datos la que se ve.
// ============================================================
public class ProgresoFragment extends BaseFragment {

    public static final int RECORDS = 0, LOGROS = 1, MEDIDAS = 2, HISTORIAL = 3;

    private static final String KEY_SECCION = "gpf_seccion_progreso";
    private static final String[] ETIQUETAS_HIJOS = {"sec_records", "sec_logros", "sec_medidas", "sec_historial"};

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final LogroApi logroApi = ApiClient.service(LogroApi.class);

    private final TextView[] pestanas = new TextView[4];
    private int seccion = RECORDS;
    private boolean abrirPesoPendiente = false;
    private FotoPerfil fotoPerfil;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) seccion = savedInstanceState.getInt(KEY_SECCION, RECORDS);
        fotoPerfil = new FotoPerfil(this, this::requireActivity, prefsManager.getUsuarioId(), this::pintarAvatar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progreso, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViewById(R.id.btnAjustes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AjustesActivity.class)));

        View avatar = findViewById(R.id.avatarProgreso);
        avatar.setBackgroundResource(R.drawable.bg_circulo_contenedor);
        avatar.setContentDescription(getString(R.string.progreso_avatar_a11y));
        avatar.setOnClickListener(v -> fotoPerfil.elegir());
        ((TextView) avatar.findViewById(R.id.tvInicial)).setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.text_display_large) * 0.93f);

        findViewById(R.id.cifraRecords).setBackgroundResource(R.drawable.bg_cifra_progreso_oro);
        ((TextView) findViewById(R.id.cifraRecords).findViewById(R.id.tvCifra))
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.gp_gold_text));

        int[] ids = {R.id.secRecords, R.id.secLogros, R.id.secMedidas, R.id.secHistorial};
        for (int i = 0; i < ids.length; i++) {
            final int indice = i;
            pestanas[i] = findViewById(ids[i]);
            pestanas[i].setOnClickListener(v -> mostrarSeccion(indice, false));
            ViewCompat.setAccessibilityDelegate(pestanas[i], new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setRoleDescription(getString(R.string.nav_rol_pestana));
                    info.setSelected(indice == seccion);
                    info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat
                            .obtain(0, 1, indice, 1, false, indice == seccion));
                }
            });
        }
        View carril = findViewById(R.id.pestanasProgreso);
        carril.setContentDescription(getString(R.string.progreso_secciones_a11y));
        ViewCompat.setAccessibilityDelegate(carril, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, 4, false));
            }
        });

        mostrarSeccion(seccion, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        pintarPerfil();
        cargarCifras();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SECCION, seccion);
    }

    /**
     * Enseña una sección. Con {@code abrirPeso}, además abre el diálogo del peso de
     * Medidas (atajo «Anotar peso o medidas» del «+»).
     */
    public void mostrarSeccion(int nueva, boolean abrirPeso) {
        if (getView() == null) {
            seccion = nueva;
            abrirPesoPendiente = abrirPeso;
            return;
        }
        seccion = nueva;
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        Fragment visible = null;
        for (int i = 0; i < ETIQUETAS_HIJOS.length; i++) {
            Fragment f = fm.findFragmentByTag(ETIQUETAS_HIJOS[i]);
            if (i == nueva) {
                if (f == null) {
                    f = crearSeccion(i);
                    tx.add(R.id.contenedorSeccion, f, ETIQUETAS_HIJOS[i]);
                } else {
                    tx.show(f);
                }
                tx.setMaxLifecycle(f, Lifecycle.State.RESUMED);
                visible = f;
            } else if (f != null) {
                tx.hide(f);
                tx.setMaxLifecycle(f, Lifecycle.State.STARTED);
            }
        }
        tx.commitNow();

        for (int i = 0; i < pestanas.length; i++) {
            boolean esta = i == nueva;
            pestanas[i].setSelected(esta);
            pestanas[i].setTextColor(color(esta ? com.google.android.material.R.attr.colorOnPrimary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant));
            pestanas[i].setTypeface(null, esta ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

        // A letra grande el carril se desplaza (GP-128): la pestaña elegida, a la vista.
        HorizontalScrollView carril = findViewById(R.id.carrilPestanasProgreso);
        View elegida = pestanas[nueva];
        carril.post(() -> carril.smoothScrollTo(
                Math.max(0, elegida.getLeft() - (carril.getWidth() - elegida.getWidth()) / 2), 0));

        if ((abrirPeso || abrirPesoPendiente) && visible instanceof MedidasFragment) {
            abrirPesoPendiente = false;
            ((AppBarLayout) findViewById(R.id.appBarProgreso)).setExpanded(false, false);
            ((MedidasFragment) visible).abrirPeso();
        }
    }

    private static Fragment crearSeccion(int i) {
        switch (i) {
            case LOGROS:    return new LogrosFragment();
            case MEDIDAS:   return new MedidasFragment();
            case HISTORIAL: return new HistorialFragment();
            default:        return new RecordsFragment();
        }
    }

    // ── Perfil ──────────────────────────────────────────────────────────────

    private void pintarAvatar() {
        if (!isAdded()) return;
        AvatarUtils.pintar(findViewById(R.id.avatarProgreso), nombreVisible(),
                prefsManager.getUsuarioId(), color(com.google.android.material.R.attr.colorOnPrimaryContainer));
    }

    // El nombre para mostrar y, sin él, el de usuario (GP-116). El «@usuario» del
    // subtítulo es siempre el de usuario.
    private String nombreVisible() {
        return NombreVisible.de(prefsManager.getNombre(), prefsManager.getUsername());
    }

    private void pintarPerfil() {
        String username = prefsManager.getUsername();
        ((TextView) findViewById(R.id.tvNombrePerfil)).setText(nombreVisible());
        pintarAvatar();
        pintarSubtitulo(username, prefsManager.getNivel(), prefsManager.getObjetivo());

        int uid = prefsManager.getUsuarioId();
        if (uid == -1) return;
        usuarioApi.getPorId(uid).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null || !isAdded()) return;
                // Puede haberse cambiado desde otro móvil: se guarda la copia y se repinta.
                prefsManager.saveNombre(prefsManager.getUsername(), u.getNombre());
                ((TextView) findViewById(R.id.tvNombrePerfil)).setText(NombreVisible.de(u.getNombre(), u.getUsername()));
                pintarAvatar();
                pintarSubtitulo(u.getUsername(), u.getNivelExperiencia(), u.getObjetivo());
            }
            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito (GP-017): la cabecera ya enseña lo guardado en el
                // teléfono, y el fallo de red lo avisan las secciones, que piden a la vez.
            }
        });
    }

    // «@usuario · Intermedio · Ganar músculo», saltando lo que no se sepa.
    private void pintarSubtitulo(String username, @Nullable String nivel, @Nullable String objetivo) {
        List<String> partes = new ArrayList<>();
        if (username != null && !username.isEmpty()) partes.add(getString(R.string.progreso_usuario, username));
        if (nivel != null && !nivel.isEmpty()) partes.add(UIHelper.traducirNivel(requireContext(), nivel));
        if (objetivo != null && !objetivo.isEmpty()) partes.add(UIHelper.traducirObjetivo(requireContext(), objetivo));
        String s = partes.isEmpty() ? "" : partes.get(0);
        for (int i = 1; i < partes.size(); i++) s = getString(R.string.hoy_toca_separador, s, partes.get(i));
        ((TextView) findViewById(R.id.tvSubPerfil)).setText(s);
    }

    // ── Cifras ──────────────────────────────────────────────────────────────

    private void cargarCifras() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) {
            cifra(R.id.cifraSesiones, 0, R.plurals.progreso_sesiones);
            cifra(R.id.cifraRecords, 0, R.plurals.progreso_records);
            cifra(R.id.cifraLogros, 0, R.plurals.progreso_logros);
            return;
        }
        usuarioApi.getEstadisticas(uid).enqueue(new ApiCallback<UsuarioEstadisticas>() {
            @Override public void onOk(UsuarioEstadisticas e) {
                if (isAdded() && e != null) cifra(R.id.cifraSesiones, e.getTotalSesiones(), R.plurals.progreso_sesiones);
            }
            @Override public void onFail(int code, String m) { if (isAdded()) sinCifra(R.id.cifraSesiones, R.plurals.progreso_sesiones); }
        });
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override public void onOk(Records r) {
                if (isAdded()) cifra(R.id.cifraRecords, r == null ? 0 : r.getRecords().size(), R.plurals.progreso_records);
            }
            @Override public void onFail(int code, String m) { if (isAdded()) sinCifra(R.id.cifraRecords, R.plurals.progreso_records); }
        });
        logroApi.getProgreso().enqueue(new ApiCallback<List<LogroProgreso>>() {
            @Override public void onOk(List<LogroProgreso> l) {
                if (isAdded()) cifra(R.id.cifraLogros, LogrosVisibles.conseguidos(l), R.plurals.progreso_logros);
            }
            @Override public void onFail(int code, String m) { if (isAdded()) sinCifra(R.id.cifraLogros, R.plurals.progreso_logros); }
        });
    }

    private void cifra(int id, int n, @PluralsRes int etiqueta) {
        View v = findViewById(id);
        String numero = String.valueOf(n);
        String texto = getResources().getQuantityString(etiqueta, n);
        ((TextView) v.findViewById(R.id.tvCifra)).setText(numero);
        ((TextView) v.findViewById(R.id.tvCifraEtiqueta)).setText(texto);
        v.setContentDescription(getString(R.string.progreso_cifra_a11y, numero, texto));
    }

    // Sin el dato, un guion y no un cero: un cero sería afirmar algo que no se sabe.
    // El aviso de red ya lo da la sección visible, que pide a la vez.
    private void sinCifra(int id, @PluralsRes int etiqueta) {
        View v = findViewById(id);
        ((TextView) v.findViewById(R.id.tvCifra)).setText(R.string.sin_dato);
        String texto = getResources().getQuantityString(etiqueta, 2);
        ((TextView) v.findViewById(R.id.tvCifraEtiqueta)).setText(texto);
        v.setContentDescription(texto);
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
