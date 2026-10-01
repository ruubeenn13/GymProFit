"""Genera la lista de alimentos básicos (GP-127) y su migración a partir de las descargas oficiales.

Entradas versionadas (lo que se revisa a mano):
    seleccion.csv  — qué alimento de cada fuente entra, con su nombre en ES y EN y su categoría.
    raciones.csv   — sus raciones, con el peso y de dónde sale cada peso.

Entradas descargadas (NO se versionan; ver README.md):
    Ciqual 2025 (ANSES), «Table Ciqual 2025_FR_2025_11_03.xlsx», Licence Ouverte / Etalab 2.0.
    USDA FoodData Central, SR Legacy (abril de 2018) en CSV, dominio público (CC0).

Salidas:
    basicos.csv    — la lista curada con los valores por 100 g, la fuente y el código de origen.
    ../../src/main/resources/db/migration/<versión>__Alimentos_basicos.sql

Uso:
    python generar_basicos.py --ciqual ciqual2025.xlsx --usda FoodData_Central_sr_legacy_food_csv_2018-04 \
        --migracion V202610011900__Alimentos_basicos.sql

Necesita pandas y openpyxl, solo para leer el Excel de Ciqual; la API no depende de esto.
"""

from __future__ import annotations

import argparse
import csv
import math
import re
import sys
from pathlib import Path

import pandas as pd

AQUI = Path(__file__).resolve().parent
MIGRACIONES = AQUI.parent.parent / "src" / "main" / "resources" / "db" / "migration"

CATEGORIAS_EN = {
    "Carnes y aves": "Meat & poultry",
    "Pescado y marisco": "Fish & seafood",
    "Huevos": "Eggs",
    "Lácteos": "Dairy",
    "Legumbres": "Legumes",
    "Cereales y pan": "Cereals & bread",
    "Frutas": "Fruits",
    "Verduras": "Vegetables",
    "Frutos secos": "Nuts",
    "Aceites y grasas": "Fats",
    "Bebidas": "Drinks",
    "Suplementos": "Supplements",
    "Snacks": "Snacks",
    "Otro": "Other",
}

# Margen de la comprobación de kcal. Es el mismo que usa BasicosCsvTest: si se cambia aquí, allí.
MARGEN_ABSOLUTO = 12.0
MARGEN_RELATIVO = 0.08

# Columnas del Excel de Ciqual 2025, por posición: los encabezados llevan saltos de línea y
# caracteres que cambian según cómo se abra el fichero.
COL_CODIGO, COL_NOMBRE = 6, 7
COL_KCAL = 10            # Energie, Règlement UE N° 1169/2011 (kcal/100 g)
COL_PROT_JONES = 14      # Protéines, N x facteur de Jones
COL_PROT_625 = 15        # Protéines, N x 6.25 (la del etiquetado UE; con ella cuadran las kcal)
COL_GLUCIDES, COL_LIPIDES, COL_FIBRES = 16, 17, 26
COL_POLYOLS, COL_ALCOOL, COL_ACIDES = 27, 29, 30

USDA_KCAL, USDA_PROT, USDA_CARB, USDA_FAT, USDA_FIBRA, USDA_ALCOHOL = 1008, 1003, 1005, 1004, 1079, 1018


def valor_ciqual(texto) -> float | None:
    """Lee una celda de Ciqual: «traces» es 0; «< x» es la mitad del límite; «-» es que no hay dato."""
    if texto is None or (isinstance(texto, float) and math.isnan(texto)):
        return None
    texto = str(texto).strip().replace(",", ".")
    if texto in ("", "-"):
        return None
    if texto.lower() == "traces":
        return 0.0
    menor = re.match(r"^<\s*([\d.]+)$", texto)
    if menor:
        return float(menor.group(1)) / 2
    return float(texto)


def leer_ciqual(ruta: Path) -> dict[str, dict]:
    tabla = pd.read_excel(ruta, sheet_name=0, dtype=str)
    columnas = tabla.columns
    datos = {}
    for _, fila in tabla.iterrows():
        prot = valor_ciqual(fila[columnas[COL_PROT_625]])
        if prot is None:
            prot = valor_ciqual(fila[columnas[COL_PROT_JONES]])
        datos[str(fila[columnas[COL_CODIGO]]).strip()] = {
            "nombre_origen": str(fila[columnas[COL_NOMBRE]]).replace("\n", " ").strip(),
            "kcal": valor_ciqual(fila[columnas[COL_KCAL]]),
            "proteinas": prot,
            "carbohidratos": valor_ciqual(fila[columnas[COL_GLUCIDES]]),
            "grasas": valor_ciqual(fila[columnas[COL_LIPIDES]]),
            "fibra": valor_ciqual(fila[columnas[COL_FIBRES]]),
            "alcohol": valor_ciqual(fila[columnas[COL_ALCOOL]]) or 0.0,
            "polioles": valor_ciqual(fila[columnas[COL_POLYOLS]]) or 0.0,
            "acidos": valor_ciqual(fila[columnas[COL_ACIDES]]) or 0.0,
        }
    return datos


