package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ============================================================
// CambioPeso — «Tu cuerpo» de Progreso (GP-216, lote 4, tablero 16): el último peso, cuánto ha
// cambiado en las últimas 8 semanas y los puntos de la línea.
//
// Las mediciones llegan de la más reciente a la más antigua (GET …/ordenadas) y algunas no traen
// peso (solo la cintura, por ejemplo): esas no cuentan. La ventana son los 56 días que acaban en
// el último peso. El cambio se mide contra el peso más antiguo de esa ventana y dice las semanas
// que de verdad abarca: con dos pesos en tres semanas no se afirma «en 8 semanas».
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class CambioPeso {

    /** Días de la ventana del cambio: 8 semanas. */
    public static final int DIAS_VENTANA = 56;

    /** Una medición con peso: su día ISO y los kilos. */
    public static final class Punto {
        public final String fecha;
        public final double peso;

        public Punto(String fecha, double peso) {
            this.fecha = fecha;
            this.peso = peso;
        }
    }

    /** Lo que enseña la tarjeta. */
    public static final class Resultado {
        /** El último peso. */
        public final Punto ultimo;
        /** Kilos de cambio (con signo) en la ventana; null con un solo peso en ella. */
        @Nullable public final Double cambio;
        /** Semanas que abarca el cambio, al menos 1. */
        public final int semanas;
        /** Los pesos de la ventana, del más antiguo al último, para la línea. */
        public final List<Punto> puntos;

        Resultado(Punto ultimo, @Nullable Double cambio, int semanas, List<Punto> puntos) {
            this.ultimo = ultimo;
            this.cambio = cambio;
            this.semanas = semanas;
            this.puntos = puntos;
        }
    }

    private CambioPeso() { }

    /**
     * @param recientePrimero las mediciones, de la más reciente a la más antigua.
     * @return null si ninguna trae peso.
     */
    @Nullable
    public static Resultado de(@NonNull List<Punto> recientePrimero) {
        Punto ultimo = null;
        List<Punto> ventana = new ArrayList<>();
        for (Punto p : recientePrimero) {
            if (p == null || p.peso <= 0 || p.fecha == null) continue;
            if (ultimo == null) ultimo = p;
            int dias = TiempoRelativo.diasEntre(p.fecha, ultimo.fecha);
            if (dias < 0 || dias > DIAS_VENTANA) continue;
            // Un peso por día, el último de ese día, como la línea (EjeFechas): con varios el
            // mismo día, el cambio no puede medirse desde uno que la línea no enseña (GP-228).
            if (!ventana.isEmpty()
                    && TiempoRelativo.diasEntre(p.fecha, ventana.get(ventana.size() - 1).fecha) == 0) {
                continue;
            }
            ventana.add(p);
        }
        if (ultimo == null) return null;
        Collections.reverse(ventana);
        Punto primero = ventana.get(0);
        if (ventana.size() < 2) return new Resultado(ultimo, null, 1, ventana);
        int dias = TiempoRelativo.diasEntre(primero.fecha, ultimo.fecha);
        int semanas = Math.max(1, (int) Math.round(dias / 7.0));
        // Al décimo, como se enseña: 79,4 − 82,1 da −2,7 y no −2,6999.
        double cambio = Math.round((ultimo.peso - primero.peso) * 10) / 10.0;
        return new Resultado(ultimo, cambio, semanas, ventana);
    }
}
