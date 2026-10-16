# Guía rápida — instalar Control Gas LP Android

## A. Preparar el servidor (solo una vez)

Sigue las instrucciones completas de `AppsScript_API/README_API_ES.md`. Al finalizar tendrás:
- una URL de aplicación web que termina en `/exec` (API Android);
- una clave `API_KEY` larga copiada del registro de ejecución.

La hoja de datos existente no cambia; el proyecto API la abre por su ID.

## B. Compilar el APK sin pagar

1. Entra a GitHub y crea un repositorio `ControlGasLPAndroid`.
2. Sube la carpeta `AndroidApp` y también `AppsScript_API`, `docs`, `README_ES.md` y `.github` a la raíz de ese repositorio.
3. Abre **Actions**. Espera a que termine el flujo **Compilar Control Gas LP para Android**.
4. Entra en la ejecución terminada y descarga el artefacto **Control-Gas-LP-APK**.
5. Descomprime el ZIP descargado para sacar `app-debug.apk`.

## C. Instalar y conectar

1. Envía `app-debug.apk` a tu Android o descárgalo desde el teléfono.
2. Abre el archivo. Android podría pedir permitir instalaciones desde esa fuente; autorízalo solo para el navegador/gestor con el que descargaste el APK y solo si el archivo proviene de tu propia compilación de GitHub.
3. Abre **Control Gas LP**.
4. Pega la URL `/exec` de la API Android y la `API_KEY` guardada en privado.
5. Pulsa **Probar conexión y guardar**.

Si la API responde “No autorizado”, comprueba la clave. Si muestra una respuesta HTML o no es JSON, comprueba que pegaste la URL de la API Android y no la URL de la app web antigua.
