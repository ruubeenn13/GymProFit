package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.RegistroSesionRepositorioApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel;
import es.pmdm.gymprofit.ui.viewmodels.RegistrarSesionViewModel.EstadoGuardado;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.Valoracion;

// ============================================================
// RegistrarSesionActivity — Apuntar un entrenamiento ya hecho (GP-012), desde el «+»:
// con su fecha (hoy por defecto, nunca futura) y su duración; el inicio es ese día a la
// hora de ahora menos la duración. Empezar a entrenar abre la sesión en vivo.
//
// Formulario para registrar una sesión de entrenamiento.
// Permite elegir una rutina (propia o predefinida), carga sus ejercicios con sus
// series, y guarda la sesión ENTERA en una sola llamada, navegando al resumen.
//
// GUARDADO (GP-006). Todo va en POST /sesiones/completa, la pantalla NO se cierra
// hasta recibir el éxito, y si falla se ofrece reintentar con lo tecleado intacto y
// la misma clave de idempotencia.
//
// BORRADOR (GP-016). La pantalla ya no guarda nada: lo tecleado, la rutina elegida,
// la clave y el guardado en marcha viven en RegistrarSesionViewModel, que los escribe
// en su SavedStateHandle. Girar, «No conservar actividades» o que Android mate la app
// en segundo plano no pierden nada. Aquí solo se pinta ese estado y se le pasa cada
// cambio; la red tampoco se llama desde aquí, sino desde el repositorio.
//
// Retoque mínimo de interfaz a propósito: GP-012 rehará esta pantalla como sesión
// en vivo.
// ============================================================
public class RegistrarSesionActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    /**
     * Id de la rutina con la que se ha entrado, o -1 si se abrió sin rutina.
     *
     * <p>Lo manda quien lanza la pantalla desde el detalle de una rutina o desde
     * el Home. Solo cuenta la primera vez: al restaurar manda lo que eligió el usuario.
     */
    public static final String EXTRA_RUTINA_ID = "rutinaId";

    private Spinner spRutina;
    private TextInputEditText etDuracion, etNotas;
    private View cardEjercicios;
    private RatingBar ratingBar;
    private TextView tvEstadoValoracion;
    private View btnQuitarValoracion;

    private RegistrarSesionViewModel vm;
    private final List<EjercicioPesoAdapter.Item> ejercicioItems = new ArrayList<>();
    private EjercicioPesoAdapter ejercicioPesoAdapter;

    // Las rutinas que hay ahora en el selector, en su orden (posición 0 = libre).
    private List<Rutina> rutinasMostradas = new ArrayList<>();
    // Posición del selector que ya refleja el borrador. El Spinner avisa también de las
    // selecciones que pone el código (al montarlo, al restaurar): esas no son una
    // elección del usuario y no deben recargar ejercicios encima de lo tecleado.
    private int posicionSincronizada = -1;
    // El aviso de fallo abierto, para no apilar otro y cerrarlo al destruir la pantalla.
    private Dialog avisoFallo;

    // Inicializa la pantalla: monta las vistas, crea (o recupera) el ViewModel, pinta
    // el borrador que tenga y se suscribe a sus cambios.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_registrar_sesion);

        spRutina             = findViewById(R.id.spRutina);
        etDuracion           = findViewById(R.id.etDuracion);
        etNotas              = findViewById(R.id.etNotas);
        cardEjercicios       = findViewById(R.id.cardEjercicios);
        ratingBar            = findViewById(R.id.ratingBar);
        tvEstadoValoracion   = findViewById(R.id.tvEstadoValoracion);
        btnQuitarValoracion  = findViewById(R.id.btnQuitarValoracion);

        int usuarioId = prefsManager.getUsuarioId();
        vm = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory(this, null) {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                                                     @NonNull SavedStateHandle handle) {
                return (T) new RegistrarSesionViewModel(handle, new RegistroSesionRepositorioApi(usuarioId));
            }
        }).get(RegistrarSesionViewModel.class);

        int rutinaDeEntrada = getIntent().getIntExtra(EXTRA_RUTINA_ID, -1);
        vm.iniciar(rutinaDeEntrada != -1 ? rutinaDeEntrada : null);

        pintarCamposDelBorrador();
        configurarFecha();
        configurarValoracion();

        RecyclerView rvEjercicios = findViewById(R.id.rvEjercicios);
        ejercicioPesoAdapter = new EjercicioPesoAdapter(ejercicioItems, vm);
        rvEjercicios.setLayoutManager(new LinearLayoutManager(this));
        rvEjercicios.setNestedScrollingEnabled(false);
        rvEjercicios.setAdapter(ejercicioPesoAdapter);

        // Salir con algo apuntado pregunta antes de tirarlo (GP-098).
        AvisoDescartar.instalar(this, findViewById(R.id.toolbar), vm::hayDatos);
        findViewById(R.id.btnGuardar).setOnClickListener(v -> pulsarGuardar());

        vm.getRutinas().observe(this, this::pintarRutinas);
        vm.getEjercicios().observe(this, this::pintarEjercicios);
        vm.getErrorEjercicios().observe(this, evento -> {
            RegistrarSesionViewModel.Fallo fallo = evento.tomar();
            if (fallo != null) UiFeedback.toastError(this, fallo.codigo, fallo.mensaje);
        });
        vm.getEstadoGuardado().observe(this, this::pintarGuardado);
    }

    /**
     * Pone en los campos lo que tenga el borrador y engancha la escritura hacia él.
     *
     * <p>El guardado de estado propio de estas vistas se apaga: la verdad es el
     * ViewModel, y que Android restaure por su cuenta la posición del selector —de una
     * lista que se ha vuelto a pedir y puede venir en otro orden— elegiría otra rutina.
     */
    private void pintarCamposDelBorrador() {
        etDuracion.setSaveEnabled(false);
        etNotas.setSaveEnabled(false);
        ratingBar.setSaveEnabled(false);
        spRutina.setSaveEnabled(false);

        etDuracion.setText(vm.getDuracion());
        etNotas.setText(vm.getNotas());
        ratingBar.setRating(vm.getValoracion());

        etDuracion.addTextChangedListener(new AlCambiar(vm::setDuracion));
        etNotas.addTextChangedListener(new AlCambiar(vm::setNotas));
    }

    /**
     * La fecha del entrenamiento (GP-012): hoy por defecto y nunca futura. El calendario
     * no deja elegir un día posterior a hoy, y el ViewModel tampoco lo acepta.
     */
    private void configurarFecha() {
        TextInputEditText etFecha = findViewById(R.id.etFecha);
        etFecha.setSaveEnabled(false);
        pintarFecha(etFecha);
        View.OnClickListener abrir = v -> {
            long hoyUtc = com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds();
            com.google.android.material.datepicker.MaterialDatePicker<Long> picker =
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                            .setTitleText(R.string.sesiones_fecha)
                            .setSelection(utcDe(vm.getFecha()))
                            .setCalendarConstraints(new com.google.android.material.datepicker.CalendarConstraints.Builder()
                                    .setEnd(hoyUtc)
                                    .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())
                                    .build())
                            .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                vm.setFecha(f.format(new java.util.Date(sel)));
                pintarFecha(etFecha);
            });
            picker.show(getSupportFragmentManager(), "fecha");
        };
        etFecha.setOnClickListener(abrir);
        ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.tilFecha))
                .setEndIconOnClickListener(abrir);
    }

    // El día elegido, en medianoche UTC, que es como lo cuenta el calendario.
    private static long utcDe(String dia) {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Integer.parseInt(dia.substring(0, 4)), Integer.parseInt(dia.substring(5, 7)) - 1,
                Integer.parseInt(dia.substring(8, 10)));
        return c.getTimeInMillis();
    }

    // «30 sept 2026», en el idioma de la app, y TalkBack lo dice con «Cambiar».
    private void pintarFecha(TextInputEditText etFecha) {
        String texto = es.pmdm.gymprofit.utils.FechaUtils.formatearFechaMedia(vm.getFecha(),
                es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(this));
        etFecha.setText(texto);
        etFecha.setContentDescription(getString(R.string.sesiones_fecha_a11y, texto));
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
        ratingBar.setOnRatingBarChangeListener((bar, estrellas, delUsuario) -> {
            vm.setValoracion(estrellas);
            pintarValoracion();
        });
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

    /**
     * Monta el selector con las rutinas recién llegadas y deja marcada la del borrador.
     *
     * <p>La opción 0 crea la sesión sin rutinaId, que es exactamente lo que la lista y
     * el resumen llaman «entrenamiento libre» (GP-057).
     */
    private void pintarRutinas(List<Rutina> rutinas) {
        rutinasMostradas = rutinas != null ? rutinas : new ArrayList<>();
        List<String> opciones = new ArrayList<>();
        opciones.add(getString(R.string.sesiones_entrenamiento_libre));
        for (Rutina r : rutinasMostradas) opciones.add(r.getNombre());

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        posicionSincronizada = posicionDe(vm.getRutinaId());
        spRutina.setAdapter(spinnerAdapter);
        spRutina.setSelection(posicionSincronizada, false);
        spRutina.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == posicionSincronizada) return;
                posicionSincronizada = position;
                boolean deLista = position > 0 && position <= rutinasMostradas.size();
                vm.elegirRutina(deLista ? rutinasMostradas.get(position - 1).getId() : null);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Posición del selector para una rutina (+1 por el «Entrenamiento libre» del 0).
    private int posicionDe(Integer rutinaId) {
        if (rutinaId == null) return 0;
        for (int i = 0; i < rutinasMostradas.size(); i++) {
            if (rutinasMostradas.get(i).getId() == rutinaId) return i + 1;
        }
        return 0;
    }

    // Pinta los ejercicios del borrador. Solo llega al cambiar de rutina o al restaurar:
    // teclear no repinta la lista, que se llevaría el foco del campo.
    //
    // Antes esto también calculaba calorías y las pintaba en una tarjeta. Se retiró con
    // DEC-004 / GP-010: esa multiplicación no conoce la carga, ni el peso, ni el descanso.
    private void pintarEjercicios(List<EjercicioPesoAdapter.Item> items) {
        ejercicioItems.clear();
        if (items != null) ejercicioItems.addAll(items);
        ejercicioPesoAdapter.notifyDataSetChanged();
        cardEjercicios.setVisibility(ejercicioItems.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // Pulsar Guardar: el ViewModel valida y manda; aquí solo se dice lo que falta.
    private void pulsarGuardar() {
        switch (vm.guardar()) {
            case FALTA_DURACION:
                UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
                break;
            case DURACION_INVALIDA:
                UIHelper.mostrarToastError(this, getString(R.string.sesiones_duracion_invalida));
                break;
            default:
                // ENVIADO lo pinta el estado; YA_EN_CURSO es un doble toque y no hace nada.
                break;
        }
    }

    /**
     * Pinta el guardado. Tras girar, la pantalla nueva recibe el estado en curso: si
     * sigue guardando, vuelve el «Cargando…»; si falló y nadie cerró el aviso, vuelve el
     * aviso; y el éxito abre el resumen una sola vez, porque se marca como abierto.
     */
    private void pintarGuardado(EstadoGuardado estado) {
        switch (estado.fase) {
            case GUARDANDO:
                LoadingDialog.show(this);
                break;
            case FALLO:
                LoadingDialog.hide(this);
                ofrecerReintento(estado);
                break;
            case EXITO:
                LoadingDialog.hide(this);
                vm.resumenAbierto();
                UIHelper.mostrarToastExito(this, getString(R.string.sesiones_exito));
                setResult(RESULT_OK);
                irAlResumen(estado.sesion);
                finish();
                break;
            default:
                LoadingDialog.hide(this);
                break;
        }
    }

    /**
     * Avisa de que NO se ha guardado —o de que no se sabe— y ofrece repetir el envío.
     *
     * <p>Lo importante no es el diálogo, es lo que NO pasa: no se cierra la pantalla,
     * no se borra nada de lo tecleado y no se cambia la clave. «Ahora no» deja al
     * usuario donde estaba, con sus datos, para volver a guardar cuando quiera.
     */
    private void ofrecerReintento(EstadoGuardado estado) {
        if (avisoFallo != null && avisoFallo.isShowing()) return;

        String titulo;
        String mensaje;
        if (estado.interrumpido) {
            titulo = getString(R.string.sesiones_error_interrumpido_titulo);
            mensaje = getString(R.string.sesiones_error_interrumpido);
        } else {
            titulo = getString(R.string.sesiones_error_guardar_titulo);
            String motivo = estado.codigo == 0
                    ? getString(R.string.sesiones_error_guardar)
                    : UiFeedback.mensaje(this, estado.codigo, estado.mensaje);
            mensaje = motivo + "\n\n" + getString(R.string.sesiones_error_guardar_ayuda);
        }

        avisoFallo = UIHelper.mostrarDialogoConIcono(this, titulo, mensaje, R.drawable.ic_ms_error,
                getString(R.string.sesiones_error_reintentar),
                getString(R.string.sesiones_error_ahora_no),
                () -> {
                    avisoFallo = null;
                    pulsarGuardar();
                },
                () -> {
                    avisoFallo = null;
                    vm.descartarFallo();
                });
    }

    // Al volver de segundo plano con el guardado aún en marcha, el «Cargando…» vuelve:
    // el LiveData no repite un estado que ya entregó.
    @Override
    protected void onStart() {
        super.onStart();
        EstadoGuardado estado = vm.getEstadoGuardado().getValue();
        if (estado != null && estado.fase == RegistrarSesionViewModel.Fase.GUARDANDO) {
            LoadingDialog.show(this);
        }
    }

    // El «Cargando…» se quita al dejar de verse: cerrado a tiempo no queda colgado de
    // una pantalla destruida al girar. onStart lo vuelve a poner si sigue guardando.
    @Override
    protected void onStop() {
        LoadingDialog.hide(this);
        super.onStop();
    }

    // El aviso de fallo se cierra sin contar como «Ahora no»: la pantalla nueva lo
    // vuelve a enseñar, porque en el ViewModel sigue abierto.
    @Override
    protected void onDestroy() {
        if (avisoFallo != null) {
            avisoFallo.dismiss();
            avisoFallo = null;
        }
        super.onDestroy();
    }

    // Abre el resumen de la sesión recién guardada.
    private void irAlResumen(SesionEntrenamiento sesion) {
        // Sin rutina no es "no lo sé", es entrenamiento libre: se le dice al resumen
        // para que no lo pinte como una rutina sin nombre.
        Integer rutinaId = vm.getRutinaId();

        ArrayList<String> nuevosLogros = new ArrayList<>();
        if (sesion.getNuevosLogros() != null) nuevosLogros.addAll(sesion.getNuevosLogros());

        Intent intent = new Intent(this, ResumenSesionActivity.class);
        intent.putExtra("sesionId", sesion.getId());
        intent.putExtra("rutinaNombre", vm.nombreRutina(rutinaId));
        intent.putExtra(ResumenSesionActivity.EXTRA_ENTRENAMIENTO_LIBRE, rutinaId == null);
        intent.putStringArrayListExtra("nuevosLogros", nuevosLogros);
        intent.putExtra(ResumenSesionActivity.EXTRA_RECORDS, new ArrayList<>(sesion.getRecordsBatidos()));
        intent.putExtra(ResumenSesionActivity.EXTRA_PRIMERAS_MARCAS,
                Marcas.ejerciciosDistintos(sesion.getPrimerasMarcas()));
        startActivity(intent);
    }

    // TextWatcher mínimo: pasa el texto entero al borrador en cada cambio.
    private static class AlCambiar implements TextWatcher {
        interface Destino { void set(String valor); }

        private final Destino destino;

        AlCambiar(Destino destino) { this.destino = destino; }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { destino.set(s.toString()); }
    }
}
