package com.gymprofit.api.service.externo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
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
// Además, un cupo por cuenta (GP-167): 3 lecturas por minuto y 30 al día. Si no, una
// sola cuenta (o un script con su token) se come las 10 de todos. La lectura que niega
// la cuenta no gasta cupo de la API. El Retry-After es lo que falte del más estricto.
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
    static final long DIA_MS = 24L * 60 * 60 * 1000;
    static final long OLVIDO_MS = 24L * 60 * 60 * 1000;
    static final int MAX_DESCONOCIDOS = 10_000;

    private final int lecturasPorMinuto;
    private final int lecturasPorMinutoCuenta;
    private final int lecturasPorDiaCuenta;
    private final Deque<Long> lecturas = new ArrayDeque<>();
    // Las lecturas de cada cuenta en las últimas 24 h, como mucho lecturasPorDiaCuenta.
    private final Map<Integer, Deque<Long>> porCuenta = new HashMap<>();
    private final Map<String, Long> desconocidos = new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> mayor) {
            return size() > MAX_DESCONOCIDOS;
        }
    };

    public LimiteOpenFoodFacts(@Value("${app.openfoodfacts.lecturas-por-minuto:10}") int lecturasPorMinuto,
                               @Value("${app.openfoodfacts.lecturas-por-minuto-cuenta:3}") int lecturasPorMinutoCuenta,
                               @Value("${app.openfoodfacts.lecturas-por-dia-cuenta:30}") int lecturasPorDiaCuenta) {
        this.lecturasPorMinuto = lecturasPorMinuto;
        this.lecturasPorMinutoCuenta = lecturasPorMinutoCuenta;
        this.lecturasPorDiaCuenta = lecturasPorDiaCuenta;
    }

    /**
     * Pide una lectura para una cuenta: la cuenta como hecha solo si hay cupo en la API
     * y en la cuenta.
     *
     * @param usuarioId la cuenta que la pide (la del token).
     * @return 0 si hay cupo; si no, los segundos que faltan para que lo haya, como mínimo 1.
     */
    public synchronized long pedirLectura(Integer usuarioId) {
        long ahora = ahora();
        while (!lecturas.isEmpty() && ahora - lecturas.peekFirst() >= VENTANA_MS) {
            lecturas.pollFirst();
        }
        Deque<Long> deLaCuenta = porCuenta.computeIfAbsent(usuarioId, k -> new ArrayDeque<>());
        while (!deLaCuenta.isEmpty() && ahora - deLaCuenta.peekFirst() >= DIA_MS) {
            deLaCuenta.pollFirst();
        }

        long espera = 0;
        if (lecturas.size() >= lecturasPorMinuto) {
            espera = VENTANA_MS - (ahora - lecturas.peekFirst());
        }
        if (deLaCuenta.size() >= lecturasPorDiaCuenta) {
            espera = Math.max(espera, DIA_MS - (ahora - deLaCuenta.peekFirst()));
        }
        long ultimoMinuto = deLaCuenta.stream().filter(t -> ahora - t < VENTANA_MS).count();
        if (ultimoMinuto >= lecturasPorMinutoCuenta) {
            // La que hace salir de la ventana a una de las del último minuto.
            long masVieja = deLaCuenta.stream().filter(t -> ahora - t < VENTANA_MS)
                    .skip(ultimoMinuto - lecturasPorMinutoCuenta).findFirst().orElse(ahora);
            espera = Math.max(espera, VENTANA_MS - (ahora - masVieja));
        }
        if (espera > 0) {
            if (deLaCuenta.isEmpty()) porCuenta.remove(usuarioId);
            return Math.max(1, (espera + 999) / 1000);
        }
        lecturas.addLast(ahora);
        deLaCuenta.addLast(ahora);
        olvidarCuentasQuietas(ahora);
        return 0;
    }

    // Las cuentas sin lecturas en 24 h no ocupan memoria. Se mira de vez en cuando, no a
    // cada lectura: con 30 al día por cuenta, el mapa crece despacio.
    private void olvidarCuentasQuietas(long ahora) {
        if (porCuenta.size() < 1000) return;
        porCuenta.values().removeIf(d -> d.isEmpty() || ahora - d.peekLast() >= DIA_MS);
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
        porCuenta.clear();
        desconocidos.clear();
    }

    // Separado para que los tests puedan mover el reloj.
    long ahora() {
        return System.currentTimeMillis();
    }
}
