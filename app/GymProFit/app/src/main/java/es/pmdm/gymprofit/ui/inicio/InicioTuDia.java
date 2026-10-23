package es.pmdm.gymprofit.ui.inicio;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.comida.DiaDiario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.network.DiarioApi;
import es.pmdm.gymprofit.ui.widget.VasosAguaView;
import es.pmdm.gymprofit.utils.Agua;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// InicioTuDia — «Tu día» de Inicio (GP-216, tablero 13): un carrusel con calorías, proteína y
// agua de hoy, y debajo el botón de la comida que toca ahora.
//
// Los números son los de la pestaña Nutrición (DiaNutricion sobre las comidas de hoy, y el agua
// del diario con el vaso de la cuenta), así que las dos pantallas no discrepan. Con la red
// caída no se enseña un cero que parezca un dato: el carrusel se cambia por el error con su
// «Reintentar» (GP-211). Sin peso ni altura (GP-103) no hay objetivo: se enseña lo comido, sin
// aro ni «te quedan».
// ============================================================
public final class InicioTuDia {

    /** Quien quiere saber las comidas de hoy cuando llegan (el primer día marca «tu primera comida»). */
    public interface AlTenerComidas { void listo(@Nullable List<Comida> comidas); }

    private final Fragment fragment;
    private final View raiz;
    private final PreferencesManager prefs;
    private final AlTenerComidas alTenerComidas;
    private final VistaEstado estado;
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final DiarioApi diarioApi = ApiClient.service(DiarioApi.class);

    // Lo que ha llegado de las dos peticiones; se pinta cuando están las dos.
    private int pendientes;
    private boolean fallo;
    @Nullable private List<Comida> comidas;
    private int aguaMl;
    // Para ignorar respuestas de una carga anterior.
    private int generacion;

