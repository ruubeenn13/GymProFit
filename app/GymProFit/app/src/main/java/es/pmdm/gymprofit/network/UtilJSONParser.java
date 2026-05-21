package es.pmdm.gymprofit.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.objetivo.ObjetivoPersonal;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;

public class UtilJSONParser {

    // AUTH

    public static String parseToken(String json) throws JSONException {
        return new JSONObject(json).optString("token", "");
    }

    public static String parseTokenUsername(String json) throws JSONException {
        return new JSONObject(json).optString("username", "");
    }

    // USUARIO

    public static Usuario parseUsuario(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Usuario u = new Usuario();
        u.setId(obj.optInt("id", -1));
        u.setUsername(obj.optString("username", ""));
        u.setEmail(obj.optString("email", ""));
        u.setPeso(obj.optString("peso", ""));
        u.setAltura(obj.optDouble("altura", 0));
        u.setEdad(obj.optInt("edad", 0));
        u.setNivelExperiencia(obj.optString("nivelExperiencia", ""));
        u.setObjetivo(obj.optString("objetivo", ""));
        u.setFechaRegistro(parseFecha(obj, "fechaRegistro"));
        u.setActivo(obj.optBoolean("activo", true));
        u.setRol(obj.optString("rol", "ROLE_USER"));
        return u;
    }

    public static List<Usuario> parseUsuarioList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Usuario> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseUsuario(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // EJERCICIO

    public static Ejercicio parseEjercicio(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Ejercicio e = new Ejercicio();
        e.setId(obj.optInt("id", -1));
        e.setNombre(obj.optString("nombre", ""));
        e.setDescripcion(obj.optString("descripcion", ""));
        e.setGrupoMuscular(obj.optString("grupoMuscular", ""));
        e.setDificultad(obj.optString("dificultad", ""));
        e.setImagenUrl(obj.optString("imagenUrl", ""));
        e.setInstrucciones(obj.optString("instrucciones", ""));
        e.setCalorias(obj.optInt("caloriasQuemadas", 0));
        e.setEquipoNecesario(obj.optString("equipoNecesario", ""));
        e.setActivo(obj.optBoolean("activo", true));
        return e;
    }

    public static List<Ejercicio> parseEjercicioList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Ejercicio> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseEjercicio(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // RUTINA

    public static Rutina parseRutina(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Rutina r = new Rutina();
        r.setId(obj.optInt("id", -1));
        r.setNombre(obj.optString("nombre", ""));
        r.setDescripcion(obj.optString("descripcion", ""));
        r.setNivel(obj.optString("nivel", ""));
        r.setDuracionMinutos(obj.optInt("duracionMinutos", 0));
        r.setCaloriasAproximadas(obj.optInt("caloriasAproximadas", 0));
        r.setNumEjercicios(obj.optInt("numEjercicios", 0));
        r.setPredefinida(obj.optBoolean("esPredefinida", false));
        r.setUsuarioId(obj.optInt("usuarioId", -1));
        r.setCategoria(obj.optString("categoria", ""));
        r.setDiasSemana(obj.optString("diasSemana", ""));
        r.setFechaCreacion(parseFecha(obj, "fechaCreacion"));
        r.setActiva(obj.optBoolean("activa", true));
        return r;
    }

    public static List<Rutina> parseRutinaList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Rutina> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseRutina(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // SESION

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
        s.setNotas(obj.optString("notas", ""));
        s.setCompletada(obj.optBoolean("completada", false));
        return s;
    }

    public static List<SesionEntrenamiento> parseSesionList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<SesionEntrenamiento> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseSesion(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // LOGRO

    public static Logro parseLogro(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Logro l = new Logro();
        l.setId(obj.optInt("id", -1));
        l.setNombre(obj.optString("nombre", ""));
        l.setDescripcion(obj.optString("descripcion", ""));
        l.setIcono(obj.optString("icono", ""));
        l.setCriterio(obj.optString("criterio", ""));
        l.setValorObjetivo(obj.optInt("valorObjetivo", 0));
        return l;
    }

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
        m.setNotas(obj.optString("notas", ""));
        return m;
    }

    public static List<MedicionCorporal> parseMedicionList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<MedicionCorporal> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseMedicion(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // OBJETIVO PERSONAL

    public static ObjetivoPersonal parseObjetivo(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        ObjetivoPersonal o = new ObjetivoPersonal();
        o.setId(obj.optInt("id", -1));
        o.setUsuarioId(obj.optInt("usuarioId", -1));
        o.setTipo(obj.optString("tipo", ""));
        o.setDescripcion(obj.optString("descripcion", ""));
        o.setValorObjetivo(obj.optDouble("valorObjetivo", 0));
        o.setValorActual(obj.optDouble("valorActual", 0));
        o.setFechaInicio(parseFecha(obj, "fechaInicio"));
        o.setFechaFin(parseFecha(obj, "fechaFin"));
        o.setCompletado(obj.optBoolean("completado", false));
        return o;
    }

    public static List<ObjetivoPersonal> parseObjetivoList(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<ObjetivoPersonal> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseObjetivo(arr.getJSONObject(i).toString()));
        }
        return list;
    }

    // ESTADISTICAS

    public static UsuarioEstadisticas parseEstadisticas(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        UsuarioEstadisticas e = new UsuarioEstadisticas();
        e.setTotalSesiones(obj.optInt("totalSesiones", 0));
        e.setSesionesCompletadas(obj.optInt("sesionesCompletadas", 0));
        e.setTotalMinutosEntrenados(obj.optInt("totalMinutosEntrenados", 0));
        e.setTotalCaloriasQuemadas(obj.optInt("totalCaloriasQuemadas", 0));
        e.setEjercicioMasFrecuente(obj.optString("ejercicioMasFrecuente", ""));
        e.setRachaActualDias(obj.optInt("rachaActualDias", 0));
        e.setMejorRachaDias(obj.optInt("mejorRachaDias", 0));
        return e;
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
