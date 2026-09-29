package es.pmdm.gymprofit.model.programa;

import java.util.List;

// ============================================================
// ProgramaQueSigue — GET /programas/seguido (GP-074, lote 1.2.1).
// El programa que sigue el usuario, su ciclo con sus rutinas y la que toca. rutinas
// trae las activas en el orden en que tocan desde hoy: la primera es la de hoy. Si ha
// borrado todas, posicionHoy es null y rutinas viene vacía.
// ============================================================
public class ProgramaQueSigue {
    private int id;
    private Programa programa;
    private int minutos;
    private int posicionInicial;
    private Integer posicionHoy;
    private List<DiaCiclo> ciclo;
    private List<RutinaConEjercicios> rutinas;

    public int getId() { return id; }
    public Programa getPrograma() { return programa; }
    public void setPrograma(Programa programa) { this.programa = programa; }
    public int getMinutos() { return minutos; }
    public void setMinutos(int minutos) { this.minutos = minutos; }
    public Integer getPosicionHoy() { return posicionHoy; }
    public void setPosicionHoy(Integer posicionHoy) { this.posicionHoy = posicionHoy; }
    public List<DiaCiclo> getCiclo() { return ciclo; }
    public void setCiclo(List<DiaCiclo> ciclo) { this.ciclo = ciclo; }
    public List<RutinaConEjercicios> getRutinas() { return rutinas; }
    public void setRutinas(List<RutinaConEjercicios> rutinas) { this.rutinas = rutinas; }

    /** Un día del ciclo. */
    public static class DiaCiclo {
        private int posicion;
        private String plantillaCodigo;
        private Integer rutinaId;
        private String rutinaNombre;
        private boolean activa;
        private boolean hecha;

        public DiaCiclo() { }

        public DiaCiclo(int posicion, String plantillaCodigo, Integer rutinaId, String rutinaNombre,
                        boolean activa, boolean hecha) {
            this.posicion = posicion;
            this.plantillaCodigo = plantillaCodigo;
            this.rutinaId = rutinaId;
            this.rutinaNombre = rutinaNombre;
            this.activa = activa;
            this.hecha = hecha;
        }

        public int getPosicion() { return posicion; }
        public String getPlantillaCodigo() { return plantillaCodigo; }
        public Integer getRutinaId() { return rutinaId; }
        public String getRutinaNombre() { return rutinaNombre; }
        public boolean isActiva() { return activa; }
        public boolean isHecha() { return hecha; }
    }
}
