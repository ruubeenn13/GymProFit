# Alta nueva — GP-103 (fase 1), con el movimiento de GP-104

Diseño aprobado por el propietario el 30-09-2026, en dos pasos: primero el contenido y después el movimiento.

- `fuente/*.dc.html`: el lienzo, tablero a tablero, con los valores exactos: medidas en px = dp, colores, textos y animaciones. Se lee como especificación; no se ejecuta. Las marcas `{{ … }}` y el bloque `data-dc-script` son del editor del lienzo: guardan el estado de ejemplo (qué opción está elegida) y las cifras que cuentan.
- `fuente/canvas.json`: el índice del lienzo, con el título y la posición de cada tablero.

## El recorrido

`Main` (bienvenida) → `Objetivo` → `Nivel` → `SobreTi`, con la hoja `Actividad` → `Donde` → `DiasTiempo` → `Plan` → `Guarda` (y `GuardaErrores`, sus errores) → `Avisos` → `Inicio`. `Entrar` es «Ya tengo cuenta».

- `Decisiones.dc.html`: las doce decisiones aprobadas.
- `Tecnica.dc.html`: lo que pide a la API, ya hecho en la 1.5.0.
- `Movimiento.dc.html`: los doce momentos del movimiento, con su duración, su curva y su vibración. Las `@keyframes` de cada tablero son la referencia exacta de tiempos y curvas.

## Cómo leerlo

Los datos son de ejemplo: Lucía, 29 años, 165 cm, 62 kg, actividad moderada, quiere ganar músculo, es intermedia, entrena en el gimnasio 3 días de 45 min. Las cifras del plan salen de `CalculadoraNutricional` con esas respuestas, y las rutinas, de la vista previa de la API.

Los colores salen de la paleta de la app: si alguno no coincide con un token, **manda el token** (sus contrastes están medidos, GP-063). El tema claro sale de los mismos tokens.

Lo que se ve en el lienzo y no va en la fase 1: el botón «Continuar con Google», que es la fase 2 (GP-086). La tarjeta del fundador de Inicio lleva el texto aprobado.
