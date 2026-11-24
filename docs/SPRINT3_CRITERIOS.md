# Sprint 3 — alcance y criterios de aceptación

El CSV del equipo contiene 15 historias con descripción y sin criterios. Los siguientes criterios concretan esas descripciones. La separación de contactos y usuarios, el módulo de reclamaciones y el asistente Gemini se acordaron con el equipo. Se conserva el backend Spring Boot y el frontend Next.js.

## Decisiones de dominio

- **Contacto:** remitente o destinatario del directorio, identificado por correo único y documento único cuando se completa. Los contactos anteriores se recuperan desde las fichas existentes; su documento aparece como pendiente de completar. Registrar un contacto no crea una cuenta.
- **Usuario:** cuenta persistente con contraseña BCrypt, rol, permisos y acceso activo/revocado. Roles iniciales: administrador, operador, repartidor y consulta. El administrador puede crear cuentas y ajustar sus permisos.
- La ficha de cada envío conserva los datos del momento de su registro. Editar el directorio no cambia otros envíos. Corregir los teléfonos o correos de una ficha queda auditado.
- Los usuarios iniciales se crean **una sola vez** desde las variables del backend. Reiniciar no reemplaza contraseñas, permisos o revocaciones. Las cuentas posteriores se crean desde el panel.
- Hora de negocio: America/Bogota. Los reportes incluyen envíos registrados entre las fechas seleccionadas, ambas inclusivas. Sin fechas: últimos 30 días; máximo tres años por consulta.

## Historias del CSV

| ID Azure / HU | Criterios de aceptación |
| --- | --- |
| 89 / HU009 Exportar listado | Una cuenta con permiso de reportes puede descargar todos los resultados de la búsqueda por código/nombre, no solo la página visible. CSV UTF-8 con BOM, encabezados y escape de celdas para Excel/Sheets. Una búsqueda vacía produce un CSV con encabezados. Se audita la exportación. |
| 92 / HU012 Editar contacto de envío | Una cuenta con ENVIO_EDITAR puede corregir teléfonos/correos desde la ficha mientras el envío no esté entregado/cancelado. Se valida formato y versión. Se auditan los valores anteriores y nuevos. No cambian el código, el estado ni otras fichas. |
| 100 / HU026 Firma o evidencia | Desde Ruta de entrega y con permiso ENTREGA se registra receptor e imagen de foto o firma PNG/JPEG de hasta 1,5 MB y 6000 píxeles por lado. Se valida el contenido real. Evidencia y cambio a Entregado se guardan en una transacción. Se registra usuario y hora. La imagen es privada y no aparece en seguimiento público. No se permite marcar Entregado usando el cambio genérico sin evidencia. |
| 101 / HU027 Intento fallido | Desde Ruta de entrega y con ENTREGA se registra motivo de 10–500 caracteres. Se incrementa el número del intento, se abre incidencia y se programa el siguiente intento para el próximo lunes–viernes; no se consulta un calendario de festivos. La ficha muestra motivo, fecha prevista, usuario y hora. La fecha es planificación operativa, no ejecución automática de un reparto. |
| 104 / HU030 Resolver incidencia | En Incidencia, una cuenta con ENVIO_ESTADO registra la solución de 10–500 caracteres. El envío retoma el estado previo; queda evento público sin el detalle privado y auditoría de la solución. No se puede cerrar una incidencia inexistente. |
| 109 / HU035 Auditoría por usuario | Con AUDITORIA se filtra el historial por usuario, envío, acción y fechas. La consulta y exportación se auditan; los registros no pueden editarse o borrarse desde la API. El filtro de usuario permite coincidencia parcial para localizar cuentas. |
| 110 / HU013 Consultar actores | Con CONTACTO_LEER se consulta el directorio de contactos y sus datos, sin abrir primero un envío. Se muestran hasta 100 coincidencias y se permite refinar la búsqueda. Con CONTACTO_EDITAR se crean y completan contactos. |
| 111 / HU014 Buscar actor | Se puede buscar por nombre o documento, sin distinguir mayúsculas. Una búsqueda sin coincidencias muestra un estado vacío. El documento y correo no pueden duplicarse entre contactos. |
| 112 / HU015 Historial de actor | Desde un contacto se consultan los envíos asociados como remitente o destinatario, su código, fecha, estado y enlace a la ficha. Se conservan las asociaciones históricas aunque se corrija el contacto de una ficha. |
| 113 / HU016 Permisos según rol | Al crear una cuenta, el rol propone permisos iniciales; el administrador puede ajustar la selección. El backend exige el permiso de cada operación y el menú refleja los permisos. Contactos sin cuenta no tienen acceso. |
| 114 / HU017 Modificar permisos | El administrador modifica rol/permisos de una cuenta existente, con control de versión. Se audita el cambio y se invalidan sus sesiones anteriores. El permiso IA requiere RECLAMOS. ADMIN conserva todos los permisos. |
| 115 / HU018 Revocar acceso | Al desactivar una cuenta, se invalida su acceso, incluso con una sesión previa. Se conserva usuario e historial. Se impide revocar el propio acceso administrativo y eliminar el último administrador activo. La cuenta puede reactivarse. |
| 116 / HU036 Retrasados | Reportes lista los envíos no entregados ni cancelados cuya fecha estimada original es anterior al día actual en Colombia, dentro del periodo de registros seleccionado. Incluye días de retraso, orden descendente, enlace y CSV. |
| 117 / HU037 Tránsito promedio | Se calcula desde el primer evento de recogida al primer evento de entrega de los envíos entregados con ambos eventos válidos. Se muestran horas y cantidad de muestras; sin muestras se indica que no hay entregas medibles, sin fabricar un cero. |
| 118 / HU038 Volumen por periodo | Se cuenta cada envío por fecha de registro, con agrupación diaria, semanal —lunes— o mensual. Se incluyen cancelados, porque fueron registrados. Se muestran totales y distribución por estado, tabla/gráfico accesible y CSV. |

