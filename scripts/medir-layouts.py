"""Mide los layouts y los estilos de la app: padding, margin, cornerRadius, elevation y textSize.

Solo lee: recorre app/GymProFit/app/src/main/res (layout*, drawable*, values*, menu, xml, animator)
y cuenta, por propiedad, cada valor distinto, cuántas veces sale y dónde. Separa lo que sale de
dimens.xml (referencia @dimen/...) de lo escrito a mano (literal en dp o sp) y de lo que viene del
tema (?attr/...). Escribe una tabla en Markdown por propiedad y un resumen al principio.

Uso, desde la raíz del repositorio:
    python scripts/medir-layouts.py > medidas.md
(GP-216; no toca nada del código del producto).
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent / "app" / "GymProFit" / "app" / "src" / "main" / "res"

# Propiedad que se mide -> nombres de atributo (sin prefijo android: ni app:) que la forman
PROPIEDADES = {
    "padding": ["padding", "paddingStart", "paddingEnd", "paddingLeft", "paddingRight", "paddingTop",
                "paddingBottom", "paddingHorizontal", "paddingVertical", "contentPadding",
                "contentPaddingStart", "contentPaddingEnd", "contentPaddingTop", "contentPaddingBottom"],
    "margin": ["layout_margin", "layout_marginStart", "layout_marginEnd", "layout_marginLeft",
               "layout_marginRight", "layout_marginTop", "layout_marginBottom", "layout_marginHorizontal",
               "layout_marginVertical"],
    "cornerRadius": ["cornerRadius", "cardCornerRadius", "chipCornerRadius", "trackCornerRadius", "radius",
                     "topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius",
                     "cornerSize", "dialogCornerRadius", "boxCornerRadiusTopStart", "boxCornerRadiusTopEnd",
                     "boxCornerRadiusBottomStart", "boxCornerRadiusBottomEnd"],
    "elevation": ["elevation", "cardElevation", "translationZ", "popupElevation", "chipElevation"],
    "textSize": ["textSize"],
}
ATRIBUTO_A_PROPIEDAD = {a: p for p, nombres in PROPIEDADES.items() for a in nombres}

# android:padding="8dp"  /  app:cardCornerRadius="20dp"  (en layouts y drawables)
RE_ATRIBUTO = re.compile(r'(?:android|app):(\w+)\s*=\s*"([^"]*)"')
# <item name="android:padding">8dp</item>  (en values: estilos y temas)
RE_ITEM = re.compile(r'<item\s+name="(?:android:)?(\w+)"[^>]*>([^<]*)</item>')
RE_DIMEN = re.compile(r'<dimen\s+name="(\w+)">([^<]*)</dimen>')


def ficheros():
    for carpeta in sorted(RAIZ.iterdir()):
        if carpeta.is_dir() and carpeta.name.split("-")[0] in {"layout", "drawable", "values", "menu", "xml",
                                                                 "animator", "color"}:
            yield from sorted(carpeta.glob("*.xml"))


def clasifica(valor: str) -> str:
    valor = valor.strip()
    if valor.startswith("@dimen/"):
        return "dimens.xml"
    if valor.startswith("?"):
        return "tema"
    if valor.startswith("@"):
        return "otro recurso"
    if re.fullmatch(r"-?\d+(\.\d+)?(dp|sp|dip|px)?", valor) or valor.endswith("%"):
        return "a mano"
    return "otro"


def main() -> None:
    dimens = {}
    for f in RAIZ.glob("values*/dimens.xml"):
        for nombre, valor in RE_DIMEN.findall(f.read_text(encoding="utf-8")):
            dimens.setdefault(nombre, valor.strip())

    # propiedad -> valor -> lista de (fichero, línea)
    datos = defaultdict(lambda: defaultdict(list))
    for f in ficheros():
        rel = f.relative_to(RAIZ).as_posix()
        for n, linea in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
            for atributo, valor in RE_ATRIBUTO.findall(linea):
                p = ATRIBUTO_A_PROPIEDAD.get(atributo)
                # android:radius también es el de un ripple o un degradado: solo cuenta el de <corners>
                if atributo == "radius" and "<corners" not in linea:
                    continue
                if p:
                    datos[p][valor.strip()].append(f"{rel}:{n}")
            for atributo, valor in RE_ITEM.findall(linea):
                p = ATRIBUTO_A_PROPIEDAD.get(atributo)
                if p:
                    datos[p][valor.strip()].append(f"{rel}:{n}")

    print("## Medidas de layouts, drawables y estilos (GP-216)\n")
    print("Generado por `scripts/medir-layouts.py` sobre `res/` (layout, drawable, values, menu, xml, animator).")
    print("«a mano» = un literal escrito en el XML; «dimens.xml» = referencia `@dimen/…`; «tema» = `?attr/…`.\n")

    print("### Resumen\n")
    print("| Propiedad | Valores distintos | Usos | De `dimens.xml` | A mano | Del tema / otros |")
    print("|---|---|---|---|---|---|")
    for p in PROPIEDADES:
        usos = sum(len(v) for v in datos[p].values())
        por = defaultdict(int)
        for valor, sitios in datos[p].items():
            por[clasifica(valor)] += len(sitios)
        distintos_a_mano = len([v for v in datos[p] if clasifica(v) == "a mano"])
        print(f"| {p} | {len(datos[p])} ({distintos_a_mano} a mano) | {usos} | {por['dimens.xml']} | {por['a mano']} | "
              f"{por['tema'] + por['otro recurso'] + por['otro']} |")
    print()

    for p in PROPIEDADES:
        print(f"### {p}\n")
        print("| Valor | Origen | Veces | Dónde (primeros usos) |")
        print("|---|---|---|---|")

        def clave(item):
            valor = item[0]
            m = re.match(r"-?\d+(\.\d+)?", valor)
            return (clasifica(valor) != "a mano", float(m.group()) if m and clasifica(valor) == "a mano" else 0, valor)

        for valor, sitios in sorted(datos[p].items(), key=clave):
            resuelto = ""
            if valor.startswith("@dimen/"):
                resuelto = f" = {dimens.get(valor[7:], '?')}"
            donde = ", ".join(f"`{s}`" for s in sitios[:4]) + (f" … (+{len(sitios) - 4})" if len(sitios) > 4 else "")
            print(f"| `{valor}`{resuelto} | {clasifica(valor)} | {len(sitios)} | {donde} |")
        print()

    sin_dimen = []
    for p in PROPIEDADES:
        for valor, sitios in datos[p].items():
            if clasifica(valor) == "a mano":
                sin_dimen.append((p, valor, len(sitios)))
    print("### Lo que no sale de `dimens.xml`\n")
    print("Los valores literales («a mano») de arriba, de más a menos usados, por propiedad:\n")
    print("| Propiedad | Valor | Veces |")
    print("|---|---|---|")
    for p, valor, n in sorted(sin_dimen, key=lambda x: (x[0], -x[2])):
        print(f"| {p} | `{valor}` | {n} |")
    print("\n`dimens.xml` define:", ", ".join(f"`{k}` {v}" for k, v in sorted(dimens.items())) + ".")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