def leer_usda(carpeta: Path) -> tuple[dict[str, dict], dict[str, list]]:
    alimentos = pd.read_csv(carpeta / "food.csv", usecols=["fdc_id", "description"])
    nutrientes = pd.read_csv(carpeta / "food_nutrient.csv", usecols=["fdc_id", "nutrient_id", "amount"])
    nutrientes = nutrientes[nutrientes.nutrient_id.isin(
        [USDA_KCAL, USDA_PROT, USDA_CARB, USDA_FAT, USDA_FIBRA, USDA_ALCOHOL])]
    por_id: dict[str, dict] = {}
    for fila in alimentos.itertuples():
        por_id[str(fila.fdc_id)] = {"nombre_origen": fila.description, "kcal": None, "proteinas": None,
                                    "carbohidratos": None, "grasas": None, "fibra": None,
                                    "alcohol": 0.0, "polioles": 0.0, "acidos": 0.0}
    clave = {USDA_KCAL: "kcal", USDA_PROT: "proteinas", USDA_CARB: "carbohidratos",
             USDA_FAT: "grasas", USDA_FIBRA: "fibra", USDA_ALCOHOL: "alcohol"}
    for fila in nutrientes.itertuples():
        registro = por_id.get(str(fila.fdc_id))
        if registro is not None and not math.isnan(fila.amount):
            registro[clave[fila.nutrient_id]] = float(fila.amount)
    porciones = pd.read_csv(carpeta / "food_portion.csv", usecols=["fdc_id", "gram_weight"])
    pesos: dict[str, list] = {}
    for fila in porciones.itertuples():
        pesos.setdefault(str(fila.fdc_id), []).append(float(fila.gram_weight))
    return por_id, pesos


def leer_csv(nombre: str) -> list[dict]:
    with open(AQUI / nombre, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter=";"))


def nota_kcal(valores: dict, kcal: int) -> str:
    """Si las kcal no cuadran con 4·P + 4·C + 9·G + 2·fibra, dice por qué; si cuadran, vacío."""
    calculadas = (4 * valores["proteinas"] + 4 * valores["carbohidratos"] + 9 * valores["grasas"]
                  + 2 * (valores["fibra"] or 0))
    if abs(kcal - calculadas) <= max(MARGEN_ABSOLUTO, MARGEN_RELATIVO * max(kcal, calculadas)):
        return ""
    motivos = []
    if valores["alcohol"] > 0.5:
        motivos.append(f"alcohol {valores['alcohol']:g} g/100 g a 7 kcal/g")
    if valores["polioles"] > 0.5:
        motivos.append(f"polioles {valores['polioles']:g} g/100 g a 2,4 kcal/g")
    if valores["acidos"] > 0.5:
        motivos.append(f"ácidos orgánicos {valores['acidos']:g} g/100 g a 3 kcal/g")
    return " y ".join(motivos) if motivos else "SIN EXPLICAR"


