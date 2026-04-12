package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class CrearRutinaActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etDescripcion, etDuracion, etCalorias, etNumEjercicios;
    private ChipGroup chipGroupNivel;
    private MaterialButton btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado(prefsManager);

        setContentView(R.layout.activity_crear_rutina);

        configurarToolbar();
        inicializarVistas();
        configurarBotonGuardar();
    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void inicializarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etDuracion = findViewById(R.id.etDuracion);
        etCalorias = findViewById(R.id.etCalorias);
        etNumEjercicios = findViewById(R.id.etNumEjercicios);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);
        btnGuardar = findViewById(R.id.btnGuardar);
    }

    private void configurarBotonGuardar() {
        btnGuardar.setOnClickListener(v -> {
            if (!validarCampos()) return;

            Intent resultado = new Intent();
            resultado.putExtra("nombre", etNombre.getText().toString().trim());
            resultado.putExtra("descripcion", etDescripcion.getText().toString().trim());
            resultado.putExtra("nivel", obtenerNivelSeleccionado());
            resultado.putExtra("duracion", Integer.parseInt(etDuracion.getText().toString().trim()));
            resultado.putExtra("calorias", Integer.parseInt(etCalorias.getText().toString().trim()));
            resultado.putExtra("numEjercicios", Integer.parseInt(etNumEjercicios.getText().toString().trim()));

            UIHelper.mostrarToastExito(this, getString(R.string.rutinas_guardada_exito));

            setResult(RESULT_OK, resultado);
            finish();
        });
    }

    private boolean validarCampos() {
        if (etNombre.getText().toString().trim().isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            etNombre.requestFocus();
            return false;
        }
        if (etDescripcion.getText().toString().trim().isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            etDescripcion.requestFocus();
            return false;
        }
        if (etDuracion.getText().toString().trim().isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            etDuracion.requestFocus();
            return false;
        }
        if (etCalorias.getText().toString().trim().isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            etCalorias.requestFocus();
            return false;
        }
        if (etNumEjercicios.getText().toString().trim().isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            etNumEjercicios.requestFocus();
            return false;
        }
        return true;
    }

    private String obtenerNivelSeleccionado() {
        int checkedId = chipGroupNivel.getCheckedChipId();
        if (checkedId == R.id.chipIntermedio) return "Intermedio";
        if (checkedId == R.id.chipAvanzado) return "Avanzado";
        return "Principiante";
    }

    private void aplicarIdiomaGuardado(PreferencesManager prefsManager) {
        String savedLanguage = prefsManager.getLanguage();
        if (!savedLanguage.isEmpty()) {
            Locale locale = new Locale(savedLanguage);
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration config = resources.getConfiguration();
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }
}