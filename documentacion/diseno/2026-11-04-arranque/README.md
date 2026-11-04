# El arranque (GP-223)

El propietario aprobó el diseño en un lienzo que no está en el repositorio. Lo que manda es
esta especificación, la de la ficha de GP-223 (4 de noviembre de 2026). Lo construido está en
`app/GymProFit/DESIGN.md` («El arranque») y en DEC-039 (las filas de la G).

## Pantallas

| Pantalla | Cómo se abre en depuración |
|---|---|
| Splash del sistema y pantalla de carga | El icono, o `am start -n com.gymprofit.app/es.pmdm.gymprofit.ui.activities.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER` |
| «Conectando…» (y el brillo) | Lo mismo con `--el depuracion_lento_ms 8000`: se renueva la sesión tras 8 s; a 1 s, el brillo; a 3 s, «Conectando…» |
| «No conseguimos conectar» | Lo mismo con `--el depuracion_lento_ms 20000`: a los 15 s se rinde |
| «Sin conexión» | Modo avión (`adb shell cmd connectivity airplane-mode enable`) y el icono |
| La sesión en vivo con su aviso | Una sesión en vivo empezada, modo avión y el icono |
| Entrar con su aviso | Trampolín: `--es destino LoginActivity --ez sesion_cerrada true` (el correo sale escrito si se entró antes con él) |

## Especificación

1. **Una sola pantalla**, oscura (#0B0C0E) también con la app en claro, como el icono, para que no haya destello. Los colores salen del tema oscuro con un ThemeOverlay, no de hex escritos a mano.
2. **El splash del sistema**, con la API SplashScreen de AndroidX. La G del logo oscuro, sin fondo de icono, a 160 dp, dentro del círculo de 192 dp del splash. Android 12 o más: la G entra animada con un AnimatedVectorDrawable. Android 11 o menos: la G quieta, con un vector estático del último fotograma.
3. **La G en vector**: dos trazados sacados de `drawable-night/logo.png`, el cuerpo dorado y el trazo naranja de arriba a la derecha, con los colores exactos del PNG.
4. **La entrada dura 700 ms.** Cuerpo: gira de −90° a 0°, escala de 0,72 a 1 y alfa de 0 a 1, en 450 ms con la curva estándar (0.2, 0, 0, 1). Trazo: llega desde 18 dp arriba a la derecha (sobre una G de 160 dp), escala de 0,6 a 1 y alfa de 0 a 1; empieza a los 280 ms, dura 420 ms y va con rebote (0.34, 1.56, 0.64, 1), con el pivote en el 74,9 % horizontal y el 20 % vertical del logo. La entrada se ve entera: si la app está lista antes, espera a que acabe, como mucho 700 ms desde que se toca el icono.
5. **Qué decide y adónde va.** Solo se llama a la API para renovar la sesión. Sin sesión, la bienvenida; con el alta a medias, el alta donde se dejó. Con sesión, la caducidad del access token se lee de su `exp`: vigente, Inicio al momento; caducado, se renueva mientras se ve la G. Sin red al abrir, «Sin conexión», salvo con una sesión en vivo guardada en el móvil, que se abre directamente. Red que no responde: servidor lento. Un 401 al renovar: Entrar, sin pasar por Inicio. La cuenta desactivada sigue con su aviso (GP-083).
6. **Tiempos.** A 1 s sin destino, el splash del sistema se quita sin animación y debajo está la pantalla de carga, con la G en el mismo sitio y al mismo tamaño. Entonces, el brillo: una franja de luz blanca al 40 %, recortada por la G, que la cruza en 1,2 s con la curva estándar y se repite cada 1,8 s. A los 3 s, «Conectando…» bajo la G: Barlow 15 sp en `gp_text_secondary` del tema oscuro (#A1A9B6), a 28 dp de la G, en 200 ms. A los 15 s, «No conseguimos conectar».
7. **Errores.** La G sube y pasa a 72 dp en 350 ms con la estándar. Debajo, el título en Barlow Condensed 30 sp y el texto en Barlow 16 sp en `gp_text_secondary`, a 300 dp de ancho como mucho. «Reintentar» abajo: 52 dp de alto, naranja con el texto oscuro, a 20 dp de los lados y 32 dp del borde inferior; vuelve a decidir desde el principio. Textos: «Sin conexión» / «Comprueba la conexión y vuelve a intentarlo.»; «No conseguimos conectar» / «El servidor tarda más de lo normal. Vuelve a intentarlo en un momento.»
8. **La salida es la misma vaya a donde vaya.** La G crece al 125 % y se desvanece en 300 ms con la curva acelerada (0.3, 0, 0.8, 0.15); el fondo se abre. Inicio y Entrar entran en cascada: 350 ms por pieza, 40 ms entre piezas, desde 16 dp más abajo. La sesión en vivo sube 40 dp mientras aparece y su aviso baja 12 dp. La bienvenida y el alta entran como antes. Sin destello ni un fotograma en blanco. Inicio no cambia por dentro.
9. **Entrar cuando la sesión ya no vale.** Arriba, un aviso con el candado en superficie 2: «Tu sesión se ha cerrado. Por seguridad, vuelve a entrar: tus datos siguen ahí.» El correo o usuario con el que se entró, ya escrito, y el foco en la contraseña. Ese dato se borra al cerrar sesión a propósito y al borrar la cuenta, pero no cuando la sesión caduca. Sale el mismo aviso cuando la sesión se pierde a mitad de uso.
10. **Sesión en vivo sin red**: un aviso arriba en superficie 2 con `cloud_off`: «Sin conexión. Tu entreno se guarda en el móvil y se sube cuando vuelva la red.» Sale mientras no hay red, también si se pierde a mitad, y se quita al volver.
11. **Movimiento**: los momentos en `Movimiento` y DESIGN.md, sin números a mano. Con «Quitar animaciones», todo directo a su estado final y sin esperar a la entrada.
12. **Accesibilidad**: TalkBack dice «Cargando GymProFit» y, a los 3 s, «Conectando»; en un error, el foco va al título. Zonas táctiles de 48 dp o más. A letra 1,3, el texto crece y parte línea.
