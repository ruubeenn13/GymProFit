package es.pmdm.gymprofit.model.usuario;

public class Usuario {

    private int id;
    private String username;
    private String email;
    private String peso;
    private double altura;
    private int edad;
    private String nivelExperiencia;
    private String objetivo;
    private String fechaRegistro;
    private boolean activo;
    private String rol;

    public Usuario() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
