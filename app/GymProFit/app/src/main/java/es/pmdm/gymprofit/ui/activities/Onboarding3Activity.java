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
    private PreferencesManager prefs;

    // Aplica tema/idioma, infla el layout y configura los botones de
    // siguiente/anterior/saltar, validando peso y altura antes de avanzar.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding3);

        etPeso = findViewById(R.id.etPesoOnboarding);
        etAltura = findViewById(R.id.etAlturaOnboarding);
        chipGroupActividad = findViewById(R.id.chipGroupActividad);

        precargarBorrador();

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

            // El peso se guarda como texto ya normalizado con punto, para que no
            // dependa de si el teclado ofrecio coma.
            prefs.guardarBorradorFisico(String.valueOf(peso), altura, actividad);
            startActivity(new Intent(this, Onboarding4Activity.class));
        });

        findViewById(R.id.btnAnterior3).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar3).setOnClickListener(v -> saltarAlHome());
    }

    // Devuelve a los campos el peso, la altura y la actividad ya contestados.
    private void precargarBorrador() {
        etPeso.setText(prefs.getBorradorPeso());

        double altura = prefs.getBorradorAltura();
        if (altura > 0) etAltura.setText(String.valueOf(altura));

        switch (prefs.getBorradorActividad()) {
            case "SEDENTARIO": chipGroupActividad.check(R.id.chipSedentario); break;
            case "LIGERO":     chipGroupActividad.check(R.id.chipLigero);     break;
            case "ACTIVO":     chipGroupActividad.check(R.id.chipActivo);     break;
            default: break;
        }
    }

    // Saltar el onboarding es una DECISION del usuario, no un abandono: se marca
    // como visto para no volver a pedirselo en cada arranque (el splash lo
    // reabriria si no) y se tira el borrador, que ya no hay nada que reanudar.
    private void saltarAlHome() {
        prefs.setOnboardingCompletado(true);
        prefs.setOnboardingCompletadoParaUsuario(prefs.getUsername());
        prefs.limpiarBorradorOnboarding();
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}