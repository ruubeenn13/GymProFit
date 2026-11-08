package es.pmdm.gymprofit.ui.widget;

import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.VistaPrevia;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.Pauta;
import es.pmdm.gymprofit.utils.TiempoRutinas;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// SeguirProgramaHoja — «¿Cuánto tiempo tienes por sesión?» (GP-243, tablero Tiempo-1; antes
// lote 1.2.1).
//
// Seguir un programa, o cambiar el tiempo del que sigue: 30, 45, 60 y 75+ en el selector,
// con «ahora» bajo el que tiene. Cada elección pide a la API la vista previa (la app no
// aplica ninguna regla: VistaPrevia) y la hoja se rehace con ella: la frase de lo que cambia
// (TiempoRutinas.frase), una tarjeta por rutina con sus bloques y su línea, desplegable con
// sus ejercicios, la regla del recorte y el aviso de lo que pasa al pulsar. Los casos de
// siempre: seguir por primera vez, «Cambiar a», «Rehacer las rutinas», «Ya sigues otro»,
// cargando y error con reintento.
// ============================================================
public final class SeguirProgramaHoja {

    private SeguirProgramaHoja() { }

    private static final int[] MINUTOS = {30, 45, 60, 75};

    // Lo que mide la cifra y su «min» o «ahora» dentro de la opción (texto de 16 sp): 24 y 13 sp.
    private static final float CIFRA_OPCION = 1.5f;
    private static final float SUB_OPCION = 13f / 16f;
    // « min» junto a los minutos de una rutina: 14 sp sobre la cifra de 26.
    private static final float MIN_RUTINA = 14f / 26f;

    /**
     * @param codigo   programa que se va a seguir.
     * @param nombre   su nombre, para los avisos.
     * @param seguido  el que sigue ahora, o null.
     * @param alSeguir se llama cuando la API lo ha creado.
     */
    public static void mostrar(FragmentActivity act, String codigo, String nombre,
                               @Nullable ProgramaQueSigue seguido, Runnable alSeguir) {
        ProgramaApi api = ApiClient.service(ProgramaApi.class);
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_seguir_programa, null, false);
        hoja.setContentView(raiz);
        // Con un alto mínimo del 85 % de la ventana: al elegir otros minutos la vista previa cambia de
        // alto y, ajustada a su contenido, la hoja crecía hacia arriba y el selector se movía bajo el
        // dedo (revisor, GP-225). Así sube desde abajo, con sus esquinas, y no cambia de alto.
        raiz.setMinimumHeight(Math.round(act.getResources().getDisplayMetrics().heightPixels * 0.85f));
        hoja.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        hoja.getBehavior().setSkipCollapsed(true);

        boolean mismo = seguido != null && seguido.getPrograma() != null
                && codigo.equals(seguido.getPrograma().getCodigo());
        int actuales = mismo ? seguido.getMinutos() : 0;
        ((TextView) raiz.findViewById(R.id.tvSeguirPrograma)).setText(mismo
                ? act.getString(R.string.seguir_tu_programa, nombre) : nombre);

        SelectorPildora chips = raiz.findViewById(R.id.chipsMinutos);
        CharSequence[] textos = new CharSequence[MINUTOS.length];
        CharSequence[] nombres = new CharSequence[MINUTOS.length];
        TiempoRutinas.Textos t = TiempoRutinas.Textos.de(act);
        for (int i = 0; i < MINUTOS.length; i++) {
            boolean ahora = MINUTOS[i] == actuales;
            textos[i] = opcion(MINUTOS[i] == 75 ? "75+" : String.valueOf(MINUTOS[i]),
                    act.getString(ahora ? R.string.seguir_tiempo_ahora : R.string.seguir_tiempo_min));
            String minutos = t.minutos(MINUTOS[i]);
            nombres[i] = ahora ? act.getString(R.string.seguir_tiempo_ahora_a11y, minutos) : minutos;
        }
        chips.setOpciones(textos, nombres);
        int inicial = mismo ? actuales : 60;
        for (int i = 0; i < MINUTOS.length; i++) {
            if (MINUTOS[i] == inicial) chips.setElegida(i, false);
        }

