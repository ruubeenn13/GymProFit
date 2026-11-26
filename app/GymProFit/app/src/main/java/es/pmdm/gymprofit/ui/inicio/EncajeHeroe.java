package es.pmdm.gymprofit.ui.inicio;

// ============================================================
// EncajeHeroe — la tarjeta «Hoy toca» frente a la barra flotante (GP-285, DEC-070).
//
// La barra flota sobre el contenido, y según el móvil, el idioma y la letra el último botón de
// la tarjeta («Empezar») caía pegado a ella (1,5 dp con letra 1,3 en español) o a medias debajo.
// Si con ceder un poco el botón queda a su margen de la barra, la tarjeta cede justo eso (el
// bloque del icono de cada ejercicio se acorta); si no llega ni cediendo todo, no se toca: el
// botón queda claramente debajo y se llega a él desplazando, como al resto.
//
// Solo la cuenta, sin vistas, para poder probarla (EncajeHeroeTest). La aplica InicioFragment.
// ============================================================
public final class EncajeHeroe {

    private EncajeHeroe() { }

    /**
     * Cuánto tiene que ceder la tarjeta, en px.
     *
     * <p>Parte del alto natural (el de ahora más lo ya cedido), así que llamarla otra vez tras
     * aplicar su resultado devuelve lo mismo y no oscila.
     *
     * @param fondoBoton dónde acaba ahora el último botón, en el contenido del scroll.
     * @param cedido     lo que la tarjeta ya ha cedido.
     * @param tope       dónde empieza la barra flotante (con el scroll arriba del todo).
     * @param margen     el mínimo entre el botón y la barra (barra_margen_boton).
     * @param maximo     lo más que puede ceder; 0 si no hay de dónde.
     * @return lo que falta para ese margen, o 0 si sobra sitio o no se llega ni con el máximo.
     */
    public static int cede(int fondoBoton, int cedido, int tope, int margen, int maximo) {
        int falta = fondoBoton + cedido + margen - tope;
        return falta > 0 && falta <= maximo ? falta : 0;
    }
}
