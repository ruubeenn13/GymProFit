package com.gymprofit.api.service.programa;

import com.gymprofit.api.enums.MedidaSerie;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.PorLado;
import com.gymprofit.api.enums.TipoEjercicioRutina;
import com.gymprofit.api.enums.TipoObjetivo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// ============================================================
// ReglasPrograma — las reglas del punto 3 del catálogo v1, aplicadas en la API (GP-074)
//
// Lógica pura, sin base de datos, para poder probarla contra el JSON en todas las
// rutinas, niveles, objetivos y tiempos. Al seguir un programa, cada plantilla se copia
// pasando por aquí:
//
//   · Avanzado (AVANZADO o EXPERTO en el perfil) en un programa de intermedio: básicos a
//     4 series y extras a 3. En los de principiante no se toca nada.
//   · «Ganar fuerza» (MEJORAR_FUERZA): básicos a 4–6 repeticiones con 3 min de descanso,
//     salvo los de peso corporal, que no tienen carga que subir.
//   · Tiempo: con 75 min, antes se suma una serie a cada básico. Si no cabe en el tiempo
//     elegido se recorta en este orden: extras, desde el final; una serie menos a cada
//     básico, hasta dejarlos en 2; y el último básico.
//
// Duración estimada, la fórmula del catálogo tal cual: 8 min de calentamiento, 1 min por
// cada cambio de ejercicio y, por serie, el trabajo (3,5 s por repetición a mitad del
// rango, o los segundos; el doble si es por lado) más su descanso, salvo tras la última
// serie de la rutina. Se redondea a los 5 min más cercanos, la mitad hacia arriba.
//
// Las cuentas van en centésimas de segundo con enteros: 3,5 s × (mín + máx) / 2 son
// 175 cs por unidad, y así el redondeo «la mitad hacia arriba» es exacto.
// ============================================================
public final class ReglasPrograma {

    /** Los tiempos por sesión que se aceptan. */
    public static final Set<Integer> MINUTOS_VALIDOS = Set.of(30, 45, 60, 75);

    /** Si la petición no trae minutos. */
    public static final int MINUTOS_POR_DEFECTO = 60;

    private static final long CALENTAMIENTO_CS = 8 * 60 * 100;
    private static final long CAMBIO_CS = 60 * 100;

    private ReglasPrograma() { }

    /**
     * Un ejercicio de rutina, con lo que usan las reglas.
     *
     * @param indice       posición en la plantilla; sirve para volver a la fila de origen.
     * @param pesoCorporal si el ejercicio es de peso corporal (no le afecta «Ganar fuerza»).
     * @param descanso     segundos de descanso tras cada serie.
     */
    public record Ejercicio(int indice, TipoEjercicioRutina tipo, int series, int min, int max,
                            MedidaSerie medida, PorLado porLado, int descanso, boolean pesoCorporal) {

        Ejercicio conSeries(int s) {
            return new Ejercicio(indice, tipo, s, min, max, medida, porLado, descanso, pesoCorporal);
        }

        boolean basico() {
            return tipo == TipoEjercicioRutina.BASICO;
        }
    }

    /**
     * Resultado de aplicar las reglas.
     *
     * @param ejercicios   los que quedan, en su orden.
     * @param centesimas   duración estimada sin redondear, en centésimas de segundo.
     * @param duracion     duración estimada en minutos, redondeada a 5.
     * @param avanzado     si se aplicaron las series de avanzado.
     */
    public record Resultado(List<Ejercicio> ejercicios, long centesimas, int duracion, boolean avanzado) {

        /** @return la duración sin redondear, en minutos. */
        public double minutos() {
            return centesimas / 6000.0;
        }
    }

