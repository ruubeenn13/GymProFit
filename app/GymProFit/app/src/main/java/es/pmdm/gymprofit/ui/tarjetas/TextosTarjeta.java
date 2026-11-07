package es.pmdm.gymprofit.ui.tarjetas;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.PaginaHistorial;
import es.pmdm.gymprofit.model.sesion.SeriesMusculo;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.ui.widget.BarrasVecesView;
import es.pmdm.gymprofit.utils.CifraTarjeta;
import es.pmdm.gymprofit.utils.ComparacionSesion;
import es.pmdm.gymprofit.utils.CuandoTarjeta;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.FrasesTarjeta;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.Zonas;

// ============================================================
// TextosTarjeta — lo que dicen las tarjetas de sesión y de rutina, ya con sus colores (GP-232).
//
// Arma con FrasesTarjeta, ComparacionSesion y CuandoTarjeta (lógica pura, probada en la JVM) los
// textos de cada tarjeta y les pone lo que no se puede probar sin Android: el «kg» pequeño junto
// a la cifra, el récord en dorado de texto, la subida en verde con su flecha y la frase entera
// para TalkBack. Ningún color nuevo: gp_gold_text, gp_success y gp_text_secondary.
// Uno por pantalla: lee los recursos y el idioma una vez.
// ============================================================
public final class TextosTarjeta {

    private final Context ctx;
    private final Locale idioma;
    private final NumberFormat numeros;
    private final FrasesTarjeta.Textos frases;
    private final CuandoTarjeta.Textos cuando;
    private final CifraTarjeta.Textos cifras;
    private final String hoy;
    private final int oro;
    private final int verde;
    private final int neutro;

    public TextosTarjeta(@NonNull Context ctx) {
        this.ctx = ctx;
        this.idioma = FechaUtils.localeDeLaApp(ctx);
        this.numeros = NumberFormat.getIntegerInstance(idioma);
        this.frases = FrasesTarjeta.Textos.de(ctx);
        this.cuando = CuandoTarjeta.Textos.de(ctx);
        this.cifras = CifraTarjeta.Textos.de(ctx, idioma);
        this.hoy = TiempoRelativo.hoy();
        this.oro = ContextCompat.getColor(ctx, R.color.gp_gold_text);
        this.verde = ContextCompat.getColor(ctx, R.color.gp_success);
        this.neutro = ContextCompat.getColor(ctx, R.color.gp_text_secondary);
    }

    // ── Cifras ───────────────────────────────────────────────────────────────

    /** «13.640», en el idioma de la app. */
    @NonNull
    public String numero(double kilos) {
        return numeros.format(Math.round(kilos));
    }

