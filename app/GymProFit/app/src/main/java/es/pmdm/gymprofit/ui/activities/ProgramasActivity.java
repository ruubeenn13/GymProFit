package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.Recomendado;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.ui.widget.TuProgramaVista;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ProgramasActivity — elegir programa (GP-074, lote 1.2.1).
//
// Dónde entrena (gimnasio, mancuernas o peso corporal) y cuántos días (2 a 6), que se
// recuerdan por cuenta. Arriba, «Para ti · nivel …» con el recomendado de la API y su
// porqué; debajo, los demás de ese material con sus días, minutos y nivel. El que sigue
// lleva «Lo sigues». Tocar uno abre su detalle. La tabla del recomendado es de la API:
// la app no decide qué programa toca.
// ============================================================
public class ProgramasActivity extends BaseActivity {

    private final ProgramaApi api = ApiClient.service(ProgramaApi.class);

    private VistaEstado estado;
    private ChipGroup chipsEquipamiento, chipsDias;

    // Lo cargado; las tres llamadas se esperan y solo cuenta la última tanda.
    @Nullable private List<Programa> programas;
    @Nullable private Recomendado recomendado;
    @Nullable private String codigoSeguido;
    private boolean seguidoListo, fallo;
    private int tanda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programas);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        estado = new VistaEstado(findViewById(R.id.estadoProgramas));

        chipsEquipamiento = findViewById(R.id.chipsEquipamiento);
        chipsDias = findViewById(R.id.chipsDias);
        for (int dias = 2; dias <= 6; dias++) {
            Chip chip = (Chip) LayoutInflater.from(this).inflate(R.layout.item_chip_filtro, chipsDias, false);
            chip.setId(View.generateViewId());
            chip.setTag(dias);
            chip.setText(String.valueOf(dias));
            chip.setContentDescription(getResources().getQuantityString(R.plurals.programa_dias, dias, dias));
            chipsDias.addView(chip);
            if (dias == prefsManager.getProgramasDias()) chip.setChecked(true);
        }
        chipsEquipamiento.check(idEquipamiento(prefsManager.getProgramasEquipamiento()));

        chipsEquipamiento.setOnCheckedStateChangeListener((g, ids) -> alCambiarFiltros());
        chipsDias.setOnCheckedStateChangeListener((g, ids) -> alCambiarFiltros());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver del detalle puede seguir otro programa: «Lo sigues» se recalcula.
        cargar();
    }

    private void alCambiarFiltros() {
        prefsManager.saveProgramasFiltros(equipamiento(), dias());
        cargar();
    }

    private String equipamiento() {
        int id = chipsEquipamiento.getCheckedChipId();
        if (id == R.id.chipMancuernas) return Programa.MANCUERNAS;
        if (id == R.id.chipPesoCorporal) return Programa.PESO_CORPORAL;
        return Programa.GIMNASIO;
    }

    private static int idEquipamiento(String equipamiento) {
        if (Programa.MANCUERNAS.equals(equipamiento)) return R.id.chipMancuernas;
        if (Programa.PESO_CORPORAL.equals(equipamiento)) return R.id.chipPesoCorporal;
        return R.id.chipGimnasio;
    }

    private int dias() {
        View chip = chipsDias.findViewById(chipsDias.getCheckedChipId());
        return chip != null && chip.getTag() instanceof Integer ? (Integer) chip.getTag() : 3;
    }

    private void cargar() {
        int esta = ++tanda;
        programas = null;
        recomendado = null;
        codigoSeguido = null;
        seguidoListo = false;
        fallo = false;
        estado.cargando();
        findViewById(R.id.contenidoProgramas).setVisibility(View.GONE);

        api.listar(equipamiento()).enqueue(new ApiCallback<List<Programa>>() {
            @Override public void onOk(List<Programa> l) {
                if (esta != tanda) return;
                programas = l != null ? l : new ArrayList<>();
                listo();
            }
            @Override public void onFail(int code, String message) { fallar(esta, code, message); }
        });
        api.recomendado(equipamiento(), dias()).enqueue(new ApiCallback<Recomendado>() {
            @Override public void onOk(Recomendado r) {
                if (esta != tanda) return;
                recomendado = r;
                listo();
            }
            @Override public void onFail(int code, String message) { fallar(esta, code, message); }
        });
        api.seguido().enqueue(new ApiCallback<ProgramaQueSigue>() {
            @Override public void onOk(ProgramaQueSigue s) {
                if (esta != tanda) return;
                codigoSeguido = s != null && s.getPrograma() != null ? s.getPrograma().getCodigo() : null;
                seguidoListo = true;
                listo();
            }
            @Override public void onFail(int code, String message) {
                // Solo decide la etiqueta «Lo sigues»: sin ella la lista se usa igual, y el
                // programa que sigue se ve en Entrenar. No merece tapar la pantalla.
                if (esta != tanda) return;
                seguidoListo = true;
                listo();
            }
        });
    }

    private void fallar(int esta, int code, String message) {
        if (esta != tanda || fallo || isFinishing()) return;
        fallo = true;
        findViewById(R.id.contenidoProgramas).setVisibility(View.GONE);
        estado.error(VistaEstado.mensaje(this, R.string.programas_error, code, message), this::cargar);
    }

    private void listo() {
        if (fallo || programas == null || recomendado == null || !seguidoListo || isFinishing()) return;
        estado.oculto();
        findViewById(R.id.contenidoProgramas).setVisibility(View.VISIBLE);

        Programa elegido = recomendado.getPrograma();
        ((TextView) findViewById(R.id.tvParaTi)).setText(getString(R.string.programas_para_ti,
                UIHelper.traducirNivel(this, recomendado.getNivel()).toLowerCase(
                        es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(this))));
        FrameLayout contenedor = findViewById(R.id.contenedorRecomendado);
        contenedor.removeAllViews();
        contenedor.addView(tarjeta(contenedor, elegido, recomendado.getMotivo()));

        LinearLayout lista = findViewById(R.id.listaProgramas);
        lista.removeAllViews();
        for (Programa p : programas) {
            if (elegido != null && p.getCodigo().equals(elegido.getCodigo())) continue;
            lista.addView(tarjeta(lista, p, null));
        }
        findViewById(R.id.tvProgramasVacio).setVisibility(lista.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private View tarjeta(ViewGroup padre, Programa p, @Nullable String motivo) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_programa, padre, false);
        String resumen = resumen(this, p);
        boolean loSigue = p.getCodigo().equals(codigoSeguido);
        ((TextView) card.findViewById(R.id.tvProgramaNombre)).setText(p.getNombre());
        ((TextView) card.findViewById(R.id.tvProgramaResumen)).setText(resumen);
        card.findViewById(R.id.tvLoSigues).setVisibility(loSigue ? View.VISIBLE : View.GONE);
        TextView tvMotivo = card.findViewById(R.id.tvProgramaMotivo);
        tvMotivo.setText(motivo);
        tvMotivo.setVisibility(motivo == null || motivo.isEmpty() ? View.GONE : View.VISIBLE);

        StringBuilder a11y = new StringBuilder(p.getNombre());
        if (loSigue) a11y.append(". ").append(getString(R.string.programas_lo_sigues));
        a11y.append(". ").append(resumen);
        if (motivo != null && !motivo.isEmpty()) a11y.append(". ").append(motivo);
        card.setContentDescription(a11y);
        card.setOnClickListener(v -> startActivity(new Intent(this, ProgramaDetalleActivity.class)
                .putExtra(ProgramaDetalleActivity.EXTRA_CODIGO, p.getCodigo())));
        return card;
    }

    /** «3 días · 45–55 min · Intermedio» (o «2–3 días»). */
    public static String resumen(android.content.Context ctx, Programa p) {
        String dias = p.getDiasMin() == p.getDiasMax()
                ? ctx.getResources().getQuantityString(R.plurals.programa_dias, p.getDiasMax(), p.getDiasMax())
                : ctx.getString(R.string.programa_dias_rango, p.getDiasMin(), p.getDiasMax());
        int[] minutos = p.minutos();
        String tiempo = minutos == null ? "" : minutos[0] == minutos[1]
                ? ctx.getString(R.string.duracion_min, minutos[0])
                : ctx.getString(R.string.programa_minutos_rango, minutos[0], minutos[1]);
        String nivel = UIHelper.traducirNivel(ctx, p.getNivel());
        return tiempo.isEmpty() ? ctx.getString(R.string.rutina_resumen, dias, nivel)
                : ctx.getString(R.string.programa_resumen, dias, tiempo, nivel);
    }

    /** «2–3 días · 50 min», para el detalle (el nivel va en la línea de arriba). */
    public static String diasYMinutos(android.content.Context ctx, Programa p) {
        String dias = p.getDiasMin() == p.getDiasMax()
                ? ctx.getResources().getQuantityString(R.plurals.programa_dias, p.getDiasMax(), p.getDiasMax())
                : ctx.getString(R.string.programa_dias_rango, p.getDiasMin(), p.getDiasMax());
        int[] minutos = p.minutos();
        if (minutos == null) return dias;
        String tiempo = minutos[0] == minutos[1] ? ctx.getString(R.string.duracion_min, minutos[0])
                : ctx.getString(R.string.programa_minutos_rango, minutos[0], minutos[1]);
        return ctx.getString(R.string.rutina_resumen, dias, tiempo);
    }

    /** «Gimnasio · Intermedio», para el detalle. */
    public static String materialYNivel(android.content.Context ctx, Programa p) {
        return ctx.getString(R.string.rutina_resumen, TuProgramaVista.equipamiento(ctx, p.getEquipamiento()),
                UIHelper.traducirNivel(ctx, p.getNivel()));
    }
}
