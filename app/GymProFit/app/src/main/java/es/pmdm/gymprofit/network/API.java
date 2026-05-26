package es.pmdm.gymprofit.network;

import android.content.Context;
import android.net.Uri;

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

    public static void uploadFotoPerfil(Context ctx, int userId, Uri imageUri, UtilREST.OnResponseListener l) {
        UtilREST.uploadMultipart(ctx, BASE + "usuarios/" + userId + "/foto", imageUri, "foto", l);
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
        UtilREST.request(BASE + "rutinas/usuario/" + usuarioId + "/activas", "GET", null, l);
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

    // ── EJERCICIOS REALIZADOS ─────────────────────────────────────────────────

    public static void crearEjercicioRealizado(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios-realizados", "POST", body.toString(), l);
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

    public static void patchMedicion(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/" + id, "PATCH", body.toString(), l);
    }

    public static void eliminarMedicion(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/" + id, "DELETE", null, l);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    public static void getAdminUsuarios(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios", "GET", null, l);
    }

    public static void getAdminUsuariosFiltrados(Boolean activo, String rol, String username, int page, int size, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/usuarios?page=" + page + "&size=" + size);
        if (activo != null) url.append("&activo=").append(activo);
        if (rol != null && !rol.isEmpty()) url.append("&rol=").append(encode(rol));
        if (username != null && !username.isEmpty()) url.append("&username=").append(encode(username));
        UtilREST.request(url.toString(), "GET", null, l);
    }

    public static void adminToggleActivoUsuario(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios/" + id + "/toggle-activo", "PATCH", null, l);
    }

    public static void adminCambiarRolUsuario(int id, String nuevoRol, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios/" + id + "/rol?nuevoRol=" + encode(nuevoRol), "PATCH", null, l);
    }

    public static void getAdminEstadisticas(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/estadisticas-globales", "GET", null, l);
    }

    public static void adminBuscarRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/rutinas/predefinidas/busqueda?");
        if (nombre != null && !nombre.isEmpty()) url.append("nombre=").append(encode(nombre)).append("&");
        if (nivel != null && !nivel.isEmpty()) url.append("nivel=").append(encode(nivel)).append("&");
        if (categoria != null && !categoria.isEmpty()) url.append("categoria=").append(encode(categoria)).append("&");
        if (activa != null) url.append("activa=").append(activa);
        UtilREST.request(url.toString(), "GET", null, l);
    }

    public static void adminBuscarEjercicios(String nombre, String grupoMuscular, String dificultad, Boolean activo, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/ejercicios/busqueda?");
        if (nombre != null && !nombre.isEmpty()) url.append("nombre=").append(encode(nombre)).append("&");
        if (grupoMuscular != null && !grupoMuscular.isEmpty()) url.append("grupoMuscular=").append(encode(grupoMuscular)).append("&");
        if (dificultad != null && !dificultad.isEmpty()) url.append("dificultad=").append(encode(dificultad)).append("&");
        if (activo != null) url.append("activo=").append(activo);
        UtilREST.request(url.toString(), "GET", null, l);
    }

    public static void adminActivarRutina(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id + "/activar", "PUT", null, l);
    }

    public static void adminDesactivarRutina(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "DELETE", null, l);
    }

    public static void adminActivarEjercicio(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id + "/activar", "PUT", null, l);
    }

    public static void adminDesactivarEjercicio(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id, "DELETE", null, l);
    }

    public static void adminEditarRutina(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "PATCH", body.toString(), l);
    }

    public static void adminEditarEjercicio(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id, "PATCH", body.toString(), l);
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
