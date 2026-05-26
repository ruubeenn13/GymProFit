package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class AcercaDeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        aplicarIdioma(prefs);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acerca_de);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.llAcercaEmail).setOnClickListener(v -> abrirEmail());
    }

    private void abrirEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rubenjuancandela06@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_contacto_asunto));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_contactanos)));
    }

    private void aplicarIdioma(PreferencesManager prefs) {
        String lang = prefs.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
