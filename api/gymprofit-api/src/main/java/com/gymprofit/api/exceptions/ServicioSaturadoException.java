package com.gymprofit.api.exceptions;

// ============================================================
// ServicioSaturadoException — sin cupo para llamar a un servicio externo (GP-160)
// ControllerExceptionHandler la convierte en 503 con Retry-After: no es un fallo del
// proveedor (eso es ExternalServiceException, 502), es que la API se ha puesto un
// límite para no pasarse del suyo.
// ============================================================
public class ServicioSaturadoException extends ExcepcionConClave {

    private final long reintentarEnSegundos;

    /**
     * @param reintentarEnSegundos segundos hasta que vuelva a haber cupo; va en Retry-After.
     * @param clave                clave del mensaje en messages*.properties
     * @param args                 argumentos del mensaje
     */
    public ServicioSaturadoException(long reintentarEnSegundos, String clave, Object... args) {
        super(clave, args, null);
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
