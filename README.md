# Tracking logístico — Sprint 1 (backend y front)

Este repositorio reúne temporalmente el backend Spring Boot en la raíz y el front Next.js en `frontend/` para las HU001-HU005: registrar, buscar por código, listar, editar y cancelar envíos. También incluye la extensión acordada de eliminación permanente. La integración se probó localmente; **subir código a GitHub no despliega la aplicación ni la base de datos**. Antes de devolver el front a su repositorio original, hay que comparar los cambios actuales del equipo de front para no sobrescribirlos. Las HU acordadas están en el entregable de revisión del proyecto.

## Tecnologías

Spring Boot 3.5.16, Java 21 como versión de compilación (puede compilarse y ejecutarse con JDK 25), Maven, PostgreSQL y Flyway. La base del sprint contiene `envio`, `evento_envio` y `auditoria_envio`; la migración es `src/main/resources/db/migration/V1__sprint1.sql`.

Spring anunció 3.5.16 como la última versión con soporte OSS de la rama 3.5.x. Se usa aquí para el sprint académico y la compatibilidad JDK 25; antes de un despliegue público sostenido debe evaluarse actualizar a una rama con soporte vigente.

## Ejecución local

1. Crear una base PostgreSQL `tracking_logistico` y un usuario con permisos sobre ese esquema. La aplicación crea las tablas mediante Flyway al arrancar.
2. Configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OPERATOR_USERNAME`, `OPERATOR_PASSWORD` (mínimo 12 caracteres) y `FRONTEND_ORIGIN`. Consultar `.env.example`. No guardar contraseñas reales en Git.
3. En Windows, ejecutar `./scripts/run-local.ps1` desde PowerShell en la raíz del repositorio. Pide la contraseña del rol PostgreSQL `tracking` y una contraseña nueva de operador; no las escribe en un archivo ni en el historial. Mantener esa terminal abierta mientras se use la API. Para pruebas automatizadas, ejecutar `mvn test`.
4. Probar `GET http://localhost:8080/actuator/health`. Las operaciones internas usan HTTP Basic con credenciales de operador; el rastreo por código es público y mínimo.
5. En otra PowerShell, entrar en `frontend/`, ejecutar `npm.cmd ci` la primera vez y luego `./scripts/run-front-local.ps1`. El script pide **la misma contraseña de operador** y arranca Next en `http://localhost:3000`. Consultar `frontend/README.md` para el ingreso y las rutas conectadas.

Sin PostgreSQL local no se puede hacer una prueba de arranque contra la base definitiva. Las pruebas automatizadas sí ejercitan migración, API y persistencia con H2 en modo compatible con PostgreSQL; H2 es **solo una dependencia de pruebas**, no la base del producto.

## Reglas confirmadas

- Solo se captura dirección de destino. Remitente y destinatario son contactos históricos del envío, no cuentas de usuario.
- `PROGRAMADO` fija una fecha futura de **entrega**, nunca fecha de recolección.
- Costos informativos provisionales: Estándar $10.000 COP (hasta 5 días), Exprés $25.000 COP (hasta 2 días), Programado $15.000 COP (fecha elegida). No se cobra ni se promete una tarifa real.
- Código `LOG-########-D`, con D de Luhn sobre las ocho cifras. Es único e inmutable.
- Estado inicial `PENDIENTE_RECOGIDA`. Solo en ese estado se permite editar o cancelar. Cancelar pasa a `CANCELADO` y conserva el registro, eventos y código. Eliminar permanentemente es una acción separada, exige confirmación y borra el envío y sus registros dependientes.
- El seguimiento público no devuelve nombres, teléfonos, correos, dirección, auditoría ni ID interno. La ficha completa se consulta por un endpoint interno autorizado.
- Las ediciones/cancelaciones requieren la versión leída en la ficha. Un cambio concurrente devuelve HTTP 409.

## Alcance y seguridad

Hay dos clases de acceso en este sprint: visitante/cliente para rastreo público limitado y operador autenticado para las cinco operaciones internas. La identidad de operador se configura en variables de entorno y no persiste en la base. Cuentas de cliente, permisos administrables y roles persistentes corresponden a HU futuras. HTTP Basic es un mecanismo temporal para probar la API; el front en `frontend/` usa un proxy y una sesión local para que la contraseña **no** quede en JavaScript del navegador. En un despliegue público se necesitan HTTPS y una revisión de autenticación/seguridad.

El contrato de endpoints y ejemplos está en `docs/API_SPRINT1.md`. La integración temporal del front está en `frontend/` y sus diferencias respecto a los mockups se explican en `docs/ACUERDOS_FRONT_SPRINT1.md`. El BPMN y el MER revisados son entregables académicos aparte; no son necesarios para ejecutar el código.
