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
                .setText(getString(R.string.acerca_version, es.pmdm.gymprofit.BuildConfig.VERSION_NAME));

        pieza(R.id.pieza1, R.drawable.ic_ms_fitness_center, R.color.gp_primary, R.string.acerca_pieza_1);
        pieza(R.id.pieza2, R.drawable.ic_ms_timer, R.color.gp_primary, R.string.acerca_pieza_2);
        pieza(R.id.pieza3, R.drawable.ic_ms_restaurant, R.color.gp_macro_proteinas, R.string.acerca_pieza_3);
        pieza(R.id.pieza4, R.drawable.ic_ms_trophy_fill, R.color.gp_gold_text, R.string.acerca_pieza_4);
        // Las dos de la derecha, separadas de las de la izquierda con la escala de espaciado.
        for (int id : new int[]{R.id.pieza2, R.id.pieza4}) {
            ((android.widget.LinearLayout.LayoutParams) findViewById(id).getLayoutParams())
                    .setMarginStart(getResources().getDimensionPixelSize(R.dimen.espacio_12));
        }

        fila(R.id.llAcercaEmail, R.drawable.ic_ms_mail, getString(R.string.acerca_autor_email_label),
                R.drawable.ic_ms_open_in_new, v -> abrirEmail());
        fila(R.id.llAcercaCompartir, R.drawable.ic_ms_support_agent, getString(R.string.acerca_compartir_label),
                R.drawable.ic_ms_chevron_right, v -> compartirViaSms());
        fila(R.id.llAcercaLicencias, R.drawable.ic_ms_license, getString(R.string.licencias_titulo),
                R.drawable.ic_ms_chevron_right, v -> startActivity(new Intent(this, LicenciasActivity.class)));
    }

    // Una pieza de «Qué hace»: un icono con su color y su frase.
    private void pieza(int id, int icono, int color, int texto) {
        android.view.View p = findViewById(id);
        android.widget.ImageView iv = p.findViewById(R.id.ivPieza);
        iv.setImageResource(icono);
        iv.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, color)));
        ((android.widget.TextView) p.findViewById(R.id.tvPieza)).setText(texto);
    }

    // Una fila del grupo de abajo: icono, texto y la flecha que dice si abre dentro o fuera.
    private void fila(int id, int icono, String titulo, int flecha, android.view.View.OnClickListener accion) {
        android.view.View f = findViewById(id);
        ((android.widget.ImageView) f.findViewById(R.id.ivIcono)).setImageResource(icono);
        ((android.widget.TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        ((android.widget.ImageView) f.findViewById(R.id.ivFlecha)).setImageResource(flecha);
        f.setContentDescription(flecha == R.drawable.ic_ms_open_in_new
                ? getString(R.string.ajustes_abre_fuera_a11y, titulo) : titulo);
        f.setOnClickListener(accion);
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

    // Abre un cliente de correo con soporte@gymprofit.app y el asunto precargados (GP-105:
    // antes era un Gmail personal).
    private void abrirEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email_soporte)});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_contacto_asunto));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_contactanos)));
    }
}
