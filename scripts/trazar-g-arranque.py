"""Traza la G del arranque desde documentacion/marca/logo.png, mide su fidelidad (GP-223)
y saca de esos trazados el logo único de la app, la web y el admin (GP-231).

documentacion/marca/logo.png es la fuente de la marca: la G dorada con el trazo naranja.
Vive fuera de res/ a propósito, porque el APK no la usa: usa sus trazados.

Tres órdenes, desde la raíz del repo:

  python scripts/trazar-g-arranque.py trazar  documentacion/marca/logo.png app/GymProFit/app/src/main/res
      Separa el cuerpo dorado y el trazo naranja por color y componente conexa, suaviza
      el borde (el PNG tiene ruido de compresión), sube cada máscara a x4, la traza con
      potrace y escribe drawable/g_arranque.xml (último fotograma) y
      drawable/g_arranque_inicial.xml (primero, base de la animada). Los colores son la
      mediana del interior de cada pieza: van a values/colors.xml (logo_g_cuerpo y
      logo_g_trazo) a mano, si cambian.

  python scripts/trazar-g-arranque.py comparar documentacion/marca/logo.png app/GymProFit/app/src/main/res/drawable/g_arranque.xml [diferencias.png]
      Rasteriza los dos trazados del vector a 512 px (supermuestreo x8, par-impar), los
      compone sobre el fondo del splash (#0B0C0E) igual que el PNG y cuenta los píxeles
      cuya diferencia máxima por canal pasa de cada umbral.

  python scripts/trazar-g-arranque.py logo documentacion/marca/logo.png app/GymProFit/app/src/main/res web/img admin/public
      El logo único (DEC-057). No vuelve a trazar: copia los dos trazados de
      drawable/g_arranque.xml y los coloca en una baldosa de 100 x 100 con la G en la
      proporción del icono en el lanzador (el círculo mínimo de la G, calculado sobre el
      PNG como en generar-icono-app.py, mide 64/72 del lado). Escribe:
        · res/drawable/logo_marca.xml — Acerca de: la baldosa en logo_baldosa (#0B0C0E en
          claro, transparente en oscuro) y la G encima;
        · web/img/logo.svg — la G sola, en la misma caja; en claro, la baldosa la pone el CSS;
        · web/img/favicon.png y admin/public/favicon.png — la baldosa con la G, 64 px;
        · web/img/apple-touch-icon.png — el icono a sangre, opaco, 180 px.
      Necesita Pillow y numpy.

  python scripts/trazar-g-arranque.py correo app/GymProFit/app/src/main/res web/img
      El logo de los correos (GP-266): web/img/logo-correo.png, 84 x 84 (3x los 28 px a
      los que lo pinta PlantillaCorreo), la G sola sobre fondo transparente. Los mismos
      trazados de g_arranque.xml; la G, sin baldosa, ocupa la caja con MARGEN_CORREO de
      aire para que el borde suavizado no se corte. El color de cada píxel del borde es
      el de la pieza, sin premultiplicar por ningún fondo: así no hay halo ni sobre blanco
      ni sobre gris oscuro. Necesita Pillow y numpy.

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
  Trazada con potrace (potracer) de documentacion/marca/logo.png: dos trazados, el cuerpo
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



# ── El logo único (GP-231, DEC-057) ─────────────────────────────────────────
LADO = 100.0                # la baldosa, en unidades del vector y del SVG
PROPORCION = 64.0 / 72.0    # diámetro del círculo mínimo de la G / lado: el icono en el lanzador
                            # (radio de 32 dp en los 72 dp visibles de la capa de 108)
ESQUINA = 0.18              # radio de las esquinas / lado, el del icono heredado
BALDOSA = '#0B0C0E'         # ic_launcher_fondo
BALDOSA_PATH = ('M18,0H82A18,18 0,0 1,100,18V82A18,18 0,0 1,82,100H18'
                'A18,18 0,0 1,0,82V18A18,18 0,0 1,18,0Z')


def circulo_minimo(ruta_png):
    """Centro y radio del círculo mínimo de la G, igual que generar-icono-app.py."""
    import math
    logo = Image.open(ruta_png).convert('RGBA')
    alfa = logo.getchannel('A')
    pts = [(x, y) for y in range(0, logo.height, 2) for x in range(0, logo.width, 2) if alfa.getpixel((x, y)) > 16]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    radio = lambda a, b: max(math.hypot(x - a, y - b) for x, y in pts)
    r, paso = radio(cx, cy), 16.0
    while paso > 0.25:
        mejor = None
        for dx, dy in ((paso, 0), (-paso, 0), (0, paso), (0, -paso)):
            rr = radio(cx + dx, cy + dy)
            if rr < r:
                r, mejor = rr, (cx + dx, cy + dy)
        if mejor:
            cx, cy = mejor
        else:
            paso /= 2
    return cx, cy, r


def piezas(ruta_xml):
    """Los dos trazados de g_arranque.xml y sus colores, en el orden en que se pintan."""
    import os
    colores = os.path.join(os.path.dirname(os.path.dirname(ruta_xml)), 'values', 'colors.xml')
    xml = open(ruta_xml, encoding='utf-8').read()
    salida = []
    for nombre in ('cuerpo', 'trazo'):
        bloque = re.search(r'<path[^>]*android:name="%s"[^>]*>' % nombre, xml, re.S).group(0)
        d = re.search(r'android:pathData="([^"]+)"', bloque).group(1)
        ref = re.search(r'android:fillColor="([^"]+)"', bloque).group(1)
        hexa = '#%02X%02X%02X' % tuple(int(v) for v in color(ref, colores))
        salida.append((nombre, ref, hexa, d))
    return salida


VECTOR_LOGO = '''<?xml version="1.0" encoding="utf-8"?>
<!--
  logo_marca — el logo único (GP-231, DEC-057): la G dorada con el trazo naranja, la del icono.
  Sobre claro va dentro de su baldosa #0B0C0E; sobre oscuro logo_baldosa es transparente
  y la G va sola, en el mismo sitio. No se recolorea ni tiene variante por tema.

  Lo escribe scripts/trazar-g-arranque.py logo: los trazados son los de g_arranque.xml,
  sin el margen del splash, y el círculo mínimo de la G mide 64/72 del lado, como en el
  icono del lanzador. No se retoca a mano.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="96dp"
    android:height="96dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <path
        android:name="baldosa"
        android:fillColor="@color/logo_baldosa"
        android:pathData="{baldosa}" />
    <group
        android:name="g"
        android:translateX="{tx}"
        android:translateY="{ty}"
        android:scaleX="{k}"
        android:scaleY="{k}">
{paths}
    </group>
</vector>
'''

PATH_LOGO = '''        <path
            android:name="{nombre}"
            android:fillColor="{ref}"
            android:pathData="{d}" />'''


def rasterizar(trozos, lado_px, k, tx, ty, esquinas):
    """La baldosa con la G a lado_px, supermuestreada; con esquinas=None, a sangre y opaca."""
    n = lado_px * SS
    s = n / LADO
    fondo = np.array([int(BALDOSA[i:i + 2], 16) for i in (1, 3, 5)], dtype=float)
    img = np.tile(fondo, (n, n, 1))
    for _, _, hexa, d in trozos:
        acc = np.zeros((n, n), dtype=bool)
        for poli in aplanar(d):
            capa = Image.new('1', (n, n), 0)
            ImageDraw.Draw(capa).polygon([((x * k + tx) * s, (y * k + ty) * s) for x, y in poli], fill=1)
            acc ^= np.array(capa, dtype=bool)
        img[acc] = np.array([int(hexa[i:i + 2], 16) for i in (1, 3, 5)], dtype=float)
    rgb = img.reshape(lado_px, SS, lado_px, SS, 3).mean(axis=(1, 3))
    if esquinas is None:
        return Image.fromarray(np.round(rgb).astype(np.uint8), 'RGB')
    mascara = Image.new('L', (n, n), 0)
    ImageDraw.Draw(mascara).rounded_rectangle([0, 0, n - 1, n - 1], radius=esquinas * n, fill=255)
    a = np.array(mascara, dtype=float).reshape(lado_px, SS, lado_px, SS).mean(axis=(1, 3))
    return Image.fromarray(np.round(np.dstack([rgb, a])).astype(np.uint8), 'RGBA')


def logo(ruta_png, res, web_img, admin_public):
    cx, cy, r = circulo_minimo(ruta_png)
    k = LADO * PROPORCION / (2 * r)
    tx, ty = LADO / 2 - cx * k, LADO / 2 - cy * k
    num = lambda v: ('%.4f' % v).rstrip('0').rstrip('.')
    trozos = piezas(res + '/drawable/g_arranque.xml')
    paths = '\n'.join(PATH_LOGO.format(nombre=n, ref=ref, d=d) for n, ref, _, d in trozos)
    with open(res + '/drawable/logo_marca.xml', 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(VECTOR_LOGO.format(baldosa=BALDOSA_PATH, tx=num(tx), ty=num(ty), k=num(k), paths=paths))
    svg = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">'
           '<!-- El logo único de GymProFit (GP-231, DEC-057): los trazados de la G del arranque. '
           'Lo escribe scripts/trazar-g-arranque.py logo; no se edita a mano. -->'
           '<g transform="translate(%s %s) scale(%s)">%s</g></svg>\n') % (
        num(tx), num(ty), num(k), ''.join('<path fill="%s" d="%s"/>' % (hexa, d) for _, _, hexa, d in trozos))
    with open(web_img + '/logo.svg', 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(svg)
    favicon = rasterizar(trozos, 64, k, tx, ty, ESQUINA)
    favicon.save(web_img + '/favicon.png', optimize=True)
    favicon.save(admin_public + '/favicon.png', optimize=True)
    rasterizar(trozos, 180, k, tx, ty, None).save(web_img + '/apple-touch-icon.png', optimize=True)
    print('círculo mínimo: centro (%.1f, %.1f) radio %.1f de 512; escala %s, desplazamiento (%s, %s)'
          % (cx, cy, r, num(k), num(tx), num(ty)))


LADO_CORREO = 84            # px: 3x los 28 px del <img> del correo
MARGEN_CORREO = 2           # px de aire a cada lado, a 84


def correo(res, web_img):
    """La G sola, transparente, a LADO_CORREO px: web/img/logo-correo.png (GP-266)."""
    trozos = piezas(res + '/drawable/g_arranque.xml')
    polis = [(hexa, aplanar(d)) for _, _, hexa, d in trozos]
    xs = [x for _, ps in polis for p in ps for x, _ in p]
    ys = [y for _, ps in polis for p in ps for _, y in p]
    ancho, alto = max(xs) - min(xs), max(ys) - min(ys)
    util = LADO_CORREO - 2 * MARGEN_CORREO
    k = util / max(ancho, alto)
    tx = (LADO_CORREO - ancho * k) / 2 - min(xs) * k
    ty = (LADO_CORREO - alto * k) / 2 - min(ys) * k
    n = LADO_CORREO * SS
    suma = np.zeros((LADO_CORREO, LADO_CORREO, 3))
    alfa = np.zeros((LADO_CORREO, LADO_CORREO))
    for hexa, ps in polis:
        acc = np.zeros((n, n), dtype=bool)
        for poli in ps:
            capa = Image.new('1', (n, n), 0)
            ImageDraw.Draw(capa).polygon([((x * k + tx) * SS, (y * k + ty) * SS) for x, y in poli], fill=1)
            acc ^= np.array(capa, dtype=bool)
        cob = acc.reshape(LADO_CORREO, SS, LADO_CORREO, SS).mean(axis=(1, 3))
        rgb = np.array([int(hexa[i:i + 2], 16) for i in (1, 3, 5)], dtype=float)
        suma += cob[..., None] * rgb
        alfa += cob
    alfa = np.clip(alfa, 0, 1)
    # Color sin premultiplicar: la media de las piezas que cubren el píxel, pesada por su
    # cobertura. Donde no cubre ninguna, alfa 0 y el color da igual (negro).
    rgb = np.where(alfa[..., None] > 0, suma / np.maximum(alfa, 1e-9)[..., None], 0)
    img = Image.fromarray(np.round(np.dstack([np.clip(rgb, 0, 255), alfa * 255])).astype(np.uint8), 'RGBA')
    img.save(web_img + '/logo-correo.png', optimize=True)
    print('logo-correo.png: %d px, G de %.1f x %.1f px' % (LADO_CORREO, ancho * k, alto * k))


def main():
    if len(sys.argv) >= 4 and sys.argv[1] == 'trazar':
        escribir_vectores(trazar_logo(sys.argv[2]), sys.argv[3])
    elif len(sys.argv) >= 6 and sys.argv[1] == 'logo':
        logo(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
    elif len(sys.argv) >= 4 and sys.argv[1] == 'correo':
        correo(sys.argv[2], sys.argv[3])
    elif len(sys.argv) >= 4 and sys.argv[1] == 'comparar':
        comparar(sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
    else:
        print(__doc__)


if __name__ == '__main__':
    main()
