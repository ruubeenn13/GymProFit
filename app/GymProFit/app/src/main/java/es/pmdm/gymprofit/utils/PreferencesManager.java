package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class PreferencesManager {

    private static final String PREF_NAME = "GymProFitPrefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_TOKEN = "auth_token";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveTheme(int themeMode) {
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
    }

    public int getTheme() {
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(getTheme());
    }

    public void saveLanguage(String languageCode) {
        editor.putString(KEY_LANGUAGE, languageCode);
        editor.apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "");
    }

    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(KEY_LANGUAGE, null);
    }

    public Boolean haySesion() {
        String token = getToken();
        return token != null && token.isEmpty();
    }

    public void cerrarSesion() {
        editor.remove(KEY_TOKEN);
        editor.apply();
    }
}