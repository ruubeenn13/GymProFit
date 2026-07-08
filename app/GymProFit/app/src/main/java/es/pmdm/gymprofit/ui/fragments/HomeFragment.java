package es.pmdm.gymprofit.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.network.UiApiCallback;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.activities.SesionesActivity;
import es.pmdm.gymprofit.utils.NavTabs;

// ============================================================
// HomeFragment — pestaña principal tras el login (pager de MainActivity).
// Muestra la cabecera con saludo/fecha, las estadísticas semanales de
// entrenamiento (sesiones, calorías, minutos) y las acciones rápidas.
// ============================================================
public class HomeFragment extends BaseFragment {
    private TextView tvConteoEntrenamientos, tvConteoCaloriasHome, tvConteoMinutosHome;
    private TextView tvRachaNumero, tvRachaUnidad, tvRachaMejor;
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_home, container, false);
    }

    // Referencia las vistas de estadísticas y configura cabecera y accesos rápidos.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvConteoEntrenamientos = findViewById(R.id.tvConteoEntrenamientos);
        tvConteoCaloriasHome   = findViewById(R.id.tvConteoCaloriasHome);
        tvConteoMinutosHome    = findViewById(R.id.tvConteoMinutosHome);
        tvRachaNumero = findViewById(R.id.tvRachaNumero);
        tvRachaUnidad = findViewById(R.id.tvRachaUnidad);
        tvRachaMejor  = findViewById(R.id.tvRachaMejor);

        setupMenuButton();
        configurarCabecera();
        configurarAccionesRapidas();
    }

    // Recarga las estadísticas semanales cada vez que la pestaña vuelve a primer plano.
    @Override
    public void onResume() {
        super.onResume();
        cargarEstadisticasSemana();
        cargarRacha();
    }

    // Carga la racha de días del usuario y la muestra como protagonista de la cabecera.
    private void cargarRacha() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        usuarioApi.getEstadisticas(usuarioId).enqueue(new ApiCallback<UsuarioEstadisticas>() {
            @Override
            public void onOk(UsuarioEstadisticas e) {
                if (e == null || !isAdded()) return;
                int dias = e.getRachaActualDias();
                tvRachaNumero.setText(String.valueOf(dias));
                tvRachaUnidad.setText(getString(dias == 1 ? R.string.home_racha_dia_unidad
                        : R.string.home_racha_dias_unidad));
                if (dias == 0) {
                    tvRachaMejor.setText(getString(R.string.home_racha_vacia));
                } else {
                    tvRachaMejor.setText(getString(R.string.home_racha_mejor, e.getMejorRachaDias()));
                }
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                tvRachaNumero.setText("0");
                tvRachaUnidad.setText(getString(R.string.home_racha_dias_unidad));
                tvRachaMejor.setText(getString(R.string.home_racha_vacia));
            }
        });
    }

    // Calcula el total de entrenamientos, calorías y minutos de la semana actual.
    private void cargarEstadisticasSemana() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        sesionApi.getDeUsuario(usuarioId).enqueue(new UiApiCallback<List<SesionEntrenamiento>>(requireActivity(), false) {
            @Override
            public void onData(List<SesionEntrenamiento> sesiones) {
                if (!isAdded()) return;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                int offset = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
                cal.add(Calendar.DAY_OF_MONTH, -offset);
                Date semanaInicio = cal.getTime();

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
                if (!isAdded()) return;
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

    // Listeners de las tarjetas de acceso rápido (entrenamiento, rutinas, comida).
    private void configurarAccionesRapidas() {
        MaterialCardView cardIniciarEntrenamiento = findViewById(R.id.cardIniciarEntrenamiento);
        MaterialCardView cardVerRutinas = findViewById(R.id.cardVerRutinas);
        MaterialCardView cardRegistrarComida = findViewById(R.id.cardRegistrarComida);

        cardIniciarEntrenamiento.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            startActivity(new Intent(requireContext(), SesionesActivity.class));
        });

        // Rutinas y Nutrición son pestañas del pager: cambio de pestaña (no Activity).
        cardVerRutinas.setOnClickListener(v -> irATab(NavTabs.RUTINAS));

        cardRegistrarComida.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            irATab(NavTabs.NUTRICION);
        });
    }
}
