package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// LicenciaActivity — texto completo de una licencia de terceros (GP-082).
//
// Sirve para lo que no sale de las dependencias de Gradle y por tanto no
// recoge play-services-oss-licenses: los Material Symbols y la fuente Barlow,
// que viajan dentro del APK como recursos, y MPAndroidChart, que se instala
// desde JitPack con un POM sin licencia declarada. El texto de la licencia va
// en inglés en los dos idiomas porque es el texto legal; lo traducido es el
// título y la línea de copyright que lo precede.
// ============================================================
public class LicenciaActivity extends AppCompatActivity {

    private static final String EXTRA_TITULO = "titulo";
    private static final String EXTRA_AVISO = "aviso";
    private static final String EXTRA_TEXTO = "texto";

    /**
     * Intent para enseñar una licencia.
     *
     * @param titulo nombre del componente, para la barra
     * @param aviso  copyright y licencia en una frase, en el idioma de la app
     * @param texto  recurso raw con el texto legal completo
     */
    public static Intent intent(Context ctx, @StringRes int titulo, @StringRes int aviso, @RawRes int texto) {
        return new Intent(ctx, LicenciaActivity.class)
                .putExtra(EXTRA_TITULO, titulo)
                .putExtra(EXTRA_AVISO, aviso)
                .putExtra(EXTRA_TEXTO, texto);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new PreferencesManager(this).applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licencia);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getIntent().getIntExtra(EXTRA_TITULO, R.string.licencias_titulo));
        toolbar.setNavigationOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.tvAvisoLicencia)).setText(getIntent().getIntExtra(EXTRA_AVISO, 0));
        ((TextView) findViewById(R.id.tvTextoLicencia)).setText(leer(getIntent().getIntExtra(EXTRA_TEXTO, 0)));
    }

    // Texto del recurso raw. Si no se puede leer es un fallo de empaquetado, no
    // del usuario: se deja la pantalla con el aviso de copyright, que ya cumple.
    private String leer(@RawRes int id) {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = getResources().openRawResource(id);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = r.readLine()) != null) sb.append(linea).append('\n');
        } catch (IOException e) {
            return "";
        }
        return sb.toString();
    }
}
