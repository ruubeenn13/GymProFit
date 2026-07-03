package es.pmdm.gymprofit.network;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ============================================================
// ApiCallback<T> — callback tipado para las interfaces Retrofit de la etapa 2.
// Envuelve el Callback<T> de Retrofit y entrega directamente el POJO ya
// deserializado (onOk), sin parseo manual. Retrofit invoca estos métodos en el
// hilo principal, así que no hace falta runOnUiThread. Centraliza el manejo del
// 401 no recuperable (limpia sesión + avisa para volver a login).
//
// Uso típico en una Activity:
//   api.getOrdenadas(id).enqueue(new ApiCallback<List<MedicionCorporal>>() {
//       public void onOk(List<MedicionCorporal> lista) { ...pintar UI... }
//       public void onFail(int code, String msg) { ...toast de error... }   // opcional
//   });
// ============================================================
public abstract class ApiCallback<T> implements Callback<T> {

    // Éxito (2xx): recibe el cuerpo ya deserializado (puede ser null en 204 No Content).
    public abstract void onOk(T body);

    // Error de red o respuesta no-2xx (401 ya gestionado aparte). Por defecto solo
    // loguea; las Activities lo sobrescriben cuando quieren mostrar error al usuario.
    public void onFail(int code, String message) {
        Log.w("GymProFit", "API fail " + code + ": " + message);
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        int code = response.code();
        if (code == 401) {
            // El TokenAuthenticator ya intentó renovar y no pudo → sesión no recuperable.
            UtilREST.notifyUnauthorized();
            onFail(401, "Sesión expirada");
        } else if (response.isSuccessful()) {
            onOk(response.body());
        } else {
            onFail(code, leerError(response));
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        onFail(-1, t != null ? t.getMessage() : "Error de red");
    }

    // Lee el cuerpo de error como texto (o un mensaje genérico si no se puede).
    private String leerError(Response<T> response) {
        try {
            if (response.errorBody() != null) return response.errorBody().string();
        } catch (Exception ignored) {
        }
        return "Error " + response.code();
    }
}
