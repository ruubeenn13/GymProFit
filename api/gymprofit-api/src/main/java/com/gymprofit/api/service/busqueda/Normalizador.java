package com.gymprofit.api.service.busqueda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ============================================================
// Normalizador — de un texto a los términos con los que se busca (GP-162, DEC-040)
//
// Lo mismo para el nombre de un alimento que para lo que escribe quien busca, para
// que coincidan aunque no se escriban igual:
//   · sin tildes ni mayúsculas, y solo letras y cifras («Plátano» → «platano»);
//   · sin palabras vacías («de», «con», «la», «of», «with»…), así que da igual el
//     orden y da igual «pechuga de pollo» que «pollo pechuga»;
//   · singular y plural iguales: no es un lematizador, es una regla que lleva las dos
//     formas al mismo sitio («huevos» y «huevo» → «huevo»; «limones» y «limón» →
//     «limon»; «nueces» y «nuez» → «nuec»). El resultado no siempre es una palabra,
//     pero es el mismo para las dos formas, que es lo único que importa;
//   · unos pocos sinónimos de uso común, de busqueda/sinonimos.txt.
// ============================================================
public final class Normalizador {

    private static final Set<String> VACIAS = Set.of(
            "de", "del", "la", "las", "el", "los", "lo", "con", "sin", "y", "e", "en", "a", "al",
            "para", "por", "o", "u", "un", "una", "unos", "unas",
            "of", "with", "without", "and", "the", "in", "an", "or", "for");

    private static final Map<String, String> SINONIMOS = cargarSinonimos();

    private Normalizador() {
    }

    /**
     * Términos de un texto, en orden y con repetidos (la posición importa para saber si
     * un nombre empieza por lo que se busca).
     *
     * @param texto nombre de un alimento o consulta; null vale como vacío.
     * @return los términos normalizados.
     */
    public static List<String> terminos(String texto) {
        List<String> terminos = new ArrayList<>();
        if (texto == null || texto.isBlank()) return terminos;
        for (String palabra : sinTildes(texto).split("[^a-z0-9]+")) {
            if (palabra.isEmpty() || VACIAS.contains(palabra)) continue;
            String raiz = raiz(palabra);
            terminos.add(SINONIMOS.getOrDefault(raiz, raiz));
        }
        return terminos;
    }

    /** Minúsculas y sin marcas diacríticas («Ñandú» → «nandu»). */
    static String sinTildes(String texto) {
        String descompuesto = Normalizer.normalize(texto.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(descompuesto.length());
        for (int i = 0; i < descompuesto.length(); i++) {
            char c = descompuesto.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) sb.append(c);
        }
        return sb.toString();
    }

    /** Lleva singular y plural a la misma forma. Solo palabras de letras de 4 o más. */
    static String raiz(String palabra) {
        if (palabra.length() < 4 || !palabra.chars().allMatch(Character::isLetter)) return palabra;
        String r = palabra;
        if (r.endsWith("ies") && r.length() > 4) {
            r = r.substring(0, r.length() - 3) + "y";          // berries → berry
        } else if (r.endsWith("ces")) {
            r = r.substring(0, r.length() - 3) + "z";          // nueces → nuez
        } else if (r.endsWith("s") && !r.endsWith("ss")) {
            r = r.substring(0, r.length() - 1);                // huevos → huevo
        }
        if (r.length() > 3 && r.endsWith("e") && !esVocal(r.charAt(r.length() - 2))) {
            r = r.substring(0, r.length() - 1);                // limone, tomate → limon, tomat
        } else if (r.endsWith("oe")) {
            r = r.substring(0, r.length() - 1);                // potatoe → potato
        }
        if (r.endsWith("z")) {
            r = r.substring(0, r.length() - 1) + "c";          // nuez, arroz → nuec, arroc
        }
        return r;
    }

    private static boolean esVocal(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    // Cada palabra del grupo apunta a la raíz de la primera.
    private static Map<String, String> cargarSinonimos() {
        Map<String, String> mapa = new HashMap<>();
        try (InputStream in = Normalizador.class.getResourceAsStream("/busqueda/sinonimos.txt")) {
            if (in == null) throw new IllegalStateException("Falta busqueda/sinonimos.txt");
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                String[] palabras = linea.split("\\s*,\\s*");
                String canonica = raiz(sinTildes(palabras[0]));
                for (String palabra : palabras) {
                    mapa.put(raiz(sinTildes(palabra)), canonica);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Map.copyOf(mapa);
    }
}
