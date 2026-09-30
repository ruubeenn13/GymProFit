package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// AlmacenSesion — el fichero de la sesión en curso, uno por cuenta (GP-012).
//
// Vive en el almacenamiento privado de la app (files/sesion_en_curso/<usuarioId>.json):
// sobrevive a salir, a que Android cierre la app, a quitarla de recientes y a
// reiniciar el móvil, y otra cuenta en el mismo móvil no lo ve porque lee otro fichero
// (y, por si acaso, el dueño va dentro y se comprueba al leer).
//
// Se escribe de forma atómica a cada cambio: primero a un .tmp, sincronizado con el
// disco, y luego se renombra encima. Si el móvil se apaga a mitad, queda el fichero
// anterior entero o el nuevo entero, nunca uno a medias. En Android el renombrado
// sustituye de una vez; la JVM de los tests en Windows no sustituye, y por eso el
// segundo intento borra antes (ahí no hay apagones que temer).
//
// Un fichero de otra versión del formato, o ilegible, se ignora sin romper la app: es
// preferible perder una sesión a medias que no poder abrir la aplicación.
// ============================================================
public class AlmacenSesion {

    private static final Gson GSON = new Gson();

    private final File carpeta;

    /** @param carpeta dónde van los ficheros; en la app, files/sesion_en_curso. */
    public AlmacenSesion(@NonNull File carpeta) {
        this.carpeta = carpeta;
    }

    private File fichero(int usuarioId) {
        return new File(carpeta, usuarioId + ".json");
    }

    /** La sesión en curso de esa cuenta, o null si no hay (o no se puede leer). */
    @Nullable
    public SesionEnCurso leer(int usuarioId) {
        File f = fichero(usuarioId);
        if (!f.isFile()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            SesionEnCurso s = GSON.fromJson(r, SesionEnCurso.class);
            if (s == null || s.formato != SesionEnCurso.FORMATO || s.usuarioId != usuarioId) return null;
            return s;
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    /**
     * Escribe la sesión en el fichero de su cuenta, de forma atómica.
     *
     * @return si quedó escrita.
     */
    public boolean escribir(@NonNull SesionEnCurso s) {
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) return false;
        File destino = fichero(s.usuarioId);
        File tmp = new File(carpeta, s.usuarioId + ".json.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(GSON.toJson(s).getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.getFD().sync();
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            return false;
        }
        if (tmp.renameTo(destino)) return true;
        //noinspection ResultOfMethodCallIgnored
        destino.delete();
        return tmp.renameTo(destino);
    }

    /** Borra la sesión de esa cuenta: al guardarla o al descartarla. */
    public void borrar(int usuarioId) {
        //noinspection ResultOfMethodCallIgnored
        fichero(usuarioId).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(carpeta, usuarioId + ".json.tmp").delete();
    }
}
