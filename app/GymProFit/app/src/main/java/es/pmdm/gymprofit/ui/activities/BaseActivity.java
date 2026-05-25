package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public abstract class BaseActivity extends AppCompatActivity {

    protected PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        super.onCreate(savedInstanceState);

        UtilREST.setOnUnauthorizedListener(() -> {
            prefsManager.cerrarSesion();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    protected void setupMenuButton() {
        View btn = findViewById(R.id.btnMenuOpciones);
        if (btn != null) btn.setOnClickListener(this::mostrarMenuOpciones);
    }

    private void mostrarMenuOpciones(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_opciones, popup.getMenu());
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                popup.setForceShowIcon(true);
            } else {
                java.lang.reflect.Field mPopup = PopupMenu.class.getDeclaredField("mPopup");
                mPopup.setAccessible(true);
                Object helper = mPopup.get(popup);
                java.lang.reflect.Method setForceShowIcon =
                        helper.getClass().getMethod("setForceShowIcon", boolean.class);
                setForceShowIcon.invoke(helper, true);
            }
        } catch (Exception ignored) {}

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_tema) {
                mostrarDialogoTema();
                return true;
            } else if (id == R.id.menu_idioma) {
                mostrarDialogoIdioma();
                return true;
            } else if (id == R.id.menu_cerrar_sesion) {
                confirmarCerrarSesion();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void mostrarDialogoTema() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tema);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        android.widget.LinearLayout root = dialog.findViewById(R.id.dialogRoot);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        fondo.setColor(tv.data);
        root.setBackground(fondo);

        int temaActual = prefsManager.getTheme();
        View ivCheckClaro = dialog.findViewById(R.id.ivCheckClaro);
        View ivCheckOscuro = dialog.findViewById(R.id.ivCheckOscuro);
        if (temaActual == AppCompatDelegate.MODE_NIGHT_YES) {
            ivCheckOscuro.setVisibility(View.VISIBLE);
        } else {
            ivCheckClaro.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.optionTemaClaro).setOnClickListener(v -> {
            prefsManager.saveTheme(AppCompatDelegate.MODE_NIGHT_NO);
            dialog.dismiss();
            recreate();
        });
        dialog.findViewById(R.id.optionTemaOscuro).setOnClickListener(v -> {
            prefsManager.saveTheme(AppCompatDelegate.MODE_NIGHT_YES);
            dialog.dismiss();
            recreate();
        });
        ((com.google.android.material.button.MaterialButton) dialog.findViewById(R.id.btnCerrarTema))
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void mostrarDialogoIdioma() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_idioma);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        android.widget.LinearLayout root = dialog.findViewById(R.id.dialogRoot);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        fondo.setColor(tv.data);
        root.setBackground(fondo);

        String idiomaActual = prefsManager.getLanguage();
        ImageView ivCheckEspanol = dialog.findViewById(R.id.ivCheckEspanol);
        ImageView ivCheckIngles = dialog.findViewById(R.id.ivCheckIngles);
        if ("es".equals(idiomaActual) || idiomaActual.isEmpty()) {
            ivCheckEspanol.setVisibility(View.VISIBLE);
        } else {
            ivCheckIngles.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.optionEspanol).setOnClickListener(v -> {
            prefsManager.saveLanguage("es");
            dialog.dismiss();
            recreate();
        });
        dialog.findViewById(R.id.optionIngles).setOnClickListener(v -> {
            prefsManager.saveLanguage("en");
            dialog.dismiss();
            recreate();
        });
        ((MaterialButton) dialog.findViewById(R.id.btnCerrarIdioma))
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void confirmarCerrarSesion() {
        UIHelper.mostrarDialogoConIcono(
                this,
                getString(R.string.perfil_cerrar_sesion),
                getString(R.string.dialog_cerrar_sesion_mensaje),
                R.drawable.ic_logout,
                () -> {
                    UtilREST.clearToken();
                    prefsManager.cerrarSesion();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
