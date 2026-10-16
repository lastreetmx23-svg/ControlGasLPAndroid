# Compilar el APK gratuitamente con GitHub

Esta guía usa el workflow incluido. No necesita Android Studio ni descargar el SDK en tu computadora; GitHub compila el APK en sus servidores.

## 1. Crear el repositorio

1. Entra a https://github.com/ y crea una cuenta o inicia sesión.
2. Abre https://github.com/new.
3. En Repository name escribe `ControlGasLPAndroid`.
4. Selecciona **Public** para evitar los límites de minutos de GitHub Actions de repositorios privados. El paquete solo contiene código genérico; no pongas tu API_KEY ni el ID real de la hoja en los archivos del repositorio.
5. Pulsa **Create repository**.

Puedes usar un repositorio privado si lo prefieres; las cuentas gratuitas tienen límites de minutos de Actions y pueden consumirlos. Si el workflow no se ejecuta, revisa la cuota de Actions.

## 2. Subir los archivos

1. Descomprime `Control_Gas_LP_Android_Kit.zip` en tu computadora.
2. Abre el repositorio vacío en GitHub y selecciona **Add file → Upload files**.
3. Desde el Explorador de archivos, arrastra al área de carga las carpetas `AndroidApp`, `AppsScript_API`, `docs`, el archivo `README_ES.md` y la carpeta oculta `.github`.
4. Asegúrate de que la estructura quede en la raíz del repositorio, así:

```
.github/workflows/build-apk.yml
AndroidApp/settings.gradle.kts
AndroidApp/build.gradle.kts
AndroidApp/app/build.gradle.kts
AndroidApp/app/src/main/...
AppsScript_API/NativeApi.gs
AppsScript_API/README_API_ES.md
docs/...
README_ES.md
```

5. En **Commit changes**, escribe `Subir proyecto inicial` y confirma.

Si no puedes arrastrar la carpeta `.github`, crea el archivo en GitHub usando **Add file → Create new file** y en el campo de nombre escribe `.github/workflows/build-apk.yml`. Copia el contenido del archivo `build-apk.yml` incluido en el kit, luego confirma el cambio.

## 3. Descargar el APK

1. Abre la pestaña **Actions** del repositorio.
2. Selecciona **Compilar Control Gas LP para Android**. Al confirmar los archivos en la rama `main`, el workflow debería comenzar automáticamente. Si no, pulsa **Run workflow**.
3. Espera que aparezca la marca verde de ejecución completada.
4. Abre esa ejecución y localiza la sección **Artifacts** al final de la página.
5. Descarga `Control-Gas-LP-APK.zip` y descomprímelo. Dentro estará `app-debug.apk`.

El APK será una compilación de depuración, válida para instalar y probar en tu Android. No es una publicación de Play Store.