    public InicioTuDia(@NonNull Fragment fragment, @NonNull View raiz, @NonNull PreferencesManager prefs,
                       @NonNull Runnable abrirNutricion, @NonNull Runnable apuntarComida,
                       @NonNull AlTenerComidas alTenerComidas) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.prefs = prefs;
        this.alTenerComidas = alTenerComidas;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoDia));
        raiz.findViewById(R.id.enlaceTuDia).setOnClickListener(v -> abrirNutricion.run());
        for (int id : new int[]{R.id.cardDiaKcal, R.id.cardDiaProteina, R.id.cardDiaAgua}) {
            raiz.findViewById(id).setOnClickListener(v -> abrirNutricion.run());
        }
        raiz.findViewById(R.id.cardComidaQueToca).setOnClickListener(v -> apuntarComida.run());
        rotulo(R.id.cardDiaKcal, R.drawable.ic_ms_local_fire_department, androidx.appcompat.R.attr.colorPrimary, R.string.inicio_dia_calorias);
        rotuloColor(R.id.cardDiaProteina, R.drawable.ic_ms_egg_alt, R.color.gp_macro_proteinas, R.string.inicio_dia_proteina);
        rotuloColor(R.id.cardDiaAgua, R.drawable.ic_ms_water_drop, R.color.gp_agua, R.string.inicio_dia_agua);
        pintarComidaQueToca(null);
    }

    /** Pide las comidas de hoy y el agua del diario, y pinta. */
    public void cargar() {
        int uid = prefs.getUsuarioId();
        if (uid == -1) {
            // Sin cuenta no hay comidas ni agua: el día en blanco.
            comidas = null;
            aguaMl = 0;
            pintar();
            return;
        }
        final int esta = ++generacion;
        pendientes = 2;
        fallo = false;
        String hoy = TiempoRelativo.hoy();
        comidaApi.getDeUsuarioFecha(uid, hoy).enqueue(new ApiCallback<List<Comida>>() {
            @Override public void onOk(List<Comida> lista) {
                if (esta != generacion || !fragment.isAdded()) return;
                comidas = lista;
                alTenerComidas.listo(lista);
                llego();
            }
            @Override public void onFail(int code, String message) { fallo(esta, code, message); }
        });
        diarioApi.dias(hoy, hoy).enqueue(new ApiCallback<List<DiaDiario>>() {
            @Override public void onOk(List<DiaDiario> dias) {
                if (esta != generacion || !fragment.isAdded()) return;
                aguaMl = dias == null || dias.isEmpty() ? 0 : dias.get(0).getAguaMl();
                llego();
            }
            @Override public void onFail(int code, String message) { fallo(esta, code, message); }
        });
    }

    private void llego() {
        if (fallo || --pendientes > 0) return;
        pintar();
    }

    private void fallo(int esta, int code, String message) {
        if (esta != generacion || !fragment.isAdded() || fallo) return;
        fallo = true;
        raiz.findViewById(R.id.scrollTuDia).setVisibility(View.GONE);
        estado.error(VistaEstado.mensaje(fragment.requireContext(), R.string.inicio_dia_error, code, message), this::cargar);
        // El botón de la comida que toca no necesita datos: se queda.
        pintarComidaQueToca(null);
    }

    // ── Pintar ───────────────────────────────────────────────────────────────

    private void pintar() {
        estado.oculto();
        raiz.findViewById(R.id.scrollTuDia).setVisibility(View.VISIBLE);
        Locale idioma = FechaUtils.localeDeLaApp(fragment.requireContext());
        NumberFormat entero = NumberFormat.getIntegerInstance(idioma);
        NumberFormat decimal = NumberFormat.getNumberInstance(idioma);
        decimal.setMaximumFractionDigits(1);
        decimal.setMinimumFractionDigits(1);

        ResultadoNutricional objetivo = DiaNutricion.objetivo(prefs);
        DiaNutricion d = DiaNutricion.de(comidas, objetivo);

        pintarCalorias(d, entero);
        pintarProteina(d);
        pintarAgua(objetivo, decimal);
        pintarComidaQueToca(d);
    }

    private void pintarCalorias(DiaNutricion d, NumberFormat entero) {
        View card = raiz.findViewById(R.id.cardDiaKcal);
        CircularProgressIndicator anillo = card.findViewById(R.id.anilloDia);
        TextView cifra = card.findViewById(R.id.tvCifraDia);
        TextView apoyo = card.findViewById(R.id.tvApoyoDia);
        String a11y;
        if (d.sinObjetivo) {
            anillo.setProgressCompat(0, false);
            cifra.setText(entero.format(d.kcal));
            apoyo.setText(R.string.inicio_dia_kcal_hoy);
            a11y = fragment.getString(R.string.nutricion_sin_objetivo_a11y, entero.format(d.kcal));
        } else {
            anillo.setProgressCompat(DiaNutricion.porcentaje(d.kcal, d.objetivoKcal), false);
            int quedan = d.kcalRestantes();
            cifra.setText(entero.format(Math.abs(quedan)));
            apoyo.setText(quedan >= 0 ? R.string.inicio_dia_quedan : R.string.inicio_dia_pasas);
            a11y = fragment.getString(R.string.nutricion_hoy_a11y, entero.format(d.kcal), entero.format(d.objetivoKcal));
        }
        card.setContentDescription(fragment.getString(R.string.inicio_dia_calorias) + ". " + a11y);
    }

    private void pintarProteina(DiaNutricion d) {
        View card = raiz.findViewById(R.id.cardDiaProteina);
        CircularProgressIndicator anillo = card.findViewById(R.id.anilloDia);
        TextView cifra = card.findViewById(R.id.tvCifraDia);
        TextView apoyo = card.findViewById(R.id.tvApoyoDia);
        int g = (int) Math.round(d.proteinas);
        cifra.setText(fragment.getString(R.string.inicio_cifra_g, g));
        String a11y;
        if (d.sinObjetivo) {
            anillo.setProgressCompat(0, false);
            apoyo.setText(R.string.inicio_dia_kcal_hoy_g);
            a11y = fragment.getString(R.string.inicio_dia_proteina_sin_objetivo_a11y, g);
        } else {
            anillo.setProgressCompat(DiaNutricion.porcentaje(d.proteinas, d.objetivoProteinas), false);
            apoyo.setText(fragment.getString(R.string.inicio_dia_de, String.valueOf(d.objetivoProteinas)));
            a11y = fragment.getString(R.string.inicio_dia_proteina_a11y, g, d.objetivoProteinas);
        }
        card.setContentDescription(fragment.getString(R.string.inicio_dia_proteina) + ". " + a11y);
    }

    private void pintarAgua(@Nullable ResultadoNutricional objetivo, NumberFormat decimal) {
        View card = raiz.findViewById(R.id.cardDiaAgua);
        card.findViewById(R.id.anilloDia).setVisibility(View.GONE);
        VasosAguaView vasos = card.findViewById(R.id.vasosDia);
        vasos.setVisibility(View.VISIBLE);
        TextView cifra = card.findViewById(R.id.tvCifraDia);
        TextView apoyo = card.findViewById(R.id.tvApoyoDia);

        int vaso = prefs.getVasoMl();
        double litrosObjetivo = objetivo == null ? 0 : objetivo.agua;
        int llenos = Agua.vasosLlenos(aguaMl, vaso);
        int objetivoVasos = Agua.vasosObjetivo(litrosObjetivo, vaso);
        vasos.setVasos(Agua.vasosAPintar(llenos, objetivoVasos), llenos);

        String bebidos = decimal.format(aguaMl / 1000.0);
        cifra.setText(fragment.getString(R.string.inicio_cifra_l, bebidos));
        String a11y;
        if (litrosObjetivo > 0) {
            String meta = decimal.format(litrosObjetivo);
            apoyo.setText(fragment.getString(R.string.inicio_dia_de, meta));
            a11y = fragment.getString(R.string.inicio_dia_agua_a11y, bebidos, meta, llenos, objetivoVasos);
        } else {
            apoyo.setText(R.string.inicio_dia_agua_hoy);
            a11y = fragment.getString(R.string.inicio_dia_agua_sin_objetivo_a11y, bebidos, llenos);
        }
        card.setContentDescription(fragment.getString(R.string.inicio_dia_agua) + ". " + a11y);
    }

    // «Apuntar la merienda · Toca ahora · te faltan 48 g de proteína».
    private void pintarComidaQueToca(@Nullable DiaNutricion d) {
        String tipo = ComidaQueToca.ahora();
        ((TextView) raiz.findViewById(R.id.tvComidaQueToca)).setText(apuntarTexto(tipo));
        int falta = d == null || d.sinObjetivo ? 0 : d.objetivoProteinas - (int) Math.round(d.proteinas);
        TextView sub = raiz.findViewById(R.id.tvComidaQueTocaSub);
        sub.setText(falta > 0
                ? fragment.getString(R.string.inicio_toca_ahora_proteina, falta)
                : fragment.getString(R.string.inicio_toca_ahora));
        MaterialCardView card = raiz.findViewById(R.id.cardComidaQueToca);
        card.setContentDescription(apuntarTexto(tipo) + ". " + sub.getText());
    }

    private String apuntarTexto(String tipo) {
        int res;
        switch (tipo) {
            case "DESAYUNO": res = R.string.inicio_apuntar_desayuno; break;
            case "ALMUERZO": res = R.string.inicio_apuntar_almuerzo; break;
            case "COMIDA":   res = R.string.inicio_apuntar_comida; break;
            case "MERIENDA": res = R.string.inicio_apuntar_merienda; break;
            default:         res = R.string.inicio_apuntar_cena; break;
        }
        return fragment.getString(res);
    }

    // ── Rótulos de cada tarjeta ──────────────────────────────────────────────

    private void rotulo(int cardId, int icono, int atributoColor, int texto) {
        android.util.TypedValue tv = new android.util.TypedValue();
        fragment.requireContext().getTheme().resolveAttribute(atributoColor, tv, true);
        rotuloConColor(cardId, icono, tv.data, texto);
    }

    private void rotuloColor(int cardId, int icono, int color, int texto) {
        rotuloConColor(cardId, icono, ContextCompat.getColor(fragment.requireContext(), color), texto);
    }

    private void rotuloConColor(int cardId, int icono, int color, int texto) {
        View card = raiz.findViewById(cardId);
        ImageView iv = card.findViewById(R.id.ivRotuloDia);
        iv.setImageResource(icono);
        iv.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        // El aro lleva el color de su macro, como el icono.
        ((CircularProgressIndicator) card.findViewById(R.id.anilloDia)).setIndicatorColor(color);
        ((TextView) card.findViewById(R.id.tvRotuloDia)).setText(texto);
    }
}
