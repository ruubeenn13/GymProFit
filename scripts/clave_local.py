"""Regenera la contraseña de una cuenta de la BD LOCAL de pruebas y la guarda fuera de git.

Las claves de las cuentas que no son de uso diario (admin, la demo @dani.entrena) no se
escriben en el repositorio: viven en docs/credenciales-locales.txt, que .gitignore excluye
entero (la carpeta docs/). Este script genera una clave nueva, pone su hash BCrypt en la
tabla usuarios de la MariaDB del 3308 y actualiza la línea «usuario=clave» de ese fichero.
No imprime la clave.

Solo toca 127.0.0.1:3308: no recibe host ni puerto, así que no puede llegar a producción.

Uso (ver scripts/README.md y documentacion/ENTORNO-PRUEBAS.md):
    GYMPROFIT_DB_PASSWORD=... python scripts/clave_local.py admin
Variable opcional: MARIADB_CLI (por defecto el cliente de MariaDB 11.8 de esta máquina).
"""
import os
import secrets
import string
import subprocess
import sys
from pathlib import Path

import bcrypt

RAIZ = Path(__file__).resolve().parent.parent
FICHERO = RAIZ / "docs" / "credenciales-locales.txt"
CLI = os.environ.get("MARIADB_CLI", r"C:\Program Files\MariaDB 11.8\bin\mariadb.exe")
# Sin «!» ni símbolos raros: «adb shell input text» no los teclea (ENTORNO-PRUEBAS.md)
ALFABETO = string.ascii_letters + string.digits


def generar_clave() -> str:
    """Clave aleatoria con mayúscula, minúscula y dígito, acabada en punto como las demás locales."""
    while True:
        cuerpo = "".join(secrets.choice(ALFABETO) for _ in range(16))
        if any(c.isupper() for c in cuerpo) and any(c.islower() for c in cuerpo) and any(c.isdigit() for c in cuerpo):
            return cuerpo + "."


def bd_local(clave_bd: str, sql: str) -> str:
    """Ejecuta una sentencia en la BD local del 3308 y devuelve la salida sin cabeceras."""
    entorno = dict(os.environ, MYSQL_PWD=clave_bd)
    salida = subprocess.run([CLI, "-N", "-h", "127.0.0.1", "-P", "3308", "-u", "root", "gymprofit_db", "-e", sql],
                            env=entorno, capture_output=True, text=True)
    if salida.returncode != 0:
        sys.exit(f"La BD local respondió con error: {salida.stderr.strip()[:300]}")
    return salida.stdout.strip()


def guardar(usuario: str, clave: str) -> None:
    """Sustituye o añade la línea «usuario=clave» en el fichero local ignorado por git."""
    FICHERO.parent.mkdir(exist_ok=True)
    lineas = FICHERO.read_text(encoding="utf-8").splitlines() if FICHERO.exists() else []
    lineas = [l for l in lineas if not l.startswith(usuario + "=")] + [f"{usuario}={clave}"]
    FICHERO.write_text("\n".join(lineas) + "\n", encoding="utf-8")


def main() -> None:
    for flujo in (sys.stdout, sys.stderr):
        flujo.reconfigure(encoding="utf-8")  # la consola de Windows rompe las «» en cp1252
    if len(sys.argv) != 2 or not all(c.isalnum() or c in "._-" for c in sys.argv[1]):
        sys.exit("Uso: python scripts/clave_local.py <usuario>")
    usuario, clave_bd = sys.argv[1], os.environ.get("GYMPROFIT_DB_PASSWORD")
    if not clave_bd:
        sys.exit("Falta GYMPROFIT_DB_PASSWORD (la de la BD local).")
    if bd_local(clave_bd, f"SELECT COUNT(*) FROM usuarios WHERE username = '{usuario}'") != "1":
        sys.exit(f"No hay ninguna cuenta «{usuario}» en la BD local.")
    clave = generar_clave()
    hash_ = bcrypt.hashpw(clave.encode("utf-8"), bcrypt.gensalt(prefix=b"2a")).decode("ascii")
    # version_sesion + 1 cierra las sesiones abiertas con la clave vieja, como un cambio de verdad (DEC-058)
    bd_local(clave_bd, f"UPDATE usuarios SET password = '{hash_}', version_sesion = version_sesion + 1 "
                       f"WHERE username = '{usuario}'")
    guardar(usuario, clave)
    print(f"Clave de «{usuario}» regenerada en la BD local y guardada en {FICHERO.relative_to(RAIZ)}.")


if __name__ == "__main__":
    main()
