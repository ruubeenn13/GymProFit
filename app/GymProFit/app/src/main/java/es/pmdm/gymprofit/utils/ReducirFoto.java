package es.pmdm.gymprofit.utils;

// ============================================================
// ReducirFoto — la cuenta de la foto de perfil antes de subirla (GP-188, lote 1.6.5)
//
// La foto de la galería o de la cámara pesa varios MB y la API admite 1 MB. Antes de
// subirla se decodifica ya reducida (sin cargar la imagen entera), se endereza según su
// EXIF, se recorta al cuadrado central y se deja en LADO × LADO, JPEG a CALIDAD: decenas
// de KB y sin metadatos (tampoco la ubicación). Aquí, sin vistas, solo la cuenta:
//   · muestreo: el inSampleSize más grande (potencia de dos) que no deja el lado corto
//     por debajo del que se quiere;
//   · giro: los grados y si hay espejo, de la orientación EXIF (los valores de
//     ExifInterface.ORIENTATION_*, del 1 al 8);
//   · recorte: el cuadrado central, como {x, y, lado}.
// FotoParaSubir hace lo de Android con esto.
// ============================================================
public final class ReducirFoto {

    /** El lado de lo que se sube. */
    public static final int LADO = 512;
    /** La calidad del JPEG que se sube. */
    public static final int CALIDAD = 85;

    // Los valores de ExifInterface.ORIENTATION_*.
    public static final int EXIF_NORMAL = 1;
    public static final int EXIF_ESPEJO_HORIZONTAL = 2;
    public static final int EXIF_GIRA_180 = 3;
    public static final int EXIF_ESPEJO_VERTICAL = 4;
    public static final int EXIF_TRANSPONER = 5;
    public static final int EXIF_GIRA_90 = 6;
    public static final int EXIF_TRANSVERSAL = 7;
    public static final int EXIF_GIRA_270 = 8;

    /** Cuánto girar (en el sentido de las agujas) y si después hay que hacer espejo horizontal. */
    public static final class Giro {
        public final int grados;
        public final boolean espejo;

        Giro(int grados, boolean espejo) {
            this.grados = grados;
            this.espejo = espejo;
        }

        /** ¿Gira un cuarto de vuelta, y el ancho pasa a ser el alto? */
        public boolean cambiaLados() {
            return grados == 90 || grados == 270;
        }
    }

    private ReducirFoto() {
    }

    /**
     * @return el inSampleSize: la mayor potencia de dos que deja el lado corto en {@code lado} o más.
     */
    public static int muestreo(int ancho, int alto, int lado) {
        int corto = Math.min(ancho, alto);
        int muestra = 1;
        while (corto / (muestra * 2) >= lado) muestra *= 2;
        return muestra;
    }

    /** El giro de una orientación EXIF; una desconocida, ninguno. */
    public static Giro giro(int orientacion) {
        switch (orientacion) {
            case EXIF_ESPEJO_HORIZONTAL: return new Giro(0, true);
            case EXIF_GIRA_180:          return new Giro(180, false);
            case EXIF_ESPEJO_VERTICAL:   return new Giro(180, true);
            case EXIF_TRANSPONER:        return new Giro(90, true);
            case EXIF_GIRA_90:           return new Giro(90, false);
            case EXIF_TRANSVERSAL:       return new Giro(270, true);
            case EXIF_GIRA_270:          return new Giro(270, false);
            default:                     return new Giro(0, false);
        }
    }

    /**
     * @return el cuadrado central de una imagen ya derecha, como {x, y, lado}.
     */
    public static int[] recorte(int ancho, int alto) {
        int lado = Math.min(ancho, alto);
        return new int[]{(ancho - lado) / 2, (alto - lado) / 2, lado};
    }
}
