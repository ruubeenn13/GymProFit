package es.pmdm.gymprofit.model.usuario;

import com.google.gson.annotations.JsonAdapter;

import es.pmdm.gymprofit.network.BooleanNumericAdapter;

// ============================================================
// Usuario — modelo de datos que representa un usuario de la app
// Contiene los datos de perfil devueltos/enviados por la API (identidad,
// datos físicos, nivel, objetivo, rol y foto de perfil). Se usa para
// deserializar respuestas JSON de la API y para mostrar/editar el perfil.
// ============================================================
public class Usuario {

    // Identificador único del usuario en la base de datos
    private int id;
    private String username;
    // Nombre para mostrar (GP-116); null si no tiene. Ver NombreVisible.
    private String nombre;
    private String email;
    private String peso;
    private double altura;
    private int edad;
    private String nivelExperiencia;
    private String objetivo;
    // GP-111: null si el usuario no los ha dicho (o la API es anterior a la 1.1.3).
    private String sexo;
    private String nivelActividad;
    private String fechaRegistro;
    // El listado admin (AdminUsuarioDTO) envía "activo" como Byte 0/1; el adaptador lo tolera.
    @JsonAdapter(BooleanNumericAdapter.class)
    private boolean activo;
    private String rol;
    private String fotoPerfil;

    // Constructor vacío requerido para deserialización JSON (Gson/Retrofit)
    public Usuario() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }

    public double getAltura() { return altura; }
    public void setAltura(double altura) { this.altura = altura; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }

    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getNivelActividad() { return nivelActividad; }
    public void setNivelActividad(String nivelActividad) { this.nivelActividad = nivelActividad; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
}
