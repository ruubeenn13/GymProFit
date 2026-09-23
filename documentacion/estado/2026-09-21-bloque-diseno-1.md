# Bloque de diseño 1 — GP-059, GP-060 y GP-061

Informe de los tres primeros arreglos salidos de
[`AUDITORIA-DISENO-2026-09.md`](../AUDITORIA-DISENO-2026-09.md). Ejecutado el
2026-09-23 sobre el emulador `Medium_Phone_API_36.1` (1080 × 2400) con el
backend de desarrollo en `localhost:8080` y la app apuntando a `10.0.2.2:8080`
(distintivo `PRUEBAS · LOCAL` visible en todas las capturas).

Capturas en [`bloque-diseno-1/`](bloque-diseno-1/).

---

## GP-059 · La barra flotante tapa contenido en las cinco pestañas

Corresponde a **T-01** de la auditoría, y a **P-15** de la pestaña de Rutinas.

### Qué decía el hallazgo

Que la `FloatingNavBar` ocupa 86 dp (72 de alto + 14 de margen inferior), que los
cinco contenedores reservan como mucho 16 dp de `paddingBottom`, y que por tanto
queda contenido inalcanzable: «Registrar comida» en Home, la última tarjeta en
Rutinas, el último ejercicio en Ejercicios, «Comida» y «Merienda» en Nutrición y
«Mis logros» en Perfil.

### Qué se ha medido antes de tocar nada

**El solape no se reproduce en ejecución.** Se instaló el APK de depuración del
2026-09-21 17:59 —el mismo con el que se tomaron las capturas de la auditoría— y
se llevó cada pestaña **al final de su desplazamiento**:

| Pestaña | Último elemento con la lista al final | ¿Tapado? |
|---|---|---|
| Home | Tarjetas «Entrenos / Calorías / Minutos» | No |
| Rutinas | Tarjeta «HIIT Avanzado» | No |
| Nutrición | Fila «Cena» | No |
| Perfil | Botón «Eliminar cuenta» | No |

El motivo es que el `paddingBottom` de los layouts **no es el que manda**:
`BaseFragment.onViewCreated` ya buscaba el primer scroller vertical del árbol y
le sumaba una reserva en ejecución desde el commit `8f64cb7` (2026-07-09), el
mismo que trajo la barra liquid glass. La auditoría midió el XML, donde esa reserva no se ve,
y las capturas que cita están tomadas **a media lista**, no al final: lo que en
ellas se ve bajo el cristal es el `clipToPadding="false"` haciendo su trabajo
—dibujar el contenido difuminado mientras pasa por debajo—, no contenido
inalcanzable.

Corrección que esto obliga a hacer en la auditoría: **T-01 y P-15 están mal
medidos**. El defecto que sí existía es otro, y es el que el encargo señalaba.

### El defecto real: la reserva estaba escrita a mano

```java
int reserva = Math.round(96 * getResources().getDisplayMetrics().density);
```

Un 96 a pelo dentro de `BaseFragment`, sin relación declarada con los `72dp` y
`14dp` que `activity_main.xml` escribía también a pelo. Tres números sueltos que
describen la misma cosa: cambiar el alto de la barra dejaba la reserva corta —o
sobrada— sin que nada avisara. Es exactamente el problema que el encargo pedía
evitar, solo que repartido entre dos ficheros en lugar de cinco.

### Qué se ha hecho

Una sola fuente para las medidas de la barra, en `res/values/dimens.xml`:

```xml
<dimen name="nav_bar_height">72dp</dimen>
<dimen name="nav_bar_margin">14dp</dimen>
<dimen name="nav_bar_content_gap">12dp</dimen>
```

- `activity_main.xml` dibuja la barra con `@dimen/nav_bar_height` y
  `@dimen/nav_bar_margin`. Ya no hay ningún `dp` escrito a mano en la barra.
- `FloatingNavBar.espacioReservado(Context)` devuelve la suma de las tres: el
  alto, el margen y el aire que separa el último elemento del cristal. Es el
  único sitio donde se calcula ese hueco.
- `BaseFragment` lo llama en lugar del 96. Se marca el scroller con
  `R.id.tag_hueco_barra_reservado` para no sumar la reserva dos veces si la
  vista se reutiliza.

Cambiar `nav_bar_height` mueve ahora la barra **y** el hueco de las cinco
pestañas a la vez, que era el requisito.

La reserva pasa de 96 dp a 98 dp (72 + 14 + 12). La diferencia es irrelevante en
pantalla; lo que cambia es de dónde sale el número.

### Por qué no se ha tocado `clipToPadding`

Porque no es el arreglo y tampoco estorba: el sitio lo reserva el `padding`, y
`clipToPadding="false"` es lo que deja que el contenido siga viéndose difuminado
bajo el cristal mientras se desplaza, que es el efecto buscado. Se ha dejado
escrito en el comentario de `BaseFragment` para que no se confunda otra vez.

### Verificación

APK reconstruido e instalado. Las cinco pestañas llevadas al final del
desplazamiento, con la cuenta `admin` (tiene las seis rutinas predefinidas y las
cuatro filas del perfil, incluido el panel de administración):

| Pestaña | Captura | Último elemento, entero y pulsable |
|---|---|---|
| Home | `gp059-home.png` | Fila «Entrenos / Calorías / Minutos» |
| Rutinas | `gp059-rutinas.png` | Tarjeta «HIIT Avanzado» |
| Ejercicios | `gp059-ejercicios.png` | «Zottman Preacher Curl», último de los 873 |
| Nutrición | `gp059-nutricion.png` | Fila «Cena» |
| Perfil | `gp059-perfil.png` | Botón «Eliminar cuenta» |

Ejercicios se recorrió entero hasta el final real de la lista (350 gestos de
desplazamiento, de «3/4 Sit-Up» a «Zottman Preacher Curl»), no hasta un punto
intermedio.

La reserva es geométrica: no depende del idioma ni del tema, así que no hay
cuatro combinaciones que comprobar aquí.
