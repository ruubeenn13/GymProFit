package es.pmdm.gymprofit.arranque;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// DecisionArranque — adónde lleva el arranque de la app (GP-223).
//
// Sin dependencias de Android, para probarla entera (DecisionArranqueTest). La
// pantalla de carga le da lo que sabe del móvil y ella responde un destino; solo
// uno de ellos, RENOVAR, llama a la API, y su resultado vuelve aquí con trasRenovar.
//
// El orden:
//   1. Sin sesión: la bienvenida o, con el alta a medias, el alta donde se dejó. No
//      hace falta red: el cuestionario no llama a la API hasta crear la cuenta.
//   2. Con sesión sin el alta hecha: el alta de una cuenta existente, como hasta ahora.
//   3. Sin red (se sabe al momento): «Sin conexión», salvo que haya una sesión en vivo
//      guardada en el móvil, que se abre directamente.
//   4. Con el access token vigente: Inicio, sin llamar a nadie.
//   5. Caducado: se renueva con /auth/refresh; sin refresh guardado, Entrar.
//
// La caducidad se lee del campo exp del JWT sin verificar la firma: solo decide si hay
// que renovar, y quien decide si el token vale es la API, petición a petición.
// ============================================================
public final class DecisionArranque {

    private DecisionArranque() { }

    /**
     * Un token que caduca en menos de esto se renueva ya. Si no, caducaría mientras
     * Inicio pide sus datos y lo renovaría el autenticador de OkHttp en paralelo.
     */
    public static final long MARGEN_CADUCIDAD_MS = 30_000L;

    // Código de la API en «cause» para una cuenta desactivada (GP-083); el mismo que lee ApiCallback.
    private static final String CUENTA_DESACTIVADA = "CUENTA_DESACTIVADA";

    /** Adónde va el arranque. RENOVAR no es un destino: es «pregunta a la API y vuelve». */
    public enum Destino {
        BIENVENIDA, ALTA, ALTA_CUENTA, INICIO, RENOVAR,
        SIN_CONEXION, SESION_EN_VIVO, ENTRAR, ENTRAR_DESACTIVADA, NO_CONECTA
    }

    /** Lo que puede pasar al renovar la sesión. */
    public enum Renovacion {
        /** Tokens nuevos guardados. */
        OK,
        /** 401: el refresh caducó (30 días sin abrir la app) o se revocó (contraseña cambiada). */
        NO_AUTORIZADO,
        /** 401 con la cuenta desactivada (GP-083). */
        CUENTA_DESACTIVADA,
        /** Sin respuesta útil: red que no llega, tiempo agotado, 5xx o límite de peticiones. */
        SIN_RESPUESTA
    }

    /** Lo que se sabe en el móvil al abrir la app. Por defecto, nada: sin sesión y sin red. */
    public static final class Estado {
        boolean sesion, onboarding, altaEmpezada, refresh, red, sesionEnVivo;
        @Nullable Long caducaMs;
        long ahoraMs;

        /** Hay un access token guardado. */
        public Estado sesion(boolean v) { sesion = v; return this; }
        /** La cuenta guardada ya hizo el alta en este móvil. */
        public Estado onboarding(boolean v) { onboarding = v; return this; }
        /** Sin cuenta, el cuestionario del alta está empezado. */
        public Estado altaEmpezada(boolean v) { altaEmpezada = v; return this; }
        /** Hay un refresh token guardado. */
        public Estado refresh(boolean v) { refresh = v; return this; }
        /** El sistema tiene una red con salida a internet. */
        public Estado red(boolean v) { red = v; return this; }
        /** Hay una sesión en vivo de esta cuenta guardada en el móvil (GP-012). */
        public Estado sesionEnVivo(boolean v) { sesionEnVivo = v; return this; }
        /** Cuándo caduca el access token (ms de época), o null si no se puede leer. */
        public Estado caducaMs(@Nullable Long v) { caducaMs = v; return this; }
        /** La hora del reloj de pared, en ms de época. */
        public Estado ahoraMs(long v) { ahoraMs = v; return this; }
    }

