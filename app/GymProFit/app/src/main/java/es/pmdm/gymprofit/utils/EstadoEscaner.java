package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// EstadoEscaner — qué hace el escáner con cada lectura y cada respuesta (lote 1.6.1)
//
//   BUSCANDO ──leído/escrito──▶ CONSULTANDO ──▶ PRODUCTO | NO_EXISTE | SIN_CUPO | SIN_RED
//      ▲                                                    │
//      └──────────────────── «Escanear otro» ───────────────┘
// La cámara solo cuenta lo que lee en BUSCANDO: con la hoja abierta (o preguntando) no
// se lee otro código. Tras un fallo de cupo o de red, «Reintentar» pregunta otra vez por
// el mismo código. Una respuesta que llega para un código que ya no es el de ahora
// (se escaneó otro mientras tanto) se descarta.
// Sin vistas ni red: EscanerActivity le pasa los eventos y pinta según el estado.
// ============================================================
public final class EstadoEscaner {

    public enum Estado { BUSCANDO, CONSULTANDO, PRODUCTO, NO_EXISTE, SIN_CUPO, SIN_RED }

    private Estado estado = Estado.BUSCANDO;
    @Nullable private String codigo;
    private long segundos;

    @NonNull
    public Estado estado() {
        return estado;
    }

    /** El código de ahora; null buscando. */
    @Nullable
    public String codigo() {
        return codigo;
    }

    /** Con SIN_CUPO, los segundos que pide la API (Retry-After). */
    public long segundos() {
        return segundos;
    }

    /** ¿Se analiza lo que ve la cámara? Solo buscando. */
    public boolean leeCamara() {
        return estado == Estado.BUSCANDO;
    }

    /** ¿Está la hoja abierta (o preguntando)? */
    public boolean hayHoja() {
        return estado != Estado.BUSCANDO;
    }

    /**
     * La cámara ha leído un código.
     *
     * @return true si hay que preguntar a la API por él; false si se ignora.
     */
    public boolean leido(@NonNull String leido) {
        if (estado != Estado.BUSCANDO) return false;
        codigo = leido;
        estado = Estado.CONSULTANDO;
        return true;
    }

    /** Un código escrito a mano (ya validado): entra igual que uno leído. */
    public boolean escrito(@NonNull String escrito) {
        return leido(escrito);
    }

    public void encontrado(@NonNull String de) {
        responder(de, Estado.PRODUCTO, 0);
    }

    public void noExiste(@NonNull String de) {
        responder(de, Estado.NO_EXISTE, 0);
    }

    public void sinCupo(@NonNull String de, long reintentarEn) {
        responder(de, Estado.SIN_CUPO, reintentarEn);
    }

    public void sinRed(@NonNull String de) {
        responder(de, Estado.SIN_RED, 0);
    }

    private void responder(String de, Estado nuevo, long espera) {
        if (estado != Estado.CONSULTANDO || !de.equals(codigo)) return;
        estado = nuevo;
        segundos = espera;
    }

    /** «Escanear otro»: cierra la hoja y vuelve a buscar. */
    public void otroMas() {
        estado = Estado.BUSCANDO;
        codigo = null;
        segundos = 0;
    }

    /**
     * «Reintentar» tras un fallo de cupo o de red.
     *
     * @return true si hay que volver a preguntar por el mismo código.
     */
    public boolean reintentar() {
        if (estado != Estado.SIN_CUPO && estado != Estado.SIN_RED) return false;
        estado = Estado.CONSULTANDO;
        segundos = 0;
        return true;
    }
}
