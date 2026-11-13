package es.pmdm.gymprofit.ui.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VerificacionCorreo;

// ============================================================
// VerificarCorreoViewModel — el código nuevo al entrar desde Perfil (GP-267).
//
// Desde Perfil la pantalla no sabe cuándo se mandó el último código (días antes, o nunca
// en este móvil) y abría en «Ese código ya no vale». Ahora, al entrar, pide uno nuevo con
// el mismo reenvío del botón y abre en el estado normal mientras llega.
//
// Una petición por ENTRADA: girar o volver de segundo plano no es entrar otra vez. Por eso
// vive aquí y no en la Activity: el ViewModel sobrevive al giro con la petición en vuelo,
// y el SavedStateHandle a que Android mate el proceso. Si el proceso murió con la petición
// en vuelo no se repite (no se sabe si llegó): se abre normal y queda el botón de reenviar.
//
// Desde el alta no pide nada: la API acaba de mandar el código al guardar el plan.
// ============================================================
public class VerificarCorreoViewModel extends ViewModel {

    /** Cómo acabó la petición de la entrada. Null: no hubo (se entró desde el alta). */
    public enum Estado {
        /** En vuelo: la pantalla, normal, con el reenvío apagado. */
        ENVIANDO,
        /** Código nuevo emitido y apuntado: valen las reglas de siempre. */
        ENVIADO,
        /** 429: la cuenta atrás del Retry-After en el reenvío. */
        ESPERA,
        /** 409: el correo ya estaba verificado. */
        YA_VERIFICADO,
        /** Red o servidor: se dice una vez y queda el botón de reenviar. */
        FALLO,
        /** El proceso murió con la petición en vuelo: no se sabe si llegó. */
        INTERRUMPIDO
    }

    /** Lo que el ViewModel necesita de fuera: la red y PreferencesManager. */
    public interface Puerto {

        /** Respuesta del reenvío. */
        interface Respuesta {
            void ok();

            /**
             * @param codigo  estado HTTP, o negativo si no hubo respuesta.
             * @param mensaje el mensaje del error, con el Retry-After delante si es un 429.
             */
            void fallo(int codigo, String mensaje);
        }

        /** {@code POST /usuarios/me/email/reenviar}. */
        void pedirCodigo(@NonNull Respuesta respuesta);

        /** Apunta que se acaba de emitir un código (para contar su caducidad). */
        void apuntarCodigo(long ahora);
    }

    /** La hora, en ms; inyectada para probar la cuenta atrás. */
    public interface Reloj {
        long ahora();
    }

    private static final String DESDE_PERFIL = "desde_perfil";
    private static final String PEDIDO = "pedido";
    private static final String ESTADO = "estado";
    private static final String ESPERA_HASTA = "espera_hasta";
    private static final String CODIGO_FALLO = "codigo_fallo";
    private static final String MENSAJE_FALLO = "mensaje_fallo";
    private static final String AVISADO = "avisado";

    private final SavedStateHandle handle;
    private final Puerto puerto;
    private final Reloj reloj;
    // Solo en memoria: si el proceso muere, la petición muere con él.
    private boolean enVuelo;

    /**
     * @param handle estado que sobrevive a la muerte del proceso.
     * @param puerto red y preferencias.
     * @param reloj  la hora.
     */
    public VerificarCorreoViewModel(@NonNull SavedStateHandle handle, @NonNull Puerto puerto, @NonNull Reloj reloj) {
        this.handle = handle;
        this.puerto = puerto;
        this.reloj = reloj;
    }

    SavedStateHandle handle() { return handle; }

