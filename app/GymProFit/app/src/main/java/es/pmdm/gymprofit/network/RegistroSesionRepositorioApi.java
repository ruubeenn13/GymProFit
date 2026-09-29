package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// RegistroSesionRepositorioApi — RegistroSesionRepositorio sobre Retrofit.
//
// Solo traduce: cada llamada va a su interfaz tipada y la respuesta pasa tal cual. El
// 401 lo sigue gestionando ApiCallback, igual que en el resto de la app.
// ============================================================
public class RegistroSesionRepositorioApi implements RegistroSesionRepositorio {

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final int usuarioId;

    /** @param usuarioId el usuario de la sesión, para pedir sus rutinas. */
    public RegistroSesionRepositorioApi(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public void rutinasDelUsuario(Respuesta<List<Rutina>> respuesta) {
        rutinaApi.getDeUsuarioActivas(usuarioId).enqueue(reenviar(respuesta));
    }

    @Override
    public void ejerciciosDeRutina(int rutinaId, Respuesta<List<RutinaEjercicio>> respuesta) {
        rutinaApi.getEjerciciosDeRutina(rutinaId).enqueue(reenviar(respuesta));
    }

    @Override
    public void guardarCompleta(Map<String, Object> cuerpo, Respuesta<SesionEntrenamiento> respuesta) {
        sesionApi.guardarCompleta(cuerpo).enqueue(reenviar(respuesta));
    }

    // Pasa el resultado de Retrofit a quien pidió la llamada, sin decidir nada aquí.
    private static <T> ApiCallback<T> reenviar(Respuesta<T> respuesta) {
        return new ApiCallback<T>() {
            @Override public void onOk(T body) { respuesta.ok(body); }
            @Override public void onFail(int code, String message) { respuesta.fallo(code, message); }
        };
    }
}