        View boton = raiz.findViewById(R.id.btnConfirmarSeguir);
        VistaEstado estado = new VistaEstado(raiz.findViewById(R.id.estadoVistaPrevia));
        // Solo cuenta la respuesta de la última elección: cambiar rápido de opción no mezcla.
        int[] pedida = {0};

        Runnable[] cargar = new Runnable[1];
        cargar[0] = () -> {
            int minutos = MINUTOS[Math.max(0, chips.getElegida())];
            ((TextView) boton).setText(act.getString(textoBoton(mismo, actuales, minutos),
                    act.getString(R.string.duracion_min, minutos)));
            int esta = ++pedida[0];
            estado.cargando();
            ((LinearLayout) raiz.findViewById(R.id.listaVistaPrevia)).removeAllViews();
            raiz.findViewById(R.id.tvFraseTiempo).setVisibility(View.GONE);
            raiz.findViewById(R.id.tvAjustes).setVisibility(View.GONE);
            boton.setEnabled(false);
            api.vistaPrevia(codigo, minutos).enqueue(new ApiCallback<VistaPrevia>() {
                @Override public void onOk(VistaPrevia vista) {
                    if (esta != pedida[0] || !hoja.isShowing()) return;
                    estado.oculto();
                    pintarVista(act, raiz, vista, minutos);
                    pintarAviso(act, raiz, mismo, seguido, vista);
                    boton.setEnabled(true);
                }
                @Override public void onFail(int code, String message) {
                    if (esta != pedida[0] || !hoja.isShowing()) return;
                    estado.error(VistaEstado.mensaje(act, R.string.seguir_error_vista, code, message), cargar[0]);
                }
            });
        };
        chips.setAlElegir(i -> cargar[0].run());

        boton.setOnClickListener(v -> {
            int minutos = MINUTOS[Math.max(0, chips.getElegida())];
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("minutos", minutos);
            LoadingDialog.show(act);
            api.seguir(codigo, cuerpo).enqueue(new ApiCallback<Void>() {
                @Override public void onOk(Void body) {
                    LoadingDialog.hide(act);
                    hoja.dismiss();
                    UIHelper.mostrarToastExito(act, act.getString(R.string.seguir_hecho, nombre));
                    alSeguir.run();
                }
                @Override public void onFail(int code, String message) {
                    // La hoja sigue abierta con lo elegido: se puede volver a pulsar.
                    LoadingDialog.hide(act);
                    UiFeedback.toastError(act, code, message);
                }
            });
        });

