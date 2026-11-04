"""La G fotograma a fotograma en un arranque grabado (GP-223).

Uso: python oro.py carpeta_fotos_X  (la de fotogramas.py, fotogramas a 270 px de ancho)
Por fotograma de la pantalla oscura, la fracción de píxeles dorados o naranjas (R > 150 y R - B > 80) en la caja
de la G, centrada en la ventana. La meseta es la mediana de los fotogramas con G (la G en reposo). Entre el primer fotograma en la
meseta (la G ya entró) y el último (empieza la salida), cualquier fotograma por debajo del
85 % de la meseta es un hueco o una atenuación.
"""
import glob
import os
import sys

import numpy as np
from PIL import Image

fs = sorted(glob.glob(os.path.join(sys.argv[1], 'f_*.png')))
oro = []
for f in fs:
    im = np.asarray(Image.open(f).convert('RGB'), dtype=int)
    h, w, _ = im.shape
    lado = int(w * 160 * float(os.environ.get("GP_DENSIDAD", "2.625")) / 1080 / 2) + 4
    cy, cx = h // 2, w // 2
    caja = im[cy - lado:cy + lado, cx - lado:cx + lado]
    r, b = caja[..., 0], caja[..., 2]
    # Solo cuentan los fotogramas de la pantalla oscura del arranque: fuera de la caja de la
    # G, casi negro. Inicio (sobre todo en claro, con naranja) no entra en la meseta.
    fuera = im.mean(axis=2).copy()
    fuera[cy - lado:cy + lado, cx - lado:cx + lado] = 0
    oscuro = fuera.sum() / (fuera.size - (2 * lado) ** 2) < 40
    oro.append(float(((r > 150) & (r - b > 80)).mean()) if oscuro else -1.0)
con_g = sorted(o for o in oro if o > 0.05)
meseta = con_g[len(con_g) // 2] if con_g else 0  # la G en reposo: lo más frecuente
dentro = [i for i, o in enumerate(oro) if o >= 0.85 * meseta]
malos = []
if dentro:
    a, z = dentro[0], dentro[-1]
    malos = [(i, round(oro[i] / meseta, 2)) for i in range(a, z + 1) if oro[i] < 0.85 * meseta]
print('%s: %d fotogramas, meseta %.3f, G entera de f%d a f%d, por debajo del 85 %%: %s' % (
    os.path.basename(sys.argv[1]), len(oro), meseta, dentro[0] if dentro else -1,
    dentro[-1] if dentro else -1, malos if malos else 'ninguno'))
