# Contrato API — sprint 2

Base local del backend: `http://localhost:8080/api`. El frontend usa las mismas rutas bajo `http://localhost:3000/api` como proxy. Errores JSON: `{ "code": "...", "message": "...", "fields": {} }`. El backend usa HTTP Basic; el front mantiene una cookie firmada y agrega Basic **solo en el servidor**. Nunca envíes contraseñas en `NEXT_PUBLIC_*`.

| Operación | Ruta | Acceso |
| --- | --- | --- |
| Estimar costo/fecha | `POST /estimaciones` | operador/admin |
| Crear envío | `POST /envios` | operador/admin |
| Listar y ver ficha privada | `GET /envios`, `GET /envios/{id}` | operador/admin/consulta |
| Editar ficha | `PUT /envios/{id}` | operador/admin |
| Cancelar pendiente | `POST /envios/{id}/cancelacion` | operador/admin |
| Cambiar estado | `POST /envios/{id}/estado` | operador/admin |
| Sugerir ciudades | `GET /ciudades?q=med` | operador/admin/consulta |
| Registrar ubicación | `POST /envios/{id}/ubicaciones` | operador/admin |
| Rastrear por código | `GET /seguimiento/{codigo}` | público |
| Consultar auditoría | `GET /admin/auditoria` | admin |
| Exportar auditoría | `GET /admin/auditoria/exportacion` | admin, CSV |

`DELETE /envios/{id}` responde HTTP 409: la eliminación física quedó desactivada para conservar el historial y la auditoría. El cancelado lógico conserva el registro.

## Crear o editar envío

```json
{
  "senderName": "Ana Gómez", "senderPhone": "+57 300 1234567", "senderEmail": "ana@example.com",
  "recipientName": "Luis Ruiz", "recipientPhone": "+57 310 7654321", "recipientEmail": "luis@example.com",
  "destinationStreet": "Calle 10", "destinationNumber": "20", "destinationCity": "Medellín",
  "weightKg": 2.50, "lengthCm": 30, "widthCm": 20, "heightCm": 15,
  "packagingType": "CAJA", "packagingDescription": null,
  "serviceType": "ESTANDAR", "scheduledDeliveryDate": null
}
```

Peso: 0,01–50 kg. Largo, ancho y alto: 0,01–200 cm cada uno. Todos admiten máximo dos decimales; el formulario acepta coma/punto y convierte a número JSON con punto. `packagingType`: `SOBRE`, `CAJA`, `BOLSA`, `TUBO`, `ESTIBA`, `OTRO`; `OTRO` exige descripción de 3 a 100 caracteres. `serviceType`: `ESTANDAR`, `EXPRES`, `PROGRAMADO`. Programado requiere fecha futura de **entrega**. La respuesta de creación incluye `id`, `trackingCode`, `version`, `status`, `shipment`, estimación y eventos.

Para editar, `PUT /envios/{id}` recibe `{ "version": 0, "shipment": { ...todos los campos anteriores... } }`. Pendiente permite editar la ficha; Recogido solo peso, dimensiones y empaque; después no hay edición. La versión evita sobrescribir una ficha modificada en paralelo (HTTP 409).

## Estado y ubicación

`POST /envios/{id}/estado`: `{ "version": 1, "status": "RECOGIDO", "reason": null }`. La secuencia normal es Pendiente → Recogido → En tránsito → Centro de distribución → Ruta de entrega → Entregado. Desde un paso no final se permite Incidencia con `reason` privado de 10–250 caracteres; desde Incidencia solo se permite regresar al estado previo. Los cambios quedan en eventos y auditoría. Un salto no permitido devuelve 409 y registra alerta `TRANSICION_INUSUAL` para administrador. Entregado es final.

`POST /envios/{id}/ubicaciones`: `{ "version": 2, "city": "Medellín", "reference": "Centro principal" }`. Ciudad obligatoria y validada contra el catálogo de 32 capitales colombianas; `reference` opcional, máximo 100 caracteres. El servidor asigna fecha/hora. No se permite ubicación antes de Recogido ni después de Entregado/Cancelado. La ficha muestra ciudad actual y eventos del más reciente al más antiguo. `GET /ciudades?q=med` devuelve sugerencias tras al menos tres caracteres.

## Seguimiento público y seguridad

`GET /seguimiento/{codigo}` devuelve código, estado, tipo de servicio, actualización, ventana de entrega (`estimatedDeliveryStart`, `estimatedDeliveryDate`), ciudad actual, nombres reducidos (`maskedSenderName`, `maskedRecipientName`), `journey` con fases `COMPLETADO`/`ACTUAL`/`PENDIENTE` y último evento público. No contiene teléfonos, correos, dirección completa, referencia de ubicación, motivos privados ni auditoría. Código inexistente válido: 404; formato/dígito incorrecto: 400. Diez códigos inexistentes consultados en diez minutos desde una misma IP bloquean quince minutos (429).

La ventana estándar es de 3 a 5 días calendario; exprés, 1 a 2 días calendario; programado, la fecha elegida. Son estimaciones provisionales, no una promesa comercial. En Incidencia se conserva visible el paso interrumpido y se muestra una alerta; Entregado se anuncia como completado.

Las cuentas actuales son provisionales y se configuran al arrancar: `operador` y `administrador` pueden escribir; `consulta` solo lee. La sesión del frontend se renueva con actividad de API y caduca a los 30 minutos sin ella. Los intentos autenticados sin permiso se registran como `INTENTO_NO_AUTORIZADO`.

## Auditoría

`GET /admin/auditoria?shipmentId=1&action=ESTADO&actor=operador&from=2026-10-01&to=2026-10-31` filtra por ID, tipo, usuario y fechas. Cada registro incluye campo, valor anterior/nuevo, usuario, IP, fecha/hora y detalle. La consulta y la exportación también se auditan. `GET /admin/auditoria/exportacion` admite los mismos filtros y devuelve CSV UTF-8 con BOM y protección de fórmulas para abrir en Excel o Google Sheets. No hay purga automática: los registros deben conservarse al menos dos años. El panel de administrador muestra alertas de transiciones inválidas e intentos no autorizados; no envía correo. Al consultar o exportar, el servidor revisa también la secuencia histórica y registra una sola alerta persistente por cada evento inconsistente detectado.

El backend sigue usando Spring Boot/PostgreSQL de los sprints anteriores. La migración V2 descarta **una sola vez** los datos de prueba del sprint 1, autorizados por el equipo, para exigir los campos físicos. No ejecutar V2 sobre datos que deban conservarse.
