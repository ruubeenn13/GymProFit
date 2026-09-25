package com.gymprofit.api.service.record;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

// ============================================================
// CalculadoraRecords — récords y progresión a partir de las series (GP-088)
//
// Lógica pura, sin base de datos: recibe las series completadas de un usuario y
// recorre cada ejercicio en orden cronológico, sesión a sesión. Por eso se puede
// probar sin contexto y por eso los récords no se guardan en ningún sitio: se
// deducen siempre de lo que se entrenó. Si se borra la sesión de un récord, sus
// series desaparecen y el récord vuelve solo al anterior.
//
// Reglas, aprobadas por el propietario:
//   · Con peso, la marca es la serie más pesada, y a igualdad de peso la de más
//     repeticiones. Sin peso, las repeticiones máximas.
//   · Igualar una marca no es récord; superarla, sí.
//   · La primera marca de un ejercicio no es un récord: es el punto de partida.
//   · Con peso y sin peso son dos marcas distintas del mismo ejercicio (unas
//     dominadas lastradas no compiten con unas sin lastre).
//   · 1RM estimado con la fórmula de Epley: peso × (1 + repeticiones / 30).
// ============================================================
public final class CalculadoraRecords {

    private CalculadoraRecords() { }

    /** Qué mide una marca: kilos (con sus repeticiones) o solo repeticiones. */
    public enum Tipo { PESO, REPETICIONES }

    /**
     * Una serie completada, tal como sale de la base de datos.
     *
     * @param peso kilos; {@code null} o cero en un ejercicio sin peso.
     */
    public record Serie(Integer sesionId, LocalDateTime fecha, Integer ejercicioId,
                        BigDecimal peso, int repeticiones) { }

    /** La mejor serie de un ejercicio en una sesión. */
    public record Marca(Integer ejercicioId, Tipo tipo, BigDecimal peso, int repeticiones,
                        BigDecimal unoRmEstimado, Integer sesionId, LocalDateTime fecha) { }

    /**
     * Un momento en que cambió la marca de un ejercicio.
     *
     * @param anterior la marca superada, o {@code null} si es la primera (punto de partida).
     */
    public record Evento(Marca marca, Marca anterior) {
        /** @return {@code true} si es la primera marca del ejercicio y no un récord. */
        public boolean esPrimera() {
            return anterior == null;
        }
    }

    /**
     * Resultado del recorrido.
     *
     * @param eventos    récords y primeras marcas, en orden cronológico.
     * @param progresion por ejercicio, la mejor serie de cada sesión en orden cronológico.
     */
    public record Resultado(List<Evento> eventos, Map<Integer, List<Marca>> progresion) {

        /** @return solo los récords de verdad, sin las primeras marcas. */
        public List<Evento> records() {
            return eventos.stream().filter(e -> !e.esPrimera()).collect(Collectors.toList());
        }

        /**
         * El récord vigente de cada ejercicio que tiene alguno. Si un ejercicio tiene
         * récord con peso y sin peso, manda el de peso.
         *
         * @return por ejercicio, el último récord batido.
         */
        public Map<Integer, Evento> recordVigentePorEjercicio() {
            Map<Integer, Evento> vigente = new LinkedHashMap<>();
            for (Evento e : records()) {
                Evento previo = vigente.get(e.marca().ejercicioId());
                if (previo == null || previo.marca().tipo() == e.marca().tipo()
                        || e.marca().tipo() == Tipo.PESO) {
                    vigente.put(e.marca().ejercicioId(), e);
                }
            }
            return vigente;
        }

        /**
         * @param sesionId sesión de la que se quieren los cambios de marca.
         * @return los récords y primeras marcas que se hicieron en esa sesión.
         */
        public List<Evento> deSesion(Integer sesionId) {
            return eventos.stream().filter(e -> e.marca().sesionId().equals(sesionId))
                    .collect(Collectors.toList());
        }
    }

    // Orden de una sesión dentro de la historia: por fecha y, en empate, por id.
    private static final Comparator<Serie> CRONOLOGICO = Comparator
            .comparing(Serie::fecha, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Serie::sesionId);

