package es.pmdm.gymprofit.utils;

// MODIFICADO - Añadidos campos para usuarioId, username, sexo, actividad,
// objetivo, calorías, macros y agua calculados en el onboarding
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class PreferencesManager {

    private static final String PREF_NAME = "GymProFitPrefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NIVEL = "nivel_experiencia";
    private static final String KEY_OBJETIVO = "objetivo";
    private static final String KEY_SEXO = "sexo";
    private static final String KEY_ACTIVIDAD = "actividad";
    private static final String KEY_CALORIAS = "calorias_diarias";
    private static final String KEY_PROTEINAS = "proteinas_diarias";
    private static final String KEY_CARBOS = "carbos_diarios";
    private static final String KEY_GRASAS = "grasas_diarias";
    private static final String KEY_AGUA = "agua_diaria";
    private static final String KEY_ONBOARDING = "onboarding_completado";
    private static final String KEY_ROL = "usuario_rol";
    private static final String KEY_PESO   = "usuario_peso";
    private static final String KEY_ALTURA = "usuario_altura";
    private static final String KEY_EDAD   = "usuario_edad";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveTheme(int themeMode) { editor.putInt(KEY_THEME, themeMode); editor.apply(); }
    public int getTheme() { return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); }
    public void applyTheme() { AppCompatDelegate.setDefaultNightMode(getTheme()); }

    public void saveLanguage(String code) { editor.putString(KEY_LANGUAGE, code); editor.apply(); }
    public String getLanguage() { return prefs.getString(KEY_LANGUAGE, ""); }

    public void saveToken(String token) { editor.putString(KEY_TOKEN, token); editor.apply(); }
    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public Boolean haySesion() { String t = getToken(); return t != null && !t.isEmpty(); }

    public void cerrarSesion() {
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USUARIO_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }

    public void saveUsuarioId(int id) { editor.putInt(KEY_USUARIO_ID, id); editor.apply(); }
    public int getUsuarioId() { return prefs.getInt(KEY_USUARIO_ID, -1); }

    public void saveUsername(String username) { editor.putString(KEY_USERNAME, username); editor.apply(); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, ""); }

    public void saveNivel(String nivel) { editor.putString(KEY_NIVEL, nivel); editor.apply(); }
    public String getNivel() { return prefs.getString(KEY_NIVEL, ""); }

    public void saveObjetivo(String objetivo) { editor.putString(KEY_OBJETIVO, objetivo); editor.apply(); }
    public String getObjetivo() { return prefs.getString(KEY_OBJETIVO, ""); }

    public void saveSexo(String sexo) { editor.putString(KEY_SEXO, sexo); editor.apply(); }
    public String getSexo() { return prefs.getString(KEY_SEXO, "HOMBRE"); }

    public void saveActividad(String actividad) { editor.putString(KEY_ACTIVIDAD, actividad); editor.apply(); }
    public String getActividad() { return prefs.getString(KEY_ACTIVIDAD, "MODERADO"); }

    public void saveResultadoNutricional(int calorias, int proteinas, int carbos, int grasas, double agua) {
        editor.putInt(KEY_CALORIAS, calorias);
        editor.putInt(KEY_PROTEINAS, proteinas);
        editor.putInt(KEY_CARBOS, carbos);
        editor.putInt(KEY_GRASAS, grasas);
        editor.putFloat(KEY_AGUA, (float) agua);
        editor.apply();
    }

    public int getCaloriasDiarias() { return prefs.getInt(KEY_CALORIAS, 2000); }
    public int getProteinasDiarias() { return prefs.getInt(KEY_PROTEINAS, 150); }
    public int getCarbosDiarios() { return prefs.getInt(KEY_CARBOS, 250); }
    public int getGrasasDiarias() { return prefs.getInt(KEY_GRASAS, 65); }
    public double getAguaDiaria() { return prefs.getFloat(KEY_AGUA, 2.0f); }

    public void setOnboardingCompletado(boolean v) { editor.putBoolean(KEY_ONBOARDING, v); editor.apply(); }
    public boolean isOnboardingCompletado() { return prefs.getBoolean(KEY_ONBOARDING, false); }

    public void setOnboardingCompletadoParaUsuario(String username) {
        editor.putBoolean("onboarding_done_" + username, true);
        editor.apply();
    }
    public boolean isOnboardingCompletadoParaUsuario(String username) {
        if (username == null || username.isEmpty()) return false;
        return prefs.getBoolean("onboarding_done_" + username, false);
    }

    public void saveRol(String rol) { editor.putString(KEY_ROL, rol); editor.apply(); }
    public String getRol() { return prefs.getString(KEY_ROL, "ROLE_USER"); }
    public boolean isAdmin() { return "ROLE_ADMIN".equals(getRol()); }
    public boolean isGuest() { return "ROLE_GUEST".equals(getRol()); }

    public void savePeso(double peso)     { editor.putFloat(KEY_PESO, (float) peso); editor.apply(); }
    public double getPeso()               { return prefs.getFloat(KEY_PESO, 70.0f); }

    public void saveAltura(double altura) { editor.putFloat(KEY_ALTURA, (float) altura); editor.apply(); }
    public double getAltura()             { return prefs.getFloat(KEY_ALTURA, 170.0f); }

    public void saveEdad(int edad)        { editor.putInt(KEY_EDAD, edad); editor.apply(); }
    public int getEdad()                  { return prefs.getInt(KEY_EDAD, 25); }
}