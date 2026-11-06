package es.pmdm.gymprofit.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.LongSupplier;

import es.pmdm.gymprofit.arranque.DecisionArranque;

// ============================================================
// TurnoRenovacion — quién llama a /auth/refresh a mitad de uso, y qué hace el resto
// mientras tanto (GP-237 y GP-235, DEC-059).
//
// Sin dependencias de Android: la llamada, los tokens y el reloj llegan inyectados, para
// probarlo entero con hilos de verdad (TurnoRenovacionTest). El TokenAuthenticator de
// ApiClient lo usa con la llamada real.
//
// Las reglas:
//   - Solo un 401 de /auth/refresh cierra la sesión. Sin respuesta, tiempo agotado, 429,
//     5xx o un cuerpo sin tokens no la cierran: los tokens se quedan y la petición falla.
//     Lo clasifica DecisionArranque, el mismo criterio que al arrancar.
//   - Una llamada por intento, y después una espera con ese refresh: 5 s tras un intento
//     sin respuesta; con 429, lo que diga Retry-After. Lo que llegue en ese rato falla
//     sin llamar (con 429, como el 429 de GP-096 y con lo que falta de espera). Pasada la
//     espera, la siguiente acción vuelve a probar, porque la red puede haber vuelto. Es
//     una sola regla y cubre también a quien esperaba el cerrojo durante el intento.
//     Antes la tanda se decidía por la hora de envío de la petición y no bastaba: el
//     Dispatcher de OkHttp deja 5 peticiones a la vez por host, y las 3 que Inicio deja
//     en cola salían cuando el primer intento ya había acabado (medido en logcat).
//   - Un refresh rechazado se recuerda: quien llegue con él se rinde sin llamar, aunque
//     el 401 todavía no haya llegado a la pantalla que borra los tokens.
// ============================================================
public final class TurnoRenovacion {

    /** Lo que se espera tras un intento sin respuesta antes de volver a llamar con el mismo refresh. */
    public static final long ESPERA_SIN_RESPUESTA_MS = 5_000;

    /** Lo que contesta /auth/refresh. */
    public static final class Respuesta {
        final int codigo;
        @Nullable final String cuerpo;
        @Nullable final String token;
        @Nullable final String refresh;
        @Nullable final Integer retryAfterSegundos;

        /**
         * @param codigo  el código HTTP, o -1 si no hubo respuesta.
         * @param cuerpo  el cuerpo tal cual, o null.
         * @param token   el access nuevo leído del cuerpo, o null si no estaba o no se pudo leer.
         * @param refresh el refresh nuevo leído del cuerpo, o null igual.
         * @param retryAfterSegundos los segundos de la cabecera Retry-After, o null si no
         *                venía o no era un número.
         */
        public Respuesta(int codigo, @Nullable String cuerpo, @Nullable String token, @Nullable String refresh,
                         @Nullable Integer retryAfterSegundos) {
            this.codigo = codigo;
            this.cuerpo = cuerpo;
            this.token = token;
            this.refresh = refresh;
            this.retryAfterSegundos = retryAfterSegundos;
        }

        /** Una respuesta sin Retry-After. */
        public Respuesta(int codigo, @Nullable String cuerpo, @Nullable String token, @Nullable String refresh) {
            this(codigo, cuerpo, token, refresh, null);
        }

        /** Sin respuesta: red que no llega o tiempo agotado. */
        public static Respuesta sinRespuesta() {
            return new Respuesta(-1, null, null, null);
        }
    }

    /** La llamada a /auth/refresh. No lanza: sin respuesta es {@link Respuesta#sinRespuesta()}. */
    public interface Llamada {
        @NonNull Respuesta pedir(@NonNull String refresh);
    }

    /** Dónde viven los tokens de la sesión (UtilREST en la app). */
    public interface Tokens {
        @Nullable String access();
        @Nullable String refresh();
        void guardar(@NonNull String access, @NonNull String refresh);
    }

    /** Cómo acaba un intento. */
    public enum Tipo {
        /** Hay un access nuevo: la petición se repite con él. */
        RENOVADA,
        /** /auth/refresh dijo 401: la sesión se cierra. */
        CERRADA,
        /** No hay refresh guardado: no se llama a nadie y la sesión se cierra, como siempre. */
        SIN_REFRESH,
        /** Sin respuesta útil (sin red, tiempo agotado, 5xx): los tokens se quedan y la petición falla como sin red. */
        SIN_RESPUESTA,
        /** /auth/refresh dijo 429: los tokens se quedan y la petición falla como un 429, con su espera. */
        LIMITADA
    }

    /** El resultado de un intento, el mismo para todos los que lo esperaban. */
    public static final class Resultado {
        @NonNull public final Tipo tipo;
        /** El access con que repetir la petición; solo en {@link Tipo#RENOVADA}. */
        @Nullable public final String token;
        /** La sesión se cerró porque la cuenta está desactivada (GP-083). */
        public final boolean cuentaDesactivada;
        /** Lo que falta de espera, en segundos redondeados hacia arriba; solo en {@link Tipo#LIMITADA}. */
        public final int segundosDeEspera;

