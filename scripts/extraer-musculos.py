# -*- coding: utf-8 -*-
"""
Fase 4 — la silueta muscular de Home.

Los 16 ic_body_*.xml son LA MISMA silueta (viewport 724x1448) repetida, cada una
con los paths de UN musculo recoloreados a @color/body_highlight. Sirven para un
icono de 40 dp en una fila de lista, pero no para pintar varios musculos a la vez
con intensidades distintas.

Este script los descompone en piezas apilables:

  · ic_silueta_frontal.xml / ic_silueta_dorsal.xml -> el cuerpo, sin ningun musculo.
  · ic_musculo_<nombre>.xml -> SOLO los paths de ese musculo, en blanco, para que
    la vista los tina en runtime segun cuantas series lo hayan tocado.

Los de 89 paths son la vista frontal y los de 70 la dorsal.

OJO con el grupo: los ficheros de la vista DORSAL llevan sus paths dentro de un
<group android:translateX="-724">, porque las coordenadas estan escritas en la
mitad derecha de un lienzo doble. Copiar los paths sin ese grupo deja el cuerpo
entero fuera del viewport y no se ve nada (pasó, y se vio en el emulador).
"""
import io, os, re

DRAWABLE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "GymProFit", "app", "src", "main", "res", "drawable")

CABECERA = (
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:width="120dp" android:height="240dp"\n'
    '    android:viewportWidth="724" android:viewportHeight="1448">\n'
)

COMENTARIO = (
    "<!--\n"
    "  {nombre} — generado a partir de {origen} (fase 4, silueta muscular de Home).\n"
    "  {descripcion}\n"
    "  NO editar a mano: se regenera con scripts/extraer-musculos.py.\n"
    "-->\n"
)

RE_PATH = re.compile(
    r'<path\s+android:fillColor="(?P<color>[^"]+)"\s+android:pathData="(?P<d>[^"]+)"\s*/>')
RE_GRUPO = re.compile(r'<group\s+android:translateX="(?P<tx>-?[0-9.]+)"\s*>')


def leer(fichero):
    with io.open(os.path.join(DRAWABLE, fichero), encoding="utf-8") as f:
        return f.read()


def escribir(nombre, comentario, paths, color, translate_x=None):
    """Escribe un vector con los paths dados, conservando el grupo de traslacion."""
    sangria = "    " if translate_x is not None else "  "
    cuerpo = "".join(
        '%s<path android:fillColor="%s" android:pathData="%s"/>\n' % (sangria, color, d)
        for d in paths
    )
    if translate_x is not None:
        cuerpo = ('  <group android:translateX="%s">\n' % translate_x) + cuerpo + "  </group>\n"

    texto = '<?xml version="1.0" encoding="utf-8"?>\n' + comentario + CABECERA + cuerpo + "</vector>\n"
    with io.open(os.path.join(DRAWABLE, nombre + ".xml"), "w", encoding="utf-8") as f:
        f.write(texto)
    return len(paths)


def partir(fichero):
    """Devuelve (todos_los_paths, paths_resaltados, translateX) de un ic_body_*.xml."""
    texto = leer(fichero)
    todos, resaltados = [], []
    for m in RE_PATH.finditer(texto):
        todos.append(m.group("d"))
        if "highlight" in m.group("color"):
            resaltados.append(m.group("d"))
    grupo = RE_GRUPO.search(texto)
    return todos, resaltados, (grupo.group("tx") if grupo else None)


# Nombre de musculo -> fichero de origen. La clave es la que usa el codigo Java.
MUSCULOS = {
    "abdominales":    "ic_body_abs.xml",
    "aductores":      "ic_body_adductors.xml",
    "biceps":         "ic_body_biceps.xml",
    "gemelos":        "ic_body_calves.xml",
    "pecho":          "ic_body_chest.xml",
    "antebrazos":     "ic_body_forearm.xml",
    "gluteos":        "ic_body_gluteal.xml",
    "isquiotibiales": "ic_body_hamstring.xml",
    "lumbares":       "ic_body_lowerback.xml",
    "cuello":         "ic_body_neck.xml",
    "cuadriceps":     "ic_body_quadriceps.xml",
    "hombros":        "ic_body_deltoids.xml",
    "trapecios":      "ic_body_trapezius.xml",
    "triceps":        "ic_body_triceps.xml",
    "dorsales":       "ic_body_upperback.xml",
}

# --- Las dos siluetas base -------------------------------------------------
frontal, _, tx_frontal = partir("ic_body_full.xml")
n = escribir("ic_silueta_frontal",
             COMENTARIO.format(nombre="ic_silueta_frontal", origen="ic_body_full.xml",
                               descripcion="Cuerpo de frente, sin ningun musculo resaltado."),
             frontal, "@color/body_silhouette", tx_frontal)
print("ic_silueta_frontal: %d paths (translateX=%s)" % (n, tx_frontal))

dorsal, _, tx_dorsal = partir("ic_body_upperback.xml")
n = escribir("ic_silueta_dorsal",
             COMENTARIO.format(nombre="ic_silueta_dorsal", origen="ic_body_upperback.xml",
                               descripcion="Cuerpo de espaldas, sin ningun musculo resaltado."),
             dorsal, "@color/body_silhouette", tx_dorsal)
print("ic_silueta_dorsal:  %d paths (translateX=%s)" % (n, tx_dorsal))

# --- Un fichero por musculo ------------------------------------------------
resumen = []
for clave, origen in sorted(MUSCULOS.items()):
    todos, resaltados, tx = partir(origen)
    if not resaltados:
        raise SystemExit("Sin paths resaltados en " + origen)
    vista = "frontal" if len(todos) == len(frontal) else "dorsal"
    nombre = "ic_musculo_" + clave
    escribir(nombre,
             COMENTARIO.format(nombre=nombre, origen=origen,
                               descripcion="Solo los paths del musculo, en blanco para tenirlos "
                                           "en runtime. Vista " + vista + "."),
             resaltados, "#FFFFFFFF", tx)
    resumen.append((clave, vista, len(resaltados), tx))
    print("%-26s %-8s %d paths (translateX=%s)" % (nombre, vista, len(resaltados), tx))

print()
print("frontales:", [c for c, v, _, _ in resumen if v == "frontal"])
print("dorsales: ", [c for c, v, _, _ in resumen if v == "dorsal"])
