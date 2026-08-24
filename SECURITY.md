# Seguridad

- Usa exclusivamente HTTPS.
- No incluyas credenciales de MySQL, Chatwoot, Evolution, OpenAI o Gemini en el repositorio Android.
- La app solo necesita `API_BASE_URL`.
- Los secretos de integraciones permanecen en el backend existente.
- El token móvil se guarda cifrado mediante Android Keystore.
- Rota el token cerrando sesión o eliminándolo desde `mobile_api_tokens`.
