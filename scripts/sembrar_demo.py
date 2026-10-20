"""Siembra la cuenta demo de las capturas (@dani.entrena) en la API LOCAL de pruebas.

Tres semanas que acaban hoy: 4 entrenos por semana con pesos que suben poco a poco,
comidas españolas de cada día con su agua y una medición de peso cada 3 días que baja
de forma suave. La sesión de torso con press de banca cae hace 3 días y la de pierna,
ayer, para que «Hoy toca» un torso sin repetir rutina de un día para otro (GP-203).

Solo habla con la API local y con la BD local del 3308: se niega a arrancar si la URL
no es de localhost o 127.0.0.1. La API pone a cada medición la fecha del momento en que
se guarda, así que al final se lleva cada una a su día con el cliente de MariaDB.

Uso, con la API local arrancada (scripts/entorno-pruebas.sh); ver scripts/README.md:
    GYMPROFIT_DEMO_PASSWORD=... GYMPROFIT_DB_PASSWORD=... python scripts/sembrar_demo.py
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

# Ejercicios del catálogo local: (id, peso inicial en kg, reps, series, subida por semana)
TORSO_A = [(903, 70, 8, 4, 2.5), (1631, 40, 8, 3, 1.25), (939, 60, 10, 3, 2.5), (1083, 12, 12, 3, 0)]
PIERNA_A = [(923, 85, 6, 4, 2.5), (1463, 80, 8, 3, 2.5), (911, 90, 10, 3, 5), (1167, 100, 10, 3, 5)]
TORSO_B = [(1419, 0, 8, 4, 0), (912, 55, 10, 3, 2.5), (1081, 24, 10, 3, 2), (904, 30, 10, 3, 0)]
PIERNA_B = [(906, 110, 5, 3, 5), (1592, 16, 10, 3, 2), (1650, 22, 12, 3, 0), (1161, 24, 12, 3, 0)]
# Lunes pierna, miércoles torso, jueves pierna y sábado torso: alternan, y el martes descansa
PLAN = {0: PIERNA_B, 2: TORSO_B, 3: PIERNA_A, 5: TORSO_A}

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


def entrenar(token: str, dia: date, ejercicios: list, semana: int) -> None:
    """Un entreno completo: el peso sube cada semana y la última serie a veces cae una repetición."""
    inicio = datetime.combine(dia, datetime.min.time()).replace(
        hour=18 + random.choice([0, 1]), minute=random.choice([5, 20, 35]))
    cuerpo = []
    for eid, peso, reps, series, sube in ejercicios:
        p = round((peso + sube * semana) / 1.25) * 1.25
        lista = [{"numero": n, "repeticiones": reps - (1 if n == series and random.random() < 0.5 else 0),
                  "peso": p if p > 0 else None, "completada": True} for n in range(1, series + 1)]
        cuerpo.append({"ejercicioId": eid, "repeticionesReales": reps, "series": lista})
    pedir("POST", "/sesiones/completa", {
        "claveIdempotencia": str(uuid.uuid4()), "fechaInicio": inicio.isoformat(timespec="seconds"),
        "duracionMinutos": random.choice([52, 58, 63, 67, 71]), "valoracion": random.choice([4, 4, 5]),
        "completada": True, "ejercicios": cuerpo}, token)


def comer(token: str, dia: date, tipo: str, platos: list) -> None:
    for aid, gramos in platos:
        pedir("POST", "/comidas/anadir", {"fecha": dia.isoformat(), "tipoComida": tipo,
                                          "alimentoId": aid, "cantidadGramos": gramos}, token)


def fechar_mediciones(clave_bd: str, mediciones: list) -> None:
    """Lleva cada medición a su día, a las 8:00, en la BD local del 3308. Solo toca las de la demo."""
    sql = "".join(
        f"UPDATE mediciones_corporales m JOIN usuarios u ON u.id = m.usuario_id "
        f"SET m.fecha = '{dia.isoformat()} 08:00:00' WHERE m.id = {int(mid)} AND u.username = '{USUARIO}';\n"
        for mid, dia in mediciones)
    cli = os.environ.get("MARIADB_CLI", "mariadb")
    entorno = dict(os.environ, MYSQL_PWD=clave_bd)
    subprocess.run([cli, "-h", "127.0.0.1", "-P", "3308", "-u", "root", "gymprofit_db"],
                   input=sql.encode("utf-8"), env=entorno, check=True)


def main() -> None:
    clave_demo, clave_bd = comprobar_entorno()
    random.seed(42)
    token = dar_de_alta(clave_demo)
    hoy = date.today()
    inicio = hoy - timedelta(days=DIAS)
    mediciones = []
    for i in range(DIAS + 1):
        dia = inicio + timedelta(days=i)
        if dia < hoy and dia.weekday() in PLAN:
            entrenar(token, dia, PLAN[dia.weekday()], i // 7)
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
    print(f"Cuenta @{USUARIO} sembrada: {DIAS + 1} días, {len(mediciones)} mediciones.")


if __name__ == "__main__":
    main()
