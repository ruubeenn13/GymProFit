package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.alta.HojasAlta;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.DatosPerfil;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.GrasaReciente;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.NivelVisible;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.PlanPrevio;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ReglasEdad;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// TusDatosActivity — Tus datos y objetivos (GP-216, tablero 18). Sustituye a Editar perfil.
//
// El mismo contenido (nombre, peso, altura y edad, sexo y actividad, objetivo y nivel) con otra
// forma: botones \u2212/+ (que repiten al mantenerlos) y opciones a la vista en vez de campos y
// desplegables, y abajo el plan recalculado ANTES de guardar. Ese plan es el de PlanPrevio, que usa
// CalculadoraNutricional con el % de grasa reciente (DEC-054): lo que se ve es lo que se guarda.
// «Guardar» está apagado mientras nada cambie respecto a lo que había al abrir (DatosPerfil).
//
// Guarda por PATCH lo que guarda la API y recalcula el objetivo local con los datos nuevos. Sexo y
// actividad van también a la API (GP-111) y se guardan en el teléfono, que es de donde calcula las
// macros. El correo no está aquí: se cambia en Perfil › Correo, con la contraseña (GP-083).
// ============================================================
public class TusDatosActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Valores enviados a la API: tres niveles (GP-103; Experto se conserva si no se toca el nivel),
    // los objetivos, el sexo y la actividad (GP-111).
    private static final String[] NIVELES = NivelVisible.OFRECIDOS;
    private static final String[] OBJETIVOS = {
            CalculadoraNutricional.OBJETIVO_PERDER_PESO, CalculadoraNutricional.OBJETIVO_MANTENER_PESO,
            CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR, CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA
    };
    private static final int[] OBJETIVO_TEXTOS = {R.string.objetivo_perder_peso, R.string.objetivo_mantener,
            R.string.objetivo_ganar_musculo, R.string.objetivo_fuerza};
    private static final int[] OBJETIVO_ICONOS = {R.drawable.ic_ms_local_fire_department, R.drawable.ic_ms_balance,
            R.drawable.ic_ms_fitness_center, R.drawable.ic_ms_bolt};
    private static final String[] SEXOS = {"HOMBRE", "MUJER"};
    private static final String[] ACTIVIDADES = {
            CalculadoraNutricional.ACTIVIDAD_SEDENTARIO, CalculadoraNutricional.ACTIVIDAD_LIGERO,
            CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.ACTIVIDAD_ACTIVO
    };
    private static final int[] ACTIVIDAD_CORTA = {R.string.actividad_corta_poca, R.string.actividad_corta_ligera,
            R.string.actividad_corta_moderada, R.string.actividad_corta_mucha};
    private static final int[] ACTIVIDAD_DESC = {R.string.actividad_sedentario_desc, R.string.actividad_ligero_desc,
            R.string.actividad_moderado_desc, R.string.actividad_activo_desc};
    private static final int[] NIVEL_TEXTOS = {R.string.nivel_principiante, R.string.nivel_intermedio,
            R.string.nivel_avanzado};

    // Por dónde se empieza cuando el dato no está puesto (una cuenta sin peso, altura o edad).
    private static final double PESO_INICIAL = 70;
    private static final int ALTURA_INICIAL = 170, EDAD_INICIAL = 30;

    // Mantener pulsado un \u2212 o un +: espera antes de repetir y cada cuánto repite.
    private static final long REPETIR_ESPERA_MS = 400, REPETIR_CADA_MS = 90;

    private PreferencesManager prefs;
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private final Handler repeticion = new Handler(Looper.getMainLooper());

    private TextInputEditText etNombre;
    private View stepperPeso, stepperAltura, stepperEdad;
    private MaterialButtonToggleGroup grupoSexo, grupoActividad, grupoNivel;
    private final View[] opcionesObjetivo = new View[OBJETIVOS.length];
    private MaterialButton btnGuardar;

    // Cómo estaba al abrirlo (y al llegar el perfil de la API) y cómo está ahora.
    private DatosPerfil inicial;
    private DatosPerfil actual;
    // El nivel que tiene la cuenta tal cual, para no convertir un Experto en Avanzado.
    @Nullable private String nivelGuardado;
    // El % de grasa más reciente: con él se calcula la proteína (DEC-054).
    @Nullable private GrasaReciente.Medida grasa;
    // Hasta que llega el perfil de la API, guardar pisaría los datos reales con lo del móvil.
    private boolean perfilCargado;
    private boolean guardando;
    // Para no repintar el formulario mientras se está pintando.
    private boolean pintando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_tus_datos);

        // Lo guardado en el móvil primero; lo de la API lo corrige al llegar.
        nivelGuardado = prefs.getNivel();
        inicial = new DatosPerfil(prefs.getNombre(), prefs.getPeso(), (int) Math.round(prefs.getAltura()),
                prefs.getEdad(), prefs.getSexo(), prefs.getActividad(), prefs.getObjetivo(),
                NivelVisible.valor(nivelGuardado));
        actual = inicial;
        grasa = prefs.getGrasaReciente();
        perfilCargado = prefs.getUsuarioId() == -1;

        etNombre = findViewById(R.id.etNombre);
        btnGuardar = findViewById(R.id.btnGuardar);
        prepararSteppers();
        prepararOpciones();
        pintar();

        etNombre.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                if (!pintando) cambiar(d -> d.conNombre(e.toString()));
            }
        });
        AvisoDescartar.instalar(this, findViewById(R.id.toolbar), this::hayCambios);
        btnGuardar.setOnClickListener(v -> guardar());
        cargarPerfil();
        cargarGrasa();
    }

    @Override
    protected void onDestroy() {
        repeticion.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ── Cargar ───────────────────────────────────────────────────────────────

    // Lo que dice la API manda sobre lo del móvil (GP-111); sin ello, lo del móvil.
    private void cargarPerfil() {
        int id = prefs.getUsuarioId();
        if (id == -1) return;
        usuarioApi.getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null || isFinishing()) return;
                nivelGuardado = u.getNivelExperiencia();
                double peso = 0;
                try {
                    if (u.getPeso() != null && !u.getPeso().isEmpty()) peso = Numeros.leerDecimal(u.getPeso());
                } catch (NumberFormatException e) {
                    // Un peso ilegible cuenta como sin peso: el formulario lo pide de nuevo.
                    peso = 0;
                }
                inicial = new DatosPerfil(u.getNombre(), peso, (int) Math.round(u.getAltura()), u.getEdad(),
                        u.getSexo() != null ? u.getSexo() : prefs.getSexo(),
                        u.getNivelActividad() != null ? u.getNivelActividad() : prefs.getActividad(),
                        u.getObjetivo() != null ? u.getObjetivo() : prefs.getObjetivo(),
                        NivelVisible.valor(nivelGuardado));
                actual = inicial;
                perfilCargado = true;
                pintar();
            }

            @Override
            public void onFail(int code, String message) {
                // Sin el perfil cargado, guardar pisaría los datos reales con el formulario del móvil:
                // «Guardar» se queda apagado y se dice, con la manera de reintentar. El 401 ya lo
                // resuelve el aviso global.
                if (code == 401 || isFinishing()) return;
                Snackbar.make(findViewById(R.id.raizTusDatos), R.string.editar_perfil_error_carga_corto,
                        Snackbar.LENGTH_INDEFINITE).setAction(R.string.btn_reintentar, v -> cargarPerfil()).show();
            }
        });
    }

    // El % de grasa más reciente, para la proteína: si no llega, se usa el último guardado en el móvil.
    private void cargarGrasa() {
        int id = prefs.getUsuarioId();
        if (id == -1) return;
        ApiClient.service(MedicionApi.class).getOrdenadas(id).enqueue(new ApiCallback<List<MedicionCorporal>>() {
            @Override
            public void onOk(List<MedicionCorporal> lista) {
                if (isFinishing()) return;
                grasa = GrasaReciente.elegir(lista);
                pintarPlan();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: el plan de antes de guardar se calcula con el último % que hubiera
                // en el móvil, y guardar vuelve a pedirlo; en el peor caso la cifra previa difiere un poco.
            }
        });
    }

    // ── Los controles ────────────────────────────────────────────────────────

    private void prepararSteppers() {
        stepperPeso = findViewById(R.id.stepperPeso);
        stepperAltura = findViewById(R.id.stepperAltura);
        stepperEdad = findViewById(R.id.stepperEdad);

        stepper(stepperPeso, R.string.alta_peso, R.string.alta_unidad_kg,
                d -> d.peso <= 0 ? d.conPeso(PESO_INICIAL) : d.menosPeso(),
                d -> d.peso <= 0 ? d.conPeso(PESO_INICIAL) : d.masPeso(),
                v -> HojasAlta.peso(this, actual.peso > 0 ? String.valueOf(actual.peso) : "",
                        p -> cambiar(d -> d.conPeso(Double.parseDouble(p)))));
        stepper(stepperAltura, R.string.alta_altura, R.string.alta_unidad_cm,
                d -> d.altura <= 0 ? d.conAltura(ALTURA_INICIAL) : d.menosAltura(),
                d -> d.altura <= 0 ? d.conAltura(ALTURA_INICIAL) : d.masAltura(),
                v -> HojasAlta.numero(this, R.string.alta_altura_titulo, DatosPerfil.ALTURA_MIN, DatosPerfil.ALTURA_MAX,
                        actual.altura, ALTURA_INICIAL, getString(R.string.alta_unidad_cm),
                        a -> cambiar(d -> d.conAltura(a))));
        stepper(stepperEdad, R.string.alta_edad, 0,
                d -> d.edad <= 0 ? d.conEdad(EDAD_INICIAL) : d.menosEdad(),
                d -> d.edad <= 0 ? d.conEdad(EDAD_INICIAL) : d.masEdad(),
                v -> HojasAlta.numero(this, R.string.alta_edad_titulo, ReglasEdad.MINIMA, ReglasEdad.MAXIMA,
                        actual.edad, EDAD_INICIAL, "", e -> cambiar(d -> d.conEdad(e))));
    }

    // Una fila \u2212/+: un toque da un paso y mantenerla pulsada repite, más deprisa cuanto más dura.
    // TalkBack no genera toques: sus acciones llegan por onClick, que da un solo paso.
    private void stepper(View fila, @StringRes int nombre, @StringRes int unidad, UnaryOperator<DatosPerfil> menos,
                         UnaryOperator<DatosPerfil> mas, View.OnClickListener escribir) {
        ((TextView) fila.findViewById(R.id.tvNombreDato)).setText(nombre);
        TextView tvUnidad = fila.findViewById(R.id.tvUnidadDato);
        if (unidad == 0) tvUnidad.setVisibility(View.GONE);
        else tvUnidad.setText(unidad);
        View btnMenos = fila.findViewById(R.id.btnMenos);
        View btnMas = fila.findViewById(R.id.btnMas);
        btnMenos.setContentDescription(getString(R.string.datos_menos_a11y, getString(nombre)));
        btnMas.setContentDescription(getString(R.string.datos_mas_a11y, getString(nombre)));
        repetible(btnMenos, menos);
        repetible(btnMas, mas);
        View valor = fila.findViewById(R.id.valorDato);
        valor.setOnClickListener(escribir);
        valor.setContentDescription(getString(R.string.datos_escribir_a11y, getString(nombre)));
    }

    private void repetible(View boton, UnaryOperator<DatosPerfil> paso) {
        // El paso se da al SOLTAR: apoyar el dedo para desplazar la pantalla no cambia nada (si la pantalla
        // se lleva el gesto llega un CANCEL). Solo repite quien mantiene el dedo quieto más de la espera.
        final Runnable[] repetir = new Runnable[1];
        final boolean[] repitiendo = {false};
        repetir[0] = () -> {
            repitiendo[0] = true;
            boton.getParent().requestDisallowInterceptTouchEvent(true);
            cambiar(paso);
            repeticion.postDelayed(repetir[0], REPETIR_CADA_MS);
        };
        boton.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    repitiendo[0] = false;
                    repeticion.postDelayed(repetir[0], REPETIR_ESPERA_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    repeticion.removeCallbacks(repetir[0]);
                    if (!repitiendo[0]) cambiar(paso);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    repeticion.removeCallbacks(repetir[0]);
                    return true;
                default:
                    return true;
            }
        });
        // El camino de TalkBack y del teclado: un clic, un paso.
        boton.setOnClickListener(v -> cambiar(paso));
    }

    private void prepararOpciones() {
        grupoSexo = findViewById(R.id.grupoSexo);
        grupoSexo.addOnButtonCheckedListener((g, id, marcado) -> {
            if (marcado && !pintando) cambiar(d -> d.conSexo(id == R.id.btnSexoMujer ? SEXOS[1] : SEXOS[0]));
        });

        grupoActividad = findViewById(R.id.grupoActividad);
        for (int i = 0; i < ACTIVIDADES.length; i++) {
            MaterialButton b = boton(grupoActividad, ACTIVIDAD_CORTA[i]);
            final int indice = i;
            b.setOnClickListener(v -> { if (!pintando) cambiar(d -> d.conActividad(ACTIVIDADES[indice])); });
        }
        grupoNivel = findViewById(R.id.grupoNivel);
        for (int i = 0; i < NIVELES.length; i++) {
            MaterialButton b = boton(grupoNivel, NIVEL_TEXTOS[i]);
            final int indice = i;
            b.setOnClickListener(v -> { if (!pintando) cambiar(d -> d.conNivel(NIVELES[indice])); });
        }

        // Los objetivos, de dos en dos.
        LinearLayout grid = findViewById(R.id.gridObjetivo);
        LayoutInflater inflater = LayoutInflater.from(this);
        int separacion = getResources().getDimensionPixelSize(R.dimen.espacio_12);
        LinearLayout fila = null;
        for (int i = 0; i < OBJETIVOS.length; i++) {
            if (i % 2 == 0) {
                fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) lp.topMargin = separacion;
                grid.addView(fila, lp);
            }
            View op = inflater.inflate(R.layout.item_objetivo_dato, fila, false);
            if (i % 2 == 1) ((LinearLayout.LayoutParams) op.getLayoutParams()).setMarginStart(separacion);
            ((ImageView) op.findViewById(R.id.ivObjetivo)).setImageResource(OBJETIVO_ICONOS[i]);
            ((TextView) op.findViewById(R.id.tvObjetivo)).setText(OBJETIVO_TEXTOS[i]);
            op.setContentDescription(getString(OBJETIVO_TEXTOS[i]));
            final int indice = i;
            op.setOnClickListener(v -> cambiar(d -> d.conObjetivo(OBJETIVOS[indice])));
            opcionesObjetivo[i] = op;
            fila.addView(op);
        }
    }

    private MaterialButton boton(MaterialButtonToggleGroup grupo, @StringRes int texto) {
        MaterialButton b = (MaterialButton) getLayoutInflater().inflate(R.layout.view_boton_conectado, grupo, false);
        b.setId(View.generateViewId());
        b.setText(texto);
        // Con la letra muy grande tres o cuatro opciones no caben en fila sin cortar el texto: van una
        // debajo de otra, de ancho completo.
        boolean enVertical = getResources().getConfiguration().fontScale >= 1.2f;
        grupo.setOrientation(enVertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = enVertical
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        grupo.addView(b, lp);
        return b;
    }

    // ── Estado y pintar ──────────────────────────────────────────────────────

    // Aplica un cambio y repinta lo que depende de él.
    private void cambiar(UnaryOperator<DatosPerfil> cambio) {
        actual = cambio.apply(actual);
        pintar();
    }

    private boolean hayCambios() {
        return perfilCargado && actual.distintoDe(inicial);
    }

    private void pintar() {
        pintando = true;
        NumberFormat peso = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        peso.setMinimumFractionDigits(1);
        peso.setMaximumFractionDigits(1);
        valorStepper(stepperPeso, actual.peso > 0 ? peso.format(actual.peso) : null);
        valorStepper(stepperAltura, actual.altura > 0 ? String.valueOf(actual.altura) : null);
        valorStepper(stepperEdad, actual.edad > 0 ? String.valueOf(actual.edad) : null);

        String nombre = etNombre.getText() == null ? "" : etNombre.getText().toString();
        if (!nombre.equals(actual.nombre)) etNombre.setText(actual.nombre);

        grupoSexo.check("MUJER".equalsIgnoreCase(actual.sexo) ? R.id.btnSexoMujer : R.id.btnSexoHombre);
        marcar(grupoActividad, indiceDe(ACTIVIDADES, actual.actividad, 2));
        marcar(grupoNivel, indiceDe(NIVELES, actual.nivel, 1));
        int objetivo = indiceDe(OBJETIVOS, actual.objetivo, 0);
        for (int i = 0; i < opcionesObjetivo.length; i++) pintarObjetivo(opcionesObjetivo[i], i == objetivo);
        ((TextView) findViewById(R.id.tvActividadTexto)).setText(ACTIVIDAD_DESC[indiceDe(ACTIVIDADES, actual.actividad, 2)]);

        // De 14 a 17 años con «Perder grasa», la calculadora da las calorías de mantenimiento (GP-103).
        boolean perder = CalculadoraNutricional.OBJETIVO_PERDER_PESO.equals(OBJETIVOS[objetivo]);
        boolean menor = actual.edad >= ReglasEdad.MINIMA && actual.edad <= CalculadoraNutricional.EDAD_SIN_DEFICIT;
        findViewById(R.id.tvNotaObjetivo).setVisibility(perder && menor ? View.VISIBLE : View.GONE);
        pintando = false;

        pintarPlan();
    }

    private void valorStepper(View fila, @Nullable String valor) {
        TextView tv = fila.findViewById(R.id.tvValorDato);
        tv.setText(valor == null ? getString(R.string.datos_sin_valor) : valor);
        TextView unidad = fila.findViewById(R.id.tvUnidadDato);
        unidad.setVisibility(valor == null || unidad.getText().length() == 0 ? View.GONE : View.VISIBLE);
        TextView nombre = fila.findViewById(R.id.tvNombreDato);
        fila.findViewById(R.id.valorDato).setContentDescription(getString(R.string.datos_valor_a11y,
                nombre.getText(), valor == null ? getString(R.string.datos_sin_valor_a11y)
                        : valor + (unidad.getVisibility() == View.VISIBLE ? " " + unidad.getText() : "")));
    }

    private static void marcar(MaterialButtonToggleGroup grupo, int indice) {
        View b = grupo.getChildAt(indice);
        if (b != null) grupo.check(b.getId());
    }

    private static int indiceDe(String[] valores, String valor, int pordefecto) {
        for (int i = 0; i < valores.length; i++) if (valores[i].equalsIgnoreCase(valor)) return i;
        return pordefecto;
    }

    private void pintarObjetivo(View op, boolean elegido) {
        op.setBackgroundResource(elegido ? R.drawable.bg_objetivo_elegido : R.drawable.bg_objetivo);
        ImageView icono = op.findViewById(R.id.ivObjetivo);
        icono.setImageTintList(android.content.res.ColorStateList.valueOf(colorDe(elegido
                ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurfaceVariant)));
        ViewCompat.setAccessibilityDelegate(op, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(elegido);
            }
        });
    }

    private int colorDe(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    // El plan de abajo y «Guardar»: se recalcula con cada cambio.
    private void pintarPlan() {
        Locale idioma = FechaUtils.localeDeLaApp(this);
        NumberFormat entero = NumberFormat.getIntegerInstance(idioma);
        PlanPrevio plan = PlanPrevio.de(actual, grasa, System.currentTimeMillis(), prefs.getCaloriasDiarias(),
                prefs.getProteinasDiarias());
        TextView quedara = findViewById(R.id.tvPlanQuedara);
        TextView dif = findViewById(R.id.tvPlanDif);
        TextView minimo = findViewById(R.id.tvPlanMinimo);
        if (plan == null) {
            quedara.setText(R.string.datos_plan_sin_datos);
            dif.setText(null);
            dif.setVisibility(View.GONE);
            minimo.setVisibility(View.GONE);
        } else {
            quedara.setText(getString(R.string.datos_plan_cifras, entero.format(plan.kcal), plan.proteinas));
            dif.setVisibility(View.VISIBLE);
            // Sin objetivo guardado todavía no hay «respecto a ahora» que decir.
            boolean hayAntes = prefs.hayObjetivoGuardado() && prefs.getCaloriasDiarias() > 0;
            if (!hayAntes) {
                dif.setText(null);
                dif.setVisibility(View.GONE);
            } else if (plan.difKcal == 0) {
                dif.setText(R.string.datos_plan_igual);
                dif.setTextColor(colorDe(com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                String n = entero.format(Math.abs(plan.difKcal));
                dif.setText(getString(plan.difKcal > 0 ? R.string.datos_plan_mas : R.string.datos_plan_menos, n));
                dif.setTextColor(colorDe(com.google.android.material.R.attr.colorOnPrimaryContainer));
            }
            // El mínimo eficaz, junto a la cifra, cuando hay rango que enseñar (DEC-054).
            boolean rango = plan.proteinaMinima > 0 && plan.proteinaMinima < plan.proteinas;
            minimo.setVisibility(rango ? View.VISIBLE : View.GONE);
            if (rango) minimo.setText(getString(R.string.datos_plan_minimo, plan.proteinaMinima));
        }
        boolean puede = hayCambios() && !guardando && plan != null;
        btnGuardar.setEnabled(puede);
    }

    // ── Guardar ──────────────────────────────────────────────────────────────

    /**
     * Comprueba la edad y, si no vale, lo dice (GP-145). Por debajo de 14 no es un número mal puesto,
     * es la edad mínima de la política (DEC-038).
     */
    private boolean edadValida() {
        if (actual.edad > 0 && ReglasEdad.estado(String.valueOf(actual.edad)) != ReglasEdad.Estado.VALIDA) {
            UIHelper.mostrarToastError(this, getString(R.string.error_edad_rango, ReglasEdad.MINIMA, ReglasEdad.MAXIMA));
            return false;
        }
        return true;
    }

    // Guarda el perfil por PATCH y, al tener éxito, guarda sexo y actividad en el teléfono y recalcula
    // las macros con los datos nuevos.
    private void guardar() {
        if (!hayCambios() || guardando || !edadValida()) return;
        int id = prefs.getUsuarioId();
        final DatosPerfil d = actual;
        // Un valor null no toca el campo: la API ignora los null del PATCH.
        Map<String, Object> body = new HashMap<>();
        // En blanco borra el nombre (GP-116): la API ignora el null, no el vacío.
        final String nombre = NombreVisible.paraEnviar(d.nombre);
        body.put("nombre", nombre);
        body.put("peso", d.peso > 0 ? new BigDecimal(String.valueOf(d.peso)) : null);
        body.put("altura", d.altura > 0 ? new BigDecimal(d.altura) : null);
        body.put("edad", d.edad > 0 ? d.edad : null);
        final String nivel = NivelVisible.aGuardar(d.nivel.isEmpty() ? NIVELES[1] : d.nivel, nivelGuardado);
        body.put("nivelExperiencia", nivel);
        body.put("objetivo", d.objetivo);
        body.put("sexo", d.sexo);
        body.put("nivelActividad", d.actividad);

        guardando = true;
        pintarPlan();
        LoadingDialog.show(this);
        usuarioApi.patch(id, body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void response) {
                LoadingDialog.hide(TusDatosActivity.this);
                prefs.saveNombre(prefs.getUsername(), nombre);
                if (d.peso > 0) prefs.savePeso(d.peso);
                if (d.altura > 0) prefs.saveAltura(d.altura);
                if (d.edad > 0) prefs.saveEdad(d.edad);
                prefs.saveObjetivo(d.objetivo);
                prefs.saveNivel(nivel);
                prefs.saveSexo(d.sexo);
                prefs.saveActividad(d.actividad);
                prefs.apuntarDuenoPerfil(prefs.getUsername());
                nivelGuardado = nivel;
                recalcularObjetivo(id);
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(TusDatosActivity.this);
                guardando = false;
                pintarPlan();
                // La API comprueba también el mínimo (DEC-038): se explica claro.
                if (code == 400 && ReglasEdad.esEdadMinima(message)) {
                    UIHelper.mostrarToastError(TusDatosActivity.this,
                            getString(R.string.error_edad_minima, ReglasEdad.MINIMA));
                    return;
                }
                UiFeedback.toastError(TusDatosActivity.this, code, message);
            }
        });
    }

    /**
     * Recalcula el objetivo con los datos nuevos y el % de grasa más reciente (GP-206). Pide las
     * mediciones para no usar un % viejo; si no llegan, va con el último guardado en el móvil.
     */
    private void recalcularObjetivo(int usuarioId) {
        LoadingDialog.show(this);
        ApiClient.service(MedicionApi.class).getOrdenadas(usuarioId).enqueue(new ApiCallback<List<MedicionCorporal>>() {
            @Override
            public void onOk(List<MedicionCorporal> lista) {
                prefs.saveGrasaReciente(GrasaReciente.elegir(lista));
                terminarGuardado();
            }

            @Override
            public void onFail(int code, String message) {
                // El perfil ya está guardado: se recalcula con el % que hubiera en el móvil, que
                // como mucho es el de la última vez, y el usuario no pierde nada.
                terminarGuardado();
            }
        });
    }

    private void terminarGuardado() {
        LoadingDialog.hide(this);
        DiaNutricion.recalcular(prefs);
        UIHelper.mostrarToastExito(this, getString(R.string.editar_perfil_guardado));
        setResult(RESULT_OK);
        finish();
    }
}
