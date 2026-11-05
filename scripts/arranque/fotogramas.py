"""Fotograma a fotograma de una grabación del arranque (GP-223).

Uso: python fotogramas.py video.mp4 carpeta_salida
Saca todos los fotogramas (sin remuestrear, con su hora) y, por cada uno, la luminancia
media y la fracción de píxeles claros (luma > 160) de la pantalla sin las barras del
sistema. Marca la primera vez que la pantalla queda oscura (el splash) y avisa de
cualquier fotograma claro entre esa y el último oscuro: eso sería un destello.
"""
import glob
import os
import re
import subprocess
import sys

import numpy as np
from PIL import Image

video, salida = sys.argv[1], sys.argv[2]
os.makedirs(salida, exist_ok=True)
for f in glob.glob(os.path.join(salida, 'f_*.png')):
    os.remove(f)
r = subprocess.run(['ffmpeg', '-hide_banner', '-i', video, '-fps_mode', 'passthrough',
                    '-vf', 'showinfo,scale=270:-1', os.path.join(salida, 'f_%06d.png')],
                   capture_output=True, text=True, encoding='utf-8', errors='replace')
tiempos = [float(t) for t in re.findall(r'pts_time:([\d.]+)', r.stderr)]
ficheros = sorted(glob.glob(os.path.join(salida, 'f_*.png')))
datos = []
for i, f in enumerate(ficheros):
    im = np.asarray(Image.open(f).convert('L'), dtype=float)
    h = im.shape[0]
    zona = im[int(h * 0.04):int(h * 0.94)]
    datos.append((tiempos[i] if i < len(tiempos) else -1, zona.mean(), (zona > 160).mean(), os.path.basename(f)))
oscuros = [i for i, d in enumerate(datos) if d[1] < 40 and d[2] < 0.05]
print('fotogramas', len(datos))
if oscuros:
    a, b = oscuros[0], oscuros[-1]
    claros = [d for d in datos[a:b + 1] if d[2] > 0.25]
    print('oscuro desde %.3f s hasta %.3f s (%d fotogramas); claros en medio: %d' % (
        datos[a][0], datos[b][0], b - a + 1, len(claros)))
    for d in claros:
        print('  CLARO', d)
for d in datos:
    print('%.3f  luma %.1f  claro %.3f  %s' % d)
