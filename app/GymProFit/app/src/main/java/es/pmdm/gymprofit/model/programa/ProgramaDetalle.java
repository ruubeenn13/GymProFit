package es.pmdm.gymprofit.model.programa;

import java.util.List;

// ============================================================
// ProgramaDetalle — un programa con cada rutina distinta y sus ejercicios (GP-074).
// ============================================================
public class ProgramaDetalle extends Programa {
    private List<RutinaConEjercicios> rutinas;

    public List<RutinaConEjercicios> getRutinas() { return rutinas; }
}