        hoja.show();
        cargar[0].run();
    }

    // «30» grande y «min» (o «ahora») debajo, en una opción del selector.
    private static CharSequence opcion(String cifra, String sub) {
        SpannableString s = new SpannableString(cifra + "\n" + sub);
        s.setSpan(new RelativeSizeSpan(CIFRA_OPCION), 0, cifra.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new RelativeSizeSpan(SUB_OPCION), cifra.length() + 1, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    // El aviso de lo que pasa al pulsar: se rehacen tus rutinas, o dejas de seguir el otro.
    private static void pintarAviso(FragmentActivity act, View raiz, boolean mismo,
                                    @Nullable ProgramaQueSigue seguido, VistaPrevia vista) {
        View aviso = raiz.findViewById(R.id.avisoSeguir);
        String titulo = null;
        String texto = null;
        int icono = R.drawable.ic_ms_restart_alt;
        if (mismo) {
            int n = vista.getRutinas() != null ? vista.getRutinas().size() : 0;
            titulo = act.getResources().getQuantityString(R.plurals.seguir_rehacen_titulo, n, n);
            texto = act.getString(R.string.seguir_rehacen_texto);
        } else if (seguido != null && seguido.getPrograma() != null) {
            titulo = act.getString(R.string.seguir_otro_titulo, seguido.getPrograma().getNombre());
            texto = act.getString(R.string.seguir_otro_texto);
            icono = R.drawable.ic_ms_autorenew;
        }
        if (titulo == null) {
            aviso.setVisibility(View.GONE);
            return;
        }
        ((ImageView) raiz.findViewById(R.id.ivAvisoSeguir)).setImageResource(icono);
        ((TextView) raiz.findViewById(R.id.tvAvisoTitulo)).setText(titulo);
        ((TextView) raiz.findViewById(R.id.tvAvisoSeguir)).setText(texto);
        aviso.setContentDescription(titulo + ". " + texto);
        aviso.setVisibility(View.VISIBLE);
    }

    /**
     * El texto del botón.
     *
     * @param mismo    si es el programa que ya sigue.
     * @param actuales los minutos que tiene ahora (solo cuentan si es el mismo).
     * @param elegidos los minutos elegidos en la hoja.
     * @return el recurso del texto; el de «Cambiar a» lleva los minutos de argumento.
     */
    static int textoBoton(boolean mismo, int actuales, int elegidos) {
        if (!mismo) return R.string.seguir_boton;
        // Con los mismos minutos no cambia el tiempo: lo que hace es rehacer las rutinas (GP-135).
        return actuales == elegidos ? R.string.seguir_boton_rehacer : R.string.seguir_boton_mismo;
    }

    // La frase, una tarjeta por rutina y el ajuste del perfil.
    private static void pintarVista(FragmentActivity act, View raiz, VistaPrevia vista, int minutos) {
        TiempoRutinas.Textos t = TiempoRutinas.Textos.de(act);
        TextView frase = raiz.findViewById(R.id.tvFraseTiempo);
        frase.setText(TiempoRutinas.frase(t, minutos, vista.getRutinas()));
        frase.setVisibility(View.VISIBLE);

        LinearLayout lista = raiz.findViewById(R.id.listaVistaPrevia);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(act);
        List<View> tarjetas = new ArrayList<>();
        if (vista.getRutinas() != null) {
            for (VistaPrevia.Rutina r : vista.getRutinas()) {
                View tarjeta = inflater.inflate(R.layout.item_vista_previa_rutina, lista, false);
                pintarRutina(act, t, tarjeta, r);
                lista.addView(tarjeta);
                tarjetas.add(tarjeta);
            }
        }
        // Momento 3: las tarjetas entran una tras otra; con «Quitar animaciones», quietas.
        Movimiento.entrar(0, tarjetas.toArray(new View[0]));

        List<String> ajustes = new ArrayList<>();
        if (vista.getAjustes() != null) {
            if (vista.getAjustes().contains(VistaPrevia.AJUSTE_AVANZADO)) {
                ajustes.add(act.getString(R.string.seguir_ajuste_avanzado));
            }
            if (vista.getAjustes().contains(VistaPrevia.AJUSTE_FUERZA)) {
                ajustes.add(act.getString(R.string.seguir_ajuste_fuerza));
            }
        }
        TextView tvAjustes = raiz.findViewById(R.id.tvAjustes);
        tvAjustes.setText(String.join(" ", ajustes));
        tvAjustes.setVisibility(ajustes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // Una rutina: nombre, minutos, bloques, línea y, desplegada, sus ejercicios.
    private static void pintarRutina(FragmentActivity act, TiempoRutinas.Textos t, View tarjeta,
                                     VistaPrevia.Rutina r) {
        String nombre = r.getNombre() != null ? r.getNombre() : "";
        String linea = TiempoRutinas.linea(t, r);
        ((TextView) tarjeta.findViewById(R.id.tvVistaNombre)).setText(nombre);
        String cifra = String.valueOf(r.getDuracionMinutos());
        SpannableString min = new SpannableString(act.getString(R.string.duracion_min, r.getDuracionMinutos()));
        min.setSpan(new RelativeSizeSpan(MIN_RUTINA), cifra.length(), min.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        min.setSpan(new ForegroundColorSpan(ContextCompat.getColor(act, R.color.gp_text_secondary)),
                cifra.length(), min.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) tarjeta.findViewById(R.id.tvVistaMinutos)).setText(min);
        ((BloquesRutinaView) tarjeta.findViewById(R.id.bloquesVista)).setBloques(TiempoRutinas.bloques(r));
        ((TextView) tarjeta.findViewById(R.id.tvVistaDetalle)).setText(linea);
        // Para TalkBack, una frase por rutina; los bloques son decorativos.
        tarjeta.findViewById(R.id.cabeceraVista).setContentDescription(act.getString(R.string.seguir_rutina_a11y,
                nombre, t.minutos(r.getDuracionMinutos()), linea));

        LinearLayout ejercicios = tarjeta.findViewById(R.id.listaVistaEjercicios);
        LayoutInflater inflater = LayoutInflater.from(act);
        if (r.getEjercicios() != null) {
            for (RutinaEjercicio e : r.getEjercicios()) {
                ejercicios.addView(filaEjercicio(act, inflater, ejercicios, e.getNombreEjercicio(),
                        Pauta.texto(act, e), true));
            }
        }
        if (r.getQuitados() != null) {
            for (String q : r.getQuitados()) {
                ejercicios.addView(filaEjercicio(act, inflater, ejercicios, q,
                        act.getString(R.string.seguir_fuera), false));
            }
        }

        ImageButton desplegar = tarjeta.findViewById(R.id.btnDesplegarVista);
        pintarDesplegar(act, desplegar, nombre, false);
        desplegar.setOnClickListener(v -> {
            boolean abrir = ejercicios.getVisibility() != View.VISIBLE;
            ejercicios.setVisibility(abrir ? View.VISIBLE : View.GONE);
            if (abrir) Movimiento.entrarUna(ejercicios, 0, Movimiento.PLIEGA, 8);
            pintarDesplegar(act, desplegar, nombre, abrir);
        });
    }

    private static void pintarDesplegar(FragmentActivity act, ImageButton b, String nombre, boolean abierta) {
        b.setImageResource(abierta ? R.drawable.ic_ms_keyboard_arrow_up : R.drawable.ic_ms_keyboard_arrow_down);
        b.setContentDescription(act.getString(abierta ? R.string.seguir_ocultar_ejercicios_a11y
                : R.string.seguir_ver_ejercicios_a11y, nombre));
    }

    // Un ejercicio desplegado: check, nombre y pauta si se queda; aspa, tachado y «fuera» si sale.
    private static View filaEjercicio(FragmentActivity act, LayoutInflater inflater, LinearLayout padre,
                                      @Nullable String nombre, @NonNull String pauta, boolean queda) {
        View fila = inflater.inflate(R.layout.item_vista_previa_ejercicio, padre, false);
        String n = nombre != null ? nombre : "";
        TextView tvNombre = fila.findViewById(R.id.tvVistaEjercicio);
        tvNombre.setText(n);
        ((TextView) fila.findViewById(R.id.tvVistaPauta)).setText(pauta);
        if (!queda) {
            ImageView icono = fila.findViewById(R.id.ivVistaEjercicio);
            icono.setImageResource(R.drawable.ic_ms_close);
            icono.setImageTintList(ContextCompat.getColorStateList(act, R.color.gp_text_secondary));
            tvNombre.setTextColor(ContextCompat.getColor(act, R.color.gp_text_secondary));
            tvNombre.setPaintFlags(tvNombre.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        fila.setContentDescription(act.getString(R.string.seguir_ejercicio_a11y, n, pauta));
        return fila;
    }
}
