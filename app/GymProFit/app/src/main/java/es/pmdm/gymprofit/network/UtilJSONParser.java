package es.pmdm.gymprofit.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.objetivo.ObjetivoPersonal;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;

// ============================================================
// UtilJSONParser — utilidades estáticas para parsear las respuestas JSON de la API
// Convierte los JSONObject/JSONArray devueltos por UtilREST en los modelos
// de dominio de la app (Usuario, Ejercicio, Rutina, Sesion, Logro,
// Medicion, Objetivo, Alimento, Comida, etc.), aplicando valores por
// defecto seguros y normalizando fechas.
// ============================================================
public class UtilJSONParser {

    /** optString seguro: devuelve "" cuando el valor JSON es null (evita la cadena "null"). */
    private static String safeStr(JSONObject obj, String key) {
        if (obj.isNull(key)) return "";
        return obj.optString(key, "");
    }

    // AUTH

    // Extrae el token JWT de la respuesta de login
    public static String parseToken(String json) throws JSONException {
        return new JSONObject(json).optString("token", "");
    }

    // Extrae el refresh token opaco de la respuesta de login/refresh
    public static String parseRefreshToken(String json) throws JSONException {
        return new JSONObject(json).optString("refreshToken", "");
    }

    // Extrae el username incluido en la respuesta de login
    public static String parseTokenUsername(String json) throws JSONException {
        return new JSONObject(json).optString("username", "");
    }

    // Extrae el primer rol de la respuesta de login, normalizado con prefijo "ROLE_"
    public static String parseTokenRol(String json) throws JSONException {
        JSONArray roles = new JSONObject(json).optJSONArray("roles");
        if (roles != null && roles.length() > 0) {
            String r = roles.optString(0, "USER");
            return r.startsWith("ROLE_") ? r : "ROLE_" + r;
        }
        return "ROLE_USER";
    }

    // USUARIO

    // Parsea un JSON de usuario a objeto Usuario
    public static Usuario parseUsuario(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Usuario u = new Usuario();
        u.setId(obj.optInt("id", -1));
        u.setUsername(safeStr(obj, "username"));
        u.setEmail(safeStr(obj, "email"));
        u.setPeso(safeStr(obj, "peso"));
        u.setAltura(obj.optDouble("altura", 0));
        u.setEdad(obj.optInt("edad", 0));
        u.setNivelExperiencia(safeStr(obj, "nivelExperiencia"));
        u.setObjetivo(safeStr(obj, "objetivo"));
        u.setFechaRegistro(parseFecha(obj, "fechaRegistro"));
        u.setActivo(obj.optBoolean("activo", true));
        u.setRol(safeStr(obj, "rol"));
        u.setFotoPerfil(safeStr(obj, "fotoPerfil"));
        return u;
    }

