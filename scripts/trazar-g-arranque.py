"""Traza la G del arranque desde drawable-night/logo.png y mide su fidelidad (GP-223).

Dos órdenes, desde la raíz del repo:

  python scripts/trazar-g-arranque.py trazar  app/GymProFit/app/src/main/res/drawable-night/logo.png app/GymProFit/app/src/main/res
      Separa el cuerpo dorado y el trazo naranja por color y componente conexa, suaviza
      el borde (el PNG tiene ruido de compresión), sube cada máscara a x4, la traza con
      potrace y escribe drawable/g_arranque.xml (último fotograma) y
      drawable/g_arranque_inicial.xml (primero, base de la animada). Los colores son la
      mediana del interior de cada pieza: van a values/colors.xml (logo_g_cuerpo y
      logo_g_trazo) a mano, si cambian.

  python scripts/trazar-g-arranque.py comparar app/GymProFit/app/src/main/res/drawable-night/logo.png app/GymProFit/app/src/main/res/drawable/g_arranque.xml [diferencias.png]
      Rasteriza los dos trazados del vector a 512 px (supermuestreo x8, par-impar), los
      compone sobre el fondo del splash (#0B0C0E) igual que el PNG y cuenta los píxeles
      cuya diferencia máxima por canal pasa de cada umbral.

Necesita Pillow, numpy y scikit-image; «trazar», además, potracer (port de potrace en
Python). Se instalaron en un entorno virtual temporal, fuera del repo:
  python -m venv <tmp> && <tmp>/Scripts/python -m pip install potracer pillow numpy scikit-image

Parámetros elegidos (informe de GP-223): x4, opttolerance 1,0 y suavizado de 1,2 px:
2 926 píxeles con diferencia > 16 de 262 144 (1,12 %), todos en el borde, y un vector
de 13,9 KB. Si cambia el logo se vuelve a ejecutar; los vectores no se retocan a mano.
"""
import json
import re
import sys

import numpy as np
from PIL import Image, ImageDraw


ESCALA = 4
OPT = 1.0
# Suavizado del borde antes de umbralizar, en píxeles del PNG: el PNG tiene el borde
# con ruido de compresión y potrace lo seguiría curva a curva.
SIGMA = 1.2


def mascara_pieza(im, es_pieza):
    a = im[..., 3] / 255.0
    from skimage import measure, morphology
    etiquetas = measure.label(es_pieza & (a > 0.5), connectivity=2)
    regiones = sorted(measure.regionprops(etiquetas), key=lambda r: -r.area)
    principal = etiquetas == regiones[0].label
    # El borde suavizado de la pieza: los píxeles parciales que tocan la componente.
    vecindad = morphology.binary_dilation(principal, morphology.disk(3))
    cobertura = np.where(vecindad & es_pieza, a, 0.0)
    cobertura = np.where(principal, np.maximum(cobertura, a), cobertura)
    return principal, cobertura


def color_interior(im, principal):
    from skimage import morphology
    interior = morphology.binary_erosion(principal, morphology.disk(4))
    px = im[interior][:, :3]
    med = np.median(px, axis=0)
    return '#%02X%02X%02X' % tuple(int(round(x)) for x in med), int(interior.sum())


def trazar(cobertura):
    alta = Image.fromarray((cobertura * 255).astype(np.uint8)).resize(
        (cobertura.shape[1] * ESCALA, cobertura.shape[0] * ESCALA), Image.BICUBIC)
    if SIGMA > 0:
        from PIL import ImageFilter
        alta = alta.filter(ImageFilter.GaussianBlur(SIGMA * ESCALA))
    bits = np.array(alta) >= 128
    import potrace
    bm = potrace.Bitmap(~bits)  # potracer toma False como relleno
    plist = bm.trace(turdsize=16, turnpolicy=potrace.POTRACE_TURNPOLICY_MINORITY,
                     alphamax=1.0, opticurve=True, opttolerance=OPT)
    partes = []
    f = 1.0 / ESCALA
    for curva in plist:
        sp = curva.start_point
        d = ['M%.2f,%.2f' % (sp.x * f, sp.y * f)]
        for seg in curva.segments:
            if seg.is_corner:
                d.append('L%.2f,%.2f' % (seg.c.x * f, seg.c.y * f))
                d.append('L%.2f,%.2f' % (seg.end_point.x * f, seg.end_point.y * f))
            else:
                d.append('C%.2f,%.2f %.2f,%.2f %.2f,%.2f' % (
                    seg.c1.x * f, seg.c1.y * f, seg.c2.x * f, seg.c2.y * f,
                    seg.end_point.x * f, seg.end_point.y * f))
        d.append('Z')
        partes.append(''.join(d))
    return ''.join(partes), len(plist)


