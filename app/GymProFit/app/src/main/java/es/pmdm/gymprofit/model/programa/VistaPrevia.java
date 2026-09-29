package es.pmdm.gymprofit.model.programa;

import java.util.List;

import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// VistaPrevia — cómo quedaría un programa al seguirlo con unos minutos (GP-074).
// La calcula la API con las reglas del catálogo; la app no aplica ninguna.
// ============================================================
public class VistaPrevia {

    /** Ajustes del perfil que puede traer: series de avanzado y «Ganar fuerza». */
    public static final String AJUSTE_AVANZADO = "AVANZADO";
    public static final String AJUSTE_FUERZA = "FUERZA";

    private int minutos;
    private List<String> ajustes;
    private List<Rutina> rutinas;

    public int getMinutos() { return minutos; }
    public List<String> getAjustes() { return ajustes; }
    public List<Rutina> getRutinas() { return rutinas; }

    /** Una rutina como quedaría. */
    public static class Rutina {
        private String codigo;
        private String nombre;
        private int duracionMinutos;
        private int duracionPlantilla;
        private List<RutinaEjercicio> ejercicios;
        private List<String> quitados;
        private Integer seriesBasicos;

        public Rutina() { }

        public Rutina(String nombre, int duracionMinutos, List<String> quitados, Integer seriesBasicos) {
            this.nombre = nombre;
            this.duracionMinutos = duracionMinutos;
            this.quitados = quitados;
            this.seriesBasicos = seriesBasicos;
        }

        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public int getDuracionMinutos() { return duracionMinutos; }
        public int getDuracionPlantilla() { return duracionPlantilla; }
        public List<RutinaEjercicio> getEjercicios() { return ejercicios; }
        public List<String> getQuitados() { return quitados; }
        public Integer getSeriesBasicos() { return seriesBasicos; }
    }
}
