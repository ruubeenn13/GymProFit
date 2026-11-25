"""Mide /un-dia en Chromium: el primer layout por escena con la CPU a ×4, y de dónde sale el CLS.

Fase 1 de «Un día» (web/PLAN-UN-DIA.md §4). Necesita Python con Playwright y su Chromium, y la web
construida y servida en local (nunca producción):

    cd web && npm run build && cd ..
    python scripts/servir-web.py 8207 &
    python scripts/medir-un-dia.py --url http://127.0.0.1:8207 > medida.md

Qué hace:

· Layout por escena. Carga /un-dia en un móvil emulado (393 × 760 @2,75, táctil), caché vacía,
  CPU ×4 por CDP, sin JavaScript (el primer layout es anterior a los módulos y así ninguna escena
  quitada rompe un script), con una traza de devtools.timeline. Apunta el primer «Layout» de la
  página (no el de about:blank), su primer recálculo de estilos y la suma de sus Layout hasta
  1,5 s después del load. Lo repite con la página
  recortada: vacía, y añadiendo una pieza cada vez (luz y reloj gigante, escenario del móvil,
  entrada, transición, entreno, cierre); la diferencia entre dos pasos es lo que añade esa pieza.
  Dos variantes más, para preparar la contención de la fase 3 (tope: ≤ 130 ms la página entera):
  content-visibility: auto en las escenas que no se ven al cargar, con la altura que ya les da el
  CSS como contain-intrinsic-size; y content-visibility: hidden en las capas del móvil que no se
  ven al cargar (Hoy toca, la sesión, el descanso) y en las fugas, que el movimiento destaparía
  justo antes de enseñarlas. Las capas van con position: absolute e inset: 0, así que contener
  su tamaño no cambia ninguna caja: sin CLS por construcción.
· CLS. Con JavaScript y en modo normal, a 417 × 834 @3,25 (el Xiaomi 17T) y a 1920 × 1080: cada
  layout-shift con sus nodos, su caja de antes y de después y el momento, durante la carga, la
  entrada y un recorrido entero de la página a saltos de 1/3 de pantalla.

Escribe Markdown por la salida estándar, con la mediana y las pasadas, para pegarlo en el informe.
"""
import argparse
import json
import re
import statistics
import sys

from playwright.sync_api import sync_playwright

MOVIL = dict(viewport={"width": 393, "height": 760}, device_scale_factor=2.75, is_mobile=True, has_touch=True)
XIAOMI = dict(viewport={"width": 417, "height": 834}, device_scale_factor=3.25, is_mobile=True, has_touch=True)
ESCRITORIO = dict(viewport={"width": 1920, "height": 1080}, device_scale_factor=1)

# Piezas del HTML de dist, por la marca con la que empiezan; cada una acaba donde empieza la siguiente.
PIEZAS = [
    ("luz", '<div class="luz', '<div class="reloj-dia'),
    ("reloj gigante", '<div class="reloj-dia', "<header"),
    ("escenario (móvil y fugas)", '<div class="escenario', '<section class="hora entrada'),
    ("entrada", '<section class="hora entrada', '<section class="hora transicion'),
    ("transición", '<section class="hora transicion', '<section class="hora entreno'),
    ("entreno", '<section class="hora entreno', '<section class="hora cierre'),
    ("cierre", '<section class="hora cierre', "</main>"),
]

# Las escenas que no se ven al cargar, con su altura del CSS del viaje (un-dia.css): la transición
# mide 220svh de pista; el entreno, 640svh (sube 100svh con su margen negativo); el cierre sale de
# su contenido, y con «auto» el navegador recuerda la altura real en cuanto lo pinta una vez.
CONTENCION = """<style id="contencion">
@media (prefers-reduced-motion: no-preference) and (scripting: enabled) {
  .transicion, .entreno, .cierre { content-visibility: auto; }
  .transicion { contain-intrinsic-size: auto none auto 220svh; }
  .entreno { contain-intrinsic-size: auto none auto 640svh; }
  .cierre { contain-intrinsic-size: auto none auto 560px; }
}
</style>"""

