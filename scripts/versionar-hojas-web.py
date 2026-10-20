"""Pone en cada página de web/ la versión de las hojas que pide: /beta.css?v=<hash>.

La versión son los 8 primeros caracteres hexadecimales del SHA-256 de la hoja, con
los saltos de línea normalizados a \\n para que salga igual en Windows y en el CI.
Así la URL cambia solo cuando cambia la hoja, y _headers puede guardarla un año
(GP-201). HojasVersionadasWebTest falla si una hoja cambia y su versión no.

Uso, desde la raíz del repositorio y antes de hacer el zip de la web:
    python scripts/versionar-hojas-web.py              # reescribe las páginas
    python scripts/versionar-hojas-web.py --comprobar  # solo dice si falta (sale con 1)
"""
import hashlib
import re
import sys
from pathlib import Path

WEB = Path(__file__).resolve().parent.parent / "web"
HOJA = re.compile(r'(<link rel="stylesheet" href="/)([a-z-]+\.css)(?:\?v=[0-9a-f]+)?(">)')


def version(hoja: Path) -> str:
    """Los 8 primeros caracteres del SHA-256 del contenido, con los saltos en \\n."""
    texto = hoja.read_bytes().replace(b"\r\n", b"\n")
    return hashlib.sha256(texto).hexdigest()[:8]


def versionar(pagina: Path) -> tuple[str, str]:
    """El HTML de la página tal cual está y con las hojas versionadas."""
    html = pagina.read_text(encoding="utf-8")
    nuevo = HOJA.sub(lambda m: f"{m[1]}{m[2]}?v={version(WEB / m[2])}{m[3]}", html)
    return html, nuevo


def main() -> int:
    comprobar = "--comprobar" in sys.argv
    pendientes = []
    for pagina in sorted(WEB.rglob("*.html")):
        html, nuevo = versionar(pagina)
        if html != nuevo:
            pendientes.append(pagina.relative_to(WEB).as_posix())
            if not comprobar:
                # newline="" para no cambiar los saltos de línea que ya tenga el fichero
                pagina.write_text(nuevo, encoding="utf-8", newline="")
    if comprobar and pendientes:
        print("Hojas sin versionar o con la versión vieja en:", ", ".join(pendientes))
        return 1
    print(("Versionadas: " + ", ".join(pendientes)) if pendientes else "Todas las hojas están al día.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
