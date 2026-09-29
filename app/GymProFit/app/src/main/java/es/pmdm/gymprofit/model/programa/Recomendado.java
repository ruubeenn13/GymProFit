package es.pmdm.gymprofit.model.programa;

// ============================================================
// Recomendado — el programa para el perfil, el equipamiento y los días (GP-074).
// El porqué, si lo hay, ya viene en el idioma de la app.
// ============================================================
public class Recomendado {
    private Programa programa;
    private String nivel;
    private boolean nivelEnPerfil;
    private String motivo;

    public Programa getPrograma() { return programa; }
    public String getNivel() { return nivel; }
    public boolean isNivelEnPerfil() { return nivelEnPerfil; }
    public String getMotivo() { return motivo; }
}
