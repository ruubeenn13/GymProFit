"""Siembra la cuenta demo de las capturas (@dani.entrena) en la API LOCAL de pruebas.

Sigue el programa «Torso y pierna» (GIM-TP, 60 min) y tiene tres semanas de historial que
acaban hoy: 4 entrenos por semana, cada uno con la rutina del programa que le toca (torso A,
pierna A, torso B, pierna B) y con pesos que suben poco a poco; comidas españolas de cada día
con su agua y una medición de peso cada 3 días que baja de forma suave. Así el revisor ve
«Hoy toca» con rutina, el resumen del entreno con su comparación frente a la sesión anterior
de la misma rutina y «Tu semana» de nutrición con datos (GP-210, GP-216).

Solo habla con la API local y con la BD local del 3308: se niega a arrancar si la URL
no es de localhost o 127.0.0.1. La API pone a cada medición la fecha del momento en que
se guarda, así que al final se lleva cada una a su día con el cliente de MariaDB. Antes de
crear nada comprueba que los ejercicios del programa tienen foto, como en producción (GP-248).

Uso, con la API local arrancada (scripts/entorno-pruebas.sh); ver scripts/README.md:
    GYMPROFIT_DEMO_PASSWORD=... GYMPROFIT_DB_PASSWORD=... python scripts/sembrar_demo.py
Con --hoy AAAA-MM-DD siembra como si hoy fuera ese día (la web de «Un día» pide un lunes).
Variables opcionales: GYMPROFIT_API_URL (por defecto http://localhost:8080/api) y
MARIADB_CLI (por defecto «mariadb» en el PATH).
"""
import json
import os
import random
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import date, datetime, timedelta

API = os.environ.get("GYMPROFIT_API_URL", "http://localhost:8080/api").rstrip("/")
HOSTS_LOCALES = {"localhost", "127.0.0.1"}
USUARIO, CORREO = "dani.entrena", "demo@gymprofit.app"
DIAS = 21

# El programa que sigue y sus minutos por sesión; seguirlo copia sus rutinas a la cuenta
PROGRAMA, MINUTOS = "GIM-TP", 60
# Lunes torso A, martes pierna A, jueves torso B y viernes pierna B: las del programa, en su orden
PLAN = {0: "GIM-TORSO-A", 1: "GIM-PIERNA-A", 3: "GIM-TORSO-B", 4: "GIM-PIERNA-B"}
# Cargas de un intermedio (unos 77 kg, dos años entrenando): los kilos de la primera semana y lo
# que suben cada semana, por ejercicio del programa (nombreEjercicio). Con mancuernas, el peso de
# cada una, como lo apunta la app. None: peso corporal, la serie va sin kilos. Antes salían del
# tipo de la pauta y del id del ejercicio, y daban 42,5 kg a las elevaciones laterales, al curl
# y a la extensión de tríceps a la vez. El press de banca acaba en 80 kg: el récord de
# «Un día» (scripts/dia-ejemplo-web.py) sube de ahí su escalón, 2,5 kg.
KILOS = {
    # Torso A
    "Press de banca con barra": (75, 2.5),
    "Remo con barra": (65, 2.5),
    "Press militar de pie, con barra": (45, 1.25),
    "Jalón al pecho": (55, 2.5),
    "Elevaciones laterales": (8, 1),
    "Curl de bíceps con barra": (30, 1.25),
    "Extensión de tríceps en polea": (22.5, 1.25),
    # Torso B
    "Press inclinado con mancuernas": (26, 2),
    "Dominadas": None,
    "Remo en polea sentado": (55, 2.5),
    "Aperturas en máquina": (40, 2.5),
    "Curl inclinado con mancuernas": (10, 1),
    "Extensión de tríceps sobre la cabeza, en polea": (20, 1.25),
    # Pierna A
    "Sentadilla con barra": (90, 2.5),
    "Peso muerto rumano con barra": (80, 2.5),
    "Prensa de piernas": (140, 5),
    "Curl femoral tumbado": (35, 2.5),
    "Elevación de gemelos de pie, en máquina": (60, 5),
    "Crunch en polea": (35, 2.5),
    # Pierna B
    "Sentadilla hack en máquina": (80, 5),
    "Hip thrust con barra": (90, 5),
    "Sentadilla búlgara con mancuernas": (14, 2),
    "Curl femoral sentado": (40, 2.5),
    "Elevación de gemelos sentado, en máquina": (40, 2.5),
    "Plancha": None,
}
# Las series por tiempo (medida SEGUNDOS, la plancha): los segundos de la primera semana y lo
# que suben cada semana, dentro del rango de la pauta.
SEGUNDOS = (40, 5)

