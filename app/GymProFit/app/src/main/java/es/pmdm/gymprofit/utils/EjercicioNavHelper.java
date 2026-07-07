package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.content.Intent;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.ui.activities.DetalleEjercicioActivity;

// ============================================================
// EjercicioNavHelper — utilidad estática para navegar al detalle de un ejercicio.
// Centraliza la construcción del Intent hacia DetalleEjercicioActivity con todos
// los extras necesarios, evitando duplicar ese código en varias Activities.
// ============================================================
public final class EjercicioNavHelper {

    // Clase de utilidad: constructor privado, no instanciable.
    private EjercicioNavHelper() {}

    // Abre DetalleEjercicioActivity pasando los datos del ejercicio como extras del Intent.
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
        intent.putExtra("imagenUrl",       e.getImagenUrl());
        intent.putExtra("imagenUrl2",      e.getImagenUrl2());
        context.startActivity(intent);
    }

    // Sobrecarga que extrae el Ejercicio de un EjercicioSeleccionado (usado en rutinas).
    public static void abrir(Context context, EjercicioSeleccionado sel) {
        abrir(context, sel.getEjercicio());
    }
}
