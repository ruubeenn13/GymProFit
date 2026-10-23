package es.pmdm.gymprofit.ui.inicio;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.widget.SiluetaMuscularView;
import es.pmdm.gymprofit.utils.Enumeracion;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.Musculos;
import es.pmdm.gymprofit.utils.Pauta;
import es.pmdm.gymprofit.utils.Zonas;
import es.pmdm.gymprofit.utils.ZonasDelDia;

// ============================================================
// InicioHoyDetalle — lo que «Hoy toca» enseña de la rutina de hoy (GP-216, tablero 13): sus
// ejercicios en un carrusel y, en el cuerpo, las zonas que trabaja.
//
// Los ejercicios y su pauta salen de la rutina (ya vienen en la del programa; la propia los pide).
// Las zonas salen del músculo de cada ejercicio, que la rutina no trae: se pide ese ejercicio
// al catálogo (una vez por ejercicio y sesión de la app, se guarda) y Musculos lo reduce a la clave
// de la silueta, como hace la API. Los iconos son los de la zona, provisionales hasta tener
// ilustraciones. De la última vez de cada ejercicio (GET /sesiones/ultima-vez) sale la marca que se
// enseña junto a la pauta; la API no da un peso objetivo y aquí no se inventa.
// ============================================================
public final class InicioHoyDetalle {

    /** Lo que el resto de la tarjeta necesita saber de la rutina. */
    public interface AlInformar {
        /** Los ejercicios llegaron: el primero (para Profi). */
        void ejercicios(@Nullable String primerEjercicio);
        /** Las zonas llegaron: su texto («Pierna y core»), o null si ninguna se conoce. */
        void zonas(@Nullable String texto);
    }

    // Ejercicios del catálogo ya pedidos, por id: no cambian mientras la app está abierta.
    private static final Map<Integer, Ejercicio> CATALOGO = new HashMap<>();

    private final Fragment fragment;
    private final View raiz;
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    // Para ignorar respuestas de una rutina anterior.
    private int generacion;

    public InicioHoyDetalle(@NonNull Fragment fragment, @NonNull View raiz) {
        this.fragment = fragment;
        this.raiz = raiz;
    }

    /** Sin rutina de hoy (o con otra cosa que contar): se esconde todo. */
    public void ocultar() {
        generacion++;
        raiz.findViewById(R.id.scrollHoyEjercicios).setVisibility(View.GONE);
        raiz.findViewById(R.id.siluetaHoy).setVisibility(View.GONE);
    }

    /** Enseña los ejercicios y las zonas de esa rutina. */
    public void mostrar(@NonNull Rutina rutina, @NonNull AlInformar alInformar) {
        final int esta = ++generacion;
        if (rutina instanceof RutinaConEjercicios && ((RutinaConEjercicios) rutina).getEjercicios() != null) {
            ejerciciosListos(esta, ((RutinaConEjercicios) rutina).getEjercicios(), alInformar);
            return;
        }
        rutinaApi.getEjerciciosDeRutina(rutina.getId()).enqueue(new ApiCallback<List<RutinaEjercicio>>() {
            @Override
            public void onOk(List<RutinaEjercicio> lista) {
                ejerciciosListos(esta, lista, alInformar);
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: «Hoy toca» ya dice la rutina y se puede empezar sin el carrusel;
                // abrir la rutina pide sus ejercicios y enseña su fallo con reintentar.
                if (esta == generacion && fragment.isAdded()) ocultarCarrusel();
            }
        });
    }

    private void ocultarCarrusel() {
        raiz.findViewById(R.id.scrollHoyEjercicios).setVisibility(View.GONE);
        raiz.findViewById(R.id.siluetaHoy).setVisibility(View.GONE);
    }

    // ── Ejercicios ───────────────────────────────────────────────────────────

    private void ejerciciosListos(int esta, @Nullable List<RutinaEjercicio> lista, AlInformar alInformar) {
        if (esta != generacion || !fragment.isAdded()) return;
        List<RutinaEjercicio> ejercicios = ordenados(lista);
        if (ejercicios.isEmpty()) {
            ocultarCarrusel();
            return;
        }
        alInformar.ejercicios(ejercicios.get(0).getNombreEjercicio());

        LinearLayout carrusel = raiz.findViewById(R.id.listaHoyEjercicios);
        carrusel.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(fragment.requireContext());
        Pauta.Formatos formatos = Pauta.Formatos.de(fragment.requireContext());
        Map<Integer, View> tarjetas = new HashMap<>();
        for (RutinaEjercicio re : ejercicios) {
            View item = inflater.inflate(R.layout.item_hoy_ejercicio, carrusel, false);
            ((TextView) item.findViewById(R.id.tvNombreEjercicio)).setText(re.getNombreEjercicio());
            ((TextView) item.findViewById(R.id.tvMetaEjercicio)).setText(Pauta.texto(formatos, re.getSeries(),
                    re.getRepeticionesMin(), re.getRepeticionesMax(), re.getRepeticiones(), re.getMedida(),
                    re.getPorLado()));
            ((ImageView) item.findViewById(R.id.ivIconoEjercicio)).setImageResource(R.drawable.ic_ms_fitness_center);
            tarjetas.put(re.getEjercicioId(), item);
            carrusel.addView(item);
        }
        raiz.findViewById(R.id.scrollHoyEjercicios).setVisibility(View.VISIBLE);

        pedirZonas(esta, ejercicios, tarjetas, alInformar);
        pedirUltimaVez(esta, ejercicios, tarjetas, formatos);
    }

