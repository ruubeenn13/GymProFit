package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.Numeros;

// ============================================================
// Onboarding3Activity — tercer paso del asistente de onboarding.
// Recoge peso, altura y nivel de actividad física del usuario y
// avanza al siguiente paso arrastrando los datos ya introducidos.
// ============================================================
public class Onboarding3Activity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private TextInputEditText etPeso, etAltura;
    private ChipGroup chipGroupActividad;

    // Aplica tema/idioma, infla el layout y configura los botones de
    // siguiente/anterior/saltar, validando peso y altura antes de avanzar.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding3);

        etPeso = findViewById(R.id.etPesoOnboarding);
        etAltura = findViewById(R.id.etAlturaOnboarding);
        chipGroupActividad = findViewById(R.id.chipGroupActividad);

        Bundle extras = getIntent().getExtras();

        findViewById(R.id.btnSiguiente3).setOnClickListener(v -> {
            String pesoTexto   = etPeso.getText().toString().trim();
            String alturaTexto = etAltura.getText().toString().trim();

            // El peso y la altura deciden el objetivo calórico de toda la app, así
            // que aquí no vale dar por bueno cualquier texto: el teclado español
            // ofrece coma (y "1,75" cerraba la app), y una altura en metros da un
            // metabolismo basal absurdo sin que nada avise.
            Double peso = Numeros.decimal(pesoTexto, 30, 300);
            if (peso == null) {
                UIHelper.marcarError(etPeso, getString(R.string.error_peso_invalido));
                etPeso.requestFocus();
                return;
            }

            Double altura = Numeros.decimal(alturaTexto, 100, 250);
            if (altura == null) {
                // Caso típico: escribir la estatura en metros. Se sugiere el valor
                // en centímetros en vez de soltar un error seco.
                Double enMetros = Numeros.decimal(alturaTexto, 1, 2.5);
                String mensaje = (enMetros != null)
                        ? getString(R.string.error_altura_en_metros, (int) Math.round(enMetros * 100))
                        : getString(R.string.error_altura_invalida);
                UIHelper.marcarError(etAltura, mensaje);
                etAltura.requestFocus();
                return;
            }

            Intent intent = new Intent(this, Onboarding4Activity.class);

            if (extras != null) {
                intent.putExtras(extras);
            }

            // El peso viaja como texto porque el resumen lo vuelve a parsear; se
            // manda ya normalizado con punto para que no dependa del teclado.
            intent.putExtra("peso", String.valueOf(peso));
            intent.putExtra("altura", altura);

            int checkedId = chipGroupActividad.getCheckedChipId();
            String actividad;

            if (checkedId == R.id.chipSedentario) {
                actividad = "SEDENTARIO";
            } else if (checkedId == R.id.chipLigero) {
                actividad = "LIGERO";
            } else if (checkedId == R.id.chipActivo) {
                actividad = "ACTIVO";
            } else {
                actividad = "MODERADO";
            }

            intent.putExtra("actividad", actividad);
            startActivity(intent);
        });

        findViewById(R.id.btnAnterior3).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar3).setOnClickListener(v -> saltarAlHome());
    }

    // Salta el onboarding y navega directo a HomeActivity, limpiando el back stack.
    private void saltarAlHome() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        finish();
    }
}