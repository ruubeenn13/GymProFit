package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;

// ============================================================
// OrdenRutinas — un solo orden para las dos hojas de elegir rutina (GP-241, punto 6):
// «¿Qué rutina hoy?» (Inicio y Entrenar) y «¿Qué entrenaste?» (Apuntar entreno).
//
//   1. La que toca hoy (la que elige HoyToca).
//   2. El resto del ciclo del programa, en el orden en que viene tras la de hoy: con el
//      ciclo A, B, C, D y hoy C, va C, D, A, B. Una rutina que se repite en el ciclo sale una
//      vez, en su primer sitio tras hoy; un día borrado no cuenta.
//   3. Las tuyas, en el orden de «Mis rutinas» (el de la lista de activas).
//
// Sin programa, la de hoy es la de «Mis rutinas» que más tiempo lleva sin hacerse, y va
// primero entre las tuyas. Lógica pura: se prueba con OrdenRutinasTest.
// ============================================================
public final class OrdenRutinas {

    private OrdenRutinas() { }

    /** Las rutinas de las dos secciones, cada una con la de hoy delante si es suya. */
    public static final class Orden {
        /** Las del programa: la de hoy y el resto del ciclo tras ella. */
        @NonNull public final List<Rutina> programa;
        /** Las tuyas, en el orden de «Mis rutinas». */
        @NonNull public final List<Rutina> propias;
        /** El nombre del programa, para «De tu programa, …»; null sin programa. */
        @Nullable public final String nombrePrograma;
        /** La que toca hoy, o null. */
        @Nullable public final Integer hoyId;

        Orden(@NonNull List<Rutina> programa, @NonNull List<Rutina> propias, @Nullable String nombrePrograma,
              @Nullable Integer hoyId) {
            this.programa = Collections.unmodifiableList(programa);
            this.propias = Collections.unmodifiableList(propias);
            this.nombrePrograma = nombrePrograma;
            this.hoyId = hoyId;
        }

        /** Las dos secciones seguidas, con la de hoy la primera: la hoja sin títulos. */
        @NonNull
        public List<Rutina> todas() {
            List<Rutina> todas = new ArrayList<>();
            Rutina hoy = null;
            for (Rutina r : propias) if (hoyId != null && r.getId() == hoyId) hoy = r;
            if (hoy != null) todas.add(hoy);
            todas.addAll(programa);
            for (Rutina r : propias) if (r != hoy) todas.add(r);
            return todas;
        }
    }

    /**
     * Ordena las rutinas activas.
     *
     * @param activas las activas del usuario (propias y del programa), en el orden de la API.
     * @param seguido el programa que sigue, o null.
     * @param hoyId   la que toca hoy (HoyToca), o null.
     */
    @NonNull
    public static Orden ordenar(@Nullable List<Rutina> activas, @Nullable ProgramaQueSigue seguido,
                                @Nullable Integer hoyId) {
        List<Rutina> programa = new ArrayList<>();
        List<Rutina> propias = new ArrayList<>();
        Map<Integer, Rutina> delPrograma = new LinkedHashMap<>();
        String nombre = seguido != null && seguido.getPrograma() != null ? seguido.getPrograma().getNombre() : null;
        if (activas != null) {
            for (Rutina r : activas) {
                if (r.esDePrograma()) {
                    delPrograma.put(r.getId(), r);
                    if (nombre == null) nombre = r.getProgramaNombre();
                } else {
                    propias.add(r);
                }
            }
        }

        // El ciclo desde hoy: primero las posiciones de hoy en adelante, luego las de antes.
        for (Integer id : cicloDesdeHoy(seguido)) {
            Rutina r = delPrograma.remove(id);
            if (r != null) programa.add(r);
        }
        // Las del programa que el ciclo no nombra (no debería pasar), al final, en su orden.
        programa.addAll(delPrograma.values());

        delante(programa, hoyId);
        delante(propias, hoyId);
        return new Orden(programa, propias, programa.isEmpty() ? null : nombre, hoyId);
    }

    // Los ids del ciclo empezando por el día de hoy, sin repetir y sin días borrados.
    private static List<Integer> cicloDesdeHoy(@Nullable ProgramaQueSigue seguido) {
        List<Integer> ids = new ArrayList<>();
        if (seguido == null || seguido.getCiclo() == null) return ids;
        List<ProgramaQueSigue.DiaCiclo> dias = new ArrayList<>(seguido.getCiclo());
        Collections.sort(dias, (a, b) -> Integer.compare(a.getPosicion(), b.getPosicion()));
        int hoy = seguido.getPosicionHoy() != null ? seguido.getPosicionHoy() : 0;
        List<ProgramaQueSigue.DiaCiclo> enOrden = new ArrayList<>();
        for (ProgramaQueSigue.DiaCiclo d : dias) if (d.getPosicion() >= hoy) enOrden.add(d);
        for (ProgramaQueSigue.DiaCiclo d : dias) if (d.getPosicion() < hoy) enOrden.add(d);
        for (ProgramaQueSigue.DiaCiclo d : enOrden) {
            if (!d.isActiva() || d.getRutinaId() == null || ids.contains(d.getRutinaId())) continue;
            ids.add(d.getRutinaId());
        }
        return ids;
    }

    // Lleva la de hoy al principio de su lista, si está; el resto no se mueve.
    private static void delante(List<Rutina> lista, @Nullable Integer hoyId) {
        if (hoyId == null) return;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == hoyId) {
                lista.add(0, lista.remove(i));
                return;
            }
        }
    }
}
