package es.pmdm.gymprofit.model.programa;

import java.util.List;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// RutinaConEjercicios — una rutina con sus ejercicios en una sola respuesta (GP-074).
// Una plantilla del catálogo (con código) o una copia del usuario (con plantillaCodigo).
// ============================================================
public class RutinaConEjercicios extends Rutina {
    private String codigo;
    private List<RutinaEjercicio> ejercicios;

    public String getCodigo() { return codigo; }
    public List<RutinaEjercicio> getEjercicios() { return ejercicios; }
    public void setEjercicios(List<RutinaEjercicio> ejercicios) { this.ejercicios = ejercicios; }
}
