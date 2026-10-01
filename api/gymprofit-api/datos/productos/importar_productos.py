"""Manda a la API, por lotes, los productos que dejó filtrar_off.py (GP-164).

Todo pasa por la API (DEC-035): este script no toca la base de datos. Lo usa el workflow
`importar-productos.yml`, y sirve igual contra la API local para medir.

Uso:
    IMPORTACION_CLAVE=... python importar_productos.py productos.jsonl https://api.gymprofit.app/api

Solo con la biblioteca estándar. Al acabar escribe en una línea JSON cuántos productos se
mandaron, cuántos guardó la API, cuántos descartó, cuántos hay en la tabla, cuánto ocupa y
cuánto tardó.
"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request

LOTE = 1000
# El límite global de la API es de 200 peticiones por minuto y por IP: con una petición
# como mucho cada 0,5 s no se acerca.
PAUSA_MINIMA_S = 0.5
REINTENTOS = 6


def peticion(url: str, clave: str, cuerpo: bytes | None) -> dict:
    """POST con la clave; reintenta lo que se puede reintentar (Render dormido, 429, 5xx)."""
    espera = 5.0
    for intento in range(1, REINTENTOS + 1):
        req = urllib.request.Request(url, data=cuerpo if cuerpo is not None else b"", method="POST")
        req.add_header("Content-Type", "application/json")
        req.add_header("X-Clave-Importacion", clave)
        try:
            with urllib.request.urlopen(req, timeout=120) as respuesta:
                return json.loads(respuesta.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            if error.code in (400, 401, 403):
                # No se arregla repitiendo: la clave o el lote están mal.
                raise SystemExit(f"{url}: {error.code} {error.read().decode('utf-8', 'replace')[:300]}")
            reintentar_tras = error.headers.get("Retry-After")
            if reintentar_tras and reintentar_tras.isdigit():
                espera = max(espera, float(reintentar_tras))
            motivo = f"HTTP {error.code}"
        except (urllib.error.URLError, TimeoutError) as error:
            motivo = str(error)
        if intento == REINTENTOS:
            raise SystemExit(f"{url}: sin respuesta tras {REINTENTOS} intentos ({motivo})")
        print(f"  {motivo}; reintento {intento} en {espera:.0f} s", file=sys.stderr)
        time.sleep(espera)
        espera = min(espera * 2, 120)
    raise AssertionError("inalcanzable")


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    origen, api = sys.argv[1], sys.argv[2].rstrip("/")
    clave = os.environ.get("IMPORTACION_CLAVE", "")
    if len(clave) < 32:
        raise SystemExit("Falta IMPORTACION_CLAVE (al menos 32 caracteres)")

    inicio = time.monotonic()
    mandados = guardados = descartados = 0
    lote: list[str] = []

    def mandar() -> None:
        nonlocal mandados, guardados, descartados
        antes = time.monotonic()
        resultado = peticion(f"{api}/importacion/productos", clave, ("[" + ",".join(lote) + "]").encode("utf-8"))
        mandados += resultado["recibidos"]
        guardados += resultado["guardados"]
        descartados += resultado["descartados"]
        lote.clear()
        if mandados % 20000 < LOTE:
            print(f"  {mandados} mandados", file=sys.stderr)
        sobra = PAUSA_MINIMA_S - (time.monotonic() - antes)
        if sobra > 0:
            time.sleep(sobra)

    with open(origen, encoding="utf-8") as f:
        for linea in f:
            if linea.strip():
                lote.append(linea.strip())
            if len(lote) == LOTE:
                mandar()
    if lote:
        mandar()

    fin = peticion(f"{api}/importacion/productos/fin", clave, None)
    print(json.dumps({
        "mandados": mandados,
        "guardados": guardados,
        "descartados": descartados,
        "en_la_tabla": fin["productos"],
        "mb_datos": round(fin["bytesDatos"] / 1048576, 1),
        "mb_indices": round(fin["bytesIndices"] / 1048576, 1),
        "segundos": round(time.monotonic() - inicio),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
