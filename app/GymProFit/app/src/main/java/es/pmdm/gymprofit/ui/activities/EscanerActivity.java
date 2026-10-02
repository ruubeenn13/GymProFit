package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.adapters.BusquedaAlimentoAdapter;
import es.pmdm.gymprofit.ui.nutricion.MarcoEscaner;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.CodigoBarras;
import es.pmdm.gymprofit.utils.EstadoEscaner;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PedidoCantidad;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EscanerActivity — escanear un código de barras (tablero 6, lote 1.6.1)
//
// La cámara con CameraX y la lectura en el móvil con ML Kit (EAN-13, EAN-8, UPC-A y
// UPC-E), con el modelo de Google Play Services, que se pide la primera vez que se abre
// el escáner y se dice mientras baja: la imagen no sale del móvil ni se guarda.
// Lo que se hace con cada lectura y cada respuesta lo decide EstadoEscaner: mientras
// hay hoja no se lee otro código.
//   · Permiso: se pide al entrar, con una línea de para qué. Si se niega, se explica y
//     quedan «Escribir el código» y el acceso a los ajustes.
//   · Al leer: vibración, esquinas en verde (momento 17) y la hoja con el producto y las
//     cifras de su ración por defecto (el envase si lo tiene; si no, 100 g), «Cambiar
//     cantidad» (la ficha), «Añadir a la merienda» (con esa ración, POST /comidas/anadir)
//     y «Escanear otro».
//   · 404: «No lo tenemos todavía», con «Crear con este código». 503 o sin red: lo dice
//     y deja reintentar.
//   · «Escribir el código» valida la longitud y el dígito de control antes de preguntar.
// La cámara va atada al ciclo de vida de la pantalla: al salir se suelta sola, y el
// lector y su hilo se cierran en onDestroy.
// ============================================================
public class EscanerActivity extends BaseActivity {

    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final EstadoEscaner estado = new EstadoEscaner();

    private String tipoComida;
    @Nullable private String fecha;
    @Nullable private Alimento alimento;

    private MarcoEscaner marco;
    private TextView aviso;
    private View hoja;
    private ImageButton btnLinterna;
    @Nullable private Camera camara;
    private boolean linterna;

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private BarcodeScanner lector;
    // Lo lee el hilo del análisis: solo se analizan imágenes mientras se busca. El primer
    // análisis que sale bien dice que el modelo de lectura ya está en el móvil.
    private volatile boolean analizar;
    private volatile boolean lectorListo;

