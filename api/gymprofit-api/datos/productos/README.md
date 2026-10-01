# Productos de España (GP-164)

Los productos que se venden en España, de la exportación de Open Food Facts, en la tabla `productos_off`. Son el grupo «Productos» de la búsqueda y la primera parada de la búsqueda por código (DEC-040, DEC-041).

## Cómo llegan

Cada lunes, el workflow `.github/workflows/importar-productos.yml` (también se lanza a mano):

1. descarga la exportación completa en CSV (`en.openfoodfacts.org.products.csv.gz`, ~1,3 GB);
2. `filtrar_off.py` la recorre sin descomprimirla a disco y deja un JSON Lines con los válidos;
3. `importar_productos.py` los manda a `POST /importacion/productos` en lotes de 1000, con la clave de la cabecera `X-Clave-Importacion` (secreto `IMPORTACION_CLAVE` del repositorio, el mismo valor que la variable de Render);
4. al acabar llama a `POST /importacion/productos/fin`, que devuelve cuántos productos hay y cuánto ocupa la tabla.

La API nunca descarga nada ni corre la importación dentro: solo recibe lotes (DEC-035). Los dos scripts usan solo la biblioteca estándar de Python.

## Qué entra

- Vendido en España (`countries_tags` contiene `en:spain`).
- Con código numérico, nombre y los cuatro valores por 100 g: kcal, proteínas, hidratos y grasas.
- Cifras posibles: ningún macro por debajo de 0 ni por encima de 100 g, la suma de macros y fibra como mucho 100 g, kcal entre 0 y 900, y kcal que cuadran con 4·P + 4·C + 9·G + 2·fibra + 7·alcohol + 2,4·polioles (margen: 20 kcal o el 20 %; con menos se pierden productos buenos, con más entran los que confunden kJ y kcal).
- Sin duplicados: con el mismo nombre normalizado, marca y kcal, se queda el más escaneado.
- Si no cupieran todos en el presupuesto de 200 MB, los más escaneados (`--max`).

La API vuelve a comprobar las mismas reglas en cada lote (`ProductoOffValidacion`).

## Medidas (2026-10-01, exportación del mismo día)

| | |
|---|---|
| Filas de la exportación | 4 535 553 |
| Vendidos en España | 354 697 |
| Válidos | 238 026 |
| Duplicados quitados | 39 794 |
| Mandados a la API | 198 232 |
| Guardados (la API descartó 8) | 198 224 |
| Tamaño en MariaDB local | 19,5 MB de datos + 9,5 MB de índices |
| Filtrar | ~3 min |
| Mandar a la API local | 100 s |

Caben todos con mucho margen: no hace falta `--max`. La medida en producción está en el informe del lote 1.6.0.

## Licencia

Los datos son de Open Food Facts, bajo la **Open Database License (ODbL)**: se cita la fuente (la app y la web lo hacen) y se ofrecen las mejoras que se hagan sobre la base. `productos_off` y las filas de `alimentos` con `fuente = 'OFF'` derivan de Open Food Facts; los datos se copian tal cual, y lo normalizado solo existe en la memoria de la API para buscar.

## Probar en local

```bash
curl -L -o off.csv.gz https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz
python filtrar_off.py off.csv.gz productos.jsonl
IMPORTACION_CLAVE=clave-de-desarrollo-solo-en-local-0123456789 \
  python importar_productos.py productos.jsonl http://localhost:8080/api
```
