package es.pmdm.gymprofit.model.record;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Locale;

// ============================================================
// Record — una marca de un ejercicio, tal como la calcula la API (GP-088).
//
// La API no guarda récords: los deduce de las series de las sesiones. Sirve igual
// para el récord vigente de un ejercicio, para uno recién batido al guardar una
// sesión (con la marca anterior) y para una primera marca, que no es un récord
// sino el punto de partida (sin marca anterior). Es Serializable porque viaja del
// registro de la sesión a su resumen dentro del Intent.
// ============================================================
public class Record implements Serializable {

    /** La marca se mide en kilos, con sus repeticiones. */
    public static final String TIPO_PESO = "PESO";
    /** Ejercicio sin peso: la marca son las repeticiones. */
    public static final String TIPO_REPETICIONES = "REPETICIONES";
    /** Ejercicio por tiempo, como la plancha: la marca son los segundos (GP-125). */
    public static final String TIPO_TIEMPO = "TIEMPO";

    private int ejercicioId;
    private String ejercicioNombre;
    private String ejercicioNombreEn;
    private String musculo;          // clave normalizada, la misma que usa la silueta
    private String tipo;
    private Double peso;
    private Integer repeticiones;
    private Double unoRmEstimado;
    private String fecha;            // ISO date-time
    private Integer sesionId;
    private Double pesoAnterior;
    private Integer repeticionesAnterior;
    // Por tiempo (GP-125): los segundos de la marca y los de la que superó.
    private Integer segundos;
    private Integer segundosAnterior;

    public int getEjercicioId() { return ejercicioId; }
    public String getMusculo() { return musculo; }
    public String getTipo() { return tipo; }
    public double getPeso() { return peso == null ? 0 : peso; }
    public int getRepeticiones() { return repeticiones == null ? 0 : repeticiones; }
    @Nullable public Double getUnoRmEstimado() { return unoRmEstimado; }
    public String getFecha() { return fecha; }
    @Nullable public Integer getSesionId() { return sesionId; }
    @Nullable public Double getPesoAnterior() { return pesoAnterior; }
    @Nullable public Integer getRepeticionesAnterior() { return repeticionesAnterior; }

    public int getSegundos() { return segundos == null ? 0 : segundos; }
    @Nullable public Integer getSegundosAnterior() { return segundosAnterior; }

    /**
     * @return {@code true} si la marca son kilos. Antes era «todo lo que no sea de
     * repeticiones», y una marca por tiempo salía como «0 kg» (GP-125).
     */
    public boolean esDePeso() { return TIPO_PESO.equals(tipo); }

    /** @return {@code true} si la marca son segundos (GP-125). */
    public boolean esDeTiempo() { return TIPO_TIEMPO.equals(tipo); }

    /** @return {@code true} si trae la marca que superó (un récord, no una primera marca). */
    public boolean tieneAnterior() {
        return esDeTiempo() ? segundosAnterior != null : pesoAnterior != null || repeticionesAnterior != null;
    }

    // Para construir casos por tiempo en los tests.
    public static Record deTiempo(int ejercicioId, String nombre, int segundos, @Nullable Integer anterior) {
        Record r = new Record(ejercicioId, nombre, null, null, TIPO_TIEMPO, null, 0, null);
        r.segundos = segundos;
        r.segundosAnterior = anterior;
        return r;
    }

    /**
     * Nombre del ejercicio en el idioma de la app.
     *
     * <p>El catálogo guarda el nombre en español y, cuando lo tiene, en inglés. Si la
     * app está en inglés y falta la traducción se enseña el español: mejor un nombre
     * en otro idioma que ninguno.
     *
     * @param idioma idioma de la interfaz
     */
    public String nombre(Locale idioma) {
        if ("en".equals(idioma.getLanguage()) && ejercicioNombreEn != null && !ejercicioNombreEn.isEmpty()) {
            return ejercicioNombreEn;
        }
        return ejercicioNombre;
    }

    // Para construir casos en los tests.
    public Record(int ejercicioId, String nombre, String nombreEn, String musculo, String tipo,
                  Double peso, Integer repeticiones, String fecha) {
        this.ejercicioId = ejercicioId;
        this.ejercicioNombre = nombre;
        this.ejercicioNombreEn = nombreEn;
        this.musculo = musculo;
        this.tipo = tipo;
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.fecha = fecha;
    }

    public Record() { }
}
