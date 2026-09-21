# Tracking logístico — front integrado para Sprint 1

Esta carpeta contiene temporalmente el front integrado dentro del repositorio del backend. Su origen fue una copia del ZIP `my-app-master.zip`; antes de sincronizar con el repositorio original del equipo de front, se deben comparar posibles cambios posteriores. El front corre en Next.js 16 y consume Spring Boot; las rutas `/api/*` de Next son un intermediario, **no reemplazan** las rutas ni la base de datos del backend.

## Arranque local en Windows

1. Arranca PostgreSQL y el backend desde la raíz de este repositorio con `./scripts/run-local.ps1`. Comprueba `http://localhost:8080/actuator/health` → `{ "status": "UP" }`. Mantén abierta esa terminal.
2. Abre otra PowerShell en esta carpeta. Ejecuta `npm.cmd ci` la primera vez.
3. Ejecuta `./scripts/run-front-local.ps1`. Si PowerShell bloquea `.ps1`, ejecuta primero `Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned -Force` **en esa misma ventana**. El script pide la **misma contraseña de operador** usada al arrancar el backend, genera un secreto de sesión temporal y ejecuta Next ligado a `127.0.0.1` (solo esta PC). No escribe las contraseñas en archivos ni en el historial.
4. Abre `http://localhost:3000` en Edge o Chrome. Busca un envío por código sin ingresar. Para crear, listar, editar, cancelar o eliminar permanentemente, elige “Ingresar como operador” e introduce la misma contraseña.

Las variables requeridas aparecen en `.env.example` solo como referencia. No rellenes ese archivo con contraseñas reales. Si arrancas Next manualmente, configura `BACKEND_URL`, `OPERATOR_USERNAME`, `OPERATOR_PASSWORD` y `SESSION_SECRET` únicamente en el entorno del proceso. Nunca uses `NEXT_PUBLIC_*` para secretos. `SESSION_SECRET` debe tener al menos 32 caracteres; el script lo genera automáticamente y cambia al reiniciar el front, por lo que las sesiones anteriores caducan.

## Rutas conectadas

| Pantalla | Ruta Next | Ruta Spring Boot |
| --- | --- | --- |
| Nuevo envío | `POST /api/estimaciones`, `POST /api/envios` | `POST /api/estimaciones`, `POST /api/envios` |
| Seguimiento público | `GET /api/seguimiento/{codigo}` y página `/envios/info?codigo=...` | `GET /api/seguimiento/{codigo}` |
| Historial | `GET /api/envios` | `GET /api/envios` |
| Ficha | `GET /api/envios/{id}` | `GET /api/envios/{id}` |
| Editar | `PUT /api/envios/{id}` | `PUT /api/envios/{id}` |
| Cancelar | `POST /api/envios/{id}/cancelacion` | `POST /api/envios/{id}/cancelacion` |
| Eliminar permanentemente | `DELETE /api/envios/{id}` | `DELETE /api/envios/{id}` |

Las rutas privadas de Next requieren una cookie de operador firmada, `HttpOnly` y `SameSite=Strict`; solo Next envía HTTP Basic al backend. El navegador nunca recibe esa cabecera ni la contraseña configurada en el proceso. Esta solución es **para demostración local del Sprint 1**, no una autenticación de producción: antes de publicar se necesitan HTTPS, usuarios/roles persistentes, controles contra intentos de acceso y revisión de seguridad.

## Límites acordados con el equipo

- El seguimiento público solo muestra código, estado, servicio, fecha estimada y último evento. La ficha privada muestra contactos y dirección. El mockup público que enseña nombres, direcciones, “ubicación actual” o un historial inventado debe corregirse o redefinirse con el equipo.
- **Cancelar** pide motivo y conserva el envío como `CANCELADO`. **Eliminar permanentemente** es una acción diferente: exige escribir el código de seguimiento y borra el envío, sus eventos y su auditoría. Editar/cancelar solo se permiten en `PENDIENTE_RECOGIDA`; HTTP 409 exige recargar la ficha antes de cualquier operación con una versión desactualizada.
- Dimensiones, dirección de origen, transición `RECOGIDO`, filtros por fecha/estado, exportación, métricas y reportes no tienen contrato en las cinco HU del Sprint 1. No se muestran como datos reales.
- La fecha de `PROGRAMADO` es la de **entrega**, no la de recolección. Precios y tiempos son informativos.

## Verificación

`npm.cmd run lint` y `npm.cmd run build` deben pasar. Con ambos servidores encendidos, visita `http://localhost:3000/api/seguimiento/LOG-00000001-8` (si ese envío sigue existiendo) y comprueba que el resultado coincide con el de `http://localhost:8080/api/seguimiento/LOG-00000001-8`. Una llamada sin sesión a `http://localhost:3000/api/envios` debe devolver HTTP 401, no contactos privados.
