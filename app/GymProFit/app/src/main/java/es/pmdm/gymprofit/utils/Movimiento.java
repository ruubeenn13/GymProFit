package es.pmdm.gymprofit.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// Movimiento — el sistema de movimiento de la app (GP-104, DEC-039).
//
// Los momentos del lienzo (documentacion/diseno/2026-09-30-alta/fuente/
// Movimiento.dc.html) salen de aquí: sus duraciones, sus cuatro curvas y sus cuatro
// vibraciones. Las pantallas no escriben un número de milisegundos ni una curva: piden
// el momento. Así lo que dice DEC-039 y lo que hace la app no se separan, y
// MovimientoTest lo comprueba leyendo los dos.
//
// Las reglas, las de DEC-039:
//   · Con «Quitar animaciones» del sistema todo aparece en su estado final. Lo hace
//     casi solo Android: con la escala de duración a 0, un ValueAnimator salta a su
//     final. Por eso aquí se anima SIEMPRE hacia el estado final, nunca desde él, y lo
//     que se repite en bucle (la demostración, el aviso de ejemplo) pregunta antes a
//     quieto() y, si toca, se queda quieto en su último fotograma.
//   · Las vibraciones se quedan con las animaciones quitadas: no son movimiento.
//   · Nada espera a una animación: ningún botón se apaga mientras algo se mueve.
//   · Solo lo que ya trae Android: animadores, interpoladores y vectores.
// ============================================================
public final class Movimiento {

    private Movimiento() {}

    // ── Curvas ──────────────────────────────────────────────────────────────

    /** cubic-bezier(.05,.7,.1,1): lo que entra en pantalla. Frena mucho al llegar. */
    public static final Interpolator ENFATIZADA = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    /** cubic-bezier(.2,0,0,1): lo que cambia de sitio o de tamaño (barra, botón). */
    public static final Interpolator ESTANDAR = new PathInterpolator(0.2f, 0f, 0f, 1f);
    /** cubic-bezier(.34,1.56,.64,1): lo que salta y se pasa un poco (check, píldora). */
    public static final Interpolator REBOTE = new PathInterpolator(0.34f, 1.56f, 0.64f, 1f);
    /** cubic-bezier(.36,.07,.19,.97): el temblor del error. */
    public static final Interpolator TIEMBLA = new PathInterpolator(0.36f, 0.07f, 0.19f, 0.97f);
    /** Lineal: lo que gira y los bucles con sus propios fotogramas. */
    public static final Interpolator LINEAL = new LinearInterpolator();

    // ── Duraciones (ms), por momento ────────────────────────────────────────
    // Los nombres son los de la tabla de DEC-039; MovimientoTest los cruza.

