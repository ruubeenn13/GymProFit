package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.alta.Alta;
import es.pmdm.gymprofit.ui.alta.BarraCapitulos;
import es.pmdm.gymprofit.ui.alta.PasoAltaFragment;
import es.pmdm.gymprofit.ui.alta.PasoDiasFragment;
import es.pmdm.gymprofit.ui.alta.PasoOpcionesFragment;
import es.pmdm.gymprofit.ui.alta.PasoPlanFragment;
import es.pmdm.gymprofit.ui.alta.PasoSobreTiFragment;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PerfilAlta;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// AltaActivity — el cuestionario del alta nueva (GP-103), de «Tu objetivo» a «Tu plan».
//
// Sustituye a Onboarding1 a 5 y al resumen. Aloja las seis pantallas como fragmentos bajo
// una cabecera fija con atrás y la barra de los cuatro capítulos, que se llena al avanzar
// (momento 5 de DEC-039). Las respuestas van al borrador de PreferencesManager según se
// eligen, y también la pantalla en la que se está: si Android cierra la app, se retoma ahí
// con lo contestado.
//
// Dos maneras de llegar:
//   · Desde la bienvenida, sin cuenta: «Tu plan» acaba en «Guarda tu plan», que la crea.
//   · Con una cuenta que entró sin onboarding (EXTRA_CUENTA_EXISTENTE): las mismas
//     preguntas, y «Tu plan» acaba en «Empezar», que sube el perfil con el PATCH, sigue
//     el programa y va a Inicio.
// ============================================================
public class AltaActivity extends BaseActivity implements Alta {

    /** La cuenta ya existe y entró sin onboarding: el plan acaba en «Empezar». */
    public static final String EXTRA_CUENTA_EXISTENTE = "cuenta_existente";

    private static final String ESTADO_PASO = "paso";

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);
    private AltaPasos.Paso paso;
    private BarraCapitulos barra;

    /** Abre el cuestionario de una cuenta que entró sin onboarding. */
    @NonNull
    public static Intent paraCuentaExistente(@NonNull Context ctx) {
        return new Intent(ctx, AltaActivity.class).putExtra(EXTRA_CUENTA_EXISTENTE, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alta);
        barra = findViewById(R.id.barraCapitulos);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> atras());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                atras();
            }
        });

        if (savedInstanceState != null) {
            // Los fragmentos los restaura el sistema; aquí solo la barra.
            paso = AltaPasos.Paso.de(savedInstanceState.getInt(ESTADO_PASO));
            barra.mostrar(paso, null, nombreCapitulo(paso));
            return;
        }
        // Donde se dejó, pero nunca más allá de la primera pregunta sin contestar.
        AltaPasos.Paso guardado = AltaPasos.Paso.de(prefsManager.getBorradorPaso());
        AltaPasos.Paso sinContestar = AltaPasos.primeroSinContestar(prefsManager.getBorradorRespuestas());
        mostrar(guardado.ordinal() <= sinContestar.ordinal() ? guardado : sinContestar, null);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(ESTADO_PASO, paso.ordinal());
    }

    @Override
    public boolean esCuentaExistente() {
        return getIntent().getBooleanExtra(EXTRA_CUENTA_EXISTENTE, false);
    }

    @Override
    public void siguiente() {
        AltaPasos.Paso despues = paso.siguiente();
        if (despues != null) {
            mostrar(despues, paso);
            return;
        }
        if (esCuentaExistente()) {
            terminarCuentaExistente();
        } else {
            startActivity(new Intent(this, GuardaPlanActivity.class));
        }
    }

    // Atrás vuelve a la pregunta anterior; desde la primera, sale del cuestionario.
    private void atras() {
        AltaPasos.Paso antes = paso.anterior();
        if (antes != null) mostrar(antes, paso);
        else finish();
    }

    private void mostrar(@NonNull AltaPasos.Paso nuevo, @Nullable AltaPasos.Paso anterior) {
        paso = nuevo;
        prefsManager.guardarBorradorPaso(nuevo.ordinal());
        barra.mostrar(nuevo, anterior, nombreCapitulo(nuevo));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedorAlta, fragmento(nuevo), nuevo.name())
                .commit();
    }

    @NonNull
    private static Fragment fragmento(@NonNull AltaPasos.Paso p) {
        switch (p) {
            case SOBRE_TI: return new PasoSobreTiFragment();
            case DIAS:     return new PasoDiasFragment();
            case PLAN:     return new PasoPlanFragment();
            default:       return PasoOpcionesFragment.de(p);
        }
    }

    // El capítulo en minúscula, para TalkBack («Capítulo 2 de 4, sobre ti»).
    private String nombreCapitulo(@NonNull AltaPasos.Paso p) {
        return getString(PasoAltaFragment.nombreCapitulo(p.capitulo)).toLowerCase(
                es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(this));
    }

    // ── Cuenta que entró sin onboarding ─────────────────────────────────────

    /**
     * «Empezar»: el perfil a la cuenta con el PATCH, y en el móvil; el programa del plan
     * con sus minutos, y a Inicio. Si el PATCH falla se dice y se queda aquí, con todo lo
     * contestado; si falla seguir el programa, queda pendiente e Inicio lo ofrece.
     */
    private void terminarCuentaExistente() {
        int id = prefsManager.getUsuarioId();
        AltaPasos.Respuestas r = prefsManager.getBorradorRespuestas();
        LoadingDialog.show(this);
        usuarioApi.patch(id, PerfilAlta.cuerpo(r)).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                if (isDestroyed()) return;
                PerfilAlta.guardarLocal(prefsManager, r, prefsManager.getUsername());
                seguirPrograma();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                LoadingDialog.hide(AltaActivity.this);
                UiFeedback.toastError(AltaActivity.this, code, message);
            }
        });
    }

    private void seguirPrograma() {
        String codigo = prefsManager.getBorradorProgramaCodigo();
        String nombre = prefsManager.getBorradorProgramaNombre();
        int minutos = prefsManager.getBorradorMinutos();
        if (codigo.isEmpty()) {
            irAInicio();
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("minutos", minutos);
        programaApi.seguir(codigo, body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                if (isDestroyed()) return;
                irAInicio();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                // El perfil ya está guardado: no se para por esto. Inicio lo ofrece.
                prefsManager.guardarProgramaPendiente(codigo, nombre, minutos);
                irAInicio();
            }
        });
    }

    private void irAInicio() {
        LoadingDialog.hide(this);
        PerfilAlta.terminar(prefsManager, prefsManager.getUsername());
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