    // Parsea un JSON array de usuarios a lista de Usuario
    public static List<Usuario> parseUsuarioList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Usuario> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseUsuario(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // EJERCICIO

    // Parsea un JSON de ejercicio a objeto Ejercicio
    public static Ejercicio parseEjercicio(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Ejercicio e = new Ejercicio();
        e.setId(obj.optInt("id", -1));
        e.setNombre(safeStr(obj, "nombre"));
        e.setDescripcion(safeStr(obj, "descripcion"));
        e.setGrupoMuscular(safeStr(obj, "grupoMuscular"));
        e.setDificultad(safeStr(obj, "dificultad"));
        e.setImagenUrl(safeStr(obj, "imagenUrl"));
        e.setInstrucciones(safeStr(obj, "instrucciones"));
        e.setCalorias(obj.optInt("caloriasQuemadas", 0));
        e.setEquipoNecesario(safeStr(obj, "equipoNecesario"));
        e.setActivo(obj.optBoolean("activo", true));
        return e;
    }

    // Parsea un JSON array de ejercicios a lista de Ejercicio
    public static List<Ejercicio> parseEjercicioList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Ejercicio> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseEjercicio(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // RUTINA

    // Parsea un JSON de rutina a objeto Rutina
    public static Rutina parseRutina(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Rutina r = new Rutina();
        r.setId(obj.optInt("id", -1));
        r.setNombre(safeStr(obj, "nombre"));
        r.setDescripcion(safeStr(obj, "descripcion"));
        r.setNivel(safeStr(obj, "nivel"));
        r.setDuracionMinutos(obj.optInt("duracionMinutos", 0));
        r.setCaloriasAproximadas(obj.optInt("caloriasAproximadas", 0));
        r.setNumEjercicios(obj.optInt("numEjercicios", 0));
        r.setPredefinida(obj.optBoolean("esPredefinida", false));
        r.setUsuarioId(obj.optInt("usuarioId", -1));
        r.setCategoria(safeStr(obj, "categoria"));
        r.setDiasSemana(safeStr(obj, "diasSemana"));
        r.setFechaCreacion(parseFecha(obj, "fechaCreacion"));
        r.setActiva(obj.optBoolean("activa", true));
        return r;
    }

    // Parsea un JSON array de rutinas a lista de Rutina
    public static List<Rutina> parseRutinaList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Rutina> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseRutina(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // SESION

    // Parsea un JSON de sesión de entrenamiento a objeto SesionEntrenamiento
    public static SesionEntrenamiento parseSesion(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setId(obj.optInt("id", -1));
        s.setUsuarioId(obj.optInt("usuarioId", -1));
        s.setRutinaId(obj.optInt("rutinaId", -1));
        s.setFechaInicio(parseFecha(obj, "fechaInicio"));
        s.setFechaFin(parseFecha(obj, "fechaFin"));
        s.setDuracionMinutos(obj.optInt("duracionMinutos", 0));
        s.setCaloriasQuemadas(obj.optInt("caloriasQuemadas", 0));
        s.setNotas(safeStr(obj, "notas"));
        s.setCompletada(obj.optBoolean("completada", false));
        return s;
    }

    // Parsea un JSON array de sesiones a lista de SesionEntrenamiento
    public static List<SesionEntrenamiento> parseSesionList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<SesionEntrenamiento> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseSesion(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // LOGRO

    // Parsea un JSON de logro a objeto Logro
    public static Logro parseLogro(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Logro l = new Logro();
        l.setId(obj.optInt("id", -1));
        l.setNombre(safeStr(obj, "nombre"));
        l.setDescripcion(safeStr(obj, "descripcion"));
        l.setIcono(safeStr(obj, "icono"));
        l.setCriterio(safeStr(obj, "criterio"));
        l.setValorObjetivo(obj.optInt("valorObjetivo", 0));
        return l;
    }

    // Parsea un JSON array de logros a lista de Logro
    public static List<Logro> parseLogroList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Logro> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseLogro(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    /** Devuelve los logroIds desbloqueados por el usuario. */
    public static Set<Integer> parseLogrosDesbloqueados(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            int logroId = arr.getJSONObject(i).optInt("logroId", -1);
            if (logroId != -1) ids.add(logroId);
        }
        return ids;
    }

    // MEDICION CORPORAL

    // Parsea un JSON de medición corporal a objeto MedicionCorporal
    public static MedicionCorporal parseMedicion(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        MedicionCorporal m = new MedicionCorporal();
        m.setId(obj.optInt("id", -1));
        m.setUsuarioId(obj.optInt("usuarioId", -1));
        m.setFecha(parseFecha(obj, "fecha"));
        m.setPeso(obj.optDouble("peso", 0));
        m.setAltura(obj.optDouble("altura", 0));
        m.setImc(obj.optDouble("imc", 0));
        m.setGrasaCorporal(obj.optDouble("grasaCorporal", 0));
        m.setMasaMuscular(obj.optDouble("masaMuscular", 0));
        m.setCintura(obj.optDouble("cintura", 0));
        m.setPecho(obj.optDouble("pecho", 0));
        m.setBrazos(obj.optDouble("brazos", 0));
        m.setPiernas(obj.optDouble("piernas", 0));
        m.setNotas(safeStr(obj, "notas"));
        return m;
    }

    // Parsea un JSON array de mediciones a lista de MedicionCorporal
    public static List<MedicionCorporal> parseMedicionList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<MedicionCorporal> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseMedicion(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // OBJETIVO PERSONAL

    // Parsea un JSON de objetivo personal a objeto ObjetivoPersonal
    public static ObjetivoPersonal parseObjetivo(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        ObjetivoPersonal o = new ObjetivoPersonal();
        o.setId(obj.optInt("id", -1));
        o.setUsuarioId(obj.optInt("usuarioId", -1));
        o.setTipo(safeStr(obj, "tipo"));
        o.setDescripcion(safeStr(obj, "descripcion"));
        o.setValorObjetivo(obj.optDouble("valorObjetivo", 0));
        o.setValorActual(obj.optDouble("valorActual", 0));
        o.setFechaInicio(parseFecha(obj, "fechaInicio"));
        o.setFechaFin(parseFecha(obj, "fechaFin"));
        o.setCompletado(obj.optBoolean("completado", false));
        return o;
    }

    // Parsea un JSON array de objetivos a lista de ObjetivoPersonal
    public static List<ObjetivoPersonal> parseObjetivoList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<ObjetivoPersonal> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseObjetivo(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // ESTADISTICAS

    // Parsea un JSON de estadísticas agregadas a objeto UsuarioEstadisticas
    public static UsuarioEstadisticas parseEstadisticas(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        UsuarioEstadisticas e = new UsuarioEstadisticas();
        e.setTotalSesiones(obj.optInt("totalSesiones", 0));
        e.setSesionesCompletadas(obj.optInt("sesionesCompletadas", 0));
        e.setTotalMinutosEntrenados(obj.optInt("totalMinutosEntrenados", 0));
        e.setTotalCaloriasQuemadas(obj.optInt("totalCaloriasQuemadas", 0));
        e.setEjercicioMasFrecuente(safeStr(obj, "ejercicioMasFrecuente"));
        e.setRachaActualDias(obj.optInt("rachaActualDias", 0));
        e.setMejorRachaDias(obj.optInt("mejorRachaDias", 0));
        return e;
    }

    // ALIMENTO

    // Parsea un JSONObject de alimento a objeto Alimento (usuarioId null si es del catálogo global)
    public static Alimento parseAlimento(JSONObject obj) throws JSONException {
        Alimento a = new Alimento();
        a.setId(obj.optInt("id", -1));
        a.setNombre(safeStr(obj, "nombre"));
        a.setCategoria(safeStr(obj, "categoria"));
        a.setCalorias(obj.optInt("calorias", 0));
        a.setProteinas(obj.optDouble("proteinas", 0));
        a.setCarbohidratos(obj.optDouble("carbohidratos", 0));
        a.setGrasas(obj.optDouble("grasas", 0));
        a.setUsuarioId(obj.isNull("usuarioId") ? null : obj.getInt("usuarioId"));
        a.setActivo(obj.optBoolean("activo", true));
        return a;
    }

    // Parsea un JSON array de alimentos a lista de Alimento
    public static List<Alimento> parseListaAlimentos(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Alimento> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseAlimento(arr.getJSONObject(i)));
        }
        return list;
    }

    // COMIDA

    // Parsea un JSONObject de comida a objeto Comida
    public static Comida parseComida(JSONObject obj) throws JSONException {
        Comida c = new Comida();
        c.setId(obj.optInt("id", -1));
        c.setTipoComida(safeStr(obj, "tipoComida"));
        c.setFecha(parseFecha(obj, "fecha"));
        c.setTotalCalorias(obj.optInt("totalCalorias", 0));
        c.setTotalProteinas(obj.optDouble("totalProteinas", 0));
        c.setTotalCarbohidratos(obj.optDouble("totalCarbohidratos", 0));
        c.setTotalGrasas(obj.optDouble("totalGrasas", 0));
        c.setUsuarioId(obj.optInt("usuarioId", -1));
        return c;
    }

    // Parsea un JSON array de comidas a lista de Comida
    public static List<Comida> parseListaComidas(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Comida> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseComida(arr.getJSONObject(i)));
        }
        return list;
    }

    // ALIMENTO COMIDA

    // Parsea un JSONObject de la relación alimento-comida (con totales calculados) a objeto AlimentoComida
    public static AlimentoComida parseAlimentoComida(JSONObject obj) throws JSONException {
        AlimentoComida ac = new AlimentoComida();
        ac.setId(obj.optInt("id", -1));
        ac.setComidaId(obj.optInt("comidaId", -1));
        ac.setAlimentoId(obj.optInt("alimentoId", -1));
        ac.setNombreAlimento(safeStr(obj, "nombreAlimento"));
        ac.setCategoriaAlimento(safeStr(obj, "categoriaAlimento"));
        ac.setCantidadGramos(obj.optDouble("cantidadGramos", 0));
        ac.setCaloriasTotales(obj.optInt("caloriasTotales", 0));
        ac.setProteinasTotales(obj.optDouble("proteinasTotales", 0));
        ac.setCarbohidratosTotales(obj.optDouble("carbohidratosTotales", 0));
        ac.setGrasasTotales(obj.optDouble("grasasTotales", 0));
        ac.setUsuarioIdAlimento(obj.isNull("usuarioIdAlimento") ? null : obj.getInt("usuarioIdAlimento"));
        return ac;
    }

    // Parsea un JSON array de relaciones alimento-comida a lista de AlimentoComida
    public static List<AlimentoComida> parseListaAlimentosComida(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<AlimentoComida> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseAlimentoComida(arr.getJSONObject(i)));
        }
        return list;
    }

    /**
     * Parsea un campo de fecha que puede llegar como String ISO ("2024-05-17T10:30:00")
     * o como array Jackson ([2024, 5, 17, 10, 30, 0]).
     * Devuelve formato "dd/MM/yyyy HH:mm".
     */
    static String parseFecha(JSONObject obj, String key) {
        try {
            Object val = obj.get(key);
            if (val instanceof String) {
                String s = (String) val;
                if (s.length() >= 10) {
                    String[] parts = s.substring(0, 10).split("-");
                    if (parts.length == 3) {
                        String result = parts[2] + "/" + parts[1] + "/" + parts[0];
                        if (s.length() >= 16) result += " " + s.substring(11, 16);
                        return result;
                    }
                }
                return s;
            }
            if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                return String.format(Locale.US, "%02d/%02d/%04d %02d:%02d",
                        arr.getInt(2), arr.getInt(1), arr.getInt(0),
                        arr.length() > 3 ? arr.getInt(3) : 0,
                        arr.length() > 4 ? arr.getInt(4) : 0);
            }
        } catch (Exception ignored) {}
        return "";
    }
}
