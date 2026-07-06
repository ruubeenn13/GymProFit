package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.network.UiApiCallback;

// ============================================================
// HomeActivity — pantalla principal tras el login.
// Muestra la cabecera con saludo/fecha, las estadísticas semanales de
// entrenamiento (sesiones, calorías, minutos), las acciones rápidas y
// la navegación inferior de la app GymProFit.
// ============================================================
public class HomeActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private TextView tvConteoEntrenamientos, tvConteoCaloriasHome, tvConteoMinutosHome;
    // Interfaz Retrofit tipada del dominio sesiones (etapa 2)
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);

    // Infla el layout, referencia las vistas de estadísticas y configura
    // cabecera, accesos rápidos, navegación inferior y permiso de notificaciones.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvConteoEntrenamientos = findViewById(R.id.tvConteoEntrenamientos);
        tvConteoCaloriasHome   = findViewById(R.id.tvConteoCaloriasHome);
        tvConteoMinutosHome    = findViewById(R.id.tvConteoMinutosHome);

        setupMenuButton();
        configurarCabecera();
        configurarAccionesRapidas();
        configurarNavegacion();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        }
    }

    // Recarga las estadísticas semanales cada vez que la Activity vuelve a primer plano.
    @Override
    protected void onResume() {
        super.onResume();
        cargarEstadisticasSemana();
    }

    // Obtiene las sesiones del usuario y calcula el total de entrenamientos,
    // calorías y minutos realizados desde el lunes de la semana actual.
    private void cargarEstadisticasSemana() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        // autoLoading=false: Home no queda en blanco (saludo/accesos visibles);
        // basta el toast de error automático (cold-start incluido) + fallback "—".
        sesionApi.getDeUsuario(usuarioId).enqueue(new UiApiCallback<List<SesionEntrenamiento>>(this, false) {
            @Override
            public void onData(List<SesionEntrenamiento> sesiones) {
                // Calcula la fecha de inicio de la semana actual (lunes a las 00:00)
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                int offset = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
                cal.add(Calendar.DAY_OF_MONTH, -offset);
                Date semanaInicio = cal.getTime();

                // La API emite la fecha en ISO-8601 (yyyy-MM-dd'T'HH:mm:ss) al mapearse directa al POJO.
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                int count = 0, calorias = 0, minutos = 0;
                if (sesiones != null) {
                    for (SesionEntrenamiento s : sesiones) {
                        try {
                            Date fecha = sdf.parse(s.getFechaInicio());
                            if (fecha != null && !fecha.before(semanaInicio)) {
                                count++;
                                calorias += s.getCaloriasQuemadas();
                                minutos  += s.getDuracionMinutos();
                            }
                        } catch (ParseException ignored) {}
                    }
                }

                tvConteoEntrenamientos.setText(String.valueOf(count));
                tvConteoCaloriasHome.setText(String.valueOf(calorias));
                tvConteoMinutosHome.setText(String.valueOf(minutos));
            }
            @Override public void onFail(int code, String message) {
                // Fallback visual "—" en las tarjetas + toast de error estándar (super).
                tvConteoEntrenamientos.setText("—");
                tvConteoCaloriasHome.setText("—");
                tvConteoMinutosHome.setText("—");
                super.onFail(code, message);
            }
        });
    }

    // Configura el saludo según la hora del día, el nombre de usuario y la fecha actual.
    private void configurarCabecera() {
        TextView tvSaludo  = findViewById(R.id.tvSaludo);
        TextView tvUsuario = findViewById(R.id.tvUsuario);
        TextView tvFecha   = findViewById(R.id.tvFecha);

        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora < 12) {
            tvSaludo.setText(R.string.home_buenos_dias);
        } else if (hora < 20) {
            tvSaludo.setText(R.string.home_buenas_tardes);
        } else {
            tvSaludo.setText(R.string.home_buenas_noches);
        }

        String username = prefsManager.getUsername();
        tvUsuario.setText(username.isEmpty() ? getString(R.string.home_usuario_defecto) : username);

        Locale locale = Locale.getDefault();
        String pattern = "en".equals(locale.getLanguage())
                ? "EEEE, MMMM d"
                : "EEEE, d 'de' MMMM";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
        tvFecha.setText(sdf.format(new Date()));
    }

    // Configura los listeners de las tarjetas de acceso rápido (iniciar entrenamiento,
    // ver rutinas, registrar comida), comprobando acceso registrado cuando aplica.
    private void configurarAccionesRapidas() {
        MaterialCardView cardIniciarEntrenamiento = findViewById(R.id.cardIniciarEntrenamiento);
        MaterialCardView cardVerRutinas = findViewById(R.id.cardVerRutinas);
        MaterialCardView cardRegistrarComida = findViewById(R.id.cardRegistrarComida);

        cardIniciarEntrenamiento.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            startActivity(new Intent(this, SesionesActivity.class));
            es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
        });

        cardVerRutinas.setOnClickListener(v -> {
            startActivity(new Intent(this, RutinasActivity.class));
            es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
        });

        cardRegistrarComida.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            startActivity(new Intent(this, NutricionActivity.class));
            es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
        });
    }

    // Configura la barra de navegación inferior, marcando "Home" como seleccionado
    // y redirigiendo a la Activity correspondiente al pulsar cada ítem.
    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            }

            return false;
        });
    }

}