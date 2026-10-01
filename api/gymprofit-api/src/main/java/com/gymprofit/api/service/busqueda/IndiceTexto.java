package com.gymprofit.api.service.busqueda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// IndiceTexto — índice invertido en memoria, inmutable (GP-162, DEC-040)
//
// Para cada término, la lista ordenada de documentos que lo tienen. Los documentos son
// números de 0 a n-1: quien construye el índice sabe a qué corresponde cada uno.
//
// Una consulta casa con un documento si TODOS sus términos casan con alguno de los del
// documento, en cualquier orden. Un término casa:
//   · exacto;
//   · por prefijo, si es el último y tiene 2 letras o más (se busca mientras se escribe);
//   · con una errata (una letra de más, de menos, cambiada o dos letras intercambiadas),
//     si tiene 5 letras o más y no ha casado de ninguna de las otras dos formas.
// Además del conjunto de documentos que casan, devuelve el de los que casan solo con
// términos exactos, que la ordenación pone delante.
//
// Inmutable: se construye entero y se sustituye de golpe, así que se puede leer desde
// cualquier hilo sin bloquear.
// ============================================================
final class IndiceTexto {

    static final int LONGITUD_MINIMA_PREFIJO = 2;
    static final int LONGITUD_MINIMA_ERRATA = 5;

    private final int documentos;
    private final String[] vocabulario;       // ordenado
    private final int[][] apariciones;        // por término, documentos en orden creciente
    private final int[][] porLongitud;        // por longitud, posiciones del vocabulario

    private IndiceTexto(int documentos, String[] vocabulario, int[][] apariciones) {
        this.documentos = documentos;
        this.vocabulario = vocabulario;
        this.apariciones = apariciones;
        int maxima = 0;
        for (String t : vocabulario) maxima = Math.max(maxima, t.length());
        int[] cuantos = new int[maxima + 2];
        for (String t : vocabulario) cuantos[t.length()]++;
        porLongitud = new int[maxima + 2][];
        for (int l = 0; l < cuantos.length; l++) porLongitud[l] = new int[cuantos[l]];
        int[] relleno = new int[maxima + 2];
        for (int i = 0; i < vocabulario.length; i++) {
            int l = vocabulario[i].length();
            porLongitud[l][relleno[l]++] = i;
        }
    }

    /** Resultado de una consulta: los que casan y, de ellos, los que casan sin prefijo ni errata. */
    record Coincidencias(BitSet todos, BitSet exactos) {
    }

    /** Construye el índice recibiendo, documento a documento, sus términos. */
    static final class Constructor {
        private final Map<String, int[]> listas = new HashMap<>();
        private final Map<String, Integer> tamanos = new HashMap<>();
        private int documentos;

        /** Añade el siguiente documento (el primero es el 0) con sus términos. */
        int anadir(List<String> terminos) {
            int doc = documentos++;
            for (String termino : terminos) {
                int[] lista = listas.get(termino);
                int n = tamanos.getOrDefault(termino, 0);
                if (lista == null) {
                    lista = new int[2];
                } else if (n > 0 && lista[n - 1] == doc) {
                    continue;                                   // repetido en el mismo documento
                } else if (n == lista.length) {
                    lista = Arrays.copyOf(lista, n * 2);
                }
                lista[n] = doc;
                listas.put(termino, lista);
                tamanos.put(termino, n + 1);
            }
            return doc;
        }

        IndiceTexto construir() {
            String[] vocabulario = listas.keySet().toArray(new String[0]);
            Arrays.sort(vocabulario);
            int[][] apariciones = new int[vocabulario.length][];
            for (int i = 0; i < vocabulario.length; i++) {
                apariciones[i] = Arrays.copyOf(listas.get(vocabulario[i]), tamanos.get(vocabulario[i]));
            }
            return new IndiceTexto(documentos, vocabulario, apariciones);
        }
    }

    int documentos() {
        return documentos;
    }

