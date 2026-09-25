package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.Valoracion;
import es.pmdm.gymprofit.utils.UiFeedback;
import com.google.android.material.appbar.MaterialToolbar;

// ============================================================
// RegistrarSesionActivity — Formulario para registrar una sesión de entrenamiento.
// Permite elegir una rutina (propia o predefinida), carga sus ejercicios con sus
// series, y guarda la sesión ENTERA en una sola llamada, navegando al resumen.
//
// GUARDADO (GP-006). Antes se creaba la sesión, se lanzaba un POST por ejercicio
// con los callbacks vacíos y se cerraba la pantalla sin esperar a nada. Si fallaba
// uno de los de en medio quedaba una sesión a medias y el usuario ya había visto
// «guardado correctamente». Ahora va todo en POST /sesiones/completa, la pantalla
// NO se cierra hasta recibir el éxito, y si falla se ofrece reintentar con lo
// tecleado todavía en pantalla.
//
// La clave de idempotencia se genera UNA vez por intento de guardado y el
// reintento reusa la misma: es lo que impide que un fallo de red que sí llegó al
// servidor acabe en dos entrenamientos.
//
// Retoque mínimo de interfaz a propósito: GP-012 rehará esta pantalla como sesión
// en vivo, así que aquí solo se arregla el guardado.
// ============================================================
public class RegistrarSesionActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private Spinner spRutina;
    private TextInputEditText etDuracion, etNotas;
    private View cardEjercicios;
    private RatingBar ratingBar;
    private TextView tvEstadoValoracion;
    private View btnQuitarValoracion;
    private PreferencesManager prefsManager;
    // Interfaz Retrofit tipada del dominio sesiones (etapa 2)
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    // Interfaz Retrofit tipada del dominio rutinas (etapa 2)
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private final List<Rutina> rutinas = new ArrayList<>();
    private final List<String> rutinaOpciones = new ArrayList<>();
    private final List<EjercicioPesoAdapter.Item> ejercicioItems = new ArrayList<>();

    /**
     * Clave del intento de guardado en curso.
     *
     * <p>Se genera al pulsar «Guardar» y NO se regenera al reintentar: si la
     * petición anterior sí llegó al servidor y lo único que se perdió fue la
     * respuesta, el servidor reconoce la clave y devuelve la sesión que ya creó en
     * vez de crear otra. Se limpia al guardar con éxito, para que el siguiente
     * entrenamiento sea un intento nuevo.
     */
    private String claveIntentoGuardado;
    private EjercicioPesoAdapter ejercicioPesoAdapter;

    // Inicializa la pantalla: monta vistas, configura el RecyclerView de
    // ejercicios/pesos y carga las rutinas disponibles para el spinner.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_registrar_sesion);

        spRutina             = findViewById(R.id.spRutina);
        etDuracion           = findViewById(R.id.etDuracion);
        etNotas              = findViewById(R.id.etNotas);
        cardEjercicios       = findViewById(R.id.cardEjercicios);
        ratingBar            = findViewById(R.id.ratingBar);
        tvEstadoValoracion   = findViewById(R.id.tvEstadoValoracion);
        btnQuitarValoracion  = findViewById(R.id.btnQuitarValoracion);
        configurarValoracion();

        RecyclerView rvEjercicios = findViewById(R.id.rvEjercicios);
        ejercicioPesoAdapter = new EjercicioPesoAdapter(ejercicioItems);
        rvEjercicios.setLayoutManager(new LinearLayoutManager(this));
        rvEjercicios.setNestedScrollingEnabled(false);
        rvEjercicios.setAdapter(ejercicioPesoAdapter);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarSesion());

        cargarRutinas();
    }

    /**
     * La valoración es opcional y arranca en «sin valorar» (GP-077).
     *
     * <p>Con el dedo, un RatingBar no baja a cero: el primer toque ya marca una
     * estrella. Por eso, en cuanto hay una, aparece «Quitar valoración» para
     * deshacer un toque por error.
     *
     * <p>TalkBack trata el RatingBar aparte: añade por su cuenta «N estrellas de 5»
     * y NO lee el stateDescription. Con estrellas eso basta; sin ellas diría
     * «0 estrellas de 5», que suena a nota y no a ausencia de nota, así que en
     * ese caso el «sin valorar» va en la propia descripción.
     */
    private void configurarValoracion() {
        ratingBar.setOnRatingBarChangeListener((bar, estrellas, delUsuario) -> pintarValoracion());
        btnQuitarValoracion.setOnClickListener(v -> {
            ratingBar.setRating(0f);
            // El botón desaparece con el toque: el foco vuelve a las estrellas, que
            // anuncian «Sin valorar», en vez de perderse.
            ratingBar.requestFocus();
            ratingBar.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
        });
        pintarValoracion();
    }

    // Refleja el estado de las estrellas en el rótulo, en TalkBack y en el botón.
    private void pintarValoracion() {
        Integer valor = Valoracion.paraEnviar(ratingBar.getRating());
        if (valor == null) {
            tvEstadoValoracion.setText(R.string.sesiones_sin_valorar);
            ratingBar.setContentDescription(getString(R.string.sesiones_valoracion_desc_sin));
            ViewCompat.setStateDescription(ratingBar, getString(R.string.sesiones_sin_valorar));
            // INVISIBLE y no GONE: el hueco se queda y la tarjeta no salta al tocar.
            btnQuitarValoracion.setVisibility(View.INVISIBLE);
        } else {
            tvEstadoValoracion.setText(getString(R.string.sesiones_valoracion_estado, valor));
            ratingBar.setContentDescription(getString(R.string.sesiones_valoracion_desc));
            ViewCompat.setStateDescription(ratingBar, getString(R.string.sesiones_valoracion_a11y, valor));
            btnQuitarValoracion.setVisibility(View.VISIBLE);
        }
    }

    // Carga en paralelo las rutinas predefinidas y las del usuario; cuando
    // ambas llamadas terminan, combina los resultados y actualiza el spinner.
    private void cargarRutinas() {
        int usuarioId = prefsManager.getUsuarioId();
        rutinaOpciones.clear();
        // La opción 0 crea la sesión sin rutinaId, que es exactamente lo que la lista
        // y el resumen llaman «entrenamiento libre» (GP-057). Antes decía «sin rutina
        // asociada»: la misma cosa con dos nombres según la pantalla.
        rutinaOpciones.add(getString(R.string.sesiones_entrenamiento_libre));

        final List<Rutina> predefinidas = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(2);

        // Rutinas predefinidas del sistema (ya deserializadas por Gson).
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> lista) {
                if (lista != null) predefinidas.addAll(lista);
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
            @Override public void onFail(int code, String message) {
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
        });

        // Rutinas activas del usuario (ya deserializadas por Gson).
        rutinaApi.getDeUsuarioActivas(usuarioId).enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> lista) {
                if (lista != null) rutinas.addAll(lista);
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
            @Override public void onFail(int code, String message) {
                if (pendientes.decrementAndGet() == 0) combinarYMostrar(predefinidas);
            }
        });
    }

    /**
     * Id de la rutina con la que se ha entrado, o -1 si se abrió sin rutina.
     *
     * <p>Lo manda quien lanza la pantalla desde el detalle de una rutina o desde
     * el Home. Antes la pantalla solo se abría desde el historial y siempre
     * arrancaba en "Sin rutina asociada", así que el camino por defecto producía
     * una sesión sin ejercicios: sin ejercicios no hay progreso y sin progreso la
     * gráfica del récord no aparece nunca.
     */
    public static final String EXTRA_RUTINA_ID = "rutinaId";

    // Une rutinas predefinidas y del usuario en una única lista y refresca
    // el spinner en el hilo principal.
    private void combinarYMostrar(List<Rutina> predefinidas) {
        List<Rutina> todas = new ArrayList<>(predefinidas);
        todas.addAll(rutinas);
        rutinas.clear();
        rutinas.addAll(todas);
        for (Rutina r : todas) rutinaOpciones.add(r.getNombre());
        actualizarSpinner();
        preseleccionarRutina();
    }

    // Rellena el spinner de rutinas y, al seleccionar una, calcula sus
    // calorías estimadas; si se selecciona "sin rutina", limpia los cálculos.
    private void actualizarSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, rutinaOpciones);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRutina.setAdapter(spinnerAdapter);
        spRutina.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= rutinas.size()) {
                    cargarEjerciciosDeRutina(rutinas.get(position - 1).getId());
                } else {
                    ejercicioItems.clear();
                    ejercicioPesoAdapter.notifyDataSetChanged();
                    cardEjercicios.setVisibility(View.GONE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Deja marcada en el selector la rutina con la que se entró, si la hay.
     *
     * <p>Se llama después de montar el spinner, porque hasta que no han llegado
     * las dos listas (predefinidas y propias) no se sabe en qué posición cae.
     */
    private void preseleccionarRutina() {
        int rutinaId = getIntent().getIntExtra(EXTRA_RUTINA_ID, -1);
        if (rutinaId == -1) return;

        for (int i = 0; i < rutinas.size(); i++) {
            if (rutinas.get(i).getId() == rutinaId) {
                // +1 porque la posición 0 del spinner es "Entrenamiento libre".
                spRutina.setSelection(i + 1);
                return;
            }
        }
    }

    // Obtiene los ejercicios de la rutina seleccionada y arma la lista de
    // ejercicios/pesos que se mostrará en el RecyclerView.
    //
    // Antes esto también calculaba calorías (calorías × series × repeticiones) y las
    // pintaba en una tarjeta. Se retira con DEC-004 / GP-010: esa multiplicación no
    // conoce la carga, ni el peso del usuario, ni el descanso.
    private void cargarEjerciciosDeRutina(int rutinaId) {
        // Relaciones rutina-ejercicio (ya deserializadas por Gson): traen el nombre
        // enriquecido desde el catálogo, igual que devolvía el JSON antiguo.
        rutinaApi.getEjerciciosDeRutina(rutinaId).enqueue(new ApiCallback<List<RutinaEjercicio>>() {
            @Override
            public void onOk(List<RutinaEjercicio> lista) {
                List<EjercicioPesoAdapter.Item> nuevosItems = new ArrayList<>();
                if (lista != null) {
                    int i = 0;
                    for (RutinaEjercicio re : lista) {
                        i++;
                        int series      = re.getSeries();
                        int reps        = re.getRepeticiones();
                        int ejercicioId = re.getEjercicioId();
                        String nombre   = (re.getNombreEjercicio() != null && !re.getNombreEjercicio().isEmpty())
                                ? re.getNombreEjercicio() : getString(R.string.ejercicio_sin_nombre, i);
                        if (ejercicioId != -1) {
                            nuevosItems.add(new EjercicioPesoAdapter.Item(ejercicioId, nombre, series, reps));
                        }
                    }
                }
                ejercicioItems.clear();
                ejercicioItems.addAll(nuevosItems);
                ejercicioPesoAdapter.notifyDataSetChanged();
                cardEjercicios.setVisibility(nuevosItems.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onFail(int code, String message) {
                // Sin los ejercicios de la rutina no hay nada que rellenar, pero la
                // sesión se puede guardar igual con su duración y sus notas. Se avisa
                // y se deja la tarjeta oculta: callarlo dejaría al usuario creyendo
                // que esa rutina no tiene ejercicios.
                UiFeedback.toastError(RegistrarSesionActivity.this, code, message);
                ejercicioItems.clear();
                ejercicioPesoAdapter.notifyDataSetChanged();
                cardEjercicios.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Valida la duración, arma el cuerpo con la sesión entera y la envía.
     *
     * <p>Un mismo intento de guardado puede mandarse varias veces: la clave se genera
     * aquí solo si no había ninguna, de modo que el reintento reusa la del fallo.
     */
    private void guardarSesion() {
        String durStr = etDuracion.getText() != null ? etDuracion.getText().toString().trim() : "";
        if (durStr.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            return;
        }

        Integer duracion = Numeros.entero(durStr, 1, 600);
        if (duracion == null) {
            UIHelper.mostrarToastError(this, getString(R.string.sesiones_duracion_invalida));
            return;
        }

        if (claveIntentoGuardado == null) {
            claveIntentoGuardado = java.util.UUID.randomUUID().toString();
        }

        enviarSesion(duracion);
    }

    /**
     * Manda la sesión completa y decide qué pasa después.
     *
     * <p>La pantalla NO se cierra hasta el éxito. Si falla, lo tecleado sigue donde
     * estaba y se ofrece reintentar con la MISMA clave.
     */
    private void enviarSesion(int duracion) {
        Map<String, Object> body = new HashMap<>();
        body.put("claveIdempotencia", claveIntentoGuardado);

        int posicion = spRutina.getSelectedItemPosition();
        if (posicion > 0 && posicion <= rutinas.size()) {
            body.put("rutinaId", rutinas.get(posicion - 1).getId());
        }

        body.put("fechaInicio", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
        body.put("duracionMinutos", duracion);
        body.put("completada", true);

        // La valoración viaja como CAMPO (GP-070). Antes se formateaba con un recurso
        // de idioma y se metía delante de las notas del usuario: no se podía consultar,
        // se quedaba congelada en el idioma del momento, y el texto no era suyo.
        // Sin estrellas no se manda el campo: la base guarda NULL, no un valor que
        // el usuario no ha dado (GP-077).
        Integer valoracion = Valoracion.paraEnviar(ratingBar.getRating());
        if (valoracion != null) body.put("valoracion", valoracion);

        String notas = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";
        if (!notas.isEmpty()) body.put("notas", notas);

        body.put("ejercicios", construirEjercicios());

        LoadingDialog.show(this);
        sesionApi.guardarCompleta(body).enqueue(new ApiCallback<SesionEntrenamiento>() {
            @Override
            public void onOk(SesionEntrenamiento sesionCreada) {
                LoadingDialog.hide(RegistrarSesionActivity.this);

                if (sesionCreada == null || sesionCreada.getId() <= 0) {
                    // Respuesta 200 sin sesión dentro: no hay nada que enseñar en el
                    // resumen, y cerrar aquí sería volver a mentir. Se trata como fallo.
                    ofrecerReintento(duracion, getString(R.string.sesiones_error_guardar));
                    return;
                }

                // El intento terminó: la clave siguiente será otra.
                claveIntentoGuardado = null;

                UIHelper.mostrarToastExito(RegistrarSesionActivity.this, getString(R.string.sesiones_exito));
                setResult(RESULT_OK);
                irAlResumen(sesionCreada);
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(RegistrarSesionActivity.this);
                ofrecerReintento(duracion, UiFeedback.mensaje(RegistrarSesionActivity.this, code));
            }
        });
    }

    /**
     * Avisa de que NO se ha guardado y ofrece repetir el envío.
     *
     * <p>Lo importante no es el diálogo, es lo que NO pasa: no se cierra la pantalla,
     * no se borra nada de lo tecleado y no se cambia la clave. Cancelar deja al usuario
     * donde estaba, con sus datos, para corregir o volver a intentarlo cuando quiera.
     */
    private void ofrecerReintento(int duracion, String motivo) {
        UIHelper.mostrarDialogoConIcono(this,
                getString(R.string.sesiones_error_guardar_titulo),
                motivo + "\n\n" + getString(R.string.sesiones_error_guardar_ayuda),
                R.drawable.ic_ms_error,
                getString(R.string.sesiones_error_reintentar),
                getString(R.string.sesiones_error_ahora_no),
                () -> enviarSesion(duracion));
    }

    // Abre el resumen de la sesión recién guardada.
    private void irAlResumen(SesionEntrenamiento sesion) {
        // La posición 0 no es "no lo sé", es entrenamiento libre: se le dice al resumen
        // para que no lo pinte como una rutina sin nombre.
        int pos = spRutina.getSelectedItemPosition();
        boolean libre = pos <= 0;
        String nombreRutina = (pos > 0 && pos <= rutinas.size()) ? rutinas.get(pos - 1).getNombre() : "";

        ArrayList<String> nuevosLogros = new ArrayList<>();
        if (sesion.getNuevosLogros() != null) nuevosLogros.addAll(sesion.getNuevosLogros());

        Intent intent = new Intent(this, ResumenSesionActivity.class);
        intent.putExtra("sesionId", sesion.getId());
        intent.putExtra("rutinaNombre", nombreRutina);
        intent.putExtra(ResumenSesionActivity.EXTRA_ENTRENAMIENTO_LIBRE, libre);
        intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
        startActivity(intent);
    }

    /**
     * Arma la lista de ejercicios que viaja DENTRO de la sesión.
     *
     * <p>Antes cada ejercicio era un POST suelto que se lanzaba tras crear la sesión,
     * con el callback vacío: si fallaba, nadie se enteraba. Ahora van en el mismo
     * cuerpo y el servidor los guarda o los rechaza junto con ella.
     *
     * <p>Los ejercicios sin ninguna serie que guardar se descartan: son los que el
     * usuario dejó en blanco porque no llegó a hacerlos.
     */
    private List<Map<String, Object>> construirEjercicios() {
        List<Map<String, Object>> ejercicios = new ArrayList<>();

        for (EjercicioPesoAdapter.Item item : ejercicioItems) {
            List<Map<String, Object>> series = construirSeries(item);
            if (series.isEmpty()) continue;

            Map<String, Object> ejercicio = new HashMap<>();
            ejercicio.put("ejercicioId", item.ejercicioId);
            ejercicio.put("repeticionesReales", item.repeticiones);
            ejercicio.put("series", series);
            ejercicios.add(ejercicio);
        }

        return ejercicios;
    }

    /**
     * Convierte lo que el usuario tecleó en el cuerpo que espera la API.
     *
     * <p>El peso y las repeticiones pasan por {@link Numeros}, que acepta coma o
     * punto y devuelve null fuera de rango en vez de lanzar: un valor imposible
     * se descarta, nunca se sustituye por uno inventado.
     *
     * @return una entrada por serie con algo que guardar; puede venir vacía.
     */
    private List<Map<String, Object>> construirSeries(EjercicioPesoAdapter.Item item) {
        List<Map<String, Object>> series = new ArrayList<>();

        for (EjercicioPesoAdapter.Serie serie : item.realizadas) {
            Integer reps = Numeros.entero(serie.repeticiones, 0, 100);
            BigDecimal peso = Numeros.exacto(serie.peso, 0, 500);

            // Una serie en blanco es una serie que no se hizo.
            if (reps == null && peso == null) continue;

            Map<String, Object> fila = new HashMap<>();
            fila.put("numero", serie.numero);
            fila.put("repeticiones", reps != null ? reps : 0);
            if (peso != null) fila.put("peso", peso);
            fila.put("completada", serie.completada);
            series.add(fila);
        }

        return series;
    }
}
