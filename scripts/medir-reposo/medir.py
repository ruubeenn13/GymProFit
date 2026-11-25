"""Sentencias que la API manda a la base estando en reposo (GP-283).

Arranca la API con el perfil ``prod`` contra una base de la MariaDB LOCAL, la deja
sin usuarios el tiempo pedido mientras simula a quien llama al health desde fuera
(Render cada 5 s, UptimeRobot cada 5 min, el keep-alive de GitHub cada 10 min), y
cuenta lo que llegó a la base con el log general de MariaDB.

Nunca toca producción: la URL se construye aquí con ``127.0.0.1`` y el script se
niega a seguir si el puerto o el host vienen de otro sitio. La base es propia
(``gymprofit_reposo``) y empieza vacía de usuarios, así que el informe no puede
llevar datos personales; aun así, solo se imprime la forma de cada sentencia, con
los literales cambiados por ``?``.

Uso (desde la raíz del repo, con el jar ya construido)::

    python scripts/medir-reposo/medir.py --minutos 15 --etiqueta antes \\
        --render /api/actuator/health --uptime /api/actuator/health

Variables: ``MARIADB_PWD`` (la de root de la MariaDB local), ``MARIADB_BIN``
(carpeta de mariadb.exe), ``JAVA_HOME``. El resultado va a
``scripts/medir-reposo/resultados/<etiqueta>.json`` y a la salida estándar.
"""
from __future__ import annotations

import argparse
import collections
import glob
import json
import os
import re
import secrets
import subprocess
import sys
import threading
import time
import urllib.request
from pathlib import Path

HOST = "127.0.0.1"
PUERTO_BD = 3308
BASE = "gymprofit_reposo"
PUERTO_API = 8090
RAIZ = Path(__file__).resolve().parents[2]
SALIDA = Path(__file__).resolve().parent / "resultados"


def sql(consulta: str, base: str | None = None) -> list[list[str]]:
    """Ejecuta una consulta en la MariaDB local y devuelve las filas como listas."""
    exe = Path(os.environ.get("MARIADB_BIN", r"C:\Program Files\MariaDB 11.8\bin")) / "mariadb.exe"
    orden = [str(exe), "-uroot", f"-h{HOST}", f"-P{PUERTO_BD}", "-N", "-B", "--raw", "-e", consulta]
    if base:
        orden.insert(1, f"-D{base}")
    entorno = dict(os.environ, MYSQL_PWD=os.environ["MARIADB_PWD"])
    r = subprocess.run(orden, capture_output=True, text=True, encoding="utf-8", env=entorno, check=True)
    return [linea.split("\t") for linea in r.stdout.splitlines() if linea]


def estado_global() -> dict[str, int]:
    """Contadores del servidor que el log general no ve: el ping no se registra en él."""
    filas = sql("SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_admin_commands','Connections')")
    return {f[0]: int(f[1]) for f in filas}


