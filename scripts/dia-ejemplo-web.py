"""Saca de la cuenta demo LOCAL las cifras del «día de ejemplo» de la portada (DEC-069, propuesta).

Escribe web/src/datos/dia-ejemplo.json. Ninguna cifra de las escenas se escribe a mano: o sale
de aquí, o la escena no la enseña (web/PLAN-UN-DIA.md, «Reglas comunes»).

Solo lee, y solo de la MariaDB local del 3308 (GP-282): ni producción ni la base anterior de
Aiven, ni para leer. Para si el host no es 127.0.0.1 o localhost. La cuenta es @dani.entrena,
sembrada con scripts/sembrar_demo.py --hoy <un lunes>; el día de ejemplo es el lunes en que se sembró, que en la
demo es «hoy»: Torso A sin hacer, la última Torso A una semana antes.

Uso, desde la raíz del repositorio:
    GYMPROFIT_DB_PASSWORD=<la de la BD local> python scripts/dia-ejemplo-web.py
Variables opcionales: GYMPROFIT_DB_HOST (127.0.0.1), MARIADB_CLI («mariadb» en el PATH).

Lo que NO sale de la base, con su regla (la misma que la app, citada en cada sitio):
· Las series de hoy: se eligen con LogicaRecordVivo.supera para que el récord caiga en la
  serie 3 (PLAN-UN-DIA.md, §0 punto 12, y 19:00).
· El reloj de la sesión: el trabajo de cada serie a 3,5 s por repetición (el ritmo que usa
  DuracionRutina) más el descanso de la pauta entre series.
"""
import json
import os
import subprocess
import sys
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "web" / "src" / "datos" / "dia-ejemplo.json"
USUARIO = "dani.entrena"
RUTINA = "Torso A"
HOSTS_LOCALES = {"127.0.0.1", "localhost"}
NBSP = " "
# DuracionRutina (app) y ReglasPrograma (API): 3,5 s de trabajo por repetición.
SEGUNDOS_POR_REP = Decimal("3.5")
# sembrar_demo.py, KILOS["Press de banca con barra"]: lo que sube el press de banca cada semana.
ESCALON_BASICO = Decimal("2.5")
# Zonas.java: músculo (grupo del catálogo) → zona; el título, values/strings.xml (zona_titulo_*).
ZONAS = {"PIERNA": "Pierna", "ESPALDA": "Espalda", "PECHO": "Pecho", "HOMBROS": "Hombros",
         "BRAZOS": "Brazos", "CORE": "Core"}


def consulta(sql: str) -> list[list[str]]:
    """Filas de la BD local, como texto. Para si el host no es local."""
    host = os.environ.get("GYMPROFIT_DB_HOST", "127.0.0.1")
    if host not in HOSTS_LOCALES:
        sys.exit(f"Me niego: {host} no es la BD local. Este script solo lee 127.0.0.1 o localhost.")
    clave = os.environ.get("GYMPROFIT_DB_PASSWORD")
    if not clave:
        sys.exit("Falta GYMPROFIT_DB_PASSWORD (la de la BD local).")
    cli = os.environ.get("MARIADB_CLI", "mariadb")
    hecho = subprocess.run([cli, "-N", "-B", "-h", host, "-P", "3308", "-u", "root", "gymprofit_db"],
                           input=sql.encode("utf-8"), env=dict(os.environ, MYSQL_PWD=clave),
                           check=True, capture_output=True)
    return [linea.split("\t") for linea in hecho.stdout.decode("utf-8").splitlines() if linea]


# ── Formatos de la app ───────────────────────────────────────────────────────

def kilos(k: Decimal) -> str:
    """Marcas.kilos: entero «80 kg» o con un decimal «82,5 kg» (NBSP antes de la unidad)."""
    k = k.normalize()
    texto = f"{k:.0f}" if k == k.to_integral() else f"{k:.1f}".replace(".", ",")
    return texto + NBSP + "kg"


def kilos_sin_unidad(k: Decimal) -> str:
    """DecimalFormat «0.##» de la columna Anterior y la pista: «80», «82,5»."""
    k = k.normalize()
    return f"{k:.0f}" if k == k.to_integral() else format(k, "f").rstrip("0").replace(".", ",")


def marca(k: Decimal, reps: int) -> str:
    """record_peso_por_reps: «%1$s × %2$d»."""
    return f"{kilos(k)} × {reps}"


def reloj(segundos: int) -> str:
    """LogicaSesion.reloj: «m:ss»."""
    return f"{segundos // 60}:{segundos % 60:02d}"


