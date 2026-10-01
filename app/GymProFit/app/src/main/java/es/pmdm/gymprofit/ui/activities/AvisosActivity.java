package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.widget.FilaAviso;
import es.pmdm.gymprofit.utils.AvisosCuenta;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// AvisosActivity — «¿Te avisamos?», después de crear la cuenta (GP-103 y GP-112,
// tablero 12 del lienzo; decisión 10).
//
// Se dice qué se manda y se elige ANTES del permiso de Android: el fin de cada descanso,
// que va con la sesión, y los tres interruptores de la cuenta con sus valores de serie
// (entrenar sí, comidas no, progreso sí). Arriba, una notificación de ejemplo que baja
// como una de verdad (momento 11).
//
// Los dos botones guardan los interruptores con el PATCH. «Activar avisos» pide además
// el permiso del sistema (Android 13+); «Ahora no», no, y Inicio tampoco lo pedirá
// después por su cuenta. Las dos acaban en Inicio: nada aquí bloquea el alta.
// ============================================================
public class AvisosActivity extends BaseActivity {

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private FilaAviso entrenar, comidas, progreso;
    @Nullable private ValueAnimator bucle;
    private ActivityResultLauncher<String> pedirPermiso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avisos);

        new FilaAviso(findViewById(R.id.filaAvisoDescanso), R.drawable.ic_ms_timer,
                R.string.avisos_descanso, R.string.avisos_descanso_sub, false);
        entrenar = new FilaAviso(findViewById(R.id.filaAvisoEntrenar), R.drawable.ic_ms_notifications_active,
                R.string.avisos_entrenar, R.string.avisos_entrenar_sub, true);
        comidas = new FilaAviso(findViewById(R.id.filaAvisoComidas), R.drawable.ic_ms_restaurant,
                R.string.avisos_comidas, R.string.avisos_comidas_sub, true);
        progreso = new FilaAviso(findViewById(R.id.filaAvisoProgreso), R.drawable.ic_ms_trophy,
                R.string.avisos_progreso, R.string.avisos_progreso_sub, true);
        AvisosCuenta.Estado deSerie = AvisosCuenta.Estado.deSerie();
        boolean girada = savedInstanceState != null;
        entrenar.setActiva(girada ? savedInstanceState.getBoolean("entrenar") : deSerie.entrenar);
        comidas.setActiva(girada ? savedInstanceState.getBoolean("comidas") : deSerie.comidas);
        progreso.setActiva(girada ? savedInstanceState.getBoolean("progreso") : deSerie.progreso);
        FilaAviso.AlCambiar nada = activa -> { /* se guardan al pulsar un botón */ };
        entrenar.alCambiar(nada);
        comidas.alCambiar(nada);
        progreso.alCambiar(nada);

        pedirPermiso = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                concedido -> irAInicio());

        View activar = findViewById(R.id.btnActivarAvisos);
        activar.setOnClickListener(v -> {
            guardar();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                irAInicio();
            }
        });
        findViewById(R.id.btnAhoraNo).setOnClickListener(v -> {
            guardar();
            irAInicio();
        });

        if (!girada) {
            Movimiento.entrar(100, findViewById(R.id.tvTituloAvisos), findViewById(R.id.tvTextoAvisos),
                    findViewById(R.id.cardTiposAviso), findViewById(R.id.filaAvisoDescanso),
                    findViewById(R.id.filaAvisoEntrenar), findViewById(R.id.filaAvisoComidas),
                    findViewById(R.id.filaAvisoProgreso), activar, findViewById(R.id.btnAhoraNo));
            Movimiento.respirar(activar, 1400);
        }
        bajarEjemplo();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("entrenar", entrenar.isActiva());
        out.putBoolean("comidas", comidas.isActiva());
        out.putBoolean("progreso", progreso.isActiva());
    }

    @Override
    protected void onDestroy() {
        if (bucle != null) bucle.cancel();
        super.onDestroy();
    }

    /**
     * Momento 11: la notificación de ejemplo baja con rebote, se queda, sube y vuelve a
     * bajar, en un bucle de 5 s; su icono zumba al llegar. Con «Quitar animaciones» se
     * queda quieta, a la vista.
     */
    private void bajarEjemplo() {
        View carta = findViewById(R.id.cardAvisoEjemplo);
        View icono = findViewById(R.id.iconoAvisoEjemplo);
        if (Movimiento.quieto(this)) return;
        bucle = ValueAnimator.ofFloat(0f, 1f);
        bucle.setDuration(Movimiento.AVISO);
        bucle.setStartDelay(400);
        bucle.setRepeatCount(ValueAnimator.INFINITE);
        bucle.setInterpolator(Movimiento.LINEAL);
        bucle.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float alto = carta.getHeight() + carta.getTop();
            float y, alfa;
            if (t < 0.10f) {
                float f = Movimiento.REBOTE.getInterpolation(t / 0.10f);
                y = -1.5f * alto * (1f - f);
                alfa = Math.min(1f, f);
            } else if (t < 0.78f) {
                y = 0f;
                alfa = 1f;
            } else if (t < 0.88f) {
                float f = (t - 0.78f) / 0.10f;
                y = -1.5f * alto * f;
                alfa = 1f - f;
            } else {
                y = -1.5f * alto;
                alfa = 0f;
            }
            carta.setTranslationY(y);
            carta.setAlpha(alfa);
            // Zumba entre el 10 y el 20 %: -14°, 12°, -8°, 5° y quieto.
            float r = 0f;
            if (t >= 0.10f && t < 0.20f) {
                float[] g = {0f, -14f, 12f, -8f, 5f, 0f};
                float p = (t - 0.10f) / 0.02f;
                int i = Math.min(4, (int) p);
                r = g[i] + (g[i + 1] - g[i]) * (p - i);
            }
            icono.setRotation(r);
        });
        bucle.start();
    }

    // Los tres interruptores a la cuenta. Si falla, se dice y se sigue: se cambian
    // luego en Ajustes › Notificaciones, y la cuenta conserva los de serie.
    private void guardar() {
        prefsManager.setAvisosPreguntados(prefsManager.getUsername());
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;
        AvisosCuenta.Estado e = new AvisosCuenta.Estado(entrenar.isActiva(), comidas.isActiva(), progreso.isActiva());
        usuarioApi.patch(id, AvisosCuenta.cuerpo(e)).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void r) {
                // Guardados: no hay nada que enseñar, la pantalla ya se ha ido.
            }

            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(getApplicationContext(), getString(R.string.avisos_error_guardar));
            }
        });
    }

    private void irAInicio() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
