package es.pmdm.gymprofit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.BuildConfig;
import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.envivo.LogicaDescanso;
import es.pmdm.gymprofit.envivo.PermisosDescanso;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.widget.FotoPerfil;
import es.pmdm.gymprofit.ui.widget.HojaElegir;
import es.pmdm.gymprofit.utils.Agua;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.AvisosCuenta;
import es.pmdm.gymprofit.utils.Correo;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// PerfilActivity — Perfil y ajustes (GP-216, tablero 17). Sustituye a Ajustes: se abre con el
// avatar de Inicio, Entrenar, Nutrición y Progreso y reúne, en un solo patrón de fila
// (view_fila_ajuste), todo lo que tenía Ajustes más lo que llevaba Editar perfil:
//   · Cabecera del perfil: el avatar con la cámara encima (es «Foto de perfil», que ya no es una
//     fila), el nombre y desde cuándo; y la tarjeta «Tus datos y objetivos» con el plan, que
//     abre Tus datos.
//   · Cuenta: correo (enmascarado; se cambia con la contraseña, GP-083) y contraseña.
//   · Entrenamiento (GP-013): empezar el descanso al marcar, el descanso sin pauta y los avisos con
//     la pantalla apagada.
//   · La app: notificaciones, tema, idioma y vaso de agua; los tres últimos, en la misma hoja.
//   · Ayuda: soporte, informar de un error (con versión, Android y modelo; nada personal) y
//     sugerir una mejora, los tres a soporte@gymprofit.app.
//   · Legal: términos de uso, privacidad, licencias y Acerca de.
//   · Administración, solo para ADMIN y temporal hasta la web (GP-085).
//   · Cerrar sesión y Eliminar cuenta, al final. «Ver el tour de nuevo» no está hasta que exista
//     GP-215.
// ============================================================
public class PerfilActivity extends BaseActivity {

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private FotoPerfil fotoPerfil;
    // Lo que dice la cuenta, para el resumen: se pinta con lo del móvil y se corrige al llegar.
    @Nullable private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        fotoPerfil = new FotoPerfil(this, () -> this, prefsManager.getUsuarioId(), this::pintarAvatar);

