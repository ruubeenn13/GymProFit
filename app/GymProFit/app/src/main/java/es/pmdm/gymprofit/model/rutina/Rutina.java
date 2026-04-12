package es.pmdm.gymprofit.model.rutina;

public class Rutina {

    private String nombre;
    private String nivel;
    private String descripcion;
    private int numEjercicios;
    private int duracionMinutos;
    private int caloriasAproximadas;

    public Rutina(String nombre, String nivel, String descripcion, int numEjercicios, int duracionMinutos, int caloriasAproximadas) {
        this.nombre = nombre;
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.numEjercicios = numEjercicios;
        this.duracionMinutos = duracionMinutos;
        this.caloriasAproximadas = caloriasAproximadas;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNivel() {
        return nivel;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getNumEjercicios() {
        return numEjercicios;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getCaloriasAproximadas() {
        return caloriasAproximadas;
    }
}
