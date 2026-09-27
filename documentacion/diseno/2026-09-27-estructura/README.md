# Estructura nueva — barra B, cuatro pestañas y Ajustes (GP-105)

Diseño aprobado por el propietario el 27-09-2026, con la **barra B** (el «+» central de acciones rápidas).

- `01-inicio.png` … `07-mapa-de-donde-va-cada-cosa.png`: la referencia visual. Tema oscuro, a 2× (1 px del lienzo = 1 dp).
- `06-barra-b-abierta.png`: el «+» abierto sobre Inicio.
- `07-mapa…png`: a dónde va cada opción de la app actual. Ninguna se pierde.
- `fuente/*.dc.html`: el lienzo en HTML, con los valores exactos (medidas en px = dp, colores, textos). Se lee como especificación; no se ejecuta.

Los datos son de ejemplo. Los colores del lienzo salen de la paleta de la app: si alguno no coincide con un token, **manda el token** (sus contrastes están medidos, GP-063). El tema claro sale de los mismos tokens.
