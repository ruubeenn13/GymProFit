package es.pmdm.gymprofit.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.model.comida.CopiaRespuesta;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.activities.AnadirAlimentoActivity;
import es.pmdm.gymprofit.ui.activities.ComidaActivity;
import es.pmdm.gymprofit.ui.activities.EstadisticasNutricionActivity;
import es.pmdm.gymprofit.ui.nutricion.TarjetaCopiarAyer;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.ComidasRecientes;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// NutricionFragment — pestaña Nutrición (GP-105), según 03-nutricion.png.
//
// La tira de la semana (de lunes a domingo, con los días futuros desactivados)
// sustituye a las flechas ‹ ›; el calendario abre un selector de fecha hasta hoy; el
// botón de gráficas abre «Historial y estadísticas». La tarjeta de kcal y macros sale
// de DiaNutricion, igual que «Nutrición de hoy» en Inicio, con las reglas de color de
// siempre. Debajo, las cinco comidas: tocar abre la comida; su «+» abre directamente
// añadir alimento, y el de la comida que toca por la hora va en naranja si está sin
// registrar.
// Desde la 1.6.4 (tablero 1), la comida que toca ahora, si el día es hoy, está vacía y la
// misma comida de ayer tuvo algo, lleva debajo «¿Copiar la de ayer?»: la ✓ la copia y el
// día se recarga; la ✗ la pliega y no vuelve ese día (se guarda en el móvil). Las dos
// pliegan la tarjeta (momento 18); las cifras que cuentan llegan con la 1.6.6.
// ============================================================
public class NutricionFragment extends BaseFragment {

    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final AlimentoComidaApi alimentoComidaApi = ApiClient.service(AlimentoComidaApi.class);

    // Día que se está viendo (por defecto hoy; nunca uno que no ha llegado).
    private final Calendar fechaSel = Calendar.getInstance();
    private final Map<String, Comida> comidasDia = new HashMap<>();
    // Qué hay dentro de cada comida, para su resumen («Tostadas, café · 420 kcal»).
    private final Map<Integer, List<AlimentoComida>> alimentosPorComida = new HashMap<>();

    private ActivityResultLauncher<Intent> recargar;