    /** 1 · Elegir: se hunde al tocar. */
    public static final long TOQUE = 120;
    /** 1 · Elegir: fondo y borde se rellenan. */
    public static final long RELLENO = 150;
    /** 1 · Elegir: el check salta. */
    public static final long CHECK = 260;
    /** 1 · Elegir: el icono gira y rebota. */
    public static final long ICONO = 420;
    /** 2 · «Siguiente» se enciende la primera vez. */
    public static final long ENCIENDE = 380;
    /** 3 · Entrar en pantalla: cada bloque sube 16 dp. */
    public static final long ENTRA = 350;
    /** 3 · Entrar en pantalla: retraso entre un bloque y el siguiente. */
    public static final long ENTRA_ESCALON = 40;
    /** 4 · Abrir capítulo: el icono aparece con rebote. */
    public static final long CAPITULO = 450;
    /** 4 · Abrir capítulo: cada latido del aro (dos). */
    public static final long LATIDO = 900;
    /** 5 · Barra de progreso: el tramo se llena. */
    public static final long BARRA = 500;
    /** 5 · Barra de progreso: destello al cerrar un capítulo. */
    public static final long DESTELLO = 700;
    /** 6 · Selector: la píldora viaja a la opción. */
    public static final long PILDORA = 320;
    /** 6 · Selector: cada día de la semana de ejemplo. */
    public static final long DIA = 280;
    /** 6 · Selector: retraso entre un día y el siguiente. */
    public static final long DIA_ESCALON = 35;
    /** 7 · Cifras que cuentan. */
    public static final long CIFRAS = 1200;
    /** 7 · Cifras: el halo al acabar. */
    public static final long HALO = 900;
    /** 8 · El programa llega: la tarjeta sube. */
    public static final long CARTA = 450;
    /** 8 · El programa llega: cada rutina entra. */
    public static final long FILA = 300;
    /** 8 · El programa llega: retraso entre rutinas. */
    public static final long FILA_ESCALON = 100;
    /** 8 · El programa llega: el brillo la cruza una vez. */
    public static final long BRILLO = 900;
    /** 9 · Cuenta creada: el botón se hace círculo. */
    public static final long CIRCULO = 300;
    /** 9 · Cuenta creada: una vuelta del giro. */
    public static final long GIRO = 700;
    /** 9 · Cuenta creada: el check se traza. */
    public static final long TRAZO = 420;
    /** 10 · Error: el campo tiembla. */
    public static final long TEMBLOR = 380;
    /** 10 · Error: el mensaje aparece debajo. */
    public static final long MENSAJE = 250;
    /** 11 · Aviso de ejemplo: una vuelta entera del bucle. */
    public static final long AVISO = 5000;
    /** 12 · Récord: el trofeo salta. */
    public static final long TROFEO = 450;
    /** 12 · Récord: las chispas salen. */
    public static final long CHISPAS = 700;
    /** 15 · El «+» se vuelve ✓: el botón se hunde a 0,94. */
    public static final long HUNDE = 70;
    /** 15 · El «+» se vuelve ✓: vuelve a su tamaño mientras se pone verde y el «+» se va. */
    public static final long VERDE = 120;
    /** 16 · La lista entra en cascada: cada fila sube y aparece. */
    public static final long CASCADA = 350;
    /** 16 · La lista entra en cascada: retraso entre filas. */
    public static final long CASCADA_ESCALON = 40;
    /** 17 · Código leído: una ida y vuelta de la línea que barre el marco. */
    public static final long BARRIDO = 2400;
    /** 17 · Código leído: el marco encaja. */
    public static final long ENCAJA = 200;
    /** 17 · Código leído: la hoja del producto sube. */
    public static final long HOJA = 450;
    /** 18 · La copia de ayer: la tarjeta «¿Copiar la de ayer?» se pliega (encoge a 0,96 y se apaga). */
    public static final long PLIEGA = 180;
    /** 18 · La copia de ayer: cada alimento copiado cae 10 dp y aparece... */
    public static final long CAE = 360;
    /** 18 · ...uno detrás de otro. */
    public static final long CAE_ESCALON = 120;
    /** 19 · El corazón late: se rellena y crece a 1,3, baja a 0,95 y vuelve. */
    public static final long CORAZON = 450;
    /** 19 · Un favorito nuevo entra en la lista con el fondo naranja suave, que se apaga. */
    public static final long RESALTA = 1400;
    /** 20 · Borrar y deshacer: al soltarla, la fila se va de lado. */
    public static final long SALE = 380;
    /** 20 · Borrar y deshacer: la comida se recoloca (y la fila vuelve con «Deshacer»). */
    public static final long RECOLOCA = 250;
    /** 23 · El plato: al cambiar las raciones, cada cifra de «Por ración» cae 6 dp. */
    public static final long CIFRA = 250;
    /** 23 · El plato: al guardar, el velo oscurece la pantalla detrás de la hoja. */
    public static final long VELO = 250;
    /** 23 · El plato: el ✓ de la hoja salta de 0,4 a 1,25 y a 1. */
    public static final long SALTA_OK = 450;
    /** 23 · El plato: el ✓ salta cuando la hoja ya está subiendo. */
    public static final long SALTA_OK_RETRASO = 200;
    /** 21 · Día completo: el anillo se cierra y se pone verde. */
    public static final long CIERRA = 600;
    /** 21 · Día completo: el punto del día en la tira salta a verde. */
    public static final long PUNTO = 450;
    /** 22 · Un vaso más: el vaso se llena de abajo arriba, con rebote. */
    public static final long VASO = 450;
    /** «Respira»: el botón principal llama la atención una vez. */
    public static final long RESPIRA = 1400;
    /** 24 · Las cifras de la cabecera suman al marcar una serie, frenando como CIFRAS. */
    public static final long SUMA = 600;
    /** 25 · La fila del récord pasa a dorado. */
    public static final long ORO = 900;
    /** 26 · El panel del descanso baja al acabar o saltarse. */
    public static final long BAJA = 250;
    /** — · Tarjeta a detalle: el contenedor crece hasta la pantalla (curva enfatizada). */
    public static final long CONTENEDOR_ABRE = 400;
    /** — · Tarjeta a detalle: al volver, el contenedor se encoge a su tarjeta (curva estándar). */
    public static final long CONTENEDOR_VUELVE = 350;
    /** — · Avanzar en un flujo: eje X compartido, curva estándar. */
    public static final long EJE_X = 300;
    /** — · Atrás predictivo en una hoja propia: al soltar, vuelve o se va en este tiempo (estándar). */
    public static final long ATRAS_SUELTA = 250;

