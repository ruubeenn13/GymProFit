package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// CodigoBarras — qué es un código de producto escrito a mano (lote 1.6.1)
//
// «Escribir el código» del escáner no pregunta a la API por un código que no puede
// existir: tiene que tener la longitud de un código de producto (8, 12 o 13 cifras) y
// su dígito de control tiene que cuadrar. Así un 7 por un 1 no gasta una petición ni
// cupo de Open Food Facts, y se dice qué está mal.
//   · 13 cifras: EAN-13.  · 12: UPC-A.  · 8: EAN-8 o UPC-E (que se comprueba expandido
//     a UPC-A, que es como se calcula su dígito).
// Lo que lee la cámara no pasa por aquí: ML Kit ya comprueba el dígito al leer.
// ============================================================
public final class CodigoBarras {

    /** Qué tiene mal un código. */
    public enum Problema {
        /** No tiene 8, 12 ni 13 cifras. */
        LONGITUD,
        /** Tiene algo que no es una cifra. */
        CIFRAS,
        /** El último dígito no es el que toca: hay una cifra mal escrita. */
        DIGITO
    }

    private CodigoBarras() {
    }

    /** Quita los espacios y guiones con los que se suele copiar un código de la etiqueta. */
    @NonNull
    public static String limpiar(@Nullable CharSequence texto) {
        if (texto == null) return "";
        return texto.toString().replaceAll("[\\s-]", "");
    }

    /**
     * @param codigo el código, ya limpio.
     * @return null si es un código de producto válido; si no, qué tiene mal.
     */
    @Nullable
    public static Problema problema(@Nullable String codigo) {
        if (codigo == null || codigo.isEmpty()) return Problema.LONGITUD;
        for (int i = 0; i < codigo.length(); i++) {
            if (codigo.charAt(i) < '0' || codigo.charAt(i) > '9') return Problema.CIFRAS;
        }
        switch (codigo.length()) {
            case 13:
            case 12:
                return cuadra(codigo) ? null : Problema.DIGITO;
            case 8:
                return cuadra(codigo) || upceCuadra(codigo) ? null : Problema.DIGITO;
            default:
                return Problema.LONGITUD;
        }
    }

    /** ¿Es un código de producto válido? */
    public static boolean valido(@Nullable String codigo) {
        return problema(codigo) == null;
    }

    // EAN-13, UPC-A y EAN-8 comparten la regla: desde la derecha, sin contar el dígito de
    // control, los dígitos pesan 3, 1, 3, 1…; el control completa la suma a decena.
    private static boolean cuadra(String codigo) {
        int n = codigo.length();
        int suma = 0;
        for (int i = n - 2, peso = 3; i >= 0; i--, peso = 4 - peso) {
            suma += (codigo.charAt(i) - '0') * peso;
        }
        int control = (10 - suma % 10) % 10;
        return control == codigo.charAt(n - 1) - '0';
    }

    // UPC-E: sistema (0 o 1), seis cifras y el control del UPC-A al que se expande.
    private static boolean upceCuadra(String codigo) {
        char sistema = codigo.charAt(0);
        if (sistema != '0' && sistema != '1') return false;
        String d = codigo.substring(1, 7);
        String cuerpo;
        switch (d.charAt(5)) {
            case '0':
            case '1':
            case '2':
                cuerpo = d.substring(0, 2) + d.charAt(5) + "0000" + d.substring(2, 5);
                break;
            case '3':
                cuerpo = d.substring(0, 3) + "00000" + d.substring(3, 5);
                break;
            case '4':
                cuerpo = d.substring(0, 4) + "00000" + d.charAt(4);
                break;
            default:
                cuerpo = d.substring(0, 5) + "0000" + d.charAt(5);
                break;
        }
        return cuadra(sistema + cuerpo + codigo.charAt(7));
    }
}
