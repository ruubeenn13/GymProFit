package es.pmdm.gymprofit.model.sesion;

public class SesionEntrenamiento {

    private int id;
    private int usuarioId;
    private int rutinaId;
    private String fecha;
    private int duracionMinutos;
    private int caloriasQuemadas;
    private String notas;

    public SesionEntrenamiento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public int getRutinaId() { return rutinaId; }
    public void setRutinaId(int rutinaId) { this.rutinaId = rutinaId; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public int getCaloriasQuemadas() { return caloriasQuemadas; }
    public void setCaloriasQuemadas(int caloriasQuemadas) { this.caloriasQuemadas = caloriasQuemadas; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
