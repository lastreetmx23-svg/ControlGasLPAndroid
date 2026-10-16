# API de Control Gas LP para la app Android

Esta API es un proyecto de Apps Script SEPARADO de la aplicación web actual. Deja intactos el proyecto web y su implementación privada. La API Android comparte la hoja de cálculo existente y utiliza una clave larga para autorizar cada solicitud.

## Configuración

1. Abre la hoja de Google Sheets que usa tu app actual.
2. Copia el ID de la hoja desde la URL: en `https://docs.google.com/spreadsheets/d/ESTE_ES_EL_ID/edit`, copia solo el tramo que ocupa `ESTE_ES_EL_ID`.
3. En `https://script.google.com/`, crea un proyecto nuevo llamado `Control Gas LP - API Android`.
4. En el archivo `Código.gs` del proyecto nuevo, reemplaza el contenido por todo el archivo `NativeApi.gs`.
5. En la primera línea de configuración, reemplaza `PEGA_AQUI_EL_ID_DE_TU_HOJA_DE_CALCULO` por el ID real de tu hoja. No agregues la URL completa ni espacios.
6. Guarda el proyecto. En el selector de funciones elige `setupNativeApi` y pulsa Ejecutar. Autoriza el acceso a Google Sheets si se solicita.
7. Abre el Registro de ejecución. Copia la cadena completa que aparece después de `API_KEY=`. Guárdala en privado.
8. Pulsa Implementar → Nueva implementación → Aplicación web.
9. En “Ejecutar como”, elige tu cuenta. En “Quién tiene acceso”, elige “Cualquier persona” / “Anyone”, ya que la app nativa no utiliza el inicio de sesión web de Apps Script. Implementa y autoriza.
10. Copia la URL de aplicación web terminada en `/exec`. En el teléfono, abre Control Gas LP Android y pega esa URL y la API_KEY en Configuración.

## Seguridad

- Solo este proyecto separado debe tener acceso “Cualquier persona”; no cambies el acceso de tu antigua aplicación web. La app web existente debe permanecer “Solo yo”.
- La API devuelve datos o ejecuta acciones solo si recibe la API_KEY correcta. No compartas la clave, el enlace de la API ni el APK con terceros.
- La API_KEY es un secreto de acceso: protégelo igual que una contraseña. La autenticación por clave es una protección práctica para uso individual, pero no equivale a OAuth empresarial. Si compartes el APK o la clave, genera una clave nueva y configura de nuevo la app.
- No publiques tu ID de hoja, enlace de implementación o API_KEY en capturas públicas o redes sociales.

## No borrar la base de datos

No ejecutes funciones de borrado ni crees otra hoja. `setupNativeApi` solo comprueba el ID y crea una API_KEY la primera vez; conserva la clave en ejecuciones posteriores y no vacía las hojas.
