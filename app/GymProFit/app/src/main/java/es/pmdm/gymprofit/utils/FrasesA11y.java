package es.pmdm.gymprofit.utils;

// ============================================================
// FrasesA11y — junta frases para un nombre accesible sin dejar «peso., te faltan» (GP-225).
//
// Cada parte es una frase; se quita el punto final de cada una y se unen con «. ». Las partes
// vacías no cuentan. TalkBack hace la pausa en cada punto.
// ============================================================
public final class FrasesA11y {

    private FrasesA11y() { }

    /**
     * Junta las frases con «. », sin punto doble ni al final.
     *
     * @param partes frases, con o sin punto final; las nulas o en blanco se saltan.
     * @return las frases unidas, o «» si no hay ninguna.
     */
    public static String unir(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p == null) continue;
            String f = p.trim();
            while (f.endsWith(".")) f = f.substring(0, f.length() - 1).trim();
            if (f.isEmpty()) continue;
            if (sb.length() > 0) sb.append(". ");
            sb.append(f);
        }
        return sb.toString();
    }
}