    /**
     * Recorre las series y saca récords, primeras marcas y progresión.
     *
     * @param series series completadas de un usuario, en cualquier orden.
     * @return el resultado del recorrido.
     */
    public static Resultado calcular(List<Serie> series) {
        List<Evento> eventos = new ArrayList<>();
        Map<Integer, List<Marca>> progresion = new TreeMap<>();

        Map<Integer, List<Serie>> porEjercicio = series.stream()
                .filter(s -> s.repeticiones() > 0)
                .collect(Collectors.groupingBy(Serie::ejercicioId, TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<Serie>> ejercicio : porEjercicio.entrySet()) {
            // Sesiones del ejercicio, en orden cronológico.
            Map<Integer, List<Serie>> porSesion = ejercicio.getValue().stream()
                    .sorted(CRONOLOGICO)
                    .collect(Collectors.groupingBy(Serie::sesionId, LinkedHashMap::new, Collectors.toList()));

            Map<Tipo, Marca> mejor = new HashMap<>();
            List<Marca> puntos = new ArrayList<>();

            for (List<Serie> sesion : porSesion.values()) {
                Marca conPeso = mejorConPeso(sesion);
                Marca sinPeso = mejorSinPeso(sesion);
                puntos.add(conPeso != null ? conPeso : sinPeso);

                for (Marca m : new Marca[]{conPeso, sinPeso}) {
                    if (m == null) continue;
                    Marca previa = mejor.get(m.tipo());
                    if (previa == null) {
                        eventos.add(new Evento(m, null));
                        mejor.put(m.tipo(), m);
                    } else if (supera(m, previa)) {
                        eventos.add(new Evento(m, previa));
                        mejor.put(m.tipo(), m);
                    }
                }
            }
            progresion.put(ejercicio.getKey(), puntos);
        }

        eventos.sort(Comparator
                .comparing((Evento e) -> e.marca().fecha(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(e -> e.marca().sesionId())
                .thenComparing(e -> e.marca().ejercicioId()));
        return new Resultado(eventos, progresion);
    }

    /**
     * 1RM estimado con la fórmula de Epley, redondeado a una décima.
     *
     * @param peso         kilos de la serie.
     * @param repeticiones repeticiones de la serie.
     * @return el 1RM estimado en kilos.
     */
    public static BigDecimal unoRmEpley(BigDecimal peso, int repeticiones) {
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(repeticiones).divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP));
        return peso.multiply(factor).setScale(1, RoundingMode.HALF_UP);
    }

    // true si `nueva` supera a `previa`: igualar no cuenta.
    private static boolean supera(Marca nueva, Marca previa) {
        if (nueva.tipo() == Tipo.REPETICIONES) return nueva.repeticiones() > previa.repeticiones();
        int porPeso = nueva.peso().compareTo(previa.peso());
        return porPeso > 0 || (porPeso == 0 && nueva.repeticiones() > previa.repeticiones());
    }

    private static boolean tienePeso(Serie s) {
        return s.peso() != null && s.peso().signum() > 0;
    }

    // La serie más pesada de la sesión; a igual peso, la de más repeticiones.
    private static Marca mejorConPeso(List<Serie> sesion) {
        return sesion.stream().filter(CalculadoraRecords::tienePeso)
                .max(Comparator.comparing(Serie::peso).thenComparingInt(Serie::repeticiones))
                .map(s -> new Marca(s.ejercicioId(), Tipo.PESO, s.peso().stripTrailingZeros(),
                        s.repeticiones(), unoRmEpley(s.peso(), s.repeticiones()), s.sesionId(), s.fecha()))
                .orElse(null);
    }

    // La serie sin peso con más repeticiones de la sesión.
    private static Marca mejorSinPeso(List<Serie> sesion) {
        return sesion.stream().filter(s -> !tienePeso(s))
                .max(Comparator.comparingInt(Serie::repeticiones))
                .map(s -> new Marca(s.ejercicioId(), Tipo.REPETICIONES, null, s.repeticiones(),
                        null, s.sesionId(), s.fecha()))
                .orElse(null);
    }
}
