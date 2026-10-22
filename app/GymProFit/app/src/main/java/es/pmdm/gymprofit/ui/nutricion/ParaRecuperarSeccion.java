package es.pmdm.gymprofit.ui.nutricion;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Favoritos;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.comida.CantidadAnterior;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.plato.LoMio;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.network.PlatoApi;
import es.pmdm.gymprofit.utils.AnadirRapido;
import es.pmdm.gymprofit.utils.Anadidos;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.Cantidades;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoQueTeEncaja;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.ParaRecuperar;
import es.pmdm.gymprofit.utils.PedidoCantidad;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ParaRecuperarSeccion — «Para recuperar» del resumen del entreno (GP-210 paso 1,
// tablero 3, con el contenido del tablero 11 de nutrición y su decisión 12).
//
// Al acabar de entrenar, si a la proteína del día le faltan 20 g o más (ParaRecuperar),
// enseña lo que falta, su barra y hasta tres ideas con su «+», que se apuntan en la comida
// que toca ahora. Las ideas salen de lo mismo que «Lo que te encaja» de Añadir
// (LoQueTeEncaja, sin IA): tus platos, tus favoritos y lo que sueles tomar en esa comida,
// ordenado por la proteína que cubre. Los objetivos del día NO cambian: apuntar solo suma
// a lo comido y la barra se acerca al objetivo de siempre.
//
// El «+» y su ✓ son los de Añadir (Anadidos, momento 15): cambian al tocar, la API va
// detrás y, si falla, la fila vuelve a como estaba y se dice. Tocar el ✓ quita la línea.
// Sin ideas que quepan, sin objetivo o con la proteína a menos de 20 g de llegar, la
// sección no ocupa sitio. Cargando y error, con VistaEstado dentro de la tarjeta.
// ============================================================
public final class ParaRecuperarSeccion {

    private final AppCompatActivity activity;
    private final MaterialCardView card;
    private final PreferencesManager prefs;
    private final VistaEstado estado;

    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final AlimentoComidaApi lineaApi = ApiClient.service(AlimentoComidaApi.class);
    private final PlatoApi platoApi = ApiClient.service(PlatoApi.class);
    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);

    private final String tipo = ComidaQueToca.ahora();
    private final String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

    // Lo cargado, para pintar cuando llegue todo.
    private List<Comida> comidasHoy;
    private LoMio loMio;
    private Favoritos favoritos;
    private List<Alimento> habituales;
    private final Set<Integer> yaApuntados = new HashSet<>();

    // Lo pintado: la proteína que había y la que suman las ideas apuntadas desde aquí.
    private double proteinaBase;
    private int objetivoProteinas;
    private double proteinaMostrada = -1;
    private final Map<String, LoQueTeEncaja.Sugerencia> porClave = new HashMap<>();
    private final Map<String, View> filas = new HashMap<>();
    private Anadidos anadidos;
    private int turno;

    /**
     * @param activity quien la enseña (destruida, deja de pintar).
     * @param card     la raíz de view_para_recuperar.
     * @param prefs    el perfil, para el objetivo de proteína.
     */
    public ParaRecuperarSeccion(@NonNull AppCompatActivity activity, @NonNull MaterialCardView card,
                                @NonNull PreferencesManager prefs) {
        this.activity = activity;
        this.card = card;
        this.prefs = prefs;
        this.estado = new VistaEstado(card.findViewById(R.id.estadoRecuperar));
    }

    /** Carga lo necesario y, si toca, enseña la sección. Se puede volver a llamar (reintentar). */
    public void cargar() {
        ResultadoNutricional objetivo = DiaNutricion.objetivo(prefs);
        if (objetivo == null) {
            // Sin peso ni altura no hay objetivo de proteína que completar (GP-103).
            card.setVisibility(View.GONE);
            return;
        }
        int miTurno = ++turno;
        comidasHoy = null;
        loMio = null;
        favoritos = null;
        habituales = null;
        yaApuntados.clear();
        AtomicInteger pendientes = new AtomicInteger(4);
        boolean[] fallo = {false};
        estadoCargando();

        comidaApi.getDeUsuarioFecha(prefs.getUsuarioId(), hoy).enqueue(new ApiCallback<List<Comida>>() {
            @Override public void onOk(List<Comida> lista) {
                comidasHoy = lista != null ? lista : new ArrayList<>();
                cargarLineasDeLaComida(miTurno, () -> llego(miTurno, pendientes, fallo, objetivo));
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                llego(miTurno, pendientes, fallo, objetivo);
            }
        });
        platoApi.loMio().enqueue(new ApiCallback<LoMio>() {
            @Override public void onOk(LoMio m) {
                loMio = m != null ? m : new LoMio();
                llego(miTurno, pendientes, fallo, objetivo);
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                llego(miTurno, pendientes, fallo, objetivo);
            }
        });
        alimentoApi.favoritos().enqueue(new ApiCallback<Favoritos>() {
            @Override public void onOk(Favoritos f) {
                favoritos = f != null ? f : new Favoritos();
                llego(miTurno, pendientes, fallo, objetivo);
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                llego(miTurno, pendientes, fallo, objetivo);
            }
        });
        comidaApi.habituales(tipo).enqueue(new ApiCallback<List<Alimento>>() {
            @Override public void onOk(List<Alimento> lista) {
                habituales = lista != null ? lista : new ArrayList<>();
                llego(miTurno, pendientes, fallo, objetivo);
            }
            @Override public void onFail(int code, String message) {
                // Solo ordena las ideas («lo que sueles tomar»): sin ello salen igual, por la
                // proteína que cubren. No es un fallo de la sección.
                habituales = new ArrayList<>();
                llego(miTurno, pendientes, fallo, objetivo);
            }
        });
    }

    // Las líneas de la comida de ahora, para no proponer lo que ya lleva. Si falla se sigue sin
    // ellas: la sección sale igual, como la de Añadir.
    private void cargarLineasDeLaComida(int miTurno, Runnable alAcabar) {
        Comida esta = null;
        for (Comida c : comidasHoy) if (tipo.equals(c.getTipoComida())) esta = c;
        if (esta == null || esta.getId() <= 0) {
            alAcabar.run();
            return;
        }
        lineaApi.getDeComida(esta.getId()).enqueue(new ApiCallback<List<AlimentoComida>>() {
            @Override public void onOk(List<AlimentoComida> lineas) {
                if (miTurno == turno && lineas != null) for (AlimentoComida l : lineas) yaApuntados.add(l.getAlimentoId());
                alAcabar.run();
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: sin las líneas se propone sin filtrar lo ya apuntado.
                alAcabar.run();
            }
        });
    }

    private void llego(int miTurno, AtomicInteger pendientes, boolean[] fallo, ResultadoNutricional objetivo) {
        if (miTurno != turno || activity.isDestroyed()) return;
        if (pendientes.decrementAndGet() > 0) return;
        if (fallo[0]) {
            estadoError();
            return;
        }
        pintar(objetivo);
    }

    // ── Estados ─────────────────────────────────────────────────────────────

    private void ocultarContenido(boolean ocultar) {
        int v = ocultar ? View.GONE : View.VISIBLE;
        card.findViewById(R.id.cabeceraRecuperar).setVisibility(v);
        card.findViewById(R.id.barraRecuperar).setVisibility(v);
        card.findViewById(R.id.listaRecuperar).setVisibility(v);
        card.findViewById(R.id.tvNotaRecuperar).setVisibility(v);
    }

    private void estadoCargando() {
        // Mientras carga no se sabe si habrá sección: no se enseña nada que luego desaparezca.
        card.setVisibility(View.GONE);
    }

    private void estadoError() {
        card.setVisibility(View.VISIBLE);
        ocultarContenido(true);
        estado.error(activity.getString(R.string.recuperar_error), this::cargar);
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    private void pintar(ResultadoNutricional objetivo) {
        DiaNutricion d = DiaNutricion.de(comidasHoy, objetivo);
        if (!ParaRecuperar.sale(d.objetivoProteinas, d.proteinas, d.sinObjetivo)) {
            card.setVisibility(View.GONE);
            return;
        }
        List<Alimento> suelen = habituales != null ? habituales : new ArrayList<>();
        Set<Integer> suele = new HashSet<>();
        for (Alimento a : suelen) suele.add(a.getId());
        LoQueTeEncaja.Resultado r = LoQueTeEncaja.de(d.kcalRestantes(), d.objetivoProteinas - d.proteinas,
                loMio != null ? loMio.getPlatos() : null, favoritos != null ? favoritos.getFavoritos() : null,
                suelen, suele, null, yaApuntados);
        List<LoQueTeEncaja.Sugerencia> ideas = new ArrayList<>();
        if (r != null) {
            // Solo lo que aporta proteína de verdad: una lechuga de 1 g no ayuda a recuperar.
            for (LoQueTeEncaja.Sugerencia s : r.sugerencias) if (ParaRecuperar.sirveDeIdea(s.proteina)) ideas.add(s);
        }
        if (ideas.isEmpty()) {
            // Sin ideas que quepan en las kcal que quedan y aporten proteína no hay nada que apuntar.
            card.setVisibility(View.GONE);
            return;
        }

        proteinaBase = d.proteinas;
        objetivoProteinas = d.objetivoProteinas;
        proteinaMostrada = -1;
        porClave.clear();
        filas.clear();
        anadidos = new Anadidos(servidor(), oyente());

        estado.oculto();
        ocultarContenido(false);
        card.setVisibility(View.VISIBLE);
        ((TextView) card.findViewById(R.id.tvNotaRecuperar)).setText(notaDe(tipo));

        LinearLayout lista = card.findViewById(R.id.listaRecuperar);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < ideas.size(); i++) {
            LoQueTeEncaja.Sugerencia s = ideas.get(i);
            View fila = inflater.inflate(R.layout.item_idea_recuperar, lista, false);
            pintarFila(fila, s, i == ideas.size() - 1);
            String clave = Anadidos.clave(tipo, s.alimento.getId(), s.alimento.getBarcode());
            porClave.put(clave, s);
            filas.put(clave, fila);
            fila.findViewById(R.id.btnIdeaMas).setOnClickListener(v -> tocar(clave));
            lista.addView(fila);
        }
        actualizarProteina(false);
        if (!Movimiento.quieto(activity)) Movimiento.entrarUna(card, 0, Movimiento.ENTRA, 16);
    }

    private void pintarFila(View fila, LoQueTeEncaja.Sugerencia s, boolean ultima) {
        ImageView icono = fila.findViewById(R.id.ivIdeaRecuperar);
        icono.setImageResource(s.alimento.esPlato() ? R.drawable.ic_ms_menu_book : R.drawable.ic_ms_nutrition);
        icono.setImageTintList(ColorStateList.valueOf(attr(com.google.android.material.R.attr.colorOnSurfaceVariant)));
        ((TextView) fila.findViewById(R.id.tvIdeaNombre)).setText(s.alimento.getNombre());
        String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(activity), FechaUtils.localeDeLaApp(activity),
                s.cantidad);
        String detalle = activity.getString(R.string.recuperar_idea_detalle, cantidad,
                (int) Math.round(s.proteina), (int) s.kcal);
        ((TextView) fila.findViewById(R.id.tvIdeaDetalle)).setText(detalle);
        fila.findViewById(R.id.rayaIdea).setVisibility(ultima ? View.GONE : View.VISIBLE);
        pintarMas(fila, false, s.alimento.getNombre(), detalle);
    }

    // El «+» o el ✓ de una fila, con el nombre que oye TalkBack.
    private void pintarMas(View fila, boolean marcado, String nombre, String detalle) {
        View boton = fila.findViewById(R.id.btnIdeaMas);
        ImageView icono = fila.findViewById(R.id.ivIdeaMas);
        boton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity,
                marcado ? R.color.gp_success_container : R.color.gp_surface_2)));
        icono.setImageResource(marcado ? R.drawable.ic_ms_check : R.drawable.ic_ms_add);
        icono.setImageTintList(ColorStateList.valueOf(marcado ? ContextCompat.getColor(activity, R.color.gp_success)
                : attr(com.google.android.material.R.attr.colorOnSurface)));
        boton.setContentDescription(marcado
                ? activity.getString(R.string.recuperar_idea_quitar, nombre.toLowerCase(FechaUtils.localeDeLaApp(activity)))
                : activity.getString(R.string.recuperar_idea_apuntar,
                nombre.toLowerCase(FechaUtils.localeDeLaApp(activity)), detalle));
    }

    // ── El «+» ──────────────────────────────────────────────────────────────

    private void tocar(String clave) {
        LoQueTeEncaja.Sugerencia s = porClave.get(clave);
        if (s == null) return;
        CantidadFicha c = s.cantidad;
        String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(activity), FechaUtils.localeDeLaApp(activity), c);
        // El pedido es el mismo que el «+» de Añadir: la última cantidad, a la comida que toca.
        // Anadidos cambia la fila al momento (oyente.cambio) y la API va detrás.
        anadidos.tocar(new Anadidos.Pedido(s.alimento.getId(), s.alimento.getBarcode(), tipo,
                PedidoCantidad.anadir(s.alimento, c, hoy, tipo), cantidad, s.kcal));
    }

    // Pinta la fila como dice Anadidos y, si cambió de estado, con el momento 15.
    private void refrescarFila(String clave, boolean animar) {
        LoQueTeEncaja.Sugerencia s = porClave.get(clave);
        View fila = filas.get(clave);
        if (s == null || fila == null) return;
        boolean marcado = anadidos.marcado(clave);
        String detalle = ((TextView) fila.findViewById(R.id.tvIdeaDetalle)).getText().toString();
        if (animar) {
            Movimiento.volverMas(fila.findViewById(R.id.btnIdeaMas), fila.findViewById(R.id.ivIdeaMas),
                    ContextCompat.getColor(activity, marcado ? R.color.gp_surface_2 : R.color.gp_success_container),
                    ContextCompat.getColor(activity, marcado ? R.color.gp_success_container : R.color.gp_surface_2));
        }
        pintarMas(fila, marcado, s.alimento.getNombre(), detalle);
    }

    private Anadidos.Servidor servidor() {
        return new Anadidos.Servidor() {
            @Override
            public void anadir(@NonNull Anadidos.Pedido pedido, @NonNull Anadidos.Respuesta<AnadirRespuesta> r) {
                comidaApi.anadir(pedido.cuerpo).enqueue(new ApiCallback<AnadirRespuesta>() {
                    @Override public void onOk(AnadirRespuesta respuesta) { r.ok(respuesta); }
                    @Override public void onFail(int code, String message) { r.fallo(code, message); }
                });
            }

            @Override
            public void deshacer(@NonNull Anadidos.Anadido a, @NonNull Anadidos.Respuesta<Void> r) {
                CantidadAnterior antes = a.getAnterior();
                if (antes == null) {
                    // La línea era nueva: se borra.
                    lineaApi.eliminar(a.getLinea().getId()).enqueue(new ApiCallback<Void>() {
                        @Override public void onOk(Void nada) { r.ok(null); }
                        @Override public void onFail(int code, String message) { r.fallo(code, message); }
                    });
                    return;
                }
                // Se había sumado a una línea que ya estaba: vuelve a la cantidad que tenía.
                Map<String, Object> cuerpo = new HashMap<>();
                cuerpo.put("cantidadGramos", decimal(antes.getCantidadGramos()));
                if (antes.getRacionId() != null && antes.getRaciones() != null) {
                    cuerpo.put("racionId", antes.getRacionId());
                    cuerpo.put("raciones", decimal(antes.getRaciones()));
                }
                lineaApi.patchLinea(a.getLinea().getId(), cuerpo).enqueue(new ApiCallback<AlimentoComida>() {
                    @Override public void onOk(AlimentoComida linea) { r.ok(null); }
                    @Override public void onFail(int code, String message) { r.fallo(code, message); }
                });
            }
        };
    }

    private Anadidos.Oyente oyente() {
        return new Anadidos.Oyente() {
            @Override
            public void cambio(@NonNull String clave) {
                if (activity.isDestroyed()) return;
                refrescarFila(clave, true);
                actualizarProteina(true);
            }

            @Override
            public void fallo(@NonNull String clave, boolean alAnadir, int code, @Nullable String message) {
                // Anadidos ya devolvió la fila a como estaba (cambio llegó antes): solo se dice.
                if (activity.isDestroyed()) return;
                LoQueTeEncaja.Sugerencia s = porClave.get(clave);
                String nombre = s != null ? s.alimento.getNombre() : "";
                UIHelper.mostrarToastError(activity, activity.getString(
                        alAnadir ? R.string.anadir_fallo_anadir : R.string.anadir_fallo_quitar, nombre,
                        UiFeedback.mensaje(activity, code, message)));
            }
        };
    }

    // ── La proteína ─────────────────────────────────────────────────────────

    private double proteinaActual() {
        double extra = 0;
        for (Map.Entry<String, LoQueTeEncaja.Sugerencia> e : porClave.entrySet()) {
            if (anadidos.marcado(e.getKey())) extra += e.getValue().proteina;
        }
        return proteinaBase + extra;
    }

    private void actualizarProteina(boolean animar) {
        double total = proteinaActual();
        double desde = proteinaMostrada < 0 ? proteinaBase : proteinaMostrada;
        proteinaMostrada = total;
        boolean conseguida = ParaRecuperar.conseguida(objetivoProteinas, total);
        int falta = ParaRecuperar.falta(objetivoProteinas, total);
        int redondeada = (int) Math.round(total);

        TextView titulo = card.findViewById(R.id.tvTituloRecuperar);
        titulo.setText(conseguida ? activity.getString(R.string.recuperar_titulo_conseguida)
                : activity.getString(R.string.recuperar_titulo_falta, falta));
        int oro = ContextCompat.getColor(activity, R.color.gp_gold);
        int oroTexto = ContextCompat.getColor(activity, R.color.gp_gold_text);
        titulo.setTextColor(conseguida ? oroTexto : attr(com.google.android.material.R.attr.colorOnSurface));
        card.setStrokeColor(conseguida ? oro : ContextCompat.getColor(activity, R.color.gp_divider));
        // El borde de la tarjeta sin conseguir es el de todas las demás (el del tema).
        if (!conseguida) card.setStrokeColor(attr(com.google.android.material.R.attr.colorOutline));

        ImageView icono = card.findViewById(R.id.ivIconoRecuperar);
        icono.setImageResource(conseguida ? R.drawable.ic_ms_workspace_premium_fill : R.drawable.ic_ms_restaurant_fill);
        icono.setImageTintList(ColorStateList.valueOf(conseguida ? oro : attr(androidx.appcompat.R.attr.colorPrimary)));
        card.findViewById(R.id.fondoIconoRecuperar).setBackgroundTintList(ColorStateList.valueOf(conseguida
                ? ContextCompat.getColor(activity, R.color.gp_gold_container)
                : attr(com.google.android.material.R.attr.colorPrimaryContainer)));

        LinearProgressIndicator barra = card.findViewById(R.id.progresoRecuperar);
        barra.setIndicatorColor(conseguida ? oro : ContextCompat.getColor(activity, R.color.gp_macro_proteinas));
        boolean mover = animar && !Movimiento.quieto(activity);
        barra.setProgressCompat(DiaNutricion.porcentaje(total, objetivoProteinas), mover);

        TextView valor = card.findViewById(R.id.tvProteinaRecuperar);
        if (mover) {
            Movimiento.contar(0, Movimiento.BARRA, f -> valor.setText(activity.getString(
                    R.string.recuperar_barra_valor, (int) Math.round(desde + (total - desde) * f), objetivoProteinas)));
        } else {
            valor.setText(activity.getString(R.string.recuperar_barra_valor, redondeada, objetivoProteinas));
        }
        card.findViewById(R.id.barraRecuperar).setContentDescription(conseguida
                ? activity.getString(R.string.recuperar_barra_a11y_conseguida, redondeada, objetivoProteinas)
                : activity.getString(R.string.recuperar_barra_a11y_falta, redondeada, objetivoProteinas, falta));
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private static int notaDe(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.recuperar_nota_desayuno;
            case "ALMUERZO": return R.string.recuperar_nota_almuerzo;
            case "COMIDA":   return R.string.recuperar_nota_comida;
            case "MERIENDA": return R.string.recuperar_nota_merienda;
            default:         return R.string.recuperar_nota_cena;
        }
    }

    private int attr(int atributo) {
        android.util.TypedValue tv = new android.util.TypedValue();
        activity.getTheme().resolveAttribute(atributo, tv, true);
        return tv.data;
    }

    private static BigDecimal decimal(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
