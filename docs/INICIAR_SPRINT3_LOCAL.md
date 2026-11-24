# Iniciar y revisar sprint 3 localmente

Rama de trabajo: `codex/sprint3-local`. No se ha realizado push. Los servicios publicados siguen usando la versión de GitHub.

## Primera terminal: backend

PostgreSQL local debe estar iniciado, con la base `tracking_logistico` y el rol `tracking` ya usados en los sprints anteriores.

```powershell
cd "C:\Users\judaq\OneDrive\Documentos\ChatGPT\UdeA\EBP05-Tracking-Logistico-Backend"
Set-ExecutionPolicy -Scope Process RemoteSigned -Force
.\scripts\run-local.ps1
```

El script conecta exclusivamente a `localhost:5432/tracking_logistico`. Pide la contraseña local de PostgreSQL y las contraseñas para las cuentas iniciales. La migración V3 agrega tablas y conserva los envíos del sprint 2. Los documentos de contactos anteriores quedan pendientes de completar. La asociación de fichas anteriores con contactos se realiza al iniciar.

Las cuentas `operador`, `administrador` y `consulta` se crean en la primera ejecución del sprint 3. **Después se conservan las contraseñas guardadas en la base**, aunque escribas otras al reiniciar el script. El administrador gestiona cuentas y contraseñas en Usuarios y permisos. El repartidor se crea desde esa pantalla. Las contraseñas tienen mínimo 12 caracteres y máximo 72 bytes UTF-8; letras con tildes/emojis pueden ocupar más de un byte.

Comprueba `http://localhost:8080/actuator/health` y deja abierta la terminal.

## Segunda terminal: frontend

```powershell
cd "C:\Users\judaq\OneDrive\Documentos\ChatGPT\UdeA\EBP05-Tracking-Logistico-Backend\frontend"
Set-ExecutionPolicy -Scope Process RemoteSigned -Force
.\scripts\run-front-local.ps1
```

Si faltan dependencias, ejecuta primero `npm.cmd ci`. El frontend ya no pide las tres contraseñas del backend: autentica cada cuenta contra la API y genera una sesión privada. Abre `http://localhost:3000` e ingresa como administrador con la contraseña que elegiste en la primera ejecución del sprint 3.

## Activar la IA de Gemini

La gestión de reclamaciones funciona sin IA. Para probar el asistente real:

1. Entra a [Google AI Studio](https://aistudio.google.com/api-keys) con tu cuenta y crea una clave para Gemini API. Usa un proyecto con acceso gratuito disponible; no hace falta activar un plan de pago para esta demostración. La cuota depende de tu cuenta y del modelo.
2. Guarda la clave de forma privada. **No la pegues en este chat, GitHub, el frontend o una captura.**
3. Detén solamente el backend con Ctrl+C y vuelve a iniciarlo así:

```powershell
.\scripts\run-local.ps1 -ConfigureAi
```

4. Cuando pida la clave de Gemini, pégala en esa terminal. El campo es privado, como una contraseña. No se guarda en un archivo.
5. El modelo predeterminado es `gemini-3.1-flash-lite`. Si tu proyecto no tiene acceso, elige uno de texto con nivel gratuito en AI Studio y configura su identificador antes de ejecutar el script:

```powershell
$env:GEMINI_MODEL = "IDENTIFICADOR_DEL_MODELO_DISPONIBLE"
.\scripts\run-local.ps1 -ConfigureAi
```

6. En Atender reclamaciones selecciona un caso, pulsa Preparar contexto, revisa/elimina información personal y confirma la revisión. Ejecuta Analizar con Gemini. Revisa sus hallazgos, acciones y borrador; Usar borrador lo lleva al campo de respuesta y Guardar lo publica.

El nivel gratuito del proveedor tiene cuotas y sus condiciones de tratamiento de datos. Usa datos ficticios para la demo y revisa siempre el contexto antes de enviarlo. El backend omite campos privados y sustituye patrones conocidos, pero no garantiza anonimización completa del texto libre. No envía fotos o firmas al modelo.

## Recorrido recomendado

1. Administrador: crear una cuenta de repartidor y otra de operador; revisar sus permisos.
2. Operador: crear/completar un contacto con documento y registrar un envío seleccionándolo.
3. Avanzar los estados hasta Ruta de entrega. Registrar ubicación y un intento fallido. Revisar la fecha del nuevo intento.
4. Resolver la incidencia indicando la solución. Confirmar entrega con una foto o imagen de firma PNG/JPEG de hasta 1,5 MB.
5. Verificar el seguimiento público: estado actualizado sin evidencia ni datos privados.
6. Registrar una reclamación con el código y correo ficticios del envío. Descargar radicado y clave; consultar el caso.
7. Atender el caso desde el panel, asignar responsable, probar la IA cuando esté configurada y publicar una respuesta. Consultarla con el acceso privado del cliente.
8. Revisar reportes y exportar CSV. Consultar la auditoría filtrada por usuario.
9. Revocar una cuenta con una sesión abierta y comprobar que la API rechace sus operaciones. Reactivarla para volver a ingresar.

Los criterios completos están en [SPRINT3_CRITERIOS.md](SPRINT3_CRITERIOS.md) y las rutas en [API_SPRINT3.md](API_SPRINT3.md).

## Comprobaciones técnicas

```powershell
# Desde la raíz:
mvn -s .mvn/settings.xml test
# Desde frontend:
npm.cmd run lint
npx.cmd tsc --noEmit
npx.cmd next build --webpack
```

Las pruebas automatizadas usan una base H2 de prueba. Además se ejecutó un recorrido con PostgreSQL temporal aislado, sin conectarse a Neon. Los archivos de revisión generados dentro de `target/` son temporales y no forman parte de la aplicación.

Volver a la rama `main` no revierte las migraciones aplicadas en tu base local. Para probar versiones anteriores, usa otra base de datos o una copia compatible. No borres la historia de Flyway ni ejecutes V1/V2 manualmente sobre tus datos.
