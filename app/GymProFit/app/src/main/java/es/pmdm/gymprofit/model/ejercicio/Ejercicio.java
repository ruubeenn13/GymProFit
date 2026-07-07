package es.pmdm.gymprofit.model.ejercicio;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import es.pmdm.gymprofit.network.BooleanNumericAdapter;

// ============================================================
// Ejercicio — modelo de datos de un ejercicio del catálogo de la app.
// Contiene la información descriptiva (grupo muscular, dificultad,
// instrucciones, imagen, etc.) que se muestra en el listado y detalle
// de ejercicios, y que se usa al construir rutinas.
// ============================================================
public class Ejercicio {

    // Identificador único del ejercicio.
    private int id;
    // Nombre del ejercicio.
    private String nombre;
    // Grupo muscular principal que trabaja el ejercicio.
    private String grupoMuscular;
    // Nivel de dificultad (principiante, intermedio, avanzado...).
    private String dificultad;
    // Descripción general del ejercicio.
    private String descripcion;
    // Instrucciones paso a paso para realizarlo.
    private String instrucciones;
    // URL de la imagen ilustrativa del ejercicio (fotograma 1).
    private String imagenUrl;
    // Fotograma 2 de la demostración (se alterna con imagenUrl para animarla).
    private String imagenUrl2;
    // Equipo/material necesario para realizarlo.
    private String equipoNecesario;
    // Calorías aproximadas quemadas por el ejercicio.
    // La API devuelve esta métrica como "caloriasQuemadas"; se mapea al campo local "calorias".
    @SerializedName("caloriasQuemadas")
    private int calorias;
    // Indica si el ejercicio está activo/visible en el catálogo.
    // La búsqueda admin (EjercicioJooqDTO) envía "activo" como Byte 0/1; el adaptador lo tolera.
    @JsonAdapter(BooleanNumericAdapter.class)
    private boolean activo;

    public Ejercicio() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getGrupoMuscular() { return grupoMuscular; }
    public void setGrupoMuscular(String grupoMuscular) { this.grupoMuscular = grupoMuscular; }

    public String getDificultad() { return dificultad; }
    public void setDificultad(String dificultad) { this.dificultad = dificultad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getImagenUrl2() { return imagenUrl2; }
    public void setImagenUrl2(String imagenUrl2) { this.imagenUrl2 = imagenUrl2; }

    public String getEquipoNecesario() { return equipoNecesario; }
    public void setEquipoNecesario(String equipoNecesario) { this.equipoNecesario = equipoNecesario; }

    public int getCalorias() { return calorias; }
    public void setCalorias(int calorias) { this.calorias = calorias; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
