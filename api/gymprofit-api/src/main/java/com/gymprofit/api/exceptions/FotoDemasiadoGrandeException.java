package com.gymprofit.api.exceptions;

// ============================================================
// FotoDemasiadoGrandeException — la foto pasa del tope (GP-188, lote 1.6.5)
// ControllerExceptionHandler la convierte en 413 con error.foto.tamano, el mismo
// mensaje que cuando el tope lo para el multipart de Tomcat.
// ============================================================
public class FotoDemasiadoGrandeException extends InvalidDataException {

    /** Código estable para «cause»: la app no depende del texto. */
    public static final String CODIGO = "FOTO_DEMASIADO_GRANDE";

    public FotoDemasiadoGrandeException() {
        super("error.foto.tamano");
    }
}
