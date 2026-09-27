package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// LicenciasActivity — licencias de terceros (GP-082), desde Ajustes › Legal y acerca
// de (GP-105; antes estaban al final de Acerca de).
//
// Las bibliotecas las lista play-services-oss-licenses a partir de las dependencias
// de la compilación; los iconos, la fuente y MPAndroidChart, que el plugin no ve,
// abren su texto completo.
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
    }
}