    private ActivityResultLauncher<String> permiso;
    private ActivityResultLauncher<Intent> despues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escaner);
        tipoComida = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_TIPO);
        if (tipoComida == null) tipoComida = "MERIENDA";
        fecha = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_FECHA);

        marco = findViewById(R.id.marcoEscaner);
        aviso = findViewById(R.id.tvAvisoEscaner);
        hoja = findViewById(R.id.hojaEscaner);
        btnLinterna = findViewById(R.id.btnLinterna);
        btnLinterna.setVisibility(View.GONE);

        findViewById(R.id.btnCerrarEscaner).setOnClickListener(v -> finish());
        btnLinterna.setOnClickListener(v -> alternarLinterna());
        findViewById(R.id.btnEscribirCodigo).setOnClickListener(v -> escribirCodigo());
        findViewById(R.id.btnOtroMas).setOnClickListener(v -> otroMas());
        findViewById(R.id.btnAjustesCamara).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null))));

        lector = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build());
        prepararLector();

        permiso = registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            if (concedido) iniciarCamara();
            else sinPermiso();
        });
        // Cambiar cantidad (la ficha) o crear con el código: si se añade, se vuelve a quien
        // abrió el escáner con lo añadido (Añadir lo cuenta en su barra, lote 1.6.3).
        despues = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == RESULT_OK) {
                setResult(RESULT_OK, r.getData());
                finish();
            }
        });

        if (tienePermiso()) {
            iniciarCamara();
        } else {
            // La línea de para qué, mientras el sistema pregunta.
            aviso.setText(R.string.escaner_permiso_para);
            permiso.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vuelve de los ajustes con el permiso dado.
        if (camara == null && tienePermiso() && findViewById(R.id.panelSinPermiso).getVisibility() == View.VISIBLE) {
            findViewById(R.id.panelSinPermiso).setVisibility(View.GONE);
            iniciarCamara();
        }
    }

    @Override
    protected void onDestroy() {
        analizar = false;
        lector.close();
        hilo.shutdown();
        super.onDestroy();
    }

    // El modelo de lectura llega con Google Play Services: si aún no está en el móvil, se
    // pide y se dice («Preparando el lector…») hasta que llega. Sin él queda escribir.
    private void prepararLector() {
        ModuleInstallClient instalador = ModuleInstall.getClient(this);
        instalador.areModulesAvailable(lector).addOnSuccessListener(r -> {
            if (r.areModulesAvailable()) {
                lectorPreparado();
                return;
            }
            // Si ya ha analizado una imagen, el lector funciona: la respuesta llega tarde.
            if (lectorListo || isDestroyed()) return;
            if (camara != null || !tienePermiso()) aviso.setText(R.string.escaner_preparando);
            ModuleInstallRequest pedido = ModuleInstallRequest.newBuilder().addApi(lector)
                    .setListener(estadoInstalacion -> {
                        int fase = estadoInstalacion.getInstallState();
                        if (fase == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED) lectorPreparado();
                        else if (fase == ModuleInstallStatusUpdate.InstallState.STATE_FAILED
                                || fase == ModuleInstallStatusUpdate.InstallState.STATE_CANCELED) sinLector();
                    }).build();
            instalador.installModules(pedido).addOnSuccessListener(respuesta -> {
                if (respuesta.areModulesAlreadyInstalled()) lectorPreparado();
            }).addOnFailureListener(e -> sinLector());
        }).addOnFailureListener(e -> sinLector());
    }

    private void lectorPreparado() {
        if (isDestroyed()) return;
        if (lectorListo) return;
        lectorListo = true;
        if (camara != null && estado.leeCamara()) aviso.setText(R.string.escaner_apunta);
    }

    // Sin Google Play Services o sin poder bajar el modelo: se dice y queda escribir el código.
    private void sinLector() {
        if (isDestroyed() || lectorListo) return;
        lectorListo = false;
        if (findViewById(R.id.panelSinPermiso).getVisibility() != View.VISIBLE) aviso.setText(R.string.escaner_sin_lector);
    }

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void sinPermiso() {
        aviso.setText(R.string.escaner_escribir);
        aviso.setVisibility(View.GONE);
        marco.setVisibility(View.GONE);
        View panel = findViewById(R.id.panelSinPermiso);
        ((TextView) findViewById(R.id.tvSinPermisoTitulo)).setText(R.string.escaner_permiso_negado_titulo);
        ((TextView) findViewById(R.id.tvSinPermisoTexto)).setText(R.string.escaner_permiso_negado_texto);
        panel.setVisibility(View.VISIBLE);
    }

    // ── Cámara ──────────────────────────────────────────────────────────────

    private void iniciarCamara() {
        aviso.setVisibility(View.VISIBLE);
        marco.setVisibility(View.VISIBLE);
        aviso.setText(lectorListo ? R.string.escaner_apunta : R.string.escaner_preparando);
        marco.buscar();
        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                ProcessCameraProvider proveedor = futuro.get();
                Preview vista = new Preview.Builder().build();
                vista.setSurfaceProvider(((PreviewView) findViewById(R.id.previewEscaner)).getSurfaceProvider());
                ImageAnalysis analisis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analisis.setAnalyzer(hilo, this::analizar);
                proveedor.unbindAll();
                camara = proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, vista, analisis);
                btnLinterna.setVisibility(camara.getCameraInfo().hasFlashUnit() ? View.VISIBLE : View.GONE);
                analizar = estado.leeCamara();
                // El lector puede haber quedado listo antes que la cámara.
                if (lectorListo && estado.leeCamara()) aviso.setText(R.string.escaner_apunta);
            } catch (Exception e) {
                // Sin cámara trasera o sin poder abrirla: se dice y queda escribir el código.
                camara = null;
                aviso.setText(R.string.escaner_sin_camara);
                marco.setVisibility(View.GONE);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizar(@NonNull ImageProxy imagen) {
        if (!analizar || imagen.getImage() == null) {
            imagen.close();
            return;
        }
        InputImage entrada = InputImage.fromMediaImage(imagen.getImage(), imagen.getImageInfo().getRotationDegrees());
        lector.process(entrada)
                .addOnSuccessListener(codigos -> {
                    // Si ha analizado, el modelo ya está: el aviso de «preparando» se va.
                    if (!lectorListo) runOnUiThread(this::lectorPreparado);
                    for (Barcode b : codigos) {
                        String valor = b.getRawValue();
                        if (valor != null && !valor.isEmpty()) {
                            runOnUiThread(() -> leido(valor));
                            break;
                        }
                    }
                })
                // Una imagen que no se puede analizar no es un error para quien escanea:
                // llega otra en unas décimas de segundo. Mientras el modelo baja, todas
                // fallan, y el aviso dice «preparando».
                .addOnFailureListener(e -> { })
                .addOnCompleteListener(t -> imagen.close());
    }

    private void alternarLinterna() {
        if (camara == null) return;
        linterna = !linterna;
        camara.getCameraControl().enableTorch(linterna);
        btnLinterna.setImageResource(linterna ? R.drawable.ic_ms_flashlight_on : R.drawable.ic_ms_flashlight_off);
        btnLinterna.setContentDescription(getString(linterna ? R.string.escaner_linterna_apagar : R.string.escaner_linterna_encender));
    }

    // ── Leer y preguntar ────────────────────────────────────────────────────

    private void leido(@NonNull String codigo) {
        if (!estado.leido(codigo)) return;
        analizar = false;
        Movimiento.vibrar(marco, Movimiento.Vibracion.EXITO);
        marco.leer();
        aviso.setText(R.string.escaner_leido);
        aviso.setTextColor(ContextCompat.getColor(this, R.color.gp_escaner_leido));
        consultar();
    }

    private void escribirCodigo() {
        View vista = getLayoutInflater().inflate(R.layout.dialog_cantidad, null);
        EditText et = vista.findViewById(R.id.etCantidad);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(R.string.escribir_codigo_hint);
        ((TextView) vista.findViewById(R.id.tvUnidadCantidad)).setVisibility(View.GONE);
        androidx.appcompat.app.AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.escribir_codigo_titulo)
                .setView(vista)
                .setPositiveButton(R.string.escribir_codigo_buscar, null)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .create();
        // Salir con un código a medio escribir pregunta antes de tirarlo (GP-108).
        es.pmdm.gymprofit.utils.AvisoDescartar.instalarEnDialogo(this, d,
                () -> es.pmdm.gymprofit.utils.AvisoDescartar.hayTexto(et.getText()), et);
        d.setOnShowListener(x -> d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String codigo = CodigoBarras.limpiar(et.getText());
            CodigoBarras.Problema p = CodigoBarras.problema(codigo);
            if (p != null) {
                UIHelper.marcarError(et, getString(p == CodigoBarras.Problema.CIFRAS ? R.string.codigo_error_cifras
                        : p == CodigoBarras.Problema.DIGITO ? R.string.codigo_error_digito : R.string.codigo_error_longitud));
                return;
            }
            d.dismiss();
            // Con una hoja abierta, el escrito sustituye a lo de antes.
            if (estado.hayHoja()) otroMas();
            if (!estado.escrito(codigo)) return;
            analizar = false;
            marco.leer();
            consultar();
        }));
        d.show();
    }

    private void consultar() {
        String codigo = estado.codigo();
        if (codigo == null) return;
        mostrarHoja();
        alimentoApi.porCodigo(codigo).enqueue(new ApiCallback<Alimento>() {
            @Override
            public void onOk(Alimento a) {
                if (isDestroyed()) return;
                if (a == null) {
                    estado.noExiste(codigo);
                } else {
                    alimento = a;
                    estado.encontrado(codigo);
                }
                mostrarHoja();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                if (code == 404) {
                    estado.noExiste(codigo);
                } else if (code == 503) {
                    Integer s = UiFeedback.segundosDeEspera(message);
                    estado.sinCupo(codigo, s != null ? s : 60);
                } else {
                    // Sin red o cualquier otro fallo: se dice y se deja reintentar.
                    estado.sinRed(codigo);
                    errorDe = code == -1 ? null : UiFeedback.mensaje(EscanerActivity.this, code, message);
                }
                mostrarHoja();
            }
        });
    }

    // El texto de un fallo que no es de red (500…), para la hoja; null si es de red.
    @Nullable private String errorDe;

    // ── La hoja ─────────────────────────────────────────────────────────────

    private void mostrarHoja() {
        boolean nueva = hoja.getVisibility() != View.VISIBLE;
        hoja.setVisibility(View.VISIBLE);
        findViewById(R.id.btnEscribirCodigo).setVisibility(View.GONE);
        View progreso = findViewById(R.id.pbEscaner);
        View cabeza = findViewById(R.id.cabezaHoja);
        View cifras = findViewById(R.id.cifrasHoja);
        View botones = findViewById(R.id.botonesHoja);
        MaterialButton principal = findViewById(R.id.btnHojaPrincipal);
        MaterialButton secundario = findViewById(R.id.btnHojaSecundario);
        TextView titulo = findViewById(R.id.tvTituloHoja);
        TextView texto = findViewById(R.id.tvTextoHoja);
        View tile = findViewById(R.id.tileHoja);

        EstadoEscaner.Estado e = estado.estado();
        progreso.setVisibility(e == EstadoEscaner.Estado.CONSULTANDO ? View.VISIBLE : View.GONE);
        cabeza.setVisibility(e == EstadoEscaner.Estado.CONSULTANDO ? View.GONE : View.VISIBLE);
        cifras.setVisibility(e == EstadoEscaner.Estado.PRODUCTO ? View.VISIBLE : View.GONE);
        botones.setVisibility(e == EstadoEscaner.Estado.CONSULTANDO ? View.GONE : View.VISIBLE);
        tile.setVisibility(e == EstadoEscaner.Estado.PRODUCTO ? View.VISIBLE : View.GONE);
        secundario.setVisibility(e == EstadoEscaner.Estado.PRODUCTO ? View.VISIBLE : View.GONE);

        switch (e) {
            case CONSULTANDO:
                hoja.setContentDescription(getString(R.string.escaner_consultando));
                break;
            case PRODUCTO:
                pintarProducto(titulo, texto, principal, secundario);
                break;
            case NO_EXISTE:
                titulo.setText(R.string.escaner_no_existe_titulo);
                texto.setText(getString(R.string.escaner_no_existe_texto, estado.codigo()));
                principal.setText(R.string.escaner_crear_con_codigo);
                principal.setOnClickListener(v -> crearConCodigo());
                break;
            case SIN_CUPO:
                titulo.setText(R.string.escaner_sin_cupo_titulo);
                int s = (int) Math.max(1, estado.segundos());
                texto.setText(getResources().getQuantityString(R.plurals.escaner_espera_segundos, s, s));
                principal.setText(R.string.btn_reintentar);
                principal.setOnClickListener(v -> reintentar());
                break;
            case SIN_RED:
                titulo.setText(errorDe == null ? R.string.escaner_sin_red_titulo : R.string.escaner_error_titulo);
                texto.setText(errorDe == null ? getString(R.string.escaner_sin_red_texto) : errorDe);
                principal.setText(R.string.btn_reintentar);
                principal.setOnClickListener(v -> reintentar());
                break;
            default:
                break;
        }
        if (e != EstadoEscaner.Estado.CONSULTANDO) {
            cabeza.setContentDescription(titulo.getText() + ". " + texto.getText());
            cabeza.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
        if (nueva && !Movimiento.quieto(this)) {
            // La hoja sube (momento 17).
            hoja.post(() -> {
                hoja.setTranslationY(hoja.getHeight());
                hoja.animate().translationY(0f).setDuration(Movimiento.HOJA).setInterpolator(Movimiento.ENFATIZADA).start();
            });
        }
    }

    private void pintarProducto(TextView titulo, TextView texto, MaterialButton principal, MaterialButton secundario) {
        Alimento a = alimento;
        if (a == null) return;
        CantidadFicha cantidad = CantidadFicha.nueva(a.getRaciones());
        NumberFormat nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        nf.setMaximumFractionDigits(1);
        CantidadFicha.Unidad u = cantidad.unidad();
        String porcion = es.pmdm.gymprofit.utils.Cantidades.de(this, u.nombre, u.unidad, u.unidadPlural,
                cantidad.raciones(), cantidad.gramos());

        ((android.widget.ImageView) findViewById(R.id.ivIconoHoja)).setImageResource(a.esProducto()
                ? R.drawable.ic_ms_inventory_2 : R.drawable.ic_ms_restaurant);
        titulo.setText(es.pmdm.gymprofit.ui.nutricion.Insignia.nombre(this, a.getNombre(), a.isRevisado(), 18));
        texto.setText(a.esProducto() ? getString(R.string.escaner_producto_sub, porcion)
                : BusquedaAlimentoAdapter.detalle(this, a));

        double g = cantidad.gramos();
        long kcal = Math.round(a.getCalorias() * g / 100.0);
        double p = a.getProteinas() * g / 100.0, c = a.getCarbohidratos() * g / 100.0, gr = a.getGrasas() * g / 100.0;
        cifra(R.id.cifraHojaKcal, nf.format(kcal), R.string.ficha_kcal, 0);
        cifra(R.id.cifraHojaProteina, getString(R.string.ficha_gramos_valor, nf.format(p)), etiquetaProteina(),
                ContextCompat.getColor(this, R.color.gp_macro_proteinas));
        cifra(R.id.cifraHojaCarb, getString(R.string.ficha_gramos_valor, nf.format(c)), R.string.ficha_carb, 0);
        cifra(R.id.cifraHojaGrasa, getString(R.string.ficha_gramos_valor, nf.format(gr)), R.string.ficha_grasa, 0);
        findViewById(R.id.cifrasHoja).setContentDescription(getString(R.string.ficha_cifras_a11y,
                nf.format(kcal), nf.format(p), nf.format(c), nf.format(gr)));

        secundario.setText(R.string.escaner_cambiar_cantidad);
        secundario.setOnClickListener(v -> despues.launch(FichaAlimentoActivity.paraAnadir(this, a, tipoComida, fecha)
                .putExtra(AnadirAlimentoActivity.EXTRA_EN_ANADIR, enAnadir())));
        principal.setText(AnadirAlimentoActivity.textoAnadirA(tipoComida));
        principal.setOnClickListener(v -> anadir(a, cantidad, principal));
    }

    private void cifra(int id, String valor, int etiqueta, int color) {
        View v = findViewById(id);
        TextView tv = v.findViewById(R.id.tvValorCifra);
        tv.setText(valor);
        if (color != 0) tv.setTextColor(color);
        ((TextView) v.findViewById(R.id.tvEtiquetaCifra)).setText(etiqueta);
    }

    private void anadir(Alimento a, CantidadFicha cantidad, View boton) {
        boton.setEnabled(false);
        LoadingDialog.show(this);
        String dia = fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(new java.util.Date());
        comidaApi.anadir(PedidoCantidad.anadir(a, cantidad, dia, tipoComida)).enqueue(new ApiCallback<AnadirRespuesta>() {
            @Override
            public void onOk(AnadirRespuesta r) {
                LoadingDialog.hide(EscanerActivity.this);
                if (isDestroyed()) return;
                Movimiento.vibrar(boton, Movimiento.Vibracion.LIGERA);
                Intent datos = new Intent();
                if (r != null && r.getLinea() != null) {
                    String texto = es.pmdm.gymprofit.utils.AnadirRapido.texto(
                            es.pmdm.gymprofit.utils.Cantidades.Formatos.de(EscanerActivity.this),
                            es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(EscanerActivity.this), cantidad);
                    datos.putExtra(AnadirAlimentoActivity.EXTRA_ANADIDO, new com.google.gson.Gson().toJson(
                            es.pmdm.gymprofit.utils.Anadidos.Anadido.de(r, a.getBarcode(), tipoComida, texto,
                                    es.pmdm.gymprofit.utils.AnadirRapido.kcal(a, cantidad))));
                }
                // Desde Añadir, lo dice su barra; desde una comida, el aviso de siempre.
                if (!enAnadir()) {
                    UIHelper.mostrarToastExito(EscanerActivity.this, getString(R.string.ficha_anadido, a.getNombre()));
                }
                setResult(RESULT_OK, datos);
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(EscanerActivity.this);
                boton.setEnabled(true);
                UiFeedback.toastError(EscanerActivity.this, code, message);
            }
        });
    }

    private void crearConCodigo() {
        despues.launch(new Intent(this, CrearAlimentoActivity.class)
                .putExtra(CrearAlimentoActivity.EXTRA_CODIGO, estado.codigo())
                .putExtra(AnadirAlimentoActivity.EXTRA_EN_ANADIR, enAnadir())
                .putExtra(AnadirAlimentoActivity.EXTRA_TIPO, tipoComida)
                .putExtra(AnadirAlimentoActivity.EXTRA_FECHA, fecha));
    }

    private boolean enAnadir() {
        return getIntent().getBooleanExtra(AnadirAlimentoActivity.EXTRA_EN_ANADIR, false);
    }

    private void reintentar() {
        if (estado.reintentar()) consultar();
    }

    private void otroMas() {
        estado.otroMas();
        alimento = null;
        errorDe = null;
        hoja.setVisibility(View.GONE);
        findViewById(R.id.btnEscribirCodigo).setVisibility(View.VISIBLE);
        if (camara != null) {
            marco.buscar();
            aviso.setText(lectorListo ? R.string.escaner_apunta : R.string.escaner_preparando);
            aviso.setTextColor(ContextCompat.getColor(this, R.color.gp_escaner_texto));
            analizar = true;
        }
    }

    // Con letra grande «proteína» no cabe en la cuarta parte del ancho sin bajar de 13 sp:
    // ahí va «prot.», como «carb.». TalkBack lee la frase entera de la fila.
    private int etiquetaProteina() {
        return getResources().getConfiguration().fontScale >= 1.3f
                ? R.string.ficha_proteina_corta : R.string.ficha_proteina;
    }
}
