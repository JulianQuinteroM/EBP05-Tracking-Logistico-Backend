# LogiFlow — seguimiento logístico, sprint 2

El proyecto conserva el backend original Spring Boot y el frontend Next.js en `frontend/`. El ZIP nuevo del equipo de front se tomó como referencia visual; las pantallas de este repositorio consultan la API real y no los datos simulados del ZIP.

## Arranque local en Windows

Requisitos: PostgreSQL con la base `tracking_logistico` y el rol `tracking` ya creados como en el sprint 1; Java 25 (el proyecto compila para Java 21), Maven y Node.js. PostgreSQL debe estar encendido.

1. En PowerShell, desde la raíz, ejecuta `Set-ExecutionPolicy -Scope Process RemoteSigned -Force` si Windows bloquea scripts; luego `./scripts/run-local.ps1`. El script pide la contraseña de PostgreSQL y tres contraseñas nuevas (mínimo 12 caracteres) para `operador`, `administrador` y `consulta`. Mantén abierta la terminal.
2. Comprueba `http://localhost:8080/actuator/health`; debe devolver `{"status":"UP"}`.
3. En otra PowerShell entra en `frontend/`. Si es la primera vez, ejecuta `npm.cmd ci`. Después ejecuta `./scripts/run-front-local.ps1`; escribe **exactamente las mismas tres contraseñas** que pusiste al iniciar el backend. Mantén abierta esta segunda terminal.
4. Abre `http://localhost:3000`. En “Ingresar al panel”, elige la cuenta que usarás. `operador` puede registrar y actualizar envíos; `administrador` además puede ver/exportar auditoría; `consulta` solo puede leer.

Las contraseñas se solicitan cada vez, no se guardan en archivos ni se envían al navegador como variables públicas. La sesión del front es firmada, `HttpOnly`, y se renueva con actividad de API; caduca después de 30 minutos sin ella. Es una solución local provisional para este sprint, no un sistema de usuarios definitivo.

## Cambio importante de base de datos

Al iniciar el backend actualizado, Flyway ejecuta **una sola vez** `V2__sprint2.sql`. Por el acuerdo de que todos los envíos anteriores eran de prueba, esa migración elimina los envíos, eventos y auditorías del sprint 1 y recrea las tablas con dimensiones/empaque obligatorios. **No hagas este arranque si ahora hay datos que debas conservar.** No debes borrar nada manualmente en pgAdmin. Los códigos nuevos pueden comenzar en una serie posterior porque se conserva la secuencia de seguimiento.

Desde la migración V2, la eliminación permanente queda deshabilitada: el sprint 2 exige historial y auditoría conservados al menos dos años. Cancelar sigue disponible mientras el envío esté pendiente y mantiene el registro. Si en el futuro quieren “quitar de la lista”, debe definirse como archivado u ocultamiento, no borrado físico de la evidencia.

## Alcance implementado

- FE 158: HU019 peso/dimensiones con límites y dos decimales; HU020 tipo de empaque y descripción si es “Otro”. En la pantalla se acepta coma o punto decimal; en JSON se envía número. Se editan en Pendiente o Recogido.
- FE 159: HU021 estado público con hora/color e incidencias; HU022 línea de pasos y ventana estimada en días calendario (estándar 3–5, exprés 1–2, programado fecha elegida); HUS01 datos personales reducidos y límite de diez búsquedas fallidas en diez minutos por dirección IP, con bloqueo de quince minutos.
- FE 160: HU023 cambios de estado validados y con confirmación, incidente y regreso al estado previo; HU024 ciudades colombianas sugeridas/validadas y referencia opcional; HUS02 cuentas provisionales `operador`, `administrador`, `consulta`; HUS03 auditoría de cambios/accesos, alertas en panel administrativo y exportación CSV compatible con Excel/Google Sheets.

El recorrido es `PENDIENTE_RECOGIDA → RECOGIDO → EN_TRANSITO → CENTRO_DISTRIBUCION → RUTA_ENTREGA → ENTREGADO`. Una `INCIDENCIA` interrumpe el paso y se resuelve regresando al estado anterior. `ENTREGADO` es final. La ubicación solo puede registrarse desde Recogido y antes de Entregado/Cancelado; el historial se muestra del más reciente al más antiguo. El catálogo local de ubicación incluye las 32 capitales departamentales; no es un catálogo de todos los municipios.

Las pantallas siguen el tema oscuro del diseño nuevo. La página inicial también se adaptó; Reportes y Gestión de Flota quedan señalados como pendientes de sprint 3, sin métricas de prueba presentadas como reales. Los usuarios/actores persistentes y la administración definitiva de permisos también quedan para sprint 3; las tres cuentas actuales se configuran al arrancar.

## API y verificación

Consulta [API_SPRINT2.md](docs/API_SPRINT2.md) para las rutas y ejemplos. El documento [API_SPRINT1.md](docs/API_SPRINT1.md) queda solo como referencia histórica; sus campos y la eliminación permanente ya no son el contrato vigente.

Backend: `mvn -s .mvn/settings.xml test`. Frontend: dentro de `frontend/`, `npm.cmd run lint` y `npx tsc --noEmit`; el build completo puede ejecutarse con `npx next build --webpack` si Turbopack falla en Windows/OneDrive. Las pruebas automáticas usan H2 en modo compatible con PostgreSQL; antes de presentar el sprint conviene hacer una prueba manual completa con PostgreSQL real y ambos servidores encendidos.

## Despliegue de demostración

La guía paso a paso para publicar esta aplicación en GitHub, Neon, Render y Vercel está en [DESPLIEGUE_GRATIS.md](docs/DESPLIEGUE_GRATIS.md). No subas credenciales al repositorio; configura las variables privadas en cada proveedor.
