package es.pmdm.gymprofit.model.medicion;

// ============================================================
// MedicionCorporal — modelo de datos de una medición corporal del usuario.
// Registra peso, altura y perímetros corporales en una fecha concreta,
// usados para el seguimiento de progreso físico dentro de GymProFit.
// ============================================================
public class MedicionCorporal {

    // Identificador único de la medición.
    private int id;
    // Id del usuario al que pertenece la medición.
    private int usuarioId;
    // Fecha en la que se realizó la medición.
    private String fecha;
    // Peso corporal (kg).
    private double peso;
    // Altura (cm).
    private double altura;
    // Índice de masa corporal calculado.
    private double imc;
    // Porcentaje de grasa corporal.
    private double grasaCorporal;
    // Porcentaje/masa de masa muscular.
    private double masaMuscular;
    // Perímetro de cintura (cm).
    private double cintura;
    // Perímetro de pecho (cm).
    private double pecho;
    // Perímetro de brazos (cm).
    private double brazos;
    // Perímetro de piernas (cm).
    private double piernas;
    // Notas u observaciones adicionales de la medición.
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
