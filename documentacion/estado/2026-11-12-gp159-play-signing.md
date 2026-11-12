# GP-159, paso 2 — App Bundle y Play App Signing

12-11-2026. Solo app (Gradle) y documentación. Sin versión ni etiqueta (DEC-056): el AAB sale como 1.8.0 y no se sube. Decisión: DEC-063. Procedimiento: `documentacion/PUBLICAR-EN-PLAY.md`.

## Qué cambia

- `app/build.gradle`: `bundleRelease` firma con la clave de subida (`upload.properties`), `assembleRelease` sigue con la actual (`keystore.properties`). AGP usa un solo `signingConfig` por variante para APK y AAB, así que la release lo elige por las tareas pedidas en la orden; pedir un bundle y un APK de release a la vez se rechaza. `-PreleaseCi` gana siempre y no cambia.
- `.gitignore` de la app: `upload.properties`, `*.p12`, `*.pem`, `*.pepk`.
- La clave actual solo se leyó (`keytool -list`, `apksigner`, `bundletool` para firmar el APK universal de prueba). No se movió ni se reescribió.

## Comprobaciones

### 1. Clave actual (solo lectura)

```
$ keytool -list -v -keystore gymprofit-release.keystore -storepass:env KSP
Tipo de Almacén de Claves: PKCS12
Nombre de Alias: gymprofit
Tipo de Entrada: PrivateKeyEntry
Propietario: CN=Rubén Juan Candela, OU=Unknown, O=Unknown, L=Crevillente, ST=Alicante, C=ES
Válido desde: Tue May 05 13:21:22 CEST 2026 hasta: Sat Sep 20 13:21:22 CEST 2053
	 SHA256: 91:D1:41:C2:B4:34:ED:EA:BB:DB:E5:44:F7:B4:65:7C:D7:17:3C:2F:1F:D4:68:42:98:C1:BF:B0:F6:D7:22:FD
Nombre del algoritmo de firma: SHA384withRSA
Algoritmo de clave pública de asunto: Clave RSA de 2048 bits
```

RSA de 2048: cumple. Contra la release 1.8.0 (`versionCode` 10800):

```
$ apksigner verify --verbose --print-certs app-release.apk
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate SHA-256 digest: 91d141c2b434edeabbdbe544f7b4657cd7173c2f1fd4684298c1bfb0f6d722fd
Signer #1 key algorithm: RSA
Signer #1 key size (bits): 2048
```

Coincide.

### 2. Clave de subida

RSA 4096, `SHA256withRSA`, 10 950 días, alias `upload`, PKCS12, fuera del repositorio. Contraseña aleatoria, solo en `upload.properties` (no versionado).

```
$ keytool -list -v -keystore gymprofit-upload.jks
Nombre de Alias: upload
Válido desde: Thu Nov 12 17:23:10 CET 2026 hasta: Sat Nov 04 17:23:10 CET 2056
	 SHA256: 62:06:1C:7B:26:D1:F4:51:2A:79:3D:5B:35:38:EC:4F:34:00:EE:BA:89:13:4D:20:5C:32:EA:5B:1C:5B:7E:A7
Nombre del algoritmo de firma: SHA256withRSA
Algoritmo de clave pública de asunto: Clave RSA de 4096 bits
```

Certificado exportado con `keytool -exportcert -rfc` a `upload_certificate.pem`.

### 3. AAB y APK

```
$ keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
Propietario: CN=GymProFit Upload, O=GymProFit, L=Crevillente, ST=Alicante, C=ES
	 SHA256: 62:06:1C:7B:26:D1:F4:51:2A:79:3D:5B:35:38:EC:4F:34:00:EE:BA:89:13:4D:20:5C:32:EA:5B:1C:5B:7E:A7
Algoritmo de clave pública de asunto: Clave RSA de 4096 bits

$ apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk   # tras assembleRelease
Signer #1 certificate SHA-256 digest: 91d141c2b434edeabbdbe544f7b4657cd7173c2f1fd4684298c1bfb0f6d722fd
```

