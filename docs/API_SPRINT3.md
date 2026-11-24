# Contrato API — sprint 3

El backend sigue en `/api`. Next expone el mismo prefijo y transmite las peticiones desde el servidor. La sesión cambió: `POST /auth/login` valida cuentas persistentes y devuelve un token opaco y el usuario. Next guarda el token dentro de una cookie HttpOnly firmada. Nunca se devuelve el token en el JSON de sesión del navegador ni se requieren contraseñas de cuentas en las variables de Next. HTTP Basic sigue disponible para herramientas y pruebas, validando la misma base de cuentas.

## Sesión y usuarios

| Método y ruta | Datos / permiso |
| --- | --- |
| POST `/auth/login` | `{username,password}`; público, con límite de intentos |
| GET `/auth/me` | Cuenta y permisos actuales; autenticado |
| POST `/auth/logout` | Revoca el token Bearer actual |
| GET `/admin/usuarios` | Cuentas y catálogo de permisos; USUARIOS |
| PUT `/admin/usuarios/{username}` | `{name,role,permissions,active,password,version}`; USUARIOS |

Para crear, `version:null` y contraseña de 12–72 caracteres. Para modificar, enviar la versión actual; contraseña vacía/nula la conserva. Los nombres de roles son ADMIN, OPERADOR, REPARTIDOR y VIEWER. Modificar una cuenta revoca sus tokens y no elimina su historial. Sesión: 30 minutos de inactividad. Los permisos se consultan en la base en cada petición, no solo al iniciar sesión.

## Contactos

| Método y ruta | Datos / permiso |
| --- | --- |
| GET `/contactos?q=` | Nombre o documento, hasta 100 coincidencias; CONTACTO_LEER |
| PUT `/contactos/{id}` | `{name,document,email,phone,version}`; CONTACTO_EDITAR. Usar ID 0 y versión nula para crear |
| GET `/contactos/{id}/envios` | Historial de remitente/destinatario; CONTACTO_LEER |

El directorio se asocia automáticamente al registrar un envío por el correo normalizado. Los envíos guardan sus propios datos históricos; la corrección del directorio no cambia fichas anteriores. Los endpoints nuevos de directorio/reclamaciones/operación usan claves SQL en snake_case; los DTO de envíos y usuarios conservan camelCase.

## Entregas y contactos de ficha

| Método y ruta | Datos / permiso |
| --- | --- |
| POST `/envios/{id}/contactos` | `{version,senderPhone,senderEmail,recipientPhone,recipientEmail}`; ENVIO_EDITAR |
| GET `/envios/{id}/operacion` | Intentos y metadatos de evidencia; ENVIO_LEER |
| POST `/envios/{id}/intentos` | `{version,reason}`; ENTREGA |
| POST `/envios/{id}/incidencia` | `{version,reason}` — solución aplicada; ENVIO_ESTADO |
| POST `/envios/{id}/entrega` | `{version,receiver,type,mime,image}`; ENTREGA |
| GET `/envios/{id}/evidencia` | Bytes privados PNG/JPEG, no-store; ENVIO_LEER |

Evidencia: `type:FOTO|FIRMA`, `mime:image/png|image/jpeg`, `image` es base64 sin prefijo data URL. Máximo 1.500.000 bytes decodificados y 6000 px por lado. Solo desde RUTA_ENTREGA. Intento fallido abre INCIDENCIA y programa el siguiente lunes–viernes. La solución devuelve al estado previo. El cambio genérico de estado ya no permite entregar sin evidencia ni resolver sin registrar la solución.

## Exportaciones y reportes

Todas requieren REPORTES. Se conservan la búsqueda, listado y demás endpoints del contrato anterior.

- GET `/envios/exportacion?q=`: todo el listado filtrado, CSV, no paginado.
- GET `/reportes?from=2026-11-01&to=2026-11-30&group=DIA`: total, entregados, porcentaje a tiempo, tránsito promedio y tamaño de muestra, retrasados, volumen, estados y reclamaciones del periodo.
- GET `/reportes/exportacion?from=...&to=...&group=MES&type=retrasados|volumen|resumen`.

Periodo: fecha de registro, inclusive, zona Colombia, hasta tres años. Sin fechas: últimos 30 días. No hay muestras se devuelve `null`, no un promedio artificial.

## Reclamaciones

| Método y ruta | Acceso / datos |
| --- | --- |
| POST `/reclamaciones/publicas` | Público: `{trackingCode,email,category,subject,description}` |
| POST `/reclamaciones/consulta` | Público: `{id,accessKey}` |
| POST `/reclamaciones/mensaje` | Público: `{id,accessKey,version,message}` |
| GET `/reclamaciones?status=&q=` | RECLAMOS, hasta 100 resultados |
| GET `/reclamaciones/responsables` | RECLAMOS, cuentas activas elegibles |
| GET `/reclamaciones/{id}` | RECLAMOS, ficha y análisis anteriores |
| PUT `/reclamaciones/{id}` | RECLAMOS: `{version,status,assignee,message}` |
| GET `/reclamaciones/{id}/contexto-ia` | RECLAMOS, contexto para revisión, disponibilidad y modelo |
| POST `/reclamaciones/{id}/analisis` | IA y RECLAMOS: `{caseText}`; respuesta estructurada |

Categorías: RETRASO, DANO, ENTREGA, ATENCION, OTRO. Estados: ABIERTA, EN_REVISION, RESUELTA, CERRADA. Radicado y clave solo se entregan en la creación. No se debe guardar la clave en URLs ni logs. Las solicitudes públicas tienen un límite local de diez en diez minutos por IP, con bloqueo de quince minutos.

Gemini se configura solo en el backend mediante GEMINI_API_KEY y GEMINI_MODEL. Modelo predeterminado `gemini-3.1-flash-lite`, configurable según disponibilidad y cuota de la cuenta. Sin clave o ante fallo devuelve 503 AI_UNAVAILABLE; no sustituye IA por reglas. El servidor valida formato y referencias a eventos. La respuesta es un borrador; guardar/publicar continúa siendo una petición explícita del usuario.

Documentación del proveedor consultada: [salidas estructuradas](https://aistudio.google.com/docs/structured-output), [modelos](https://aistudio.google.com/docs/models), [precios y acceso gratuito](https://aistudio.google.com/docs/pricing).