# Alimentos del catálogo local: (id, gramos)
DESAYUNOS = [[(6554, 200), (6405, 60), (6466, 50), (6543, 10)],  # café con leche, tostada con tomate y aceite
             [(6554, 200), (6389, 50), (6424, 120)],  # café con leche, avena y plátano
             [(6554, 200), (6395, 70), (6237, 30), (6466, 40)]]  # café con leche, pan con jamón y tomate
COMIDAS = [[(6349, 250), (6395, 40), (6425, 180)],  # lentejas, pan y manzana
           [(6201, 180), (6371, 200), (6472, 80), (6543, 10)],  # pollo con arroz y ensalada
           [(6634, 300), (6472, 80)],  # paella y ensalada
           [(6262, 180), (6417, 200), (6505, 150), (6543, 10)],  # merluza con patata y judías
           [(6353, 250), (6201, 100), (6427, 160)],  # garbanzos con pollo y naranja
           [(6376, 220), (6258, 80), (6466, 100), (6543, 10)]]  # pasta con atún y tomate
MERIENDAS = [[(6313, 125), (6434, 120)], [(6315, 150), (6424, 120)], [(6524, 25), (6425, 180)]]
CENAS = [[(6301, 120), (6472, 100), (6466, 100), (6543, 8)],  # tortilla francesa y ensalada
         [(6256, 160), (6481, 200), (6543, 8)],  # salmón con brócoli
         [(6208, 160), (6489, 200), (6418, 150)],  # pavo con calabacín y patata
         [(6302, 200), (6472, 100), (6466, 100)]]  # tortilla de patatas y ensalada
# Una medición cada 3 días: baja medio kilo por semana, con una semana plana en medio
PESOS = [78.6, 78.4, 78.0, 78.0, 77.6, 77.4, 77.2, 76.9]


def comprobar_entorno() -> tuple[str, str]:
    """Para el script si la API no es local o faltan las contraseñas; devuelve las dos."""
    host = urllib.parse.urlparse(API).hostname
    if host not in HOSTS_LOCALES:
        sys.exit(f"Me niego: {API} no es la API local. Este script solo siembra localhost o 127.0.0.1.")
    demo, bd = os.environ.get("GYMPROFIT_DEMO_PASSWORD"), os.environ.get("GYMPROFIT_DB_PASSWORD")
    if not demo or not bd:
        sys.exit("Faltan GYMPROFIT_DEMO_PASSWORD (la de la cuenta demo) y GYMPROFIT_DB_PASSWORD (la de la BD local).")
    return demo, bd


def pedir(metodo: str, ruta: str, cuerpo=None, token: str | None = None):
    """Una petición JSON a la API local; devuelve el cuerpo decodificado o para con el error."""
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json; charset=utf-8")
    req.add_header("Accept-Language", "es")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode("utf-8")
            return json.loads(texto) if texto else None
    except urllib.error.HTTPError as e:
        sys.exit(f"{metodo} {ruta} -> {e.code}: {e.read().decode('utf-8')[:400]}")


def dar_de_alta(clave: str) -> str:
    """Crea la cuenta demo y devuelve su token. Si ya existe, para: se borra antes desde la app."""
    pedir("POST", "/auth/register", {
        "username": USUARIO, "password": clave, "email": CORREO, "nombre": "Dani",
        "peso": PESOS[0], "altura": 178, "edad": 29, "sexo": "HOMBRE",
        "nivelExperiencia": "INTERMEDIO", "objetivo": "PERDER_PESO", "nivelActividad": "MODERADO"})
    login = pedir("POST", "/auth/login", {"username": USUARIO, "password": clave})
    return login["token"]


