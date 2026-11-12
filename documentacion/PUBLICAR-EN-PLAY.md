# Cómo se publica en Play

Decisión: DEC-063 (GP-159). Este documento dice qué clave firma qué, qué se sube y cómo se comprueba. Lo de Play Console lo hace el propietario.

## Las dos claves

| | Clave de firma de la app | Clave de subida |
|---|---|---|
| Fichero de Gradle | `app/GymProFit/keystore.properties` | `app/GymProFit/upload.properties` |
| Alias | `gymprofit` | `upload` |
| Algoritmo | RSA 2048 | RSA 4096 |
| SHA-256 del certificado | `91:D1:41:C2:B4:34:ED:EA:BB:DB:E5:44:F7:B4:65:7C:D7:17:3C:2F:1F:D4:68:42:98:C1:BF:B0:F6:D7:22:FD` | `62:06:1C:7B:26:D1:F4:51:2A:79:3D:5B:35:38:EC:4F:34:00:EE:BA:89:13:4D:20:5C:32:EA:5B:1C:5B:7E:A7` |
| Firma | los APK de `assembleRelease` (los que se reparten fuera de Play) y, en Play, los APK que Play genera | solo el AAB de `bundleRelease` |
| Dónde vive | la tiene Play (exportada con PEPK) y la copia del propietario | solo el propietario, fuera del repositorio |
| Si se pierde | no hay arreglo: los APK repartidos no se actualizan | Play Console la cambia a petición |

Ninguno de los dos `.properties`, ninguna clave, ningún `.pem` y ninguna salida de PEPK entran en el repositorio (`app/GymProFit/.gitignore`). Las huellas de arriba son públicas: el certificado va dentro de cada APK.

Los dos ficheros tienen la misma forma:

```properties
storeFile=<ruta a la clave, fuera del repositorio>
storePassword=…
keyAlias=…
keyPassword=…
```

## Qué orden firma con qué

```bash
cd app/GymProFit
sh ./gradlew bundleRelease     # AAB para Play, con la clave de subida
sh ./gradlew assembleRelease   # APK para repartir, con la clave de firma
```

- Se lanzan **por separado**: AGP firma APK y AAB de una variante con el mismo `signingConfig`, así que la release elige la clave según la orden y pedir las dos a la vez falla con un mensaje.
- Sin `upload.properties`, `bundleRelease` falla al empezar y lo dice. Sin `keystore.properties`, igual `assembleRelease`. El debug y el CI no necesitan ninguno.
- `-PreleaseCi` (GP-247) no cambia: firma con la clave de depuración.

Salida: `app/build/outputs/bundle/release/app-release.aab`.

## Alta de Play App Signing (una sola vez)

1. En Play Console, *Integridad de la app → Firma de apps*: elegir **usar la clave de otro repositorio de claves** y **exportar y subir una clave desde Java keystore (PEPK)**. Play da `pepk.jar` y su clave pública de cifrado (`encryption_public_key.pem`).
2. Exportar la clave de firma, sin sacarla de su sitio, a una carpeta fuera del repositorio:

   ```bash
   java -jar pepk.jar \
     --keystore=<ruta a gymprofit-release.keystore> --alias=gymprofit \
     --output=<carpeta fuera del repo>/pepk/gymprofit-firma.zip \
     --include-cert --rsa-aes-encryption \
     --encryption-key-path=<ruta a encryption_public_key.pem>
   ```

   PEPK solo lee la clave. Pide las contraseñas por la consola; no van en la orden ni en ningún fichero.
3. Subir ese `.zip` a Play Console.
4. Subir el certificado de la clave de subida (`upload_certificate.pem`, exportado con `keytool -exportcert -rfc`).
5. Comparar en Play Console las dos huellas SHA-256 con las de la tabla.

## Comprobar antes de subir

Con `bundletool` (`bundletool-all` de su página de versiones) y las `build-tools` del SDK:

```bash
# El AAB va firmado con la clave de subida
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab

# Lo que entregará Play: un APK universal firmado con la clave de firma
java -jar bundletool-all.jar build-apks --mode=universal \
  --bundle=app/build/outputs/bundle/release/app-release.aab --output=universal.apks \
  --ks=<gymprofit-release.keystore> --ks-key-alias=gymprofit \
  --ks-pass=file:<fichero temporal> --key-pass=file:<fichero temporal>
unzip universal.apks universal.apk

apksigner verify --print-certs universal.apk   # SHA-256 91d141c2…d722fd
zipalign -c -P 16 -v 4 universal.apk           # todas las .so (OK): alineadas a 16 KB
```

En Windows, la ruta de `file:` tiene que ser de Windows (`C:/…`): con `/c/…` de Git Bash `bundletool` no la encuentra. El fichero temporal de la contraseña se borra al acabar.

Y en un emulador: instalar la release anterior, entrar, instalar el universal encima con `adb install -r` y comprobar que sigue dentro y que `firstInstallTime` no cambia.

## Versión

El primer AAB que se suba a Play es la **2.0.0** (DEC-056). Mientras se prueba la firma, el AAB sale con la versión que haya (`1.8.0`) y no se sube.
