package es.pmdm.gymprofit.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.objetivo.ObjetivoPersonal;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.Usuario;

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
        u.setFechaRegistro(obj.optString("fechaRegistro", ""));
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
        r.setFechaCreacion(obj.optString("fechaCreacion", ""));
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
        s.setFecha(obj.optString("fecha", ""));
        s.setDuracionMinutos(obj.optInt("duracionMinutos", 0));
        s.setCaloriasQuemadas(obj.optInt("caloriasQuemadas", 0));
        s.setNotas(obj.optString("notas", ""));
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
        o.setFechaInicio(obj.optString("fechaInicio", ""));
        o.setFechaFin(obj.optString("fechaFin", ""));
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
}