def normalizar(sentencia: str) -> str:
    """Quita literales y espacios para agrupar sentencias iguales."""
    s = re.sub(r"'(?:[^'\\]|\\.)*'", "?", sentencia)
    s = re.sub(r"\b\d+(\.\d+)?\b", "?", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s[:140]


def llamar_periodicamente(ruta: str, cada: float, parar: threading.Event, cuenta: collections.Counter) -> None:
    """Hace GET a la ruta cada ``cada`` segundos hasta que se pida parar."""
    while not parar.is_set():
        try:
            with urllib.request.urlopen(f"http://{HOST}:{PUERTO_API}{ruta}", timeout=5) as r:
                cuenta[f"{ruta} {r.status}"] += 1
        except Exception as e:  # el informe dice cuántas fallaron; no se ocultan
            cuenta[f"{ruta} ERROR {type(e).__name__}"] += 1
        parar.wait(cada)


def esperar_arranque(ruta: str, tope: float = 240) -> None:
    """Espera a que la API responda 200 en la ruta dada."""
    if not ruta.startswith("/api/"):
        # Git Bash convierte "/api/..." en una ruta de Windows: lanzar con MSYS_NO_PATHCONV=1.
        raise SystemExit(f"Ruta rara: {ruta!r}")
    limite = time.time() + tope
    ultimo = None
    while time.time() < limite:
        try:
            with urllib.request.urlopen(f"http://{HOST}:{PUERTO_API}{ruta}", timeout=3) as r:
                if r.status == 200:
                    return
        except Exception as e:
            ultimo = e  # todavía arrancando: se reintenta hasta el tope
        time.sleep(2)
    raise SystemExit(f"La API no arrancó a tiempo; último error: {ultimo!r}")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--minutos", type=float, default=15)
    p.add_argument("--etiqueta", required=True)
    p.add_argument("--render", default="/api/actuator/health")
    p.add_argument("--uptime", default="/api/actuator/health")
    p.add_argument("--keepalive", default="/api/actuator/health")
    p.add_argument("--jar", help="jar a medir; por defecto, el de api/gymprofit-api/target")
    p.add_argument("--calentar", type=float, default=90, help="segundos tras arrancar antes de contar")
    a = p.parse_args()

    jar = [a.jar] if a.jar else sorted(glob.glob(str(RAIZ / "api/gymprofit-api/target/gymprofit-api-*.jar")))
    jar = [j for j in jar if not j.endswith("plain.jar")]
    if not jar:
        raise SystemExit("Falta el jar: sh ./mvnw -B -q -DskipTests package")

    if puerto_ocupado(PUERTO_API):
        raise SystemExit(f"El puerto {PUERTO_API} ya está en uso: otra API seguiría respondiendo por esta")
    sql(f"CREATE DATABASE IF NOT EXISTS {BASE} CHARACTER SET utf8mb4")
    previo = sql("SELECT @@general_log, @@log_output")[0]
    try:
        medir(a, jar, previo)
    finally:
        # Pase lo que pase, el servidor vuelve a como estaba: sin log general.
        sql(f"SET GLOBAL general_log=0")
        sql(f"SET GLOBAL log_output='{previo[1]}'")
        sql(f"SET GLOBAL general_log={previo[0]}")
        sql("TRUNCATE TABLE mysql.general_log")


def puerto_ocupado(puerto: int) -> bool:
    """True si algo escucha ya en ese puerto local."""
    import socket
    with socket.socket() as s:
        s.settimeout(1)
        return s.connect_ex((HOST, puerto)) == 0


def medir(a: argparse.Namespace, jar: list[str], previo: list[str]) -> None:
    """Arranca la API, la deja en reposo el tiempo pedido y guarda el recuento."""
    sql("SET GLOBAL log_output='TABLE'")
    sql("TRUNCATE TABLE mysql.general_log")
    sql("SET GLOBAL general_log=1")

    entorno = dict(os.environ)
    entorno.update({
        "SPRING_PROFILES_ACTIVE": "prod",
        "SPRING_DATASOURCE_URL": f"jdbc:mariadb://{HOST}:{PUERTO_BD}/{BASE}",
        "SPRING_DATASOURCE_USERNAME": "root",
        "SPRING_DATASOURCE_PASSWORD": os.environ["MARIADB_PWD"],
        # Los dos ajustes de Hikari que producción tiene en las variables de Render.
        "SPRING_DATASOURCE_HIKARI_KEEPALIVE_TIME": "120000",
        "SPRING_DATASOURCE_HIKARI_MAX_LIFETIME": "1500000",
        "JWT_SECRET": secrets.token_urlsafe(48),
        "BREVO_API_KEY": "xkeysib-medida-local-no-envia",
        "MAIL_FROM": "Medida <medida@localhost>",
        "IMPORTACION_CLAVE": secrets.token_urlsafe(40),
        "PORT": str(PUERTO_API),
        "LOG_DIR": str(SALIDA / "logs"),
        "FIREBASE_CREDENTIALS_JSON": "",
        "FIREBASE_CREDENTIALS_PATH": "",
    })
    entorno.pop("SENTRY_DSN", None)
    assert HOST in entorno["SPRING_DATASOURCE_URL"], "la medida solo corre contra la base local"

    java = Path(os.environ["JAVA_HOME"]) / "bin" / "java"
    SALIDA.mkdir(exist_ok=True)
    log_api = open(SALIDA / f"api-{a.etiqueta}.log", "w", encoding="utf-8")
    api = subprocess.Popen([str(java), "-Xmx512m", "-jar", jar[-1]], env=entorno, stdout=log_api,
                           stderr=subprocess.STDOUT, cwd=SALIDA)
    llamadas: collections.Counter = collections.Counter()
    parar = threading.Event()
    try:
        esperar_arranque(a.render)
        time.sleep(a.calentar)
        estado0 = estado_global()
        t0 = sql("SELECT NOW(6)")[0][0]
        hilos = [threading.Thread(target=llamar_periodicamente, args=(ruta, cada, parar, llamadas), daemon=True)
                 for ruta, cada in ((a.render, 5), (a.uptime, 300), (a.keepalive, 600))]
        for h in hilos:
            h.start()
        time.sleep(a.minutos * 60)
        t1 = sql("SELECT NOW(6)")[0][0]
        estado1 = estado_global()
        parar.set()
    finally:
        api.terminate()
        try:
            api.wait(30)
        except subprocess.TimeoutExpired:
            api.kill()
        log_api.close()
        limite = time.time() + 30
        while puerto_ocupado(PUERTO_API) and time.time() < limite:
            time.sleep(1)

    sql("SET GLOBAL general_log=0")
    # Hilos de la API: los que se conectaron a la base de la medida.
    hilos_api = {f[0] for f in sql(
        f"SELECT DISTINCT thread_id FROM mysql.general_log WHERE command_type='Connect' "
        f"AND argument LIKE '%on {BASE}%'")}
    filas = sql(
        f"SELECT thread_id, command_type, REPLACE(REPLACE(CONVERT(argument USING utf8mb4), '\\n', ' '), '\\t', ' ') "
        f"FROM mysql.general_log WHERE event_time >= '{t0}' AND event_time < '{t1}'")

    por_tipo: collections.Counter = collections.Counter()
    por_sentencia: collections.Counter = collections.Counter()
    for fila in filas:
        if len(fila) < 3 or fila[0] not in hilos_api:
            continue
        por_tipo[fila[1]] += 1
        por_sentencia[f"{fila[1]}: {normalizar(fila[2])}"] += 1

    horas = a.minutos / 60
    resultado = {
        "etiqueta": a.etiqueta,
        "minutos": a.minutos,
        "rutas": {"render_5s": a.render, "uptime_5min": a.uptime, "keepalive_10min": a.keepalive},
        "llamadas_http": dict(llamadas),
        "total": sum(por_tipo.values()),
        "total_por_hora": round(sum(por_tipo.values()) / horas, 1),
        "por_tipo_por_hora": {k: round(v / horas, 1) for k, v in por_tipo.most_common()},
        "por_sentencia": dict(por_sentencia.most_common()),
        # COM_PING (keepalive de Hikari, isValid del health y la comprobación al prestar
        # una conexión): Com_admin_commands. Conexiones nuevas: Connections, menos las 3
        # que abre este script entre las dos lecturas (NOW de t0, NOW de t1 y la lectura).
        "pings_por_hora": round((estado1["Com_admin_commands"] - estado0["Com_admin_commands"]) / horas, 1),
        "conexiones_nuevas_por_hora": round((estado1["Connections"] - estado0["Connections"] - 3) / horas, 1),
    }
    (SALIDA / f"{a.etiqueta}.json").write_text(json.dumps(resultado, ensure_ascii=False, indent=2), encoding="utf-8")
    json.dump(resultado, sys.stdout, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