        // Perfil: la cámara sobre el avatar es «Foto de perfil».
        View avatar = findViewById(R.id.avatarPerfil);
        avatar.setContentDescription(getString(R.string.perfil_cambiar_foto_a11y));
        avatar.setOnClickListener(v -> fotoPerfil.elegir());
        findViewById(R.id.btnCamaraPerfil).setOnClickListener(v -> fotoPerfil.elegir());
        ((TextView) avatar.findViewById(R.id.tvInicial)).setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.text_number));
        findViewById(R.id.cardTusDatos).setOnClickListener(v -> startActivity(new Intent(this, TusDatosActivity.class)));

        // Cuenta
        fila(R.id.filaCorreo, R.drawable.ic_ms_mail, R.string.ajustes_correo,
                () -> startActivity(new Intent(this, CambiarCorreoActivity.class)));
        fila(R.id.filaContrasena, R.drawable.ic_ms_lock, R.string.ajustes_contrasena,
                () -> startActivity(new Intent(this, CambiarPasswordActivity.class)));

        // Entrenamiento (GP-013)
        configurarEntrenamiento();

        // La app
        fila(R.id.filaNotificaciones, R.drawable.ic_ms_notifications, R.string.ajustes_notificaciones,
                () -> startActivity(new Intent(this, NotificacionesActivity.class)));
        fila(R.id.filaTema, R.drawable.ic_ms_palette, R.string.ajustes_tema, this::elegirTema);
        fila(R.id.filaIdioma, R.drawable.ic_ms_language, R.string.ajustes_idioma, this::elegirIdioma);
        // El vaso de agua (lote 1.6.8, decisión 11): se guarda en el móvil, sin pasar por la API.
        fila(R.id.filaVaso, R.drawable.ic_ms_local_drink, R.string.perfil_vaso, this::elegirVaso);

        // Ayuda
        fila(R.id.filaSoporte, R.drawable.ic_ms_support_agent, R.string.ajustes_soporte,
                () -> escribir(getString(R.string.email_soporte_asunto), null));
        fila(R.id.filaError, R.drawable.ic_ms_bug_report, R.string.ajustes_error,
                () -> escribir(getString(R.string.email_error_asunto), cuerpoError()));
        fila(R.id.filaMejora, R.drawable.ic_ms_lightbulb, R.string.ajustes_mejora,
                () -> escribir(getString(R.string.email_mejora_asunto), null));

        // Legal
        fila(R.id.filaTerminos, R.drawable.ic_ms_gavel, R.string.ajustes_terminos,
                () -> UIHelper.abrirUrl(this, getString(R.string.url_terminos)));
        abreFuera(R.id.filaTerminos, R.string.ajustes_terminos);
        fila(R.id.filaPrivacidad, R.drawable.ic_ms_policy, R.string.politica_privacidad,
                () -> UIHelper.abrirUrl(this, getString(R.string.url_privacidad)));
        abreFuera(R.id.filaPrivacidad, R.string.politica_privacidad);
        fila(R.id.filaLicencias, R.drawable.ic_ms_license, R.string.licencias_titulo,
                () -> startActivity(new Intent(this, LicenciasActivity.class)));
        fila(R.id.filaAcerca, R.drawable.ic_ms_info, R.string.ajustes_acerca,
                () -> startActivity(new Intent(this, AcercaDeActivity.class)));
        valor(R.id.filaAcerca, R.string.ajustes_acerca, BuildConfig.VERSION_NAME);

        // Administración: solo ADMIN, temporal (GP-085).
        boolean admin = prefsManager.isAdmin();
        findViewById(R.id.tvSeccionAdmin).setVisibility(admin ? View.VISIBLE : View.GONE);
        findViewById(R.id.grupoAdmin).setVisibility(admin ? View.VISIBLE : View.GONE);
        fila(R.id.filaAdmin, R.drawable.ic_ms_workspace_premium, R.string.ajustes_admin,
                () -> startActivity(new Intent(this, AdminActivity.class)));

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> confirmarCerrarSesion());
        findViewById(R.id.btnEliminarCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, EliminarCuentaActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintarAvatar();
        pintarValores();
        pintarResumen();
        cargarCuenta();
    }

    // ── Perfil ───────────────────────────────────────────────────────────────

    private void pintarAvatar() {
        AvatarUtils.pintar(findViewById(R.id.avatarPerfil), nombreVisible(), prefsManager.getUsuarioId());
    }

    private String nombreVisible() {
        String nombre = NombreVisible.de(prefsManager.getNombre(), prefsManager.getUsername());
        return nombre.isEmpty() ? getString(R.string.home_usuario_defecto) : nombre;
    }

    // El nombre, «@usuario · desde septiembre», los datos en píldoras y el plan.
    private void pintarResumen() {
        ((TextView) findViewById(R.id.tvPerfilNombre)).setText(nombreVisible());
        String sub = "@" + prefsManager.getUsername();
        String desde = desde(usuario == null ? null : usuario.getFechaRegistro());
        ((TextView) findViewById(R.id.tvPerfilSub)).setText(
                desde == null ? sub : getString(R.string.hoy_toca_separador, sub, desde));

        Locale idioma = FechaUtils.localeDeLaApp(this);
        ChipGroup grupo = findViewById(R.id.grupoDatos);
        grupo.removeAllViews();
        List<String> a11y = new ArrayList<>();
        String objetivo = usuario != null && usuario.getObjetivo() != null ? usuario.getObjetivo() : prefsManager.getObjetivo();
        String nivel = usuario != null && usuario.getNivelExperiencia() != null
                ? usuario.getNivelExperiencia() : prefsManager.getNivel();
        anadirDato(grupo, a11y, UIHelper.traducirObjetivo(this, objetivo), true);
        anadirDato(grupo, a11y, nivel.isEmpty() ? null : UIHelper.traducirNivel(this, nivel), false);
        anadirDato(grupo, a11y, UIHelper.traducirActividad(this, prefsManager.getActividad()), false);
        double peso = prefsManager.getPeso();
        if (peso > 0) {
            NumberFormat nf = NumberFormat.getNumberInstance(idioma);
            nf.setMaximumFractionDigits(1);
            anadirDato(grupo, a11y, getString(R.string.perfil_dato_kg, nf.format(peso)), false);
        }
        if (prefsManager.getAltura() > 0) {
            anadirDato(grupo, a11y, getString(R.string.perfil_dato_cm, (int) Math.round(prefsManager.getAltura())), false);
        }
        if (prefsManager.getEdad() > 0) {
            anadirDato(grupo, a11y, getResources().getQuantityString(R.plurals.perfil_dato_anios,
                    prefsManager.getEdad(), prefsManager.getEdad()), false);
        }

        ResultadoNutricional plan = DiaNutricion.objetivo(prefsManager);
        View bloque = findViewById(R.id.bloquePlan);
        String descripcion = getString(R.string.perfil_datos_titulo) + ". " + android.text.TextUtils.join(", ", a11y);
        if (plan == null) {
            bloque.setVisibility(View.GONE);
        } else {
            NumberFormat entero = NumberFormat.getIntegerInstance(idioma);
            bloque.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvPlanKcal)).setText(getString(R.string.perfil_plan_kcal, entero.format(plan.calorias)));
            ((TextView) findViewById(R.id.tvPlanProteina)).setText(getString(R.string.inicio_cifra_g, plan.proteinas));
            descripcion += ". " + getString(R.string.perfil_plan_a11y, entero.format(plan.calorias), plan.proteinas);
        }
        findViewById(R.id.cardTusDatos).setContentDescription(descripcion);
    }

    private void anadirDato(ChipGroup grupo, List<String> a11y, @Nullable String texto, boolean destacado) {
        if (texto == null || texto.isEmpty()) return;
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextAppearance(R.style.TextAppearance_GymProFit_Apoyo);
        tv.setTextColor(destacado ? colorDe(com.google.android.material.R.attr.colorOnPrimaryContainer)
                : colorDe(com.google.android.material.R.attr.colorOnSurface));
        tv.setBackgroundResource(destacado ? R.drawable.bg_chip_dato_destacado : R.drawable.bg_chip_dato);
        int h = getResources().getDimensionPixelSize(R.dimen.espacio_12);
        int v = getResources().getDimensionPixelSize(R.dimen.espacio_8);
        tv.setPadding(h, v, h, v);
        tv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        grupo.addView(tv);
        a11y.add(texto);
    }

    private int colorDe(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    // «desde septiembre» si es de este año; con año si no. Null si no se sabe.
    @Nullable
    private String desde(@Nullable String iso) {
        if (iso == null || iso.length() < 7) return null;
        try {
            int anio = Integer.parseInt(iso.substring(0, 4));
            int mes = Integer.parseInt(iso.substring(5, 7));
            if (mes < 1 || mes > 12) return null;
            Locale idioma = FechaUtils.localeDeLaApp(this);
            String nombre = new DateFormatSymbols(idioma).getMonths()[mes - 1].toLowerCase(idioma);
            boolean esteAnio = anio == Calendar.getInstance().get(Calendar.YEAR);
            return esteAnio ? getString(R.string.perfil_desde, nombre) : getString(R.string.perfil_desde_anio, nombre, anio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Valores de las filas ─────────────────────────────────────────────────

    // Valores de la derecha: lo guardado en el teléfono ya, y el correo y los avisos al llegar.
    private void pintarValores() {
        pintarEntrenamiento();
        valor(R.id.filaTema, R.string.ajustes_tema, getString(
                prefsManager.getTheme() == AppCompatDelegate.MODE_NIGHT_NO ? R.string.ajustes_tema_claro : R.string.ajustes_tema_oscuro));
        // El idioma en uso, no la preferencia: sin elegir, la app sigue al del sistema.
        valor(R.id.filaIdioma, R.string.ajustes_idioma, getString(esIngles() ? R.string.ajustes_idioma_en : R.string.ajustes_idioma_es));
        valor(R.id.filaVaso, R.string.perfil_vaso, getString(R.string.perfil_vaso_ml, prefsManager.getVasoMl()));
        pintarNotificaciones(null);
    }

    private boolean esIngles() {
        return "en".equals(FechaUtils.localeDeLaApp(this).getLanguage());
    }

    private void cargarCuenta() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) return;
        usuarioApi.getPorId(uid).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null || isFinishing()) return;
                usuario = u;
                valor(R.id.filaCorreo, R.string.ajustes_correo, Correo.enmascarar(u.getEmail()));
                pintarNotificaciones(AvisosCuenta.Estado.de(u.getAvisosEntrenar(), u.getAvisosComidas(), u.getAvisosProgreso()));
                pintarResumen();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito (GP-017): las filas se usan igual sin el valor de la derecha
                // y la tarjeta del perfil enseña lo guardado en el móvil; las pantallas que cambian
                // el correo o los avisos lo vuelven a pedir y avisan.
            }
        });
    }

    // «3 de 4»: el fin del descanso va siempre con la sesión y los otros tres son los de la cuenta.
    private void pintarNotificaciones(@Nullable AvisosCuenta.Estado e) {
        if (e == null) {
            valor(R.id.filaNotificaciones, R.string.ajustes_notificaciones, null);
            return;
        }
        int activos = 1 + (e.entrenar ? 1 : 0) + (e.comidas ? 1 : 0) + (e.progreso ? 1 : 0);
        valor(R.id.filaNotificaciones, R.string.ajustes_notificaciones, getString(R.string.perfil_notificaciones_n, activos, 4));
    }

    // ── Entrenamiento (GP-013) ───────────────────────────────────────────────

    // El descanso entre series: si empieza al marcar, el de sin pauta y si los avisos con
    // la pantalla apagada pueden llegar a su hora. Se guardan en el móvil, como el tema.
    private void configurarEntrenamiento() {
        View alMarcar = findViewById(R.id.filaDescansoAlMarcar);
        ((ImageView) alMarcar.findViewById(R.id.ivIcono)).setImageResource(R.drawable.ic_ms_timer);
        ((TextView) alMarcar.findViewById(R.id.tvTitulo)).setText(R.string.ajustes_descanso_al_marcar);
        TextView subMarcar = alMarcar.findViewById(R.id.tvSub);
        subMarcar.setText(R.string.perfil_descanso_al_marcar_sub);
        subMarcar.setVisibility(View.VISIBLE);
        alMarcar.findViewById(R.id.ivFlecha).setVisibility(View.GONE);
        alMarcar.findViewById(R.id.swInterruptor).setVisibility(View.VISIBLE);
        alMarcar.setOnClickListener(v -> {
            prefsManager.saveDescansoAlMarcar(!prefsManager.getDescansoAlMarcar());
            pintarEntrenamiento();
        });
        // TalkBack la lee como un interruptor: «Empezar el descanso…, activado».
        ViewCompat.setAccessibilityDelegate(alMarcar, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(prefsManager.getDescansoAlMarcar());
            }
        });
        alMarcar.setContentDescription(getString(R.string.ajustes_fila_sub_a11y,
                getString(R.string.ajustes_descanso_al_marcar), getString(R.string.perfil_descanso_al_marcar_sub)));

        View sinPauta = findViewById(R.id.filaDescansoSinPauta);
        ((ImageView) sinPauta.findViewById(R.id.ivIcono)).setImageResource(R.drawable.ic_ms_hourglass);
        ((TextView) sinPauta.findViewById(R.id.tvTitulo)).setText(R.string.ajustes_descanso_sin_pauta);
        TextView subPauta = sinPauta.findViewById(R.id.tvSub);
        subPauta.setText(R.string.ajustes_descanso_sin_pauta_sub);
        subPauta.setVisibility(View.VISIBLE);
        sinPauta.setOnClickListener(v -> elegirDescansoSinPauta());

        View avisos = findViewById(R.id.filaAvisosDescanso);
        ((ImageView) avisos.findViewById(R.id.ivIcono)).setImageResource(R.drawable.ic_ms_notifications_active);
        ((TextView) avisos.findViewById(R.id.tvTitulo)).setText(R.string.ajustes_avisos_apagada);
        avisos.findViewById(R.id.ivFlecha).setVisibility(View.GONE);
        // La fila solo informa; lo que se toca es «Activar».
        avisos.setClickable(false);
        avisos.setBackground(null);
        MaterialButton activar = avisos.findViewById(R.id.btnAccion);
        activar.setText(R.string.ajustes_avisos_activar);
        activar.setContentDescription(getString(R.string.ajustes_avisos_activar_a11y));
        activar.setOnClickListener(v -> PermisosDescanso.activar(this));
    }

    private void pintarEntrenamiento() {
        View alMarcar = findViewById(R.id.filaDescansoAlMarcar);
        ((MaterialSwitch) alMarcar.findViewById(R.id.swInterruptor)).setChecked(prefsManager.getDescansoAlMarcar());

        View sinPauta = findViewById(R.id.filaDescansoSinPauta);
        String valor = textoDescanso(prefsManager.getDescansoSinPauta());
        TextView tvValor = sinPauta.findViewById(R.id.tvValor);
        tvValor.setText(valor);
        tvValor.setVisibility(View.VISIBLE);
        sinPauta.setContentDescription(getString(R.string.ajustes_fila_valor_a11y,
                getString(R.string.ajustes_descanso_sin_pauta),
                valor + ". " + getString(R.string.ajustes_descanso_sin_pauta_sub)));

        View avisos = findViewById(R.id.filaAvisosDescanso);
        boolean listos = PermisosDescanso.falta(this) == PermisosDescanso.Falta.NADA;
        String estado = getString(listos ? R.string.ajustes_avisos_activados : R.string.ajustes_avisos_sin_permiso);
        TextView sub = avisos.findViewById(R.id.tvSub);
        sub.setText(estado);
        sub.setVisibility(View.VISIBLE);
        avisos.findViewById(R.id.btnAccion).setVisibility(listos ? View.GONE : View.VISIBLE);
        avisos.setContentDescription(getString(R.string.ajustes_fila_sub_a11y,
                getString(R.string.ajustes_avisos_apagada), estado));
    }

    // «90 s», «2 min».
    private String textoDescanso(int segundos) {
        return segundos % 60 == 0 && segundos >= 120
                ? getString(R.string.ajustes_descanso_minutos, segundos / 60)
                : getString(R.string.ajustes_descanso_segundos, segundos);
    }

    // ── Las hojas de elegir: tema, idioma, vaso y descanso (tablero 22) ─────────

    private void elegirTema() {
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        opciones.add(HojaElegir.Opcion.conMiniatura(getString(R.string.ajustes_tema_oscuro),
                R.color.gp_vista_tema_oscuro_fondo, R.color.gp_vista_tema_oscuro_tarjeta, R.color.gp_vista_tema_oscuro_trazo));
        opciones.add(HojaElegir.Opcion.conMiniatura(getString(R.string.ajustes_tema_claro),
                R.color.gp_vista_tema_claro_fondo, R.color.gp_vista_tema_claro_tarjeta, R.color.gp_vista_tema_claro_trazo));
        int actual = prefsManager.getTheme() == AppCompatDelegate.MODE_NIGHT_NO ? 1 : 0;
        HojaElegir.mostrar(this, R.string.ajustes_tema, opciones, actual, R.string.perfil_hoja_nota, i -> {
            int modo = i == 1 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            if (modo == prefsManager.getTheme()) return;
            prefsManager.saveTheme(modo);
            recreate();
        });
    }

    private void elegirIdioma() {
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        opciones.add(HojaElegir.Opcion.de(getString(R.string.ajustes_idioma_es)));
        opciones.add(HojaElegir.Opcion.de(getString(R.string.ajustes_idioma_en)));
        HojaElegir.mostrar(this, R.string.ajustes_idioma, opciones, esIngles() ? 1 : 0, R.string.perfil_hoja_nota, i -> {
            String lang = i == 1 ? "en" : "es";
            if ((i == 1) != esIngles()) cambiarIdioma(lang);
        });
    }

    // El tamaño del vaso de agua, de una lista cerrada (Agua.VASOS). Lo que ya se bebió no
    // cambia: el agua se guarda en mililitros; solo cambia lo que suma cada toque.
    private void elegirVaso() {
        int[] vasos = Agua.VASOS;
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        int elegida = -1;
        for (int i = 0; i < vasos.length; i++) {
            opciones.add(HojaElegir.Opcion.de(getString(R.string.perfil_vaso_ml, vasos[i])));
            if (vasos[i] == prefsManager.getVasoMl()) elegida = i;
        }
        HojaElegir.mostrar(this, R.string.perfil_vaso, opciones, elegida, R.string.perfil_vaso_nota, i -> {
            prefsManager.setVasoMl(vasos[i]);
            pintarValores();
        });
    }

    private void elegirDescansoSinPauta() {
        int[] tiempos = LogicaDescanso.SIN_PAUTA_OPCIONES;
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        int elegida = -1;
        for (int i = 0; i < tiempos.length; i++) {
            opciones.add(HojaElegir.Opcion.de(textoDescanso(tiempos[i])));
            if (tiempos[i] == prefsManager.getDescansoSinPauta()) elegida = i;
        }
        HojaElegir.mostrar(this, R.string.ajustes_descanso_sin_pauta, opciones, elegida,
                R.string.ajustes_descanso_sin_pauta_sub, i -> {
                    prefsManager.saveDescansoSinPauta(tiempos[i]);
                    pintarEntrenamiento();
                });
    }

    // ── Filas ────────────────────────────────────────────────────────────────

    private void fila(int id, @DrawableRes int icono, @StringRes int titulo, Runnable accion) {
        View f = findViewById(id);
        ((ImageView) f.findViewById(R.id.ivIcono)).setImageResource(icono);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        f.setContentDescription(getString(titulo));
        f.setOnClickListener(v -> accion.run());
    }

    private void valor(int id, @StringRes int titulo, @Nullable String valor) {
        View f = findViewById(id);
        TextView tv = f.findViewById(R.id.tvValor);
        boolean hay = valor != null && !valor.isEmpty();
        tv.setText(valor);
        tv.setVisibility(hay ? View.VISIBLE : View.GONE);
        f.setContentDescription(hay ? getString(R.string.ajustes_fila_valor_a11y, getString(titulo), valor)
                : getString(titulo));
    }

    // Las que salen de la app llevan la flecha de «abre fuera» y lo dicen a TalkBack.
    private void abreFuera(int id, @StringRes int titulo) {
        View f = findViewById(id);
        ((ImageView) f.findViewById(R.id.ivFlecha)).setImageResource(R.drawable.ic_ms_open_in_new);
        f.setContentDescription(getString(R.string.ajustes_abre_fuera_a11y, getString(titulo)));
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    // Correo a soporte@gymprofit.app con el asunto (y el cuerpo) de cada caso.
    private void escribir(String asunto, @Nullable String cuerpo) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email_soporte)});
        intent.putExtra(Intent.EXTRA_SUBJECT, asunto);
        if (cuerpo != null) intent.putExtra(Intent.EXTRA_TEXT, cuerpo);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.email_elegir_app)));
        } catch (ActivityNotFoundException e) {
            UIHelper.mostrarToastError(this, getString(R.string.email_sin_app));
        }
    }

    // Versión de la app, de Android y el modelo del teléfono. Nada personal.
    private String cuerpoError() {
        return getString(R.string.email_error_cuerpo, BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE, Build.MANUFACTURER + " " + Build.MODEL);
    }
}