    /**
     * La pantalla se ha creado: la primera vez desde Perfil pide el código; las demás
     * (giro, vuelta de segundo plano, proceso recreado) no.
     *
     * @param desdePerfil true si se entró desde el aviso de Perfil.
     */
    public void entrar(boolean desdePerfil) {
        if (!desdePerfil) return;
        handle.set(DESDE_PERFIL, true);
        if (Boolean.TRUE.equals(handle.get(PEDIDO))) {
            if (handle.get(ESTADO) == Estado.ENVIANDO && !enVuelo) {
                // La petición se perdió con el proceso anterior.
                handle.set(ESTADO, Estado.INTERRUMPIDO);
            }
            return;
        }
        handle.set(PEDIDO, true);
        handle.set(ESTADO, Estado.ENVIANDO);
        enVuelo = true;
        puerto.pedirCodigo(new Puerto.Respuesta() {
            @Override
            public void ok() {
                enVuelo = false;
                puerto.apuntarCodigo(reloj.ahora());
                responder(Estado.ENVIADO);
            }

            @Override
            public void fallo(int codigo, String mensaje) {
                enVuelo = false;
                switch (VerificacionCorreo.apertura(codigo)) {
                    case YA_VERIFICADO:
                        responder(Estado.YA_VERIFICADO);
                        break;
                    case ESPERA:
                        Integer s = UiFeedback.segundosDeEspera(mensaje);
                        if (s != null) {
                            handle.set(ESPERA_HASTA, reloj.ahora() + s * 1000L);
                            responder(Estado.ESPERA);
                            break;
                        }
                        // Un 429 sin Retry-After no tiene cuenta atrás que enseñar: se
                        // trata como cualquier fallo, con su mensaje.
                        guardarFallo(codigo, mensaje);
                        break;
                    default:
                        guardarFallo(codigo, mensaje);
                }
            }
        });
    }

    private void guardarFallo(int codigo, String mensaje) {
        handle.set(CODIGO_FALLO, codigo);
        handle.set(MENSAJE_FALLO, mensaje);
        responder(Estado.FALLO);
    }

    private void responder(@NonNull Estado estado) {
        handle.set(AVISADO, false);
        handle.set(ESTADO, estado);
    }

    /** Cómo va la petición de la entrada; null si no hubo. */
    @NonNull
    public LiveData<Estado> estado() {
        return handle.getLiveData(ESTADO);
    }

    /** Segundos que le quedan a la espera del 429; 0 si ya pasó o no la hay. */
    public int segundosDeEspera(long ahora) {
        Long hasta = handle.get(ESPERA_HASTA);
        if (hasta == null) return 0;
        return (int) Math.max(0L, (long) Math.ceil((hasta - ahora) / 1000.0));
    }

    /** El código HTTP del fallo, para UiFeedback. */
    public int codigoFallo() {
        Integer c = handle.get(CODIGO_FALLO);
        return c == null ? -1 : c;
    }

    /** El mensaje del fallo, para UiFeedback. */
    @Nullable
    public String mensajeFallo() {
        return handle.get(MENSAJE_FALLO);
    }

    /** True si la respuesta aún no se ha dicho (toast, TalkBack): se dice una vez, no en cada giro. */
    public boolean tocaAvisar() {
        return Boolean.FALSE.equals(handle.get(AVISADO));
    }

    /** La respuesta ya se ha dicho. */
    public void avisado() {
        handle.set(AVISADO, true);
    }

    /**
     * Dice si la pantalla se abre en «Ese código ya no vale».
     * <p>
     * Desde Perfil, solo con el código que pidió esta entrada ya emitido: entonces valen
     * las reglas de siempre (quince minutos, cinco intentos). Sin él —en vuelo, con el
     * 429, sin red— lo apuntado es de un código viejo y no dice nada: nunca abre en error.
     *
     * @param emitidoEn cuándo se emitió el último código apuntado; 0 o menos si no se sabe.
     * @param fallos    códigos equivocados desde entonces.
     * @param ahora     la hora, en ms.
     */
    public boolean abrirCaducado(long emitidoEn, int fallos, long ahora) {
        boolean desdePerfil = Boolean.TRUE.equals(handle.get(DESDE_PERFIL));
        if (desdePerfil && handle.get(ESTADO) != Estado.ENVIADO) return false;
        return VerificacionCorreo.caducado(emitidoEn, fallos, ahora);
    }
}
