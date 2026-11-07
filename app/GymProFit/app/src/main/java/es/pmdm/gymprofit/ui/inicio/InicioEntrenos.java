package es.pmdm.gymprofit.ui.inicio;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.PaginaHistorial;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta;
import es.pmdm.gymprofit.ui.tarjetas.TextosTarjeta;
import es.pmdm.gymprofit.ui.widget.BarrasVecesView;
import es.pmdm.gymprofit.utils.CifraTarjeta;
import es.pmdm.gymprofit.utils.ComparacionSesion;
import es.pmdm.gymprofit.utils.Transiciones;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// InicioEntrenos — «Tus últimos entrenos» de Inicio (GP-216, tablero 13; rehecho en GP-232): un
// carrusel con los cinco últimos entrenos terminados, cada uno con cuándo, la rutina, los kilos
// con las barras de sus últimas veces y una sola línea destacada: sus récords en dorado o, si no
// los hubo, la comparación con la anterior de la misma rutina. Sin récords ni comparación, nada:
// ya no sale «Sin récords».
//
// Todo sale de la primera página de GET /sesiones/historial, una petición: ya no se pide el
// resumen de cada tarjeta. Los récords son los que batió cada sesión ese día, como en el
// Historial y en el resumen; uno superado después sigue contando en su sesión.
// ============================================================
public final class InicioEntrenos {

    /** Cuántos entrenos se enseñan. */
    private static final int MAXIMO = 5;

    private final Fragment fragment;
    private final View raiz;
    private final VistaEstado estado;
    private final Runnable alCargar;
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final EntradaTarjeta entrada = new EntradaTarjeta();
    @Nullable private PaginaHistorial pagina;
    private int tanda;

    /**
     * @param alCargar avisa cuando llegan los entrenos (Profi dice los récords de la última).
     */
    public InicioEntrenos(@NonNull Fragment fragment, @NonNull View raiz, @NonNull Runnable abrirHistorial,
                          @NonNull Runnable alCargar) {
        this.fragment = fragment;
        this.raiz = raiz;
        this.alCargar = alCargar;
        this.estado = new VistaEstado(raiz.findViewById(R.id.estadoEntrenos));
        raiz.findViewById(R.id.enlaceEntrenos).setOnClickListener(v -> abrirHistorial.run());
    }

    /** Pide la primera página del historial. Lo que ya se ve se queda hasta que llega lo nuevo. */
    public void cargar() {
        int esta = ++tanda;
        if (pagina == null) {
            raiz.findViewById(R.id.scrollEntrenos).setVisibility(View.GONE);
            estado.cargando();
        }
        sesionApi.historial(null).enqueue(new ApiCallback<PaginaHistorial>() {
            @Override
            public void onOk(PaginaHistorial p) {
                if (!fragment.isAdded() || esta != tanda) return;
                pagina = p;
                pintar();
                alCargar.run();
            }

            @Override
            public void onFail(int code, String message) {
                if (!fragment.isAdded() || esta != tanda) return;
                if (pagina != null) return;   // Lo que había se queda.
                raiz.findViewById(R.id.scrollEntrenos).setVisibility(View.GONE);
                estado.error(VistaEstado.mensaje(fragment.requireContext(), R.string.inicio_entrenos_error, code,
                        message), InicioEntrenos.this::cargar);
            }
        });
    }

    /** Los récords que batió la última sesión terminada; -1 mientras no se sabe. */
    public int recordsDeLaUltima() {
        if (pagina == null) return -1;
        List<PaginaHistorial.Sesion> s = pagina.sesiones();
        return s.isEmpty() ? 0 : s.get(0).getRecords();
    }

    // ── Pintar ───────────────────────────────────────────────────────────────

