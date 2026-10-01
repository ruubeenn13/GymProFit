package es.pmdm.gymprofit.utils;

// MODIFICADO - Añadidos campos para usuarioId, username, sexo, actividad,
// objetivo, calorías, macros y agua calculados en el onboarding
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// ============================================================
// PreferencesManager — encapsula el acceso a SharedPreferences de la app.
// Centraliza persistencia de tema, idioma, sesión (token/usuario), datos de
// onboarding (nivel, objetivo, sexo, actividad), resultados nutricionales y
// datos físicos del usuario, evitando el acceso directo desde las Activities.
// ============================================================
public class PreferencesManager implements PerfilCuenta.Almacen {

    // Nombre del archivo de SharedPreferences y claves usadas para cada dato guardado.
    private static final String PREF_NAME = "GymProFitPrefs";
    // Archivo SEPARADO y CIFRADO (EncryptedSharedPreferences) solo para los tokens sensibles.
    private static final String SECURE_PREF_NAME = "gymprofit_secure_prefs";
    private static final String KEY_THEME = "theme_mode";
    // Descanso entre series (GP-013): del móvil, no de la cuenta, como el tema.
    private static final String KEY_DESCANSO_AL_MARCAR = "descanso_al_marcar";
    private static final String KEY_DESCANSO_SIN_PAUTA = "descanso_sin_pauta";
    private static final String KEY_HOJA_PERMISO_DESCANSO = "descanso_hoja_permiso_vista";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NIVEL = "nivel_experiencia";
    private static final String KEY_OBJETIVO = "objetivo";
    private static final String KEY_SEXO = "sexo";
    private static final String KEY_ACTIVIDAD = "actividad";
    // De qué cuenta son el sexo y la actividad guardados (GP-111): ver PerfilCuenta.
    private static final String KEY_PERFIL_DUENO = "perfil_dueno";
    private static final String PREFIJO_ONBOARDING = "onboarding_done_";
    private static final String PREFIJO_NOMBRE = "nombre_";
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

