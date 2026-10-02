package com.gymprofit.api.service.alimento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// ============================================================
// FrenoAvisos — que una cuenta no infle los avisos de alimentos (lote 1.6.1)
//
// En memoria, como LimiteOpenFoodFacts: la tabla de avisos no guarda quién, así que el
// freno no puede vivir allí. Dos reglas por cuenta:
//   · el mismo aviso (alimento y motivo) cuenta una vez al día;
//   · como mucho 10 avisos por hora.
// Lo que frena no da error: quien reporta ve lo mismo, y el aviso no suma. Así no hay
// nada que tantear desde fuera. Con una sola instancia en Render basta.
// ============================================================
@Component
public class FrenoAvisos {

    static final long HORA_MS = 60L * 60 * 1000;
    static final long DIA_MS = 24 * HORA_MS;
    // Como mucho tantos «ya enviado» recordados; al llenarse se olvida el más viejo.
    static final int MAX_RECORDADOS = 50_000;

    private final int porHora;
    private final Map<Integer, Deque<Long>> recientes = new HashMap<>();
    private final Map<String, Long> enviados = new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> mayor) {
            return size() > MAX_RECORDADOS;
        }
    };

    public FrenoAvisos(@Value("${app.avisos-alimento.por-hora:10}") int porHora) {
        this.porHora = porHora;
    }

    /**
     * ¿Cuenta este aviso? Si sí, lo apunta.
     *
     * @param usuarioId la cuenta que lo envía.
     * @param clave     el alimento o el código, como en la tabla.
     * @param motivo    el motivo.
     * @return true si suma; false si la cuenta ya lo envió hoy o ha llegado a su tope.
     */
    public synchronized boolean admitir(Integer usuarioId, String clave, String motivo) {
        long ahora = ahora();
        String clavePorCuenta = usuarioId + "|" + clave + "|" + motivo;
        Long antes = enviados.get(clavePorCuenta);
        if (antes != null && ahora - antes < DIA_MS) return false;

        Deque<Long> deLaCuenta = recientes.computeIfAbsent(usuarioId, k -> new ArrayDeque<>());
        while (!deLaCuenta.isEmpty() && ahora - deLaCuenta.peekFirst() >= HORA_MS) deLaCuenta.pollFirst();
        if (deLaCuenta.size() >= porHora) return false;

        deLaCuenta.addLast(ahora);
        enviados.remove(clavePorCuenta);
        enviados.put(clavePorCuenta, ahora);
        if (recientes.size() > 1000) {
            recientes.values().removeIf(d -> d.isEmpty() || ahora - d.peekLast() >= HORA_MS);
        }
        return true;
    }

    /** Olvida todo (para los tests). */
    public synchronized void reiniciar() {
        recientes.clear();
        enviados.clear();
    }

    // Separado para que los tests puedan mover el reloj.
    long ahora() {
        return System.currentTimeMillis();
    }
}