def hace(dias: int) -> str:
    """TiempoRelativo.texto con los plurales tiempo_hace_*."""
    if dias == 0:
        return "hoy"
    if dias == 1:
        return "ayer"
    if dias < 7:
        return f"hace {dias} días"
    if dias < 35:
        n = dias // 7
        return f"hace {n} semana" if n == 1 else f"hace {n} semanas"
    n = dias // 30
    return f"hace {n} mes" if n == 1 else f"hace {n} meses"


def supera(nueva: tuple[Decimal, int], previa: tuple[Decimal, int]) -> bool:
    """LogicaRecordVivo.supera con peso: más kilos, o los mismos con más repeticiones."""
    return nueva[0] > previa[0] or (nueva[0] == previa[0] and nueva[1] > previa[1])


# ── El día ───────────────────────────────────────────────────────────────────

def main() -> None:
    (uid, alta), = consulta(f"SELECT id, DATE(fecha_registro) FROM usuarios WHERE username = '{USUARIO}';")
    dia = date.fromisoformat(alta)
    if dia.weekday() != 0:
        sys.exit(f"La demo se sembró el {dia}, que no es lunes: «Hoy toca» no sería Torso A.")
    (rid, duracion), = consulta(
        f"SELECT id, duracion_minutos FROM rutinas WHERE usuario_id = {uid} AND nombre = '{RUTINA}';")
    ejercicios = consulta(
        "SELECT re.ejercicio_id, e.nombre, re.series, re.repeticiones_min, re.repeticiones_max, "
        "re.tiempo_descanso, re.tipo, e.grupo_muscular FROM rutina_ejercicio re "
        f"JOIN ejercicios e ON e.id = re.ejercicio_id WHERE re.rutina_id = {rid} ORDER BY re.orden;")

    # La última Torso A antes del día de ejemplo: la columna «Anterior» y las pistas.
    (sid, inicio), = consulta(
        f"SELECT id, fecha_inicio FROM sesiones_entrenamiento WHERE usuario_id = {uid} AND rutina_id = {rid} "
        f"AND fecha_inicio < '{dia.isoformat()}' ORDER BY fecha_inicio DESC LIMIT 1;")
    ultima = datetime.fromisoformat(inicio.split(".")[0]).date()
    primero = ejercicios[0]
    eid, nombre, series, rmin, rmax, descanso, tipo = primero[0], primero[1], int(primero[2]), \
        int(primero[3]), int(primero[4]), int(primero[5]), primero[6]
    anterior = [(Decimal(p), int(r)) for p, r in consulta(
        "SELECT sr.peso, sr.repeticiones FROM series_realizadas sr JOIN ejercicios_realizados er "
        f"ON er.id = sr.ejercicio_realizado_id WHERE er.sesion_id = {sid} AND er.ejercicio_id = {eid} "
        "ORDER BY sr.numero;")]

    # La mejor marca guardada (CalculadoraRecords: la más pesada; a igual peso, más reps).
    todas = [(Decimal(p), int(r)) for p, r in consulta(
        "SELECT sr.peso, sr.repeticiones FROM series_realizadas sr JOIN ejercicios_realizados er "
        "ON er.id = sr.ejercicio_realizado_id JOIN sesiones_entrenamiento s ON s.id = er.sesion_id "
        f"WHERE s.usuario_id = {uid} AND er.ejercicio_id = {eid} AND s.fecha_inicio < '{dia.isoformat()}' "
        "AND sr.peso IS NOT NULL;")]
    mejor = todas[0]
    for m in todas[1:]:
        if supera(m, mejor):
            mejor = m

    # Hoy: igualar la marca en las dos primeras (no es récord) y, en la tercera, subir el escalón
    # de los básicos con las repeticiones del centro de la pauta (sí lo es, por los kilos).
    hoy = [mejor, mejor, (mejor[0] + ESCALON_BASICO, (rmin + rmax) // 2)]
    record = next(i for i, s in enumerate(hoy) if supera(s, mejor))
    if record != 2:
        sys.exit(f"Con estos datos el récord caería en la serie {record + 1}, no en la 3.")

    # «Última: <kilos>» del carrusel (InicioHoyDetalle.mejorPeso): el mayor peso de esa vez.
    ultimos = dict(consulta(
        "SELECT er.ejercicio_id, MAX(sr.peso) FROM series_realizadas sr JOIN ejercicios_realizados er "
        f"ON er.id = sr.ejercicio_realizado_id WHERE er.sesion_id = {sid} AND sr.peso IS NOT NULL "
        "GROUP BY er.ejercicio_id;"))

    # Zonas (ZonasDelDia): series por zona, de más a menos, estable; las tres primeras.
    por_zona: dict[str, int] = {}
    for e in ejercicios:
        por_zona[e[7]] = por_zona.get(e[7], 0) + int(e[2])
    zonas = sorted(por_zona, key=lambda z: -por_zona[z])[:3]
    zonas_texto = ", ".join(ZONAS[z] for z in zonas[:-1]) + " y " + ZONAS[zonas[-1]]

    # El reloj de la sesión: trabajo de cada serie y el descanso de la pauta entre ellas.
    tiempos, t = [], 0
    for i, (_, reps) in enumerate(hoy):
        t += int(SEGUNDOS_POR_REP * reps)
        tiempos.append(t)
        t += descanso
    volumen, acumulado = [], 0
    for k, reps in hoy:
        acumulado += int(k * reps)
        volumen.append(acumulado)
    total_series = sum(int(e[2]) for e in ejercicios)

    datos = {
        "_origen": {
            "orden": "GYMPROFIT_DB_PASSWORD=… python scripts/dia-ejemplo-web.py",
            "sacado": date.today().isoformat(),
            "base": "MariaDB local 127.0.0.1:3308 (GP-282)",
            "cuenta": "@" + USUARIO + ", sembrada el " + dia.isoformat() + " con scripts/sembrar_demo.py",
            "reglas": [
                "Series de hoy: 1 y 2 igualan la mejor marca (igualar no es récord); la 3 sube el escalón "
                "semanal del press de banca de sembrar_demo.py con las repeticiones del centro de la pauta, y supera la "
                "marca por los kilos (LogicaRecordVivo.supera).",
                "Reloj: 3,5 s por repetición (DuracionRutina) más el descanso de la pauta entre series.",
            ],
        },
        "dia": {"fecha": dia.isoformat(), "nombre": "lunes"},
        "horas": {"entrada": "07:00", "entreno": "19:00", "record": "19:40"},
        "hoyToca": {
            "rotulo": "Hoy toca",
            "rutina": RUTINA,
            "subtitulo": f"{len(ejercicios)} ejercicios · unos{NBSP}{duracion}{NBSP}min · {zonas_texto}",
            "ultimaVez": f"La última vez, {hace((dia - ultima).days)}",
            "ejercicios": [{"nombre": e[1], "pauta": f"{e[2]} × {e[3]}–{e[4]}",
                            "ultima": "Última: " + kilos(Decimal(ultimos[e[0]])) if e[0] in ultimos else None}
                           for e in ejercicios],
        },
        "sesion": {
            "totalSeries": total_series,
            "ejercicio": nombre,
            "pauta": f"{series} × {rmin}–{rmax}",
            "descanso": f"Descanso {descanso // 60}{NBSP}min" if descanso >= 120 and descanso % 60 == 0
                        else f"Descanso {descanso}{NBSP}s",
            "descansoSegundos": descanso,
            "descansoTexto": reloj(descanso),
            "recordAntes": "Tu récord: " + marca(*mejor),
            "recordDespues": "Tu récord: " + marca(*hoy[2]) + ", de hoy",
            "ultimaVez": f"La última vez, {hace((dia - ultima).days)}",
            "aviso": "¡Récord! " + marca(*hoy[2]),
            "avisoAntes": "Tu mejor marca era " + marca(*mejor),
            "tipo": tipo,
            # Las marcas en bruto, para que UnDiaWebTest las pase por LogicaRecordVivo.supera.
            "marcaGuardada": {"peso": format(mejor[0].normalize(), "f"), "repeticiones": mejor[1]},
            # values/strings.xml: envivo_descanso, envivo_descanso_menos, _mas y _saltar.
            "botones": {"descanso": "Descanso", "menos": f"−15{NBSP}s", "mas": f"+15{NBSP}s", "saltar": "Saltar"},
            "series": [{
                "numero": i + 1,
                "anterior": f"{kilos_sin_unidad(a[0])} × {a[1]}",
                "pistaKg": kilos_sin_unidad(a[0]),
                "pistaReps": str(a[1]),
                "kg": kilos_sin_unidad(h[0]),
                "pesoExacto": format(h[0].normalize(), "f"),
                "reps": str(h[1]),
                "volumen": f"{volumen[i]:,}".replace(",", "."),
                "tiempo": reloj(tiempos[i]),
                "record": i == record,
                "siguiente": (f"Siguiente: serie {i + 2} · {marca(*anterior[i + 1])}" if i + 1 < len(hoy) else None),
                "aviso": (f"¡A por la serie {i + 2}!" if i + 1 < len(hoy) else None),
            } for i, (a, h) in enumerate(zip(anterior, hoy))],
            "finTexto": f"{nombre} · se acabó el descanso",
        },
    }
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(json.dumps(datos, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"Escrito {SALIDA.relative_to(RAIZ)}: {RUTINA} del {dia}, récord {marca(*hoy[2])} "
          f"sobre {marca(*mejor)}.")


if __name__ == "__main__":
    main()