    // Borrador del onboarding: lo que el usuario lleva contestado mientras el
    // asistente sigue a medias. Se borra al terminarlo o al saltarlo, y es
    // distinto de las claves de arriba, que son ya el perfil definitivo.
    private static final String KEY_OB_NOMBRE    = "ob_nombre";
    // Ya no se escribe (GP-083: el asistente no pide el correo). Se sigue borrando en
    // limpiarBorradorOnboarding para no dejar el de un borrador anterior en el móvil.
    private static final String KEY_OB_EMAIL     = "ob_email";
    private static final String KEY_OB_EDAD      = "ob_edad";
    private static final String KEY_OB_SEXO      = "ob_sexo";
    private static final String KEY_OB_PESO      = "ob_peso";
    private static final String KEY_OB_ALTURA    = "ob_altura";
    private static final String KEY_OB_ACTIVIDAD = "ob_actividad";
    private static final String KEY_OB_OBJETIVO  = "ob_objetivo";
    private static final String KEY_OB_NIVEL     = "ob_nivel";
    // Del alta nueva (GP-103, lote 1.5.1).
    private static final String KEY_OB_DONDE     = "ob_donde";
    private static final String KEY_OB_DIAS      = "ob_dias";
    private static final String KEY_OB_MINUTOS   = "ob_minutos";
    private static final String KEY_OB_SIN_DATOS = "ob_sin_datos";
    private static final String KEY_OB_PASO      = "ob_paso";
    private static final String KEY_OB_PROGRAMA  = "ob_programa";
    private static final String KEY_OB_PROGRAMA_NOMBRE = "ob_programa_nombre";

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
    // Oscuro por defecto (rediseño atlético); el usuario puede forzar claro desde el menú.
    public int getTheme() { return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES); }
    public void applyTheme() { AppCompatDelegate.setDefaultNightMode(getTheme()); }

    /** «Empezar el descanso al marcar una serie»: sí, de serie (GP-013). */
    public boolean getDescansoAlMarcar() { return prefs.getBoolean(KEY_DESCANSO_AL_MARCAR, true); }
    public void saveDescansoAlMarcar(boolean si) { editor.putBoolean(KEY_DESCANSO_AL_MARCAR, si).apply(); }

    /** «Descanso sin pauta», en segundos: una de las opciones, 90 de serie. */
    public int getDescansoSinPauta() {
        return es.pmdm.gymprofit.envivo.LogicaDescanso.sinPautaValido(
                prefs.getInt(KEY_DESCANSO_SIN_PAUTA, es.pmdm.gymprofit.envivo.LogicaDescanso.SIN_PAUTA_DEFECTO));
    }
    public void saveDescansoSinPauta(int segundos) { editor.putInt(KEY_DESCANSO_SIN_PAUTA, segundos).apply(); }

    /** Si ya salió sola la hoja del permiso de avisar con la pantalla apagada: solo una vez. */
    public boolean getHojaPermisoDescansoVista() { return prefs.getBoolean(KEY_HOJA_PERMISO_DESCANSO, false); }
    public void saveHojaPermisoDescansoVista() { editor.putBoolean(KEY_HOJA_PERMISO_DESCANSO, true).apply(); }

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

    // Borra solo los tokens del almacén cifrado (lo usa UtilREST al cerrar la sesión).
    public void borrarTokens() {
        securePrefs.edit().remove(KEY_TOKEN).remove(KEY_REFRESH_TOKEN).apply();
    }

    // Borra los datos de sesión (tokens cifrados, id y username) pero conserva
    // las preferencias de onboarding, tema e idioma.
    public void cerrarSesion() {
        borrarTokens();
        editor.remove(KEY_USUARIO_ID);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }

    /**
     * Borra del dispositivo todo rastro de la cuenta (GP-008).
     *
     * <p>No vale con {@link #cerrarSesion()}: cerrar sesión conserva a propósito el
     * perfil local (peso, altura, macros, nivel, borrador del onboarding y la marca
     * de onboarding completado) para que volver a entrar no obligue a repetirlo.
     * Cuando la cuenta ya no existe en el servidor, ese perfil local es un residuo
     * de datos personales de alguien que ha pedido justo lo contrario, y además
     * reaparecería en la siguiente cuenta que se creara en el mismo móvil.
     *
     * <p>Se vacía el almacén cifrado entero (no solo las dos claves de sesión) y se
     * tiran todas las preferencias en claro <b>salvo tema e idioma</b>, que son
     * ajustes del dispositivo y no datos de la cuenta: reiniciarlos dejaría la app
     * en otro color y otro idioma justo después de borrarse, sin explicación.
     */
    public void borrarDatosLocales() {
        securePrefs.edit().clear().apply();

        int tema = getTheme();
        String idioma = getLanguage();

        editor.clear();
        editor.putInt(KEY_THEME, tema);
        editor.putString(KEY_LANGUAGE, idioma);
        editor.apply();
    }

    public void saveUsuarioId(int id) { editor.putInt(KEY_USUARIO_ID, id); editor.apply(); }
    public int getUsuarioId() { return prefs.getInt(KEY_USUARIO_ID, -1); }

    // Último token FCM enviado al backend (evita re-registrar si no cambió).
    public void saveFcmTokenEnviado(String token) { editor.putString("fcm_token_enviado", token); editor.apply(); }
    public String getFcmTokenEnviado() { return prefs.getString("fcm_token_enviado", ""); }
    public void clearFcmTokenEnviado() { editor.remove("fcm_token_enviado"); editor.apply(); }

    // ── Programas (GP-074, lote 1.2.1) ──
    // Dónde entrena y cuántos días, lo último que eligió en Programas. Por cuenta: en un
    // móvil con dos cuentas cada una tiene lo suyo. La primera vez, gimnasio y 3 días.
    private static final String PREFIJO_PROGRAMAS_EQUIPAMIENTO = "programas_equipamiento_";
    private static final String PREFIJO_PROGRAMAS_DIAS = "programas_dias_";

    public String getProgramasEquipamiento() {
        return prefs.getString(PREFIJO_PROGRAMAS_EQUIPAMIENTO + getUsuarioId(), "GIMNASIO");
    }

    public int getProgramasDias() {
        return prefs.getInt(PREFIJO_PROGRAMAS_DIAS + getUsuarioId(), 3);
    }

    public void saveProgramasFiltros(String equipamiento, int dias) {
        editor.putString(PREFIJO_PROGRAMAS_EQUIPAMIENTO + getUsuarioId(), equipamiento);
        editor.putInt(PREFIJO_PROGRAMAS_DIAS + getUsuarioId(), dias);
        editor.apply();
    }

    public void saveUsername(String username) { editor.putString(KEY_USERNAME, username); editor.apply(); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, ""); }

    // Nombre para mostrar (GP-116), guardado por cuenta: en un móvil con dos cuentas
    // cada una ve el suyo nada más entrar, antes de que conteste la API.
    public void saveNombre(String username, @Nullable String nombre) {
        if (username == null || username.isEmpty()) return;
        if (nombre == null || nombre.trim().isEmpty()) editor.remove(PREFIJO_NOMBRE + username);
        else editor.putString(PREFIJO_NOMBRE + username, nombre.trim());
        editor.apply();
    }

    /** Nombre para mostrar de la cuenta que ha entrado, o "" si no tiene. */
    public String getNombre() { return prefs.getString(PREFIJO_NOMBRE + getUsername(), ""); }

    public void saveNivel(String nivel) { editor.putString(KEY_NIVEL, nivel); editor.apply(); }
    // Nivel, objetivo, peso, altura y edad: como el sexo, el de otra cuenta no se usa (GP-129).
    public String getNivel() { return perfilUsable() ? prefs.getString(KEY_NIVEL, "") : ""; }

    public void saveObjetivo(String objetivo) { editor.putString(KEY_OBJETIVO, objetivo); editor.apply(); }
    public String getObjetivo() { return perfilUsable() ? prefs.getString(KEY_OBJETIVO, "") : ""; }

    // Sexo y actividad (GP-111). Los getters dan el valor por defecto si lo guardado es
    // de otra cuenta del mismo móvil: para la que ha entrado es como si no hubiera nada.
    public void saveSexo(String sexo) { editor.putString(KEY_SEXO, sexo); editor.apply(); }
    public String getSexo() { return perfilUsable() ? prefs.getString(KEY_SEXO, "HOMBRE") : "HOMBRE"; }

    public void saveActividad(String actividad) { editor.putString(KEY_ACTIVIDAD, actividad); editor.apply(); }
    public String getActividad() { return perfilUsable() ? prefs.getString(KEY_ACTIVIDAD, "MODERADO") : "MODERADO"; }

    // Clave de las preferencias de cada campo del perfil que se cruza con la API.
    private static String clave(PerfilCuenta.Campo campo) {
        switch (campo) {
            case SEXO:      return KEY_SEXO;
            case ACTIVIDAD: return KEY_ACTIVIDAD;
            case PESO:      return KEY_PESO;
            case ALTURA:    return KEY_ALTURA;
            case EDAD:      return KEY_EDAD;
            case OBJETIVO:  return KEY_OBJETIVO;
            default:        return KEY_NIVEL;
        }
    }

    /**
     * El valor guardado tal cual, como texto, o null si la clave no existe: así se
     * distingue un valor elegido del valor por defecto que dan los getters.
     */
    @Nullable @Override
    public String leer(@NonNull PerfilCuenta.Campo campo) {
        String k = clave(campo);
        if (!prefs.contains(k)) return null;
        switch (campo) {
            case PESO:
            case ALTURA: return String.valueOf(prefs.getFloat(k, 0f));
            case EDAD:   return String.valueOf(prefs.getInt(k, 0));
            default:
                String v = prefs.getString(k, "");
                return v.isEmpty() ? null : v;
        }
    }

    @Override
    public void guardar(@NonNull PerfilCuenta.Campo campo, @NonNull String valor) {
        String k = clave(campo);
        switch (campo) {
            case PESO:
            case ALTURA: editor.putFloat(k, Float.parseFloat(valor)); break;
            case EDAD:   editor.putInt(k, Integer.parseInt(valor)); break;
            default:     editor.putString(k, valor);
        }
        editor.apply();
    }

    @Override
    public void borrar(@NonNull PerfilCuenta.Campo campo) { editor.remove(clave(campo)); editor.apply(); }

    /** Usuario de quien son el sexo y la actividad guardados; null en instalaciones anteriores. */
    @Nullable @Override
    public String getDuenoPerfil() { return prefs.getString(KEY_PERFIL_DUENO, null); }

    @Override
    public void apuntarDuenoPerfil(String usuario) { editor.putString(KEY_PERFIL_DUENO, usuario); editor.apply(); }

    /** Usuarios que han terminado el onboarding en este móvil. */
    @NonNull @Override
    public Set<String> getCuentasConOnboarding() {
        Set<String> cuentas = new HashSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith(PREFIJO_ONBOARDING) && Boolean.TRUE.equals(e.getValue())) {
                cuentas.add(e.getKey().substring(PREFIJO_ONBOARDING.length()));
            }
        }
        return cuentas;
    }

    private boolean perfilUsable() {
        return PerfilCuenta.usable(PerfilCuenta.propiedad(getDuenoPerfil(), getCuentasConOnboarding(), getUsername()));
    }

    // Guarda de una sola vez el resultado nutricional calculado en el onboarding.
    public void saveResultadoNutricional(int calorias, int proteinas, int carbos, int grasas, double agua) {
        editor.putInt(KEY_CALORIAS, calorias);
        editor.putInt(KEY_PROTEINAS, proteinas);
        editor.putInt(KEY_CARBOS, carbos);
        editor.putInt(KEY_GRASAS, grasas);
        editor.putFloat(KEY_AGUA, (float) agua);
        editor.apply();
    }

    /**
     * Si hay con qué calcular las calorías: peso y altura de esta cuenta (GP-103). Sin
     * ellos —«Prefiero no decirlo» en el alta— no hay objetivo, en vez de uno inventado
     * con los valores por defecto.
     */
    public boolean hayDatosParaCalorias() {
        return perfilUsable() && prefs.contains(KEY_PESO) && prefs.contains(KEY_ALTURA);
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
        editor.putBoolean(PREFIJO_ONBOARDING + username, true);
        editor.apply();
    }
    // Comprueba si el usuario indicado ya completó el onboarding.
    public boolean isOnboardingCompletadoParaUsuario(String username) {
        if (username == null || username.isEmpty()) return false;
        return prefs.getBoolean(PREFIJO_ONBOARDING + username, false);
    }

    // ------------------------------------------------------------------
    // Borrador del onboarding
    //
    // El asistente tiene seis pantallas y antes cada una arrastraba en los
    // extras del Intent todo lo contestado hasta entonces. Si el sistema mataba
    // la app a mitad (una llamada, quedarse sin memoria), esos extras se perdian
    // y no habia forma de retomarlo: se empezaba de cero. Persistiendo cada paso
    // al avanzar, el asistente se reanuda con lo ya contestado.
    // ------------------------------------------------------------------

    /**
     * Guarda las respuestas del paso de datos personales.
     *
     * @param edad en anios, o 0 si el usuario lo dejo en blanco (es opcional).
     */
    public void guardarBorradorDatos(String nombre, int edad, String sexo) {
        editor.putString(KEY_OB_NOMBRE, nombre);
        editor.putInt(KEY_OB_EDAD, edad);
        editor.putString(KEY_OB_SEXO, sexo);
        editor.apply();
    }

    /**
     * Guarda las respuestas del paso fisico.
     *
     * @param peso ya normalizado con punto decimal, para no depender del teclado.
     * @param altura en centimetros.
     */
    public void guardarBorradorFisico(String peso, double altura, String actividad) {
        editor.putString(KEY_OB_PESO, peso);
        editor.putFloat(KEY_OB_ALTURA, (float) altura);
        editor.putString(KEY_OB_ACTIVIDAD, actividad);
        editor.apply();
    }

    /** Guarda el objetivo elegido en el paso de objetivos. */
    public void guardarBorradorObjetivo(String objetivo) {
        editor.putString(KEY_OB_OBJETIVO, objetivo);
        editor.apply();
    }

    /** Guarda el nivel de experiencia elegido en el ultimo paso. */
    public void guardarBorradorNivel(String nivel) {
        editor.putString(KEY_OB_NIVEL, nivel);
        editor.apply();
    }

    public String getBorradorNombre()    { return prefs.getString(KEY_OB_NOMBRE, ""); }
    public int    getBorradorEdad()      { return prefs.getInt(KEY_OB_EDAD, 0); }
    public String getBorradorSexo()      { return prefs.getString(KEY_OB_SEXO, "HOMBRE"); }
    public String getBorradorPeso()      { return prefs.getString(KEY_OB_PESO, ""); }
    public double getBorradorAltura()    { return prefs.getFloat(KEY_OB_ALTURA, 0f); }
    public String getBorradorActividad() { return prefs.getString(KEY_OB_ACTIVIDAD, ""); }
    public String getBorradorObjetivo()  { return prefs.getString(KEY_OB_OBJETIVO, ""); }
    public String getBorradorNivel()     { return prefs.getString(KEY_OB_NIVEL, ""); }

    // ── Borrador del alta nueva (GP-103, lote 1.5.1) ──
    // Cada respuesta se guarda al elegirla, no al pulsar «Siguiente»: si Android cierra
    // la app a mitad de una pregunta, lo elegido sigue ahí al volver.

    public void guardarBorradorSexo(String sexo)        { editor.putString(KEY_OB_SEXO, sexo).apply(); }
    public void guardarBorradorEdad(int edad)           { editor.putInt(KEY_OB_EDAD, edad).apply(); }
    public void guardarBorradorAltura(double altura)    { editor.putFloat(KEY_OB_ALTURA, (float) altura).apply(); }
    /** @param peso ya normalizado con punto decimal. */
    public void guardarBorradorPeso(String peso)        { editor.putString(KEY_OB_PESO, peso).apply(); }
    public void guardarBorradorActividad(String a)      { editor.putString(KEY_OB_ACTIVIDAD, a).apply(); }
    public void guardarBorradorDonde(String donde)      { editor.putString(KEY_OB_DONDE, donde).apply(); }
    public void guardarBorradorDias(int dias)           { editor.putInt(KEY_OB_DIAS, dias).apply(); }
    public void guardarBorradorMinutos(int minutos)     { editor.putInt(KEY_OB_MINUTOS, minutos).apply(); }
    /** «Prefiero no decirlo» en «Sobre ti»: el plan sale sin calorías. */
    public void guardarBorradorSinDatos(boolean si)     { editor.putBoolean(KEY_OB_SIN_DATOS, si).apply(); }
    /** La pantalla del cuestionario en la que se está, por su orden. */
    public void guardarBorradorPaso(int paso)           { editor.putInt(KEY_OB_PASO, paso).apply(); }

    /** El sexo elegido, o "" si aún no (getBorradorSexo da HOMBRE por defecto). */
    public String getBorradorSexoElegido() { return prefs.getString(KEY_OB_SEXO, ""); }
    public String getBorradorDonde()       { return prefs.getString(KEY_OB_DONDE, ""); }
    public int    getBorradorDias()        { return prefs.getInt(KEY_OB_DIAS, 0); }
    public int    getBorradorMinutos()     { return prefs.getInt(KEY_OB_MINUTOS, 0); }
    public boolean isBorradorSinDatos()    { return prefs.getBoolean(KEY_OB_SIN_DATOS, false); }
    public int    getBorradorPaso()        { return prefs.getInt(KEY_OB_PASO, 0); }

    /** El programa que recomendó «Tu plan», para seguirlo al crear la cuenta. */
    public void guardarBorradorPrograma(String codigo, String nombre) {
        editor.putString(KEY_OB_PROGRAMA, codigo).putString(KEY_OB_PROGRAMA_NOMBRE, nombre).apply();
    }
    public String getBorradorProgramaCodigo() { return prefs.getString(KEY_OB_PROGRAMA, ""); }
    public String getBorradorProgramaNombre() { return prefs.getString(KEY_OB_PROGRAMA_NOMBRE, ""); }

    /** Borra las cinco respuestas de «Sobre ti» (al elegir «Prefiero no decirlo»). */
    public void borrarBorradorSobreTi() {
        editor.remove(KEY_OB_SEXO).remove(KEY_OB_EDAD).remove(KEY_OB_ALTURA)
              .remove(KEY_OB_PESO).remove(KEY_OB_ACTIVIDAD).apply();
    }

    /** Lo contestado, de una vez, para las reglas de AltaPasos. */
    public AltaPasos.Respuestas getBorradorRespuestas() {
        return new AltaPasos.Respuestas(getBorradorObjetivo(), getBorradorNivel(), getBorradorSexoElegido(),
                getBorradorEdad(), getBorradorAltura(), getBorradorPeso(), getBorradorActividad(),
                isBorradorSinDatos(), getBorradorDonde(), getBorradorDias(), getBorradorMinutos());
    }

    /**
     * Tira el borrador. Se llama al terminar el asistente y al saltarlo: en
     * ambos casos deja de haber nada que reanudar.
     */
    public void limpiarBorradorOnboarding() {
        editor.remove(KEY_OB_NOMBRE).remove(KEY_OB_EMAIL).remove(KEY_OB_EDAD)
              .remove(KEY_OB_SEXO).remove(KEY_OB_PESO).remove(KEY_OB_ALTURA)
              .remove(KEY_OB_ACTIVIDAD).remove(KEY_OB_OBJETIVO).remove(KEY_OB_NIVEL)
              .remove(KEY_OB_DONDE).remove(KEY_OB_DIAS).remove(KEY_OB_MINUTOS)
              .remove(KEY_OB_SIN_DATOS).remove(KEY_OB_PASO)
              .remove(KEY_OB_PROGRAMA).remove(KEY_OB_PROGRAMA_NOMBRE);
        editor.apply();
    }

    public void saveRol(String rol) { editor.putString(KEY_ROL, rol); editor.apply(); }
    public String getRol() { return prefs.getString(KEY_ROL, "ROLE_USER"); }
    public boolean isAdmin() { return "ROLE_ADMIN".equals(getRol()); }
    public boolean isGuest() { return "ROLE_GUEST".equals(getRol()); }

    public void savePeso(double peso)     { editor.putFloat(KEY_PESO, (float) peso); editor.apply(); }
    public double getPeso()               { return perfilUsable() ? prefs.getFloat(KEY_PESO, 70.0f) : 70.0; }

    public void saveAltura(double altura) { editor.putFloat(KEY_ALTURA, (float) altura); editor.apply(); }
    public double getAltura()             { return perfilUsable() ? prefs.getFloat(KEY_ALTURA, 170.0f) : 170.0; }

    public void saveEdad(int edad)        { editor.putInt(KEY_EDAD, edad); editor.apply(); }
    public int getEdad()                  { return perfilUsable() ? prefs.getInt(KEY_EDAD, 25) : 25; }
}