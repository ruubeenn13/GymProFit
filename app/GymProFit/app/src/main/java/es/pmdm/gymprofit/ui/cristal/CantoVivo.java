package es.pmdm.gymprofit.ui.cristal;

// ============================================================
// CantoVivo — dónde va el velo que desvanece el contenido junto al canto de un cristal vivo
// bajo el que pasa (GP-285, remate del lote 2; DEC-070).
//
// Lo que pasa por detrás de un cristal vivo se ve desenfocado o se desvanece antes de su canto,
// nunca nítido y cortado: en la Sesión en vivo, el texto de la tarjeta de debajo asomaba entero
// justo bajo el resumen, con media línea ya tapada por él. Un velo (BordeSistema, el propio
// fondo ambiental) lo desvanece antes de llegar. De fuera adentro:
//
//   · una rampa, de transparente a opaco, que acaba un margen antes del canto. Es corta (da
//     para una línea de texto y media) y por eso su curva es suave en los dos extremos y a media
//     rampa ya tapa la mitad: con el cuadrado de los bordes del sistema, que ahí tapa un cuarto,
//     el texto llegaba al canto todavía a medio ver;
//   · opaco en ese margen, en el canto y hasta un cuarto de radio por dentro del cristal: el
//     canto no corta nada, y quedan tapados los dos picos que dejan sus esquinas redondas;
//   · y por dentro, hasta un radio del canto, otra rampa de vuelta a transparente (la entrada):
//     el cristal la tapa, pero en los márgenes de los lados no hay cristal, y un velo que
//     acabara en seco cortaba en una raya la sombra de la tarjeta de debajo.
//
// Más adentro la lista sigue pasando: es lo que el cristal desenfoca (niveles 1 y 2). En el
// nivel 3 el cristal es opaco y solo queda el velo.
//
// Aquí solo hay cuentas, sin nada de Android, para poder probarlas (CantoVivoTest); el alfa de
// cada fila lo da BordesSistema.alfas, y quien las usa es SesionEnVivoActivity.
// ============================================================
public final class CantoVivo {

    /** Cuánto del radio, desde dentro, tarda el velo en hacerse opaco. */
    static final float ENTRADA = 0.75f;

    private CantoVivo() { }

    /**
     * Dónde va un velo, en px: su canto de arriba, su alto, cuánto de él no es la rampa de
     * fuera (macizo) y, de eso, cuánto es la rampa de dentro (entrada).
     */
    public static final class Velo {
        public final int arriba, alto, macizo, entrada;

        Velo(int arriba, int alto, int macizo, int entrada) {
            this.arriba = arriba;
            this.alto = alto;
            this.macizo = macizo;
            this.entrada = entrada;
        }
    }

    /**
     * El velo bajo el canto de abajo de un cristal (el resumen): se desvanece hacia abajo.
     *
     * @param canto  la y del canto de abajo del cristal, en px.
     * @param radio  el radio de sus esquinas, en px.
     * @param margen lo que lo opaco sobresale por fuera del canto, en px.
     * @param rampa  alto del tramo que se desvanece hacia la lista, en px.
     */
    public static Velo bajo(int canto, float radio, int margen, int rampa) {
        int dentro = Math.round(Math.max(0f, radio));
        return new Velo(canto - dentro, dentro + margen + rampa, dentro + margen, entrada(radio));
    }

    /**
     * El velo sobre el canto de arriba de un cristal (el descanso): se desvanece hacia arriba.
     *
     * @param canto la y del canto de arriba del cristal, en px; lo demás, como en {@link #bajo}.
     */
    public static Velo sobre(int canto, float radio, int margen, int rampa) {
        int dentro = Math.round(Math.max(0f, radio));
        return new Velo(canto - margen - rampa, rampa + margen + dentro, margen + dentro, entrada(radio));
    }

    private static int entrada(float radio) {
        return Math.round(Math.max(0f, radio) * ENTRADA);
    }

    /**
     * Lo que hay que dejar libre entre el canto del cristal y lo primero de la lista en reposo
     * para que el velo no lo toque: el margen y la rampa.
     */
    public static int hueco(int margen, int rampa) {
        return margen + rampa;
    }

    /**
     * Lo que la lista tiene que dejar libre de más junto al cristal, además de lo que ya la separa
     * de él, para que quepa el velo.
     *
     * @param hueco      el margen y la rampa del velo ({@link #hueco}), en px.
     * @param separacion lo que ya separa lo primero de la lista del canto del cristal, en px.
     */
    public static int sitio(int hueco, int separacion) {
        return Math.max(0, hueco - separacion);
    }
}
