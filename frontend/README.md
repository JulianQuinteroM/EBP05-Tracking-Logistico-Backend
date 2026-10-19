# Frontend LogiFlow — sprint 2

Este frontend Next.js mantiene el proxy de API hacia el backend Spring Boot del sprint 1. El diseño oscuro toma como referencia el ZIP y PDF entregados por el equipo de front, pero los datos de las pantallas provienen del backend real.

Para arrancarlo, primero enciende PostgreSQL y el backend desde la raíz con `./scripts/run-local.ps1`. En otra PowerShell, dentro de `frontend/`, ejecuta `npm.cmd ci` si faltan dependencias y luego `./scripts/run-front-local.ps1`. Si PowerShell bloquea el script, usa `Set-ExecutionPolicy -Scope Process RemoteSigned -Force` en esa terminal. El frontend pide las mismas contraseñas de operador, administrador y consulta que se eligieron al arrancar el backend. Abre `http://localhost:3000`.

Las rutas `app/api/**` son un proxy: el navegador solicita al frontend y solo el servidor Next agrega la credencial Basic correspondiente al backend. La cookie de sesión está firmada y no contiene contraseñas. La consulta pública pasa por una respuesta reducida; no se carga una ficha completa para ocultarla visualmente.

Pantallas: inicio, nuevo envío, búsqueda/seguimiento, listado, ficha con edición/estado/ubicación, auditoría de administrador y marcador de Reportes para sprint 3. La eliminación permanente se desactivó por la conservación del historial. El contrato completo está en [API_SPRINT2.md](../docs/API_SPRINT2.md).

Verificación: `npm.cmd run lint`, `npx tsc --noEmit` y `npx next build --webpack`. Si Windows/OneDrive bloquea la limpieza de `.next`, se puede usar temporalmente `$env:NEXT_DIST_DIR='.next-sprint2'; npx next build --webpack`; esta carpeta de verificación está ignorada por Git.