    // «¿Copiar la de ayer?» (1.6.4): la comida de ayer y a qué comida de hoy va; null si no
    // toca. Mientras se copia no se vuelve a pintar, y si falla vuelve.
    @Nullable private ComidaReciente copiaAyer;
    @Nullable private String copiaAyerTipo;
    private boolean copiandoAyer;
    private int turnoCopiaAyer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recargar = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) return;
                    cargarComidas();
                    // Se añadió a otra comida que la de la tarjeta: se dice a cuál (decisión 15).
                    String a = result.getData() != null
                            ? result.getData().getStringExtra(AnadirAlimentoActivity.EXTRA_ANADIDO_A) : null;
                    if (a != null) {
                        es.pmdm.gymprofit.utils.UIHelper.mostrarToastExito(requireActivity(),
                                getString(es.pmdm.gymprofit.ui.nutricion.ElegirComida.anadidoA(a)));
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nutricion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViewById(R.id.btnHistorial).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EstadisticasNutricionActivity.class)));
        findViewById(R.id.btnCalendario).setOnClickListener(v -> elegirFecha());
        ((TextView) findViewById(R.id.macroProteinas).findViewById(R.id.tvMacroNombre)).setText(R.string.nutricion_proteinas);
        ((TextView) findViewById(R.id.macroCarbos).findViewById(R.id.tvMacroNombre)).setText(R.string.nutricion_carbohidratos);
        ((TextView) findViewById(R.id.macroGrasas).findViewById(R.id.tvMacroNombre)).setText(R.string.nutricion_grasas);
        colorBarra(R.id.macroProteinas, R.color.gp_macro_proteinas);
        colorBarra(R.id.macroCarbos, R.color.gp_macro_carbos);
        colorBarra(R.id.macroGrasas, R.color.gp_macro_grasas);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Si la app se quedó abierta de un día para otro, «hoy» ya es otro.
        Calendar hoy = Calendar.getInstance();
        if (fechaSel.after(hoy)) fechaSel.setTime(hoy.getTime());
        pintarSemana();
        cargarComidas();
    }

    // ── Tira de la semana ───────────────────────────────────────────────────

    private void pintarSemana() {
        LinearLayout tira = findViewById(R.id.tiraSemana);
        tira.removeAllViews();
        String[] iniciales = getResources().getStringArray(R.array.dias_semana_iniciales);
        Locale idioma = FechaUtils.localeDeLaApp(requireContext());
        SimpleDateFormat largo = new SimpleDateFormat(getString(R.string.home_fecha_patron), idioma);

        Calendar hoy = Calendar.getInstance();
        Calendar dia = (Calendar) fechaSel.clone();
        dia.add(Calendar.DAY_OF_MONTH, -((dia.get(Calendar.DAY_OF_WEEK) + 5) % 7));   // lunes

        int primario = color(androidx.appcompat.R.attr.colorPrimary);
        int sobrePrimario = color(com.google.android.material.R.attr.colorOnPrimary);
        int texto = color(com.google.android.material.R.attr.colorOnSurface);
        int secundario = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int apagado = color(com.google.android.material.R.attr.colorOutline);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < 7; i++) {
            final Calendar esteDia = (Calendar) dia.clone();
            boolean elegido = mismoDia(esteDia, fechaSel);
            boolean futuro = esteDia.after(hoy) && !mismoDia(esteDia, hoy);

            View celda = inflater.inflate(R.layout.item_dia_semana, tira, false);
            TextView tvInicial = celda.findViewById(R.id.tvInicial);
            TextView tvNumero = celda.findViewById(R.id.tvNumero);
            tvInicial.setText(iniciales[i]);
            tvNumero.setText(String.valueOf(esteDia.get(Calendar.DAY_OF_MONTH)));

            if (elegido) {
                celda.setBackgroundResource(R.drawable.bg_dia_elegido);
                tvInicial.setTextColor(sobrePrimario);
                tvNumero.setTextColor(sobrePrimario);
                tvNumero.setTypeface(tvNumero.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                celda.setBackgroundResource(fondoPulsable());
                tvInicial.setTextColor(futuro ? apagado : secundario);
                tvNumero.setTextColor(futuro ? apagado : texto);
            }

            String nombre = largo.format(esteDia.getTime());
            celda.setContentDescription(futuro ? getString(R.string.dia_futuro_a11y, nombre) : nombre);
            celda.setSelected(elegido);
            celda.setEnabled(!futuro);
            celda.setOnClickListener(futuro ? null : v -> {
                fechaSel.setTime(esteDia.getTime());
                pintarSemana();
                cargarComidas();
            });
            tira.addView(celda);
            dia.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    // Selector de fecha hasta hoy; la tira salta a la semana del día elegido.
    private void elegirFecha() {
        long hoyUtc = MaterialDatePicker.todayInUtcMilliseconds();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(utcDe(fechaSel))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(hoyUtc)
                        .setValidator(DateValidatorPointBackward.before(hoyUtc + 1))
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(utc -> {
            Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            u.setTimeInMillis(utc);
            fechaSel.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH));
            pintarSemana();
            cargarComidas();
        });
        picker.show(getParentFragmentManager(), "fecha_nutricion");
    }

    private static long utcDe(Calendar local) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return u.getTimeInMillis();
    }

    private static boolean mismoDia(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private boolean esHoy() {
        return mismoDia(fechaSel, Calendar.getInstance());
    }

    private String fechaSelStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(fechaSel.getTime());
    }

    // ── Carga ───────────────────────────────────────────────────────────────

    private void cargarComidas() {
        int usuarioId = prefsManager.getUsuarioId();
        final String fecha = fechaSelStr();
        if (usuarioId == -1) {
            comidasDia.clear();
            alimentosPorComida.clear();
            pintar();
            return;
        }
        comidaApi.getDeUsuarioFecha(usuarioId, fecha).enqueue(new ApiCallback<List<Comida>>() {
            @Override
            public void onOk(List<Comida> lista) {
                if (!isAdded() || !fecha.equals(fechaSelStr())) return;
                comidasDia.clear();
                alimentosPorComida.clear();
                if (lista != null) for (Comida c : lista) comidasDia.put(c.getTipoComida(), c);
                pintar();
                cargarAlimentos(fecha);
                buscarCopiaAyer(fecha);
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                // Un día sin comidas no llega aquí: es 200 con []. Esto es un fallo de
                // verdad, y se avisa; no se pinta un día vacío que no lo es.
                UiFeedback.toastError(requireActivity(), code, message);
            }
        });
    }

    // Lo que hay dentro de cada comida, para su resumen. Si alguna falla, esa comida
    // se queda con el resumen corto (solo kcal), que sigue siendo cierto.
    private void cargarAlimentos(String fecha) {
        for (Comida c : comidasDia.values()) {
            alimentoComidaApi.getDeComida(c.getId()).enqueue(new ApiCallback<List<AlimentoComida>>() {
                @Override
                public void onOk(List<AlimentoComida> lista) {
                    if (!isAdded() || !fecha.equals(fechaSelStr())) return;
                    alimentosPorComida.put(c.getId(), lista != null ? lista : new ArrayList<>());
                    pintarComidas();
                }
                @Override
                public void onFail(int code, String message) {
                    // Se ignora a propósito (GP-017): el resumen de la tarjeta se queda en
                    // las kcal, que ya están; el detalle está al abrir la comida.
                }
            });
        }
    }

    // ── «¿Copiar la de ayer?» (1.6.4, B2) ───────────────────────────────────

    // Solo hoy, solo la comida que toca ahora, solo si está vacía y no se ha dicho que no.
    // Un fallo no se enseña: sin la tarjeta, el diario es el de siempre.
    private void buscarCopiaAyer(String fecha) {
        int miTurno = ++turnoCopiaAyer;
        String tipo = ComidaQueToca.ahora();
        if (copiandoAyer) return;
        if (!esHoy() || registrada(comidasDia.get(tipo)) || prefsManager.copiaAyerDescartada(fecha, tipo)) {
            if (copiaAyer != null) {
                copiaAyer = null;
                pintarComidas();
            }
            return;
        }
        comidaApi.recientes(fecha, tipo).enqueue(new ApiCallback<List<ComidaReciente>>() {
            @Override
            public void onOk(List<ComidaReciente> lista) {
                if (!isAdded() || miTurno != turnoCopiaAyer || !fecha.equals(fechaSelStr())) return;
                copiaAyer = ComidasRecientes.deAyer(lista, tipo, fecha);
                copiaAyerTipo = tipo;
                pintarComidas();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: la tarjeta es un atajo; sin ella se apunta igual.
            }
        });
    }

    // La ✓: la tarjeta se pliega al momento y la comida se copia; al volver, el día se
    // recarga con ella. Si falla, la tarjeta vuelve y se dice.
    private void copiarAyer(@NonNull View tarjeta, @NonNull ComidaReciente ayer, @NonNull String tipo) {
        if (copiandoAyer) return;
        copiandoAyer = true;
        String fecha = fechaSelStr();
        Movimiento.vibrar(tarjeta, Movimiento.Vibracion.LIGERA);
        Movimiento.plegar(tarjeta, null);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("comidaId", ayer.getId());
        cuerpo.put("fecha", fecha);
        cuerpo.put("tipoComida", tipo);
        comidaApi.copiar(cuerpo).enqueue(new ApiCallback<CopiaRespuesta>() {
            @Override
            public void onOk(CopiaRespuesta r) {
                copiandoAyer = false;
                if (!isAdded()) return;
                copiaAyer = null;
                cargarComidas();
            }

            @Override
            public void onFail(int code, String message) {
                copiandoAyer = false;
                if (!isAdded()) return;
                pintarComidas();
                UIHelper.mostrarToastError(requireActivity(), getString(R.string.copiar_ayer_fallo,
                        UiFeedback.mensaje(requireContext(), code, message)));
            }
        });
    }

    // La ✗: se pliega y no vuelve hoy para esta comida, ni aquí ni en su pantalla.
    private void noCopiarAyer(@NonNull View tarjeta, @NonNull String tipo) {
        prefsManager.descartarCopiaAyer(fechaSelStr(), tipo);
        copiaAyer = null;
        Movimiento.plegar(tarjeta, null);
    }

    private static boolean registrada(@Nullable Comida c) {
        return c != null && c.getTotalCalorias() > 0;
    }

    // ── Pintado ─────────────────────────────────────────────────────────────

    private void pintar() {
        pintarKcal();
        pintarComidas();
    }

    private void pintarKcal() {
        DiaNutricion d = DiaNutricion.de(new ArrayList<>(comidasDia.values()), DiaNutricion.objetivo(prefsManager));
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(requireContext()));

        ((TextView) findViewById(R.id.tvKcal)).setText(nf.format(d.kcal));
        TextView rotulo = findViewById(R.id.tvQuedanRotulo);
        TextView quedan = findViewById(R.id.tvQuedan);
        if (d.sinObjetivo) {
            // Sin peso ni altura (GP-103): lo comido, sin objetivo que restar.
            ((TextView) findViewById(R.id.tvKcalObjetivo)).setText(R.string.nutricion_sin_objetivo);
            rotulo.setText(R.string.nutricion_sin_objetivo_rotulo);
            quedan.setText(R.string.nutricion_sin_objetivo_accion);
            quedan.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
            ((LinearProgressIndicator) findViewById(R.id.barraKcal)).setProgressCompat(0, false);
            findViewById(R.id.filaKcal).setContentDescription(
                    getString(R.string.nutricion_sin_objetivo_a11y, nf.format(d.kcal)));
            macroSinObjetivo(R.id.macroProteinas, d.proteinas);
            macroSinObjetivo(R.id.macroCarbos, d.carbohidratos);
            macroSinObjetivo(R.id.macroGrasas, d.grasas);
            return;
        }
        ((TextView) findViewById(R.id.tvKcalObjetivo)).setText(getString(R.string.nutricion_kcal_de, nf.format(d.objetivoKcal)));
        int restantes = d.kcalRestantes();
        rotulo.setText(restantes >= 0 ? R.string.nutricion_te_quedan : R.string.nutricion_te_pasas);
        quedan.setText(getString(R.string.nutricion_kcal_valor, nf.format(Math.abs(restantes))));
        quedan.setTextColor(restantes >= 0 ? color(com.google.android.material.R.attr.colorOnSurface)
                : color(androidx.appcompat.R.attr.colorError));
        ((LinearProgressIndicator) findViewById(R.id.barraKcal)).setProgressCompat(
                DiaNutricion.porcentaje(d.kcal, d.objetivoKcal), false);
        findViewById(R.id.filaKcal).setContentDescription(getString(R.string.nutricion_kcal_a11y,
                nf.format(d.kcal), nf.format(d.objetivoKcal), rotulo.getText(), quedan.getText()));

        macro(R.id.macroProteinas, d.proteinas, d.objetivoProteinas, d.estadoProteinas());
        macro(R.id.macroCarbos, d.carbohidratos, d.objetivoCarbohidratos, d.estadoCarbohidratos());
        macro(R.id.macroGrasas, d.grasas, d.objetivoGrasas, d.estadoGrasas());
    }

    private void macroSinObjetivo(int id, double valor) {
        View fila = findViewById(id);
        TextView tvValor = fila.findViewById(R.id.tvMacroValor);
        tvValor.setText(getString(R.string.nutricion_macro_solo, (int) Math.round(valor)));
        tvValor.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
        ((LinearProgressIndicator) fila.findViewById(R.id.barraMacro)).setProgressCompat(0, false);
        fila.setContentDescription(((TextView) fila.findViewById(R.id.tvMacroNombre)).getText()
                + ", " + tvValor.getText());
    }

    private void macro(int id, double valor, int objetivo, DiaNutricion.Estado estado) {
        View fila = findViewById(id);
        TextView tvValor = fila.findViewById(R.id.tvMacroValor);
        tvValor.setText(getString(R.string.nutricion_macro_valor, (int) Math.round(valor), objetivo));
        tvValor.setTextColor(colorEstado(estado));
        ((LinearProgressIndicator) fila.findViewById(R.id.barraMacro))
                .setProgressCompat(DiaNutricion.porcentaje(valor, objetivo), false);
        fila.setContentDescription(((TextView) fila.findViewById(R.id.tvMacroNombre)).getText()
                + ", " + tvValor.getText());
    }

    private void pintarComidas() {
        LinearLayout lista = findViewById(R.id.listaComidas);
        lista.removeAllViews();
        String queToca = esHoy() ? ComidaQueToca.ahora() : null;
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (String tipo : ComidaQueToca.TIPOS) {
            Comida c = comidasDia.get(tipo);
            boolean registrada = registrada(c);
            boolean toca = tipo.equals(queToca) && !registrada;

            View card = inflater.inflate(R.layout.item_comida_dia, lista, false);
            String nombre = getString(ComidaQueToca.titulo(tipo));
            ((TextView) card.findViewById(R.id.tvComidaNombre)).setText(nombre);
            String resumen = resumenComida(c, toca);
            ((TextView) card.findViewById(R.id.tvComidaResumen)).setText(resumen);
            card.setContentDescription(nombre + ". " + resumen);
            card.setOnClickListener(v -> abrirComida(tipo));

            ImageButton mas = card.findViewById(R.id.btnAnadirComida);
            mas.setContentDescription(getString(anadirA11y(tipo)));
            if (toca) {
                mas.setBackgroundResource(R.drawable.bg_circulo_primario_pulsable);
                mas.setImageTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnPrimary)));
            }
            mas.setOnClickListener(v -> anadirAlimento(tipo));
            if (toca && copiaAyer != null && tipo.equals(copiaAyerTipo) && !copiandoAyer) {
                ViewGroup ranura = card.findViewById(R.id.ranuraCopiarAyer);
                View tarjeta = inflater.inflate(R.layout.view_copiar_ayer_diario, ranura, false);
                ComidaReciente ayer = copiaAyer;
                TarjetaCopiarAyer.pintar(tarjeta, ayer, tipo, new TarjetaCopiarAyer.Respuesta() {
                    @Override public void si() { copiarAyer(tarjeta, ayer, tipo); }
                    @Override public void no() { noCopiarAyer(tarjeta, tipo); }
                });
                ranura.addView(tarjeta);
                ranura.setVisibility(View.VISIBLE);
            }
            lista.addView(card);
        }
    }

    // «Tostadas con aguacate, café con leche · 420 kcal» con uno o dos alimentos;
    // «3 alimentos · 820 kcal» con más; «Sin registrar», con «· ahora toca» si toca.
    private String resumenComida(@Nullable Comida c, boolean toca) {
        if (c == null || c.getTotalCalorias() <= 0) {
            return toca ? getString(R.string.comida_resumen, getString(R.string.sin_registrar),
                    getString(R.string.comida_ahora_toca)) : getString(R.string.sin_registrar);
        }
        String kcal = getString(R.string.unidad_kcal, c.getTotalCalorias());
        List<AlimentoComida> alimentos = alimentosPorComida.get(c.getId());
        if (alimentos == null || alimentos.isEmpty()) return kcal;
        String queHay;
        if (alimentos.size() <= 2) {
            List<String> nombres = new ArrayList<>();
            for (AlimentoComida a : alimentos) nombres.add(a.getNombreAlimento());
            queHay = android.text.TextUtils.join(", ", nombres);
        } else {
            queHay = getResources().getQuantityString(R.plurals.comida_n_alimentos, alimentos.size(), alimentos.size());
        }
        return getString(R.string.comida_resumen, queHay, kcal);
    }

    private static int anadirA11y(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.comida_anadir_desayuno;
            case "ALMUERZO": return R.string.comida_anadir_almuerzo;
            case "COMIDA":   return R.string.comida_anadir_comida;
            case "MERIENDA": return R.string.comida_anadir_merienda;
            default:         return R.string.comida_anadir_cena;
        }
    }

    // Tocar la tarjeta abre la comida del día elegido, como antes.
    private void abrirComida(String tipo) {
        Intent intent = new Intent(requireContext(), ComidaActivity.class);
        intent.putExtra("tipoComida", tipo);
        Comida c = comidasDia.get(tipo);
        intent.putExtra("comidaId", c != null ? c.getId() : -1);
        intent.putExtra("fecha", fechaSelStr());
        recargar.launch(intent);
    }

    // El «+» abre directamente añadir alimento en esa comida y ese día.
    private void anadirAlimento(String tipo) {
        Intent intent = new Intent(requireContext(), AnadirAlimentoActivity.class);
        intent.putExtra("tipoComida", tipo);
        Comida c = comidasDia.get(tipo);
        intent.putExtra("comidaId", c != null ? c.getId() : -1);
        intent.putExtra("fecha", fechaSelStr());
        recargar.launch(intent);
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private void colorBarra(int filaId, int colorRes) {
        ((LinearProgressIndicator) findViewById(filaId).findViewById(R.id.barraMacro))
                .setIndicatorColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private int colorEstado(DiaNutricion.Estado e) {
        switch (e) {
            case LOGRADO: return ContextCompat.getColor(requireContext(), R.color.gp_success);
            case PASADO:  return color(androidx.appcompat.R.attr.colorError);
            default:      return color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        }
    }

    private int fondoPulsable() {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
