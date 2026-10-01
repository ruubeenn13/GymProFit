package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// LicenciasActivity — licencias de terceros (GP-082), desde Ajustes › Legal y acerca
// de (GP-105; antes estaban al final de Acerca de).
//
// Las bibliotecas las lista play-services-oss-licenses a partir de las dependencias
// de la compilación; los iconos, la fuente y MPAndroidChart, que el plugin no ve,
// abren su texto completo.
//
// Debajo, los datos (GP-153): cada fuente con su licencia, qué trae y el enlace a su
// proyecto. free-exercise-db es la base del catálogo de ejercicios y de sus imágenes;
// wger, el nombre y la descripción en español de parte de ellos, y Open Food Facts, los
// alimentos que se importan por código de barras (los dos importadores de la API).
// ============================================================
public class LicenciasActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_licencias);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        findViewById(R.id.tvLicenciasBibliotecas).setOnClickListener(v -> {
            com.google.android.gms.oss.licenses.OssLicensesMenuActivity.setActivityTitle(
                    getString(R.string.licencias_bibliotecas));
            startActivity(new Intent(this, com.google.android.gms.oss.licenses.OssLicensesMenuActivity.class));
        });
        findViewById(R.id.tvLicenciaMaterialSymbols).setOnClickListener(v -> startActivity(LicenciaActivity.intent(this,
                R.string.licencias_material_symbols, R.string.licencias_aviso_material_symbols, R.raw.licencia_apache_2_0)));
        findViewById(R.id.tvLicenciaBarlow).setOnClickListener(v -> startActivity(LicenciaActivity.intent(this,
                R.string.licencias_barlow, R.string.licencias_aviso_barlow, R.raw.licencia_ofl_barlow)));
        findViewById(R.id.tvLicenciaMpAndroidChart).setOnClickListener(v -> startActivity(LicenciaActivity.intent(this,
                R.string.licencias_mpandroidchart, R.string.licencias_aviso_mpandroidchart, R.raw.licencia_apache_2_0)));

        fuente(R.id.filaFuenteFed, R.string.licencias_fed, R.string.licencias_fed_uso, R.string.url_fed);
        fuente(R.id.filaFuenteWger, R.string.licencias_wger, R.string.licencias_wger_uso, R.string.url_wger);
        fuente(R.id.filaFuenteOff, R.string.licencias_off, R.string.licencias_off_uso, R.string.url_off);
    }

    // Una fuente de datos: nombre y licencia, qué trae, y su proyecto en el navegador.
    private void fuente(@IdRes int fila, @StringRes int nombre, @StringRes int uso, @StringRes int url) {
        View v = findViewById(fila);
        ((TextView) v.findViewById(R.id.tvFuenteNombre)).setText(nombre);
        ((TextView) v.findViewById(R.id.tvFuenteUso)).setText(uso);
        v.setContentDescription(getString(R.string.ajustes_abre_fuera_a11y,
                getString(nombre) + ". " + getString(uso)));
        v.setOnClickListener(x -> UIHelper.abrirUrl(this, getString(url)));
    }
}
