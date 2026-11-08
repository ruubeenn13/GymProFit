package es.pmdm.gymprofit.model.sesion;

// ============================================================
// VolumenSemanal — los kilos movidos y las series hechas en una semana (GP-216 y GP-249).
// Una entrada por semana pedida a GET /sesiones/usuario/{id}/volumen-semanal, también las
// de cero, en orden. El inicio es el lunes a las 00:00 que mandó la app. Las series llegan
// desde GP-249; una API anterior no las manda y cuentan como cero.
// ============================================================
public class VolumenSemanal {

    private String inicio;
    private Double volumenKg;
    private Integer sesiones;
    private Integer series;

    public String getInicio() { return inicio; }
    public void setInicio(String inicio) { this.inicio = inicio; }

    public double getVolumenKg() { return volumenKg == null ? 0 : volumenKg; }
    public void setVolumenKg(Double volumenKg) { this.volumenKg = volumenKg; }

    public int getSesiones() { return sesiones == null ? 0 : sesiones; }
    public void setSesiones(Integer sesiones) { this.sesiones = sesiones; }

    public int getSeries() { return series == null ? 0 : series; }
    public void setSeries(Integer series) { this.series = series; }
}
