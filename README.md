# Hermanos Limpieza Android v1.0.0

Aplicación Android nativa para la plataforma **Hermanos Limpieza v1.13.14**.

## Funciones incluidas

- Inicio de sesión mediante token móvil.
- Servicios del día con detalle.
- Calendario móvil por rango mensual.
- Crear nuevos servicios.
- CRM de WhatsApp sobre Chatwoot.
- Ver conversaciones y mensajes.
- Enviar mensajes desde el CRM.
- Analizar una conversación con la IA ya configurada en Hermanos Limpieza.
- Crear un servicio directamente desde los datos extraídos del chat.
- Avatares de contactos provenientes de Chatwoot.
- Persistencia segura del token usando Android Keystore.

## Backend requerido

El directorio `backend_patch/` contiene:

- `mobile_api.php`
- `01_mobile_api_tokens.sql`

Sube `mobile_api.php` a la misma carpeta donde están `bootstrap.php`, `crm_lib.php`,
`chatwoot_lib.php`, etc. Ejecuta una sola vez la migración SQL.

La API reutiliza usuarios, roles, agenda, Chatwoot, Evolution e IA de la plataforma existente.

## URL de la API

La compilación usa `API_BASE_URL`.

Ejemplo:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=https://tudominio.com/ruta/
```

También acepta la variable de entorno `API_BASE_URL`.

La URL **debe terminar con `/`**.

## Compilar en GitHub Actions

El workflow `.github/workflows/android.yml` instala Java 17, Android SDK 35 y Gradle 8.9,
y genera el APK debug como artifact.

En GitHub configura un Repository Variable:

`API_BASE_URL = https://tu-dominio.com/ruta/`

Si no se configura, el proyecto compila usando `https://example.com/`, pero la app no podrá
conectarse a tu servidor hasta recompilar con la URL correcta.

## Requisitos

- Android 8.0 (API 26) o superior.
- Backend HTTPS.
- Hermanos Limpieza v1.13.14.
- Chatwoot configurado para usar CRM.
- IA OpenAI/Gemini configurada para el usuario que vaya a analizar conversaciones.
