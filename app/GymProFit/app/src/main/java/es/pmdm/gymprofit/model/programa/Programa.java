package es.pmdm.gymprofit.model.programa;

import java.util.List;

// ============================================================
// Programa — un programa del catálogo (GET /programas, GP-074).
// Varios días; cada día, una rutina. Los textos llegan en el idioma de la petición.
// ============================================================
public class Programa {

    /** Los tres equipamientos del alta, tal como los manda la API. */
    public static final String GIMNASIO = "GIMNASIO";
    public static final String MANCUERNAS = "MANCUERNAS";
    public static final String PESO_CORPORAL = "PESO_CORPORAL";

    private String codigo;
    private String nombre;
    private String descripcion;
    private String nivel;
    private String equipamiento;
    private int diasMin;
    private int diasMax;
    private List<DiaPrograma> semana;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getEquipamiento() { return equipamiento; }
    public void setEquipamiento(String equipamiento) { this.equipamiento = equipamiento; }
    public int getDiasMin() { return diasMin; }
    public void setDiasMin(int diasMin) { this.diasMin = diasMin; }
    public int getDiasMax() { return diasMax; }
    public void setDiasMax(int diasMax) { this.diasMax = diasMax; }
    public List<DiaPrograma> getSemana() { return semana; }
    public void setSemana(List<DiaPrograma> semana) { this.semana = semana; }

    /**
     * Minutos de la sesión más larga de su semana, para la lista («40–55 min»).
     *
     * @return {mínimo, máximo}, o null si la semana no trae duraciones.
     */
    public int[] minutos() {
        if (semana == null || semana.isEmpty()) return null;
        int min = Integer.MAX_VALUE, max = 0;
        for (DiaPrograma d : semana) {
            if (d.getDuracionMinutos() <= 0) continue;
            min = Math.min(min, d.getDuracionMinutos());
            max = Math.max(max, d.getDuracionMinutos());
        }
        return max == 0 ? null : new int[]{min, max};
    }
}
