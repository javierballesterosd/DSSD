# RescueSync

Trabajo Práctico Integrador de Desarrollo de Software en Sistemas Distribuidos (DSSD), curso 2026.

RescueSync es una plataforma para coordinar la respuesta ante desastres naturales a nivel regional. Un municipio registra una emergencia, el Centro Coordinador Regional la desglosa en un lote de necesidades y abre una convocatoria, y las ONGs cargan ofertas de recursos (individuales o en consorcio) dentro de una ventana de tiempo.

Este repositorio contiene la **aplicación web local** (`frontend/` + `backend/`) y el modelo del proceso en Bonita (`Modelado/`).

## Arquitectura

| Componente | Rol | Dónde está |
|---|---|---|
| **Bonita BPM** | Orquesta el proceso, administra la ventana de ofertas con timers y es la fuente de usuarios y roles. | `Modelado/` |
| **Backend** | API REST (Spring Boot 4, Java 21, Maven). Reglas de negocio, ofertas y su versionado, cliente de Bonita. | `Proyecto/backend/` |
| **Frontend** | SPA (Angular 22, Bootstrap 5) con una sección por rol. | `Proyecto/frontend/` |
| **Base de datos** | PostgreSQL 15 en Docker, con pgAdmin. | `Proyecto/docker-compose.yml`, `Proyecto/db/seed/` |
| **Sistema Nacional** | API externa con JWT que valida y compromete recursos. | Proyecto aparte |

El front solo habla con el backend (`/api/...`). El backend escribe en PostgreSQL y, como último paso de cada operación, avanza el caso en Bonita; si Bonita falla, se revierte lo escrito. El login también pasa por Bonita: la app no tiene tabla de usuarios.

Roles: Operador Municipal (`MUNICIPAL`), Coordinador Regional (`COORDINADOR`), Representante de ONG (`ONG`) y Auditor (`AUDITOR`). Cada usuario tiene uno solo.

Más detalle:
- [docs/arquitectura.md](docs/arquitectura.md): estructura de paquetes y carpetas, manejo de errores, rutas, guía de diseño de pantallas.
- [docs/funcionalidades.md](docs/funcionalidades.md): qué puede hacer cada rol, endpoints y reglas de negocio.
- [docs/bonita.md](docs/bonita.md): organización, vínculo con la base e instanciación del proceso.

## Requisitos

- Docker y Docker Compose
- Java 21
- Node.js con npm
- Bonita Studio 7.9.0 o superior

## Instalación

Todos los comandos se corren desde la carpeta `Proyecto/`, salvo que se indique otra.

