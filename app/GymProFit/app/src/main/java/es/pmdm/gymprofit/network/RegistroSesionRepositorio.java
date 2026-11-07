package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// RegistroSesionRepositorio — las llamadas de Apuntar entreno (GP-016; GP-241 añade el
// programa, las sesiones, la última vez y las mejores marcas, las mismas de la sesión en vivo).
//
// Es una interfaz y no la clase de Retrofit directamente por una sola razón: que los
// tests del ViewModel puedan darle un falso que responde cuando el test quiere, o no
// responde nunca, que es como se prueba el doble envío y el fallo. La de verdad es
// RegistroSesionRepositorioApi.
// ============================================================
public interface RegistroSesionRepositorio {

    /** Respuesta de una llamada: el valor, o el código y el mensaje del fallo. */
    interface Respuesta<T> {
        void ok(T valor);

        /**
         * @param codigo  código HTTP, o -1 si no llegó respuesta (red).
         * @param mensaje texto del error tal cual, para UiFeedback.
         */
        void fallo(int codigo, String mensaje);
    }

    /**
     * Rutinas activas del usuario de la sesión, las propias y las de su programa. Las
     * predefinidas ya no se piden: desde la 1.2.0 no hay ninguna (GP-074).
     */
    void rutinasDelUsuario(Respuesta<List<Rutina>> respuesta);

    /** Ejercicios de una rutina, con sus series y repeticiones. */
    void ejerciciosDeRutina(int rutinaId, Respuesta<List<RutinaEjercicio>> respuesta);

    /** El programa que sigue, o null si no sigue ninguno (GET /programas/seguido da 404). */
    void programaSeguido(Respuesta<ProgramaQueSigue> respuesta);

    /** Sus sesiones: deciden la que toca hoy (HoyToca) y la duración de un entreno libre. */
    void sesiones(Respuesta<List<SesionEntrenamiento>> respuesta);

    /** La última vez de cada ejercicio (GET /sesiones/ultima-vez, GP-014). */
    void ultimaVez(List<Integer> ejercicioIds, Respuesta<List<UltimaVez>> respuesta);

    /** La mejor marca de cada ejercicio, como en la sesión en vivo (GP-210). */
    void mejoresMarcas(List<Integer> ejercicioIds, Respuesta<Map<Integer, SesionEnCurso.Marca>> respuesta);

    /** La sesión entera en una llamada idempotente: POST /sesiones/completa (GP-006). */
    void guardarCompleta(Map<String, Object> cuerpo, Respuesta<SesionEntrenamiento> respuesta);
}
