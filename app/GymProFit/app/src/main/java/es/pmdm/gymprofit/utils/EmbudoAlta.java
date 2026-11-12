package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EmbudoApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ============================================================
// EmbudoAlta — avisa a la API de cada pantalla del alta a la que se llega (GP-019, DEC-064).
//
// Para saber en qué paso se va la gente, sin saber quién. Lo que sale es el nombre del
// paso y nada más: ni token (va por ApiClient.anonimo), ni cuenta, ni móvil, ni hora.
//
// Una vez por intento: el paso que ya se avisó no se vuelve a avisar, aunque se vuelva
// atrás, se gire la pantalla o Android cierre la app y se retome. Lo avisado se guarda en
// el móvil; un intento nuevo empieza en la bienvenida después de un alta terminada.
//
// Sin esperar respuesta ni reintentar: si falla, ese paso no se cuenta y el alta sigue
// igual. Solo el alta de verdad: la cuenta que entró sin onboarding no avisa.
// ============================================================
public final class EmbudoAlta {

    private EmbudoAlta() {}

    /** Las pantallas que se cuentan, con los mismos nombres que PasoAlta de la API. */
    public enum Paso {
        BIENVENIDA, OBJETIVO, NIVEL, SOBRE_TI, DONDE, DIAS, PLAN, GUARDA,
        /** «Todo listo»: la cuenta ya está creada. */
        CUENTA,
        AVISOS;

        /** La pantalla del cuestionario con ese paso; se llaman igual. */
        @NonNull
        public static Paso de(@NonNull AltaPasos.Paso p) {
            return valueOf(p.name());
        }
    }

    /** Dónde se apunta lo ya avisado en este intento. */
    public interface Avisados {
        @NonNull Set<String> leer();
        void guardar(@NonNull Set<String> pasos);
    }

    /** Cómo sale el aviso. */
    public interface Envio {
        void enviar(@NonNull Map<String, String> cuerpo);
    }

    /** Lo único que se manda: {"paso": "<NOMBRE>"}. */
    @NonNull
    public static Map<String, String> cuerpo(@NonNull Paso paso) {
        return Collections.singletonMap("paso", paso.name());
    }

    /**
     * Avisa de {@code paso} si en este intento aún no se ha avisado.
     *
     * @return si ha salido el aviso
     */
    public static boolean avisar(@NonNull Paso paso, @NonNull Avisados avisados, @NonNull Envio envio) {
        Set<String> ya = new HashSet<>(avisados.leer());
        // Tras un alta terminada, volver a la bienvenida es empezar otra.
        if (paso == Paso.BIENVENIDA && ya.contains(Paso.CUENTA.name())) ya.clear();
        if (!ya.add(paso.name())) return false;
        avisados.guardar(ya);
        envio.enviar(cuerpo(paso));
        return true;
    }

    /** Avisa de {@code paso} desde una pantalla, sin esperar ni reintentar. */
    public static void avisar(@NonNull Context contexto, @NonNull Paso paso) {
        PreferencesManager prefs = new PreferencesManager(contexto.getApplicationContext());
        avisar(paso, new Avisados() {
            @NonNull @Override public Set<String> leer() { return prefs.getEmbudoAvisados(); }
            @Override public void guardar(@NonNull Set<String> pasos) { prefs.guardarEmbudoAvisados(pasos); }
        }, cuerpo -> ApiClient.anonimo(EmbudoApi.class).avisar(cuerpo).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Contado, o rechazado (un 429 por el cupo de la IP): no hay nada que enseñar.
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Se ignora a propósito: es un recuento, y perder un paso no cambia el alta.
            }
        }));
    }
}