    int terminosDistintos() {
        return vocabulario.length;
    }

    /** Posición del término en el vocabulario, o -1 si no está. */
    int posicion(String termino) {
        int p = Arrays.binarySearch(vocabulario, termino);
        return p >= 0 ? p : -1;
    }


    /**
     * Documentos que casan con todos los términos de la consulta.
     *
     * @param consulta términos ya normalizados; no vacía.
     */
    Coincidencias buscar(List<String> consulta) {
        BitSet todos = null;
        BitSet exactos = null;
        for (int i = 0; i < consulta.size(); i++) {
            boolean ultimo = i == consulta.size() - 1;
            List<Integer> candidatos = candidatos(consulta.get(i), ultimo);
            int exacto = Arrays.binarySearch(vocabulario, consulta.get(i));
            BitSet deEste = new BitSet(documentos);
            for (int c : candidatos) marcar(deEste, apariciones[c]);
            BitSet exactoDeEste = new BitSet(documentos);
            if (exacto >= 0) marcar(exactoDeEste, apariciones[exacto]);
            if (todos == null) {
                todos = deEste;
                exactos = exactoDeEste;
            } else {
                todos.and(deEste);
                exactos.and(exactoDeEste);
            }
            if (todos.isEmpty()) break;
        }
        if (todos == null) return new Coincidencias(new BitSet(), new BitSet());
        exactos.and(todos);
        return new Coincidencias(todos, exactos);
    }

    /** Posiciones del vocabulario con las que casa un término (ver la cabecera). */
    List<Integer> candidatos(String termino, boolean ultimo) {
        List<Integer> candidatos = new ArrayList<>();
        int exacto = Arrays.binarySearch(vocabulario, termino);
        if (ultimo && termino.length() >= LONGITUD_MINIMA_PREFIJO) {
            int desde = exacto >= 0 ? exacto : -exacto - 1;
            for (int i = desde; i < vocabulario.length && vocabulario[i].startsWith(termino); i++) {
                candidatos.add(i);
            }
        } else if (exacto >= 0) {
            candidatos.add(exacto);
        }
        if (candidatos.isEmpty() && termino.length() >= LONGITUD_MINIMA_ERRATA) {
            for (int l = termino.length() - 1; l <= termino.length() + 1; l++) {
                if (l < 0 || l >= porLongitud.length) continue;
                for (int i : porLongitud[l]) {
                    if (unaErrata(termino, vocabulario[i])) candidatos.add(i);
                }
            }
        }
        return candidatos;
    }

    private static void marcar(BitSet bits, int[] docs) {
        for (int d : docs) bits.set(d);
    }

    /**
     * ¿Están a una errata como mucho? Una letra de más, de menos o cambiada, o dos
     * contiguas intercambiadas (distancia de Damerau restringida ≤ 1).
     */
    static boolean unaErrata(String a, String b) {
        int la = a.length();
        int lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        if (la == lb) {
            int primera = -1;
            int diferencias = 0;
            for (int i = 0; i < la; i++) {
                if (a.charAt(i) != b.charAt(i)) {
                    if (diferencias == 0) primera = i;
                    diferencias++;
                    if (diferencias > 2) return false;
                }
            }
            if (diferencias <= 1) return true;
            // Dos diferencias: solo vale si son dos letras contiguas intercambiadas.
            int s = primera;
            return s + 1 < la && a.charAt(s) == b.charAt(s + 1) && a.charAt(s + 1) == b.charAt(s)
                    && a.substring(s + 2).equals(b.substring(s + 2));
        }
        String corta = la < lb ? a : b;
        String larga = la < lb ? b : a;
        int i = 0;
        int j = 0;
        boolean saltada = false;
        while (i < corta.length() && j < larga.length()) {
            if (corta.charAt(i) == larga.charAt(j)) {
                i++;
                j++;
            } else {
                if (saltada) return false;
                saltada = true;
                j++;
            }
        }
        return true;
    }
}
