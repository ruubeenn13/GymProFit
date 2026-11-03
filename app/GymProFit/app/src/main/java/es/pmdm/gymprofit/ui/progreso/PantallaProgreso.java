package es.pmdm.gymprofit.ui.progreso;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.function.IntConsumer;

import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.activities.MainActivity;
import es.pmdm.gymprofit.ui.fragments.ProgresoFragment;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// PantallaProgreso — lo que pide Progreso y a quién se lo da (GP-216, lote 4).
//
// Seis fuentes, pedidas a la vez: sesiones, récords, logros, kilos por semana (KilosProgreso),
// mediciones y el perfil. La lista de sesiones se pide una sola vez y la leen el resumen, «Todo»
// de los kilos y el historial. Cada fuente, al llegar, repinta solo las secciones que la leen; un
// fallo se dice en esas secciones y su «Reintentar» vuelve a pedir solo esa fuente.
// ============================================================
public final class PantallaProgreso {

    private final Fragment fragment;
    private final int usuarioId;
    private final ResumenProgreso resumen;
    private final KilosProgreso kilos;
    private final ListasProgreso listas;
    private final CuerpoProgreso cuerpo;

    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final LogroApi logroApi = ApiClient.service(LogroApi.class);
    private final MedicionApi medicionApi = ApiClient.service(MedicionApi.class);
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    private final Fuente<List<SesionEntrenamiento>> sesiones = new Fuente<>();
    private final Fuente<Records> records = new Fuente<>();
    private final Fuente<List<LogroProgreso>> logros = new Fuente<>();
    private final Fuente<List<MedicionCorporal>> mediciones = new Fuente<>();
    private final Fuente<Usuario> usuario = new Fuente<>();

    /**
     * @param periodo      el período de los kilos que había elegido (índice de KilosMovidos.PERIODOS).
     * @param abrirSeccion abre la lista completa de una sección (ProgresoFragment.RECORDS…HISTORIAL).
     */
    public PantallaProgreso(@NonNull Fragment fragment, @NonNull View raiz, @NonNull PreferencesManager prefs,
                            int periodo, @NonNull IntConsumer abrirSeccion) {
        this.fragment = fragment;
        this.usuarioId = prefs.getUsuarioId();
        resumen = new ResumenProgreso(raiz, this::reintentarResumen);
        kilos = new KilosProgreso(fragment, raiz, usuarioId, periodo, this::pedirSesiones);
        listas = new ListasProgreso(fragment, raiz, abrirSeccion);
        cuerpo = new CuerpoProgreso(fragment, raiz, prefs, () -> abrirSeccion.accept(ProgresoFragment.MEDIDAS),
                this::pedirMediciones);
    }

    /** El período de los kilos elegido, para guardarlo al recrearse. */
    public int periodo() { return kilos.periodo(); }

    /** Pide todo a la vez (al abrir Progreso y al volver a él). */
    public void cargar() {
        // Sin cuenta no hay nada que pedir; no pasa con sesión iniciada, pero no se pide con -1.
        if (usuarioId == -1) return;
        pedirSesiones();
        pedirRecords();
        pedirLogros();
        kilos.pedir();
        pedirMediciones();
        pedirUsuario();
    }

    private void pedirSesiones() {
        sesiones.pedir(sesionApi.getDeUsuario(usuarioId), fragment, this::pintarSesiones);
        pintarSesiones();
    }

    private void pintarSesiones() {
        resumen.pintar(sesiones, records, logros);
        kilos.setSesiones(sesiones);
        listas.pintarHistorial(sesiones, this::pedirSesiones);
    }

    private void pedirRecords() {
        records.pedir(recordApi.getRecords(null), fragment, this::pintarRecords);
        pintarRecords();
    }

    private void pintarRecords() {
        resumen.pintar(sesiones, records, logros);
        listas.pintarRecords(records, this::pedirRecords);
    }

    private void pedirLogros() {
        logros.pedir(logroApi.getProgreso(), fragment, this::pintarLogros);
        pintarLogros();
    }

    private void pintarLogros() {
        resumen.pintar(sesiones, records, logros);
        listas.pintarLogros(logros, this::pedirLogros);
    }

    private void pedirMediciones() {
        mediciones.pedir(medicionApi.getOrdenadas(usuarioId), fragment, this::pintarCuerpo);
        pintarCuerpo();
    }

    private void pedirUsuario() {
        usuario.pedir(usuarioApi.getPorId(usuarioId), fragment, this::pintarCuerpo);
    }

    private void pintarCuerpo() {
        cuerpo.pintar(mediciones, usuario, this::pedirMediciones);
        // La hoja del «+» enseña el último peso: con esto sigue al día tras apuntarlo aquí o en Medidas.
        if (mediciones.ok() && fragment.getActivity() instanceof MainActivity) {
            ((MainActivity) fragment.getActivity()).publicarUltimoPeso(cuerpo.ultima());
        }
    }

    // El resumen sale de tres fuentes: se vuelven a pedir solo las que fallaron.
    private void reintentarResumen() {
        if (sesiones.fallo()) pedirSesiones();
        if (records.fallo()) pedirRecords();
        if (logros.fallo()) pedirLogros();
    }
}
