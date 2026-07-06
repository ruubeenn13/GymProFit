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

        findViewById(R.id.llAcercaEmail).setOnClickListener(v -> abrirEmail());
        findViewById(R.id.llAcercaWeb).setOnClickListener(v -> abrirWeb());
        findViewById(R.id.llAcercaDial).setOnClickListener(v -> abrirDial());
        findViewById(R.id.llAcercaCompartir).setOnClickListener(v -> compartirViaSms());
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

    // Abre la web oficial de GymProFit en el navegador
    private void abrirWeb() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gymprofit.infinityfree.me/login"));
        startActivity(intent);
    }

    // Abre el marcador telefónico con el número de contacto precargado
    private void abrirDial() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+34600000000"));
        startActivity(intent);
    }
}
