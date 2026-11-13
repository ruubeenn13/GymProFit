package es.pmdm.gymprofit.ui.progreso;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.core.content.ContextCompat;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.utils.HistorialSesiones;
import es.pmdm.gymprofit.utils.LogrosVisibles;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ResumenProgreso — las tres cifras de arriba de Progreso (GP-216, lote 4, tablero 16): sesiones
// terminadas, récords vigentes y logros conseguidos, con su plural.
//
// Sale de tres fuentes (sesiones, récords y logros). Espera a las tres para no enseñar una cifra
// que luego cambia sola; si alguna falla, dice el fallo con «Reintentar», que pide solo las que
// fallaron. Una cuenta nueva no ve tres ceros: ve qué va a llenar esto.
// ============================================================
final class ResumenProgreso {

    private final View raiz;
    private final VistaEstado estado;
    private final Runnable reintentar;

    ResumenProgreso(@NonNull View raiz, @NonNull Runnable reintentar) {
        this.raiz = raiz;
        this.reintentar = reintentar;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoResumen));
        View records = raiz.findViewById(R.id.cifraRecords);
        records.setBackgroundResource(R.drawable.bg_cifra_progreso_oro);
        ((TextView) records.findViewById(R.id.tvCifra))
                .setTextColor(ContextCompat.getColor(raiz.getContext(), R.color.gp_gold_text));
    }

    void pintar(@NonNull Fuente<List<SesionEntrenamiento>> sesiones, @NonNull Fuente<Records> records,
                @NonNull Fuente<List<LogroProgreso>> logros) {
        Context ctx = raiz.getContext();
        View fila = raiz.findViewById(R.id.filaCifras);
        if (!sesiones.lista() || !records.lista() || !logros.lista()) {
            if (fila.getVisibility() != View.VISIBLE) estado.cargando();
            return;
        }
        Fuente<?> fallida = sesiones.fallo() ? sesiones : records.fallo() ? records : logros.fallo() ? logros : null;
        if (fallida != null) {
            fila.setVisibility(View.GONE);
            estado.error(VistaEstado.mensaje(ctx, R.string.progreso_resumen_error, fallida.code(), fallida.mensaje()),
                    reintentar);
            return;
        }
        int nSesiones = HistorialSesiones.terminadas(sesiones.dato()).size();
        int nRecords = records.dato() == null ? 0 : records.dato().getRecords().size();
        int nLogros = LogrosVisibles.conseguidos(logros.dato());
        if (nSesiones == 0 && nRecords == 0 && nLogros == 0) {
            fila.setVisibility(View.GONE);
            estado.vacio(R.drawable.ic_ms_insights, R.string.progreso_resumen_vacio);
            return;
        }
        estado.oculto();
        fila.setVisibility(View.VISIBLE);
        cifra(R.id.cifraSesiones, nSesiones, R.plurals.progreso_sesiones);
        cifra(R.id.cifraRecords, nRecords, R.plurals.progreso_records);
        cifra(R.id.cifraLogros, nLogros, R.plurals.progreso_logros);
    }

    private void cifra(int id, int n, @PluralsRes int etiqueta) {
        Context ctx = raiz.getContext();
        View v = raiz.findViewById(id);
        String numero = String.valueOf(n);
        String texto = ctx.getResources().getQuantityString(etiqueta, n);
        ((TextView) v.findViewById(R.id.tvCifra)).setText(numero);
        ((TextView) v.findViewById(R.id.tvCifraEtiqueta)).setText(texto);
        v.setContentDescription(ctx.getString(R.string.progreso_cifra_a11y, numero, texto));
    }
}
