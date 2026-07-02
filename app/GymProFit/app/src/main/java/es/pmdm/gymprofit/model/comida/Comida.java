package es.pmdm.gymprofit.model.comida;

// ============================================================
// Comida — modelo de datos de una comida registrada por el usuario.
// Representa un registro (desayuno, almuerzo, cena, etc.) con los totales
// nutricionales agregados de los alimentos que la componen. Se usa para
// mapear las respuestas JSON del endpoint de comidas de la API.
// ============================================================
public class Comida {

    // Identificador único de la comida en la BD.
    private int id;
    // Tipo de comida (desayuno, almuerzo, cena, snack, etc.).
    private String tipoComida;
    // Fecha en la que se registró la comida.
    private String fecha;
    // Suma de calorías de todos los alimentos que forman la comida.
    private int totalCalorias;
    // Suma de proteínas (g) de la comida.
    private double totalProteinas;
    // Suma de carbohidratos (g) de la comida.
    private double totalCarbohidratos;
    // Suma de grasas (g) de la comida.
    private double totalGrasas;
    // Id del usuario propietario de la comida.
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