    /** Distancia del eje X compartido, en dp. */
    public static final int EJE_X_DP = 30;
    /** Hasta dónde sigue la hoja al dedo en el atrás predictivo: el 90 % de su tamaño. */
    public static final float ATRAS_ESCALA = 0.9f;

    // ── Vibraciones ─────────────────────────────────────────────────────────

    /** Las cuatro vibraciones del lienzo. */
    public enum Vibracion { LIGERA, MEDIA, EXITO, ERROR }

    /**
     * Vibra con el patrón del sistema más parecido. Respeta el ajuste de «vibración al
     * tocar» del usuario, porque es performHapticFeedback y no el Vibrator.
     *
     * @param vista la vista que se ha tocado o que cambia.
     * @param tipo  ligera al elegir, media al abrir capítulo, éxito y error.
     */
    public static void vibrar(@Nullable View vista, @NonNull Vibracion tipo) {
        if (vista == null) return;
        int constante;
        switch (tipo) {
            case MEDIA:
                constante = HapticFeedbackConstants.CONTEXT_CLICK;
                break;
            case EXITO:
                constante = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS;
                break;
            case ERROR:
                constante = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS;
                break;
            default:
                constante = HapticFeedbackConstants.CLOCK_TICK;
        }
        vista.performHapticFeedback(constante);
    }

    // ── Animaciones quitadas ────────────────────────────────────────────────

