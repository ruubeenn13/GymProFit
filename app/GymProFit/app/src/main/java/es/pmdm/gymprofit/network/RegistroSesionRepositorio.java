package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// RegistroSesionRepositorio — las cuatro llamadas de Registrar sesión (GP-016).
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

    /** La sesión entera en una llamada idempotente: POST /sesiones/completa (GP-006). */
    void guardarCompleta(Map<String, Object> cuerpo, Respuesta<SesionEntrenamiento> respuesta);
}
