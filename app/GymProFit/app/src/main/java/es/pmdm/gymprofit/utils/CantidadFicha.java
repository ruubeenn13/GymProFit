package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Racion;

// ============================================================
// CantidadFicha — la cantidad que se elige en la ficha del alimento (lote 1.6.1)
//
// Las unidades son las raciones del alimento, en su orden, y al final los gramos. Por
// defecto, la primera ración (el envase de un producto, si lo tiene) por una, o 100 g si
// no tiene raciones. − y + van de uno en uno en raciones y de 25 en 25 en gramos, sin
// bajar del paso; la cifra también se escribe. Cambiar de unidad conserva los gramos:
// a gramos, tal cual; a raciones, a la media ración más cercana (y nunca cero).
// Sin vistas: la ficha la pinta y AnadirAlimento la manda con estos datos.
// ============================================================
public final class CantidadFicha {

    public static final double PASO_GRAMOS = 25;
    public static final double PASO_RACIONES = 1;
    public static final double MAX_GRAMOS = 5000;
    public static final double MAX_RACIONES = 99;
    public static final double GRAMOS_POR_DEFECTO = 100;

    /** Una unidad de la cantidad: una ración del alimento, o los gramos. */
    public static final class Unidad {
        /** Id de la ración; null en gramos o en una ración de un producto sin materializar. */
        @Nullable public final Integer racionId;
        /** Posición de la ración en la lista del alimento; -1 en gramos. */
        public final int indice;
        /** Nombre de la ración («1 envase»); null en gramos. */
        @Nullable public final String nombre;
        /** Gramos de una ración; 1 en gramos. */
        public final double gramos;

        Unidad(@Nullable Integer racionId, int indice, @Nullable String nombre, double gramos) {
            this.racionId = racionId;
            this.indice = indice;
            this.nombre = nombre;
            this.gramos = gramos;
        }

        public boolean esGramos() {
            return indice < 0;
        }
    }

    private final List<Unidad> unidades;
    private int elegida;
    private double valor;

    private CantidadFicha(List<Unidad> unidades, int elegida, double valor) {
        this.unidades = unidades;
        this.elegida = elegida;
        this.valor = valor;
    }

    /** Para añadir: la primera ración por una, o 100 g. */
    @NonNull
    public static CantidadFicha nueva(@Nullable List<Racion> raciones) {
        List<Unidad> u = unidadesDe(raciones);
        return u.size() > 1 ? new CantidadFicha(u, 0, 1) : new CantidadFicha(u, 0, GRAMOS_POR_DEFECTO);
    }

    /**
     * Para editar una línea: con su ración y cuántas si la tiene (y la ración sigue
     * existiendo); si no, en sus gramos.
     */
    @NonNull
    public static CantidadFicha deLinea(@Nullable List<Racion> raciones, @Nullable Integer racionId,
                                        @Nullable Double cuantas, double gramos) {
        List<Unidad> u = unidadesDe(raciones);
        if (racionId != null && cuantas != null && cuantas > 0) {
            for (int i = 0; i < u.size() - 1; i++) {
                if (racionId.equals(u.get(i).racionId)) return new CantidadFicha(u, i, cuantas);
            }
        }
        return new CantidadFicha(u, u.size() - 1, gramos);
    }

    private static List<Unidad> unidadesDe(@Nullable List<Racion> raciones) {
        List<Unidad> u = new ArrayList<>();
        if (raciones != null) {
            for (int i = 0; i < raciones.size(); i++) {
                Racion r = raciones.get(i);
                if (r.getGramos() > 0) u.add(new Unidad(r.getId(), i, r.getNombre(), r.getGramos()));
            }
        }
        u.add(new Unidad(null, -1, null, 1));
        return u;
    }

    @NonNull
    public List<Unidad> unidades() {
        return Collections.unmodifiableList(unidades);
    }

    /** Posición de la unidad elegida en {@link #unidades()}. */
    public int elegida() {
        return elegida;
    }

    @NonNull
    public Unidad unidad() {
        return unidades.get(elegida);
    }

    /** Cuántas raciones, o cuántos gramos. */
    public double valor() {
        return valor;
    }

    /** Los gramos de la cantidad, que es lo que cuenta. */
    public double gramos() {
        return valor * unidad().gramos;
    }

    private double paso() {
        return unidad().esGramos() ? PASO_GRAMOS : PASO_RACIONES;
    }

    private double maximo() {
        return unidad().esGramos() ? MAX_GRAMOS : MAX_RACIONES;
    }

    public void mas() {
        valor = Math.min(maximo(), valor + paso());
    }

    /** Baja un paso, sin bajar del paso; si ya está en él o por debajo, no hace nada. */
    public void menos() {
        if (!puedeMenos()) return;
        valor = Math.max(paso(), valor - paso());
    }

    public boolean puedeMenos() {
        return valor > paso();
    }

    /**
     * Pone la cifra escrita.
     *
     * @return false si no vale (cero, negativa o por encima del máximo): no cambia nada.
     */
    public boolean escribir(double nuevo) {
        if (Double.isNaN(nuevo) || nuevo <= 0 || nuevo > maximo()) return false;
        valor = nuevo;
        return true;
    }

    /** Cambia de unidad conservando los gramos. */
    public void elegir(int indice) {
        if (indice < 0 || indice >= unidades.size() || indice == elegida) return;
        double gramos = gramos();
        elegida = indice;
        if (unidad().esGramos()) {
            valor = Math.min(MAX_GRAMOS, Math.max(1, Math.round(gramos)));
        } else {
            double medias = Math.round(gramos / unidad().gramos * 2) / 2.0;
            valor = Math.min(MAX_RACIONES, Math.max(0.5, medias));
        }
    }

    /**
     * El nombre de una ración como unidad: sin el «1 » con que llega («1 envase» →
     * «envase») y en minúscula, para «2 × envase (400 g)». Las raciones vienen en
     * singular y no se pluralizan: «2 × rebanada» se entiende y no se equivoca nunca.
     */
    @NonNull
    public static String nombreUnidad(@NonNull String nombre) {
        String t = nombre.trim();
        if (t.startsWith("1 ")) t = t.substring(2).trim();
        if (t.isEmpty()) return nombre;
        return Character.toLowerCase(t.charAt(0)) + t.substring(1);
    }
}