OBSERVAR_CLS = """
window.__cambios = [];
new PerformanceObserver((l) => {
  for (const e of l.getEntries()) {
    if (e.hadRecentInput) continue;
    const quien = (n) => !n ? '(quitado)' : n.nodeType === 3 ? '#texto en ' + quien(n.parentElement)
      : n.tagName.toLowerCase() + (n.id ? '#' + n.id : '') + (n.className && typeof n.className === 'string'
        ? '.' + n.className.trim().split(/\\s+/).join('.') : '');
    const caja = (r) => `${Math.round(r.x)},${Math.round(r.y)} ${Math.round(r.width)}×${Math.round(r.height)}`;
    window.__cambios.push({ t: Math.round(e.startTime), valor: e.value, y: scrollY,
      fuentes: (e.sources || []).map((s) => `${quien(s.node)} ${caja(s.previousRect)} → ${caja(s.currentRect)}`) });
  }
}).observe({ type: 'layout-shift', buffered: true });
"""


# Las capas del móvil y las fugas que no se ven al cargar (la de arranque las tapa).
CAPAS_OCULTAS = """<style id="capas-ocultas">
@media (prefers-reduced-motion: no-preference) and (scripting: enabled) {
  .capa-hoy, .capa-sesion, .capa-descanso, .fugas { content-visibility: hidden; }
}
</style>"""

ES_LA_PAGINA = re.compile(r"/un-dia(\?|$)")


def recortar(html: str, quedan: set[str]) -> str:
    """El HTML de /un-dia sin las piezas que no están en «quedan»."""
    # Los cortes se buscan en el original: la marca de fin de una pieza es el principio de la
    # siguiente, que puede haberse quitado ya.
    cortes = [(html.index(desde), html.index(hasta)) for nombre, desde, hasta in PIEZAS if nombre not in quedan]
    for i, j in sorted(cortes, reverse=True):
        html = html[:i] + html[j:]
    return html


def layouts(traza: bytes) -> tuple[float, float, float]:
    """Primer Layout y primer UpdateLayoutTree (estilos) de la página, y la suma de sus Layout (ms).

    Solo los del marco de /un-dia (el del firstContentfulPaint): antes de que llegue el documento
    el renderizador hace un layout de about:blank que no cuenta.
    """
    eventos = json.loads(traza)["traceEvents"]
    marcos = {e["args"]["frame"] for e in eventos if e.get("name") == "firstContentfulPaint"}
    de_la_pagina = lambda nombre: sorted(
        (e for e in eventos if e.get("name") == nombre and e.get("ph") == "X"
         and e.get("args", {}).get("beginData", {}).get("frame") in marcos), key=lambda e: e["ts"])
    lista = de_la_pagina("Layout")
    estilos = de_la_pagina("UpdateLayoutTree")
    if not lista:
        return 0.0, 0.0, 0.0
    return lista[0]["dur"] / 1000, (estilos[0]["dur"] / 1000 if estilos else 0.0), sum(e["dur"] for e in lista) / 1000


def una_carga(navegador, url: str, html: str | None) -> tuple[float, float, float]:
    contexto = navegador.new_context(**MOVIL)
    pagina = contexto.new_page()
    pagina.route("**/*.js", lambda r: r.abort())
    if html is not None:
        pagina.route(ES_LA_PAGINA, lambda r: r.fulfill(status=200, content_type="text/html; charset=utf-8", body=html))
    cdp = contexto.new_cdp_session(pagina)
    cdp.send("Emulation.setCPUThrottlingRate", {"rate": 4})
    navegador.start_tracing(page=pagina, categories=["devtools.timeline", "disabled-by-default-devtools.timeline"])
    pagina.goto(url + "/un-dia", wait_until="load")
    pagina.wait_for_timeout(1500)
    traza = navegador.stop_tracing()
    contexto.close()
    return layouts(traza)


