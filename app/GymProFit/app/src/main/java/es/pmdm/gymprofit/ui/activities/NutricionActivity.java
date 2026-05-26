package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.util.TypedValue;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.ResultadoNutricional;

/**
 * Pantalla principal de seguimiento nutricional.
 * Muestra calorías y macros del día, con cards por tipo de comida.
 */
public class NutricionActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressCalorias, progressProteinas, progressCarbos, progressGrasas;
    private TextView tvCaloriasActuales, tvCaloriasObjetivo;
    private TextView tvProteinasActuales, tvCarbosActuales, tvGrasasActuales;
    private TextView tvSubDesayuno, tvSubAlmuerzo, tvSubComida, tvSubMerienda, tvSubCena;

    private int objetivoCalorias = 2000, objetivoProteinas = 150, objetivoCarbos = 250, objetivoGrasas = 65;
    private final Map<String, Comida> comidasHoy = new HashMap<>();

    private ActivityResultLauncher<Intent> comidaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutricion);

        setupMenuButton();
        inicializarVistas();

        comidaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        recalcularObjetivos();
                        cargarComidasHoy();
                    }
                });

        configurarCardsComida();
        configurarNavegacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        recalcularObjetivos();
        cargarComidasHoy();
    }

    // ── Vistas ───────────────────────────────────────────────────────────────

    private void inicializarVistas() {
        progressCalorias   = findViewById(R.id.progressCalorias);
        progressProteinas  = findViewById(R.id.progressProteinas);
        progressCarbos     = findViewById(R.id.progressCarbos);
        progressGrasas     = findViewById(R.id.progressGrasas);
        tvCaloriasActuales = findViewById(R.id.tvCaloriasActuales);
        tvCaloriasObjetivo = findViewById(R.id.tvCaloriasObjetivo);
        tvProteinasActuales = findViewById(R.id.tvProteinasActuales);
        tvCarbosActuales    = findViewById(R.id.tvCarbosActuales);
        tvGrasasActuales    = findViewById(R.id.tvGrasasActuales);
        tvSubDesayuno  = findViewById(R.id.tvSubDesayuno);
        tvSubAlmuerzo  = findViewById(R.id.tvSubAlmuerzo);
        tvSubComida    = findViewById(R.id.tvSubComida);
        tvSubMerienda  = findViewById(R.id.tvSubMerienda);
        tvSubCena      = findViewById(R.id.tvSubCena);
    }

    // ── Objetivos nutricionales ──────────────────────────────────────────────

    /**
     * Recalcula los objetivos nutricionales a partir de los datos del perfil guardados en prefs.
     */
    private void recalcularObjetivos() {
        double peso      = prefsManager.getPeso();
        double altura    = prefsManager.getAltura();
        int edad         = prefsManager.getEdad();
        boolean hombre   = "HOMBRE".equals(prefsManager.getSexo());
        String actividad = prefsManager.getActividad();
        String objetivo  = prefsManager.getObjetivo();

        ResultadoNutricional r = CalculadoraNutricional.calcular(peso, altura, edad, hombre, actividad, objetivo);
        objetivoCalorias  = r.calorias;
        objetivoProteinas = r.proteinas;
        objetivoCarbos    = r.carbohidratos;
        objetivoGrasas    = r.grasas;

        prefsManager.saveResultadoNutricional(objetivoCalorias, objetivoProteinas, objetivoCarbos, objetivoGrasas, r.agua);
        tvCaloriasObjetivo.setText("/ " + objetivoCalorias + " kcal");
    }

    // ── Carga de comidas ─────────────────────────────────────────────────────

    /**
     * Obtiene las comidas del día actual desde la API y actualiza la UI.
     */
    private void cargarComidasHoy() {
        int usuarioId = prefsManager.getUsuarioId();
        final String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        API.getComidasDeUsuarioFecha(usuarioId, hoy, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Comida> lista = UtilJSONParser.parseListaComidas(response);
                    runOnUiThread(() -> {
                        comidasHoy.clear();
                        if (lista != null) {
                            for (Comida c : lista) {
                                comidasHoy.put(c.getTipoComida(), c);
                            }
                        }
                        actualizarUI(hoy);
                    });
                } catch (JSONException e) {
                    runOnUiThread(() -> actualizarUI(hoy));
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                // 404 = no hay comidas hoy → estado vacío
                runOnUiThread(() -> {
                    comidasHoy.clear();
                    actualizarUI(hoy);
                });
            }
        });
    }

    // ── Actualización de la UI ────────────────────────────────────────────────

    /**
     * Actualiza totales, barras de progreso, macros y subtítulos de cards.
     *
     * @param fecha fecha actual en formato yyyy-MM-dd
     */
    private void actualizarUI(String fecha) {
        int totalCal = 0;
        double totalProt = 0, totalCarb = 0, totalGras = 0;

        for (Comida c : comidasHoy.values()) {
            totalCal  += c.getTotalCalorias();
            totalProt += c.getTotalProteinas();
            totalCarb += c.getTotalCarbohidratos();
            totalGras += c.getTotalGrasas();
        }

        tvCaloriasActuales.setText(String.valueOf(totalCal));

        progressCalorias.setProgress(Math.min(100, (int) (totalCal * 100.0 / objetivoCalorias)));
        progressProteinas.setProgress(Math.min(100, (int) (totalProt * 100.0 / objetivoProteinas)));
        progressCarbos.setProgress(Math.min(100, (int) (totalCarb * 100.0 / objetivoCarbos)));
        progressGrasas.setProgress(Math.min(100, (int) (totalGras * 100.0 / objetivoGrasas)));

        int colorNormal = getAttrColor(com.google.android.material.R.attr.colorOnSurface);
        int colorError  = getAttrColor(com.google.android.material.R.attr.colorError);

        tvProteinasActuales.setText(String.format(Locale.getDefault(), "%.0fg", totalProt));
        tvProteinasActuales.setTextColor(totalProt > objetivoProteinas ? colorError : colorNormal);

        tvCarbosActuales.setText(String.format(Locale.getDefault(), "%.0fg", totalCarb));
        tvCarbosActuales.setTextColor(totalCarb > objetivoCarbos ? colorError : colorNormal);

        tvGrasasActuales.setText(String.format(Locale.getDefault(), "%.0fg", totalGras));
        tvGrasasActuales.setTextColor(totalGras > objetivoGrasas ? colorError : colorNormal);

        actualizarSubtituloCard("DESAYUNO", tvSubDesayuno, fecha);
        actualizarSubtituloCard("ALMUERZO", tvSubAlmuerzo, fecha);
        actualizarSubtituloCard("COMIDA",   tvSubComida,   fecha);
        actualizarSubtituloCard("MERIENDA", tvSubMerienda, fecha);
        actualizarSubtituloCard("CENA",     tvSubCena,     fecha);
    }

    /**
     * Actualiza el subtítulo de la card de un tipo de comida.
     */
    private void actualizarSubtituloCard(String tipo, TextView tvSub, String fecha) {
        Comida c = comidasHoy.get(tipo);
        if (c != null && c.getTotalCalorias() > 0) {
            tvSub.setText(c.getTotalCalorias() + " kcal");
        } else {
            tvSub.setText(getString(R.string.sin_registrar));
        }
    }

    /**
     * Resuelve un color de atributo del tema actual.
     */
    private int getAttrColor(int attrResId) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attrResId, tv, true);
        return tv.data;
    }

    // ── Cards de comida ───────────────────────────────────────────────────────

    /**
     * Configura los listeners de click en cada card de tipo de comida.
     */
    private void configurarCardsComida() {
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        setupCardComida(R.id.cardDesayuno, "DESAYUNO", hoy);
        setupCardComida(R.id.cardAlmuerzo, "ALMUERZO", hoy);
        setupCardComida(R.id.cardComida,   "COMIDA",   hoy);
        setupCardComida(R.id.cardMerienda, "MERIENDA", hoy);
        setupCardComida(R.id.cardCena,     "CENA",     hoy);
    }

    /**
     * Asigna el listener de click a una card de comida y lanza ComidaActivity.
     */
    private void setupCardComida(int cardId, String tipo, String fecha) {
        findViewById(cardId).setOnClickListener(v -> {
            Intent intent = new Intent(this, ComidaActivity.class);
            intent.putExtra("tipoComida", tipo);
            Comida c = comidasHoy.get(tipo);
            intent.putExtra("comidaId", c != null ? c.getId() : -1);
            intent.putExtra("fecha", fecha);
            comidaLauncher.launch(intent);
        });
    }

    // ── Navegación ────────────────────────────────────────────────────────────

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_nutricion);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}
