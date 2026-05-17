package es.pmdm.gymprofit.model.medicion;

public class MedicionCorporal {

    private int id;
    private int usuarioId;
    private String fecha;
    private double peso;
    private double altura;
    private double imc;
    private double grasaCorporal;
    private double masaMuscular;
    private double cintura;
    private double pecho;
    private double brazos;
    private double piernas;
    private String notas;

    public MedicionCorporal() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public double getAltura() { return altura; }
    public void setAltura(double altura) { this.altura = altura; }

    public double getImc() { return imc; }
    public void setImc(double imc) { this.imc = imc; }

    public double getGrasaCorporal() { return grasaCorporal; }
    public void setGrasaCorporal(double grasaCorporal) { this.grasaCorporal = grasaCorporal; }

    public double getMasaMuscular() { return masaMuscular; }
    public void setMasaMuscular(double masaMuscular) { this.masaMuscular = masaMuscular; }

    public double getCintura() { return cintura; }
    public void setCintura(double cintura) { this.cintura = cintura; }

    public double getPecho() { return pecho; }
    public void setPecho(double pecho) { this.pecho = pecho; }

    public double getBrazos() { return brazos; }
    public void setBrazos(double brazos) { this.brazos = brazos; }

    public double getPiernas() { return piernas; }
    public void setPiernas(double piernas) { this.piernas = piernas; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
