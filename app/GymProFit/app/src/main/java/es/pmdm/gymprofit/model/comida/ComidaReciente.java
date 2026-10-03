package es.pmdm.gymprofit.model.comida;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// ComidaReciente — una comida que se puede copiar (lote 1.6.4, GET /comidas/recientes)
// «Merienda de ayer»: su tipo, su día (yyyy-MM-dd), las kcal de lo que se copiaría y sus
// líneas, como las de una comida. La API ya deja fuera lo que no se puede copiar.
// ============================================================
public class ComidaReciente {

    private int id;
    private String tipoComida;
    private String fecha;
    private int kcal;
    private List<AlimentoComida> lineas;

    public ComidaReciente() {
    }

    public ComidaReciente(int id, String tipoComida, String fecha, int kcal, List<AlimentoComida> lineas) {
        this.id = id;
        this.tipoComida = tipoComida;
        this.fecha = fecha;
        this.kcal = kcal;
        this.lineas = lineas;
    }

    public int getId() { return id; }

    public String getTipoComida() { return tipoComida; }

    public String getFecha() { return fecha; }

    public int getKcal() { return kcal; }

    @NonNull
    public List<AlimentoComida> getLineas() { return lineas != null ? lineas : new ArrayList<>(); }
}
