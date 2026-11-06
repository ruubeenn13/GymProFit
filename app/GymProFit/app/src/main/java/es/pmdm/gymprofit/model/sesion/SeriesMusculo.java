package es.pmdm.gymprofit.model.sesion;

import androidx.annotation.Nullable;

// ============================================================
// SeriesMusculo — cuántas series cayeron en un músculo (GP-232).
// La clave es la de Musculos.normalizar, la misma de la silueta y los récords; Zonas la pasa
// a zona y FrasesTarjeta a frase. En una sesión son las series hechas; en una rutina, las
// pautadas. Llega de GET /sesiones/historial y de las rutinas activas.
// ============================================================
public class SeriesMusculo {

    private String musculo;
    private int series;

    public SeriesMusculo() { }

    public SeriesMusculo(@Nullable String musculo, int series) {
        this.musculo = musculo;
        this.series = series;
    }

    @Nullable
    public String getMusculo() { return musculo; }

    public int getSeries() { return series; }
}
