package es.pmdm.gymprofit.model.envivo;

import java.math.BigDecimal;
import java.util.List;

// ============================================================
// UltimaVez — un elemento de GET /sesiones/ultima-vez (GP-014): las series de la última
// sesión terminada del usuario que tuvo ese ejercicio. Los ejercicios sin ninguna no
// vienen en la respuesta.
// ============================================================
public class UltimaVez {

    public int ejercicioId;
    /** Inicio de la sesión, «yyyy-MM-ddTHH:mm:ss». */
    public String fecha;
    public List<Serie> series;

    /** Una serie de la última vez. */
    public static class Serie {
        public int numero;
        /** Null en peso corporal. */
        public BigDecimal peso;
        public Integer repeticiones;
        /** Solo en las series por tiempo. */
        public Integer segundos;
    }
}
