package es.pmdm.gymprofit.ui.activities;

import android.animation.ObjectAnimator;
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
import es.pmdm.gymprofit.utils.Numeros;
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

        calcularYMostrar(prefs);

        findViewById(R.id.btnComenzar).setOnClickListener(v ->
                guardarEnApiYContinuar(prefs));
    }

    /**
     * Lee lo contestado en el asistente, calcula calorías, macros y agua, lo
     * guarda como perfil definitivo y lo pinta en el resumen.
     *
     * <p>Los datos salen del borrador persistido y no de los extras del Intent:
     * así el resumen sale bien aunque el sistema haya matado la app a mitad del
     * asistente y el usuario lo haya retomado desde el principio de la sesión.
     */
    private void calcularYMostrar(PreferencesManager prefs) {
        double altura = prefs.getBorradorAltura();
        if (altura <= 0) altura = 170;

        int edad = prefs.getBorradorEdad();
        if (edad <= 0) edad = 25;

        String sexo = prefs.getBorradorSexo();

        String actividad = prefs.getBorradorActividad();
        if (actividad.isEmpty()) actividad = CalculadoraNutricional.ACTIVIDAD_MODERADO;

        String objetivo = prefs.getBorradorObjetivo();
        if (objetivo.isEmpty()) objetivo = CalculadoraNutricional.OBJETIVO_MANTENER_PESO;

        String nivel = prefs.getBorradorNivel();

        // El paso 3 ya valida y normaliza el peso; aquí se vuelve a comprobar por
        // si el borrador viniese de una versión anterior o quedara a medias.
        Double pesoLeido = Numeros.decimal(prefs.getBorradorPeso(), 30, 300);
        double peso = (pesoLeido != null) ? pesoLeido : 70;
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
        ((TextView) findViewById(R.id.tvResumenProteinas)).setText(getString(R.string.unidad_g_entero, resultado.proteinas));
        ((TextView) findViewById(R.id.tvResumenCarbos)).setText(getString(R.string.unidad_g_entero, resultado.carbohidratos));
        ((TextView) findViewById(R.id.tvResumenGrasas)).setText(getString(R.string.unidad_g_entero, resultado.grasas));
        ((TextView) findViewById(R.id.tvResumenAgua)).setText(getString(R.string.unidad_litros, resultado.agua));

        // Las tres barras de macros estaban fijadas a 0 y sin id, así que nadie
        // podía tocarlas: tres rayas grises bajo tres números, para siempre. Una
        // barra a cero no se lee como decoración, se lee como que no ha cargado.
        // Ahora muestran qué parte de las calorías del día aporta cada macro
        // (4 kcal por gramo de proteína y de carbohidrato, 9 por gramo de grasa).
        pintarMacro(R.id.pbResumenProteinas, resultado.proteinas * 4, resultado.calorias);
        pintarMacro(R.id.pbResumenCarbos, resultado.carbohidratos * 4, resultado.calorias);
        pintarMacro(R.id.pbResumenGrasas, resultado.grasas * 9, resultado.calorias);
        ((ProgressBar) findViewById(R.id.progressResumen)).setProgress(100);
    }

    /**
     * Pinta la parte de las calorías del día que aporta un macro.
     *
     * @param idBarra  la barra a rellenar.
     * @param kcalMacro calorías que aporta ese macro.
     * @param kcalTotal calorías del día.
     */
    private void pintarMacro(int idBarra, int kcalMacro, int kcalTotal) {
        ProgressBar barra = findViewById(idBarra);
        if (barra == null || kcalTotal <= 0) return;

        int porcentaje = Math.max(0, Math.min(100, Math.round(kcalMacro * 100f / kcalTotal)));

        // Crece desde cero al entrar: el reparto se percibe mejor viéndolo llenarse.
        ObjectAnimator.ofInt(barra, "progress", 0, porcentaje)
                .setDuration(600L)
                .start();
    }

    // Traduce el valor del enum de objetivo (API) a su texto localizado.
    private String obtenerNombreObjetivo(String objetivo) {
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO:             return getString(R.string.objetivo_perder_peso);
            case CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR:     return getString(R.string.objetivo_ganar_musculo);
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO:           return getString(R.string.objetivo_mantener);

            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA:          return getString(R.string.objetivo_fuerza);
            default: return objetivo;
        }
    }

    // Envía los datos del onboarding a la API mediante PATCH /usuarios/{id}.
    // Si no hay usuario logueado o faltan datos, o si la llamada falla, se
    // guarda igualmente el progreso localmente y se continúa al Home.
    private void guardarEnApiYContinuar(PreferencesManager prefs) {
        int usuarioId = prefs.getUsuarioId();

        if (usuarioId == -1) {
            marcarOnboardingCompletado(prefs);
            irAlHome();
            return;
        }

        try {
            // Cuerpo de escritura como Map; los decimales viajan como BigDecimal.
            Map<String, Object> body = new HashMap<>();

            // Sin correo (GP-083): el asistente ya no lo pide y la cuenta lo tiene.
            // Cambiarlo tiene su propia ruta, con la contraseña, en editar perfil.

            BigDecimal pesoExacto = Numeros.exacto(prefs.getBorradorPeso(), 30, 300);
            if (pesoExacto != null) body.put("peso", pesoExacto);

            double altura = prefs.getBorradorAltura();
            if (altura > 0) body.put("altura", BigDecimal.valueOf(altura));

            int edad = prefs.getBorradorEdad();
            if (edad > 0) body.put("edad", edad);

            String nivel = prefs.getBorradorNivel();
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
    // usuario actual (para no repetirlo tras cerrar sesión y volver a entrar), y
    // tira el borrador: los datos ya están en el perfil definitivo.
    private void marcarOnboardingCompletado(PreferencesManager prefs) {
        prefs.setOnboardingCompletado(true);
        prefs.setOnboardingCompletadoParaUsuario(prefs.getUsername());
        prefs.limpiarBorradorOnboarding();
    }

    // Navega al Home limpiando el back stack para evitar volver al onboarding.
    private void irAlHome() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