def trazar_logo(ruta_png):
    from skimage import measure, morphology  # noqa: F401 (las usan las funciones de arriba)
    im = np.array(Image.open(ruta_png).convert('RGBA')).astype(float)
    r, g = im[..., 0], im[..., 1]
    naranja = (g < 150) & (r > 150)
    oro = g >= 150
    salida = {}
    for nombre, es in (('cuerpo', oro), ('trazo', naranja)):
        principal, cobertura = mascara_pieza(im, es)
        color, n = color_interior(im, principal)
        d, curvas = trazar(cobertura)
        ys, xs = np.where(principal)
        salida[nombre] = {'color': color, 'pathData': d, 'subtrazados': curvas,
                          'interior_px': n, 'bbox': [int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())]}
        print(nombre, color, 'subtrazados', curvas, 'bbox', salida[nombre]['bbox'], 'longitud d', len(d))
    return salida

LOGO = 512.0          # el PNG de origen: el logo entero
G_DP = 160.0          # la G mide 160 dp
CAJA_DP = 288.0       # el icono del splash sin fondo: 288 dp, con el círculo de 192
ESCALA_LOGO = G_DP / LOGO  # 0.3125
DESPLAZA = (CAJA_DP - G_DP) / 2  # 64
TRAZO_DESDE = 18.0 * LOGO / G_DP  # 18 dp sobre una G de 160 dp, en unidades del logo
PIVOTE_TRAZO = (0.749 * LOGO, 0.20 * LOGO)

CABECERA = '''<?xml version="1.0" encoding="utf-8"?>
<!--
  {nombre} — la G del logo para el arranque (GP-223). {que}
  Trazada con potrace (potracer) de drawable-night/logo.png: dos trazados, el cuerpo
  dorado y el trazo naranja, con los colores del PNG (logo_g_cuerpo y logo_g_trazo).
  Fidelidad: documentacion/estado/2026-11-04-gp-223-arranque.md. Si cambia el logo,
  se vuelve a trazar con el script del informe; no se retoca a mano.

  288 x 288 dp, el icono sin fondo del splash de Android 12: la G mide 160 dp y queda
  dentro del círculo de 192. Los trazados van en las coordenadas del PNG (0..512) y el
  grupo «logo» los lleva a su sitio.
-->
'''

PLANTILLA = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="288dp"
    android:height="288dp"
    android:viewportWidth="288"
    android:viewportHeight="288">
    <group
        android:name="logo"
        android:translateX="{desplaza:g}"
        android:translateY="{desplaza:g}"
        android:scaleX="{escala:g}"
        android:scaleY="{escala:g}">
        <group
            android:name="giro_cuerpo"
            android:pivotX="256"
            android:pivotY="256"
            android:rotation="{rotacion}"
            android:scaleX="{escala_cuerpo}"
            android:scaleY="{escala_cuerpo}">
            <path
                android:name="cuerpo"
                android:fillColor="@color/logo_g_cuerpo"
                android:fillAlpha="{alfa}"
                android:pathData="{cuerpo}" />
        </group>
        <group
            android:name="vuelo_trazo"
            android:pivotX="{px:g}"
            android:pivotY="{py:g}"
            android:translateX="{tx}"
            android:translateY="{ty}"
            android:scaleX="{escala_trazo}"
            android:scaleY="{escala_trazo}">
            <path
                android:name="trazo"
                android:fillColor="@color/logo_g_trazo"
                android:fillAlpha="{alfa}"
                android:pathData="{trazo}" />
        </group>
    </group>
