package es.pmdm.gymprofit.model.comida;

// ============================================================
// AlimentoComida — modelo POJO de la relación alimento-comida
// Representa un alimento concreto añadido a una comida, con la
// cantidad en gramos y los totales nutricionales ya calculados
// para esa cantidad (calorías y macros totales de la porción).
// ============================================================
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
    // Id del usuario propietario del alimento si es personalizado; null si es del catálogo global
    private Integer usuarioIdAlimento;
    // Lote 1.6.1: la ración con la que se eligió, si se eligió por raciones («2 rebanadas»).
    private Integer racionId;
    private String racionNombre;
    private Double racionGramos;
    private Double raciones;
    // Lote 1.6.2 (GP-172): la unidad de esa ración, en singular y en plural, en el idioma de
    // la petición; null si va por gramos o la ración no tiene unidad («Media taza»).
    private String racionUnidad;
    private String racionUnidadPlural;
    // Lote 1.6.6: si el alimento es un plato propio, y si la ración es la de un plato sin
    // peso hecho (pesa 100 g por dentro: sus gramos no se enseñan).
    private Boolean alimentoEsPlato;
    private Boolean racionSinPeso;

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

    public Integer getRacionId() { return racionId; }
    public String getRacionNombre() { return racionNombre; }
    public Double getRacionGramos() { return racionGramos; }
    public Double getRaciones() { return raciones; }
    public String getRacionUnidad() { return racionUnidad; }
    public String getRacionUnidadPlural() { return racionUnidadPlural; }
    public boolean isAlimentoEsPlato() { return Boolean.TRUE.equals(alimentoEsPlato); }
    public boolean isRacionSinPeso() { return Boolean.TRUE.equals(racionSinPeso); }
    public void setAlimentoEsPlato(boolean v) { this.alimentoEsPlato = v; }
    public void setRacion(Integer racionId, String nombre, Double gramos, Double raciones, String unidad,
                          String unidadPlural, boolean sinPeso) {
        this.racionId = racionId;
        this.racionNombre = nombre;
        this.racionGramos = gramos;
        this.raciones = raciones;
        this.racionUnidad = unidad;
        this.racionUnidadPlural = unidadPlural;
        this.racionSinPeso = sinPeso;
    }
}
