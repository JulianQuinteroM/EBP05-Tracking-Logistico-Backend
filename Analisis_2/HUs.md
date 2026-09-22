Historias de Usuario Sprint 1

HU01
Como operador logístico, necesito ingresar los datos básicos del envío y seleccionar su prioridad (estándar, exprés, programado) para iniciar el proceso de trazabilidad, definiendo tiempos de entrega, costos y estados operativos asociados. 

Criterios de aceptación: 
1. El operador logístico autenticado debe tener un botón o enlace "Nuevo envío" visible en su panel principal

2. El formulario debe incluir los siguientes campos obligatorios:
• Datos del remitente (nombre, teléfono, correo)
• Datos del destinatario (nombre, teléfono, correo)
• Dirección de destino (calle, número, ciudad)
• Peso del paquete

3. El sistema debe validar que todos los campos obligatorios estén completos antes de permitir el envío del formulario.

4. Se debe incluir un Combobox con las opciones:
Estándar (predeterminado)
Exprés
Programado

5. Al guardar, el seleccionar una opción en el combobox, se debe mostrar el tiempo estimado:
Estándar: 3-5 días
Exprés: 24-48h
Programado: fecha seleccionable (se debe habilitar un selector de fecha).

 6. Al seleccionar una opción, se debe mostrar el costo estimado informativo:
Estándar: $10.000
Exprés: $25.000
Programado: $15.000

7. Tras guardar exitosamente, el sistema debe redirigir al operador a la pantalla de detalles del envío recién creado.

8. Si ocurre un error al guardar, el sistema debe mostrar un mensaje claro: "Error al crear el envío. Intente nuevamente."


HU02
Como operador logístico, necesito que el sistema asigne un identificador único a cada envío para rastrearlo durante toda la cadena de transporte 

Criterios de aceptación: 

1. El sistema debe mostrar un campo de búsqueda visible en la página principal y en la sección de envíos, identificado con la etiqueta "Ingresa tu código de seguimiento" o similar.

2. El sistema debe aceptar el código en cualquiera de sus formatos, con o sin guiones (LOG-38472915-5 o LOG384729155) y sin distinguir entre mayúsculas y minúsculas.

3. Al presionar Enter o hacer clic en el botón "Buscar", el sistema debe validar que el código tenga el formato correcto (3 letras + 8 dígitos + 1 dígito verificador) antes de realizar la consulta. Si el formato es inválido, debe mostrar un mensaje indicando "El código ingresado no es válido. Verifica el formato."

4. Si el código existe, el sistema debe redirigir a la pantalla de detalles completa del envío mostrando toda la información asociada: datos del remitente, datos del destinatario, estado actual, historial de movimientos y ubicación actual.

5. Si el código no existe, el sistema debe mostrar un mensaje claro indicando "No se encontró ningún envío con el código ingresado. Verifica que sea correcto."

6. La búsqueda debe estar disponible tanto para usuarios autenticados como para visitantes sin sesión, permitiendo que cualquier persona pueda rastrear un envío.

7. El campo de búsqueda debe estar disponible en el panel de seguimiento público y en el panel del operador logístico.



HU03
Como operador logístico, necesito visualizar todos los envíos registrados con filtros y opciones de búsqueda para tener control operativo del sistema y acceder rápidamente a la información de cada envío. 

Criterios de Aceptación 
1. El operador debe acceder al listado desde un menú principal y visualizar una tabla con los campos: código de seguimiento, remitente, destinatario, estado (con color e ícono), tipo de envío y fecha de registro.

2. El sistema debe permitir búsqueda por texto libre que filtre por código de seguimiento, nombre del remitente o nombre del destinatario.

3. El sistema debe ofrecer filtros combinables por estado del envío, tipo de envío y rango de fechas de registro.

4. El listado debe incluir paginación cuando existan más de 20 registros, con opciones para cambiar la cantidad de elementos por página.

5. Cada fila debe tener un botón "Ver detalles" que redirija a la pantalla completa del envío. Adicionalmente, los envíos en estado "Pendiente" o "Recogido" deben mostrar botones "Editar" y "Eliminar".

6. Al hacer clic en el encabezado de cualquier columna, el sistema debe ordenar los resultados de forma ascendente o descendente alternativamente.

7. El operador debe poder exportar el listado resultante a Excel o CSV.


HU04
Como operador logístico, necesito eliminar un envío del sistema para depurar registros incorrectos o cancelados. 

1. Debe existir un botón "Eliminar envío" disponible en la pantalla de detalle del envío.
2. El sistema debe solicitar confirmación mediante un modal antes de proceder: "¿Está seguro de que desea eliminar el envío?".
3. La eliminación debe realizarse mediante borrado lógico, cambiando el estado a "Cancelado".
4.Tras confirmar la eliminación, redirigir al operador a la lista general de envíos con el mensaje: "Envío eliminado correctamente".
5. Solo se permite eliminar envíos que se encuentren en estado "Pendiente de recogida".


HU05
Como operador logístico, necesito modificar los datos de un envío existente para corregir errores o actualizar información antes del despacho. 

Criterios de aceptación

1. El operador debe tener un botón "Editar" en la pantalla de detalles del envío.

2. Al hacer clic en "Editar", el sistema debe mostrar el formulario con todos los datos actuales del envío precargados.

3. El operador debe poder modificar: datos del remitente, datos del destinatario, dirección, peso, dimensiones, tipo de envío.

4. El código de seguimiento, ID interno, fecha de registro y hora de registro no deben ser editables.

5. El sistema debe validar que todos los campos obligatorios estén completos antes de guardar

6. Al guardar, el sistema debe persistir los cambios y mostrar mensaje de éxito: "Envío actualizado correctamente".

7. El sistema debe registrar en auditoría: qué usuario editó, qué campos modificó, fecha y hora de la edición.

8. Tras guardar, redirigir a la pantalla de detalles del envío con los datos actualizados.

9. Si ocurre un error al guardar, mostrar mensaje: "Error al actualizar el envío. Intente nuevamente."

