# Control Gas LP Android — proyecto nativo

Este paquete es un **proyecto Android nativo** (interfaz hecha con controles de Android, no con WebView) para gestionar el negocio de suministro de gas LP en pipa. Se conecta a la misma hoja de cálculo de Google Sheets que ya utiliza Control Gas LP en la computadora.

## Qué incluye

- Panel de ventas del día/mes, litros, cobros, gastos y saldos pendientes.
- Clientes: alta y listado.
- Ventas: litros × precio/litro y pago inicial.
- Créditos/cobranza: abonos por venta y saldo restante.
- Agenda: suministro, cobranza y seguimientos; marcar eventos realizados.
- Gastos y reportes mensuales.
- Configuración local de la URL y una clave API.
- Flujo de GitHub Actions para compilar gratuitamente un APK de depuración firmado.
- API de Google Apps Script para conectarse a la hoja existente sin hacer pública la app web actual.

## Importante: qué está y qué no está listo

El **código fuente de la app nativa está preparado**, pero este paquete no contiene todavía un APK compilado. El entorno en el que se preparó no tiene Android SDK/Gradle instalado ni acceso al servidor necesario para descargarlos. El workflow incluido compila el APK en GitHub Actions, sin pagar un servicio de compilación. Luego tendrás que descargar el artefacto `Control-Gas-LP-APK` e instalar `app-debug.apk` en tu Android.

## Orden recomendado

1. Conserva intacto el proyecto web que ya funciona y la hoja de cálculo actual.
2. Sigue `AppsScript_API/README_API_ES.md` para crear un segundo proyecto Apps Script que solo funcione como API Android. No pegues esa API sobre el código de tu aplicación web.
3. Sube el contenido de este kit a un repositorio GitHub. Incluye la carpeta oculta `.github/workflows` en la raíz del repositorio y la carpeta `AndroidApp` completa.
4. El flujo `Compilar Control Gas LP para Android` compilará automáticamente el APK al subir el código a la rama `main`, o puedes iniciarlo desde la pestaña Actions → Run workflow.
5. Descarga el artefacto `Control-Gas-LP-APK` del resultado de Actions. Descomprímelo para obtener `app-debug.apk`.
6. Instala el APK en el teléfono. La primera vez, introduce la URL `/exec` del proyecto **API Android separado** y su `API_KEY`.
7. Primero prueba con datos de prueba. No uses operaciones reales hasta confirmar que aparecen en la hoja compartida.

## Generar el APK gratis en GitHub

- Crea una cuenta gratuita en GitHub si no tienes una.
- Crea un repositorio llamado `ControlGasLPAndroid`.
- Sube las carpetas `AndroidApp`, `AppsScript_API`, `docs`, el archivo `README_ES.md` y la carpeta `.github` que viene en este kit. No subas capturas ni exportaciones de tu base de datos.
- Confirma que `.github/workflows/build-apk.yml` quede exactamente en la raíz del repositorio, no dentro de `AndroidApp`.
- Abre la pestaña **Actions** y busca **Compilar Control Gas LP para Android**. Abre la última ejecución. Cuando termine en verde, entra al resultado y descarga **Control-Gas-LP-APK** en la sección Artifacts.
- GitHub crea un APK de depuración que se puede instalar en el teléfono para uso personal. No es una versión publicada en Play Store.

## Seguridad de la API

La API debe ser un proyecto Apps Script separado y estar protegida por la API_KEY. Deja la implementación de la app web actual en “Solo yo”. No compartas la API_KEY ni el enlace privado con otras personas. Consulta `AppsScript_API/README_API_ES.md`.