    // En el orden de la rutina.
    private static List<RutinaEjercicio> ordenados(@Nullable List<RutinaEjercicio> lista) {
        List<RutinaEjercicio> copia = new ArrayList<>();
        if (lista == null) return copia;
        copia.addAll(lista);
        java.util.Collections.sort(copia, (a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        return copia;
    }

    // ── Zonas ────────────────────────────────────────────────────────────────

    private void pedirZonas(int esta, List<RutinaEjercicio> ejercicios, Map<Integer, View> tarjetas,
                            AlInformar alInformar) {
        Set<Integer> faltan = new LinkedHashSet<>();
        for (RutinaEjercicio re : ejercicios) {
            if (!CATALOGO.containsKey(re.getEjercicioId())) faltan.add(re.getEjercicioId());
        }
        if (faltan.isEmpty()) {
            zonasListas(esta, ejercicios, tarjetas, alInformar);
            return;
        }
        final int[] pendientes = {faltan.size()};
        for (int id : faltan) {
            ejercicioApi.getPorId(id).enqueue(new ApiCallback<Ejercicio>() {
                @Override
                public void onOk(Ejercicio e) {
                    if (e != null) CATALOGO.put(id, e);
                    uno();
                }

                @Override
                public void onFail(int code, String message) {
                    // Se ignora a propósito: sin el músculo de ese ejercicio la silueta enciende los de los
                    // demás y su icono se queda genérico; nada de «Hoy toca» depende de ello.
                    uno();
                }

                private void uno() {
                    if (--pendientes[0] == 0) zonasListas(esta, ejercicios, tarjetas, alInformar);
                }
            });
        }
    }

    private void zonasListas(int esta, List<RutinaEjercicio> ejercicios, Map<Integer, View> tarjetas,
                             AlInformar alInformar) {
        if (esta != generacion || !fragment.isAdded()) return;
        List<ZonasDelDia.Ejercicio> deHoy = new ArrayList<>();
        for (RutinaEjercicio re : ejercicios) {
            Ejercicio e = CATALOGO.get(re.getEjercicioId());
            String musculo = e == null ? null : Musculos.normalizar(e.getMusculoPrimario(), e.getGrupoMuscular());
            deHoy.add(new ZonasDelDia.Ejercicio(musculo, re.getSeries()));
            View item = tarjetas.get(re.getEjercicioId());
            Zonas.Zona zona = Zonas.deMusculo(musculo);
            if (item != null && zona != null) {
                ((ImageView) item.findViewById(R.id.ivIconoEjercicio)).setImageResource(icono(zona));
            }
        }
        ZonasDelDia zonas = ZonasDelDia.de(deHoy);
        SiluetaMuscularView silueta = raiz.findViewById(R.id.siluetaHoy);
        if (zonas.zonas.isEmpty()) {
            silueta.setVisibility(View.GONE);
            alInformar.zonas(null);
            return;
        }
        List<String> nombres = new ArrayList<>();
        for (Zonas.Zona z : zonas.zonas) {
            if (nombres.size() < 3) nombres.add(fragment.getString(z.titulo));
        }
        String texto = Enumeracion.unir(nombres, fragment.getString(R.string.enumeracion_y));
        silueta.setVolumen(zonas.seriesPorMusculo);
        silueta.setContentDescription(fragment.getString(R.string.inicio_zonas_hoy_a11y, texto));
        silueta.setVisibility(View.VISIBLE);
        alInformar.zonas(texto);
    }

    @DrawableRes
    private static int icono(Zonas.Zona zona) {
        if (zona == Zonas.PIERNA) return R.drawable.ic_ms_directions_run;
        if (zona == Zonas.ESPALDA || zona == Zonas.CORE) return R.drawable.ic_ms_accessibility_new;
        return R.drawable.ic_ms_fitness_center;
    }

    // ── Última vez ───────────────────────────────────────────────────────────

    private void pedirUltimaVez(int esta, List<RutinaEjercicio> ejercicios, Map<Integer, View> tarjetas,
                                Pauta.Formatos formatos) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (RutinaEjercicio re : ejercicios) {
            if (ids.size() < 30) ids.add(re.getEjercicioId());
        }
        sesionApi.ultimaVez(android.text.TextUtils.join(",", ids)).enqueue(new ApiCallback<List<UltimaVez>>() {
            @Override
            public void onOk(List<UltimaVez> lista) {
                if (esta != generacion || !fragment.isAdded() || lista == null) return;
                Set<Integer> hechos = new HashSet<>();
                for (UltimaVez u : lista) {
                    BigDecimal mejor = mejorPeso(u);
                    View item = tarjetas.get(u.ejercicioId);
                    if (item == null || mejor == null || !hechos.add(u.ejercicioId)) continue;
                    TextView meta = item.findViewById(R.id.tvMetaEjercicio);
                    String marca = fragment.getString(R.string.inicio_ejercicio_ultima,
                            Marcas.kilos(fragment.requireContext(), mejor.doubleValue()).replace(' ', '\u00A0'));
                    meta.setText(fragment.getString(R.string.inicio_dos_lineas, meta.getText(), marca));
                }
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: sin la última marca el carrusel enseña solo la pauta.
            }
        });
    }

    // El mayor peso de las series de esa vez, o null si no hubo pesos (por tiempo o sin peso).
    @Nullable
    private static BigDecimal mejorPeso(UltimaVez u) {
        BigDecimal mejor = null;
        if (u.series == null) return null;
        for (UltimaVez.Serie s : u.series) {
            if (s.segundos != null || s.peso == null || s.peso.signum() <= 0) continue;
            if (mejor == null || s.peso.compareTo(mejor) > 0) mejor = s.peso;
        }
        return mejor;
    }
}
