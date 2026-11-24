# Validación de sprint 3 local

Fecha: 23 de noviembre de 2026. Rama: `codex/sprint3-local`.

## Verificado

- **26 pruebas automatizadas del backend**, sin errores ni fallos: seguimiento y privacidad existentes, reglas de envíos, permisos personalizados, revocación, directorio e historial, correcciones de contacto, intentos fallidos, resolución, evidencia privada, reclamaciones, CSV y cálculos de reportes.
- Cliente Gemini probado con transporte simulado: solicitud estructurada y cabecera de clave, lectura de respuesta válida, cuota agotada y generación truncada. Se comprueba el rechazo de citas a eventos no suministrados y el error explícito cuando falta clave.
- **TypeScript y ESLint** del frontend sin errores. **Build de producción Next.js con webpack correcto**, incluyendo las nuevas páginas y rutas API. Para evitar archivos generados bloqueados por el entorno Windows/OneDrive se usó `NEXT_DIST_DIR=.next-sprint3`, que está ignorado por Git.
- **JAR Spring Boot generado correctamente**. Las pruebas usan el agente Mockito explícito para evitar problemas de autoanexión de Java 25.
- **Recorrido real por Next y PostgreSQL 18 temporal:** ingreso y cookie, registro de envío, consulta de contactos, exportación CSV, secuencia de estados, ubicación, intento fallido, corrección de contacto, solución de incidencia, entrega con imagen válida, lectura privada de evidencia, reclamación del cliente, consulta por radicado/clave, gestión y respuesta, contexto de IA, reportes, auditoría y seguimiento público.
- **Revisión visual en navegador:** menú según permisos, reportes con datos reales, bandeja de reclamaciones y pantalla de preparación del contexto con aviso de clave faltante. Se mantuvo el estilo oscuro y azul.

## Pendiente para cerrar la validación funcional de IA

Configurar la clave privada del equipo en el backend local, realizar una llamada real a Gemini y revisar la utilidad/fidelidad de sus resultados. Las pruebas simuladas verifican la integración y sus errores, pero **no prueban el acceso del proyecto a un modelo real, su cuota ni la calidad de sus respuestas**.

La app permite continuar con gestión manual cuando Gemini no está disponible. No hay respuestas simuladas presentadas como IA en el producto.

## Entorno y datos

Las comprobaciones de PostgreSQL utilizaron una instancia temporal distinta de `tracking_logistico`, en el puerto 55439. Los servidores de revisión usaron 18080 y 3300. Los scripts normales conservan los puertos 8080 y 3000 y PostgreSQL local 5432. No se utilizó la conexión de Neon ni se enviaron cambios a GitHub, Render o Vercel.

V3 agrega las tablas del sprint 3. V1/V2 se conservaron sin modificaciones. Las cuentas se guardan en la base y no se sobrescriben desde variables de entorno al reiniciar.
