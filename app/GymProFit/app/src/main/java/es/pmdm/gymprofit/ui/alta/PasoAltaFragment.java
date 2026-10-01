package es.pmdm.gymprofit.ui.alta;

import android.animation.AnimatorInflater;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// PasoAltaFragment — lo común a las preguntas del alta (GP-103).
//
// La cabecera (icono del capítulo, antetítulo, título y para qué sirve) con su entrada
// (momentos 3 y 4 de DEC-039), y «Siguiente»: apagado hasta responder (decisión 1 del
// lienzo), se enciende con su rebote la primera vez (momento 2) y avisa a la actividad.
//
// Las respuestas van al borrador al elegirlas, así que no hay nada que guardar al
// avanzar ni al volver.
// ============================================================
public abstract class PasoAltaFragment extends Fragment {

    protected PreferencesManager prefs;
    @Nullable private MaterialButton siguiente;
    // Si «Siguiente» ya estaba encendido: solo se anima al encenderse.
    private boolean encendido;

    /** La pregunta que es esta pantalla. */
    @NonNull
    protected abstract AltaPasos.Paso paso();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PreferencesManager(requireContext());
    }

    /** La actividad que aloja el cuestionario. */
    @NonNull
    protected Alta alta() {
        return (Alta) requireActivity();
    }

    /**
     * Pinta la cabecera y la hace entrar.
     *
     * @param icono  el icono relleno del capítulo, o 0 si la pregunta no lo abre.
     * @param titulo el título de la pregunta.
     * @param texto  para qué sirve.
     * @param resto  lo que entra detrás, en orden (opciones, botones…).
     */
    protected void cabecera(@NonNull View raiz, @DrawableRes int icono, @StringRes int titulo,
                            @StringRes int texto, @NonNull View... resto) {
        AltaPasos.Paso p = paso();
        View bloqueIcono = raiz.findViewById(R.id.iconoCapitulo);
        TextView antetitulo = raiz.findViewById(R.id.tvAntetitulo);
        TextView tvTitulo = raiz.findViewById(R.id.tvTituloPaso);
        TextView tvTexto = raiz.findViewById(R.id.tvTextoPaso);

        antetitulo.setText(getString(R.string.alta_antetitulo, getString(nombreCapitulo(p.capitulo)),
                p.capitulo, AltaPasos.CAPITULOS));
        tvTitulo.setText(titulo);
        tvTexto.setText(texto);

        if (icono != 0 && p.abreCapitulo) {
            bloqueIcono.setVisibility(View.VISIBLE);
            ((ImageView) bloqueIcono.findViewById(R.id.ivIconoCapitulo)).setImageResource(icono);
            Movimiento.abrirCapitulo(bloqueIcono.findViewById(R.id.circuloCapitulo),
                    bloqueIcono.findViewById(R.id.aroCapitulo));
        }

        // Con icono, el texto entra un poco más tarde, como en el lienzo (60 ms).
        long base = p.abreCapitulo ? 60 : 0;
        View[] todo = new View[3 + resto.length];
        todo[0] = antetitulo;
        todo[1] = tvTitulo;
        todo[2] = tvTexto;
        System.arraycopy(resto, 0, todo, 3, resto.length);
        Movimiento.entrar(base, todo);
    }

    /** El nombre del capítulo, con mayúscula, para el antetítulo. */
    @StringRes
    public static int nombreCapitulo(int capitulo) {
        switch (capitulo) {
            case 1:  return R.string.alta_capitulo_objetivo;
            case 2:  return R.string.alta_capitulo_sobre_ti;
            case 3:  return R.string.alta_capitulo_entrenamiento;
            default: return R.string.alta_capitulo_plan;
        }
    }

    /**
     * Prepara «Siguiente»: el toque, el clic y su estado según lo ya contestado.
     */
    protected void prepararSiguiente(@NonNull MaterialButton boton) {
        siguiente = boton;
        boton.setStateListAnimator(AnimatorInflater.loadStateListAnimator(requireContext(), R.animator.toque));
        boton.setOnClickListener(v -> {
            if (!AltaPasos.puedeSeguir(paso(), prefs.getBorradorRespuestas())) return;
            alta().siguiente();
        });
        encendido = AltaPasos.puedeSeguir(paso(), prefs.getBorradorRespuestas());
        boton.setEnabled(encendido);
    }

    /** Vuelve a mirar si se puede seguir; al encenderse, rebota (momento 2). */
    protected void refrescarSiguiente() {
        if (siguiente == null) return;
        boolean puede = AltaPasos.puedeSeguir(paso(), prefs.getBorradorRespuestas());
        siguiente.setEnabled(puede);
        if (puede && !encendido) Movimiento.encender(siguiente);
        encendido = puede;
    }
}
