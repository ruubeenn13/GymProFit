package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.Rutina;

// ============================================================
// TuPrograma — qué va a «Tu programa» y qué a «Mis rutinas» (GP-074, lote 1.2.1).
//
// Lógica pura, sin Android. La rutina que toca la calcula la API (GET
// /programas/seguido): aquí solo se reparte lo que llega.
//   · «Mis rutinas» son las propias: las de un programa (con programaUsuarioId) van a
//     «Tu programa», no a la lista.
//   · La de hoy es la primera de las rutinas del programa, que la API manda en el orden
//     en que tocan; el resto son las demás, una vez cada una.
//   · La barra del ciclo: cada posición hecha en esta vuelta, la de hoy, pendiente o
//     borrada (su rutina está desactivada y el ciclo la salta).
// ============================================================
public final class TuPrograma {

    private TuPrograma() { }

    /** Cómo se pinta cada posición de la barra del ciclo. */
    public enum EstadoDia { HECHA, HOY, PENDIENTE, BORRADA }

    /** Las rutinas de «Mis rutinas»: las activas que no son de un programa. */
    public static List<Rutina> misRutinas(@Nullable List<Rutina> activas) {
        List<Rutina> propias = new ArrayList<>();
        if (activas == null) return propias;
        for (Rutina r : activas) {
            if (!r.esDePrograma()) propias.add(r);
        }
        return propias;
    }

    /** La rutina que toca hoy en el programa, o null si ha borrado todas. */
    @Nullable
    public static RutinaConEjercicios hoy(@Nullable ProgramaQueSigue seguido) {
        if (seguido == null || seguido.getPosicionHoy() == null) return null;
        List<RutinaConEjercicios> rutinas = seguido.getRutinas();
        return rutinas == null || rutinas.isEmpty() ? null : rutinas.get(0);
    }

    /** Las demás rutinas del programa, una vez cada una, en el orden en que tocan. */
    public static List<RutinaConEjercicios> resto(@Nullable ProgramaQueSigue seguido) {
        List<RutinaConEjercicios> resto = new ArrayList<>();
        if (hoy(seguido) == null) return resto;
        List<RutinaConEjercicios> rutinas = seguido.getRutinas();
        for (int i = 1; i < rutinas.size(); i++) resto.add(rutinas.get(i));
        return resto;
    }

    /** Si sigue un programa pero ha borrado todas sus rutinas. */
    public static boolean todasBorradas(@Nullable ProgramaQueSigue seguido) {
        return seguido != null && hoy(seguido) == null;
    }

    /** El estado de cada posición del ciclo, en orden. */
    public static List<EstadoDia> barra(@Nullable ProgramaQueSigue seguido) {
        List<EstadoDia> barra = new ArrayList<>();
        if (seguido == null || seguido.getCiclo() == null) return barra;
        Integer hoy = seguido.getPosicionHoy();
        for (ProgramaQueSigue.DiaCiclo d : seguido.getCiclo()) {
            if (!d.isActiva()) barra.add(EstadoDia.BORRADA);
            else if (hoy != null && d.getPosicion() == hoy) barra.add(EstadoDia.HOY);
            else if (d.isHecha()) barra.add(EstadoDia.HECHA);
            else barra.add(EstadoDia.PENDIENTE);
        }
        return barra;
    }

    /**
     * Qué día del ciclo toca hoy, contando desde 1: el lugar de la posición de hoy en el ciclo
     * («Día 3 de 3»). Inicio y Entrenar lo leen de aquí; Inicio contaba las hechas más una y
     * decía otro día (lote 12).
     *
     * @return 0 si no hay programa o no se sabe qué toca.
     */
    public static int diaHoy(@Nullable ProgramaQueSigue seguido) {
        if (seguido == null || seguido.getCiclo() == null || seguido.getPosicionHoy() == null) return 0;
        List<ProgramaQueSigue.DiaCiclo> ciclo = seguido.getCiclo();
        for (int i = 0; i < ciclo.size(); i++) {
            if (ciclo.get(i).getPosicion() == seguido.getPosicionHoy()) return i + 1;
        }
        return 0;
    }

    /** Cuántos días tiene el ciclo, para «Día 3 de 3». */
    public static int diasCiclo(@Nullable ProgramaQueSigue seguido) {
        return seguido == null || seguido.getCiclo() == null ? 0 : seguido.getCiclo().size();
    }

    /** Cuántas posiciones van hechas en esta vuelta. */
    public static int hechas(@Nullable ProgramaQueSigue seguido) {
        int n = 0;
        for (EstadoDia e : barra(seguido)) if (e == EstadoDia.HECHA) n++;
        return n;
    }

    /**
     * Junta nombres como en una frase: «A», «A ni B», «A, B ni C». El conector va en
     * strings.xml («ni» en español, «or» en inglés).
     */
    public static String enumerar(List<String> nombres, String separador, String conector) {
        if (nombres == null || nombres.isEmpty()) return "";
        if (nombres.size() == 1) return nombres.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nombres.size() - 1; i++) {
            if (i > 0) sb.append(separador);
            sb.append(nombres.get(i));
        }
        return sb.append(conector).append(nombres.get(nombres.size() - 1)).toString();
    }
}
