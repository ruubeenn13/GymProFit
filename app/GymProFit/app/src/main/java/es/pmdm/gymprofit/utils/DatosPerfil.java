package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

// ============================================================
// DatosPerfil — lo que se edita en «Tus datos y objetivos» (GP-216, tablero 18).
//
// Un valor inmutable: cada botón \u2212/+ o cada opción devuelve una copia. Así la pantalla
// guarda el estado inicial, compara con el actual (distintoDe) y enciende «Guardar» solo
// si algo cambió, y vuelve a apagarlo si se deshace. Los límites son los de siempre:
// peso 30 a 300 (HojasAlta), altura 100 a 250 y edad 14 a 100 (ReglasEdad, DEC-038).
// ============================================================
public final class DatosPerfil {

    public static final double PESO_MIN = 30, PESO_MAX = 300, PESO_PASO = 0.1;
    public static final int ALTURA_MIN = 100, ALTURA_MAX = 250;

    @NonNull public final String nombre;
    public final double peso;
    public final int altura;
    public final int edad;
    /** HOMBRE o MUJER, como en la API (GP-111). */
    @NonNull public final String sexo;
    /** Una de las ACTIVIDAD_* de {@link CalculadoraNutricional}. */
    @NonNull public final String actividad;
    /** Uno de los valores de objetivo de la API. */
    @NonNull public final String objetivo;
    /** Nivel de experiencia tal cual lo guarda la cuenta (un EXPERTO se conserva). */
    @NonNull public final String nivel;

    public DatosPerfil(@Nullable String nombre, double peso, int altura, int edad, @Nullable String sexo,
                       @Nullable String actividad, @Nullable String objetivo, @Nullable String nivel) {
        this.nombre = nombre == null ? "" : nombre;
        this.peso = peso;
        this.altura = altura;
        this.edad = edad;
        this.sexo = sexo == null ? "" : sexo;
        this.actividad = actividad == null ? "" : actividad;
        this.objetivo = objetivo == null ? "" : objetivo;
        this.nivel = nivel == null ? "" : nivel;
    }

    public DatosPerfil conNombre(String v) { return new DatosPerfil(v, peso, altura, edad, sexo, actividad, objetivo, nivel); }
    public DatosPerfil conPeso(double v) { return new DatosPerfil(nombre, redondear(v), altura, edad, sexo, actividad, objetivo, nivel); }
    public DatosPerfil conAltura(int v) { return new DatosPerfil(nombre, peso, v, edad, sexo, actividad, objetivo, nivel); }
    public DatosPerfil conEdad(int v) { return new DatosPerfil(nombre, peso, altura, v, sexo, actividad, objetivo, nivel); }
    public DatosPerfil conSexo(String v) { return new DatosPerfil(nombre, peso, altura, edad, v, actividad, objetivo, nivel); }
    public DatosPerfil conActividad(String v) { return new DatosPerfil(nombre, peso, altura, edad, sexo, v, objetivo, nivel); }
    public DatosPerfil conObjetivo(String v) { return new DatosPerfil(nombre, peso, altura, edad, sexo, actividad, v, nivel); }
    public DatosPerfil conNivel(String v) { return new DatosPerfil(nombre, peso, altura, edad, sexo, actividad, objetivo, v); }

    // Los botones \u2212/+. Se paran en el límite, no lo pasan.
    public DatosPerfil masPeso() { return conPeso(Math.min(PESO_MAX, peso + PESO_PASO)); }
    public DatosPerfil menosPeso() { return conPeso(Math.max(PESO_MIN, peso - PESO_PASO)); }
    public DatosPerfil masAltura() { return conAltura(Math.min(ALTURA_MAX, altura + 1)); }
    public DatosPerfil menosAltura() { return conAltura(Math.max(ALTURA_MIN, altura - 1)); }
    public DatosPerfil masEdad() { return conEdad(Math.min(ReglasEdad.MAXIMA, edad + 1)); }
    public DatosPerfil menosEdad() { return conEdad(Math.max(ReglasEdad.MINIMA, edad - 1)); }

    /**
     * True si algo es distinto de {@code otro}. El nombre se compara recortado: «Dani » y
     * «Dani» son el mismo, y en blanco es en blanco.
     */
    public boolean distintoDe(@NonNull DatosPerfil otro) {
        return !nombre.trim().equals(otro.nombre.trim())
                || Double.compare(peso, otro.peso) != 0
                || altura != otro.altura
                || edad != otro.edad
                || !sexo.equals(otro.sexo)
                || !actividad.equals(otro.actividad)
                || !objetivo.equals(otro.objetivo)
                || !nivel.equals(otro.nivel);
    }

    // Una décima exacta: 78.6 + 0.1 en coma flotante es 78.69999…, y eso se arrastraría.
    private static double redondear(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DatosPerfil && !distintoDe((DatosPerfil) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.trim(), peso, altura, edad, sexo, actividad, objetivo, nivel);
    }
}
