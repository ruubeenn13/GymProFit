"""Pone en web/portada.css la reserva de cada captura de la portada (GP-205).

Uso, desde la raíz del repositorio, cada vez que cambie una captura de web/img/capturas:

    python scripts/reservas-capturas-web.py

La reserva es la captura reducida a 20 × 44 px, en webp y en línea (data URI): el
navegador la estira y sale borrosa. Va de fondo de la imagen, así que mientras la
captura de verdad no ha llegado o no está decodificada se ve una versión difusa de
la pantalla, y nunca un rectángulo negro. Se escribe entre las marcas
«▼ RESERVAS DE LAS CAPTURAS» y «▲ FIN DE LAS RESERVAS» de portada.css, con el color
medio de cada captura debajo por si el webp en línea no se pinta. Necesita Pillow.
"""

import base64
import io
import re
from pathlib import Path

from PIL import Image, ImageFilter, ImageStat

WEB = Path(__file__).resolve().parent.parent / "web"
HOJA = WEB / "portada.css"
CAPTURAS = ("inicio", "entreno", "nutricion", "progreso")
INICIO = "/* ▼ RESERVAS DE LAS CAPTURAS (scripts/reservas-capturas-web.py): no se editan a mano */"
FIN = "/* ▲ FIN DE LAS RESERVAS */"


def reserva(nombre: str) -> tuple[str, str]:
    """El color medio en hexadecimal y la captura diminuta como data URI webp."""
    captura = Image.open(WEB / "img" / "capturas" / f"{nombre}-360.webp").convert("RGB")
    media = ImageStat.Stat(captura).mean
    color = "#{:02X}{:02X}{:02X}".format(*(round(c) for c in media))
    diminuta = captura.filter(ImageFilter.GaussianBlur(4)).resize((20, 44), Image.LANCZOS)
    salida = io.BytesIO()
    diminuta.save(salida, "WEBP", quality=40, method=6)
    return color, "data:image/webp;base64," + base64.b64encode(salida.getvalue()).decode("ascii")


def main() -> None:
    reglas = []
    for nombre in CAPTURAS:
        color, uri = reserva(nombre)
        reglas.append(f'.movil img[src*="/{nombre}-"] {{ background: {color} url("{uri}") 0 0 / 100% 100%; }}')
    bloque = INICIO + "\n" + "\n".join(reglas) + "\n" + FIN
    css = HOJA.read_text(encoding="utf-8")
    patron = re.compile(re.escape(INICIO) + r".*?" + re.escape(FIN), re.S)
    if not patron.search(css):
        raise SystemExit(f"No están las marcas de las reservas en {HOJA}")
    HOJA.write_text(patron.sub(lambda _: bloque, css), encoding="utf-8")
    print("Reservas escritas:", ", ".join(CAPTURAS))


if __name__ == "__main__":
    main()
