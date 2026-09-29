package es.pmdm.gymprofit.model.programa;

// ============================================================
// DiaPrograma — un día de la semana de un programa, en orden (GP-074).
// ============================================================
public class DiaPrograma {
    private int posicion;
    private String rutinaCodigo;
    private String rutinaNombre;
    private int duracionMinutos;

    public DiaPrograma() { }

    public DiaPrograma(int posicion, String rutinaCodigo, String rutinaNombre, int duracionMinutos) {
        this.posicion = posicion;
        this.rutinaCodigo = rutinaCodigo;
        this.rutinaNombre = rutinaNombre;
        this.duracionMinutos = duracionMinutos;
    }

    public int getPosicion() { return posicion; }
    public String getRutinaCodigo() { return rutinaCodigo; }
    public String getRutinaNombre() { return rutinaNombre; }
    public int getDuracionMinutos() { return duracionMinutos; }
}
