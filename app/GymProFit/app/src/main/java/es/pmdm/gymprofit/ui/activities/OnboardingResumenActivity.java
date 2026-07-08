package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// OnboardingResumenActivity — Paso final del onboarding: resumen y guardado de datos.
// Calcula las métricas nutricionales a partir de los datos recogidos en los pasos
// previos, las muestra al usuario y, al pulsar "Comenzar", las persiste en
// preferencias locales y las envía a la API (PATCH /usuarios/{id}) antes de ir al Home.
// ============================================================
public class OnboardingResumenActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Interfaz Retrofit tipada del dominio usuarios (etapa 2)
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Inicializa la pantalla, calcula y muestra el resumen nutricional, y
    // configura el botón "Comenzar" para guardar los datos y navegar al Home.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding_resumen);

        Bundle extras = getIntent().getExtras();

        calcularYMostrar(extras, prefs);

        findViewById(R.id.btnComenzar).setOnClickListener(v ->
                guardarEnApiYContinuar(extras, prefs));
    }

    // Lee los datos del onboarding recibidos por extras, calcula el resultado
    // nutricional (calorías, macros, agua), lo guarda en preferencias y lo
    // muestra en las vistas del resumen.
    private void calcularYMostrar(Bundle extras, PreferencesManager prefs) {
        if (extras == null) return;

        String pesoStr = extras.getString("peso", "70");
        double altura  = extras.getDouble("altura", 170);
        int edad       = extras.getInt("edad", 25);
        String sexo    = extras.getString("sexo", "HOMBRE");
        String actividad = extras.getString("actividad", CalculadoraNutricional.ACTIVIDAD_MODERADO);
        String objetivo  = extras.getString("objetivo", CalculadoraNutricional.OBJETIVO_MANTENER_PESO);
        String nivel     = extras.getString("nivel", "");

        double peso = Double.parseDouble(pesoStr.replace(",", "."));
        prefs.savePeso(peso);
        prefs.saveAltura(altura);
        prefs.saveEdad(edad);
        if (!nivel.isEmpty()) prefs.saveNivel(nivel);
        boolean esHombre = "HOMBRE".equals(sexo);

        ResultadoNutricional resultado = CalculadoraNutricional.calcular(
                peso, altura, edad, esHombre, actividad, objetivo);

        prefs.saveResultadoNutricional(
                resultado.calorias, resultado.proteinas,
                resultado.carbohidratos, resultado.grasas, resultado.agua);
        prefs.saveObjetivo(objetivo);
        prefs.saveSexo(sexo);
        prefs.saveActividad(actividad);

        ((TextView) findViewById(R.id.tvResumenCalorias)).setText(String.valueOf(resultado.calorias));
        ((TextView) findViewById(R.id.tvResumenObjetivo)).setText(obtenerNombreObjetivo(objetivo));
        ((TextView) findViewById(R.id.tvResumenProteinas)).setText(resultado.proteinas + "g");
        ((TextView) findViewById(R.id.tvResumenCarbos)).setText(resultado.carbohidratos + "g");
        ((TextView) findViewById(R.id.tvResumenGrasas)).setText(resultado.grasas + "g");
        ((TextView) findViewById(R.id.tvResumenAgua)).setText(resultado.agua + "L");
        ((ProgressBar) findViewById(R.id.progressResumen)).setProgress(100);
    }

    // Traduce el valor del enum de objetivo (API) a su texto localizado.
    private String obtenerNombreObjetivo(String objetivo) {
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO:             return getString(R.string.objetivo_perder_peso);
            case CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR:     return getString(R.string.objetivo_ganar_musculo);
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO:           return getString(R.string.objetivo_mantener);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_RESISTENCIA:     return getString(R.string.objetivo_resistencia);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA:          return getString(R.string.objetivo_fuerza);
            case CalculadoraNutricional.OBJETIVO_REDUCIR_GRASA:           return getString(R.string.objetivo_reducir_grasa);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FLEXIBILIDAD:    return getString(R.string.objetivo_flexibilidad);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_VELOCIDAD:       return getString(R.string.objetivo_velocidad);
            case CalculadoraNutricional.OBJETIVO_AUMENTAR_CALORIAS:       return getString(R.string.objetivo_aumentar_calorias);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_MOVILIDAD:       return getString(R.string.objetivo_movilidad);
            default: return objetivo;
        }
    }

    // Envía los datos del onboarding a la API mediante PATCH /usuarios/{id}.
    // Si no hay usuario logueado o faltan datos, o si la llamada falla, se
    // guarda igualmente el progreso localmente y se continúa al Home.
    private void guardarEnApiYContinuar(Bundle extras, PreferencesManager prefs) {
        int usuarioId = prefs.getUsuarioId();

        if (usuarioId == -1 || extras == null) {
            marcarOnboardingCompletado(prefs);
            irAlHome();
            return;
        }

        try {
            // Cuerpo de escritura como Map; los decimales viajan como BigDecimal.
            Map<String, Object> body = new HashMap<>();

            String emailStr = extras.getString("email", "");
            if (!emailStr.isEmpty()) body.put("email", emailStr);

            String pesoStr = extras.getString("peso", "");
            if (!pesoStr.isEmpty()) {
                body.put("peso", new BigDecimal(pesoStr.replace(",", ".")));
            }

            double altura = extras.getDouble("altura", 0);
            if (altura > 0) body.put("altura", BigDecimal.valueOf(altura));

            int edad = extras.getInt("edad", 0);
            if (edad > 0) body.put("edad", edad);

            String nivel = extras.getString("nivel", "");
            if (!nivel.isEmpty()) body.put("nivelExperiencia", nivel);

            body.put("objetivo", prefs.getObjetivo());

            usuarioApi.patch(usuarioId, body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void response) {
                    UIHelper.mostrarToastExito(OnboardingResumenActivity.this,
                            getString(R.string.onboarding_guardado_exito));
                    marcarOnboardingCompletado(prefs);
                    irAlHome();
                }

                @Override
                public void onFail(int code, String message) {
                    UIHelper.mostrarToastInfo(OnboardingResumenActivity.this,
                            getString(R.string.onboarding_guardado_local));
                    marcarOnboardingCompletado(prefs);
                    irAlHome();
                }
            });
        } catch (NumberFormatException e) {
            marcarOnboardingCompletado(prefs);
            irAlHome();
        }
    }

    // Marca el onboarding como completado, tanto de forma global como para el
    // usuario actual (para no repetirlo tras cerrar sesión y volver a entrar).
    private void marcarOnboardingCompletado(PreferencesManager prefs) {
        prefs.setOnboardingCompletado(true);
        prefs.setOnboardingCompletadoParaUsuario(prefs.getUsername());
    }

    // Navega al Home limpiando el back stack para evitar volver al onboarding.
    private void irAlHome() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
