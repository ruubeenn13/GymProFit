"""La caja de la G a resolución completa en un arranque grabado (GP-223).

Uso: python -I caja.py video.mp4
Saca todos los fotogramas a 1080 px (sin remuestrear en el tiempo), y por cada fotograma de la
pantalla oscura mide, en un cuadrado de 640 px centrado, la fracción de píxeles de la G
(dorados o naranjas: R > 150 y R - B > 80; con el brillo encima siguen cumpliéndolo) y su
caja (x0, y0, x1, y1). Mismo criterio de hueco que oro.py: entre el primer fotograma con la G
entera (>= 85 % de la meseta) y el último, cualquiera por debajo es hueco o atenuación. La
caja se compara en esos fotogramas contra la mediana: la deriva máxima de cada borde y del
ancho y alto, en px.
"""
import glob
import os
import re
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

video = sys.argv[1]
d = tempfile.mkdtemp(prefix='caja_')
r = subprocess.run(['ffmpeg', '-hide_banner', '-i', video, '-fps_mode', 'passthrough',
                    '-vf', 'showinfo', os.path.join(d, 'f_%06d.png')],
                   capture_output=True, text=True, encoding='utf-8', errors='replace')
tiempos = [float(t) for t in re.findall(r'pts_time:([\d.]+)', r.stderr)]
fs = sorted(glob.glob(os.path.join(d, 'f_*.png')))
# Medio lado del cuadrado: 320 px a 1080 de ancho y 420 dpi (la G de 160 dp ocupa 420 px).
L = int(os.environ.get('GP_CAJA', '320'))
oro, cajas = [], []
for f in fs:
    im = np.asarray(Image.open(f).convert('RGB'), dtype=np.int16)
    h, w, _ = im.shape
    cy, cx = h // 2, w // 2
    c = im[cy - L:cy + L, cx - L:cx + L]
    fuera = im.mean(axis=2)
    fuera[cy - L:cy + L, cx - L:cx + L] = 0
    oscuro = fuera.sum() / (fuera.size - (2 * L) ** 2) < 40
    m = (c[..., 0] > 150) & (c[..., 0] - c[..., 2] > 80)
    if not oscuro:
        oro.append(-1.0)
        cajas.append(None)
        continue
    oro.append(float(m.mean()))
    ys, xs = np.nonzero(m)
    cajas.append((xs.min(), ys.min(), xs.max(), ys.max()) if len(xs) else None)
    os.remove(f)
for f in glob.glob(os.path.join(d, 'f_*.png')):
    os.remove(f)
os.rmdir(d)

con_g = sorted(o for o in oro if o > 0.02)
meseta = con_g[len(con_g) // 2] if con_g else 0
dentro = [i for i, o in enumerate(oro) if meseta and o >= 0.85 * meseta]
malos, deriva = [], None
if dentro:
    a, z = dentro[0], dentro[-1]
    malos = [(i, round(oro[i] / meseta, 2)) for i in range(a, z + 1) if oro[i] < 0.85 * meseta]
    # La caja en reposo: desde que la G ya no crece (misma caja que la mediana) hasta la salida.
    cs = np.array([cajas[i] for i in range(a, z + 1) if cajas[i] is not None and oro[i] >= 0.85 * meseta])
    med = np.median(cs, axis=0)
    # Del primer al último fotograma con la caja exacta de la mediana: ahí dentro cae el
    # traspaso, y cualquier movimiento o cambio de tamaño se mide.
    reposo = [i for i in range(a, z + 1) if cajas[i] is not None
              and np.abs(np.array(cajas[i]) - med).max() == 0]
    if reposo:
        cr = np.array([cajas[i] for i in range(reposo[0], reposo[-1] + 1) if cajas[i] is not None])
        anch = cr[:, 2] - cr[:, 0]
        alto = cr[:, 3] - cr[:, 1]
        deriva = (int(np.abs(cr - med).max()), int(anch.max() - anch.min()), int(alto.max() - alto.min()),
                  len(cr), reposo[0], reposo[-1])
t = lambda i: tiempos[i] if i < len(tiempos) else -1
print('%s: %d fotogramas, meseta %.4f, G entera f%d-f%d (%.2f-%.2f s), huecos: %s, caja: %s' % (
    os.path.basename(video), len(oro), meseta, dentro[0] if dentro else -1, dentro[-1] if dentro else -1,
    t(dentro[0]) if dentro else -1, t(dentro[-1]) if dentro else -1, malos if malos else 'ninguno',
    ('borde max %d px, ancho %d px, alto %d px en %d fotogramas (f%d-f%d)' % deriva) if deriva else 'sin datos'))
