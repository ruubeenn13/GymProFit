package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// AcercaDeActivity — pantalla "Acerca de" con datos de contacto y enlaces externos
// Permite contactar por email, abrir la web, marcar un teléfono o compartir
// la app por SMS eligiendo un contacto. El selector del sistema (ACTION_PICK)
// concede acceso temporal a la URI elegida, así que NO requiere READ_CONTACTS.
// No usa API, solo Intents del sistema.
// ============================================================
public class AcercaDeActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Launcher que recibe el contacto elegido en el selector del sistema
    private ActivityResultLauncher<Intent> seleccionContactoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acerca_de);

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

        // La versión se lee del build: un texto fijo se quedaba atrás en cada entrega.
        ((android.widget.TextView) findViewById(R.id.tvVersion))
                .setText(getString(R.string.splash_version, es.pmdm.gymprofit.BuildConfig.VERSION_NAME));
        findViewById(R.id.llAcercaEmail).setOnClickListener(v -> abrirEmail());
        findViewById(R.id.llAcercaCompartir).setOnClickListener(v -> compartirViaSms());
        // La política se abre en el navegador: no viaja dentro del APK (GP-008).
        findViewById(R.id.llAcercaPrivacidad).setOnClickListener(v ->
                es.pmdm.gymprofit.utils.UIHelper.abrirUrl(this, getString(R.string.url_privacidad)));

        configurarLicencias();
    }

    // Comparte la app por SMS: abre el selector de contactos del sistema. El picker
    // concede acceso temporal a la URI elegida, así que no hace falta READ_CONTACTS.
    private void compartirViaSms() {
        abrirSelectorContacto();
    }

    // Abre el selector de contactos del sistema para elegir el destinatario del SMS
    private void abrirSelectorContacto() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        seleccionContactoLauncher.launch(intent);
    }

    // Consulta el ContentResolver para extraer el número de teléfono del contacto seleccionado
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

    // Abre la app de SMS con el número y el texto de compartir precargados
    private void enviarSms(String numero) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numero));
        intent.putExtra("sms_body", getString(R.string.acerca_compartir_texto));
        startActivity(intent);
    }

    // Abre un cliente de correo con el email de contacto y asunto precargados
    private void abrirEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rubenjuancandela06@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_contacto_asunto));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_contactanos)));
    }

    /**
     * Licencias de terceros (GP-082). Las bibliotecas las lista play-services-oss-licenses
     * a partir de las dependencias de la compilación; los iconos, la fuente y
     * MPAndroidChart, que el plugin no ve, abren su texto completo.
     */
    private void configurarLicencias() {
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