    /**
     * Aplica nivel, objetivo y tiempo a una plantilla.
     *
     * @param plantilla     ejercicios de la plantilla, en orden.
     * @param nivelPrograma PRINCIPIANTE o INTERMEDIO.
     * @param nivelUsuario  nivel del perfil; puede ser null.
     * @param objetivo      objetivo del perfil; puede ser null.
     * @param minutos       30, 45, 60 o 75.
     * @return la rutina ajustada y su duración.
     */
    public static Resultado aplicar(List<Ejercicio> plantilla, Nivel nivelPrograma,
                                    NivelExperiencia nivelUsuario, TipoObjetivo objetivo, int minutos) {
        if (!MINUTOS_VALIDOS.contains(minutos)) {
            throw new IllegalArgumentException("Minutos no válidos: " + minutos);
        }
        boolean avanzado = nivelPrograma == Nivel.INTERMEDIO
                && (nivelUsuario == NivelExperiencia.AVANZADO || nivelUsuario == NivelExperiencia.EXPERTO);
        boolean fuerza = objetivo == TipoObjetivo.MEJORAR_FUERZA;

        List<Ejercicio> rutina = new ArrayList<>();
        for (Ejercicio e : plantilla) {
            Ejercicio a = e;
            if (avanzado) a = a.conSeries(a.basico() ? 4 : 3);
            if (fuerza && a.basico() && !a.pesoCorporal() && a.medida() == MedidaSerie.REPETICIONES) {
                a = new Ejercicio(a.indice(), a.tipo(), a.series(), 4, 6, a.medida(), a.porLado(), 180,
                        a.pesoCorporal());
            }
            if (minutos >= 75 && a.basico()) a = a.conSeries(a.series() + 1);
            rutina.add(a);
        }

        long limite = minutos * 6000L;
        while (centesimas(rutina) > limite && recortar(rutina)) {
            // recortar() quita un paso cada vez y dice si quedaba algo que quitar.
        }

        long cs = centesimas(rutina);
        return new Resultado(List.copyOf(rutina), cs, redondear(cs), avanzado);
    }

    // Un paso de recorte, en el orden del catálogo. false si ya no se puede recortar más.
    private static boolean recortar(List<Ejercicio> rutina) {
        for (int i = rutina.size() - 1; i >= 0; i--) {
            if (!rutina.get(i).basico()) {
                rutina.remove(i);
                return true;
            }
        }
        boolean bajada = false;
        for (int i = 0; i < rutina.size(); i++) {
            Ejercicio e = rutina.get(i);
            if (e.series() > 2) {
                rutina.set(i, e.conSeries(e.series() - 1));
                bajada = true;
            }
        }
        if (bajada) return true;
        if (rutina.size() > 1) {
            rutina.remove(rutina.size() - 1);
            return true;
        }
        return false;
    }

    /**
     * Duración estimada sin redondear.
     *
     * @param rutina ejercicios en orden.
     * @return centésimas de segundo.
     */
    public static long centesimas(List<Ejercicio> rutina) {
        long total = CALENTAMIENTO_CS + CAMBIO_CS * Math.max(0, rutina.size() - 1);
        int seriesTotales = rutina.stream().mapToInt(Ejercicio::series).sum();
        int hechas = 0;
        for (Ejercicio e : rutina) {
            long trabajo = (long) (e.min() + e.max()) * (e.medida() == MedidaSerie.SEGUNDOS ? 50 : 175);
            if (e.porLado() != null) trabajo *= 2;
            for (int s = 0; s < e.series(); s++) {
                total += trabajo;
                if (++hechas < seriesTotales) total += e.descanso() * 100L;
            }
        }
        return total;
    }

    /**
     * A los 5 min más cercanos, la mitad hacia arriba.
     *
     * @param centesimas duración en centésimas de segundo.
     * @return minutos, múltiplo de 5.
     */
    public static int redondear(long centesimas) {
        // 5 min son 30 000 cs; sumar la mitad y truncar redondea la mitad hacia arriba.
        return (int) ((centesimas + 15_000) / 30_000) * 5;
    }
}
