package es.pmdm.gymprofit.model.record;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// Records — respuesta de GET /records (GP-088).
//
// {@code records}: el récord vigente de cada ejercicio que tiene alguno, del más
// reciente al más antiguo. {@code recientes}: los batidos desde la fecha pedida
// (vacía si no se pidió ninguna).
// ============================================================
public class Records {

    private List<Record> records;
    private List<Record> recientes;

    public List<Record> getRecords() { return records != null ? records : new ArrayList<>(); }
    public List<Record> getRecientes() { return recientes != null ? recientes : new ArrayList<>(); }
}
