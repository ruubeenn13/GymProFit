package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.plato.IngredienteElegido;

// ============================================================
// IngredientesPlato — los ingredientes de un plato mientras se crea o se edita (lote 1.6.6)
//
// La pantalla del plato y Añadir en modo ingredientes comparten esto: el «+» de una fila
// añade el alimento al plato (no a una comida) y el ✓ lo quita; la ficha lo añade o le
// cambia la cantidad; deslizarlo en la pantalla del plato lo quita y «Deshacer» lo
// devuelve a su sitio. Un alimento está una vez, en el orden en que se añadió, y nada
// va a la API hasta guardar el plato. Sin vistas, para probarlo.
// ============================================================
public final class IngredientesPlato {

    private final List<IngredienteElegido> lista = new ArrayList<>();

    public IngredientesPlato() {
    }

    public IngredientesPlato(@Nullable List<IngredienteElegido> desde) {
        if (desde != null) for (IngredienteElegido i : desde) poner(i);
    }

    @NonNull
    public List<IngredienteElegido> lista() {
        return Collections.unmodifiableList(lista);
    }

    public int cuantos() {
        return lista.size();
    }

    public boolean tiene(@NonNull Alimento a) {
        return indice(IngredienteElegido.clave(a)) >= 0;
    }

    /** Lo que se eligió de ese alimento, o null si no está. */
    @Nullable
    public IngredienteElegido de(@NonNull Alimento a) {
        int i = indice(IngredienteElegido.clave(a));
        return i >= 0 ? lista.get(i) : null;
    }

    /** ¿Caben más? (La API admite 50.) */
    public boolean lleno() {
        return lista.size() >= CuentaPlato.INGREDIENTES_MAX;
    }

    /**
     * El «+» o el ✓ de una fila: si no está, entra con la cantidad del «+»; si está, sale.
     *
     * @return true si queda dentro.
     */
    public boolean alternar(@NonNull Alimento a, @NonNull CantidadFicha cantidad) {
        int i = indice(IngredienteElegido.clave(a));
        if (i >= 0) {
            lista.remove(i);
            return false;
        }
        if (lleno()) return false;
        lista.add(IngredienteElegido.de(a, cantidad));
        return true;
    }

    /** Lo que vuelve de la ficha: entra al final o, si ya estaba, cambia su cantidad en su sitio. */
    public void poner(@NonNull IngredienteElegido nuevo) {
        int i = indice(nuevo.clave());
        if (i >= 0) lista.set(i, nuevo);
        else if (!lleno()) lista.add(nuevo);
    }

    /** Quita el de esa posición y lo devuelve, para «Deshacer». */
    @NonNull
    public IngredienteElegido quitar(int posicion) {
        return lista.remove(posicion);
    }

    /** «Deshacer»: vuelve a su sitio (o al final, si la lista ha cambiado de tamaño). */
    public void devolver(int posicion, @NonNull IngredienteElegido i) {
        if (indice(i.clave()) >= 0) return;
        lista.add(Math.max(0, Math.min(posicion, lista.size())), i);
    }

    /** La suma de los ingredientes, cada uno como se apuntaría (CuentaPlato). */
    @NonNull
    public CuentaPlato.Cifras total() {
        CuentaPlato.Cifras t = CuentaPlato.Cifras.CERO;
        for (IngredienteElegido i : lista) t = t.mas(CuentaPlato.linea(i.getAlimento(), i.getGramos()));
        return t;
    }

    private int indice(@NonNull String clave) {
        for (int i = 0; i < lista.size(); i++) if (lista.get(i).clave().equals(clave)) return i;
        return -1;
    }
}
