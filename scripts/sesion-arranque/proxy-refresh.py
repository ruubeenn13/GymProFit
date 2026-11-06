"""Proxy de pruebas de la renovación de sesión (GP-237, GP-235, DEC-059).

Escucha en el 8080, donde busca la API la app de depuración, y reenvía todo a la API
local en el 8081, salvo POST /api/auth/refresh, que hace lo que diga el fichero modo.txt:

  pasar             reenvía a la API
  503/500/502/429   responde ese código sin tocar la API (el 429 con Retry-After: 30)
  colgar            no contesta en 70 s (la app agota su tiempo a los 60)
  cortar            cierra la conexión sin responder

Cada llamada a /auth/refresh queda en refresh.log con la hora y el modo. modo.txt y
refresh.log viven en la carpeta temporal del sistema (gymprofit-proxy-refresh), fuera del
repositorio; tanda.sh los lee del mismo sitio.

La API se arranca en el 8081 con el access corto, sin tocar ningún fichero:
  sh ./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --jwt.expiration=60000"

Uso: python scripts/renovacion/proxy-refresh.py [puerto]   (Ctrl+C para pararlo y liberar el 8080)
"""
import http.client
import os
import sys
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DIR = os.path.join(tempfile.gettempdir(), "gymprofit-proxy-refresh")
MODO = os.path.join(DIR, "modo.txt")
LOG = os.path.join(DIR, "refresh.log")
DESTINO = ("127.0.0.1", 8081)
cerrojo = threading.Lock()


def modo() -> str:
    """El modo de /auth/refresh; «pasar» si no hay fichero."""
    try:
        with open(MODO, encoding="utf-8") as f:
            return f.read().strip() or "pasar"
    except OSError:
        return "pasar"


def apuntar(texto: str) -> None:
    """Añade una línea con la hora a refresh.log."""
    with cerrojo, open(LOG, "a", encoding="utf-8") as f:
        f.write(f"{time.strftime('%H:%M:%S')}.{int(time.time() * 1000) % 1000:03d} {texto}\n")


class Proxy(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def _reenviar(self, cuerpo: bytes) -> int:
        con = http.client.HTTPConnection(*DESTINO, timeout=120)
        cabeceras = {k: v for k, v in self.headers.items() if k.lower() not in ("host", "connection")}
        con.request(self.command, self.path, body=cuerpo or None, headers=cabeceras)
        r = con.getresponse()
        datos = r.read()
        self.send_response(r.status)
        for k, v in r.getheaders():
            if k.lower() not in ("transfer-encoding", "connection", "content-length"):
                self.send_header(k, v)
        self.send_header("Content-Length", str(len(datos)))
        self.end_headers()
        self.wfile.write(datos)
        return r.status

    def _todo(self):
        n = int(self.headers.get("Content-Length") or 0)
        cuerpo = self.rfile.read(n) if n else b""
        if not self.path.endswith("/auth/refresh"):
            self._reenviar(cuerpo)
            return
        m = modo()
        if m == "pasar":
            apuntar(f"refresh pasar -> {self._reenviar(cuerpo)}")
            return
        apuntar(f"refresh {m}")
        if m == "colgar":
            time.sleep(70)
            self.close_connection = True
            return
        if m == "cortar":
            self.close_connection = True
            self.connection.close()
            return
        datos = b'{"code":%s,"message":"prueba"}' % m.encode()
        self.send_response(int(m))
        self.send_header("Content-Type", "application/json")
        if m == "429":
            self.send_header("Retry-After", "30")
        self.send_header("Content-Length", str(len(datos)))
        self.end_headers()
        self.wfile.write(datos)

    do_GET = do_POST = do_PUT = do_PATCH = do_DELETE = _todo


if __name__ == "__main__":
    os.makedirs(DIR, exist_ok=True)
    puerto = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"proxy en {puerto} -> {DESTINO[1]}; modo y log en {DIR}")
    ThreadingHTTPServer(("0.0.0.0", puerto), Proxy).serve_forever()
