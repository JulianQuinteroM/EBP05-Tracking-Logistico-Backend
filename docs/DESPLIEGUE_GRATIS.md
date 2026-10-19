# Despliegue de demostración: Vercel + Render + Neon

Este procedimiento mantiene Next.js, Spring Boot 3 y PostgreSQL. Está pensado para una demostración universitaria con **datos ficticios**, no para alojar datos personales reales ni para prometer disponibilidad continua. No pegues contraseñas ni cadenas de conexión en GitHub, capturas de pantalla o chats.

## 1. Publicar el código en GitHub

El repositorio tiene el backend en la raíz y el frontend en `frontend/`. Desde la raíz del proyecto, revisa los archivos que vas a subir:

```powershell
git status --short
git add .dockerignore .env.example .gitignore Dockerfile README.md docs frontend pom.xml scripts src
git diff --cached --stat
git diff --cached --name-only
```

Confirma que **no** aparecen `.env`, `.env.local`, `node_modules`, `.next`, `target`, `.m2` ni contraseñas. Si todo está bien:

```powershell
git commit -m "Preparar despliegue de LogiFlow"
git push origin main
```

No uses `--force` si Git rechaza el push: revisa primero qué hay en el repositorio remoto.

## 2. Crear PostgreSQL en Neon

1. Crea un proyecto en [Neon](https://console.neon.tech/) en una región próxima a la que usarás en Render. La base puede conservar el nombre predeterminado de Neon; no tiene que llamarse `tracking_logistico`.
2. En **Connect**, selecciona la conexión **directa** (no el host terminado en `-pooler`) y guarda de forma privada el host, nombre de base, usuario y contraseña. No los compartas en el chat.
3. Neon muestra habitualmente una URL como `postgresql://USUARIO:CLAVE@HOST/BASE?sslmode=require`. Spring necesita los componentes separados:

   - `DB_URL`: `jdbc:postgresql://HOST/BASE?sslmode=require`
   - `DB_USERNAME`: `USUARIO`
   - `DB_PASSWORD`: `CLAVE`

La base en Neon empieza vacía. Al iniciar Spring Boot, Flyway crea el esquema con V1 y V2. V2 elimina datos del sprint 1, por lo que **solo debe ejecutarse sobre esta base nueva o sobre una base cuyos datos se puedan perder**. Los registros de PostgreSQL local no se transfieren automáticamente.

## 3. Publicar Spring Boot en Render

1. En [Render](https://dashboard.render.com/), crea **New → Web Service** conectado al repositorio de GitHub y a la rama `main`.
2. Selecciona el entorno **Docker**, el plan **Free** y deja la raíz del servicio en la raíz del repositorio (ahí están `Dockerfile` y `pom.xml`). El Dockerfile compila con Java 21; el Java 25 de tu PC no afecta el servidor.
3. Configura estas variables en **Environment**. Elige contraseñas nuevas y distintas de al menos 12 caracteres; conserva una copia privada para poner los mismos valores en Vercel:

   | Variable | Valor |
   | --- | --- |
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Los tres componentes obtenidos de Neon |
   | `OPERATOR_USERNAME` | `operador` (o el nombre que elijas) |
   | `OPERATOR_PASSWORD` | Contraseña larga del operador |
   | `ADMIN_USERNAME` | `administrador` |
   | `ADMIN_PASSWORD` | Contraseña larga del administrador |
   | `VIEWER_USERNAME` | `consulta` |
   | `VIEWER_PASSWORD` | Contraseña larga de consulta |
   | `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `3` |
   | `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | `0` |
   | `SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT` | `30000` |

   Render proporciona `PORT`; `application.yml` lo usa automáticamente. `FRONTEND_ORIGIN` se configurará cuando conozcas el dominio de Vercel.
4. Como ruta de verificación de salud, usa `/actuator/health`. Cuando termine el despliegue, visita `https://NOMBRE.onrender.com/actuator/health`: debe responder `{"status":"UP"}`. La primera apertura después de un periodo inactivo puede tardar aproximadamente un minuto. Si el servicio falla al arrancar, revisa **Logs**; el plan Free tiene solo 512 MB y quizá no alcance para este backend Java.

## 4. Publicar Next.js en Vercel

1. En [Vercel](https://vercel.com/new), importa **el mismo repositorio** de GitHub. Elige el plan gratuito Hobby para el proyecto universitario no comercial.
2. En la importación, configura **Root Directory** como `frontend` y comprueba que detecte **Next.js**. No elijas una exportación estática: la app usa rutas API y páginas renderizadas en servidor.
3. Antes de pulsar **Deploy**, crea estas variables para el entorno **Production**. Las contraseñas y `SESSION_SECRET` deben guardarse como secretos; ninguna lleva el prefijo `NEXT_PUBLIC_`:

   | Variable | Valor |
   | --- | --- |
   | `BACKEND_URL` | `https://NOMBRE.onrender.com` (sin `/api` al final) |
   | `OPERATOR_USERNAME`, `OPERATOR_PASSWORD` | Exactamente los mismos valores configurados en Render |
   | `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Exactamente los mismos valores configurados en Render |
   | `VIEWER_USERNAME`, `VIEWER_PASSWORD` | Exactamente los mismos valores configurados en Render |
   | `SESSION_SECRET` | Cadena aleatoria de al menos 32 caracteres, diferente de las contraseñas |

   Puedes generar `SESSION_SECRET` en PowerShell con `([Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48)))`. Copia el resultado directamente a Vercel, sin publicarlo.
4. Despliega y guarda la URL principal `https://NOMBRE.vercel.app`. En Render, añade `FRONTEND_ORIGIN=https://NOMBRE.vercel.app` (sin barra final). Si cambias variables de Vercel después de desplegar, vuelve a desplegar para que el nuevo despliegue las reciba.

## 5. Probar el sistema completo

1. Abre `/actuator/health` en Render y espera `UP`.
2. Abre la URL de Vercel. Ingresa al panel como operador y crea un envío **ficticio**.
3. Busca el código en la página inicial; abre los detalles, cambia el estado a Recogido y registra una ciudad. Revisa que la búsqueda pública refleje esos cambios.
4. Ingresa como administrador y verifica la auditoría.
5. Espera 15–20 minutos sin usar la app y repite una consulta. Render Free se suspende por inactividad; el frontend espera hasta 90 segundos en producción antes de informar que el backend no está disponible. Si el arranque tarda más, abre primero la ruta de salud y luego vuelve a intentar.

## Límites y seguridad

- Render Free puede suspenderse y dispone de 512 MB de RAM. Java podría superar ese límite incluso con el heap reducido; si ocurre, no hay que borrar datos ni cambiar la base: habrá que elegir un servidor con más memoria.
- Neon Free tiene límites de almacenamiento/cómputo. Su historial de restauración no sustituye copias de seguridad periódicas si los datos importan.
- El backend usa cuentas provisionales configuradas por variables de entorno, no un sistema definitivo de usuarios. Usa solo datos de demostración y no compartas las credenciales.
- El dominio `vercel.app` y el dominio `onrender.com` funcionan con HTTPS; no necesitas comprar un dominio para la entrega.

Documentación de los proveedores: [Vercel monorepos](https://vercel.com/docs/monorepos), [variables de entorno de Vercel](https://vercel.com/docs/environment-variables), [Render Docker](https://render.com/docs/docker), [Render Free](https://render.com/docs/free), [Neon](https://neon.com/docs/get-started-with-neon/connect-neon).
