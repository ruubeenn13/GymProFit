package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// ZonasDelDia — las zonas del cuerpo que trabaja la rutina de hoy (GP-216, «Hoy toca»).
//
// De los ejercicios de la rutina (cada uno con su clave de músculo, de Musculos, y sus series)
// salen las series por músculo, que enciende la silueta, y las zonas ordenadas por series,
// que escribe el subtítulo de la tarjeta. Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class ZonasDelDia {

    /** Un ejercicio de la rutina: la clave de su músculo (null si no tiene) y sus series. */
    public static final class Ejercicio {
        @Nullable public final String musculo;
        public final int series;

        public Ejercicio(@Nullable String musculo, int series) {
            this.musculo = musculo;
            this.series = series;
        }
    }

    /** Series por clave de músculo, en el orden en que aparecen. */
    public final Map<String, Integer> seriesPorMusculo;
    /** Las zonas tocadas, de más a menos series. */
    public final List<Zonas.Zona> zonas;

    private ZonasDelDia(Map<String, Integer> seriesPorMusculo, List<Zonas.Zona> zonas) {
        this.seriesPorMusculo = Collections.unmodifiableMap(seriesPorMusculo);
        this.zonas = Collections.unmodifiableList(zonas);
    }

    /** Las zonas de esos ejercicios. Un ejercicio sin series cuenta una, para que se encienda. */
    public static ZonasDelDia de(@Nullable List<Ejercicio> ejercicios) {
        Map<String, Integer> porMusculo = new LinkedHashMap<>();
        if (ejercicios != null) {
            for (Ejercicio e : ejercicios) {
                if (e.musculo == null) continue;
                porMusculo.merge(e.musculo, Math.max(1, e.series), Integer::sum);
            }
        }
        LinkedHashMap<Zonas.Zona, Integer> porZona = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> m : porMusculo.entrySet()) {
            Zonas.Zona z = Zonas.deMusculo(m.getKey());
            if (z != null) porZona.merge(z, m.getValue(), Integer::sum);
        }
        List<Zonas.Zona> zonas = new ArrayList<>(porZona.keySet());
        // Estable: a igualdad de series, el orden en que aparecieron.
        Collections.sort(zonas, (a, b) -> Integer.compare(porZona.get(b), porZona.get(a)));
        return new ZonasDelDia(porMusculo, zonas);
    }
}
