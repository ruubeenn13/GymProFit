package es.pmdm.gymprofit.model.ejercicio;

// ============================================================
// EjercicioMasHecho — un elemento de GET /ejercicios-realizados/mas-hechos (GP-216,
// Biblioteca): el id del ejercicio del catálogo y en cuántas sesiones terminadas del usuario
// ha salido. El nombre, la foto y la marca los pone la pantalla con el catálogo y /records.
// ============================================================
public class EjercicioMasHecho {
    private int ejercicioId;
    private long veces;

    public int getEjercicioId() { return ejercicioId; }
    public long getVeces() { return veces; }
}
