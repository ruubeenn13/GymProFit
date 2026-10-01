package com.gymprofit.api.service.busqueda;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// NormalizadorTest — de un texto a los términos de búsqueda (GP-162)
// Lo que importa no es que la raíz sea una palabra, sino que singular y plural, con y
// sin tilde, acaben en lo mismo.
// ============================================================
class NormalizadorTest {

    @ParameterizedTest(name = "«{0}» y «{1}» se buscan igual")
    @CsvSource({
            "huevo, huevos", "Plátano, PLATANOS", "limón, limones", "nuez, nueces", "tomate, tomates",
            "arroz, arroces", "pan, panes", "egg, eggs", "berry, berries", "potato, potatoes",
            "yogur, yogurt", "yogur, Yoghurt", "alubias, judías"})
    void misma_forma(String a, String b) {
        assertThat(Normalizador.terminos(a)).isEqualTo(Normalizador.terminos(b));
    }

    @Test
    @DisplayName("sin palabras vacías y en orden")
    void sin_vacias() {
        assertThat(Normalizador.terminos("Pechuga de pollo, con la piel")).containsExactly("pechuga", "pollo", "piel");
        assertThat(Normalizador.terminos("Leche 0% sin lactosa")).containsExactly("lech", "0", "lactosa");
        assertThat(Normalizador.terminos("  ")).isEmpty();
        assertThat(Normalizador.terminos(null)).isEmpty();
    }

    @Test
    @DisplayName("una errata: letra de más, de menos, cambiada o dos contiguas intercambiadas")
    void una_errata() {
        assertThat(IndiceTexto.unaErrata("pechga", "pechuga")).isTrue();
        assertThat(IndiceTexto.unaErrata("pechuuga", "pechuga")).isTrue();
        assertThat(IndiceTexto.unaErrata("pechuga", "pechuga")).isTrue();
        assertThat(IndiceTexto.unaErrata("pecuhga", "pechuga")).isTrue();
        assertThat(IndiceTexto.unaErrata("pexhuga", "pechuga")).isTrue();
        assertThat(IndiceTexto.unaErrata("pchga", "pechuga")).isFalse();
        assertThat(IndiceTexto.unaErrata("manzana", "naranja")).isFalse();
    }

    @Test
    @DisplayName("el índice casa en cualquier orden, por prefijo el último y con errata desde 5 letras")
    void indice() {
        IndiceTexto.Constructor c = new IndiceTexto.Constructor();
        c.anadir(Normalizador.terminos("Pechuga de pollo"));   // 0
        c.anadir(Normalizador.terminos("Pollo asado"));        // 1
        c.anadir(Normalizador.terminos("Pera"));               // 2
        IndiceTexto indice = c.construir();

        assertThat(indice.buscar(Normalizador.terminos("pollo pechuga")).todos().stream().boxed().toList())
                .isEqualTo(List.of(0));
        assertThat(indice.buscar(Normalizador.terminos("pol")).todos().stream().boxed().toList())
                .isEqualTo(List.of(0, 1));
        assertThat(indice.buscar(Normalizador.terminos("pechga")).todos().stream().boxed().toList())
                .isEqualTo(List.of(0));
        // «pero» tiene 4 letras: sin errata, no da «pera».
        assertThat(indice.buscar(Normalizador.terminos("pero")).todos().isEmpty()).isTrue();
        assertThat(indice.buscar(Normalizador.terminos("pollo")).exactos().stream().boxed().toList())
                .isEqualTo(List.of(0, 1));
    }
}