def seguir_programa(token: str) -> dict:
    """Sigue el programa y devuelve las rutinas que se copian a la cuenta, por código de rutina."""
    pedir("POST", f"/programas/{PROGRAMA}/seguir", {"minutos": MINUTOS}, token)
    seguido = pedir("GET", "/programas/seguido", None, token)
    # Las copias de la cuenta llevan el código de su plantilla en plantillaCodigo
    rutinas = {r["plantillaCodigo"]: r for r in seguido["rutinas"]}
    faltan = [c for c in PLAN.values() if c not in rutinas]
    if faltan:
        sys.exit(f"El programa {PROGRAMA} no trae las rutinas {faltan}: ¿cambió su semana?")
    return rutinas


def entrenar(token: str, dia: date, rutina: dict, semana: int) -> None:
    """Un entreno completo de una rutina del programa: el peso sube cada semana y la última serie
    a veces cae una repetición."""
    inicio = datetime.combine(dia, datetime.min.time()).replace(
        hour=18 + random.choice([0, 1]), minute=random.choice([5, 20, 35]))
    cuerpo = []
    for e in sorted(rutina["ejercicios"], key=lambda x: x["orden"]):
        nombre = e["nombreEjercicio"]
        if nombre not in KILOS:
            sys.exit(f"«{nombre}» no tiene carga en KILOS: ¿cambió el programa {PROGRAMA}?")
        carga = KILOS[nombre]
        kilos = None if carga is None else carga[0] + carga[1] * semana
        reps, series = e["repeticiones"], e["series"]
        lista = []
        for n in range(1, series + 1):
            # La última serie a veces cae una repetición (o 5 s, si va por tiempo)
            cae = n == series and random.random() < 0.5
            if e.get("medida") == "SEGUNDOS":
                segundos = SEGUNDOS[0] + SEGUNDOS[1] * semana - (5 if cae else 0)
                lista.append({"numero": n, "repeticiones": 0, "segundos": segundos,
                              "peso": None, "completada": True})
            else:
                lista.append({"numero": n, "repeticiones": reps - (1 if cae else 0),
                              "peso": kilos, "completada": True})
        cuerpo.append({"ejercicioId": e["ejercicioId"], "repeticionesReales": reps, "series": lista})
    pedir("POST", "/sesiones/completa", {
        "claveIdempotencia": str(uuid.uuid4()), "fechaInicio": inicio.isoformat(timespec="seconds"),
        "rutinaId": rutina["id"], "duracionMinutos": random.choice([52, 58, 63, 67, 71]),
        "valoracion": random.choice([4, 4, 5]), "completada": True, "ejercicios": cuerpo}, token)


def comer(token: str, dia: date, tipo: str, platos: list) -> None:
    for aid, gramos in platos:
        pedir("POST", "/comidas/anadir", {"fecha": dia.isoformat(), "tipoComida": tipo,
                                          "alimentoId": aid, "cantidadGramos": gramos}, token)


def bd_local(clave_bd: str, sql: str) -> str:
    """Pasa el SQL al cliente de MariaDB contra la BD local del 3308 y devuelve lo que escribe."""
    cli = os.environ.get("MARIADB_CLI", "mariadb")
    entorno = dict(os.environ, MYSQL_PWD=clave_bd)
    hecho = subprocess.run([cli, "-N", "-h", "127.0.0.1", "-P", "3308", "-u", "root", "gymprofit_db"],
                           input=sql.encode("utf-8"), env=entorno, check=True, capture_output=True)
    return hecho.stdout.decode("utf-8").strip()


def comprobar_fotos(clave_bd: str) -> None:
    """Para antes de crear nada si algún ejercicio del programa no tiene foto (GP-248).

    En producción todos la tienen, porque vienen de free-exercise-db; si aquí falta, la demo
    enseñaría siluetas en su lugar. scripts/entorno-pruebas.sh las rellena al arrancar."""
    faltan = bd_local(clave_bd, (
        "SELECT e.nombre FROM programas p JOIN programa_rutina pr ON pr.programa_id = p.id "
        "JOIN rutina_ejercicio re ON re.rutina_id = pr.rutina_id JOIN ejercicios e ON e.id = re.ejercicio_id "
        f"WHERE p.codigo = '{PROGRAMA}' AND (e.imagen_url IS NULL OR e.imagen_url = '');"))
    if faltan:
        sys.exit(f"Ejercicios de {PROGRAMA} sin foto en la BD local: {', '.join(faltan.splitlines())}.\n"
                 "Arranca con scripts/entorno-pruebas.sh, que las rellena desde fed_id; las que sigan sin foto "
                 "no tienen fed_id y se miran a mano.")


