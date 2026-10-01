package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.NivelVisible;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ReglasEdad;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EditarPerfilActivity — tus datos y objetivos (GP-105: Ajustes › Tus datos y
// objetivos).
// Nombre para mostrar (GP-116); peso, altura y edad; sexo y actividad; objetivo y nivel. Guarda vía PATCH lo que
// guarda la API y recalcula las macros nutricionales locales con los datos nuevos.
//
// Sexo y actividad van también a la API (GP-111) y se guardan en el teléfono, que es
// de donde calcula las macros; el perfil del teléfono queda apuntado a esta cuenta. El correo ya
// no está aquí: se cambia en Ajustes › Correo, con la contraseña (GP-083).
// ============================================================
public class EditarPerfilActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private PreferencesManager prefsManager;
    private TextInputEditText etNombre, etPeso, etAltura, etEdad;
    private Spinner spNivel, spObjetivo, spSexo, spActividad;
    // Cómo estaba el formulario al abrirlo (y al llegar el perfil): salir con algo
    // distinto pregunta antes de tirarlo (GP-108).
    private String[] textosIniciales = {"", "", "", ""};
    private int[] seleccionIniciales = {0, 0, 0, 0};
    // Interfaz Retrofit tipada del dominio usuarios (etapa 2)
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Valores enviados a la API para nivel de experiencia: tres (GP-103). Experto se
    // enseña como Avanzado y se conserva si no se cambia (NivelVisible).
    private static final String[] NIVELES = NivelVisible.OFRECIDOS;
    // El nivel que tiene la cuenta, tal cual, para no convertir un Experto en Avanzado.
    private String nivelGuardado;
    // Valores enviados a la API para el objetivo del usuario.
    private static final String[] OBJETIVOS = {
            "PERDER_PESO", "GANAR_MASA_MUSCULAR", "MANTENER_PESO", "MEJORAR_FUERZA"
    };
    // Valores de sexo y actividad, los mismos en el teléfono y en la API (GP-111).
    private static final String[] SEXOS = {"HOMBRE", "MUJER"};
    private static final String[] ACTIVIDADES = {
            CalculadoraNutricional.ACTIVIDAD_SEDENTARIO, CalculadoraNutricional.ACTIVIDAD_LIGERO,
            CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.ACTIVIDAD_ACTIVO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_editar_perfil);

        etNombre    = findViewById(R.id.etNombre);
        etPeso      = findViewById(R.id.etPeso);
        etAltura    = findViewById(R.id.etAltura);
        etEdad      = findViewById(R.id.etEdad);
        spNivel     = findViewById(R.id.spNivel);
        spObjetivo  = findViewById(R.id.spObjetivo);
        spSexo      = findViewById(R.id.spSexo);
        spActividad = findViewById(R.id.spActividad);

        // Lo guardado en el móvil primero; lo de la API lo corrige al llegar.
        etNombre.setText(prefsManager.getNombre());
        configurarSpinners();
        cargarDatosUsuario();
        guardarEstadoInicial();
        AvisoDescartar.instalar(this, findViewById(R.id.toolbar), this::hayCambios);
        vigilarNotaMenores();
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarPerfil());
    }

    // Spinners con textos localizados; los valores reales son las constantes de arriba.
    private void configurarSpinners() {
        rellenar(spNivel, getString(R.string.nivel_principiante), getString(R.string.nivel_intermedio),
                getString(R.string.nivel_avanzado));
        rellenar(spObjetivo, getString(R.string.objetivo_perder_peso), getString(R.string.objetivo_ganar_musculo),
                getString(R.string.objetivo_mantener), getString(R.string.objetivo_fuerza));
        rellenar(spSexo, getString(R.string.onboarding_hombre), getString(R.string.onboarding_mujer));
        rellenarActividad();
        seleccionarSpinner(spSexo, SEXOS, prefsManager.getSexo());
        seleccionarSpinner(spActividad, ACTIVIDADES, prefsManager.getActividad());
        nivelGuardado = prefsManager.getNivel();
        seleccionarSpinner(spNivel, NIVELES, NivelVisible.valor(nivelGuardado));
    }

    /**
     * La actividad con lo que cuenta cada opción (GP-103): en la lista desplegada, título
     * y descripción; cerrada, el título, con la descripción de la elegida debajo.
     */
    private void rellenarActividad() {
        int[] titulos = {R.string.onboarding_sedentario, R.string.onboarding_ligero,
                R.string.onboarding_moderado, R.string.onboarding_activo};
        int[] descripciones = {R.string.actividad_sedentario_desc, R.string.actividad_ligero_desc,
                R.string.actividad_moderado_desc, R.string.actividad_activo_desc};
        String[] textos = new String[titulos.length];
        for (int i = 0; i < titulos.length; i++) textos[i] = getString(titulos[i]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, textos) {
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView,
                                                     @androidx.annotation.NonNull android.view.ViewGroup parent) {
                android.widget.TextView v = (android.widget.TextView) super.getDropDownView(position, convertView, parent);
                v.setSingleLine(false);
                v.setText(getString(R.string.actividad_con_descripcion,
                        getString(titulos[position]), getString(descripciones[position])));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spActividad.setAdapter(adapter);
        android.widget.TextView desc = findViewById(R.id.tvActividadDesc);
        spActividad.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                desc.setText(descripciones[pos]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) {
                desc.setText(null);
            }
        });
    }

    private void rellenar(Spinner spinner, String... textos) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, textos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Obtiene los datos del usuario actual desde la API y rellena el formulario.
    private void cargarDatosUsuario() {
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;

        usuarioApi.getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null) return;
                etNombre.setText(u.getNombre() == null ? "" : u.getNombre());
                if (u.getPeso() != null && !u.getPeso().isEmpty()) etPeso.setText(u.getPeso());
                if (u.getAltura() > 0) etAltura.setText(String.valueOf((int) u.getAltura()));
                if (u.getEdad() > 0) etEdad.setText(String.valueOf(u.getEdad()));
                nivelGuardado = u.getNivelExperiencia();
                seleccionarSpinner(spNivel, NIVELES, NivelVisible.valor(nivelGuardado));
                seleccionarSpinner(spObjetivo, OBJETIVOS, u.getObjetivo());
                // Lo de la API manda sobre lo del móvil (GP-111); sin ellos, lo del móvil.
                seleccionarSpinner(spSexo, SEXOS, u.getSexo());
                seleccionarSpinner(spActividad, ACTIVIDADES, u.getNivelActividad());
                guardarEstadoInicial();
            }

            @Override
            public void onFail(int code, String message) {
                // Sin el perfil cargado, guardar pisaría los datos reales con el
                // formulario en blanco: se avisa. El 401 ya lo resuelve el aviso global.
                if (code != 401) {
                    UIHelper.mostrarToastError(EditarPerfilActivity.this,
                            getString(R.string.editar_perfil_error_carga));
                }
            }
        });
    }

    private void guardarEstadoInicial() {
        textosIniciales = new String[]{texto(etNombre), texto(etPeso), texto(etAltura), texto(etEdad)};
        seleccionIniciales = seleccion();
    }

    private boolean hayCambios() {
        return AvisoDescartar.distintos(textosIniciales, etNombre.getText(), etPeso.getText(), etAltura.getText(), etEdad.getText())
                || !java.util.Arrays.equals(seleccionIniciales, seleccion());
    }

    private int[] seleccion() {
        return new int[]{spNivel.getSelectedItemPosition(), spObjetivo.getSelectedItemPosition(),
                spSexo.getSelectedItemPosition(), spActividad.getSelectedItemPosition()};
    }

    private static String texto(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    // Selecciona en el spinner la posición cuyo valor coincide con el actual del usuario.
    private void seleccionarSpinner(Spinner spinner, String[] valores, String valorActual) {
        if (valorActual == null || valorActual.isEmpty()) return;
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].equalsIgnoreCase(valorActual)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    /**
     * De 14 a 17 años con «Perder grasa», la calculadora da las calorías de mantenimiento
     * (GP-103): la nota lo dice debajo del objetivo mientras se cumplan las dos cosas.
     */
    private void vigilarNotaMenores() {
        Runnable pintar = () -> {
            Integer edad = ReglasEdad.leer(texto(etEdad));
            boolean perder = CalculadoraNutricional.OBJETIVO_PERDER_PESO.equals(
                    OBJETIVOS[Math.max(0, spObjetivo.getSelectedItemPosition())]);
            boolean menor = edad != null && edad <= CalculadoraNutricional.EDAD_SIN_DEFICIT;
            findViewById(R.id.tvNotaObjetivo).setVisibility(perder && menor ? android.view.View.VISIBLE : android.view.View.GONE);
        };
        etEdad.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable e) { pintar.run(); }
        });
        spObjetivo.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                pintar.run();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) {
                pintar.run();
            }
        });
        pintar.run();
    }

    /**
     * Comprueba la edad escrita y, si no vale, lo dice en su campo (GP-145).
     *
     * <p>Por debajo de 14 no es un número mal escrito, es la edad mínima de la política
     * (DEC-038): se explica eso, no «pon un número entre…».
     *
     * @return true si está vacía o es de 14 a 100.
     */
    private boolean edadValida(String edadStr) {
        switch (ReglasEdad.estado(edadStr)) {
            case MENOR:
                marcarEdad(getString(R.string.error_edad_minima, ReglasEdad.MINIMA));
                return false;
            case FUERA:
                marcarEdad(getString(R.string.error_edad_rango, ReglasEdad.MINIMA, ReglasEdad.MAXIMA));
                return false;
            default:
                ((TextInputLayout) findViewById(R.id.tilEdad)).setError(null);
                return true;
        }
    }

    private void marcarEdad(String mensaje) {
        ((TextInputLayout) findViewById(R.id.tilEdad)).setError(mensaje);
        etEdad.requestFocus();
    }

    // Guarda el perfil por PATCH y, al tener éxito, guarda sexo y actividad en el
    // teléfono y recalcula las macros con los datos nuevos.
    private void guardarPerfil() {
        int id = prefsManager.getUsuarioId();
        try {
            // Un valor null no toca el campo: la API ignora los null del PATCH.
            Map<String, Object> body = new HashMap<>();

            // En blanco borra el nombre (GP-116): la API ignora el null, no el vacío.
            String nombre = NombreVisible.paraEnviar(etNombre.getText());
            body.put("nombre", nombre);

            String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
            body.put("peso", pesoStr.isEmpty() ? null : Numeros.leerExacto(pesoStr));

            String alturaStr = etAltura.getText() != null ? etAltura.getText().toString().trim() : "";
            body.put("altura", alturaStr.isEmpty() ? null : new BigDecimal(alturaStr));

            // De 14 a 100, leída sin romper con cualquier texto (GP-145).
            String edadStr = etEdad.getText() != null ? etEdad.getText().toString().trim() : "";
            if (!edadValida(edadStr)) return;
            Integer edad = ReglasEdad.leer(edadStr);
            body.put("edad", edad);

            String nivel = NivelVisible.aGuardar(NIVELES[spNivel.getSelectedItemPosition()], nivelGuardado);
            body.put("nivelExperiencia", nivel);
            body.put("objetivo", OBJETIVOS[spObjetivo.getSelectedItemPosition()]);
            body.put("sexo", SEXOS[spSexo.getSelectedItemPosition()]);
            body.put("nivelActividad", ACTIVIDADES[spActividad.getSelectedItemPosition()]);

            LoadingDialog.show(this);
            usuarioApi.patch(id, body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void response) {
                    LoadingDialog.hide(EditarPerfilActivity.this);
                    prefsManager.saveNombre(prefsManager.getUsername(), nombre);
                    if (!pesoStr.isEmpty()) prefsManager.savePeso(Numeros.leerDecimal(pesoStr));
                    if (!alturaStr.isEmpty()) prefsManager.saveAltura(Double.parseDouble(alturaStr));
                    if (edad != null) prefsManager.saveEdad(edad);
                    String objetivo = OBJETIVOS[spObjetivo.getSelectedItemPosition()];
                    prefsManager.saveObjetivo(objetivo);
                    prefsManager.saveNivel(nivel);
                    prefsManager.saveSexo(SEXOS[spSexo.getSelectedItemPosition()]);
                    prefsManager.saveActividad(ACTIVIDADES[spActividad.getSelectedItemPosition()]);
                    prefsManager.apuntarDuenoPerfil(prefsManager.getUsername());

                    // Recalcular macros con los nuevos datos
                    ResultadoNutricional r = CalculadoraNutricional.calcular(prefsManager.getPeso(),
                            prefsManager.getAltura(), prefsManager.getEdad(),
                            "HOMBRE".equals(prefsManager.getSexo()), prefsManager.getActividad(), objetivo);
                    prefsManager.saveResultadoNutricional(r.calorias, r.proteinas, r.carbohidratos, r.grasas, r.agua);

                    UIHelper.mostrarToastExito(EditarPerfilActivity.this, getString(R.string.editar_perfil_guardado));
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFail(int code, String message) {
                    LoadingDialog.hide(EditarPerfilActivity.this);
                    // La API comprueba también el mínimo (DEC-038): se explica en el campo.
                    if (code == 400 && ReglasEdad.esEdadMinima(message)) {
                        marcarEdad(getString(R.string.error_edad_minima, ReglasEdad.MINIMA));
                        return;
                    }
                    UiFeedback.toastError(EditarPerfilActivity.this, code, message);
                }
            });
        } catch (NumberFormatException e) {
            LoadingDialog.hide(this);
            UIHelper.mostrarToastError(this, getString(R.string.editar_perfil_error));
        }
    }
}