Guardas:

- `bundleRelease assembleRelease` en una orden → falla: «bundleRelease y un APK de release en la misma orden: cada uno se firma con una clave distinta…».
- Sin `upload.properties`: `bundleRelease` falla con «No se puede construir el AAB de Play: falta upload.properties…»; `assembleDebug` compila.

### 4. Lo que entregará Play: APK universal firmado con la clave actual

`bundletool` 1.18.3, `build-apks --mode=universal` con `gymprofit-release.keystore` (contraseñas por `file:` temporal, borrado al acabar).

```
$ apksigner verify --print-certs universal.apk
Signer #1 certificate SHA-256 digest: 91d141c2b434edeabbdbe544f7b4657cd7173c2f1fd4684298c1bfb0f6d722fd

$ zipalign -c -P 16 -v 4 universal.apk
 3702784 lib/arm64-v8a/libandroidx.graphics.path.so (OK)
 3719168 lib/arm64-v8a/libdatastore_shared_counter.so (OK)
 3735552 lib/arm64-v8a/libimage_processing_util_jni.so (OK)
 3768320 lib/arm64-v8a/libsurface_util_jni.so (OK)
 3784704 lib/armeabi-v7a/libandroidx.graphics.path.so (OK)
 3801088 lib/armeabi-v7a/libdatastore_shared_counter.so (OK)
 3817472 lib/armeabi-v7a/libimage_processing_util_jni.so (OK)
 3850240 lib/armeabi-v7a/libsurface_util_jni.so (OK)
 3866624 lib/x86/libandroidx.graphics.path.so (OK)
 3883008 lib/x86/libdatastore_shared_counter.so (OK)
 3899392 lib/x86/libimage_processing_util_jni.so (OK)
 3948544 lib/x86/libsurface_util_jni.so (OK)
 3964928 lib/x86_64/libandroidx.graphics.path.so (OK)
 3981312 lib/x86_64/libdatastore_shared_counter.so (OK)
 3997696 lib/x86_64/libimage_processing_util_jni.so (OK)
 4063232 lib/x86_64/libsurface_util_jni.so (OK)
Verification successful
```

16 `.so`, todas en desplazamientos múltiplo de 16 384. Las cuatro librerías vienen de AndroidX y CameraX; la app no compila código nativo propio.

### 5. Actualización encima de la 1.8.0 sin perder la sesión

Emulador `Medium_Phone_API_36.1` (x86_64), contra producción, con una cuenta de usar y tirar:

1. `adb install` de la release 1.8.0 (`firstInstallTime=2026-11-12 16:31:52`).
2. Alta de la cuenta por `/auth/register`, Entrar en la app, cuestionario, avisos. Reinicio de la app: abre `MainActivity` con «Hola, prueba159».
3. `adb install -r` del APK universal: `Success`. `lastUpdateTime=16:37:35`, `firstInstallTime` sin cambiar (actualización, no reinstalación).
4. Al abrir: `MainActivity`, «Hola, prueba159», el mismo Inicio. Sesión conservada.
5. Cuenta borrada con `DELETE /usuarios/me` (200); el login después da 401. Fichero de la clave borrado tras comprobarlo.

### 6. Build, tests y lint

`sh ./gradlew assembleDebug test lintDebug` en local, como el CI: verde (exit 0). CI: en el PR.

## Pendiente

- **PEPK**: espera a que el propietario pase `pepk.jar` y la clave de cifrado de Play Console. La orden está en `PUBLICAR-EN-PLAY.md`; la salida va a la carpeta de claves, fuera del repositorio.
- Lo de Play Console (subir la salida de PEPK y el certificado de subida, comparar huellas) lo hace el propietario.

## Tests

No hay test unitario nuevo: el cambio es de configuración de Gradle y su comportamiento solo se ve construyendo. Se comprobó a mano en los dos sentidos (sección 3: guardas que fallan, firmas que salen de cada clave).
