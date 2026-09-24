package es.pmdm.gymprofit.depuracion;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.core.content.ContextCompat;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Medidor de depuración (GP-089 y GP-062): recorre el árbol de accesibilidad de
 * la pantalla que está a la vista y vuelca al log, con la etiqueta {@code MEDIDOR},
 * el tamaño de cada texto y el de cada zona pulsable.
 *
 * <p>Mide lo que recibe TalkBack, no el XML: el tamaño de texto sale de la
 * información de renderizado del nodo (API 30+) y el de la zona pulsable, de sus
 * límites en pantalla. Por eso cuenta también lo que se crea desde código.</p>
 *
 * <p>Solo existe en la variante {@code debug}. Uso:</p>
 * <pre>
 * adb shell settings put secure enabled_accessibility_services \
 *     com.gymprofit.app/es.pmdm.gymprofit.depuracion.MedidorAccesibilidad
 * adb shell am broadcast -a es.pmdm.gymprofit.MEDIR -p com.gymprofit.app --es etiqueta home
 * adb logcat -d -s MEDIDOR
 * </pre>
 *
 * <p>Cada línea {@code TXT} lleva el tamaño en sp equivalentes (píxeles entre
 * densidad y escala de letra del sistema): un texto fijado en dp sale por debajo
 * de su valor cuando la letra del sistema está agrandada, que es justo lo que hay
 * que detectar. Cada línea {@code TAP} lleva el ancho y el alto en dp y la marca
 * {@code <48} si no llega al mínimo.</p>
 */
public class MedidorAccesibilidad extends AccessibilityService {

    private static final String TAG = "MEDIDOR";
    private static final String ACCION = "es.pmdm.gymprofit.MEDIR";
    private static final String PAQUETE = "com.gymprofit.app";

    private final BroadcastReceiver receptor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Fuera del hilo principal: las consultas al árbol son síncronas y
            // la app que se mide vive en este mismo proceso.
            String etiqueta = intent.getStringExtra("etiqueta");
            new Thread(() -> medir(etiqueta), "medidor").start();
        }
    };

    @Override
    protected void onServiceConnected() {
        ContextCompat.registerReceiver(this, receptor, new IntentFilter(ACCION),
                ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receptor);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Solo mide cuando se le pide: los eventos no le interesan.
    }

    @Override
    public void onInterrupt() {
        // Nada que interrumpir: no habla ni reproduce nada.
    }

    private void medir(String etiqueta) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float escala = getResources().getConfiguration().fontScale;
        Contador c = new Contador();
        Log.i(TAG, "=== INICIO " + (etiqueta != null ? etiqueta : "") + " escala=" + escala);
        List<AccessibilityWindowInfo> ventanas = getWindows();
        for (AccessibilityWindowInfo w : ventanas != null ? ventanas
                : Collections.<AccessibilityWindowInfo>emptyList()) {
            AccessibilityNodeInfo raiz = w.getRoot();
            if (raiz == null || raiz.getPackageName() == null
                    || !PAQUETE.contentEquals(raiz.getPackageName())) continue;
            recorrer(raiz, dm, escala, c);
        }
        Log.i(TAG, "=== FIN " + (etiqueta != null ? etiqueta : "")
                + " textos=" + c.textos + " minSp=" + (c.textos > 0 ? c.minSp : 0)
                + " textos<13=" + c.textosBajos + " pulsables=" + c.pulsables
                + " pulsables<48=" + c.pulsablesBajos);
    }

    private void recorrer(AccessibilityNodeInfo n, DisplayMetrics dm, float escala, Contador c) {
        if (n == null) return;
        if (n.isVisibleToUser()) {
            Rect r = new Rect();
            n.getBoundsInScreen(r);
            String id = n.getViewIdResourceName() != null
                    ? n.getViewIdResourceName().replace(PAQUETE + ":id/", "") : "-";
            CharSequence texto = n.getText();
            if (texto != null && texto.length() > 0 && Build.VERSION.SDK_INT >= 30) {
                n.refreshWithExtraData(AccessibilityNodeInfo.EXTRA_DATA_RENDERING_INFO_KEY, new Bundle());
                if (n.getExtraRenderingInfo() != null
                        && n.getExtraRenderingInfo().getTextSizeInPx() > 0) {
                    float sp = n.getExtraRenderingInfo().getTextSizeInPx() / (dm.density * escala);
                    c.textos++;
                    c.minSp = Math.min(c.minSp, sp);
                    boolean bajo = sp < 12.95f;
                    if (bajo) c.textosBajos++;
                    Log.i(TAG, String.format(Locale.ROOT, "TXT %5.1fsp %s %s \"%s\"",
                            sp, bajo ? "<13" : "   ", id, recortar(texto)));
                }
            }
            if (n.isClickable() || n.isLongClickable() || n.isCheckable()) {
                float wDp = r.width() / dm.density;
                float hDp = r.height() / dm.density;
                boolean bajo = wDp < 47.5f || hDp < 47.5f;
                c.pulsables++;
                if (bajo) c.pulsablesBajos++;
                CharSequence nombre = n.getContentDescription() != null
                        ? n.getContentDescription() : texto;
                Log.i(TAG, String.format(Locale.ROOT, "TAP %3.0fx%-3.0f %s %s %s \"%s\"",
                        wDp, hDp, bajo ? "<48" : "   ", n.getClassName(), id, recortar(nombre)));
            }
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            recorrer(n.getChild(i), dm, escala, c);
        }
    }

    private static String recortar(CharSequence s) {
        if (s == null) return "";
        String t = s.toString().replace('\n', ' ');
        return t.length() > 40 ? t.substring(0, 40) + "…" : t;
    }

    /** Totales de una medición. */
    private static final class Contador {
        int textos;
        int textosBajos;
        int pulsables;
        int pulsablesBajos;
        float minSp = Float.MAX_VALUE;
    }
}