def fechar_mediciones(clave_bd: str, mediciones: list) -> None:
    """Lleva cada medición a su día, a las 8:00, en la BD local del 3308. Solo toca las de la demo."""
    bd_local(clave_bd, "".join(
        f"UPDATE mediciones_corporales m JOIN usuarios u ON u.id = m.usuario_id "
        f"SET m.fecha = '{dia.isoformat()} 08:00:00' WHERE m.id = {int(mid)} AND u.username = '{USUARIO}';\n"
        for mid, dia in mediciones))


def dia_de_hoy() -> date:
    """El «hoy» de la demo: el de la máquina o, con --hoy AAAA-MM-DD, otro anterior.

    «Un día» de la web es un lunes (Torso A sin hacer): con --hoy se siembra ese lunes aunque
    se siembre otro día de la semana."""
    if "--hoy" not in sys.argv:
        return date.today()
    try:
        hoy = date.fromisoformat(sys.argv[sys.argv.index("--hoy") + 1])
    except (IndexError, ValueError):
        sys.exit("--hoy pide una fecha AAAA-MM-DD.")
    if hoy > date.today():
        sys.exit(f"--hoy {hoy} es futuro: los entrenos y las comidas no pueden ir por delante.")
    return hoy


def main() -> None:
    clave_demo, clave_bd = comprobar_entorno()
    comprobar_fotos(clave_bd)
    hoy = dia_de_hoy()
    random.seed(42)
    token = dar_de_alta(clave_demo)
    rutinas = seguir_programa(token)
    inicio = hoy - timedelta(days=DIAS)
    mediciones = []
    entrenos = 0
    for i in range(DIAS + 1):
        dia = inicio + timedelta(days=i)
        if dia < hoy and dia.weekday() in PLAN:
            entrenar(token, dia, rutinas[PLAN[dia.weekday()]], i // 7)
            entrenos += 1
        comer(token, dia, "DESAYUNO", random.choice(DESAYUNOS))
        comer(token, dia, "COMIDA", COMIDAS[i % len(COMIDAS)])
        # Hoy van apuntados el desayuno y la comida; la merienda y la cena, todavía no
        if dia < hoy:
            comer(token, dia, "MERIENDA", random.choice(MERIENDAS))
            comer(token, dia, "CENA", CENAS[i % len(CENAS)])
        pedir("PUT", f"/diario/{dia.isoformat()}/agua",
              {"mililitros": 1500 if dia == hoy else random.choice([2000, 2250, 2500])}, token)
        if i % 3 == 0:
            # El usuarioId del cuerpo lo pide el DTO, pero el dueño sale del token (DEC-013)
            m = pedir("POST", "/mediciones-corporales", {"usuarioId": 0, "peso": PESOS[i // 3],
                                                         "altura": 178, "cintura": round(86 - i * 0.08, 1)}, token)
            mediciones.append((m["id"], dia))
    fechar_mediciones(clave_bd, mediciones)
    if hoy != date.today():
        # El alta lleva la fecha del momento: «Un día» (scripts/dia-ejemplo-web.py) toma de ella
        # el día de ejemplo, así que se lleva al «hoy» elegido.
        bd_local(clave_bd, f"UPDATE usuarios SET fecha_registro = '{hoy.isoformat()} 07:00:00' "
                           f"WHERE username = '{USUARIO}';")
    print(f"Cuenta @{USUARIO} sembrada: sigue {PROGRAMA}, {entrenos} entrenos con rutina, "
          f"{DIAS + 1} días de comidas, {len(mediciones)} mediciones.")


if __name__ == "__main__":
    main()
