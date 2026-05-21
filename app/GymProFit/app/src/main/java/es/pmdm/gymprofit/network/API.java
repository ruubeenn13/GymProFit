package es.pmdm.gymprofit.network;

import org.json.JSONException;
import org.json.JSONObject;

import es.pmdm.gymprofit.BuildConfig;

public class API {

    private static final String BASE = BuildConfig.BASE_URL;

    // ── AUTH ──────────────────────────────────────────────────────────────────

    public static void login(String username, String password, UtilREST.OnResponseListener l) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            UtilREST.request(BASE + "auth/login", "POST", body.toString(), l);
        } catch (JSONException e) {
            l.onError(e.getMessage(), -1);
        }
    }

    public static void register(String username, String password, String email, UtilREST.OnResponseListener l) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            body.put("email", email);
            UtilREST.request(BASE + "auth/register", "POST", body.toString(), l);
        } catch (JSONException e) {
            l.onError(e.getMessage(), -1);
        }
    }

    // ── USUARIOS ──────────────────────────────────────────────────────────────

    public static void getUsuarioPorUsername(String username, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/username/" + encode(username), "GET", null, l);
    }

    public static void getUsuarioPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/" + id, "GET", null, l);
    }

    public static void actualizarUsuario(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios", "PUT", body.toString(), l);
    }

    public static void patchUsuario(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/" + id, "PATCH", body.toString(), l);
    }

    public static void getEstadisticasUsuario(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/" + id + "/estadisticas", "GET", null, l);
    }

    // ── EJERCICIOS ────────────────────────────────────────────────────────────

    public static void getEjercicios(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios", "GET", null, l);
    }

    public static void getEjerciciosActivos(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/activos", "GET", null, l);
    }

    public static void getEjercicioPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id, "GET", null, l);
    }

    public static void getEjerciciosPorGrupo(String grupo, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/grupo/" + encode(grupo), "GET", null, l);
    }

    public static void buscarEjerciciosPorNombre(String nombre, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/nombre/" + encode(nombre), "GET", null, l);
    }

    // ── RUTINAS ───────────────────────────────────────────────────────────────

    public static void getRutinasPredefinidas(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/predefinidas", "GET", null, l);
    }

    public static void getRutinasPredefinidasPorNivel(String nivel, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/predefinidas/nivel/" + encode(nivel), "GET", null, l);
    }

    public static void getRutinasDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/usuario/" + usuarioId, "GET", null, l);
    }

    public static void getRutinasPorNivel(String nivel, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/nivel/" + encode(nivel), "GET", null, l);
    }

    public static void getRutinaPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "GET", null, l);
    }

    public static void getRutinaEjerciciosPorRutina(int rutinaId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas-ejercicios/rutina/" + rutinaId, "GET", null, l);
    }

    public static void eliminarEjercicioDeRutina(int rutinaId, int ejercicioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas-ejercicios/rutina/" + rutinaId + "/ejercicio/" + ejercicioId, "DELETE", null, l);
    }

    public static void crearRutina(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas", "POST", body.toString(), l);
    }

    public static void actualizarRutina(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "PUT", body.toString(), l);
    }

    public static void patchRutina(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "PATCH", body.toString(), l);
    }

    public static void eliminarRutina(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "DELETE", null, l);
    }

    public static void addEjercicioARutina(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas-ejercicios", "POST", body.toString(), l);
    }

    // ── SESIONES ──────────────────────────────────────────────────────────────

    public static void getSesionesDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/usuario/" + usuarioId, "GET", null, l);
    }

    public static void getSesionPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "GET", null, l);
    }

    public static void crearSesion(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones", "POST", body.toString(), l);
    }

    public static void actualizarSesion(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "PUT", body.toString(), l);
    }

    public static void patchSesion(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "PATCH", body.toString(), l);
    }

    public static void eliminarSesion(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "DELETE", null, l);
    }

    // ── LOGROS ────────────────────────────────────────────────────────────────

    public static void getLogros(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "logros", "GET", null, l);
    }

    public static void getLogrosDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "logros/usuario/" + usuarioId, "GET", null, l);
    }

    // ── OBJETIVOS PERSONALES ──────────────────────────────────────────────────

    public static void getObjetivosDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos/usuario/" + usuarioId, "GET", null, l);
    }

    public static void crearObjetivo(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos", "POST", body.toString(), l);
    }

    public static void patchObjetivo(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos/" + id, "PATCH", body.toString(), l);
    }

    public static void eliminarObjetivo(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos/" + id, "DELETE", null, l);
    }

    // ── MEDICIONES CORPORALES ─────────────────────────────────────────────────

    public static void getMedicionesDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/usuario/" + usuarioId + "/ordenadas", "GET", null, l);
    }

    public static void crearMedicion(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales", "POST", body.toString(), l);
    }

    public static void eliminarMedicion(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/" + id, "DELETE", null, l);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    public static void getAdminUsuarios(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios", "GET", null, l);
    }

    public static void getAdminEstadisticas(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/estadisticas-globales", "GET", null, l);
    }

    // ── UTIL ──────────────────────────────────────────────────────────────────

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
