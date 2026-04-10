package es.pmdm.gymprofit.model.ejercicio;

public class Ejercicio {

    private String nombre;
    private String grupoMuscular;
    private String dificultad;
    private String descripcion;
    private int calorias;
    private int iconoRes;

    public Ejercicio(String nombre, String grupoMuscular, String dificultad, String descripcion, int calorias, int iconoRes) {
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
        this.dificultad = dificultad;
        this.descripcion = descripcion;
        this.calorias = calorias;
        this.iconoRes = iconoRes;
    }

    public String getNombre() {
        return nombre;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public String getDificultad() {
        return dificultad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCalorias() {
        return calorias;
    }

    public int getIconoRes() {
        return iconoRes;
    }
}
