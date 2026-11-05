"""Cuándo se ve el brillo sobre la G (GP-223). Uso: python -I brillo.py carpeta_fotos_X
Por fotograma de pantalla oscura, la fracción de píxeles de la G aclarados por la franja
(R > 200 y B > 90: el dorado con blanco encima) en la caja de la G. Imprime los tramos con brillo."""
import glob, os, re, sys
import numpy as np
from PIL import Image
d = sys.argv[1]
ts = [float(l.split()[0]) for l in open(d + '.txt', encoding='utf-8').read().splitlines()[3:] if l.strip()]
fs = sorted(glob.glob(os.path.join(d, 'f_*.png')))
cajas, oscuros = [], []
for f in fs:
    im = np.asarray(Image.open(f).convert('RGB'), dtype=int); h, w, _ = im.shape
    l = int(w * 160 * float(os.environ.get("GP_DENSIDAD", "2.625")) / 1080 / 2) + 4
    cajas.append(im[h // 2 - l:h // 2 + l, w // 2 - l:w // 2 + l]); oscuros.append(im.mean() < 40)
ref = np.median(np.array([c for c, o in zip(cajas, oscuros) if o]), axis=0)
tramos, dentro, n = [], None, 0
for i, c in enumerate(cajas):
    b = ((c[..., 2] - ref[..., 2]) > 40).mean() if oscuros[i] else 0
    if b > 0.004:
        n += 1
        if dentro is None: dentro = ts[i]
    elif dentro is not None: tramos.append((dentro, ts[i])); dentro = None
print(os.path.basename(d), n, 'fotogramas con brillo; tramos', ['%.2f-%.2f' % t for t in tramos])
