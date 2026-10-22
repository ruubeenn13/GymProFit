package es.pmdm.gymprofit.model.comida;

// ============================================================
// AlimentoApuntado — un alimento y las veces que se apuntó en una semana
// (GET /diario/mas-apuntados). Es «Lo que más has comido» de «Tu semana de nutrición».
// ============================================================
public class AlimentoApuntado {

    private int alimentoId;
    private String nombre;
    private int veces;

    public int getAlimentoId() { return alimentoId; }
    public String getNombre() { return nombre; }
    public int getVeces() { return veces; }

    public static AlimentoApuntado de(int alimentoId, String nombre, int veces) {
        AlimentoApuntado a = new AlimentoApuntado();
        a.alimentoId = alimentoId;
        a.nombre = nombre;
        a.veces = veces;
        return a;
    }
}
