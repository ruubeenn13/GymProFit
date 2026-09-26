package es.pmdm.gymprofit.depuracion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**
 * Trampolín de depuración para capturar pantallas desde adb.
 *
 * <p>Las pantallas de la app no están exportadas, así que {@code am start} no puede
 * abrirlas: para sacar decenas de capturas en ES/EN y claro/oscuro habría que llegar
 * a cada una tocando. Esta actividad, que solo existe en la variante {@code debug},
 * sí está exportada y abre la que se le indique con los extras que traiga:</p>
 * <pre>
 * adb shell am start -n com.gymprofit.app/es.pmdm.gymprofit.depuracion.TrampolinActivity \
 *     --es destino DetalleRutinaActivity --ei rutinaId 12
 * </pre>
 *
 * <p>La sesión es la que haya en el dispositivo: si no se ha entrado antes, las
 * pantallas que llaman a la API reciben 401 como en cualquier otro caso.</p>
 */
public class TrampolinActivity extends Activity {

    private static final String PAQUETE_PANTALLAS = "es.pmdm.gymprofit.ui.activities.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El token lo carga UtilREST del almacén cuando le falta (GP-091): ya no hace
        // falta copiarlo aquí como hacía la splash.

        String destino = getIntent().getStringExtra("destino");
        if (destino != null) {
            String clase = destino.contains(".") ? destino : PAQUETE_PANTALLAS + destino;
            Intent i = new Intent().setClassName(getPackageName(), clase);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                extras.remove("destino");
                i.putExtras(extras);
            }
            try {
                startActivity(i);
            } catch (RuntimeException e) {
                Log.e("TRAMPOLIN", "No se pudo abrir " + clase, e);
            }
        }
        finish();
    }
}
