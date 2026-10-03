package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.ComidaReciente;

// ============================================================
// ComidasRecientes — copiar una comida entera (lote 1.6.4, B1 y B2), sin vistas
//
// · Cómo se llama una comida reciente: «Merienda de hoy», «de ayer», «del lunes» (de
//   hace 2 a 6 días) o «del 28 de septiembre». Cuándo es, frente al día de hoy.
// · «¿Copiar la de ayer?»: de las recientes que da la API para una comida, la del mismo
//   tipo del día anterior a esa comida, si la hubo. Si no, no se propone nada.
// · Lo descartado con la ✗: «fecha|tipo» por cuenta, en el móvil, para que no vuelva ese
//   día para esa comida. Se guardan como mucho las últimas MAXIMO_DESCARTES.
// ============================================================
public final class ComidasRecientes {

    /** Cuándo fue una comida, para su nombre. */
    public enum Cuando { HOY, AYER, SEMANA, FECHA }

    /** Descartes que se guardan como mucho: de sobra para no volver a ver uno reciente. */
    public static final int MAXIMO_DESCARTES = 60;

    private ComidasRecientes() {
    }

    /**
     * @param fecha día de la comida (yyyy-MM-dd).
     * @param hoy   el día de hoy.
     * @return HOY, AYER, SEMANA (de hace 2 a 6 días) o FECHA (lo demás, también un día que no ha llegado).
     */
    @NonNull
    public static Cuando cuando(@NonNull String fecha, @NonNull Calendar hoy) {
        Calendar dia = dia(fecha);
        if (dia == null) return Cuando.FECHA;
        Calendar h = (Calendar) hoy.clone();
        h.set(Calendar.HOUR_OF_DAY, 12);
        h.set(Calendar.MINUTE, 0);
        h.set(Calendar.SECOND, 0);
        h.set(Calendar.MILLISECOND, 0);
        long dias = Math.round((h.getTimeInMillis() - dia.getTimeInMillis()) / 86_400_000.0);
        if (dias == 0) return Cuando.HOY;
        if (dias == 1) return Cuando.AYER;
        if (dias >= 2 && dias <= 6) return Cuando.SEMANA;
        return Cuando.FECHA;
    }

    /** El día de una fecha yyyy-MM-dd, a mediodía (para que el horario de verano no mueva la cuenta). */
    @Nullable
    public static Calendar dia(@Nullable String fecha) {
        if (fecha == null || fecha.length() < 10) return null;
        try {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(fecha.substring(0, 4)), Integer.parseInt(fecha.substring(5, 7)) - 1,
                    Integer.parseInt(fecha.substring(8, 10)), 12, 0, 0);
            return c;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** El día anterior a una fecha yyyy-MM-dd, igual escrito. */
    @Nullable
    public static String diaAnterior(@Nullable String fecha) {
        Calendar c = dia(fecha);
        if (c == null) return null;
        c.add(Calendar.DAY_OF_MONTH, -1);
        return String.format(java.util.Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * La comida del mismo tipo del día anterior, para «¿Copiar la de ayer?».
     *
     * @param recientes lo que dio GET /comidas/recientes para esa comida.
     * @param tipo      la comida (DESAYUNO…CENA).
     * @param fecha     su día (yyyy-MM-dd).
     * @return esa comida, si tuvo algo; si no, null.
     */
    @Nullable
    public static ComidaReciente deAyer(@Nullable List<ComidaReciente> recientes, @NonNull String tipo,
                                        @Nullable String fecha) {
        String ayer = diaAnterior(fecha);
        if (recientes == null || ayer == null) return null;
        for (ComidaReciente r : recientes) {
            if (tipo.equals(r.getTipoComida()) && ayer.equals(r.getFecha()) && !r.getLineas().isEmpty()) return r;
        }
        return null;
    }

    /** Los nombres de sus alimentos, en su orden: «Pan integral, Pechuga de pavo». */
    @NonNull
    public static String nombres(@NonNull List<AlimentoComida> lineas) {
        StringBuilder sb = new StringBuilder();
        for (AlimentoComida l : lineas) {
            String n = l.getNombreAlimento();
            if (n == null || n.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(n.trim());
        }
        return sb.toString();
    }

    // ── Lo descartado con la ✗ ──────────────────────────────────────────────

    @NonNull
    public static String claveDescarte(@NonNull String fecha, @NonNull String tipo) {
        return fecha + "|" + tipo;
    }

    /**
     * Los descartes con uno más; si pasan de MAXIMO_DESCARTES, se van los de días más viejos.
     */
    @NonNull
    public static Set<String> descartar(@Nullable Set<String> antes, @NonNull String fecha, @NonNull String tipo) {
        List<String> todos = new ArrayList<>(antes != null ? antes : Collections.emptySet());
        String nuevo = claveDescarte(fecha, tipo);
        if (!todos.contains(nuevo)) todos.add(nuevo);
        // yyyy-MM-dd ordena como fecha: los más nuevos al final.
        Collections.sort(todos);
        int sobran = todos.size() - MAXIMO_DESCARTES;
        return new HashSet<>(sobran > 0 ? todos.subList(sobran, todos.size()) : todos);
    }
}
