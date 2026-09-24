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
// Onboarding2Activity — segundo paso del asistente de onboarding.
// Recoge nombre, edad y sexo del usuario (el correo no: GP-083), precargando los
// valores recibidos del paso anterior, y avanza al siguiente paso.
// ============================================================
public class Onboarding2Activity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private TextInputEditText etNombre, etEdad;
    private ChipGroup chipGroupSexo;
    private PreferencesManager prefs;

    // Aplica tema/idioma, infla el layout, precarga los datos recibidos
    // y configura los botones de siguiente/anterior/saltar.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding2);

        etNombre = findViewById(R.id.etNombreOnboarding);
        etEdad = findViewById(R.id.etEdadOnboarding);
        chipGroupSexo = findViewById(R.id.chipGroupSexo);

        precargarBorrador();

        findViewById(R.id.btnSiguiente2).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                UIHelper.marcarError(etNombre, getString(R.string.error_campo_requerido));
                etNombre.requestFocus();
                return;
            }

            // La edad es opcional, pero si se escribe tiene que ser creible: entra
            // en el calculo del metabolismo basal. Ademas, un numero larguisimo
            // reventaba Integer.parseInt y cerraba la app.
            String edadTexto = etEdad.getText().toString().trim();
            int edad = 0;
            if (!edadTexto.isEmpty()) {
                Integer leida = Numeros.entero(edadTexto, 10, 120);
                if (leida == null) {
                    UIHelper.marcarError(etEdad, getString(R.string.error_edad_invalida));
                    etEdad.requestFocus();
                    return;
                }
                edad = leida;
            }

            String sexo = (chipGroupSexo.getCheckedChipId() == R.id.chipMujer) ? "MUJER" : "HOMBRE";

            prefs.guardarBorradorDatos(nombre, edad, sexo);
            startActivity(new Intent(this, Onboarding3Activity.class));
        });

        findViewById(R.id.btnAnterior2).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar2).setOnClickListener(v -> saltarAlHome());
    }

    // Devuelve a los campos lo que el usuario ya habia contestado, venga de la
    // pantalla de acceso o de una sesion anterior del asistente.
    private void precargarBorrador() {
        etNombre.setText(prefs.getBorradorNombre());

        int edad = prefs.getBorradorEdad();
        if (edad > 0) etEdad.setText(String.valueOf(edad));

        if ("MUJER".equals(prefs.getBorradorSexo())) chipGroupSexo.check(R.id.chipMujer);
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