def sql_texto(texto: str | None) -> str:
    if texto is None:
        return "NULL"
    return "'" + texto.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_numero(valor: float | None) -> str:
    return "NULL" if valor is None else f"{valor:.2f}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ciqual", required=True, type=Path)
    parser.add_argument("--usda", required=True, type=Path)
    parser.add_argument("--migracion", required=True, help="nombre del fichero de la migración")
    args = parser.parse_args()

    ciqual = leer_ciqual(args.ciqual)
    usda, pesos_usda = leer_usda(args.usda)
    errores: list[str] = []

    basicos = []
    for fila in leer_csv("seleccion.csv"):
        origen = (ciqual if fila["fuente"] == "CIQUAL" else usda).get(fila["codigo"])
        if origen is None:
            errores.append(f"{fila['fuente']} {fila['codigo']}: no existe en la descarga")
            continue
        valores = dict(origen)
        if any(valores[c] is None for c in ("proteinas", "carbohidratos", "grasas")):
            errores.append(f"{fila['fuente']} {fila['codigo']} ({fila['nombre_es']}): faltan macros")
            continue
        if valores["kcal"] is None:
            errores.append(f"{fila['fuente']} {fila['codigo']} ({fila['nombre_es']}): faltan kcal")
            continue
        kcal = round(valores["kcal"])
        nota = nota_kcal(valores, kcal)
        if nota == "SIN EXPLICAR" and (fila.get("nota_kcal") or "").strip():
            nota = fila["nota_kcal"].strip()  # la justificación la escribe quien revisa
        if nota == "SIN EXPLICAR":
            errores.append(f"{fila['fuente']} {fila['codigo']} ({fila['nombre_es']}): kcal {kcal} no cuadran")
        basicos.append({
            "fuente": fila["fuente"], "codigo": fila["codigo"], "categoria": fila["categoria"],
            "nombre_es": fila["nombre_es"], "nombre_en": fila["nombre_en"],
            "kcal": kcal,
            "proteinas": round(valores["proteinas"], 2), "carbohidratos": round(valores["carbohidratos"], 2),
            "grasas": round(valores["grasas"], 2),
            "fibra": None if valores["fibra"] is None else round(valores["fibra"], 2),
            "habitual": fila["habitual"], "excepcion_kcal": nota,
            "nombre_origen": valores["nombre_origen"],
        })

    claves = {(b["fuente"], b["codigo"]) for b in basicos}
    raciones = leer_csv("raciones.csv")
    for racion in raciones:
        if (racion["fuente"], racion["codigo"]) not in claves:
            errores.append(f"ración de {racion['fuente']} {racion['codigo']} sin básico")
        cita = re.match(r"^USDA FDC (\d+)", racion["fuente_peso"])
        if cita and cita.group(1) not in usda:
            errores.append(f"ración {racion['nombre_es']}: la cita USDA FDC {cita.group(1)} no existe")
        elif cita and cita.group(1) not in pesos_usda:
            errores.append(f"ración {racion['nombre_es']}: USDA FDC {cita.group(1)} no tiene porciones")

    if errores:
        print("\n".join(errores), file=sys.stderr)
        sys.exit(1)

    campos = ["fuente", "codigo", "categoria", "nombre_es", "nombre_en", "kcal", "proteinas",
              "carbohidratos", "grasas", "fibra", "habitual", "excepcion_kcal", "nombre_origen"]
    with open(AQUI / "basicos.csv", "w", encoding="utf-8", newline="") as f:
        escritor = csv.DictWriter(f, fieldnames=campos, delimiter=";", lineterminator="\n")
        escritor.writeheader()
        for b in basicos:
            escritor.writerow({**b, "fibra": "" if b["fibra"] is None else b["fibra"]})

    escribir_migracion(MIGRACIONES / args.migracion, basicos, raciones)
    print(f"{len(basicos)} básicos y {len(raciones)} raciones")


def escribir_migracion(ruta: Path, basicos: list[dict], raciones: list[dict]) -> None:
    """Una sola sentencia por tabla: idempotente por (fuente, código) y rápida en el arranque."""
    lineas = [
        "-- GP-127 · Alimentos básicos: Ciqual 2025 (ANSES, Licence Ouverte / Etalab 2.0) y",
        "-- USDA FoodData Central SR Legacy (dominio público). GENERADO por",
        "-- datos/basicos/generar_basicos.py desde datos/basicos/basicos.csv: no se edita a mano.",
        "-- Idempotente por (fuente, codigo_origen): volver a pasarla no duplica nada.",
        "",
        "INSERT INTO alimentos (nombre, nombre_en, categoria, categoria_en, calorias, proteinas,",
        "                       carbohidratos, grasas, fibra, porcion_gramos, activo, fuente,",
        "                       codigo_origen, revisado)",
        "VALUES",
    ]
    filas = []
    for b in basicos:
        filas.append("    (" + ", ".join([
            sql_texto(b["nombre_es"]), sql_texto(b["nombre_en"]), sql_texto(b["categoria"]),
            sql_texto(CATEGORIAS_EN[b["categoria"]]), str(b["kcal"]), sql_numero(b["proteinas"]),
            sql_numero(b["carbohidratos"]), sql_numero(b["grasas"]), sql_numero(b["fibra"]),
            "100", "1", sql_texto(b["fuente"]), sql_texto(b["codigo"]), "1",
        ]) + ")")
    lineas.append(",\n".join(filas))
    lineas.append("ON DUPLICATE KEY UPDATE id = id;")
    lineas.append("")
    lineas.append("INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden)")
    lineas.append("SELECT a.id, r.nombre, r.nombre_en, r.gramos, r.fuente_peso, r.orden")
    lineas.append("FROM alimentos a")
    lineas.append("JOIN (")
    orden: dict[tuple, int] = {}
    selects = []
    for r in raciones:
        clave = (r["fuente"], r["codigo"])
        orden[clave] = orden.get(clave, 0) + 1
        selects.append("    SELECT " + ", ".join([
            sql_texto(r["fuente"]) + " AS f", sql_texto(r["codigo"]) + " AS c",
            sql_texto(r["nombre_es"]) + " AS nombre", sql_texto(r["nombre_en"]) + " AS nombre_en",
            f"{float(r['gramos']):.1f} AS gramos", sql_texto(r["fuente_peso"]) + " AS fuente_peso",
            f"{orden[clave]} AS orden",
        ]))
    lineas.append("\n    UNION ALL\n".join(selects))
    lineas.append(") r ON a.fuente = r.f AND a.codigo_origen = r.c")
    lineas.append("ON DUPLICATE KEY UPDATE alimento_raciones.gramos = alimento_raciones.gramos;")
    ruta.write_text("\n".join(lineas) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
