"""Filtra la exportación CSV de Open Food Facts y deja los productos que se venden en España.

Lo usa el workflow semanal `importar-productos.yml` (GP-164): lee el CSV comprimido tal cual se
descarga, sin descomprimirlo a disco, y escribe un JSON Lines con un producto por línea, listo para
mandarlo por lotes a `POST /admin/productos/lote`.

Solo con la biblioteca estándar: el workflow no instala nada.

Uso:
    python filtrar_off.py en.openfoodfacts.org.products.csv.gz productos.jsonl [--max N]

Reglas (DEC-041):
- Vendido en España: ``countries_tags`` contiene ``en:spain``.
- Con código, nombre y los cuatro valores (kcal, proteínas, hidratos y grasas) por 100 g.
- Cifras posibles: ningún macro fuera de [0, 100] ni la suma de macros y fibra por encima de 100 g,
  kcal entre 0 y 900, y kcal que cuadran con 4·P + 4·C + 9·G + 2·fibra (+ 7·alcohol + 2,4·polioles).
- Sin duplicados: con el mismo nombre normalizado, marca normalizada y kcal se queda el más escaneado.
- Si no caben todos, los más escaneados (``--max``).

Los datos de Open Food Facts no se modifican: se copian tal cual (recortados a la longitud de la
columna). Lo normalizado solo sirve para deduplicar aquí y para buscar en la API.
"""

from __future__ import annotations

import argparse
import csv
import gzip
import json
import re
import sys
import unicodedata

csv.field_size_limit(sys.maxsize if sys.maxsize < 2**31 else 2**31 - 1)

# Margen para que las kcal declaradas cuadren con las calculadas desde los macros. Las etiquetas
# redondean cada valor y algunos fabricantes calculan con factores propios: con menos margen se
# perdían productos correctos; con más, entraban los que confunden kJ y kcal (×4,18).
MARGEN_KCAL_ABSOLUTO = 20.0
MARGEN_KCAL_RELATIVO = 0.20


def numero(texto: str) -> float | None:
    """Convierte una celda numérica de la exportación; vacía o ilegible → None."""
    if not texto:
        return None
    try:
        valor = float(texto)
    except ValueError:
        return None
    return valor if valor == valor else None  # descarta NaN


def normalizar(texto: str) -> str:
    """Minúsculas, sin tildes y con un solo espacio entre palabras (solo para deduplicar)."""
    sin_tildes = unicodedata.normalize("NFD", texto.lower())
    sin_tildes = "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", " ", sin_tildes).strip()


def kcal_cuadran(kcal: float, p: float, c: float, g: float, fibra: float,
                 alcohol: float, polioles: float) -> bool:
    """¿Las kcal declaradas se parecen a las que salen de los macros?"""
    calculadas = 4 * p + 4 * c + 9 * g + 2 * fibra + 7 * alcohol + 2.4 * polioles
    margen = max(MARGEN_KCAL_ABSOLUTO, MARGEN_KCAL_RELATIVO * max(kcal, calculadas))
    return abs(kcal - calculadas) <= margen


def recortar(texto: str, maximo: int) -> str | None:
    texto = (texto or "").strip()
    if not texto:
        return None
    return texto[:maximo]


def producto_valido(fila: dict[str, str]) -> dict | None:
    """Devuelve el producto listo para la API, o None si no pasa los filtros."""
    if "en:spain" not in (fila.get("countries_tags") or ""):
        return None
    codigo = (fila.get("code") or "").strip()
    nombre = (fila.get("product_name") or "").strip()
    if not codigo or not nombre or len(codigo) > 32 or not codigo.isdigit():
        return None

    kcal = numero(fila.get("energy-kcal_100g", ""))
    p = numero(fila.get("proteins_100g", ""))
    c = numero(fila.get("carbohydrates_100g", ""))
    g = numero(fila.get("fat_100g", ""))
    if kcal is None or p is None or c is None or g is None:
        return None
    fibra = numero(fila.get("fiber_100g", ""))
    alcohol = numero(fila.get("alcohol_100g", "")) or 0.0
    polioles = numero(fila.get("polyols_100g", "")) or 0.0

    for valor in (p, c, g, fibra or 0.0):
        if valor < 0 or valor > 100:
            return None
    if p + c + g + (fibra or 0.0) > 100:
        return None
    if kcal < 0 or kcal > 900:
        return None
    if not kcal_cuadran(kcal, p, c, g, fibra or 0.0, alcohol, polioles):
        return None

    racion = numero(fila.get("serving_quantity", ""))
    if racion is not None and not (0 < racion <= 2000):
        racion = None

    return {
        "codigo": codigo,
        "nombre": recortar(nombre, 200),
        "marca": recortar((fila.get("brands") or "").split(",")[0], 100),
        "kcal": round(kcal, 1),
        "proteinas": round(p, 2),
        "carbohidratos": round(c, 2),
        "grasas": round(g, 2),
        "fibra": None if fibra is None else round(fibra, 2),
        "racionGramos": None if racion is None else round(racion, 1),
        "racionTexto": recortar(fila.get("serving_size", ""), 60),
        "envase": recortar(fila.get("quantity", ""), 60),
        "escaneos": int(numero(fila.get("unique_scans_n", "")) or 0),
        # Solo para que la API vuelva a comprobar las kcal; no se guardan.
        "alcohol": round(alcohol, 2) if alcohol else None,
        "polioles": round(polioles, 2) if polioles else None,
    }


def filtrar(origen: str) -> tuple[list[dict], dict[str, int]]:
    """Recorre la exportación y devuelve los productos válidos ya deduplicados."""
    cuentas = {"filas": 0, "espana": 0, "validos": 0, "duplicados": 0}
    por_clave: dict[tuple, dict] = {}
    with gzip.open(origen, "rt", encoding="utf-8", newline="") as f:
        lector = csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE)
        for fila in lector:
            cuentas["filas"] += 1
            if "en:spain" in (fila.get("countries_tags") or ""):
                cuentas["espana"] += 1
            producto = producto_valido(fila)
            if producto is None:
                continue
            cuentas["validos"] += 1
            clave = (normalizar(producto["nombre"]), normalizar(producto["marca"] or ""),
                     round(producto["kcal"]))
            previo = por_clave.get(clave)
            if previo is None:
                por_clave[clave] = producto
            else:
                cuentas["duplicados"] += 1
                if producto["escaneos"] > previo["escaneos"]:
                    por_clave[clave] = producto
    productos = sorted(por_clave.values(), key=lambda x: (-x["escaneos"], x["codigo"]))
    return productos, cuentas


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("origen", help="exportación CSV comprimida (.csv.gz)")
    parser.add_argument("destino", help="JSON Lines de salida")
    parser.add_argument("--max", type=int, default=0, help="como mucho N productos, los más escaneados")
    args = parser.parse_args()

    productos, cuentas = filtrar(args.origen)
    if args.max and len(productos) > args.max:
        cuentas["fuera_por_presupuesto"] = len(productos) - args.max
        productos = productos[: args.max]
    with open(args.destino, "w", encoding="utf-8") as salida:
        for producto in productos:
            salida.write(json.dumps(producto, ensure_ascii=False) + "\n")
    cuentas["escritos"] = len(productos)
    print(json.dumps(cuentas, ensure_ascii=False))


if __name__ == "__main__":
    main()