    /**
     * Si el sistema tiene las animaciones quitadas («Quitar animaciones» de
     * Accesibilidad, o la escala de duración a 0 en Opciones de desarrollador).
     */
    public static boolean quieto(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return !ValueAnimator.areAnimatorsEnabled();
        }
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
    }

    // ── Momentos ────────────────────────────────────────────────────────────

    /**
     * 3 · Entrar en pantalla: cada vista sube 16 dp y aparece, una tras otra.
     * Se pueden tocar desde el primer fotograma.
     *
     * @param retraso retraso de la primera, en ms.
     * @param vistas  en el orden de lectura; las null se saltan.
     */
    public static void entrar(long retraso, @NonNull View... vistas) {
        long paso = 0;
        for (View v : vistas) {
            if (v == null) continue;
            entrarUna(v, retraso + paso, ENTRA, 16);
            paso += ENTRA_ESCALON;
        }
    }

    /** Una vista que sube {@code dp} y aparece en {@code duracion} ms tras {@code retraso}. */
    public static void entrarUna(@NonNull View v, long retraso, long duracion, float dp) {
        if (quieto(v.getContext())) {
            v.setAlpha(1f);
            v.setTranslationY(0f);
            return;
        }
        v.setAlpha(0f);
        v.setTranslationY(dp * v.getResources().getDisplayMetrics().density);
        v.animate().alpha(1f).translationY(0f).setStartDelay(retraso).setDuration(duracion)
                .setInterpolator(ENFATIZADA).start();
    }

    /** 1 · El check o el icono que salta: de 0,4 a 1 pasando por 1,18. */
    public static void saltar(@NonNull View v, long duracion) {
        v.setScaleX(0.4f);
        v.setScaleY(0.4f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(0).setDuration(duracion)
                .setInterpolator(REBOTE).start();
    }

    /** 1 · El icono elegido: gira -14° y rebota a su sitio. */
    public static void rebotarIcono(@NonNull View v) {
        v.setScaleX(0.6f);
        v.setScaleY(0.6f);
        v.setRotation(-14f);
        v.animate().scaleX(1f).scaleY(1f).rotation(0f).setStartDelay(0).setDuration(ICONO)
                .setInterpolator(REBOTE).start();
    }

    /** 2 · «Siguiente» se enciende: crece de 0,94 a 1 con rebote. */
    public static void encender(@NonNull View v) {
        v.setScaleX(0.94f);
        v.setScaleY(0.94f);
        v.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(ENCIENDE)
                .setInterpolator(REBOTE).start();
    }

    /**
     * 4 · Abrir capítulo: el icono salta y su aro late dos veces. Vibración media.
     *
     * @param icono el círculo del icono.
     * @param aro   el aro que late, del mismo tamaño y detrás; puede ser null.
     */
    public static void abrirCapitulo(@NonNull View icono, @Nullable View aro) {
        vibrar(icono, Vibracion.MEDIA);
        saltar(icono, CAPITULO);
        if (aro == null) return;
        aro.setAlpha(0f);
        if (quieto(aro.getContext())) return;
        latir(aro, 300, 2);
    }

    // El aro crece de 1 a 1,9 y se apaga de 0,7 a 0, tantas veces como se pida.
    private static void latir(@NonNull View aro, long retraso, int veces) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(LATIDO);
        a.setStartDelay(retraso);
        a.setRepeatCount(veces - 1);
        a.setInterpolator(new android.view.animation.DecelerateInterpolator());
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            aro.setScaleX(1f + 0.9f * t);
            aro.setScaleY(1f + 0.9f * t);
            aro.setAlpha(0.7f * (1f - t));
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { aro.setAlpha(0f); }
        });
        a.start();
    }

    /**
     * «Respira»: el botón principal crece un 3 % y vuelve, una vez, para decir «aquí».
     *
     * @param retraso cuándo, en ms.
     */
    public static void respirar(@NonNull View v, long retraso) {
        if (quieto(v.getContext())) return;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(RESPIRA);
        a.setStartDelay(retraso);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            // Sube hasta el 45 % del tiempo y baja; el lienzo lo hace con ease-in-out.
            float s = t < 0.45f ? t / 0.45f : (1f - t) / 0.55f;
            float e = (float) (0.5 - 0.5 * Math.cos(Math.PI * s));
            v.setScaleX(1f + 0.03f * e);
            v.setScaleY(1f + 0.03f * e);
        });
        a.start();
    }

    /**
     * 10 · Error: el campo tiembla 380 ms. Vibración de error.
     */
    public static void temblar(@NonNull View v) {
        vibrar(v, Vibracion.ERROR);
        if (quieto(v.getContext())) return;
        float d = v.getResources().getDisplayMetrics().density;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(TEMBLOR);
        a.setInterpolator(TIEMBLA);
        // Los fotogramas del lienzo: -7, 6, -4, 3 y 0 dp, cada uno en un 20 %.
        float[] x = {0f, -7f, 6f, -4f, 3f, 0f};
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue() * 5f;
            int i = Math.min(4, (int) t);
            float f = t - i;
            v.setTranslationX(d * (x[i] + (x[i + 1] - x[i]) * f));
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { v.setTranslationX(0f); }
        });
        a.start();
    }

    /** 10 · Error: el mensaje baja 4 dp y aparece. */
    public static void aparecerMensaje(@NonNull View v) {
        entrarUna(v, 0, MENSAJE, -4);
    }

    /**
     * Cuenta de 0 al valor con la curva del lienzo (1 − (1 − t)³), para las cifras.
     *
     * @param retraso cuándo empieza, en ms.
     * @param paso    recibe la fracción 0..1 ya con la curva aplicada.
     * @return el animador, ya en marcha; con las animaciones quitadas acaba en 1.
     */
    public static ValueAnimator contar(long retraso, long duracion, @NonNull Paso paso) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(duracion);
        a.setStartDelay(retraso);
        a.setInterpolator(LINEAL);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            paso.en((float) (1 - Math.pow(1 - t, 3)));
        });
        a.start();
        return a;
    }

    /** Lo que se hace en cada fotograma de una cuenta. */
    public interface Paso { void en(float fraccion); }

    /**
     * 8 y 12 · Un brillo que cruza la vista una vez, de izquierda a derecha, sin tocar
     * su diseño: se pinta en su capa superpuesta.
     *
     * @param color   color del centro del brillo, con su transparencia.
     * @param retraso cuándo, en ms.
     */
    public static void brillar(@NonNull View v, @ColorInt int color, long retraso, long duracion) {
        if (quieto(v.getContext())) return;
        GradientDrawable brillo = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{color & 0x00FFFFFF, color, color & 0x00FFFFFF});
        v.post(() -> {
            int ancho = Math.max(1, Math.round(v.getWidth() * 0.3f));
            int alto = v.getHeight();
            v.getOverlay().add(brillo);
            ValueAnimator a = ValueAnimator.ofFloat(-1.6f, 4.2f);
            a.setDuration(duracion);
            a.setStartDelay(retraso);
            a.setInterpolator(ESTANDAR);
            a.addUpdateListener(an -> {
                int x = Math.round((float) an.getAnimatedValue() * ancho);
                brillo.setBounds(x, 0, x + ancho, alto);
            });
            a.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) { v.getOverlay().remove(brillo); }
            });
            a.start();
        });
    }

    /**
     * 12 · Récord: el trofeo salta, suelta seis chispas doradas y un brillo cruza la
     * fila. Vibración de éxito.
     *
     * @param fila      la fila del récord, que recibe el brillo.
     * @param trofeo    el trofeo, que salta y del que salen las chispas.
     * @param oro       dorado de las chispas (gp_gold).
     * @param claro     el otro color de las chispas (on_primary_container).
     * @param retraso   cuándo, en ms.
     */
    public static void celebrarRecord(@NonNull ViewGroup fila, @NonNull View trofeo,
                                      @ColorInt int oro, @ColorInt int claro, long retraso) {
        celebrarRecord(fila, trofeo, oro, claro, retraso, true);
    }

    /**
     * 12 · Récord, con o sin vibración: la demostración de la bienvenida lo repite en
     * bucle y no puede vibrar cada 8 segundos.
     */
    public static void celebrarRecord(@NonNull ViewGroup fila, @NonNull View trofeo,
                                      @ColorInt int oro, @ColorInt int claro, long retraso, boolean conVibracion) {
        if (conVibracion) vibrar(fila, Vibracion.EXITO);
        if (quieto(fila.getContext())) return;

        trofeo.setScaleX(0.5f);
        trofeo.setScaleY(0.5f);
        trofeo.animate().scaleX(1f).scaleY(1f).setStartDelay(retraso).setDuration(TROFEO)
                .setInterpolator(REBOTE).start();

        float d = fila.getResources().getDisplayMetrics().density;
        fila.post(() -> {
            int[] enFila = posicionEn(fila, trofeo);
            float cx = enFila[0] + trofeo.getWidth() / 2f;
            float cy = enFila[1] + trofeo.getHeight() / 2f;
            int lado = Math.round(6 * d);
            Drawable[] chispas = new Drawable[6];
            for (int i = 0; i < 6; i++) {
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(i % 2 == 0 ? oro : claro);
                g.setAlpha(0);
                chispas[i] = g;
                fila.getOverlay().add(g);
            }
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(CHISPAS);
            a.setStartDelay(retraso + 60);
            a.setInterpolator(ESTANDAR);
            a.addUpdateListener(an -> {
                float t = (float) an.getAnimatedValue();
                float r = (4f + 26f * t) * d;
                float escala = t < 0.1f ? 0.3f + 7f * t : 1f - 0.6f * t;
                int alfa = Math.round(255 * (t < 0.1f ? t * 10f : (1f - t) / 0.9f));
                int mitad = Math.max(1, Math.round(lado * escala / 2f));
                for (int i = 0; i < 6; i++) {
                    double ang = Math.toRadians(60 * i);
                    int x = Math.round((float) (cx + r * Math.cos(ang)));
                    int y = Math.round((float) (cy + r * Math.sin(ang)));
                    chispas[i].setBounds(x - mitad, y - mitad, x + mitad, y + mitad);
                    chispas[i].setAlpha(Math.max(0, Math.min(255, alfa)));
                }
            });
            a.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    for (Drawable c : chispas) fila.getOverlay().remove(c);
                }
            });
            a.start();
        });
        brillar(fila, 0x4DFFECBE, retraso + 150, BRILLO);
    }

    /**
     * 15 · El «+» se vuelve ✓ (o el ✓, «+»): el botón se hunde a 0,94, vuelve mientras su
     * fondo pasa al color nuevo, y el icono nuevo salta con rebote. Vibración ligera.
     * Con las animaciones quitadas, todo en su estado final (el fondo ya va puesto).
     *
     * @param boton      el botón redondo.
     * @param icono      el icono que acaba de cambiar.
     * @param fondoAntes el color de fondo de antes.
     * @param fondoAhora el color de fondo nuevo.
     */
    public static void volverMas(@NonNull View boton, @NonNull View icono, @ColorInt int fondoAntes,
                                 @ColorInt int fondoAhora) {
        vibrar(boton, Vibracion.LIGERA);
        boton.animate().cancel();
        icono.animate().cancel();
        if (quieto(boton.getContext())) {
            boton.setScaleX(1f);
            boton.setScaleY(1f);
            icono.setScaleX(1f);
            icono.setScaleY(1f);
            icono.setAlpha(1f);
            return;
        }
        boton.animate().scaleX(0.94f).scaleY(0.94f).setStartDelay(0).setDuration(HUNDE).setInterpolator(ESTANDAR)
                .withEndAction(() -> boton.animate().scaleX(1f).scaleY(1f).setDuration(VERDE)
                        .setInterpolator(ESTANDAR).start())
                .start();
        ValueAnimator color = ValueAnimator.ofArgb(fondoAntes, fondoAhora);
        color.setStartDelay(HUNDE);
        color.setDuration(VERDE);
        color.setInterpolator(ESTANDAR);
        color.addUpdateListener(a -> boton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf((int) a.getAnimatedValue())));
        boton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fondoAntes));
        color.start();
        icono.setScaleX(0f);
        icono.setScaleY(0f);
        icono.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(HUNDE).setDuration(CHECK)
                .setInterpolator(REBOTE).start();
    }

    /**
     * 19 · El corazón late al marcar un favorito: crece a 1,3, baja a 0,95 y vuelve.
     * Vibración ligera. Al quitarlo no se llama: se vacía sin latido.
     */
    public static void latirCorazon(@NonNull View corazon) {
        vibrar(corazon, Vibracion.LIGERA);
        corazon.animate().cancel();
        corazon.setScaleX(1f);
        corazon.setScaleY(1f);
        if (quieto(corazon.getContext())) return;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(CORAZON);
        a.setInterpolator(REBOTE);
        // Los fotogramas del tablero de la ficha: 1 → 1,3 (30 %) → 0,95 (60 %) → 1.
        a.addUpdateListener(an -> {
            float t = Math.max(0f, Math.min(1f, (float) an.getAnimatedValue()));
            float s;
            if (t < 0.3f) s = 1f + 0.3f * (t / 0.3f);
            else if (t < 0.6f) s = 1.3f - 0.35f * ((t - 0.3f) / 0.3f);
            else s = 0.95f + 0.05f * ((t - 0.6f) / 0.4f);
            corazon.setScaleX(s);
            corazon.setScaleY(s);
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                corazon.setScaleX(1f);
                corazon.setScaleY(1f);
            }
        });
        a.start();
    }

    /**
     * 18 · La tarjeta «¿Copiar la de ayer?» se pliega: encoge a 0,96 y se apaga, y después
     * deja de ocupar sitio. {@code alFinal} se hace también con «Quitar animaciones».
     */
    public static void plegar(@NonNull View tarjeta, @Nullable Runnable alFinal) {
        tarjeta.animate().cancel();
        if (quieto(tarjeta.getContext())) {
            tarjeta.setVisibility(View.GONE);
            if (alFinal != null) alFinal.run();
            return;
        }
        tarjeta.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(PLIEGA).setInterpolator(ESTANDAR)
                .withEndAction(() -> {
                    tarjeta.setVisibility(View.GONE);
                    tarjeta.setAlpha(1f);
                    tarjeta.setScaleX(1f);
                    tarjeta.setScaleY(1f);
                    if (alFinal != null) alFinal.run();
                }).start();
    }

    /**
     * 18 · Un alimento copiado cae en su comida: baja 10 dp y aparece, {@code orden} veces
     * CAE_ESCALON después del primero.
     */
    public static void caer(@NonNull View fila, int orden) {
        if (quieto(fila.getContext())) return;
        float d = fila.getResources().getDisplayMetrics().density;
        fila.animate().cancel();
        fila.setAlpha(0f);
        fila.setTranslationY(-10 * d);
        fila.animate().alpha(1f).translationY(0f).setStartDelay(orden * CAE_ESCALON).setDuration(CAE)
                .setInterpolator(ENFATIZADA).start();
    }

    /**
     * 19 · Un favorito recién aceptado entra en la lista: baja 12 dp y aparece con el
     * fondo {@code color} encima, que se apaga hasta el final.
     */
    public static void resaltar(@NonNull View fila, @ColorInt int color) {
        if (quieto(fila.getContext())) return;
        float d = fila.getResources().getDisplayMetrics().density;
        android.graphics.drawable.ColorDrawable fondo = new android.graphics.drawable.ColorDrawable(color);
        fila.post(() -> {
            fondo.setBounds(0, 0, fila.getWidth(), fila.getHeight());
            fila.getOverlay().add(fondo);
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(RESALTA);
            a.setInterpolator(LINEAL);
            a.addUpdateListener(an -> {
                float t = (float) an.getAnimatedValue();
                // Hasta el 40 %, entra; después, el fondo se apaga.
                float e = ENFATIZADA.getInterpolation(Math.min(1f, t / 0.4f));
                fila.setAlpha(e);
                fila.setTranslationY(-12 * d * (1f - e));
                fondo.setAlpha(t < 0.4f ? 255 : Math.round(255 * (1f - (t - 0.4f) / 0.6f)));
            });
            a.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    fila.getOverlay().remove(fondo);
                    fila.setAlpha(1f);
                    fila.setTranslationY(0f);
                }
            });
            a.start();
        });
    }

    /**
     * 23 · Una cifra de «Por ración» cambia: cae 6 dp desde 0,3 de opacidad hasta su sitio.
     * Con «Quitar animaciones», ya está.
     */
    public static void caerCifra(@NonNull View cifra) {
        cifra.animate().cancel();
        cifra.setAlpha(1f);
        cifra.setTranslationY(0f);
        if (quieto(cifra.getContext())) return;
        float d = cifra.getResources().getDisplayMetrics().density;
        cifra.setAlpha(0.3f);
        cifra.setTranslationY(-6 * d);
        cifra.animate().alpha(1f).translationY(0f).setDuration(CIFRA).setInterpolator(ESTANDAR).start();
    }

    /**
     * 23 · El plato guardado: el velo se oscurece (VELO), la hoja sube desde abajo (HOJA) y
     * su ✓ salta de 0,4 a 1,25 y a 1 (SALTA_OK), SALTA_OK_RETRASO después. Vibra con éxito.
     * Todo se puede tocar desde el primer fotograma.
     */
    public static void subirHojaPlato(@NonNull View velo, @NonNull View hoja, @NonNull View check) {
        vibrar(hoja, Vibracion.EXITO);
        velo.setVisibility(View.VISIBLE);
        hoja.setVisibility(View.VISIBLE);
        velo.animate().cancel();
        hoja.animate().cancel();
        check.animate().cancel();
        if (quieto(hoja.getContext())) {
            velo.setAlpha(1f);
            hoja.setAlpha(1f);
            hoja.setTranslationY(0f);
            check.setScaleX(1f);
            check.setScaleY(1f);
            return;
        }
        float d = hoja.getResources().getDisplayMetrics().density;
        velo.setAlpha(0f);
        velo.animate().alpha(1f).setDuration(VELO).setInterpolator(ESTANDAR).start();
        hoja.setAlpha(0f);
        hoja.setTranslationY(60 * d);
        hoja.animate().alpha(1f).translationY(0f).setDuration(HOJA).setInterpolator(ENFATIZADA).start();
        check.setScaleX(0.4f);
        check.setScaleY(0.4f);
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(SALTA_OK);
        a.setStartDelay(SALTA_OK_RETRASO);
        a.setInterpolator(LINEAL);
        // Como el tablero: de 0,4 a 1,25 hasta el 55 % y de ahí a 1, cada tramo con el rebote.
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            float s = t < 0.55f ? 0.4f + 0.85f * REBOTE.getInterpolation(t / 0.55f)
                    : 1.25f - 0.25f * REBOTE.getInterpolation((t - 0.55f) / 0.45f);
            check.setScaleX(s);
            check.setScaleY(s);
        });
        a.start();
    }

    // ── Sesión en vivo y descanso (GP-210 paso 3, momentos 24 a 26) ─────────

    /**
     * 24 · Serie marcada: el botón se rellena del color nuevo (RELLENO) y su check salta
     * (CHECK). Vibración ligera, salvo que la serie sea un récord y vibre con éxito. Con
     * las animaciones quitadas, el botón ya queda del color final.
     *
     * @param boton        el botón redondo de marcar, teñido con su fondo.
     * @param antes        el color de fondo de antes.
     * @param ahora        el color de fondo nuevo.
     * @param conVibracion si vibra: no cuando el récord ya vibra por su cuenta.
     */
    public static void marcarSerie(@NonNull View boton, @ColorInt int antes, @ColorInt int ahora, boolean conVibracion) {
        if (conVibracion) vibrar(boton, Vibracion.LIGERA);
        boton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ahora));
        if (quieto(boton.getContext())) return;
        ValueAnimator a = ValueAnimator.ofArgb(antes, ahora);
        a.setDuration(RELLENO);
        a.setInterpolator(ESTANDAR);
        a.addUpdateListener(an -> boton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf((int) an.getAnimatedValue())));
        a.start();
        boton.setScaleX(0.4f);
        boton.setScaleY(0.4f);
        boton.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(CHECK)
                .setInterpolator(REBOTE).start();
    }

    /**
     * 24 · Una cifra de la cabecera que suma: cuenta de la de antes a la nueva con la curva
     * de las cifras (1 − (1 − t)³) en SUMA ms. Con las animaciones quitadas salta a la nueva.
     *
     * @param paso recibe el valor intermedio ya interpolado.
     */
    public static ValueAnimator sumar(double desde, double hasta, @NonNull ValorPaso paso) {
        return contar(0, SUMA, f -> paso.en(desde + (hasta - desde) * f));
    }

    /** Lo que se hace en cada fotograma de una cifra que suma. */
    public interface ValorPaso { void en(double valor); }

    /**
     * 24 · La barra de la sesión avanza (o retrocede, si se desmarca) de {@code desde} a
     * {@code hasta} en BARRA ms con la curva estándar. Con las animaciones quitadas, salta a
     * {@code hasta}.
     */
    public static void llenarBarra(@NonNull com.google.android.material.progressindicator.BaseProgressIndicator<?> barra,
                                   int desde, int hasta) {
        if (quieto(barra.getContext()) || desde == hasta) {
            barra.setProgress(hasta);
            return;
        }
        ValueAnimator a = ValueAnimator.ofInt(desde, hasta);
        a.setDuration(BARRA);
        a.setInterpolator(ESTANDAR);
        a.addUpdateListener(an -> barra.setProgress((int) an.getAnimatedValue()));
        a.start();
    }

    /**
     * 25 · La fila del récord pasa a dorado: un destello a {@code pico} al 25 % y se asienta
     * en {@code fin} (los colores del lienzo, #3A2E12 y #2A2414), en ORO ms con la curva
     * estándar. Con las animaciones quitadas queda en {@code fin}.
     */
    public static void dorarFila(@NonNull View fila, @ColorInt int desde, @ColorInt int pico, @ColorInt int fin) {
        fila.setBackgroundColor(fin);
        if (quieto(fila.getContext())) return;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(ORO);
        a.setInterpolator(LINEAL);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            // La curva estándar a cada tramo: del fondo al destello hasta el 25 %, y de ahí al fin.
            int color = t < 0.25f
                    ? mezclar(desde, pico, ESTANDAR.getInterpolation(t / 0.25f))
                    : mezclar(pico, fin, ESTANDAR.getInterpolation((t - 0.25f) / 0.75f));
            fila.setBackgroundColor(color);
        });
        a.start();
    }

    private static int mezclar(@ColorInt int a, @ColorInt int b, float f) {
        return android.graphics.Color.argb(
                Math.round(android.graphics.Color.alpha(a) + (android.graphics.Color.alpha(b) - android.graphics.Color.alpha(a)) * f),
                Math.round(android.graphics.Color.red(a) + (android.graphics.Color.red(b) - android.graphics.Color.red(a)) * f),
                Math.round(android.graphics.Color.green(a) + (android.graphics.Color.green(b) - android.graphics.Color.green(a)) * f),
                Math.round(android.graphics.Color.blue(a) + (android.graphics.Color.blue(b) - android.graphics.Color.blue(a)) * f));
    }

    /**
     * 25 · El aviso del récord baja desde 8 dp y aparece (ENTRA, enfatizada). Se puede tocar
     * desde el primer fotograma.
     */
    public static void bajarAviso(@NonNull View aviso) {
        entrarUna(aviso, 0, ENTRA, -8);
    }

    /**
     * 26 · El panel del descanso sube desde abajo (HOJA, enfatizada). No hay velo: la lista
     * sigue debajo y se puede tocar.
     */
    public static void subirPanel(@NonNull View panel) {
        panel.animate().cancel();
        panel.setAlpha(1f);
        if (quieto(panel.getContext())) {
            panel.setTranslationY(0f);
            return;
        }
        panel.setTranslationY(panel.getHeight() > 0 ? panel.getHeight() : 120 * panel.getResources().getDisplayMetrics().density);
        panel.animate().translationY(0f).setStartDelay(0).setDuration(HOJA).setInterpolator(ENFATIZADA).start();
    }

    /**
     * 26 · El panel del descanso baja (BAJA, estándar) y, al acabar, se esconde y avisa.
     * Con las animaciones quitadas se esconde al momento.
     *
     * @param alFinal lo que pasa cuando ya no se ve (puede ser null).
     */
    public static void bajarPanel(@NonNull View panel, @Nullable Runnable alFinal) {
        panel.animate().cancel();
        if (quieto(panel.getContext())) {
            panel.setVisibility(View.GONE);
            if (alFinal != null) alFinal.run();
            return;
        }
        panel.animate().translationY(panel.getHeight()).setStartDelay(0).setDuration(BAJA)
                .setInterpolator(ESTANDAR).withEndAction(() -> {
                    panel.setVisibility(View.GONE);
                    panel.setTranslationY(0f);
                    if (alFinal != null) alFinal.run();
                }).start();
    }

    /** 26 · A falta de 10 s el número late una vez: crece a 1,08 al 40 % y vuelve (LATIDO). */
    public static void latirNumero(@NonNull View numero) {
        if (quieto(numero.getContext())) return;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(LATIDO);
        a.setInterpolator(ESTANDAR);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            float s = t < 0.4f ? 1f + 0.08f * (t / 0.4f) : 1.08f - 0.08f * ((t - 0.4f) / 0.6f);
            numero.setScaleX(s);
            numero.setScaleY(s);
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                numero.setScaleX(1f);
                numero.setScaleY(1f);
            }
        });
        a.start();
    }

    // Esquina de {@code hija} medida desde {@code padre}, que la contiene.
    private static int[] posicionEn(@NonNull View padre, @NonNull View hija) {
        int[] p = new int[2];
        int[] h = new int[2];
        padre.getLocationInWindow(p);
        hija.getLocationInWindow(h);
        return new int[]{h[0] - p[0], h[1] - p[1]};
    }
}
