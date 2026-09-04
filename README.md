# Hermanos Limpieza Android v1.1.0

Aplicación Android nativa en Kotlin + Jetpack Compose para **Hermanos Limpieza**.

## Diseño

Identidad principal:

- Azul `#13287E`
- Amarillo `#FFF323`

La interfaz es móvil, moderna y de alta jerarquía visual: tarjetas redondeadas,
acciones grandes, navegación inferior y CRM compacto.

## Novedades v1.1.0

- Rediseño completo de Inicio, CRM, Chat, Agenda, Login y formularios.
- Hora visible en cada mensaje.
- Hora/fecha del último mensaje en la lista CRM.
- Caché local persistente de conversaciones y mensajes.
- Apertura cache-first + sincronización silenciosa desde Chatwoot.
- Envío optimista de mensajes.
- Edición de servicios para administradores.
- `GET action=me` para recuperar el rol de una sesión guardada.
- `POST action=appointment_update` para edición segura del servicio.

## Actualizar backend desde Android v1.0.0

Reemplaza el `mobile_api.php` de tu servidor por:

`backend_patch/mobile_api.php`

No hay SQL adicional para actualizar de v1.0.0 a v1.1.0.

Para una instalación nueva sí debes ejecutar una vez:

`backend_patch/01_mobile_api_tokens.sql`

## GitHub Actions

El workflow permanece en:

`.github/workflows/android.yml`

Crea/conserva la Repository Variable:

`API_BASE_URL=https://tu-dominio.com/ruta/`

Después ejecuta **Build Android APK** desde la pestaña Actions.

## Arquitectura del CRM

`WhatsApp -> Evolution -> Chatwoot -> mobile_api.php -> Android`

Chatwoot continúa siendo la fuente de verdad. El caché Android solo acelera la
experiencia y puede reconstruirse en cualquier momento.
