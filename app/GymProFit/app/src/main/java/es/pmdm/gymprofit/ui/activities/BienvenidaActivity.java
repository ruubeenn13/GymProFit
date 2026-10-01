package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SaludApi;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BienvenidaActivity — la bienvenida del alta nueva (GP-103, tablero 1 del lienzo).
//
// La puerta de entrada sin sesión: la demostración en bucle de lo que hace la app, el
// idioma (abre el selector de siempre), «Empezar», que va al cuestionario, y «Ya tengo
// cuenta», que va a Entrar. Sustituye al login como primera pantalla; el login sigue
// para quien ya tiene cuenta y para cuando se cierra la sesión.
// ============================================================
public class BienvenidaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bienvenida);

        MaterialButton idioma = findViewById(R.id.btnIdioma);
        boolean ingles = "en".equals(FechaUtils.localeDeLaApp(this).getLanguage());
        idioma.setText(ingles ? R.string.bienvenida_idioma_en : R.string.bienvenida_idioma_es);
        idioma.setContentDescription(getString(R.string.bienvenida_idioma_a11y,
                getString(ingles ? R.string.ajustes_idioma_en : R.string.ajustes_idioma_es)));
        idioma.setOnClickListener(v -> mostrarDialogoIdioma());

        View empezar = findViewById(R.id.btnEmpezar);
        empezar.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.toque));
        empezar.setOnClickListener(v -> startActivity(new Intent(this, AltaActivity.class)));
        findViewById(R.id.btnYaTengoCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        if (savedInstanceState == null) {
            despertarApi();
            Movimiento.entrarUna(findViewById(R.id.cabeceraBienvenida), 0, 400, 16);
            Movimiento.entrarUna(findViewById(R.id.tvTituloBienvenida), 150, 450, 16);
            Movimiento.entrarUna(findViewById(R.id.tvTextoBienvenida), 230, 450, 16);
            Movimiento.entrarUna(empezar, 310, 450, 16);
            Movimiento.entrarUna(findViewById(R.id.btnYaTengoCuenta), 370, 450, 16);
            Movimiento.respirar(empezar, 1800);
        }
    }

    /**
     * Una llamada ligera para que la API despierte mientras se contesta el cuestionario
     * (Render gratis la duerme). No se espera ni se enseña nada: si falla, «Tu plan»
     * tiene su carga y su error, y lo vuelve a pedir.
     */
    private void despertarApi() {
        ApiClient.service(SaludApi.class).despertar().enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                // Despierta: no hay nada que enseñar.
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: solo era para despertarla, y quien la necesita de
                // verdad («Tu plan») tiene su carga, su error y su reintento.
            }
        });
    }
}
