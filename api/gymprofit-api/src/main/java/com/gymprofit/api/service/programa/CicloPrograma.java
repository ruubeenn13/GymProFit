package com.gymprofit.api.service.programa;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// ============================================================
// CicloPrograma — qué rutina toca en un programa que se sigue (GP-074, lote 1.2.1)
//
// Lógica pura. El programa no tiene días fijos: su semana es un ciclo que vuelve a
// empezar. Desde la posición inicial, cada sesión hecha con una de sus rutinas, por orden
// de fecha, lleva a la siguiente aparición de esa rutina en el ciclo, y toca la de
// después. Así:
//   · valen las rutinas que se repiten (Empuje y Tirón en el de 6 días): cuenta la
//     aparición más próxima, no la primera;
//   · hacer una fuera de orden salta las de en medio;
//   · faltar un día no salta ninguna: solo cuentan las sesiones, no el calendario.
// Las rutinas borradas (copias inactivas) se saltan al decidir la de hoy. «Hechas» son
// las posiciones hechas en esta vuelta, para la barra del ciclo: al dar la vuelta se
// vacían.
//
// Posiciones desde 1, como en la API.
// ============================================================
public final class CicloPrograma {

    private CicloPrograma() { }

    /**
     * Estado del ciclo.
     *
     * @param posicionHoy la que toca, desde 1; null si todas sus rutinas están borradas.
     * @param hechas      posiciones hechas en esta vuelta, desde 1.
     */
    public record Estado(Integer posicionHoy, Set<Integer> hechas) { }

    /**
     * @param semana          códigos de plantilla del ciclo, en orden.
     * @param activas         códigos cuya copia sigue activa.
     * @param posicionInicial desde 1; fuera de rango cuenta como 1.
     * @param sesiones        código de plantilla de cada sesión hecha, por orden de fecha.
     */
    public static Estado calcular(List<String> semana, Set<String> activas, int posicionInicial,
                                  List<String> sesiones) {
        int n = semana.size();
        if (n == 0) return new Estado(null, Set.of());

        int toca = posicionInicial >= 1 && posicionInicial <= n ? posicionInicial - 1 : 0;
        Set<Integer> hechas = new TreeSet<>();
        for (String codigo : sesiones) {
            int salto = -1;
            for (int k = 0; k < n; k++) {
                if (semana.get((toca + k) % n).equals(codigo)) {
                    salto = k;
                    break;
                }
            }
            if (salto < 0) continue; // una rutina que no es del ciclo no mueve nada

            if (toca + salto >= n) hechas.clear(); // ha dado la vuelta
            int hecha = (toca + salto) % n;
            hechas.add(hecha + 1);
            toca = hecha + 1;
            if (toca == n) {
                toca = 0;
                hechas.clear(); // vuelta completa: la barra empieza de cero
            }
        }

        for (int k = 0; k < n; k++) {
            int p = (toca + k) % n;
            if (activas.contains(semana.get(p))) return new Estado(p + 1, hechas);
        }
        return new Estado(null, hechas);
    }
}
