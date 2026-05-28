package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;

public class HomeActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private TextView tvConteoEntrenamientos, tvConteoCaloriasHome, tvConteoMinutosHome;

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

    @Override
    protected void onResume() {
        super.onResume();
        cargarEstadisticasSemana();
    }

    private void cargarEstadisticasSemana() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        API.getSesionesDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<SesionEntrenamiento> sesiones = UtilJSONParser.parseSesionList(response);

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    int offset = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
                    cal.add(Calendar.DAY_OF_MONTH, -offset);
                    Date semanaInicio = cal.getTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
                    int count = 0, calorias = 0, minutos = 0;
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

                    final int fc = count, fcal = calorias, fmin = minutos;
                    runOnUiThread(() -> {
                        tvConteoEntrenamientos.setText(String.valueOf(fc));
                        tvConteoCaloriasHome.setText(String.valueOf(fcal));
                        tvConteoMinutosHome.setText(String.valueOf(fmin));
                    });
                } catch (JSONException ignored) {}
            }
            @Override public void onError(String message, int statusCode) {
                runOnUiThread(() -> {
                    tvConteoEntrenamientos.setText("—");
                    tvConteoCaloriasHome.setText("—");
                    tvConteoMinutosHome.setText("—");
                });
            }
        });
    }

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

    private void configurarAccionesRapidas() {
        MaterialCardView cardIniciarEntrenamiento = findViewById(R.id.cardIniciarEntrenamiento);
        MaterialCardView cardVerRutinas = findViewById(R.id.cardVerRutinas);
        MaterialCardView cardRegistrarComida = findViewById(R.id.cardRegistrarComida);

        cardIniciarEntrenamiento.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            startActivity(new Intent(this, SesionesActivity.class));
            overridePendingTransition(0, 0);
        });

        cardVerRutinas.setOnClickListener(v -> {
            startActivity(new Intent(this, RutinasActivity.class));
            overridePendingTransition(0, 0);
        });

        cardRegistrarComida.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            startActivity(new Intent(this, NutricionActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

}