    private void pintar() {
        List<PaginaHistorial.Sesion> todas = pagina == null ? java.util.Collections.emptyList() : pagina.sesiones();
        View scroll = raiz.findViewById(R.id.scrollEntrenos);
        if (todas.isEmpty()) {
            scroll.setVisibility(View.GONE);
            estado.vacio(R.string.inicio_entrenos_vacio);
            return;
        }
        estado.oculto();
        scroll.setVisibility(View.VISIBLE);
        TextosTarjeta textos = new TextosTarjeta(fragment.requireContext());
        LinearLayout lista = raiz.findViewById(R.id.listaEntrenos);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(fragment.requireContext());
        for (PaginaHistorial.Sesion s : todas.subList(0, Math.min(MAXIMO, todas.size()))) {
            View tarjeta = inflater.inflate(R.layout.item_ultimo_entreno, lista, false);
            lista.addView(tarjeta);
            rellenar(tarjeta, s, textos);
        }
    }

    private void rellenar(View tarjeta, PaginaHistorial.Sesion s, TextosTarjeta textos) {
        String cuando = textos.cuandoRelativo(s.getFechaInicio());
        String rutina = textos.titulo(s);
        ((TextView) tarjeta.findViewById(R.id.tvEntrenoCuando)).setText(cuando);
        ((TextView) tarjeta.findViewById(R.id.tvEntrenoRutina)).setText(rutina);
        TextView kilos = tarjeta.findViewById(R.id.tvEntrenoKilos);
        BarrasVecesView barras = tarjeta.findViewById(R.id.barrasEntreno);
        TextView destaca = tarjeta.findViewById(R.id.tvEntrenoExtra);

        StringBuilder a11y = new StringBuilder(rutina);
        String cuandoA11y = textos.cuandoA11y(s.getFechaInicio());
        if (!cuandoA11y.isEmpty()) a11y.append(", ").append(cuandoA11y);
        a11y.append(". ");
        CifraTarjeta cifra = textos.cifra(s);
        textos.pintarCifra(kilos, cifra, entrada, "s:" + s.getId(), R.style.TextAppearance_GymProFit_Cifra_Pequena,
                R.dimen.text_tarjeta_kg_carrusel, barras,
                s.esLibre() ? java.util.Collections.emptyList() : s.getBarras());
        a11y.append(textos.cifraA11y(cifra));
        if (cifra.esAviso()) {
            // Un aviso: debajo, los minutos (si se saben), sin icono y en el texto secundario.
            String minutos = textos.minutos(s);
            // En la letra de apoyo, no en la de la línea destacada: no es un dato que destaque.
            destaca.setTextAppearance(R.style.TextAppearance_GymProFit_Apoyo_Pequeno);
            textos.pintarNeutra(destaca, minutos);
            a11y.append('.');
            if (!minutos.isEmpty()) a11y.append(' ').append(minutos).append('.');
        } else if (s.getRecords() > 0) {
            // Una sola línea destacada: el récord manda sobre la comparación.
            textos.pintarRecords(destaca, s.getRecords());
            a11y.append(fragment.getString(R.string.enumeracion_y)).append(destaca.getText()).append('.');
        } else {
            // Sin kilos no hay comparación: entonces, sin línea destacada.
            ComparacionSesion c = textos.comparacion(s);
            textos.pintarComparacion(destaca, c);
            if (c != null) a11y.append(", ").append(Character.toLowerCase(destaca.getText().charAt(0)))
                    .append(destaca.getText().subSequence(1, destaca.getText().length()));
            a11y.append('.');
        }
        String b = s.esLibre() || !cifra.conKilos() ? null : textos.barrasA11y(s.getBarras());
        if (b != null) a11y.append(' ').append(b).append('.');
        tarjeta.setContentDescription(a11y.toString());
        tarjeta.setOnClickListener(v -> abrirResumen(s, v));
    }

    private void abrirResumen(PaginaHistorial.Sesion s, View tarjeta) {
        Intent intent = new Intent(fragment.requireContext(), ResumenSesionActivity.class);
        intent.putExtra("sesionId", s.getId());
        Transiciones.abrirTarjeta(fragment.requireActivity(), tarjeta, intent);
    }
}
