package es.pmdm.gymprofit.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

// ============================================================
// FotoParaSubir — la foto de perfil, reducida antes de subirla (GP-188, lote 1.6.5)
//
// Con la cuenta de ReducirFoto: se leen solo las medidas, se decodifica ya reducida (sin
// la imagen entera en memoria), se endereza según el EXIF, se recorta al cuadrado central,
// se deja en 512 × 512 y se comprime en JPEG 85. Bitmap.compress no escribe EXIF: lo que
// sube no lleva metadatos, tampoco la ubicación. Todo fuera del hilo principal.
// Lo mismo, sin recortar ni comprimir, para pintar una foto descargada: puede quedar
// alguna grande de antes de esta versión, sin reducir y con su EXIF.
// ============================================================
public final class FotoParaSubir {

    /** De dónde se vuelve a leer la imagen (se lee dos o tres veces: medidas, EXIF y datos). */
    interface Fuente {
        InputStream abrir() throws IOException;
    }

    private FotoParaSubir() {
    }

    /**
     * @return el JPEG de 512 × 512 listo para subir.
     * @throws IOException si no se puede leer o no es una imagen.
     */
    @WorkerThread
    @NonNull
    public static byte[] preparar(@NonNull ContentResolver cr, @NonNull Uri uri) throws IOException {
        Bitmap derecha = derecha(() -> {
            InputStream is = cr.openInputStream(uri);
            if (is == null) throw new IOException("Sin datos: " + uri);
            return is;
        }, ReducirFoto.LADO);
        int[] r = ReducirFoto.recorte(derecha.getWidth(), derecha.getHeight());
        Bitmap cuadrada = Bitmap.createBitmap(derecha, r[0], r[1], r[2], r[2]);
        Bitmap lista = Bitmap.createScaledBitmap(cuadrada, ReducirFoto.LADO, ReducirFoto.LADO, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        lista.compress(Bitmap.CompressFormat.JPEG, ReducirFoto.CALIDAD, out);
        if (lista != cuadrada) lista.recycle();
        if (cuadrada != derecha) cuadrada.recycle();
        derecha.recycle();
        return out.toByteArray();
    }

    /**
     * Una foto descargada, reducida a {@code lado} como mucho por su lado corto y derecha.
     *
     * @return el bitmap, o null si los bytes no son una imagen.
     */
    @WorkerThread
    @Nullable
    public static Bitmap paraPintar(@NonNull byte[] datos, int lado) {
        try {
            return derecha(() -> new ByteArrayInputStream(datos), lado);
        } catch (IOException e) {
            return null;
        }
    }

    // Decodificada reduciendo y girada según su EXIF.
    private static Bitmap derecha(Fuente fuente, int lado) throws IOException {
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        try (InputStream is = fuente.abrir()) {
            BitmapFactory.decodeStream(is, null, medidas);
        }
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) throw new IOException("No es una imagen");

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = ReducirFoto.muestreo(medidas.outWidth, medidas.outHeight, lado);
        Bitmap bmp;
        try (InputStream is = fuente.abrir()) {
            bmp = BitmapFactory.decodeStream(is, null, opciones);
        }
        if (bmp == null) throw new IOException("No se ha podido decodificar");

        int orientacion;
        try (InputStream is = fuente.abrir()) {
            orientacion = new ExifInterface(is).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException | RuntimeException sinExif) {
            // Sin EXIF legible (un PNG, una foto ya reducida): tal cual.
            orientacion = ExifInterface.ORIENTATION_NORMAL;
        }
        ReducirFoto.Giro giro = ReducirFoto.giro(orientacion);
        if (giro.grados == 0 && !giro.espejo) return bmp;
        Matrix m = new Matrix();
        m.postRotate(giro.grados);
        if (giro.espejo) m.postScale(-1, 1);
        Bitmap girada = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        if (girada != bmp) bmp.recycle();
        return girada;
    }
}
