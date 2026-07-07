package es.pmdm.gymprofit.model.alimento;

import com.google.gson.annotations.JsonAdapter;

import es.pmdm.gymprofit.network.BooleanNumericAdapter;

// ============================================================
// Alimento — modelo POJO de un alimento de la base de datos nutricional
// Representa un alimento con su información nutricional por porción
// (calorías, macros) para el sistema de seguimiento tipo MyFitnessPal.
// Se usa como modelo de datos en las respuestas Retrofit de la API.
// ============================================================
public class Alimento {

    private int id;
    private String nombre;
    private String categoria;
    private int calorias;
    private double proteinas;
    private double carbohidratos;
    private double grasas;
    // Id del usuario propietario si es un alimento personalizado; null si es del catálogo global
    private Integer usuarioId;
    // Código de barras (Open Food Facts). En resultados de búsqueda externa el
    // id llega null (Gson lo deja a 0) y este campo permite importar el producto
    private String barcode;
    // Marca/fabricante del producto (Open Food Facts)
    private String marca;
    // Indica si el alimento está activo (borrado lógico)
    // La búsqueda admin (AlimentoJooqDTO) envía "activo" como Byte 0/1; el adaptador lo tolera.
    @JsonAdapter(BooleanNumericAdapter.class)
    private boolean activo;

    public Alimento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public int getCalorias() { return calorias; }
    public void setCalorias(int calorias) { this.calorias = calorias; }

    public double getProteinas() { return proteinas; }
    public void setProteinas(double proteinas) { this.proteinas = proteinas; }

    public double getCarbohidratos() { return carbohidratos; }
    public void setCarbohidratos(double carbohidratos) { this.carbohidratos = carbohidratos; }

    public double getGrasas() { return grasas; }
    public void setGrasas(double grasas) { this.grasas = grasas; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    // true si es un resultado de la búsqueda externa (OFF) aún sin importar
    // a la BD local (la API envía id null → Gson lo deserializa como 0)
    public boolean esExterno() { return id == 0; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