</vector>
'''


def escribir(ruta, nombre, que, inicial, g):
    valores = dict(desplaza=DESPLAZA, escala=ESCALA_LOGO, px=round(PIVOTE_TRAZO[0], 2), py=round(PIVOTE_TRAZO[1], 2),
                   cuerpo=g['cuerpo']['pathData'], trazo=g['trazo']['pathData'])
    if inicial:
        valores.update(rotacion='-90', escala_cuerpo='0.72', alfa='0',
                       tx='%g' % TRAZO_DESDE, ty='%g' % -TRAZO_DESDE, escala_trazo='0.6')
    else:
        valores.update(rotacion='0', escala_cuerpo='1', alfa='1', tx='0', ty='0', escala_trazo='1')
    with open(ruta, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(CABECERA.format(nombre=nombre, que=que))
        fh.write(PLANTILLA.format(**valores))


def escribir_vectores(g, res):
    escribir(res + '/drawable/g_arranque.xml', 'g_arranque',
             'El último fotograma, quieto: el splash de Android 11 o menos y la pantalla de carga.', False, g)
    escribir(res + '/drawable/g_arranque_inicial.xml', 'g_arranque_inicial',
             'El primer fotograma (invisible): la base de g_arranque_animada.', True, g)
    print('colores', g['cuerpo']['color'], g['trazo']['color'])



SS = 8
FONDO = np.array([0x0B, 0x0C, 0x0E], dtype=float)
TOKEN = re.compile(r'[MLCZmlcz]|-?\d*\.?\d+(?:e-?\d+)?')


def aplanar(d, pasos=24):
    polis, actual, pos, ini = [], [], (0.0, 0.0), (0.0, 0.0)
    t = TOKEN.findall(d)
    i, cmd = 0, None
    def num():
        nonlocal i
        v = float(t[i]); i += 1
        return v
    while i < len(t):
        if t[i] in 'MLCZmlcz':
            cmd = t[i]; i += 1
        if cmd in 'Zz':
            if actual: polis.append(actual)
            actual, pos = [], ini
            continue
        if cmd == 'M':
            if actual: polis.append(actual)
            pos = (num(), num()); ini = pos; actual = [pos]; cmd = 'L'
        elif cmd == 'L':
            pos = (num(), num()); actual.append(pos)
        elif cmd == 'C':
            p0 = pos
            p1 = (num(), num()); p2 = (num(), num()); p3 = (num(), num())
            for k in range(1, pasos + 1):
                s = k / pasos; u = 1 - s
                actual.append((u**3*p0[0] + 3*u*u*s*p1[0] + 3*u*s*s*p2[0] + s**3*p3[0],
                               u**3*p0[1] + 3*u*u*s*p1[1] + 3*u*s*s*p2[1] + s**3*p3[1]))
            pos = p3
        else:
            raise ValueError('comando no soportado: ' + cmd)
    if actual: polis.append(actual)
    return polis


def cobertura(d):
    acc = np.zeros((512 * SS, 512 * SS), dtype=bool)
    for poli in aplanar(d):
        capa = Image.new('1', (512 * SS, 512 * SS), 0)
        ImageDraw.Draw(capa).polygon([(x * SS, y * SS) for x, y in poli], fill=1)
        acc ^= np.array(capa, dtype=bool)
    return acc.reshape(512, SS, 512, SS).mean(axis=(1, 3))




def color(hexa, colores):
    if hexa.startswith('@color/'):
        xml = open(colores, encoding='utf-8').read()
        hexa = re.search(r'<color name="%s">([^<]+)</color>' % hexa[7:], xml).group(1)
    hexa = hexa.lstrip('#')[-6:]
    return np.array([int(hexa[k:k + 2], 16) for k in (0, 2, 4)], dtype=float)


def comparar(ruta_png, ruta_xml, ruta_diff=None):
    import os
    colores = os.path.join(os.path.dirname(os.path.dirname(ruta_xml)), 'values', 'colors.xml')
    xml = open(ruta_xml, encoding='utf-8').read()
    img = np.tile(FONDO, (512, 512, 1))
    for nombre in ('cuerpo', 'trazo'):
        m = re.search(r'<path[^>]*android:name="%s"[^>]*>' % nombre, xml, re.S)
        bloque = m.group(0)
        d = re.search(r'android:pathData="([^"]+)"', bloque).group(1)
        c = color(re.search(r'android:fillColor="([^"]+)"', bloque).group(1), colores)
        cov = cobertura(d)[..., None]
        img = img * (1 - cov) + c * cov
    png = np.array(Image.open(ruta_png).convert('RGBA')).astype(float)
    a = png[..., 3:4] / 255.0
    ref = FONDO * (1 - a) + png[..., :3] * a
    diff = np.abs(img - ref).max(axis=2)
    total = 512 * 512
    print('pixeles totales', total)
    for u in (0, 8, 16, 32, 64, 128):
        n = int((diff > u).sum())
        print('dif > %3d: %6d (%.2f %%)' % (u, n, 100.0 * n / total))
    if ruta_diff:
        Image.fromarray(np.clip(diff * 2, 0, 255).astype(np.uint8)).save(ruta_diff)



def main():
    if len(sys.argv) >= 4 and sys.argv[1] == 'trazar':
        escribir_vectores(trazar_logo(sys.argv[2]), sys.argv[3])
    elif len(sys.argv) >= 4 and sys.argv[1] == 'comparar':
        comparar(sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
    else:
        print(__doc__)


if __name__ == '__main__':
    main()
