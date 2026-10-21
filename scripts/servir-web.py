"""Sirve web/ en local como lo sirve Cloudflare Pages, para probar antes de subir el zip.

Uso, desde la raíz del repositorio:

    python scripts/servir-web.py            # http://127.0.0.1:8205
    python scripts/servir-web.py 9000       # otro puerto

Lo que imita de Pages (GP-205), porque es lo que se mide con Lighthouse y lo que
comprueban las grabaciones:

- Rutas limpias: /beta sirve beta.html y /beta/entrar, beta/entrar.html.
- Una ruta que no existe responde 404 con 404.html, si existe.
- Las cabeceras de web/_headers, regla a regla (con * como comodín).
- Compresión gzip de HTML, CSS, JS, SVG y JSON cuando el navegador la acepta.
- No sirve lo que no va en el zip: README.md, DISENO.md, PRODUCT.md y .impeccable/.
- Responde a HEAD con las mismas cabeceras que GET, para mirarlas con curl -I.
"""

import gzip
import mimetypes
import re
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit

WEB = Path(__file__).resolve().parent.parent / "web"
FUERA_DEL_ZIP = {"README.md", "DISENO.md", "PRODUCT.md"}
COMPRIMIBLE = ("text/", "application/javascript", "application/json", "image/svg+xml")
mimetypes.add_type("application/javascript", ".js")
mimetypes.add_type("image/webp", ".webp")
mimetypes.add_type("font/woff2", ".woff2")


def leer_cabeceras() -> list[tuple[re.Pattern, list[tuple[str, str]]]]:
    """Las reglas de _headers: un patrón de ruta y sus cabeceras."""
    reglas, actual = [], None
    fichero = WEB / "_headers"
    if not fichero.exists():
        return reglas
    for linea in fichero.read_text(encoding="utf-8").splitlines():
        if not linea.strip() or linea.lstrip().startswith("#"):
            continue
        if not linea[0].isspace():
            patron = "^" + re.escape(linea.strip()).replace(r"\*", ".*") + "$"
            actual = (re.compile(patron), [])
            reglas.append(actual)
        elif actual and ":" in linea:
            nombre, valor = linea.strip().split(":", 1)
            actual[1].append((nombre.strip(), valor.strip()))
    return reglas


def resolver(ruta: str) -> Path | None:
    """El fichero que Pages serviría para la ruta, o None si daría 404."""
    partes = [p for p in ruta.split("/") if p]
    if any(p.startswith(".") for p in partes) or (partes and partes[0] in FUERA_DEL_ZIP):
        return None
    base = WEB.joinpath(*partes) if partes else WEB
    for candidato in (base, base.with_name(base.name + ".html"), base / "index.html"):
        if candidato.is_file() and WEB in candidato.resolve().parents:
            return candidato
    return None


class Pages(BaseHTTPRequestHandler):
    reglas = leer_cabeceras()

    def do_HEAD(self) -> None:  # noqa: N802 (nombre de http.server)
        """Las mismas cabeceras que GET, sin cuerpo: para comprobar la caché con curl -I."""
        self.do_GET(cuerpo=False)

    def do_GET(self, cuerpo: bool = True) -> None:  # noqa: N802 (nombre de http.server)
        enviar_cuerpo = cuerpo
        ruta = unquote(urlsplit(self.path).path)
        fichero, estado = resolver(ruta), 200
        if fichero is None:
            fichero, estado = WEB / "404.html", 404
        cuerpo = fichero.read_bytes() if fichero.exists() else b"404"
        tipo = mimetypes.guess_type(fichero.name)[0] or "application/octet-stream"
        cabeceras = [("Content-Type", tipo + ("; charset=utf-8" if tipo.startswith("text/") else ""))]
        for patron, valores in self.reglas:
            if patron.match(ruta):
                cabeceras += valores
        if tipo.startswith(COMPRIMIBLE) and "gzip" in self.headers.get("Accept-Encoding", ""):
            cuerpo = gzip.compress(cuerpo, 9)
            cabeceras.append(("Content-Encoding", "gzip"))
        self.send_response(estado)
        for nombre, valor in cabeceras:
            self.send_header(nombre, valor)
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        if enviar_cuerpo:
            self.wfile.write(cuerpo)

    def log_message(self, formato: str, *args) -> None:
        pass


def main() -> None:
    puerto = int(sys.argv[1]) if len(sys.argv) > 1 else 8205
    print(f"Sirviendo web/ en http://127.0.0.1:{puerto}")
    ThreadingHTTPServer(("127.0.0.1", puerto), Pages).serve_forever()


if __name__ == "__main__":
    main()