def medir_layouts(navegador, url: str, pasadas: int) -> None:
    original = navegador.new_context().request.get(url + "/un-dia").text()
    pasos = [("vacía (cabecera y pie)", set())]
    acumulado: set[str] = set()
    for nombre, _, _ in PIEZAS:
        acumulado = acumulado | {nombre}
        pasos.append(("+ " + nombre, set(acumulado)))
    completa = original
    variantes = [(n, recortar(original, q)) for n, q in pasos]
    variantes.append(("completa + content-visibility: auto en las escenas", completa.replace("</head>", CONTENCION + "</head>")))
    variantes.append(("completa + content-visibility: hidden en las capas ocultas",
                      completa.replace("</head>", CAPAS_OCULTAS + "</head>")))
    variantes.append(("completa + las dos", completa.replace("</head>", CONTENCION + CAPAS_OCULTAS + "</head>")))

    print(f"## Primer layout de /un-dia, CPU ×4, 393 × 760 @2,75, caché vacía, sin JS ({pasadas} pasadas)\n")
    print("| Página | Primer layout (mediana) | Añade | Pasadas | Primer recálculo de estilos | Layout total hasta load + 1,5 s |")
    print("|---|---|---|---|---|---|")
    anterior = None
    for nombre, html in variantes:
        r = [una_carga(navegador, url, html) for _ in range(pasadas)]
        primero = statistics.median(x[0] for x in r)
        estilos = statistics.median(x[1] for x in r)
        total = statistics.median(x[2] for x in r)
        anade = "" if anterior is None or nombre.startswith("completa +") else f"{primero - anterior:+.0f} ms"
        if nombre.startswith("completa +"):
            anade = f"{primero - anterior:+.0f} ms frente a la completa"
        else:
            anterior = primero
        print(f"| {nombre} | {primero:.0f} ms | {anade} | {', '.join(f'{x[0]:.0f}' for x in r)} | {estilos:.0f} ms | {total:.0f} ms |")
        sys.stdout.flush()
    print()


def medir_cls(navegador, url: str, nombre: str, opciones: dict, extra: str = "", html: str | None = None) -> None:
    contexto = navegador.new_context(**opciones)
    pagina = contexto.new_page()
    if html is not None:
        pagina.route(ES_LA_PAGINA, lambda r: r.fulfill(status=200, content_type="text/html; charset=utf-8", body=html))
    pagina.add_init_script(OBSERVAR_CLS)
    pagina.goto(url + "/un-dia" + extra, wait_until="load")
    pagina.wait_for_timeout(3000)
    alto = pagina.evaluate("document.documentElement.scrollHeight")
    paso = opciones["viewport"]["height"] // 3
    y = 0
    while y < alto:
        y += paso
        pagina.evaluate(f"scrollTo(0, {y})")
        pagina.wait_for_timeout(120)
        alto = pagina.evaluate("document.documentElement.scrollHeight")
    pagina.wait_for_timeout(1000)
    cambios = pagina.evaluate("window.__cambios")
    contexto.close()
    total = sum(c["valor"] for c in cambios)
    print(f"### CLS {nombre}{' ' + extra if extra else ''}: {total:.4f} ({len(cambios)} cambios)\n")
    for c in cambios:
        print(f"- {c['t']} ms, scrollY {c['y']}, {c['valor']:.5f}")
        for f in c["fuentes"]:
            print(f"  - {f}")
    print()


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    argumentos = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    argumentos.add_argument("--url", default="http://127.0.0.1:8207")
    argumentos.add_argument("--pasadas", type=int, default=5)
    argumentos.add_argument("--solo", choices=["layout", "cls"])
    a = argumentos.parse_args()
    if re.search(r"gymprofit\.app|pages\.dev", a.url):
        sys.exit("solo contra el servidor local")
    with sync_playwright() as p:
        navegador = p.chromium.launch()
        if a.solo != "cls":
            medir_layouts(navegador, a.url, a.pasadas)
        if a.solo != "layout":
            print("## CLS con JavaScript, modo normal (carga, entrada y recorrido entero)\n")
            medir_cls(navegador, a.url, "Xiaomi 417 × 834 @3,25", XIAOMI, "?debug=motion")
            medir_cls(navegador, a.url, "escritorio 1920 × 1080", ESCRITORIO, "?debug=motion")
            original = navegador.new_context().request.get(a.url + "/un-dia").text()
            contenida = original.replace("</head>", CONTENCION + "</head>")
            medir_cls(navegador, a.url, "Xiaomi, content-visibility: auto en las escenas", XIAOMI, "?debug=motion", contenida)
            medir_cls(navegador, a.url, "escritorio, content-visibility: auto en las escenas", ESCRITORIO, "?debug=motion", contenida)
        navegador.close()


if __name__ == "__main__":
    main()
