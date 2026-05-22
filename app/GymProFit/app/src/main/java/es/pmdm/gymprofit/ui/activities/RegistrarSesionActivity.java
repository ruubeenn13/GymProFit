package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class RegistrarSesionActivity extends AppCompatActivity {

    private Spinner spRutina;
    private TextInputEditText etDuracion, etNotas;
    private TextView tvCaloriasCalculadas;
    private View cardCalorias;
    private RatingBar ratingBar;
    private PreferencesManager prefsManager;

    private int caloriasCalculadas = 0;
    private final List<Rutina> rutinas = new ArrayList<>();
    private final List<String> rutinaOpciones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_registrar_sesion);

        spRutina             = findViewById(R.id.spRutina);
        etDuracion           = findViewById(R.id.etDuracion);
        etNotas              = findViewById(R.id.etNotas);
        tvCaloriasCalculadas = findViewById(R.id.tvCaloriasCalculadas);
        cardCalorias         = findViewById(R.id.cardCalorias);
        ratingBar            = findViewById(R.id.ratingBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarSesion());

        cargarRutinas();
    }

    private void cargarRutinas() {
        int usuarioId = prefsManager.getUsuarioId();
        rutinaOpciones.clear();
        rutinaOpciones.add(getString(R.string.sesiones_sin_rutina));

        final List<Rutina> predefinidas = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(2);

        API.getRutinasPredefinidas(new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try { predefinidas.addAll(UtilJSONParser.parseRutinaList(response)); }
                catch (JSONException ignored) {}
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
            @Override public void onError(String message, int statusCode) {
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
        });

        API.getRutinasDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try { rutinas.addAll(UtilJSONParser.parseRutinaList(response)); }
                catch (JSONException ignored) {}
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
            @Override public void onError(String message, int statusCode) {
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
        });
    }

    private void combinarYMostrar(List<Rutina> predefinidas) {
        List<Rutina> todas = new ArrayList<>(predefinidas);
        todas.addAll(rutinas);
        rutinas.clear();
        rutinas.addAll(todas);
        for (Rutina r : todas) rutinaOpciones.add(r.getNombre());
        runOnUiThread(this::actualizarSpinner);
    }

    private void actualizarSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, rutinaOpciones);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRutina.setAdapter(spinnerAdapter);
        spRutina.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= rutinas.size()) {
                    calcularCaloriasRutina(rutinas.get(position - 1).getId());
                } else {
                    caloriasCalculadas = 0;
                    cardCalorias.setVisibility(View.GONE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void calcularCaloriasRutina(int rutinaId) {
        API.getRutinaEjerciciosPorRutina(rutinaId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    JSONArray arr = new JSONArray(response);
                    int total = 0;
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        int calorias = obj.optInt("caloriasEjercicio", 0);
                        int series   = obj.optInt("series", 0);
                        int reps     = obj.optInt("repeticiones", 0);
                        total += calorias * series * reps;
                    }
                    final int kcal = total;
                    runOnUiThread(() -> {
                        caloriasCalculadas = kcal;
                        if (kcal > 0) {
                            tvCaloriasCalculadas.setText(getString(R.string.sesiones_kcal, kcal));
                            cardCalorias.setVisibility(View.VISIBLE);
                        } else {
                            cardCalorias.setVisibility(View.GONE);
                        }
                    });
                } catch (JSONException ignored) {}
            }
            @Override public void onError(String message, int statusCode) {}
        });
    }

    private void guardarSesion() {
        String durStr = etDuracion.getText() != null ? etDuracion.getText().toString().trim() : "";
        if (durStr.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("usuarioId", prefsManager.getUsuarioId());

            int posicion = spRutina.getSelectedItemPosition();
            if (posicion > 0 && posicion <= rutinas.size()) {
                body.put("rutinaId", rutinas.get(posicion - 1).getId());
            }

            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
            body.put("fechaInicio", now);
            body.put("duracionMinutos", Integer.parseInt(durStr));

            if (caloriasCalculadas > 0) body.put("caloriasQuemadas", caloriasCalculadas);

            int estrellas = (int) ratingBar.getRating();
            String valoracion = getString(R.string.sesiones_valoracion_fmt, estrellas);
            String notasUsuario = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";
            String notasFinal = notasUsuario.isEmpty() ? valoracion : valoracion + "\n" + notasUsuario;
            body.put("notas", notasFinal);

            body.put("completada", true);

            API.crearSesion(body, new UtilREST.OnResponseListener() {
                @Override
                public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
                        UIHelper.mostrarToastExito(RegistrarSesionActivity.this,
                                getString(R.string.sesiones_exito));

                        int sesionIdGuardada = -1;
                        ArrayList<String> nuevosLogros = new ArrayList<>();
                        try {
                            JSONObject json = new JSONObject(response);
                            sesionIdGuardada = json.optInt("id", -1);
                            JSONArray logrosArr = json.optJSONArray("nuevosLogros");
                            if (logrosArr != null) {
                                for (int i = 0; i < logrosArr.length(); i++) {
                                    nuevosLogros.add(logrosArr.getString(i));
                                }
                            }
                        } catch (JSONException ignored) {}

                        String nombreRutina = "";
                        int pos = spRutina.getSelectedItemPosition();
                        if (pos > 0 && pos <= rutinas.size()) {
                            nombreRutina = rutinas.get(pos - 1).getNombre();
                        }

                        setResult(RESULT_OK);

                        if (sesionIdGuardada != -1) {
                            Intent intent = new Intent(RegistrarSesionActivity.this,
                                    ResumenSesionActivity.class);
                            intent.putExtra("sesionId", sesionIdGuardada);
                            intent.putExtra("rutinaNombre", nombreRutina);
                            intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
                            startActivity(intent);
                        }

                        finish();
                    });
                }
                @Override
                public void onError(String message, int statusCode) {
                    runOnUiThread(() -> UIHelper.mostrarToastError(
                            RegistrarSesionActivity.this, getString(R.string.error_conexion)));
                }
            });

        } catch (JSONException | NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
