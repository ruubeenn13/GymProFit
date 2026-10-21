"""Genera las dos texturas de la web (GP-205): el punto de la tela y la fibra del dorsal.

Uso, desde la raíz del repositorio:

    python scripts/texturas-web.py

Escribe ``web/img/tela.webp`` y ``web/img/tyvek.webp``. Son mosaicos sin costura
(lo que sale por un borde entra por el contrario) con transparencia (el punto en blanco,
para la tela oscura; la fibra en negro, para el panel claro): la hoja decide su
opacidad. La semilla
es fija, de modo que dos ejecuciones dan el mismo fichero.
"""

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

DESTINO = Path(__file__).resolve().parent.parent / "web" / "img"
SEMILLA = 205


def punto_de_tela(lado: int = 96, paso_x: int = 4, paso_y: int = 6) -> Image.Image:
    """El punto de una camiseta técnica: columnas de uves pequeñas, cada una con su brillo."""
    rng = np.random.default_rng(SEMILLA)
    escala = 4  # se dibuja a 4× y se reduce para que las líneas salgan suaves
    lienzo = Image.new("L", (lado * escala, lado * escala), 0)
    pincel = ImageDraw.Draw(lienzo)
    for y in range(0, lado, paso_y):
        for x in range(0, lado, paso_x):
            brillo = int(rng.integers(90, 200))
            cx, top, fondo = (x + paso_x / 2) * escala, y * escala, (y + paso_y) * escala
            ancho = 1.2 * escala
            pincel.line([(x * escala, top), (cx, fondo)], fill=brillo, width=int(ancho))
            pincel.line([((x + paso_x) * escala, top), (cx, fondo)], fill=brillo, width=int(ancho))
    alfa = lienzo.resize((lado, lado), Image.LANCZOS)
    blanco = Image.new("L", (lado, lado), 255)
    return Image.merge("LA", (blanco, alfa)).convert("RGBA")


def fibra_de_tyvek(lado: int = 256, fibras: int = 320) -> Image.Image:
    """Fibras finas y curvas en todas direcciones, como el polietileno prensado de un dorsal."""
    rng = np.random.default_rng(SEMILLA + 1)
    lienzo = Image.new("L", (lado * 3, lado * 3), 0)
    pincel = ImageDraw.Draw(lienzo)
    for _ in range(fibras):
        x, y = rng.uniform(0, lado, 2)
        angulo = rng.uniform(0, np.pi)
        largo = rng.uniform(18, 70)
        curva = rng.uniform(-0.6, 0.6)
        brillo = int(rng.integers(60, 180))
        puntos = []
        for t in np.linspace(0, 1, 8):
            a = angulo + curva * t
            puntos.append((x + np.cos(a) * largo * t, y + np.sin(a) * largo * t))
        # La fibra se dibuja en las nueve copias del mosaico para que no haya costura
        for dx in (0, lado, 2 * lado):
            for dy in (0, lado, 2 * lado):
                pincel.line([(px + dx, py + dy) for px, py in puntos], fill=brillo, width=1)
    centro = lienzo.crop((lado, lado, 2 * lado, 2 * lado)).filter(ImageFilter.GaussianBlur(0.4))
    # Fibra en tinta oscura: va sobre el panel claro del dorsal
    negro = Image.new("L", (lado, lado), 0)
    return Image.merge("LA", (negro, centro)).convert("RGBA")


def main() -> None:
    """Escribe las dos texturas en web/img."""
    DESTINO.mkdir(parents=True, exist_ok=True)
    punto_de_tela().save(DESTINO / "tela.webp", "WEBP", lossless=True, method=6)
    fibra_de_tyvek().save(DESTINO / "tyvek.webp", "WEBP", quality=55, method=6)
    for nombre in ("tela.webp", "tyvek.webp"):
        print(nombre, (DESTINO / nombre).stat().st_size, "bytes")


if __name__ == "__main__":
    main()
