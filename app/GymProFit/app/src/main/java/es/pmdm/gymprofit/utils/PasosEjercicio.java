package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// PasosEjercicio — «Cómo se hace» de la ficha del ejercicio (GP-216, tablero 30). Las
// instrucciones del catálogo llegan en un solo bloque de texto: un paso por línea, o, si es un
// párrafo, uno por frase. Se quita la numeración que ya traigan («1.», «2)», «3 -») para no
// duplicarla con el círculo numerado de la pantalla. Sin dependencias de Android.
// ============================================================
public final class PasosEjercicio {

    private PasosEjercicio() { }

    /** Los pasos, en orden y sin vacíos; lista vacía si no hay texto. */
    public static List<String> de(@Nullable String instrucciones) {
        List<String> pasos = new ArrayList<>();
        if (instrucciones == null || instrucciones.trim().isEmpty()) return pasos;
        String texto = instrucciones.trim();
        String[] lineas = texto.split("\r?\n+");
        // Un párrafo suelto: se parte donde acaba una frase y empieza otra con mayúscula, así
        // «2.5 kg» no se corta.
        if (lineas.length == 1) lineas = texto.split("(?<=[.!?])\\s+(?=[A-ZÁÉÍÓÚÑ¿¡])");
        for (String l : lineas) {
            String limpio = l.trim().replaceFirst("^\\d+\\s*[.)\\-:]\\s*", "").trim();
            if (!limpio.isEmpty()) pasos.add(limpio);
        }
        return pasos;
    }
}