## Valor agregado: atención de reclamaciones

1. Con código válido y correo del remitente/destinatario, el cliente puede registrar categoría, asunto y descripción; máximo tres casos abiertos por envío.
2. Recibe radicado y clave aleatoria que se muestra una sola vez y puede descargar. El servidor guarda solo el hash de la clave. La consulta no transmite claves en la URL.
3. Con radicado y clave consulta estado y conversación, y agrega información mientras el caso no esté cerrado. Un mensaje a un caso resuelto lo devuelve a revisión.
4. Con RECLAMOS se consulta la bandeja, se filtra por estado/radicado/guía, se asigna responsable activo y se responde. Estados: Abierta, En revisión, Resuelta y Cerrada. Resolver exige describir la solución; cerrar exige resolución previa.
5. Las respuestas se publican dentro de la app; no se envía correo. La consulta pública no muestra usuarios internos ni el correo del reclamante. Hay limitación de solicitudes públicas para reducir abuso.

## Valor agregado: asistente Gemini

1. Solo cuentas con IA y RECLAMOS generan análisis. Antes se muestra el contexto reducido y editable: texto del caso, estado, estimación y hasta 60 eventos recientes con identificadores.
2. Se excluyen los campos de contacto, dirección, referencia y motivos privados. Se sustituyen datos conocidos y patrones comunes en el texto, pero el operador debe revisarlo: la sustitución automática no garantiza anonimizar un texto libre.
3. El operador confirma que revisó el contexto antes de enviarlo a Google. No se envían imágenes de evidencia.
4. La respuesta estructurada contiene resumen, prioridad sugerida, hallazgos con identificadores de eventos, acciones, información faltante y borrador.
5. El servidor rechaza formatos inválidos y referencias a eventos no suministrados. Esta validación no demuestra que una inferencia sea correcta; la revisión humana sigue siendo necesaria.
6. El borrador solo se publica cuando el operador lo revisa y guarda una respuesta. La IA no cambia estados, permisos o entregas.
7. Sin clave, sin conexión o con cuota agotada, se muestra un error y continúa disponible la gestión manual. No se simulan respuestas como si fueran IA. Se limita a un análisis simultáneo y 20 guardados diarios por usuario.
8. Cada análisis válido se guarda con modelo, usuario y hora y genera auditoría. El texto enviado y la clave del proveedor no se guardan en auditoría.

## Verificación y límites de la entrega local

- Pruebas automatizadas: regresiones de sprints anteriores y flujos nuevos en H2 compatible con PostgreSQL.
- Prueba de integración adicional con PostgreSQL 18 temporal y proxy Next: ingreso/cookie, registro, directorio, CSV, estados, ubicación, intento fallido, resolución, evidencia, reclamación/respuesta, reportes y auditoría.
- Queda pendiente la llamada real a Gemini con la clave del equipo y la evaluación humana de sus respuestas. Las pruebas sin clave verifican indisponibilidad explícita, reducción de contexto y rechazo de citas inexistentes.
- No se publica esta rama ni se modifica Neon. La migración V3 es aditiva y no borra envíos. No modificar ni volver a ejecutar manualmente V1/V2.
