package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.fragments.HistorialFragment;
import es.pmdm.gymprofit.ui.fragments.LogrosFragment;
import es.pmdm.gymprofit.ui.fragments.MedidasFragment;
import es.pmdm.gymprofit.ui.fragments.ProgresoFragment;
import es.pmdm.gymprofit.ui.fragments.RecordsFragment;

// ============================================================
// SeccionProgresoActivity — la lista completa de una sección de Progreso en su propia pantalla
// (GP-216, lote 4). Progreso es ya una sola pantalla con un resumen de cada cosa; cada «Ver todo»
// abre aquí Récords, Logros, Medidas o Historial, que son los fragments de siempre, alojados sin
// cambiarlos.
//
// También llegan aquí las notificaciones de Progreso, «Anotar peso» del «+» (Medidas con la hoja
// del peso abierta) y los enlaces de Inicio. Siempre se abre encima de MainActivity en Progreso:
// atrás vuelve a Progreso, también desde una notificación con la app cerrada (GP-097).
// ============================================================
public class SeccionProgresoActivity extends BaseActivity {

    private static final String EXTRA_SECCION = "seccion_progreso";
    private static final String EXTRA_ABRIR_PESO = "abrir_peso";
    private static final String ETIQUETA = "seccion";

    /**
     * El intent para abrir una sección.
     *
     * @param seccion   {@link ProgresoFragment#RECORDS} … {@link ProgresoFragment#HISTORIAL}.
     * @param abrirPeso solo con Medidas: abre además la hoja del peso.
     */
    @NonNull
    public static Intent intent(@NonNull Context ctx, int seccion, boolean abrirPeso) {
        return new Intent(ctx, SeccionProgresoActivity.class)
                .putExtra(EXTRA_SECCION, seccion)
                .putExtra(EXTRA_ABRIR_PESO, abrirPeso);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seccion_progreso);
        int seccion = getIntent().getIntExtra(EXTRA_SECCION, ProgresoFragment.RECORDS);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(titulo(seccion));
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Al recrearse (tema, idioma, giro) el fragment ya está y la hoja no se vuelve a abrir.
        if (savedInstanceState != null) return;
        Fragment f = crear(seccion);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.contenedorSeccion, f, ETIQUETA)
                .commitNow();
        // Sin la vista todavía, MedidasFragment la abre en cuanto carga la última medición.
        if (f instanceof MedidasFragment && getIntent().getBooleanExtra(EXTRA_ABRIR_PESO, false)) {
            ((MedidasFragment) f).abrirPeso();
        }
    }

    @NonNull
    private static Fragment crear(int seccion) {
        switch (seccion) {
            case ProgresoFragment.LOGROS:    return new LogrosFragment();
            case ProgresoFragment.MEDIDAS:   return new MedidasFragment();
            case ProgresoFragment.HISTORIAL: return new HistorialFragment();
            default:                         return new RecordsFragment();
        }
    }

    @StringRes
    private static int titulo(int seccion) {
        switch (seccion) {
            case ProgresoFragment.LOGROS:    return R.string.seccion_logros;
            case ProgresoFragment.MEDIDAS:   return R.string.seccion_medidas;
            case ProgresoFragment.HISTORIAL: return R.string.seccion_historial;
            default:                         return R.string.seccion_records;
        }
    }
}
