package es.pmdm.gymprofit.network;

import android.content.Context;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import es.pmdm.gymprofit.BuildConfig;

// ============================================================
// API — punto único de acceso a los endpoints REST de GymProFit
// Clase de métodos estáticos que construye las URLs y delega en UtilREST
// para ejecutar cada petición HTTP (GET/POST/PUT/PATCH/DELETE) contra la
// API Spring Boot. Organizada por dominio (auth, usuarios, ejercicios,
// rutinas, sesiones, logros, objetivos, mediciones, admin, nutrición).
// ============================================================
public class API {

    // URL base de la API, inyectada por Gradle según el build type/flavor
    private static final String BASE = BuildConfig.BASE_URL;

    // ── AUTH ──────────────────────────────────────────────────────────────────

    // Autentica a un usuario con username/password
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

    // Registra un nuevo usuario con rol USER por defecto
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

    // Crea una sesión de invitado (rol GUEST) sin credenciales
    public static void loginAsGuest(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "auth/guest", "POST", null, l);
    }

    // ── USUARIOS ──────────────────────────────────────────────────────────────

    // Busca un usuario por su username
    public static void getUsuarioPorUsername(String username, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/username/" + encode(username), "GET", null, l);
    }

    // Obtiene el perfil de un usuario por su id
    public static void getUsuarioPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/" + id, "GET", null, l);
    }

    // Actualiza (reemplazo completo) los datos de un usuario. Reservado a ADMIN
    public static void actualizarUsuario(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios", "PUT", body.toString(), l);
    }

    // Actualiza parcialmente los datos de un usuario (usado en onboarding y edición de perfil)
    public static void patchUsuario(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/" + id, "PATCH", body.toString(), l);
    }

    // Sube la foto de perfil del usuario como multipart/form-data
    public static void uploadFotoPerfil(Context ctx, int userId, Uri imageUri, UtilREST.OnResponseListener l) {
        UtilREST.uploadMultipart(ctx, BASE + "usuarios/" + userId + "/foto", imageUri, "foto", l);
    }

    // Obtiene las estadísticas de entrenamiento agregadas de un usuario
    public static void getEstadisticasUsuario(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "usuarios/" + id + "/estadisticas", "GET", null, l);
    }

    // ── EJERCICIOS ────────────────────────────────────────────────────────────

    // Obtiene todos los ejercicios (activos e inactivos)
    public static void getEjercicios(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios", "GET", null, l);
    }

    // Obtiene solo los ejercicios activos
    public static void getEjerciciosActivos(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/activos", "GET", null, l);
    }

    // Obtiene el detalle de un ejercicio por id
    public static void getEjercicioPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id, "GET", null, l);
    }

    // Filtra ejercicios por grupo muscular
    public static void getEjerciciosPorGrupo(String grupo, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/grupo/" + encode(grupo), "GET", null, l);
    }

    // Busca ejercicios cuyo nombre contenga el texto indicado
    public static void buscarEjerciciosPorNombre(String nombre, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/nombre/" + encode(nombre), "GET", null, l);
    }

    // ── RUTINAS ───────────────────────────────────────────────────────────────

    // Obtiene todas las rutinas predefinidas por el sistema/admin
    public static void getRutinasPredefinidas(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/predefinidas", "GET", null, l);
    }

    // Obtiene rutinas predefinidas filtradas por nivel de experiencia
    public static void getRutinasPredefinidasPorNivel(String nivel, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/predefinidas/nivel/" + encode(nivel), "GET", null, l);
    }

    // Obtiene las rutinas activas creadas o asignadas a un usuario
    public static void getRutinasDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/usuario/" + usuarioId + "/activas", "GET", null, l);
    }

    // Obtiene rutinas filtradas por nivel de experiencia
    public static void getRutinasPorNivel(String nivel, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/nivel/" + encode(nivel), "GET", null, l);
    }

    // Obtiene el detalle de una rutina por id
    public static void getRutinaPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "GET", null, l);
    }

    // Obtiene los ejercicios asociados a una rutina
    public static void getRutinaEjerciciosPorRutina(int rutinaId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas-ejercicios/rutina/" + rutinaId, "GET", null, l);
    }

    // Elimina un ejercicio concreto de una rutina
    public static void eliminarEjercicioDeRutina(int rutinaId, int ejercicioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas-ejercicios/rutina/" + rutinaId + "/ejercicio/" + ejercicioId, "DELETE", null, l);
    }

    // Crea una nueva rutina
    public static void crearRutina(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas", "POST", body.toString(), l);
    }

    // Actualiza (reemplazo completo) una rutina existente
    public static void actualizarRutina(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "PUT", body.toString(), l);
    }

    // Actualiza parcialmente una rutina existente
    public static void patchRutina(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "PATCH", body.toString(), l);
    }

    // Elimina (borrado lógico) una rutina
    public static void eliminarRutina(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "DELETE", null, l);
    }

    // Añade un ejercicio a una rutina existente
    public static void addEjercicioARutina(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas-ejercicios", "POST", body.toString(), l);
    }

    // ── SESIONES ──────────────────────────────────────────────────────────────

    // Obtiene las sesiones de entrenamiento de un usuario
    public static void getSesionesDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/usuario/" + usuarioId, "GET", null, l);
    }

    // Obtiene el detalle de una sesión por id
    public static void getSesionPorId(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "GET", null, l);
    }

    // Crea una nueva sesión de entrenamiento
    public static void crearSesion(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones", "POST", body.toString(), l);
    }

    // Actualiza (reemplazo completo) una sesión existente
    public static void actualizarSesion(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "PUT", body.toString(), l);
    }

    // Actualiza parcialmente una sesión (p.ej. marcarla como completada)
    public static void patchSesion(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "PATCH", body.toString(), l);
    }

    // Elimina una sesión de entrenamiento
    public static void eliminarSesion(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "sesiones/" + id, "DELETE", null, l);
    }

    // ── EJERCICIOS REALIZADOS ─────────────────────────────────────────────────

    // Registra un ejercicio realizado dentro de una sesión (series, repeticiones, peso, etc.)
    public static void crearEjercicioRealizado(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios-realizados", "POST", body.toString(), l);
    }

    // ── LOGROS ────────────────────────────────────────────────────────────────

    // Obtiene el catálogo completo de logros disponibles
    public static void getLogros(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "logros", "GET", null, l);
    }

    // Obtiene los logros desbloqueados por un usuario
    public static void getLogrosDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "logros/usuario/" + usuarioId, "GET", null, l);
    }

    // ── OBJETIVOS PERSONALES ──────────────────────────────────────────────────

    // Obtiene los objetivos personales de un usuario
    public static void getObjetivosDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos/usuario/" + usuarioId, "GET", null, l);
    }

    // Crea un nuevo objetivo personal
    public static void crearObjetivo(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos", "POST", body.toString(), l);
    }

    // Actualiza parcialmente un objetivo (p.ej. progreso o estado)
    public static void patchObjetivo(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos/" + id, "PATCH", body.toString(), l);
    }

    // Elimina un objetivo personal
    public static void eliminarObjetivo(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "objetivos/" + id, "DELETE", null, l);
    }

    // ── MEDICIONES CORPORALES ─────────────────────────────────────────────────

    // Obtiene el historial de mediciones corporales de un usuario, ordenado por fecha
    public static void getMedicionesDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/usuario/" + usuarioId + "/ordenadas", "GET", null, l);
    }

    // Registra una nueva medición corporal
    public static void crearMedicion(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales", "POST", body.toString(), l);
    }

    // Actualiza parcialmente una medición corporal existente
    public static void patchMedicion(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/" + id, "PATCH", body.toString(), l);
    }

    // Elimina una medición corporal
    public static void eliminarMedicion(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "mediciones-corporales/" + id, "DELETE", null, l);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    // Obtiene el listado completo de usuarios (solo ADMIN)
    public static void getAdminUsuarios(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios", "GET", null, l);
    }

    // Obtiene usuarios paginados y filtrados por activo/rol/username (solo ADMIN)
    public static void getAdminUsuariosFiltrados(Boolean activo, String rol, String username, int page, int size, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/usuarios?page=" + page + "&size=" + size);
        if (activo != null) url.append("&activo=").append(activo);
        if (rol != null && !rol.isEmpty()) url.append("&rol=").append(encode(rol));
        if (username != null && !username.isEmpty()) url.append("&username=").append(encode(username));
        UtilREST.request(url.toString(), "GET", null, l);
    }

    // Activa/desactiva la cuenta de un usuario (solo ADMIN)
    public static void adminToggleActivoUsuario(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios/" + id + "/toggle-activo", "PATCH", null, l);
    }

    // Cambia el rol de un usuario (solo ADMIN)
    public static void adminCambiarRolUsuario(int id, String nuevoRol, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/usuarios/" + id + "/rol?nuevoRol=" + encode(nuevoRol), "PATCH", null, l);
    }

    // Obtiene estadísticas globales de la aplicación (solo ADMIN)
    public static void getAdminEstadisticas(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "admin/estadisticas-globales", "GET", null, l);
    }

    // Busca rutinas predefinidas con filtros combinables (nombre, nivel, categoría, activa)
    public static void adminBuscarRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/rutinas/predefinidas/busqueda?");
        if (nombre != null && !nombre.isEmpty()) url.append("nombre=").append(encode(nombre)).append("&");
        if (nivel != null && !nivel.isEmpty()) url.append("nivel=").append(encode(nivel)).append("&");
        if (categoria != null && !categoria.isEmpty()) url.append("categoria=").append(encode(categoria)).append("&");
        if (activa != null) url.append("activa=").append(activa);
        UtilREST.request(url.toString(), "GET", null, l);
    }

    // Busca ejercicios con filtros combinables (nombre, grupo muscular, dificultad, activo)
    public static void adminBuscarEjercicios(String nombre, String grupoMuscular, String dificultad, Boolean activo, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/ejercicios/busqueda?");
        if (nombre != null && !nombre.isEmpty()) url.append("nombre=").append(encode(nombre)).append("&");
        if (grupoMuscular != null && !grupoMuscular.isEmpty()) url.append("grupoMuscular=").append(encode(grupoMuscular)).append("&");
        if (dificultad != null && !dificultad.isEmpty()) url.append("dificultad=").append(encode(dificultad)).append("&");
        if (activo != null) url.append("activo=").append(activo);
        UtilREST.request(url.toString(), "GET", null, l);
    }

    // Reactiva una rutina previamente desactivada
    public static void adminActivarRutina(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id + "/activar", "PUT", null, l);
    }

    // Desactiva (borrado lógico) una rutina
    public static void adminDesactivarRutina(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "DELETE", null, l);
    }

    // Reactiva un ejercicio previamente desactivado
    public static void adminActivarEjercicio(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id + "/activar", "PUT", null, l);
    }

    // Desactiva (borrado lógico) un ejercicio
    public static void adminDesactivarEjercicio(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id, "DELETE", null, l);
    }

    // Edita parcialmente una rutina desde el panel de administración
    public static void adminEditarRutina(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "rutinas/" + id, "PATCH", body.toString(), l);
    }

    // Edita parcialmente un ejercicio desde el panel de administración
    public static void adminEditarEjercicio(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "ejercicios/" + id, "PATCH", body.toString(), l);
    }

    // ── NUTRICION ─────────────────────────────────────────────────────────────

    // Obtiene el catálogo de alimentos activos
    public static void getAlimentosActivos(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos/activos", "GET", null, l);
    }

    // Busca alimentos cuyo nombre contenga el texto indicado
    public static void buscarAlimentosPorNombre(String nombre, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos/nombre/" + encode(nombre), "GET", null, l);
    }

    // Obtiene los alimentos personalizados creados por un usuario
    public static void getAlimentosDeUsuario(int usuarioId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos/usuario/" + usuarioId, "GET", null, l);
    }

    // Obtiene las categorías de alimentos disponibles
    public static void getCategorias(UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos/categorias", "GET", null, l);
    }

    // Crea un nuevo alimento (personalizado por el usuario)
    public static void crearAlimento(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos", "POST", body.toString(), l);
    }

    // Obtiene las comidas registradas por un usuario en una fecha concreta
    public static void getComidasDeUsuarioFecha(int usuarioId, String fecha, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "comidas/usuario/" + usuarioId + "/fecha/" + encode(fecha), "GET", null, l);
    }

    // Crea una nueva comida (desayuno, almuerzo, etc.)
    public static void crearComida(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "comidas", "POST", body.toString(), l);
    }

    // Elimina una comida registrada
    public static void eliminarComida(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "comidas/" + id, "DELETE", null, l);
    }

    // Obtiene los alimentos asociados a una comida concreta
    public static void getAlimentosDeComida(int comidaId, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos-comida/comida/" + comidaId, "GET", null, l);
    }

    // Añade un alimento (con cantidad) a una comida
    public static void anadirAlimentoAComida(JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos-comida", "POST", body.toString(), l);
    }

    // Elimina un alimento de una comida
    public static void eliminarAlimentoDeComida(int id, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos-comida/" + id, "DELETE", null, l);
    }

    // Actualiza parcialmente la relación alimento-comida (p.ej. cantidad)
    public static void patchAlimentoComida(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos-comida/" + id, "PATCH", body.toString(), l);
    }

    // Busca alimentos con filtros combinables (nombre, categoría, activo) para el panel admin
    public static void adminBuscarAlimentos(String nombre, String categoria, Boolean activo, UtilREST.OnResponseListener l) {
        StringBuilder url = new StringBuilder(BASE + "admin/alimentos/busqueda?");
        if (nombre != null && !nombre.isEmpty()) url.append("nombre=").append(encode(nombre)).append("&");
        if (categoria != null && !categoria.isEmpty()) url.append("categoria=").append(encode(categoria)).append("&");
        if (activo != null) url.append("activo=").append(activo);
        UtilREST.request(url.toString(), "GET", null, l);
    }

    // Activa o desactiva (borrado lógico) un alimento según el flag "activar"
    public static void adminToggleActivoAlimento(int id, boolean activar, UtilREST.OnResponseListener l) {
        if (activar) {
            UtilREST.request(BASE + "alimentos/" + id + "/activar", "PUT", null, l);
        } else {
            UtilREST.request(BASE + "alimentos/" + id, "DELETE", null, l);
        }
    }

    // Edita parcialmente un alimento desde el panel de administración
    public static void adminPatchAlimento(int id, JSONObject body, UtilREST.OnResponseListener l) {
        UtilREST.request(BASE + "alimentos/" + id, "PATCH", body.toString(), l);
    }

    // ── UTIL ──────────────────────────────────────────────────────────────────

    // Codifica un valor para uso seguro en la URL (espacios como %20)
    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
