package es.pmdm.gymprofit.model.plato;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.utils.CantidadFicha;

// ============================================================
// IngredienteElegido — un ingrediente en la pantalla del plato, antes de guardarlo (1.6.6)
// El alimento entero (para sus valores, su icono y su ficha) y su cantidad como la deja
// la ficha: la ración (por id o, un producto aún sin materializar, por su posición) y
// cuántas, o los gramos. Viaja en JSON entre la pantalla del plato, Añadir y la ficha.
// ============================================================
public class IngredienteElegido {

    private Alimento alimento;
    @Nullable private Integer racionId;
    private int racionIndice = -1;
    @Nullable private Double raciones;
    private double gramos;

    public IngredienteElegido() {
    }

    /** Con la cantidad elegida en la ficha, o la que añade el «+». */
    @NonNull
    public static IngredienteElegido de(@NonNull Alimento a, @NonNull CantidadFicha c) {
        IngredienteElegido i = new IngredienteElegido();
        i.alimento = a;
        CantidadFicha.Unidad u = c.unidad();
        if (!u.esGramos()) {
            i.racionId = u.racionId;
            i.racionIndice = u.indice;
            i.raciones = c.valor();
        }
        i.gramos = c.gramos();
        return i;
    }

    /** Uno que ya está en el plato, como lo devuelve la API. */
    @NonNull
    public static IngredienteElegido de(@NonNull Ingrediente ing) {
        IngredienteElegido i = new IngredienteElegido();
        i.alimento = ing.getAlimento();
        i.racionId = ing.getRacionId();
        i.raciones = ing.getRacionId() != null ? ing.getRaciones() : null;
        if (i.racionId != null) {
            for (int k = 0; k < i.alimento.getRaciones().size(); k++) {
                if (i.racionId.equals(i.alimento.getRaciones().get(k).getId())) i.racionIndice = k;
            }
        }
        i.gramos = ing.getCantidadGramos();
        return i;
    }

    @NonNull public Alimento getAlimento() { return alimento; }

    @Nullable public Integer getRacionId() { return racionId; }

    public int getRacionIndice() { return racionIndice; }

    @Nullable public Double getRaciones() { return raciones; }

    public double getGramos() { return gramos; }

    /** Su cantidad, para la ficha y para escribirla. */
    @NonNull
    public CantidadFicha cantidad() {
        return CantidadFicha.deLinea(alimento, racionId, raciones, gramos);
    }

    /** El mismo alimento, que es lo que no puede repetirse en la lista. */
    @NonNull
    public String clave() {
        return clave(alimento);
    }

    @NonNull
    public static String clave(@NonNull Alimento a) {
        return a.getId() > 0 ? "a:" + a.getId() : "c:" + a.getBarcode();
    }
}
