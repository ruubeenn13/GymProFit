"""Pone en cada página de web/ la versión de las hojas y los scripts que pide: /beta.css?v=<hash>.

La versión son los 8 primeros caracteres hexadecimales del SHA-256 del fichero, con
los saltos de línea normalizados a \\n para que salga igual en Windows y en el CI.
Así la URL cambia solo cuando cambia el fichero, y _headers puede guardarla un año
(GP-201). Desde GP-205 vale igual para los .js (DEC-053), estén en un src o en la
lista data-libs que carga portada.js. HojasVersionadasWebTest falla si un fichero
cambia y su versión no.

Uso, desde la raíz del repositorio y antes de hacer el zip de la web:
    python scripts/versionar-hojas-web.py              # reescribe las páginas
    python scripts/versionar-hojas-web.py --comprobar  # solo dice si falta (sale con 1)
"""
import hashlib
import re
import sys
from pathlib import Path

WEB = Path(__file__).resolve().parent.parent / "web"
# Una ruta propia a una hoja o a un script, entre comillas o separada por espacios
# (data-libs lleva varias), con o sin la versión de antes.
RECURSO = re.compile(r'(?<=["\s])(/[A-Za-z0-9/_.-]+\.(?:css|js))(?:\?v=[0-9a-f]+)?(?=["\s])')


def version(fichero: Path) -> str:
    """Los 8 primeros caracteres del SHA-256 del contenido, con los saltos en \\n."""
    texto = fichero.read_bytes().replace(b"\r\n", b"\n")
    return hashlib.sha256(texto).hexdigest()[:8]


def versionar(pagina: Path) -> tuple[str, str]:
    """El HTML de la página tal cual está y con las hojas y los scripts versionados."""
    html = pagina.read_text(encoding="utf-8")

    def con_version(m: re.Match) -> str:
        fichero = WEB / m[1].lstrip("/")
        return f"{m[1]}?v={version(fichero)}" if fichero.is_file() else m[0]

    return html, RECURSO.sub(con_version, html)


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
        print("Hojas o scripts sin versionar o con la versión vieja en:", ", ".join(pendientes))
        return 1
    print(("Versionadas: " + ", ".join(pendientes)) if pendientes else "Todas las hojas y scripts están al día.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
