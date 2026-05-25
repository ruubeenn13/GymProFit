package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.content.Intent;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.ui.activities.DetalleEjercicioActivity;

public final class EjercicioNavHelper {

    private EjercicioNavHelper() {}

    public static void abrir(Context context, Ejercicio e) {
        Intent intent = new Intent(context, DetalleEjercicioActivity.class);
        intent.putExtra("id",              e.getId());
        intent.putExtra("nombre",          e.getNombre());
        intent.putExtra("descripcion",     e.getDescripcion());
        intent.putExtra("instrucciones",   e.getInstrucciones());
        intent.putExtra("grupoMuscular",   e.getGrupoMuscular());
        intent.putExtra("dificultad",      e.getDificultad());
        intent.putExtra("calorias",        e.getCalorias());
        intent.putExtra("equipoNecesario", e.getEquipoNecesario());
        context.startActivity(intent);
    }

    public static void abrir(Context context, EjercicioSeleccionado sel) {
        abrir(context, sel.getEjercicio());
    }
}
