package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.dto.EjercicioDTO;
import es.pmdm.gymprofit.network.dto.LoginDTO;
import es.pmdm.gymprofit.network.dto.RegisterDTO;
import es.pmdm.gymprofit.network.dto.RutinaCreateDTO;
import es.pmdm.gymprofit.network.dto.RutinaDTO;
import es.pmdm.gymprofit.network.dto.TokenDTO;
import es.pmdm.gymprofit.network.dto.UsuarioDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // AUTH
    @POST("auth/login")
    Call<TokenDTO> login(@Body LoginDTO loginDTO);

    @POST("auth/register")
    Call<Map<String, Object>> register(@Body RegisterDTO registerDTO);

    // USUARIOS
    @GET("usuarios/username/{username}")
    Call<UsuarioDTO> getUsuarioPorUsername(@Path("username") String username);

    @GET("usuarios/{id}")
    Call<UsuarioDTO> getUsuarioporId(@Path("id") int id);

    @PUT("usuarios/{id}")
    Call<UsuarioDTO> actualizarUsuario(@Path("id") int id, @Body UsuarioDTO usuarioDTO);

    // EJERCICIOS
    @GET("ejercicios")
    Call<List<EjercicioDTO>> getEjercicios();

    @GET("ejercicios/activos")
    Call<List<EjercicioDTO>> getEjerciciosActivos();

    @GET("ejercicios/grupo/{grupoMuscular}")
    Call<List<EjercicioDTO>> getEjerciciosPorGrupo(@Path("grupoMuscular") String grupoMuscular);

    @GET("ejercicios/nombre/{nombre}")
    Call<List<EjercicioDTO>> buscarEjercicioPorNombre(@Path("nombre") String nombre);

    @GET("ejercicios/{id}")
    Call<EjercicioDTO> getEjercicioPorId(@Path("id") int id);

    // RUTINAS
    @GET("rutinas/predefinidas")
    Call<List<RutinaDTO>> getRutinasPredefinidas();

    @GET("rutinas/predefinidas/nivel/{nivel}")
    Call<List<RutinaDTO>> getRutinasPredefinidasPorNivel(@Path("nivel") String nivel);

    @GET("rutinas/usuario/{usuarioId}")
    Call<List<RutinaDTO>> getRutinasDeUsuario(@Path("usuarioId") int usuarioId);

    @GET("rutinas/nivel/{nivel}")
    Call<List<RutinaDTO>> getRutinasPorNivel(@Path("nivel") String nivel);

    @POST("rutinas")
    Call<RutinaDTO> crearRutina(@Body RutinaCreateDTO rutinaCreateDTO);

    @PUT("rutinas/{id}")
    Call<RutinaDTO> actualizarRutina(@Path("id") int id, @Body RutinaCreateDTO rutinaCreateDTO);

    @DELETE("rutinas/{id}")
    Call<Void> eliminarRutina(@Path("id") int id);
}
