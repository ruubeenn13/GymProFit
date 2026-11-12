package es.pmdm.gymprofit.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// ReintentoEmbudo — un aviso del embudo del alta se reintenta como mucho una vez (GP-264).
//
// Por qué: en producción, los cuatro primeros pasos de un alta recibieron 429 (el cupo por
// IP del embudo estaba gastado desde esa IP) y se perdieron sin más. Un 429 no cuenta, así
// que reintentarlo pasado el Retry-After no puede contar dos veces. Un fallo de red se
// reintenta también una vez: ahí sí cabe contar doble si la petición llegó y se perdió la
// respuesta, y se acepta a cambio de no perder el paso.
//
// Reglas: un solo reintento por aviso, sea cual sea el motivo del primer fallo. Tras un 429,
// el Retry-After en segundos con un tope de 60 s; sin él, o si no es un número, 60 s. Otros
// rechazos (400, 5xx) no se reintentan. Nada persistente: el reintento vive en memoria y, si
// la app se cierra antes, el aviso se pierde (aceptado). Nunca bloquea el alta: todo va por
// callbacks y por el programador diferido.
// ============================================================
public final class ReintentoEmbudo {

    /** Espera máxima antes del reintento tras un 429, y la que se usa sin Retry-After. */
    static final long ESPERA_MAX_MS = 60_000L;

    private ReintentoEmbudo() {}

    /** Cómo acabó un intento. */
    public interface Resultado {
        /** La API contestó 2xx: el paso está contado. */
        void contado();

        /** La API contestó con error; {@code retryAfter} es la cabecera tal cual, si vino. */
        void rechazado(int codigo, @Nullable String retryAfter);

        /** No hubo respuesta (sin conexión, tiempo agotado...). */
        void falloDeRed();
    }

    /** Lanza una petición y avisa del resultado, sin bloquear. */
    public interface Intento {
        void lanzar(@NonNull Resultado resultado);
    }

    /** Ejecuta una tarea pasados unos milisegundos, sin bloquear al que llama. */
    public interface Programador {
        void enDiferido(long ms, @NonNull Runnable tarea);
    }

    /**
     * Lanza el aviso y, si falla por un 429 o por la red, lo reintenta una sola vez.
     *
     * @param intento     la petición (cada llamada a {@code lanzar} es un intento nuevo)
     * @param programador dónde se espera hasta el reintento
     */
    public static void enviar(@NonNull Intento intento, @NonNull Programador programador) {
        intento.lanzar(new Resultado() {
            @Override
            public void contado() {
                // Contado a la primera: nada más que hacer.
            }

            @Override
            public void rechazado(int codigo, @Nullable String retryAfter) {
                if (codigo != 429) return; // un 400 o un 500 no cambian por insistir
                programador.enDiferido(esperaTras429(retryAfter), () -> intento.lanzar(SIN_MAS));
            }

            @Override
            public void falloDeRed() {
                programador.enDiferido(0L, () -> intento.lanzar(SIN_MAS));
            }
        });
    }

    /** Lo que se espera tras un 429: Retry-After en segundos, con tope; 60 s si no vale. */
    static long esperaTras429(@Nullable String retryAfter) {
        if (retryAfter == null) return ESPERA_MAX_MS;
        try {
            long segundos = Long.parseLong(retryAfter.trim());
            if (segundos < 0) return ESPERA_MAX_MS;
            return Math.min(segundos * 1000L, ESPERA_MAX_MS);
        } catch (NumberFormatException e) {
            // Una fecha HTTP u otra cosa: la API manda segundos, así que se usa el tope.
            return ESPERA_MAX_MS;
        }
    }

    // El segundo intento: pase lo que pase, no hay tercero. Perder el paso no cambia el alta.
    private static final Resultado SIN_MAS = new Resultado() {
        @Override public void contado() { /* contado al segundo intento */ }
        @Override public void rechazado(int codigo, @Nullable String retryAfter) { /* ya se reintentó: se pierde a propósito */ }
        @Override public void falloDeRed() { /* ya se reintentó: se pierde a propósito */ }
    };
}
