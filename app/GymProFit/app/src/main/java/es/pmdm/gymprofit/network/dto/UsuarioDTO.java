package es.pmdm.gymprofit.network.dto;

public class UsuarioDTO {
    private Integer id;
    private String username;
    private String email;
    private String peso;
    private Double altura;
    private Integer edad;
    private String nivelExperiencia;
    private String objetivo;
    private String fechaRegistro;
    private Boolean activo;

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPeso() { return peso; }
    public Double getAltura() { return altura; }
    public Integer getEdad() { return edad; }
    public String getNivelExperiencia() { return nivelExperiencia; }
    public String getObjetivo() { return objetivo; }
    public String getFechaRegistro() { return fechaRegistro; }
    public Boolean getActivo() { return activo; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPeso(String peso) { this.peso = peso; }
    public void setAltura(Double altura) { this.altura = altura; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
}