package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.sesion.ResumenSesion;

// ============================================================
// ComparacionEntreno — «Frente a la última vez» del resumen del entreno (GP-210 paso 1,
// GP-023), en lógica pura para poder probarla.
//
// Por ejercicio, los kilos de hoy y el % frente a la última vez que se hizo ese ejercicio
// (lo calcula el servidor con la definición de volumen de GP-011; aquí solo se compara).
//   · La primera vez no tiene porcentaje: «Primera vez: es tu punto de partida».
//   · Una marca anterior sin kilos (peso corporal) tampoco: dividir entre cero no es un
//     «+∞ %», es no tener con qué comparar.
//   · Lo que no mueve kilos (por tiempo o de cero kilos) no sale: no hay cifra que enseñar.
//   · Una diferencia que redondea a 0 % es «igual», no un «+0 %» ni un «−0 %».
// ============================================================
public final class ComparacionEntreno {

    private ComparacionEntreno() { }

    /** Hacia dónde va una cifra frente a la anterior. */
    public enum Tendencia { SUBE, BAJA, IGUAL }

    /** El % frente a la vez anterior: con signo, redondeado y sin decimales. */
    public static final class Variacion {
        @NonNull public final Tendencia tendencia;
        /** Positivo si sube, negativo si baja, 0 si es igual. */
        public final int porcentaje;

        Variacion(@NonNull Tendencia tendencia, int porcentaje) {
            this.tendencia = tendencia;
            this.porcentaje = porcentaje;
        }
    }

    /** Una fila de «Frente a la última vez». */
    public static final class Fila {
        public final int ejercicioId;
        @NonNull public final String nombre;
        public final double kilos;
        /** null en la primera vez, o sin con qué comparar. */
        @Nullable public final Variacion variacion;

        Fila(int ejercicioId, @NonNull String nombre, double kilos, @Nullable Variacion variacion) {
            this.ejercicioId = ejercicioId;
            this.nombre = nombre;
            this.kilos = kilos;
            this.variacion = variacion;
        }

        /** Sin porcentaje: «Primera vez: es tu punto de partida». */
        public boolean primeraVez() {
            return variacion == null;
        }
    }

    /**
     * El % de {@code actual} frente a {@code anterior}.
     *
     * @return null si {@code anterior} no es positivo: no hay con qué comparar.
     */
    @Nullable
    public static Variacion variacion(double actual, double anterior) {
        if (anterior <= 0) return null;
        int pct = (int) Math.round((actual - anterior) * 100.0 / anterior);
        if (pct == 0) return new Variacion(Tendencia.IGUAL, 0);
        return new Variacion(pct > 0 ? Tendencia.SUBE : Tendencia.BAJA, pct);
    }

    /**
     * Las filas de «Frente a la última vez», en el orden en que se hicieron los ejercicios.
     *
     * @param resumen el resumen de la sesión; null o sin ejercicios da una lista vacía.
     */
    @NonNull
    public static List<Fila> filas(@Nullable ResumenSesion resumen) {
        List<Fila> filas = new ArrayList<>();
        if (resumen == null) return filas;
        for (ResumenSesion.Ejercicio e : resumen.getEjercicios()) {
            if (e.isPorTiempo() || e.getVolumenKg() <= 0) continue;
            ResumenSesion.UltimaMarca antes = e.getAnterior();
            Variacion v = antes == null ? null : variacion(e.getVolumenKg(), antes.getVolumenKg());
            filas.add(new Fila(e.getEjercicioId(), e.getNombre() != null ? e.getNombre() : "", e.getVolumenKg(), v));
        }
        return filas;
    }

    /**
     * La ficha de la cabecera: los kilos de la sesión frente a la anterior de la misma rutina.
     *
     * @return null si no hay sesión anterior (la primera vez, o un entrenamiento libre).
     */
    @Nullable
    public static Variacion variacionDeSesion(@Nullable ResumenSesion resumen) {
        if (resumen == null || resumen.getAnterior() == null) return null;
        return variacion(resumen.getVolumenKg(), resumen.getAnterior().getVolumenKg());
    }
}
