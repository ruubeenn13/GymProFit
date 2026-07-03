package es.pmdm.gymprofit.network;

import java.util.List;

import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.logro.UsuarioLogro;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

// ============================================================
// LogroApi — interfaz Retrofit tipada del dominio "logros" (etapa 2).
// Las respuestas se deserializan a POJOs (Logro / UsuarioLogro) vía Gson,
// sin UtilJSONParser. Solo lecturas: catálogo de logros y logros
// desbloqueados por un usuario. Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface LogroApi {

    // Catálogo completo de logros disponibles en la app.
    @GET("logros")
    Call<List<Logro>> getLogros();

    // Logros desbloqueados por un usuario: cada elemento aporta el logroId conseguido.
    @GET("logros/usuario/{usuarioId}")
    Call<List<UsuarioLogro>> getLogrosDeUsuario(@Path("usuarioId") int usuarioId);
}