1. **Crear el `.env`** en `Proyecto/` con las variables de [Configuración](#configuración). No está versionado.
2. **Preparar Bonita** siguiendo [Configuración del login con Bonita](#configuración-del-login-con-bonita). Bonita Studio tiene que quedar abierto.
3. **Levantar la base**:

   ```bash
   docker compose up -d postgres
   ```

4. **Levantar el backend** (desde `backend/`). La primera vez crea las tablas:

   ```bash
   ./mvnw spring-boot:run
   ```

5. **Cargar los datos de ejemplo** (solo la primera vez; ver [Datos de ejemplo](#datos-de-ejemplo)):

   ```bash
   docker exec -i postgres_db psql -U postgres -d rescuesync < db/seed/01-reset-y-seed.sql
   ```

6. **Levantar el frontend** (desde `frontend/`), en http://localhost:4200:

   ```bash
   npm install
   npm start
   ```

7. Entrar con alguno de los [usuarios de ejemplo](#usuarios-de-ejemplo).

### Todo en Docker

Como alternativa a los pasos 3 a 6, se puede levantar el stack completo. Bonita Studio igual tiene que estar corriendo en la máquina.

```bash
docker compose up --build
```

El frontend queda en http://localhost y el seed se carga solo si la base está vacía. Después de cambiar código hay que reconstruir la imagen del servicio afectado (`docker compose up -d --build backend`); si solo cambia el `.env` o el `docker-compose.yml`, alcanza con `docker compose up -d`.

### Tests y build

| | Backend (desde `backend/`) | Frontend (desde `frontend/`) |
|---|---|---|
| Tests | `./mvnw test` | `npm test` |
| Build | `./mvnw clean package` | `npm run build` |

## Configuración

### Variables de entorno

El `.env` de `Proyecto/` lo leen Docker Compose y el backend cuando corre en local.

| Variable | Uso | Valor de ejemplo |
|---|---|---|
| `DB_HOST` | Host de PostgreSQL para el backend local (en Docker se fuerza `postgres`) | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Base y credenciales | `rescuesync` / `postgres` / (completar) |
| `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` | Login de pgAdmin | (completar) |
| `SERVER_PORT` | Puerto del backend en local | `8081` |
| `BACKEND_PORT` | Puerto del host donde Docker publica el backend (por defecto `8081`) | `8081` |
| `BONITA_BASE_URL` | URL de Bonita para el backend local (opcional) | `http://localhost:8080/bonita` |
| `BONITA_DOCKER_URL` | URL de Bonita para el backend en Docker (opcional) | `http://host.docker.internal:8080/bonita` |

> **El backend va en el 8081.** Bonita Studio ocupa el 8080, que es el valor por defecto de `SERVER_PORT`, así que hay que definirlo en `8081`. El frontend en desarrollo apunta a `http://localhost:8081/api` (`frontend/src/environments/environment.development.ts`); si usás otro puerto, cambialo ahí también.

### Puertos

| Servicio | Local | Docker Compose |
|---|---|---|
| Backend | http://localhost:8081 (`SERVER_PORT`) | http://localhost:8081 (`BACKEND_PORT`) |
| Frontend | http://localhost:4200 | http://localhost |
| PostgreSQL | `localhost:5432` | `localhost:5432` |
| pgAdmin | n/a | http://localhost:5050 |
| Bonita Studio | http://localhost:8080/bonita | (corre en el host) |

Para conectar pgAdmin a la base, el host es `postgres` (red interna de Docker).

### Datos de ejemplo

`db/seed/01-reset-y-seed.sql` **borra todos los datos** y carga el juego común: 4 regiones, 5 municipios, 10 recursos y 5 ONGs con su inventario. También ajusta el esquema que Hibernate no actualiza solo. No carga emergencias ni lotes: cada emergencia necesita su caso en Bonita, así que se crean desde la app.

En una base nueva tiene que correrse después de levantar el backend al menos una vez, porque es el backend el que crea las tablas.

Con `docker compose up`, el servicio `seed` lo corre solo y únicamente si la base está vacía, así que reiniciar el stack no borra nada. Para forzarlo (borra todo):

```bash
docker compose run --rm -e FORCE_SEED=1 seed
```

## Configuración del login con Bonita

Los usuarios, contraseñas y roles viven en la organización `RescueSync` de Bonita. El backend valida cada login contra el `loginservice` de Bonita y guarda esa sesión para operar el proceso en nombre del usuario.

1. Abrir Bonita Studio e importar `Modelado/rescueSync-V1.3.bos`.
2. **Mapear los actores** del proceso: Municipio → `Operador Municipal`, Centro Coordinador Regional → `Coordinador Regional`, ONG → `Representante de ONG`.
3. **Activar** la organización `RescueSync` y hacer **Desplegar**.
4. Desplegar el proceso en el motor de Studio: el backend lo busca por nombre (`Sistema P1`).
5. Verificar que `BONITA_BASE_URL` apunte a ese Bonita (por defecto `http://localhost:8080/bonita`).

### Usuarios de ejemplo

Todos tienen contraseña `bpm`.

| Rol en Bonita | Rol en la app | Usuarios |
|---|---|---|
| Operador Municipal | `MUNICIPAL` | `operador.laplata`, `operador.citybell`, `operador.berisso`, `operador.lobos`, `operador.quilmes` |
| Coordinador Regional | `COORDINADOR` | `coord.norte`, `coord.sur`, `coord.region3`, `coord.region4` |
| Representante de ONG | `ONG` | `ong.cruzroja`, `ong.caritas`, `ong.bomberos.1`, `ong.bancoalimentos.1`, `ong.techo.1` |
| Auditor | `AUDITOR` | `auditor.nacional` |

La lista completa, con la región de cada municipio y la ONG de cada usuario, está en [docs/bonita.md](docs/bonita.md).

### Problemas frecuentes

| Síntoma | Causa |
|---|---|
| El login devuelve 401 con un usuario que debería existir | La organización `RescueSync` no está activa o desplegada (por ejemplo, quedó activa ACME). |
| El login devuelve 404 | El grupo del usuario existe en Bonita pero no hay fila con ese path en la base: falta correr el seed. |
| Una operación devuelve 502 | Falló la llamada a Bonita (por ejemplo, no está corriendo o el proceso no está desplegado). El detalle queda en el log del backend. |
| El backend no arranca, o Docker falla con `ports are not available` | El puerto está ocupado por Bonita Studio: revisar `SERVER_PORT` / `BACKEND_PORT`. |

## Convenciones

**Git**
- Se trabaja en ramas `feature/<nombre>`, `bugfix/<nombre>` o `refactor/<nombre>` creadas desde `development`.
- Los cambios entran a `development` por pull request; no se commitea directo ahí.

**Backend**
- Paquetes por capa: `controller -> service -> repository`. El controller nunca llama a un repository ni expone entidades: trabaja con DTOs (`<Concepto>Request` / `<Concepto>Response`).
- Endpoints bajo `/api/<recurso>`, con validación por Bean Validation en los DTOs.
- Las llamadas a sistemas externos van en `client/`. Primero se escribe en la base y Bonita es el último paso de la transacción.
- Los errores salen por `GlobalExceptionHandler` con un campo `mensaje` que muestra el front.

**Frontend**
- Carpetas `core/`, `shared/`, `layout/` y `features/<perfil>/`, con un archivo de rutas por perfil.
- Componentes standalone, sin sufijo `Component`, con signals y control flow moderno (`@if`, `@for`).
- Llamadas HTTP solo desde servicios, con `apiUrl` tomado de `environment`.
- Errores con `ToastService`; formato con Prettier.

El detalle de cada regla, cómo agregar una pantalla y la guía de diseño están en [docs/arquitectura.md](docs/arquitectura.md).
