package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class AcercaDeActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> permisosContactosLauncher;
    private ActivityResultLauncher<Intent> seleccionContactoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        aplicarIdioma(prefs);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acerca_de);

        permisosContactosLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) abrirSelectorContacto();
                else Toast.makeText(this, getString(R.string.permiso_contactos_denegado), Toast.LENGTH_SHORT).show();
            }
        );

        seleccionContactoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String numero = obtenerNumero(result.getData().getData());
                    if (numero != null) enviarSms(numero);
                }
            }
        );

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.llAcercaEmail).setOnClickListener(v -> abrirEmail());
        findViewById(R.id.llAcercaWeb).setOnClickListener(v -> abrirWeb());
        findViewById(R.id.llAcercaDial).setOnClickListener(v -> abrirDial());
        findViewById(R.id.llAcercaCompartir).setOnClickListener(v -> compartirViaSms());
    }

    private void compartirViaSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            abrirSelectorContacto();
        } else {
            permisosContactosLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void abrirSelectorContacto() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        seleccionContactoLauncher.launch(intent);
    }

    private String obtenerNumero(Uri contactUri) {
        if (contactUri == null) return null;
        try (Cursor cursor = getContentResolver().query(contactUri,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return null;
    }

    private void enviarSms(String numero) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numero));
        intent.putExtra("sms_body", getString(R.string.acerca_compartir_texto));
        startActivity(intent);
    }

    private void abrirEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rubenjuancandela06@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_contacto_asunto));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_contactanos)));
    }

    private void abrirWeb() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gymprofit.infinityfree.me/login"));
        startActivity(intent);
    }

    private void abrirDial() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+34600000000"));
        startActivity(intent);
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