    /** «13.640 kg» con el «kg» en Barlow pequeño y en el texto secundario, como el lienzo. */
    @NonNull
    public CharSequence kilos(double kilos, @DimenRes int tamanoKg) {
        String cifra = numero(kilos);
        SpannableString s = new SpannableString(ctx.getString(R.string.inicio_kilos_n, cifra));
        int desde = cifra.length();
        s.setSpan(new AbsoluteSizeSpan(ctx.getResources().getDimensionPixelSize(tamanoKg)), desde, s.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(neutro), desde, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Typeface barlow = androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.barlow);
        if (barlow != null) s.setSpan(new Fuente(barlow), desde, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    // ── La cifra grande (CifraTarjeta) ───────────────────────────────────────

    /** La de una sesión: kilos, series o el aviso. */
    @NonNull
    public CifraTarjeta cifra(@NonNull PaginaHistorial.Sesion s) {
        return CifraTarjeta.deSesion(s.getKilos(), s.getSeries(), s.getEjercicios());
    }

    /** «3 series» o «sin series hechas», dentro de una frase visible. */
    @NonNull
    public String cifraEnFrase(@NonNull CifraTarjeta c) {
        return c.enFrase(cifras);
    }

    /** «13.640 kilos», «3 series» o «Sin ejercicios apuntados», para TalkBack: nunca «0 kilos». */
    @NonNull
    public String cifraA11y(@NonNull CifraTarjeta c) {
        return c.a11y(cifras);
    }

    /**
     * Pinta la cifra en su sitio. Con kilos, los kilos (que cuentan al entrar) y las barras; con
     * series, «3 series» en la misma condensada y sin barras; si es un aviso, en el texto
     * secundario y sin barras.
     *
     * @param clave       la de EntradaTarjeta: lo que ya entró no se vuelve a animar.
     * @param estiloCifra la condensada de esa tarjeta.
     * @param veces       las barras, si las lleva (vacío en un entrenamiento libre).
     */
    public void pintarCifra(@NonNull TextView tv, @NonNull CifraTarjeta c, @NonNull EntradaTarjeta entrada,
                            @NonNull String clave, @androidx.annotation.StyleRes int estiloCifra,
                            @DimenRes int tamanoKg, @NonNull BarrasVecesView barras, @NonNull List<VezRutina> veces) {
        if (c.conKilos()) {
            tv.setTextAppearance(estiloCifra);
            barras.setVeces(veces);
            entrada.pintar(clave, tv, c.kilos, tamanoKg, this, barras);
            return;
        }
        barras.setVeces(new ArrayList<>());
        entrada.pintar(clave, null, 0, tamanoKg, this, barras);
        tv.setTextAppearance(c.esAviso() ? R.style.TextAppearance_GymProFit_Cuerpo : estiloCifra);
        if (c.esAviso()) tv.setTextColor(neutro);
        tv.setText(c.texto(cifras));
    }

    // ── Sesión ───────────────────────────────────────────────────────────────

    /** La rutina o «Entrenamiento libre». */
    @NonNull
    public String titulo(@NonNull PaginaHistorial.Sesion s) {
        String nombre = s.getRutinaNombre();
        return s.esLibre() || nombre == null || nombre.isEmpty()
                ? ctx.getString(R.string.sesiones_entrenamiento_libre) : nombre;
    }

    /** «Pecho, espalda y brazos», de más a menos series; "" si no se sabe ninguna. */
    @NonNull
    public String zonas(@NonNull List<SeriesMusculo> musculos) {
        return zonasEnFrase(FrasesTarjeta.zonas(musculos));
    }

    /** Las zonas ya elegidas (las del catálogo, en las rutinas del programa), en frase. */
    @NonNull
    public String zonasEnFrase(@NonNull List<Zonas.Zona> zonas) {
        List<String> nombres = new ArrayList<>();
        for (Zonas.Zona z : zonas) {
            if (nombres.size() < FrasesTarjeta.MAX_ZONAS) nombres.add(ctx.getString(z.nombre));
        }
        return FrasesTarjeta.frase(nombres, idioma, frases);
    }

    /** «Ayer, 18:05» o «Martes 3, 18:05», para el Historial completo. */
    @NonNull
    public String cuandoHistorial(@Nullable String iso) {
        return CuandoTarjeta.historial(iso, hoy, idioma, cuando);
    }

    /** «Ayer, martes», «Lunes» o «12 oct», para Inicio y Progreso. */
    @NonNull
    public String cuandoRelativo(@Nullable String iso) {
        return CuandoTarjeta.relativo(iso, hoy, idioma, cuando);
    }

    /** «ayer a las 18:05», para TalkBack. */
    @NonNull
    public String cuandoA11y(@Nullable String iso) {
        return CuandoTarjeta.a11y(iso, hoy, idioma, cuando);
    }

    /** «ESTA SEMANA» o «SEMANA DEL 19 OCT». */
    @NonNull
    public String semana(@Nullable String lunes) {
        return CuandoTarjeta.semana(lunes, hoy, idioma, cuando);
    }

    /** La cabecera en frase, para TalkBack: «Semana del 19 oct», «Week of Oct 19». */
    @NonNull
    public String semanaA11y(@Nullable String lunes) {
        return CuandoTarjeta.semanaEnFrase(lunes, hoy, idioma, cuando);
    }

    /** «2 entrenos y 22.050 kg», o «3 entrenos» si la semana no tuvo kilos. */
    @NonNull
    public String resumenSemana(int entrenos, double kilos) {
        return CifraTarjeta.semana(entrenos, kilos, cifras);
    }

    /** «2 entrenos y 22.050 kilos», o «3 entrenos», para TalkBack. */
    @NonNull
    public String resumenSemanaA11y(int entrenos, double kilos) {
        return CifraTarjeta.semanaA11y(entrenos, kilos, cifras);
    }

    /**
     * «58 minutos, 18 series y 1 récord», con el récord en dorado. Si la cifra no son kilos, las
     * series no se repiten: o ya son la cifra o no hubo.
     */
    @NonNull
    public CharSequence meta(@NonNull PaginaHistorial.Sesion s) {
        return conOro(FrasesTarjeta.meta(s.getDuracionMinutos(), s.getSeries(), s.getRecords(),
                !cifra(s).conKilos(), frases));
    }

    /** La misma línea, sin color, para TalkBack. */
    @NonNull
    public String metaA11y(@NonNull PaginaHistorial.Sesion s) {
        return FrasesTarjeta.meta(s.getDuracionMinutos(), s.getSeries(), s.getRecords(), !cifra(s).conKilos(),
                frases).texto;
    }

    /** Los minutos solos («25 minutos»), para la línea de un aviso en Inicio; "" si no se saben. */
    @NonNull
    public String minutos(@NonNull PaginaHistorial.Sesion s) {
        return FrasesTarjeta.meta(s.getDuracionMinutos(), 0, 0, true, frases).texto;
    }

    /** La comparación con la anterior de la misma rutina; null si no se compara (ni sin kilos). */
    @Nullable
    public ComparacionSesion comparacion(@NonNull PaginaHistorial.Sesion s) {
        if (!cifra(s).conKilos()) return null;
        return ComparacionSesion.de(s.esLibre(), s.getKilos(), s.getKilosAnterior());
    }

    /** «3 % más que la anterior», «Igual que la anterior» o «Primera vez con esta rutina». */
    @NonNull
    public String textoComparacion(@NonNull ComparacionSesion c) {
        switch (c.tipo) {
            case SUBE: return ctx.getString(R.string.tarjeta_comparacion_sube, c.porcentaje);
            case BAJA: return ctx.getString(R.string.tarjeta_comparacion_baja, c.porcentaje);
            case IGUAL: return ctx.getString(R.string.tarjeta_comparacion_igual);
            default: return ctx.getString(R.string.tarjeta_comparacion_primera);
        }
    }

    /**
     * Pinta la comparación en su línea: verde y flecha arriba si sube; neutra con la flecha abajo
     * si baja, con el signo de igual si no cambia y con la bandera la primera vez. Sin
     * comparación, la línea no se ve.
     */
    public void pintarComparacion(@NonNull TextView tv, @Nullable ComparacionSesion c) {
        if (c == null) {
            tv.setVisibility(android.view.View.GONE);
            return;
        }
        tv.setVisibility(android.view.View.VISIBLE);
        tv.setText(textoComparacion(c));
        int color = c.tipo == ComparacionSesion.Tipo.SUBE ? verde : neutro;
        int icono;
        switch (c.tipo) {
            case SUBE: icono = R.drawable.ic_ms_trending_up; break;
            case BAJA: icono = R.drawable.ic_ms_trending_down; break;
            case IGUAL: icono = R.drawable.ic_ms_trending_flat; break;
            default: icono = R.drawable.ic_ms_flag; break;
        }
        pintarConIcono(tv, icono, color);
    }

    /** «1 récord» con el trofeo, en dorado: la línea destacada de Inicio cuando hubo récords. */
    public void pintarRecords(@NonNull TextView tv, int records) {
        tv.setVisibility(android.view.View.VISIBLE);
        tv.setText(FrasesTarjeta.records(records, frases));
        pintarConIcono(tv, R.drawable.ic_ms_trophy_fill, oro);
    }

    /** Una línea neutra, sin icono (los minutos bajo un aviso); sin texto, no se ve. */
    public void pintarNeutra(@NonNull TextView tv, @NonNull String texto) {
        tv.setVisibility(texto.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        tv.setText(texto);
        tv.setTextColor(neutro);
        tv.setCompoundDrawablesRelative(null, null, null, null);
    }

    private void pintarConIcono(@NonNull TextView tv, int icono, int color) {
        tv.setTextColor(color);
        // El icono a 18 dp, como el lienzo: con el tamaño propio (24) descolocaba la línea.
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(ctx, icono);
        if (d != null) {
            d = d.mutate();
            int lado = Math.round(18 * ctx.getResources().getDisplayMetrics().density);
            d.setBounds(0, 0, lado, lado);
        }
        tv.setCompoundDrawablesRelative(d, null, null, null);
        TextViewCompat.setCompoundDrawableTintList(tv, ColorStateList.valueOf(color));
    }

    /**
     * «Tus 6 últimas veces con esta rutina, de 11.020 a 13.640 kilos», o «…, la primera y la
     * última con 1.475 kilos» si coinciden; null con menos de dos.
     */
    @Nullable
    public String barrasA11y(@NonNull List<VezRutina> veces) {
        if (veces.size() < 2) return null;
        List<VezRutina> v = es.pmdm.gymprofit.utils.BarrasVeces.recortar(veces);
        String primera = numero(v.get(0).getKilos()), ultima = numero(v.get(v.size() - 1).getKilos());
        int plural = primera.equals(ultima) ? R.plurals.tarjeta_barras_iguales_a11y : R.plurals.tarjeta_barras_a11y;
        return ctx.getResources().getQuantityString(plural, v.size(), v.size(), primera, ultima);
    }

    /**
     * Lo que TalkBack lee de una tarjeta de sesión, de una vez: «Pierna A, ayer a las 18:05.
     * Pierna y abdomen. 13.640 kilos, 3 % más que la anterior. 58 minutos, 18 series y 1 récord.
     * Tus 6 últimas veces con esta rutina, de 11.020 a 13.640 kilos.»
     */
    @NonNull
    public String sesionA11y(@NonNull PaginaHistorial.Sesion s) {
        StringBuilder b = new StringBuilder(titulo(s));
        String cuandoA11y = cuandoA11y(s.getFechaInicio());
        if (!cuandoA11y.isEmpty()) b.append(", ").append(cuandoA11y);
        b.append(". ");
        String z = zonas(s.getMusculos());
        if (!z.isEmpty()) b.append(z).append(". ");
        CifraTarjeta cifra = cifra(s);
        b.append(cifraA11y(cifra));
        ComparacionSesion c = comparacion(s);
        if (c != null) b.append(", ").append(Character.toLowerCase(textoComparacion(c).charAt(0)))
                .append(textoComparacion(c).substring(1));
        b.append(". ");
        String meta = metaA11y(s);
        if (!meta.isEmpty()) b.append(meta).append(". ");
        String barras = s.esLibre() || !cifra.conKilos() ? null : barrasA11y(s.getBarras());
        if (barras != null) b.append(barras).append('.');
        return b.toString().trim();
    }

    /** «Borrar Pierna A del 3 de noviembre» (GP-227). */
    @NonNull
    public String borrarA11y(@NonNull PaginaHistorial.Sesion s) {
        String dia = FechaUtils.fechaCorta(s.getFechaInicio(), idioma, ctx.getString(R.string.sesiones_eliminar_fecha_patron));
        return dia == null ? ctx.getString(R.string.sesiones_eliminar_a11y_sin_fecha, titulo(s))
                : ctx.getString(R.string.sesiones_eliminar_a11y, titulo(s), dia);
    }

    // ── Rutina ───────────────────────────────────────────────────────────────

    /** «7 ejercicios y 55 minutos». */
    @NonNull
    public String pauta(int ejercicios, int minutos) {
        return FrasesTarjeta.pauta(ejercicios, minutos, frases);
    }

    /** «La última vez, hace 6 días, con 2 récords» (el récord en dorado), o «Sin hacer todavía». */
    @NonNull
    public CharSequence ultimaVez(@NonNull List<VezRutina> ultimas) {
        if (ultimas.isEmpty()) return ctx.getString(R.string.tarjeta_sin_hacer);
        VezRutina u = ultimas.get(ultimas.size() - 1);
        String hace = TiempoRelativo.texto(ctx, u.getFecha());
        String antes = ctx.getString(R.string.tarjeta_ultima_vez, hace == null ? "" : hace);
        return conOro(FrasesTarjeta.conRecords(antes, ctx.getString(R.string.tarjeta_enlace_con),
                u.isRecord() ? 1 : 0, frases));
    }

    /** «La última vez, hace 6 días», sin récords: la de la hoja de elegir rutina. */
    @NonNull
    public String ultimaVezCorta(@NonNull List<VezRutina> ultimas) {
        if (ultimas.isEmpty()) return ctx.getString(R.string.tarjeta_sin_hacer);
        String hace = TiempoRelativo.texto(ctx, ultimas.get(ultimas.size() - 1).getFecha());
        return ctx.getString(R.string.tarjeta_ultima_vez, hace == null ? "" : hace);
    }

    /**
     * «La última vez, hace 6 días, 7.920 kilos» (o «…, 3 series», o «…, sin series hechas»), para
     * TalkBack; «Sin hacer todavía» si nunca.
     */
    @NonNull
    public String ultimaVezA11y(@NonNull List<VezRutina> ultimas) {
        if (ultimas.isEmpty()) return ctx.getString(R.string.tarjeta_sin_hacer);
        VezRutina u = ultimas.get(ultimas.size() - 1);
        String hace = TiempoRelativo.texto(ctx, u.getFecha());
        String cifra = CifraTarjeta.deVez(u).enFrase(cifras);
        String texto = ctx.getString(R.string.tarjeta_ultima_a11y, hace == null ? "" : hace, cifra);
        if (u.isRecord()) texto += ctx.getString(R.string.tarjeta_enlace_con) + FrasesTarjeta.records(1, frases);
        return texto;
    }

    // ── Piezas ───────────────────────────────────────────────────────────────

    /** El récord de una línea en dorado de texto y seminegrita. */
    @NonNull
    public CharSequence conOro(@NonNull FrasesTarjeta.Linea l) {
        if (l.inicioRecord < 0) return l.texto;
        SpannableString s = new SpannableString(l.texto);
        s.setSpan(new ForegroundColorSpan(oro), l.inicioRecord, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new StyleSpan(Typeface.BOLD), l.inicioRecord, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    // El «kg» en Barlow regular dentro de una cifra condensada (TypefaceSpan con Typeface es de la API 28).
    private static final class Fuente extends android.text.style.MetricAffectingSpan {
        private final Typeface tf;

        Fuente(Typeface tf) { this.tf = tf; }

        @Override
        public void updateDrawState(android.text.TextPaint p) { p.setTypeface(tf); }

        @Override
        public void updateMeasureState(@NonNull android.text.TextPaint p) { p.setTypeface(tf); }
    }

    /** El idioma de la app. */
    @NonNull
    public Locale idioma() {
        return idioma;
    }

    /** Los textos de las frases, para quien necesita armar una lista (Progreso, Programas). */
    @NonNull
    public FrasesTarjeta.Textos frases() {
        return frases;
    }
}
