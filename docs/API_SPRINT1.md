# Contrato API - Sprint 1

Base local: `http://localhost:8080/api`. Respuestas JSON UTF-8. Los errores usan `{ "code": "...", "message": "...", "fields": {} }`. Las rutas privadas requieren autenticación de operador. El cliente puede usar el endpoint público de rastreo sin credenciales.

| HU | Método y ruta | Acceso | Resultado |
| --- | --- | --- | --- |
| HU001 | `POST /estimaciones` | Operador | costo informativo y fecha de entrega estimada |
| HU001 | `POST /envios` | Operador | HTTP 201, ficha y código nuevo; cabecera `Location` |
| HU002 | `GET /seguimiento/{codigo}` | Público | resumen mínimo, nunca ficha privada |
| HU003 | `GET /envios` | Operador | lista paginada/ordenada con búsqueda textual |
| HU003 | `GET /envios/{id}` | Operador | ficha privada e historial logístico |
| HU005 | `PUT /envios/{id}` | Operador | ficha actualizada o HTTP 409 |
| HU004 | `POST /envios/{id}/cancelacion` | Operador | ficha en CANCELADO o HTTP 409 |

## Crear envío

```json
{
  "senderName": "Ana Pérez",
  "senderPhone": "+57 300 1234567",
  "senderEmail": "ana@example.com",
  "recipientName": "Luis Gómez",
  "recipientPhone": "+57 310 7654321",
  "recipientEmail": "luis@example.com",
  "destinationStreet": "Calle 10",
  "destinationNumber": "20",
  "destinationCity": "Medellín",
  "weightKg": 2.500,
  "serviceType": "ESTANDAR",
  "scheduledDeliveryDate": null
}
```

Campos de texto obligatorios sin espacios vacíos; teléfono de 7-30 caracteres con cifras, espacios, `+`, paréntesis o guion; correo válido; peso decimal entre `0.001` y `9999999.999` kg. `serviceType` acepta `ESTANDAR`, `EXPRES`, `PROGRAMADO`. Solo `PROGRAMADO` admite y exige `scheduledDeliveryDate` futura (`YYYY-MM-DD`). No existe campo de origen. No enviar dimensiones en Sprint 1. Una propiedad desconocida como `status`, `trackingCode` o `originStreet` se rechaza con HTTP 400.

La ficha devuelta contiene `id`, `trackingCode`, `status`, `version`, `shipment` (los datos capturados), `estimatedCostCop`, `estimatedDeliveryDate`, `createdAt`, `updatedAt` y `events`. Se necesita `id` y `version` para editar/cancelar. `estimatedDeliveryDate` es el límite superior de la estimación provisional: cinco o dos días desde la creación para Estándar/Exprés, o la fecha elegida para Programado. No equivale a fecha real de entrega.

Para previsualizar costo/fecha antes de crear o cambiar de servicio: `POST /estimaciones` con `{ "serviceType": "EXPRES", "scheduledDeliveryDate": null }`. Devuelve `{ "costCop": 25000.00, "deliveryDate": "YYYY-MM-DD", "deliveryWindow": "24-48 horas" }`. La ventana es informativa y la fecha almacenada es su límite aproximado en días calendario, no una promesa exacta de 48 horas.

## Seguimiento público

`GET /seguimiento/LOG-29047381-8` o `GET /seguimiento/log290473818`. Se valida Luhn antes de la búsqueda. El ejemplo corresponde a la serie `29047381`; no es un envío pregrabado. Una respuesta encontrada tiene esta forma:

```json
{
  "trackingCode": "LOG-29047381-8",
  "status": "PENDIENTE_RECOGIDA",
  "serviceType": "ESTANDAR",
  "updatedAt": "2026-09-15T14:00:00Z",
  "estimatedDeliveryDate": "2026-09-20",
  "latestPublicEvent": {
    "type": "REGISTRO",
    "resultingStatus": "PENDIENTE_RECOGIDA",
    "occurredAt": "2026-09-15T14:00:00Z",
    "description": "Envío registrado; pendiente de recogida"
  }
}
```

El endpoint no amplía la respuesta si lo llama un operador. Formato/dígito inválido: HTTP 400 `INVALID_TRACKING_CODE`. Código válido inexistente: HTTP 404 `SHIPMENT_NOT_FOUND`. No muestra remitente, destinatario, dirección ni origen.

## Listado, ficha, edición y cancelación

`GET /envios?page=0&size=20&q=Ana&sortBy=createdAt&direction=desc`. `size` solo 20 o 50. `sortBy` acepta `createdAt`, `trackingCode`, `status`, `serviceType`; `direction` acepta `asc`/`desc`. La búsqueda considera código o nombre de remitente/destinatario. La respuesta estable es `{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }`. Orden por fecha descendente e ID descendente como desempate por defecto. Los cancelados permanecen en la lista. Filtros por estado/fecha y exportación no están en esta HU.

`GET /envios/{id}` devuelve la ficha privada, con contactos y dirección de destino. El historial `events` conserva el registro y, si ocurrió, la cancelación. No expone auditoría en el contrato de Sprint 1. Si no se registraron dimensiones verificadas, el front no debe inventarlas.

Para editar: `PUT /envios/{id}` con `{ "version": 0, "shipment": { ...todos los campos de creación... } }`. Es reemplazo completo de los campos editables, no patch parcial. Código, estado, ID y fecha de creación no se aceptan en el body. Sin cambios efectivos, no se crea auditoría ficticia. La estimación provisional solo se recalcula cuando cambia servicio o fecha programada.

Para cancelar: `POST /envios/{id}/cancelacion` con `{ "version": 0, "reason": "Registro duplicado" }`. El motivo es obligatorio y privado. Es cancelación lógica; no hay `DELETE` de envíos.

En ambos casos, solo se permite `PENDIENTE_RECOGIDA`; enviar versión vieja o intentar operar un envío cancelado devuelve HTTP 409 `SHIPMENT_CONFLICT`. El front debe recargar la ficha y no afirmar éxito al recibir 400/409/500.

## Integración y límites

El backend permite `FRONTEND_ORIGIN` exacto para CORS. Las rutas privadas usan HTTP Basic en esta versión local. El front requiere un mecanismo de sesión/proxy que no exponga credenciales del operador en el navegador. Las pruebas automáticas de H2 verifican el contrato y persistencia, pero antes de la demostración conjunta hay que ensayar con PostgreSQL real y el front integrado.
