"""Genera el icono de la app desde logo.png (la versión elegida usa drawable-night/logo.png) (GP-081).

Salen de la misma fuente, en las cinco densidades:
  · mipmap-*/ic_launcher_foreground.png — el logo dentro de la zona segura del
    icono adaptativo (círculo de 66 dp sobre 108), con 1 dp de margen, para que
    ninguna máscara del launcher lo recorte;
  · mipmap-*/ic_launcher_monochrome.png — la silueta, para el icono temático;
  · mipmap-*/ic_launcher.png y ic_launcher_round.png — los heredados de API 24-25;
  · drawable-*/ic_notificacion.png — la silueta blanca, icono pequeño de las
    notificaciones;
  · el PNG de 512 × 512 para la ficha de Play.

El logo se coloca por su círculo MÍNIMO, no por su caja: la G es casi redonda y
por la caja quedaría más pequeño de lo necesario.

uso (desde la raíz del repo, con Pillow instalado):
  python scripts/generar-icono-app.py app/GymProFit/app/src/main/res       app/GymProFit/app/src/main/res/drawable-night/logo.png "#0B0C0E" icono-play.png

Si cambia el logo, se vuelve a ejecutar; los PNG no se retocan a mano.
"""
import math, sys
from PIL import Image, ImageDraw

RES, LOGO, FONDO, PLAY = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
DENS = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

# Radio del logo en dp dentro de su lienzo. Zona segura del adaptativo: círculo de
# 66 dp de diámetro en un lienzo de 108 dp. Se deja 1 dp de margen: radio 32.
R_ADAPTATIVO, LIENZO_ADAPTATIVO = 32, 108
R_NOTIF, LIENZO_NOTIF = 10, 24          # 24 dp con 2 dp de aire, como pide Material

logo = Image.open(LOGO).convert('RGBA')
alfa = logo.getchannel('A')
pts = [(x, y) for y in range(0, logo.height, 2) for x in range(0, logo.width, 2) if alfa.getpixel((x, y)) > 16]


def circulo_minimo(puntos):
    """Centro y radio del círculo mínimo que contiene el logo (búsqueda local)."""
    xs, ys = [p[0] for p in puntos], [p[1] for p in puntos]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    radio = lambda a, b: max(math.hypot(x - a, y - b) for x, y in puntos)
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


CX, CY, R = circulo_minimo(pts)


def colocar(lienzo_px, radio_px, silueta=False):
    """Logo escalado para que su círculo mínimo mida radio_px, centrado en el lienzo."""
    k = radio_px / R
    src = logo
    if silueta:  # blanco con el alfa del logo: lo que piden monocromo y notificación
        src = Image.new('RGBA', logo.size, (255, 255, 255, 0))
        src.putalpha(alfa)
    esc = src.resize((max(1, round(logo.width * k)), max(1, round(logo.height * k))), Image.LANCZOS)
    out = Image.new('RGBA', (lienzo_px, lienzo_px), (0, 0, 0, 0))
    out.alpha_composite(esc, (round(lienzo_px / 2 - CX * k), round(lienzo_px / 2 - CY * k)))
    return out


def fondo_con(forma, lado_px, logo_img):
    base = Image.new('RGBA', logo_img.size, (0, 0, 0, 0))
    m = Image.new('L', logo_img.size, 0)
    d = ImageDraw.Draw(m)
    o = (logo_img.width - lado_px) // 2
    caja = [o, o, o + lado_px - 1, o + lado_px - 1]
    if forma == 'circulo':
        d.ellipse(caja, fill=255)
    else:
        d.rounded_rectangle(caja, radius=round(lado_px * 0.18), fill=255)
    base.paste(Image.new('RGBA', logo_img.size, FONDO), (0, 0), m)
    base.alpha_composite(logo_img)
    return base


for nombre, f in DENS.items():
    mip = f'{RES}/mipmap-{nombre}'
    import os
    os.makedirs(mip, exist_ok=True)
    lado = round(LIENZO_ADAPTATIVO * f)
    colocar(lado, R_ADAPTATIVO * f).save(f'{mip}/ic_launcher_foreground.png')
    colocar(lado, R_ADAPTATIVO * f, silueta=True).save(f'{mip}/ic_launcher_monochrome.png')
    # Heredados (API 24-25): 48 dp, forma de 44 dp, logo a la misma proporción.
    l48, l44 = round(48 * f), round(44 * f)
    interior = colocar(l48, R_ADAPTATIVO * 44 / LIENZO_ADAPTATIVO * f)
    fondo_con('cuadrado', l44, interior).save(f'{mip}/ic_launcher.png')
    fondo_con('circulo', l44, interior).save(f'{mip}/ic_launcher_round.png')
    dr = f'{RES}/drawable-{nombre}'
    os.makedirs(dr, exist_ok=True)
    colocar(round(LIENZO_NOTIF * f), R_NOTIF * f, silueta=True).save(f'{dr}/ic_notificacion.png')

# Play: 512 a sangre, logo a la misma proporción que en el adaptativo.
play = Image.new('RGBA', (512, 512), FONDO)
play.alpha_composite(colocar(512, 512 * R_ADAPTATIVO / LIENZO_ADAPTATIVO))
play.convert('RGB').save(PLAY)
print(f'círculo mínimo del logo: centro ({CX:.0f},{CY:.0f}) radio {R:.0f}px de 512; ocupa {2*R_ADAPTATIVO} dp de 66')
