package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.model.medicion.MedicionCorporal;

// ============================================================
// MedicionHoy — una medición por día (upsert): si la última es de hoy se corrige, y si es de
// otro día se crea la de hoy copiando la última y cambiando lo que se apunta. Así cada día es un
// punto de la gráfica y apuntar el peso no borra la cintura de ayer.
//
// La usan Medidas (cada fila) y «Apuntar peso» de Progreso (GP-216, lote 4), que apunta sin
// salir de la pantalla. Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class MedicionHoy {

    private MedicionHoy() { }

    /** Si la medición es del día {@code hoy} («yyyy-MM-dd»). */
    public static boolean esDeHoy(@Nullable MedicionCorporal m, @NonNull String hoy) {
        return m != null && m.getFecha() != null && m.getFecha().length() >= 10
                && m.getFecha().substring(0, 10).equals(hoy);
    }

    /** El cuerpo para crear la medición de hoy con los valores de la última (o vacía, sin última). */
    @NonNull
    public static Map<String, Object> copiaDe(@Nullable MedicionCorporal ultima, int usuarioId) {
        Map<String, Object> body = new HashMap<>();
        body.put("usuarioId", usuarioId);
        if (ultima == null) return body;
        if (ultima.getPeso() > 0)          body.put("peso", BigDecimal.valueOf(ultima.getPeso()));
        if (ultima.getAltura() > 0)        body.put("altura", BigDecimal.valueOf(ultima.getAltura()));
        if (ultima.getGrasaCorporal() != null && ultima.getGrasaCorporal() > 0)
            body.put("grasaCorporal", BigDecimal.valueOf(ultima.getGrasaCorporal()));
        if (ultima.getMasaMuscular() > 0)  body.put("masaMuscular", BigDecimal.valueOf(ultima.getMasaMuscular()));
        if (ultima.getCintura() > 0)       body.put("cintura", BigDecimal.valueOf(ultima.getCintura()));
        if (ultima.getPecho() > 0)         body.put("pecho", BigDecimal.valueOf(ultima.getPecho()));
        if (ultima.getBrazos() > 0)        body.put("brazos", BigDecimal.valueOf(ultima.getBrazos()));
        if (ultima.getPiernas() > 0)       body.put("piernas", BigDecimal.valueOf(ultima.getPiernas()));
        if (ultima.getNotas() != null && !ultima.getNotas().isEmpty()) body.put("notas", ultima.getNotas());
        return body;
    }
}
