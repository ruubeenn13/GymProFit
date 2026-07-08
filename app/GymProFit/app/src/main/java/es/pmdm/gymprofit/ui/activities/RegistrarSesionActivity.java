package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RegistrarSesionActivity — Formulario para registrar una sesión de entrenamiento.
// Permite elegir una rutina (propia o predefinida), calcula automáticamente las
// calorías estimadas y los ejercicios/pesos asociados, y guarda la sesión junto
// con los ejercicios realizados en la API, navegando después al resumen.
// ============================================================
public class RegistrarSesionActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private Spinner spRutina;
    private TextInputEditText etDuracion, etNotas;
    private TextView tvCaloriasCalculadas;
    private View cardCalorias, cardEjercicios;
    private RatingBar ratingBar;
    private PreferencesManager prefsManager;
    // Interfaz Retrofit tipada del dominio sesiones (etapa 2)
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    // Interfaz Retrofit tipada del dominio rutinas (etapa 2)
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private int caloriasCalculadas = 0;
    private final List<Rutina> rutinas = new ArrayList<>();
    private final List<String> rutinaOpciones = new ArrayList<>();
    private final List<EjercicioPesoAdapter.Item> ejercicioItems = new ArrayList<>();
    private EjercicioPesoAdapter ejercicioPesoAdapter;

    // Inicializa la pantalla: monta vistas, configura el RecyclerView de
    // ejercicios/pesos y carga las rutinas disponibles para el spinner.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_registrar_sesion);

        spRutina             = findViewById(R.id.spRutina);
        etDuracion           = findViewById(R.id.etDuracion);
        etNotas              = findViewById(R.id.etNotas);
        tvCaloriasCalculadas = findViewById(R.id.tvCaloriasCalculadas);
        cardCalorias         = findViewById(R.id.cardCalorias);
        cardEjercicios       = findViewById(R.id.cardEjercicios);
        ratingBar            = findViewById(R.id.ratingBar);

        RecyclerView rvEjercicios = findViewById(R.id.rvEjercicios);
        ejercicioPesoAdapter = new EjercicioPesoAdapter(ejercicioItems);
        rvEjercicios.setLayoutManager(new LinearLayoutManager(this));
        rvEjercicios.setNestedScrollingEnabled(false);
        rvEjercicios.setAdapter(ejercicioPesoAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarSesion());

        cargarRutinas();
    }

    // Carga en paralelo las rutinas predefinidas y las del usuario; cuando
    // ambas llamadas terminan, combina los resultados y actualiza el spinner.
    private void cargarRutinas() {
        int usuarioId = prefsManager.getUsuarioId();
        rutinaOpciones.clear();
        rutinaOpciones.add(getString(R.string.sesiones_sin_rutina));

        final List<Rutina> predefinidas = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(2);

        // Rutinas predefinidas del sistema (ya deserializadas por Gson).
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> lista) {
                if (lista != null) predefinidas.addAll(lista);
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
            @Override public void onFail(int code, String message) {
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
        });

        // Rutinas activas del usuario (ya deserializadas por Gson).
        rutinaApi.getDeUsuarioActivas(usuarioId).enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> lista) {
                if (lista != null) rutinas.addAll(lista);
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
            @Override public void onFail(int code, String message) {
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
        });
    }

    // Une rutinas predefinidas y del usuario en una única lista y refresca
    // el spinner en el hilo principal.
    private void combinarYMostrar(List<Rutina> predefinidas) {
        List<Rutina> todas = new ArrayList<>(predefinidas);
        todas.addAll(rutinas);
        rutinas.clear();
        rutinas.addAll(todas);
        for (Rutina r : todas) rutinaOpciones.add(r.getNombre());
        actualizarSpinner();
    }

    // Rellena el spinner de rutinas y, al seleccionar una, calcula sus
    // calorías estimadas; si se selecciona "sin rutina", limpia los cálculos.
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
                    ejercicioItems.clear();
                    ejercicioPesoAdapter.notifyDataSetChanged();
                    cardEjercicios.setVisibility(View.GONE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Obtiene los ejercicios de la rutina seleccionada, calcula las calorías
    // totales (calorías x series x repeticiones) y arma la lista de
    // ejercicios/pesos que se mostrará en el RecyclerView.
    private void calcularCaloriasRutina(int rutinaId) {
        // Relaciones rutina-ejercicio (ya deserializadas por Gson): incluyen calorías
        // y nombre enriquecidos desde el catálogo, igual que devolvía el JSON antiguo.
        rutinaApi.getEjerciciosDeRutina(rutinaId).enqueue(new ApiCallback<List<RutinaEjercicio>>() {
            @Override
            public void onOk(List<RutinaEjercicio> lista) {
                int total = 0;
                List<EjercicioPesoAdapter.Item> nuevosItems = new ArrayList<>();
                if (lista != null) {
                    int i = 0;
                    for (RutinaEjercicio re : lista) {
                        i++;
                        int calorias    = re.getCaloriasEjercicio();
                        int series      = re.getSeries();
                        int reps        = re.getRepeticiones();
                        int ejercicioId = re.getEjercicioId();
                        String nombre   = (re.getNombreEjercicio() != null && !re.getNombreEjercicio().isEmpty())
                                ? re.getNombreEjercicio() : "Ejercicio " + i;
                        total += calorias * series * reps;
                        if (ejercicioId != -1) {
                            nuevosItems.add(new EjercicioPesoAdapter.Item(ejercicioId, nombre, series, reps));
                        }
                    }
                }
                caloriasCalculadas = total;
                if (total > 0) {
                    tvCaloriasCalculadas.setText(getString(R.string.sesiones_kcal, total));
                    cardCalorias.setVisibility(View.VISIBLE);
                } else {
                    cardCalorias.setVisibility(View.GONE);
                }
                ejercicioItems.clear();
                ejercicioItems.addAll(nuevosItems);
                ejercicioPesoAdapter.notifyDataSetChanged();
                cardEjercicios.setVisibility(nuevosItems.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onFail(int code, String message) {}
        });
    }

    // Valida la duración (obligatoria), construye el JSON de la sesión
    // (rutina, fecha, duración, calorías, valoración y notas) y la envía a
    // la API; si se crea correctamente, registra los ejercicios realizados
    // y navega al resumen de la sesión.
    private void guardarSesion() {
        String durStr = etDuracion.getText() != null ? etDuracion.getText().toString().trim() : "";
        if (durStr.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            return;
        }

        try {
            // Cuerpo de creación como Map<String,Object> (Gson lo serializa a JSON).
            Map<String, Object> body = new HashMap<>();
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

            // Muestra el spinner modal mientras se envía la sesión a la API.
            LoadingDialog.show(this);
            // La respuesta ya viene deserializada: id de la sesión + logros nuevos.
            sesionApi.crear(body).enqueue(new ApiCallback<SesionEntrenamiento>() {
                @Override
                public void onOk(SesionEntrenamiento sesionCreada) {
                    // Oculta el spinner al completarse el guardado correctamente.
                    LoadingDialog.hide(RegistrarSesionActivity.this);
                    UIHelper.mostrarToastExito(RegistrarSesionActivity.this,
                            getString(R.string.sesiones_exito));

                    int sesionIdGuardada = sesionCreada != null ? sesionCreada.getId() : -1;
                    ArrayList<String> nuevosLogros = new ArrayList<>();
                    if (sesionCreada != null && sesionCreada.getNuevosLogros() != null) {
                        nuevosLogros.addAll(sesionCreada.getNuevosLogros());
                    }

                    String nombreRutina = "";
                    int pos = spRutina.getSelectedItemPosition();
                    if (pos > 0 && pos <= rutinas.size()) {
                        nombreRutina = rutinas.get(pos - 1).getNombre();
                    }

                    setResult(RESULT_OK);

                    if (sesionIdGuardada != -1) {
                        registrarEjerciciosRealizados(sesionIdGuardada);
                        Intent intent = new Intent(RegistrarSesionActivity.this,
                                ResumenSesionActivity.class);
                        intent.putExtra("sesionId", sesionIdGuardada);
                        intent.putExtra("rutinaNombre", nombreRutina);
                        intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
                        startActivity(intent);
                    }

                    finish();
                }
                @Override
                public void onFail(int code, String message) {
                    // Oculta el spinner y muestra el toast de error mapeado según el código.
                    LoadingDialog.hide(RegistrarSesionActivity.this);
                    UiFeedback.toastError(RegistrarSesionActivity.this, code, message);
                }
            });

        } catch (NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    // Envía a la API un registro de ejercicio realizado por cada ítem de la
    // lista (series, repeticiones y peso usado), asociado a la sesión creada.
    private void registrarEjerciciosRealizados(int sesionId) {
        for (EjercicioPesoAdapter.Item item : ejercicioItems) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("sesionId", sesionId);
                body.put("ejercicioId", item.ejercicioId);
                body.put("seriesCompletadas", item.series);
                body.put("repeticionesReales", item.repeticiones);
                if (!item.peso.isEmpty()) {
                    // Peso como BigDecimal para conservar la precisión decimal.
                    body.put("pesoUsado", new BigDecimal(item.peso.replace(",", ".")));
                }
                sesionApi.crearEjercicioRealizado(body).enqueue(new ApiCallback<Void>() {
                    @Override public void onOk(Void b) {}
                    @Override public void onFail(int c, String m) {}
                });
            } catch (NumberFormatException ignored) {}
        }
    }
}
