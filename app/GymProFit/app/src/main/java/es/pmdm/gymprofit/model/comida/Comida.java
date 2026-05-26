package es.pmdm.gymprofit.model.comida;

public class Comida {

    private int id;
    private String tipoComida;
    private String fecha;
    private int totalCalorias;
    private double totalProteinas;
    private double totalCarbohidratos;
    private double totalGrasas;
    private int usuarioId;

    public Comida() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipoComida() { return tipoComida; }
    public void setTipoComida(String tipoComida) { this.tipoComida = tipoComida; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getTotalCalorias() { return totalCalorias; }
    public void setTotalCalorias(int totalCalorias) { this.totalCalorias = totalCalorias; }

    public double getTotalProteinas() { return totalProteinas; }
    public void setTotalProteinas(double totalProteinas) { this.totalProteinas = totalProteinas; }

    public double getTotalCarbohidratos() { return totalCarbohidratos; }
    public void setTotalCarbohidratos(double totalCarbohidratos) { this.totalCarbohidratos = totalCarbohidratos; }

    public double getTotalGrasas() { return totalGrasas; }
    public void setTotalGrasas(double totalGrasas) { this.totalGrasas = totalGrasas; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
}