    /**
     * El destino con lo que hay en el móvil, sin llamar a la API.
     *
     * @return un destino final, o {@link Destino#RENOVAR} si hay que preguntar antes.
     */
    @NonNull
    public static Destino decidir(@NonNull Estado e) {
        if (!e.sesion) return e.altaEmpezada ? Destino.ALTA : Destino.BIENVENIDA;
        if (!e.onboarding) return Destino.ALTA_CUENTA;
        if (!e.red) return e.sesionEnVivo ? Destino.SESION_EN_VIVO : Destino.SIN_CONEXION;
        if (e.caducaMs != null && e.caducaMs - MARGEN_CADUCIDAD_MS > e.ahoraMs) return Destino.INICIO;
        return e.refresh ? Destino.RENOVAR : Destino.ENTRAR;
    }

    /**
     * El destino tras renovar. Un servidor que no responde con una sesión en vivo
     * guardada abre la sesión: el entreno se guarda en el móvil y no depende de la API.
     */
    @NonNull
    public static Destino trasRenovar(@NonNull Renovacion r, boolean sesionEnVivo) {
        switch (r) {
            case OK: return Destino.INICIO;
            case NO_AUTORIZADO: return Destino.ENTRAR;
            case CUENTA_DESACTIVADA: return Destino.ENTRAR_DESACTIVADA;
            default: return sesionEnVivo ? Destino.SESION_EN_VIVO : Destino.NO_CONECTA;
        }
    }

    /**
     * Qué significa la respuesta de /auth/refresh.
     *
     * @param codigo el código HTTP, o -1 si no hubo respuesta (red, tiempo, cancelada).
     * @param cuerpo el cuerpo tal cual, o null.
     */
    @NonNull
    public static Renovacion clasificar(int codigo, @Nullable String cuerpo) {
        if (codigo >= 200 && codigo < 300) return Renovacion.OK;
        if (codigo == 401) {
            return cuerpo != null && cuerpo.contains(CUENTA_DESACTIVADA)
                    ? Renovacion.CUENTA_DESACTIVADA : Renovacion.NO_AUTORIZADO;
        }
        return Renovacion.SIN_RESPUESTA;
    }

    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    /**
     * Cuándo caduca un JWT, leído de su campo exp. No verifica la firma.
     *
     * @return ms de época, o null si el token no se puede leer.
     */
    @Nullable
    public static Long caducidadMs(@Nullable String jwt) {
        if (jwt == null) return null;
        String[] partes = jwt.split("\\.");
        if (partes.length < 2) return null;
        byte[] cuerpo = base64Url(partes[1]);
        if (cuerpo == null) return null;
        Matcher m = EXP.matcher(new String(cuerpo, StandardCharsets.UTF_8));
        if (!m.find()) return null;
        try {
            return Long.parseLong(m.group(1)) * 1000L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Base64url sin relleno, a mano: java.util.Base64 llega en Android 8 y la app arranca
    // desde Android 7 (minSdk 24); android.util.Base64 ataría la clase a Android.
    @Nullable
    private static byte[] base64Url(String s) {
        int n = s.length();
        if (n % 4 == 1) return null;
        byte[] salida = new byte[n * 3 / 4];
        int bits = 0, acumulado = 0, i = 0;
        for (int k = 0; k < n; k++) {
            int v = valor(s.charAt(k));
            if (v < 0) return null;
            acumulado = (acumulado << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                salida[i++] = (byte) (acumulado >> bits);
                acumulado &= (1 << bits) - 1;
            }
        }
        byte[] justo = new byte[i];
        System.arraycopy(salida, 0, justo, 0, i);
        return justo;
    }

    private static int valor(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '-') return 62;
        if (c == '_') return 63;
        return -1;
    }
}
