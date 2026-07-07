package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// DetalleEjercicioActivity — pantalla de detalle de un ejercicio.
// Muestra nombre, descripción, instrucciones, estadísticas (músculo,
// nivel, calorías, equipo) y la demostración visual: si el ejercicio
// trae 2 fotogramas (free-exercise-db) se alternan en bucle, animando
// al "monigote" haciendo el ejercicio.
// ============================================================
public class DetalleEjercicioActivity extends AppCompatActivity {

    // Milisegundos entre fotogramas de la demostración animada
    private static final long FRAME_MS = 700;

    // Alternador de fotogramas de la demostración
    private final Handler frameHandler = new Handler(Looper.getMainLooper());
    private Runnable frameRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_detalle_ejercicio);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String nombre        = getIntent().getStringExtra("nombre");
        String descripcion   = getIntent().getStringExtra("descripcion");
        String instrucciones = getIntent().getStringExtra("instrucciones");
        String grupoMuscular = getIntent().getStringExtra("grupoMuscular");
        String dificultad    = getIntent().getStringExtra("dificultad");
        int calorias         = getIntent().getIntExtra("calorias", 0);
        String equipo        = getIntent().getStringExtra("equipoNecesario");
        String imagenUrl     = getIntent().getStringExtra("imagenUrl");
        String imagenUrl2    = getIntent().getStringExtra("imagenUrl2");

        poblarVistas(nombre, descripcion, instrucciones, grupoMuscular, dificultad, calorias, equipo);
        configurarDemostracion(imagenUrl, imagenUrl2);
    }

    // Muestra la demostración del ejercicio en la cabecera: con 2 fotogramas
    // se alternan en bucle (animación del monigote); con 1, imagen estática;
    // sin URL se mantiene el placeholder genérico.
    private void configurarDemostracion(String url1, String url2) {
        if (isEmpty(url1)) return;

        View placeholder = findViewById(R.id.layoutVideoPlaceholder);
        ImageView ivImagen = findViewById(R.id.ivImagenDetalle);
        ivImagen.setVisibility(View.VISIBLE);
        placeholder.setVisibility(View.GONE);
        Glide.with(this).load(url1).into(ivImagen);

        if (isEmpty(url2)) return;

        // Precarga el fotograma 2 y arranca la alternancia en bucle
        Glide.with(this).load(url2).preload();
        frameRunnable = new Runnable() {
            private boolean mostrarSegundo = true;

            @Override
            public void run() {
                // placeholder(drawable actual) evita el parpadeo entre fotogramas
                Glide.with(DetalleEjercicioActivity.this)
                        .load(mostrarSegundo ? url2 : url1)
                        .placeholder(ivImagen.getDrawable())
                        .into(ivImagen);
                mostrarSegundo = !mostrarSegundo;
                frameHandler.postDelayed(this, FRAME_MS);
            }
        };
        frameHandler.postDelayed(frameRunnable, FRAME_MS);
    }

    // Detiene la animación de fotogramas al salir de la pantalla.
    @Override
    protected void onDestroy() {
        if (frameRunnable != null) frameHandler.removeCallbacks(frameRunnable);
        super.onDestroy();
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

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
