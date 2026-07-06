package es.pmdm.gymprofit.ui.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// DetalleEjercicioActivity — pantalla de detalle de un ejercicio.
// Muestra nombre, descripción, instrucciones, estadísticas (músculo,
// nivel, calorías, equipo) y, si existe, el vídeo demostrativo del
// ejercicio recibido por extras del Intent.
// ============================================================
public class DetalleEjercicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_detalle_ejercicio);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        int id               = getIntent().getIntExtra("id", -1);
        String nombre        = getIntent().getStringExtra("nombre");
        String descripcion   = getIntent().getStringExtra("descripcion");
        String instrucciones = getIntent().getStringExtra("instrucciones");
        String grupoMuscular = getIntent().getStringExtra("grupoMuscular");
        String dificultad    = getIntent().getStringExtra("dificultad");
        int calorias         = getIntent().getIntExtra("calorias", 0);
        String equipo        = getIntent().getStringExtra("equipoNecesario");

        poblarVistas(nombre, descripcion, instrucciones, grupoMuscular, dificultad, calorias, equipo);
        configurarVideo(id);
    }

    // Rellena las vistas de la pantalla con los datos del ejercicio,
    // ocultando las secciones (equipamiento/descripción/instrucciones) vacías.
    private void poblarVistas(String nombre, String descripcion, String instrucciones,
                              String grupoMuscular, String dificultad, int calorias, String equipo) {
        ((TextView) findViewById(R.id.tvNombreDetalle)).setText(nombre != null ? nombre : "");

        // Grupo y nivel traducidos al idioma de la app (la API envía los enums crudos).
        ((TextView) findViewById(R.id.tvStatMusculo)).setText(
                !isEmpty(grupoMuscular) ? es.pmdm.gymprofit.utils.UIHelper.traducirGrupoMuscular(this, grupoMuscular) : "—");
        ((TextView) findViewById(R.id.tvStatNivel)).setText(
                !isEmpty(dificultad) ? es.pmdm.gymprofit.utils.UIHelper.traducirNivel(this, dificultad) : "—");
        ((TextView) findViewById(R.id.tvStatCalorias)).setText(
                calorias > 0 ? calorias + " kcal" : "—");

        if (!isEmpty(equipo)) {
            ((TextView) findViewById(R.id.tvStatEquipamiento)).setText(equipo);
        } else {
            findViewById(R.id.rowEquipamiento).setVisibility(View.GONE);
        }

        TextView tvDesc = findViewById(R.id.tvDescripcion);
        if (!isEmpty(descripcion)) {
            tvDesc.setText(descripcion);
        } else {
            findViewById(R.id.cardDescripcion).setVisibility(View.GONE);
        }

        TextView tvInstr = findViewById(R.id.tvInstrucciones);
        if (!isEmpty(instrucciones)) {
            tvInstr.setText(instrucciones);
        } else {
            findViewById(R.id.cardInstrucciones).setVisibility(View.GONE);
        }
    }

    // Busca un vídeo local (res/raw/video_<id>) para el ejercicio y, si
    // existe, lo reproduce en bucle sustituyendo al placeholder.
    private void configurarVideo(int ejercicioId) {
        if (ejercicioId <= 0) return;
        // Resuelve dinámicamente el recurso raw según el id del ejercicio.
        int resId = getResources().getIdentifier("video_" + ejercicioId, "raw", getPackageName());
        if (resId == 0) return;

        View placeholder = findViewById(R.id.layoutVideoPlaceholder);
        VideoView videoView = findViewById(R.id.videoView);

        placeholder.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + resId));

        MediaController mc = new MediaController(this);
        mc.setAnchorView(videoView);
        videoView.setMediaController(mc);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.start();
        });
        videoView.requestFocus();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
