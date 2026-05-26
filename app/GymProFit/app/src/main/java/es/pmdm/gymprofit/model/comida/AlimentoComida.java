package es.pmdm.gymprofit.model.comida;

public class AlimentoComida {

    private int id;
    private int comidaId;
    private int alimentoId;
    private String nombreAlimento;
    private String categoriaAlimento;
    private double cantidadGramos;
    private int caloriasTotales;
    private double proteinasTotales;
    private double carbohidratosTotales;
    private double grasasTotales;
    private Integer usuarioIdAlimento;

    public AlimentoComida() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getComidaId() { return comidaId; }
    public void setComidaId(int comidaId) { this.comidaId = comidaId; }

    public int getAlimentoId() { return alimentoId; }
    public void setAlimentoId(int alimentoId) { this.alimentoId = alimentoId; }

    public String getNombreAlimento() { return nombreAlimento; }
    public void setNombreAlimento(String nombreAlimento) { this.nombreAlimento = nombreAlimento; }

    public String getCategoriaAlimento() { return categoriaAlimento; }
    public void setCategoriaAlimento(String categoriaAlimento) { this.categoriaAlimento = categoriaAlimento; }

    public double getCantidadGramos() { return cantidadGramos; }
    public void setCantidadGramos(double cantidadGramos) { this.cantidadGramos = cantidadGramos; }

    public int getCaloriasTotales() { return caloriasTotales; }
    public void setCaloriasTotales(int caloriasTotales) { this.caloriasTotales = caloriasTotales; }

    public double getProteinasTotales() { return proteinasTotales; }
    public void setProteinasTotales(double proteinasTotales) { this.proteinasTotales = proteinasTotales; }

    public double getCarbohidratosTotales() { return carbohidratosTotales; }
    public void setCarbohidratosTotales(double carbohidratosTotales) { this.carbohidratosTotales = carbohidratosTotales; }

    public double getGrasasTotales() { return grasasTotales; }
    public void setGrasasTotales(double grasasTotales) { this.grasasTotales = grasasTotales; }

    public Integer getUsuarioIdAlimento() { return usuarioIdAlimento; }
    public void setUsuarioIdAlimento(Integer usuarioIdAlimento) { this.usuarioIdAlimento = usuarioIdAlimento; }
}
