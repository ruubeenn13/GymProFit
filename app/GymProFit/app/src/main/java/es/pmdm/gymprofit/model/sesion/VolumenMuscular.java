package es.pmdm.gymprofit.model.sesion;

// ============================================================
// VolumenMuscular — series que ha recibido un músculo en los últimos días.
// Es lo que la API devuelve para encender la silueta de la pantalla de inicio:
// una entrada por músculo tocado, con la clave ya normalizada por el servidor
// ("pecho", "cuadriceps", "dorsales"…). Los músculos sin trabajar no vienen.
// ============================================================
public class VolumenMuscular {

    private String musculo;
    private Integer series;

    public String getMusculo() { return musculo; }
    public void setMusculo(String musculo) { this.musculo = musculo; }

    public int getSeries() { return series == null ? 0 : series; }
    public void setSeries(Integer series) { this.series = series; }
}
