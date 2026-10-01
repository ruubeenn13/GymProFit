package com.gymprofit.api.service.externo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

// ============================================================
// LimiteOpenFoodFacts — cupo de lecturas a Open Food Facts para toda la API (GP-160)
//
// Open Food Facts permite 15 lecturas de producto por minuto y por IP, y todos los
// usuarios salen por la IP de Render. La API se queda en 10 por minuto, entre todos,
// para no rozar el suyo: ventana deslizante de 60 s. Sin cupo, quien llama responde
// 503 con Retry-After (los segundos hasta que se libere la lectura más antigua).
//
// Y recuerda durante un día los códigos que Open Food Facts no tiene: volver a
// escanear uno no gasta cupo. Como mucho 10 000; al llenarse se olvida el más viejo.
//
// En memoria y sin dependencias, como el limitador de AuthRateLimitFilter. Con una
// sola instancia en Render basta; con varias, cada una tendría su cupo.
// ============================================================
@Component
public class LimiteOpenFoodFacts {

    static final long VENTANA_MS = 60_000;
    static final long OLVIDO_MS = 24L * 60 * 60 * 1000;
    static final int MAX_DESCONOCIDOS = 10_000;

    private final int lecturasPorMinuto;
    private final Deque<Long> lecturas = new ArrayDeque<>();
    private final Map<String, Long> desconocidos = new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> mayor) {
            return size() > MAX_DESCONOCIDOS;
        }
    };

    public LimiteOpenFoodFacts(@Value("${app.openfoodfacts.lecturas-por-minuto:10}") int lecturasPorMinuto) {
        this.lecturasPorMinuto = lecturasPorMinuto;
    }

    /**
     * Pide una lectura.
     *
     * @return 0 si hay cupo (y la cuenta como hecha); si no, los segundos que faltan
     * para que lo haya, como mínimo 1.
     */
    public synchronized long pedirLectura() {
        long ahora = ahora();
        while (!lecturas.isEmpty() && ahora - lecturas.peekFirst() >= VENTANA_MS) {
            lecturas.pollFirst();
        }
        if (lecturas.size() < lecturasPorMinuto) {
            lecturas.addLast(ahora);
            return 0;
        }
        long espera = VENTANA_MS - (ahora - lecturas.peekFirst());
        return Math.max(1, (espera + 999) / 1000);
    }

    /** ¿Se preguntó por este código hace menos de un día y Open Food Facts no lo tenía? */
    public synchronized boolean esDesconocido(String codigo) {
        Long desde = desconocidos.get(codigo);
        if (desde == null) return false;
        if (ahora() - desde >= OLVIDO_MS) {
            desconocidos.remove(codigo);
            return false;
        }
        return true;
    }

    /** Apunta que Open Food Facts no tiene este código (o no es aceptable). */
    public synchronized void apuntarDesconocido(String codigo) {
        desconocidos.put(codigo, ahora());
    }

    /** Olvida los códigos desconocidos y las lecturas hechas (para los tests). */
    public synchronized void reiniciar() {
        lecturas.clear();
        desconocidos.clear();
    }

    // Separado para que los tests puedan mover el reloj.
    long ahora() {
        return System.currentTimeMillis();
    }
}
