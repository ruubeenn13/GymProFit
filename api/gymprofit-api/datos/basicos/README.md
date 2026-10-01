# Alimentos básicos (GP-127)

Unos 450 alimentos genéricos de los que más se comen en España, con sus valores por 100 g y sus raciones. Son el grupo «Básicos» de la búsqueda (DEC-040).

## Ficheros

| Fichero | Qué es | Se edita a mano |
|---|---|---|
| `seleccion.csv` | Qué alimento de cada fuente entra: fuente, código, categoría, nombre en ES y EN, si es habitual (búsqueda vacía) y, si hace falta, por qué sus kcal no cuadran con los macros. | Sí |
| `raciones.csv` | Las raciones de cada básico: nombre en ES y EN, gramos y de dónde sale ese peso. | Sí |
| `basicos.csv` | La lista curada con los valores por 100 g, la fuente y el código de origen en cada fila. La revisa `BasicosCsvTest`. | No: la genera el script |
| `generar_basicos.py` | Lee las descargas y los dos CSV de arriba y escribe `basicos.csv` y la migración. | — |

La migración (`src/main/resources/db/migration/V202610011910__Alimentos_basicos.sql`) también la genera el script. Es idempotente por `(fuente, codigo_origen)`. **Una migración publicada no se edita**: para cambiar los básicos se regenera con otro nombre de versión y se añade como migración nueva (lo que ya existe no se pisa: `ON DUPLICATE KEY UPDATE id = id`; para corregir valores ya cargados hace falta un `UPDATE` explícito).

## Fuentes y licencias

- **Ciqual 2025** (ANSES, Francia), `Table Ciqual 2025_FR_2025_11_03.xlsx`. Licence Ouverte / Etalab 2.0, que permite el uso comercial citando la fuente. Descarga: <https://entrepot.recherche.data.gouv.fr/dataset.xhtml?persistentId=doi:10.57745/RDMHWY>. Es la base.
- **USDA FoodData Central**, SR Legacy (abril de 2018), CSV. Dominio público (CC0). Descarga: <https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip>. Para lo que Ciqual no tiene (proteína en polvo, edamame, tempeh, requesón, queso cottage, huevo frito, nata para montar…) y para el peso de casi todas las raciones.
- **BEDCA no se usa**: pide autorización expresa de AESAN para el uso comercial.

Las descargas **no se versionan**.

## Cómo se leen los valores

- Ciqual: la energía es la del Reglamento UE 1169/2011; la proteína, la de N × 6,25, que es la del etiquetado y con la que la energía cuadra. «traces» cuenta 0; «< x», la mitad de x; «-» es que no hay dato (y entonces el alimento no puede entrar).
- Las kcal tienen que cuadrar con 4·P + 4·C + 9·G + 2·fibra (margen: 12 kcal o el 8 %). Lo que no cuadra lo explica el propio CSV: el alcohol (7 kcal/g), los ácidos orgánicos (3 kcal/g), los polioles (2,4 kcal/g) y los factores de Atwater propios que USDA usa en la soja.
- Las raciones citan la porción de USDA de la que salen (`USDA FDC 173944: 1 medium`), o el envase habitual en España; las pocas que son una estimación lo dicen.

## Regenerar

```bash
pip install pandas openpyxl
python generar_basicos.py --ciqual "Table Ciqual 2025_FR_2025_11_03.xlsx" \
    --usda FoodData_Central_sr_legacy_food_csv_2018-04 \
    --migracion V<AAAAMMDDHHmm>__Alimentos_basicos_<cambio>.sql
```

El script falla, sin escribir nada, si un código no existe en la descarga, si a un alimento le falta un valor, si sus kcal no cuadran y no está explicado, o si una ración cita una porción de USDA que no existe.
