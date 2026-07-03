package es.pmdm.gymprofit.model.logro;

// ============================================================
// UsuarioLogro — modelo de la relación usuario-logro (logro desbloqueado).
// Representa cada fila devuelta por GET logros/usuario/{id}: identifica el
// logro conseguido (logroId) por el usuario. La app solo usa logroId para
// marcar en el catálogo qué logros están desbloqueados; el resto de campos
// desnormalizados del DTO (nombre, tipo, fecha) se ignoran al deserializar.
// ============================================================
public class UsuarioLogro {

    // Identificador de la propia relación usuario-logro.
    private int id;
    // Identificador del logro desbloqueado (clave usada para marcar el catálogo).
    private int logroId;

    public UsuarioLogro() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLogroId() { return logroId; }
    public void setLogroId(int logroId) { this.logroId = logroId; }
}