        private Resultado(@NonNull Tipo tipo, @Nullable String token, boolean cuentaDesactivada, int segundosDeEspera) {
            this.tipo = tipo;
            this.token = token;
            this.cuentaDesactivada = cuentaDesactivada;
            this.segundosDeEspera = segundosDeEspera;
        }

        static Resultado renovada(String token) { return new Resultado(Tipo.RENOVADA, token, false, 0); }
        static Resultado cerrada(boolean desactivada) { return new Resultado(Tipo.CERRADA, null, desactivada, 0); }
        // Hacia arriba, para no invitar a volver antes de tiempo.
        static Resultado limitada(long faltaMs) {
            return new Resultado(Tipo.LIMITADA, null, false, (int) Math.max(1, (faltaMs + 999) / 1000));
        }
        static final Resultado SIN_REFRESH = new Resultado(Tipo.SIN_REFRESH, null, false, 0);
        static final Resultado SIN_RESPUESTA = new Resultado(Tipo.SIN_RESPUESTA, null, false, 0);
    }

    private final Llamada llamada;
    private final Tokens tokens;
    private final LongSupplier reloj;

    // La espera en curso (solo dentro del cerrojo): con qué refresh, hasta cuándo y si es
    // la de un 429.
    @Nullable private String esperaRefresh;
    private long esperaHastaMs;
    private boolean esperaLimitada;
    // El último refresh que la API rechazó con 401, y si fue por la cuenta desactivada.
    @Nullable private String rechazado;
    private boolean rechazadoDesactivada;

    /**
     * @param reloj milisegundos de un reloj que no retrocede (SystemClock.elapsedRealtime
     *              en la app): solo mide la espera.
     */
    public TurnoRenovacion(@NonNull Llamada llamada, @NonNull Tokens tokens, @NonNull LongSupplier reloj) {
        this.llamada = llamada;
        this.tokens = tokens;
        this.reloj = reloj;
    }

    /**
     * Renueva el access, o devuelve lo que toca sin llamar si otro ya renovó, el refresh
     * fue rechazado o sigue la espera de un intento fallido.
     *
     * <p>Va por el cerrojo de este objeto, el mismo que la renovación del arranque
     * ({@link ApiClient#renovarAlArrancar}): el refresh es de un solo uso, y dos llamadas
     * a la vez con el mismo gastarían una la de la otra.
     *
     * @param accessUsado el access que llevaba la petición rechazada, o null si no llevaba.
     */
    @NonNull
    public synchronized Resultado renovar(@Nullable String accessUsado) {
        // Otro ya trajo un access distinto del que se usó: se repite con ese.
        String actual = tokens.access();
        if (actual != null && !actual.isEmpty() && !actual.equals(accessUsado)) {
            return Resultado.renovada(actual);
        }

        String refresh = tokens.refresh();
        if (refresh == null || refresh.isEmpty()) return Resultado.SIN_REFRESH;
        if (refresh.equals(rechazado)) return Resultado.cerrada(rechazadoDesactivada);
        long ahora = reloj.getAsLong();
        if (refresh.equals(esperaRefresh) && ahora < esperaHastaMs) {
            return esperaLimitada ? Resultado.limitada(esperaHastaMs - ahora) : Resultado.SIN_RESPUESTA;
        }

        Respuesta r = llamada.pedir(refresh);
        DecisionArranque.Renovacion clase = DecisionArranque.clasificar(r.codigo, r.cuerpo);
        switch (clase) {
            case OK:
                if (r.token != null && !r.token.isEmpty() && r.refresh != null && !r.refresh.isEmpty()) {
                    tokens.guardar(r.token, r.refresh);
                    return Resultado.renovada(r.token);
                }
                // 2xx sin los dos tokens: nada que guardar, y el refresh puede seguir valiendo.
                return esperar(refresh, r);
            case NO_AUTORIZADO:
            case CUENTA_DESACTIVADA:
                boolean desactivada = clase == DecisionArranque.Renovacion.CUENTA_DESACTIVADA;
                rechazado = refresh;
                rechazadoDesactivada = desactivada;
                return Resultado.cerrada(desactivada);
            default:
                return esperar(refresh, r);
        }
    }

    // Abre la espera de un intento fallido. Cuenta desde que acaba el intento: un tiempo
    // agotado tarda, y quien esperaba el cerrojo entra justo después.
    private Resultado esperar(String refresh, Respuesta r) {
        boolean limitada = r.codigo == 429;
        long espera = limitada && r.retryAfterSegundos != null && r.retryAfterSegundos > 0
                ? r.retryAfterSegundos * 1000L : ESPERA_SIN_RESPUESTA_MS;
        esperaRefresh = refresh;
        esperaHastaMs = reloj.getAsLong() + espera;
        esperaLimitada = limitada;
        return limitada ? Resultado.limitada(espera) : Resultado.SIN_RESPUESTA;
    }
}
