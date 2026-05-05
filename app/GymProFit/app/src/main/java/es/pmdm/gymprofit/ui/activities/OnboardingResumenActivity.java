package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.dto.UsuarioDTO;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnboardingResumenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        aplicarIdioma(prefs);

        setContentView(R.layout.activity_onboarding_resumen);

        Bundle extras = getIntent().getExtras();

        calcularYMostrar(extras, prefs);

        findViewById(R.id.btnComenzar).setOnClickListener(v ->
                guardarEnApiYContinuar(extras, prefs));
    }

    private void calcularYMostrar(Bundle extras, PreferencesManager prefs) {
        if (extras == null) {
            return;
        }

        String pesoStr = extras.getString("peso", "70");
        double altura = extras.getDouble("altura", 170);
        int edad = extras.getInt("edad", 25);
        String sexo = extras.getString("sexo", "HOMBRE");
        String actividad = extras.getString("actividad", CalculadoraNutricional.ACTIVIDAD_MODERADO);
        String objetivo = extras.getString("objetivo", CalculadoraNutricional.OBJETIVO_MANTENER_PESO);

        double peso = Double.parseDouble(pesoStr.replace(",", "."));
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

    private String obtenerNombreObjetivo(String objetivo) {
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO: return getString(R.string.objetivo_perder_peso);
            case CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR: return getString(R.string.objetivo_ganar_musculo);
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO: return getString(R.string.objetivo_mantener);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_RESISTENCIA: return getString(R.string.objetivo_resistencia);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA: return getString(R.string.objetivo_fuerza);
            case CalculadoraNutricional.OBJETIVO_REDUCIR_GRASA: return getString(R.string.objetivo_reducir_grasa);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FLEXIBILIDAD: return getString(R.string.objetivo_flexibilidad);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_VELOCIDAD: return getString(R.string.objetivo_velocidad);
            case CalculadoraNutricional.OBJETIVO_AUMENTAR_CALORIAS: return getString(R.string.objetivo_aumentar_calorias);
            case CalculadoraNutricional.OBJETIVO_MEJORAR_MOVILIDAD: return getString(R.string.objetivo_movilidad);
            default: return objetivo;
        }
    }

    private void guardarEnApiYContinuar(Bundle extras, PreferencesManager prefs) {
        int usuarioId = prefs.getUsuarioId();

        if (usuarioId == -1 || extras == null) {
            irAlHome(prefs);
            return;
        }

        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername(extras.getString("nombre", prefs.getUsername()));
        dto.setEmail(extras.getString("email", ""));

        String pesoStr = extras.getString("peso", "");
        if (!pesoStr.isEmpty()) {
            dto.setPeso(pesoStr);
        }

        double altura = extras.getDouble("altura", 0);
        if (altura > 0) {
            dto.setAltura(altura);
        }

        int edad = extras.getInt("edad", 0);
        if(edad > 0) {
            dto.setEdad(edad);
        }

        dto.setNivelExperiencia(extras.getString("nivel", ""));
        dto.setObjetivo(prefs.getObjetivo());

        ApiClient.getApiService().actualizarUsuario(usuarioId, dto)
                .enqueue(new Callback<UsuarioDTO>() {
                    @Override
                    public void onResponse(Call<UsuarioDTO> call, Response<UsuarioDTO> response) {
                        if (response.isSuccessful()) {
                            UIHelper.mostrarToastExito(OnboardingResumenActivity.this,
                                    getString(R.string.onboarding_guardado_exito));
                        }

                        prefs.setOnboardingCompletado(true);

                        irAlHome(prefs);
                    }

                    @Override
                    public void onFailure(Call<UsuarioDTO> call, Throwable t) {
                        UIHelper.mostrarToastInfo(OnboardingResumenActivity.this,
                                getString(R.string.onboarding_guardado_local));

                        prefs.setOnboardingCompletado(true);

                        irAlHome(prefs);
                    }
                });
    }

    private void irAlHome(PreferencesManager prefs) {
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        finish();
    }

    private void aplicarIdioma(PreferencesManager prefs) {
        String lang = prefs.getLanguage();

        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);

            Resources resources = getResources();

            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(locale);

            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }
}