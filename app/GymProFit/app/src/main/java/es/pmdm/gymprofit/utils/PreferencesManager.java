package es.pmdm.gymprofit.utils;

// MODIFICADO - Añadidos campos para usuarioId, username, sexo, actividad,
// objetivo, calorías, macros y agua calculados en el onboarding
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

// ============================================================
// PreferencesManager — encapsula el acceso a SharedPreferences de la app.
// Centraliza persistencia de tema, idioma, sesión (token/usuario), datos de
// onboarding (nivel, objetivo, sexo, actividad), resultados nutricionales y
// datos físicos del usuario, evitando el acceso directo desde las Activities.
// ============================================================
public class PreferencesManager {

    // Nombre del archivo de SharedPreferences y claves usadas para cada dato guardado.
    private static final String PREF_NAME = "GymProFitPrefs";
    // Archivo SEPARADO y CIFRADO (EncryptedSharedPreferences) solo para los tokens sensibles.
    private static final String SECURE_PREF_NAME = "gymprofit_secure_prefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
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
    // Preferencias cifradas donde se guardan token y refresh token (nunca en claro).
    private SharedPreferences securePrefs;

    // Constructor: abre las preferencias normales (tema, idioma, datos no sensibles)
    // y el almacén cifrado para los tokens.
    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        securePrefs = crearSecurePrefs(context);
    }

    // Crea el almacén cifrado (EncryptedSharedPreferences). Si el almacén está corrupto
    // (p. ej. la clave del Keystore cambió tras un restore), lo borra y lo recrea: se pierde
    // la sesión (el usuario deberá volver a iniciar sesión) pero se evita un crash.
    private SharedPreferences crearSecurePrefs(Context context) {
        try {
            return abrirCifrado(context);
        } catch (Exception e) {
            Log.w("GymProFit", "Almacén cifrado corrupto, recreando: " + e.getMessage());
            context.deleteSharedPreferences(SECURE_PREF_NAME);
            try {
                return abrirCifrado(context);
            } catch (Exception ex) {
                // Fallback extremo: usar las preferencias normales para no dejar la app inutilizable.
                Log.e("GymProFit", "No se pudo crear el almacén cifrado: " + ex.getMessage());
                return prefs;
            }
        }
    }

    // Construye el MasterKey (AES256-GCM, respaldado por el Android Keystore) y el archivo cifrado.
    private SharedPreferences abrirCifrado(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context,
                SECURE_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public void saveTheme(int themeMode) { editor.putInt(KEY_THEME, themeMode); editor.apply(); }
    public int getTheme() { return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); }
    public void applyTheme() { AppCompatDelegate.setDefaultNightMode(getTheme()); }

    public void saveLanguage(String code) { editor.putString(KEY_LANGUAGE, code); editor.apply(); }
    public String getLanguage() { return prefs.getString(KEY_LANGUAGE, ""); }

    // Token y refresh se guardan/leen SIEMPRE del almacén cifrado (securePrefs), nunca en claro.
    public void saveToken(String token) { securePrefs.edit().putString(KEY_TOKEN, token).apply(); }
    public String getToken() { return securePrefs.getString(KEY_TOKEN, null); }
    public Boolean haySesion() { String t = getToken(); return t != null && !t.isEmpty(); }

    // Refresh token opaco: permite renovar el access token sin volver a introducir credenciales.
    public void saveRefreshToken(String refreshToken) { securePrefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply(); }
    public String getRefreshToken() { return securePrefs.getString(KEY_REFRESH_TOKEN, null); }

    // Guarda de una vez ambos tokens de la sesión (access + refresh) en el almacén cifrado.
    public void saveSesion(String token, String refreshToken) {
        securePrefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    // Borra los datos de sesión (tokens cifrados, id y username) pero conserva
    // las preferencias de onboarding, tema e idioma.
    public void cerrarSesion() {
        securePrefs.edit().remove(KEY_TOKEN).remove(KEY_REFRESH_TOKEN).apply();
        editor.remove(KEY_USUARIO_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }

    public void saveUsuarioId(int id) { editor.putInt(KEY_USUARIO_ID, id); editor.apply(); }
    public int getUsuarioId() { return prefs.getInt(KEY_USUARIO_ID, -1); }

    // Último token FCM enviado al backend (evita re-registrar si no cambió).
    public void saveFcmTokenEnviado(String token) { editor.putString("fcm_token_enviado", token); editor.apply(); }
    public String getFcmTokenEnviado() { return prefs.getString("fcm_token_enviado", ""); }
    public void clearFcmTokenEnviado() { editor.remove("fcm_token_enviado"); editor.apply(); }

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

    // Guarda de una sola vez el resultado nutricional calculado en el onboarding.
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

    // Marca el onboarding como completado para un usuario concreto (clave dinámica por username).
    public void setOnboardingCompletadoParaUsuario(String username) {
        editor.putBoolean("onboarding_done_" + username, true);
        editor.apply();
    }
    // Comprueba si el usuario indicado ya completó el onboarding.
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