# RescueSync

Trabajo Práctico Integrador, Desarrollo de Software en Sistemas Distribuidos (DSSD), curso 2026.

RescueSync es una plataforma para coordinar la respuesta ante desastres naturales a nivel regional. Conecta municipios afectados con ONGs y organismos de rescate para distribuir recursos y personal de forma solidaria y ordenada.

Este repositorio contiene la **aplicación web local** (`frontend/` + `backend/`). El proceso Bonita y la API del Sistema Nacional son componentes aparte (ver [Componentes](#componentes-del-sistema)).

## Proceso de negocio

1. **Registro de emergencia**: un municipio afectado registra una emergencia (inundación, incendio, terremoto) con nivel de gravedad, zona afectada y descripción.
2. **Generación de lotes**: el Centro Coordinador Regional revisa la alerta y la desglosa en "Lotes de Necesidades" (ej. 5 paramédicos, 1000 raciones de alimento). Luego publica la convocatoria a toda la red.
3. **Convocatoria y ofertas**: se abre una ventana de tiempo para recibir ofertas. Las ONGs cargan "Ofertas de Ayuda" (recursos y personal) para cubrir los lotes. Se permiten **ofertas parciales o conjuntas (consorcios entre ONGs)** y **modificar la oferta dentro de la ventana**.
4. **Validación externa y asignación flexible**: al vencer el tiempo, el sistema evalúa cada oferta contra el Sistema Nacional. Este **no rechaza de forma binaria**: devuelve perfiles de competencia o niveles de habilitación. Las ofertas se adaptan a lotes principales o de apoyo. La API cloud garantiza consistencia y bloqueo concurrente para evitar sobre-asignaciones.
5. **Adjudicación y notificación**: el municipio ve **solo las ofertas validadas** y elige las que mejor cubran sus necesidades. Se notifica formalmente a las ONGs seleccionadas.
6. **Compromiso nacional**: tras adjudicar, se registra en el Sistema Nacional el compromiso de los recursos, para que la ONG no los ofrezca en otra emergencia en paralelo.
7. **Ejecución, finalización y cierre bilateral**: el Centro Coordinador monitorea el despliegue. Las ONGs marcan sus actividades como finalizadas en la app; la plataforma informa al sistema central vía API cloud, se marca el proyecto como terminado y se liberan los recursos.

## Componentes del sistema

| Componente | Rol |
|---|---|
| **Bonita BPM** (7.9.0 o superior) | Orquesta el proceso y administra la ventana de ofertas con un Timer Event. |
| **App web local** (este repo) | Formularios, RBAC, ofertas y versiones, BD local PostgreSQL. Expone el endpoint que consume Bonita. |
| **Sistema Nacional de Gestión de Recursos y Riesgos** | API REST externa con JWT, dockerizada y desplegada en la nube (Render o Heroku). Valida, bloquea, compromete y libera recursos. |
| **Dashboard** | Indicadores para el personal directivo, con datos propios y datos de la API de Bonita. |

### Requisitos por componente

**Proceso en Bonita**
- Orquesta el ciclo de vida completo de la emergencia, incluyendo notificaciones y cierre operativo.
- Timer Event para la ventana de ofertas. Si vence y no se cubrió el total de lotes, un camino alternativo devuelve la tarea al Centro Coordinador para que decida: reabrir la convocatoria, reformular lotes o continuar con cobertura parcial.
- Los consorcios y las ofertas parciales se resuelven en los formularios web y la BD local, no en los nodos de Bonita.

**Aplicación web**
- Interfaces para cada etapa del proceso, integradas con la API de Bonita. Responsive.
- **RBAC** con 4 perfiles, con interfaces y permisos propios:
  1. Operador Municipal
  2. Centro Coordinador Regional
  3. Representante de ONG: gestiona inventario, ve notificaciones de adjudicación, registra ofertas parciales y consorcios, y marca actividades como finalizadas.
  4. Auditor / Directivo
- **Versionado de ofertas**: las ONGs editan su oferta dentro de la ventana de tiempo, con trazabilidad en el backend.

**Sistema Nacional (API externa)**
- Autenticación JWT, dockerizada, con documentación Swagger.
- Servicios REST mínimos:
  1. Autenticación y cuentas: `/login` con JWT y registro (onboarding) de ONGs.
  2. Catálogo y consulta: certificaciones, ONGs habilitadas y disponibilidad global de recursos.
  3. Validación por niveles de competencia: objetos con capacidades habilitadas y niveles de riesgo permitidos, sin rechazo binario.
  4. Concurrencia y bloqueo: validar restricciones y ejecutar el bloqueo transaccional de recursos al adjudicar, con locking en PostgreSQL contra race conditions.
  5. Liberación y cierre: reportar la finalización de actividades de la ONG y el cierre del proyecto, liberando los recursos bloqueados.

**Dashboard**
- Reportes que combinan datos propios (suministros movilizados en el mes, porcentaje de resolución mediante consorcios, estado de proyectos finalizados) con datos de la API de Bonita (tiempos promedio de cada etapa).
- Gráficos interactivos y exportación de datos estructurados.

## Directivas de integración

1. **Ofertas locales**: las ONGs registran y editan sus ofertas solo a través de la app web, persistidas en la BD local (PostgreSQL). La API del Sistema Nacional **no** guarda borradores ni ofertas preliminares.
2. **Tiempo y cierre en Bonita**: Bonita administra la ventana temporal con un Timer Event. Al vencer, el flujo avanza automáticamente a una tarea de validación.
3. **Consulta orquestada**: Bonita inicia la comunicación. Al vencer el timer, una tarea de servicio o conector hace un `GET` a un endpoint del backend local para pedir el listado consolidado de ofertas de esa emergencia. El backend responde un JSON con todas las ofertas.
4. **Validación, bloqueo y cierre**: con ese JSON, Bonita invoca la API del Sistema Nacional, que evalúa las credenciales y aplica locking. Al terminar la ejecución y marcarse el cierre en la app local, se invoca el servicio de liberación de la API cloud.

## Cronograma de entregas

| Semana | Entrega |
|---|---|
| 10/9 | **E1**: modelo de proceso en Bonita (timers y eventos alternativos). Grooming de tareas y estimación de la siguiente etapa. |
| 1/10 | **E2**: formulario web de alta de emergencia y publicación de lotes (municipio/coordinador). Formulario básico de carga de ofertas para ONGs. Integración inicial con la API de Bonita (iniciar instancia y setear variables de proceso). Validar el grooming contra las tareas hechas y planificar la siguiente etapa. |
| 22/10 | **E3**: API JWT dockerizada en la nube (validación de certificaciones, gestión de ONGs, compromiso y liberación de recursos). URLs de servicios, Swagger, repositorios y Dockerfile. |
| 5/11 | **E4**: diagrama de Gantt con lo realizado (con desvíos) y lo restante. |
| 19/11 | **E5**: informe final con la documentación de cada componente, su rol e interacción en la arquitectura final. |
| 26/11 | Coloquio final |
| 3/12 | Recuperatorio de coloquio |

Todas las entregas son obligatorias: no realizarlas implica perder la cursada. No aprobar las intermedias reduce la nota final del trabajo, que es la de mayor peso en la materia.

## Stack

- **Backend**: Spring Boot 4.1.1, Java 21, Maven. Dependencias: web, data-jpa, validation, postgresql.
- **Frontend**: Angular 22, TypeScript, npm, Bootstrap 5. Tests con Vitest, formato con Prettier.
- **Base de datos**: PostgreSQL 15 en Docker (pgAdmin incluido).

## Arquitectura

### Backend: Package-by-Layer

App chica que no va a escalar, así que se organiza por capa técnica y no por dominio. Paquete base: `com.proyecto.backend`.

```
com.proyecto.backend
  controller/    endpoints REST; solo reciben y devuelven DTOs, sin lógica de negocio
  service/       lógica de negocio y transacciones (@Transactional)
  repository/    interfaces Spring Data JPA
  model/         entidades JPA y enums del dominio (NivelGravedad, EstadoOferta, EstadoLote)
  dto/           request/response; nunca se exponen entidades en la API
  mapper/        conversión entidad <-> DTO
  client/        clientes HTTP hacia sistemas externos (Bonita, Sistema Nacional)
  security/      autenticación, roles (RBAC), filtros
  exception/     excepciones de negocio y @RestControllerAdvice global
  config/        configuración (CORS, seguridad, RestClient, etc.)
```

Reglas:
- Flujo de dependencias: `controller -> service -> repository`. El controller nunca llama a un repository.
- Las entidades no salen del service; el controller trabaja con DTOs.
- Validación de entrada con Bean Validation en los DTOs (`@Valid`).
- Las llamadas a Bonita y al Sistema Nacional van en `client/`, invocadas desde los services.
- Endpoints bajo `/api/...`.

### Frontend: core / shared / layout / features

Componentes standalone, signals y control flow moderno (`@if`, `@for`). Estructura bajo `frontend/src/app/`:

```
core/       singletons de la app, se cargan una vez
  guards/     guards de rutas (auth-guard)
  services/   servicios globales (auth, cliente HTTP base)
  models/     interfaces y tipos TypeScript compartidos (espejo de los DTOs del backend)
layout/     estructura visual de la app (navbar, notifications)
shared/     piezas reutilizables sin lógica de negocio
  components/ directives/ pipes/
features/   una carpeta por funcionalidad o pantalla (lazy-loaded desde app.routes.ts)
```

Reglas:
- Cada feature vive en `features/<nombre>/` con sus componentes, servicios y rutas propias. Puede haber una por perfil (municipal, coordinador, ong, auditor) o por proceso (emergencias, lotes, ofertas).
- `core` no depende de `features`. `features` puede usar `core` y `shared`. `shared` no depende del resto.
- Rutas protegidas con `authGuard` en `app.routes.ts` (sin verificación de rol, ver "Identidad, login y ONGs").
- Llamadas HTTP solo desde servicios, nunca desde componentes.
- Estado de UI con signals.

### Rutas del frontend y cómo agregar una pantalla

Cada perfil tiene su propio archivo de rutas, cargado con lazy loading desde `app.routes.ts`. Los perfiles (`MUNICIPAL`, `COORDINADOR`, `ONG`, `AUDITOR`, definidos en `core/models/rol.ts`) son un concepto de UI/menú, no de control de acceso: **hoy no hay restricción por rol**, cualquier usuario logueado puede entrar a cualquier pantalla.

| Ruta | Feature |
|---|---|
| `/login` | `features/auth/` (pública) |
| `/municipal/...` | `features/municipal/municipal.routes.ts` |
| `/coordinador/...` | `features/coordinador/coordinador.routes.ts` |
| `/ong/...` | `features/ong/ong.routes.ts` |
| `/auditor/...` | `features/auditor/auditor.routes.ts` |

Todo lo que no es `/login` está dentro de `MainLayout` (navbar + contenido) y protegido por `authGuard`; `/` redirige al inicio del rol del usuario. Cualquier otra ruta muestra la página 404.

Para agregar una pantalla a un perfil (ej. `ong`):
1. Crear el componente en `features/ong/` (o en una subcarpeta si tiene varias piezas).
2. Agregar la ruta en `features/ong/ong.routes.ts`: `{ path: 'ofertas', component: Ofertas }`. No hace falta tocar `app.routes.ts`.
3. Agregar el link en `NAV_LINKS` de `layout/navbar/navbar.ts` para que aparezca en el menú.

## Identidad, login y ONGs (vinculación con Bonita)

Por indicación de la cátedra, **los usuarios viven en la organización de Bonita**, no en una tabla propia. La app no guarda usuarios, contraseñas ni roles.

### Login
1. El front hace `POST /api/auth/login` con `{ username, password }`.
2. El backend (`BonitaClient`) llama a `POST {BONITA_BASE_URL}/loginservice` (form-urlencoded) y obtiene las cookies `JSESSIONID` y `X-Bonita-API-Token`.
3. Con esa sesión consulta a Bonita el usuario y su **membership** (rol + grupo). Cada usuario debe tener **exactamente una** membership.
4. El rol de Bonita se traduce al rol de la app: `Operador Municipal` → `MUNICIPAL`, `Coordinador Regional` → `COORDINADOR`, `Representante de ONG` → `ONG`, `Auditor` → `AUDITOR`.
5. La sesión de Bonita se guarda en la `HttpSession` del backend (cookie de sesión del backend). Endpoints: `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`.
6. Cada intento de login deja un log (`INFO` si funciona; `WARN`/`ERROR` con el motivo si falla). Las contraseñas no se loguean.

En el front, `Auth` (`core/services/auth.ts`) llama a esos endpoints con `withCredentials`, y al arrancar la app restaura la sesión con `GET /api/auth/me`. `authGuard` deja pasar solo a usuarios logueados. Los errores de login se muestran con un toast (`ToastService`). Sigue sin haber restricción por rol en las rutas del front.

### Organización de Bonita (`RescueSync`)
La organización se define en `RescueSync.xml` (dentro del `.bos` del proceso, carpeta `Modelado/`). Para usarla: en Bonita Studio, importar el `.bos`, **activar** la organización RescueSync y hacer **Desplegar**. Si el login devuelve 401 para un usuario que debería existir, casi seguro la organización no está activa/desplegada (por ejemplo, quedó activa ACME).

Todos los usuarios de ejemplo tienen contraseña `bpm`.

| Rol | Grupo | Usuarios |
|---|---|---|
| Operador Municipal | subgrupo de una región (ver abajo) | `operador.laplata`, `operador.citybell`, `operador.berisso`, `operador.lobos`, `operador.quilmes` |
| Coordinador Regional | grupo de su región (ver abajo) | `coord.norte`, `coord.sur`, `coord.region3`, `coord.region4` |
| Auditor | `Sistema Nacional` | `auditor.nacional`, `Usuario1` |
| Representante de ONG | subgrupo de `ONG` (ver abajo) | ver abajo |

### Municipios y regiones
Los municipios son **subgrupos de una región**, y las regiones son subgrupos de `/Municipio`. Un Coordinador Regional pertenece a una región y trabaja con las emergencias de los municipios de esa región.

| Región (path en Bonita) | Coordinador | Municipios (path) | Operador |
|---|---|---|---|
| `/Municipio/Region1` | `coord.norte` | `/Municipio/Region1/LaPlata` | `operador.laplata` |
| | | `/Municipio/Region1/CityBell` | `operador.citybell` |
| `/Municipio/Region2` | `coord.sur` | `/Municipio/Region2/Berisso` | `operador.berisso` |
| `/Municipio/Region3` | `coord.region3` | `/Municipio/Region3/Lobos` | `operador.lobos` |
| `/Municipio/Region4` | `coord.region4` | `/Municipio/Region4/Quilmes` | `operador.quilmes` |

Igual que con las ONGs, el vínculo con la base es por **path**: `municipio.bonita_group_path` y `region.bonita_group_path` (únicos, obligatorios), y `municipio.region_id` apunta a su región. En el login:
- `MUNICIPAL`: el path del grupo (`/Municipio/Region1/LaPlata`) resuelve el municipio y su región; la respuesta trae `municipioId`, `municipioNombre`, `regionId` y `regionNombre`.
- `COORDINADOR`: el path del grupo (`/Municipio/Region1`) resuelve la región; la respuesta trae `regionId` y `regionNombre`.
- Los demás roles reciben esos campos en `null`.

Al registrar una emergencia el municipio sale del usuario logueado (no del request) y se manda a Bonita el `regionGroupPath`, para que la app filtre las tareas del coordinador por región.

### Regla: un lote por emergencia
Una emergencia tiene **un solo lote no cancelado**. Si el coordinador cancela el lote (por ejemplo, porque nadie ofertó) y arma otro, cada publicación conserva su propia **ventana de ofertas** (`lote.fecha_apertura_ofertas` / `lote.fecha_cierre_ofertas`); por eso las fechas viven en el lote y no en la emergencia. La base lo garantiza con un índice único parcial (`lote(emergencia_id) WHERE estado <> 'CANCELADO'`) y `LoteRepository.existsByEmergenciaIdAndEstadoNot` queda listo para que la creación de lotes lo valide. `OfertaService.registrar` rechaza ofertas antes de la apertura o después del cierre del lote.

### Edición y baja de ofertas
- Mientras la ventana del lote esté abierta y la oferta siga `PENDIENTE`, **cualquier ONG que participe** de la oferta puede editarla o eliminarla desde el detalle del lote (botones "Editar" y "Eliminar", este último con confirmación).
- **Editar** (`PUT /api/ofertas/{id}`) cambia solo las cantidades. Las ONGs participantes no se pueden cambiar: para eso hay que eliminar la oferta y registrar una nueva.
- **Eliminar** (`DELETE /api/ofertas/{id}`) es una **baja lógica**: la oferta pasa al estado `ELIMINADA`, conserva sus detalles y deja de aparecer en los listados.
- La oferta registra la fecha de su última modificación (`fecha_modificacion`) y su número de versión vigente (`numero_version`).
- **Trazabilidad**: cada alta, edición y baja guarda una versión inmutable (`oferta_version` + `detalle_oferta_version`) con el número, el tipo de cambio (`CREACION`, `EDICION`, `BAJA`), el estado, la fecha, el **username de Bonita** de quien lo hizo y su ONG, más una copia de todas las cantidades de ese momento. `GET /api/ofertas/{id}/versiones` devuelve el historial (ONGs participantes y auditor). Las ONGs lo ven en la pestaña "Historial" del detalle de la oferta; el auditor ve todas las ofertas, incluidas las eliminadas, en `/auditor/ofertas` (`GET /api/ofertas`, solo auditor).
- Fuera de la ventana, con una oferta no pendiente o desde una ONG que no participa, el backend rechaza la operación (400 / 403).

> **Enums y base de datos:** Hibernate crea un *check constraint* con los valores de cada enum persistido (por ejemplo `oferta_estado_check`) y `ddl-auto=update` no lo actualiza. Si se agrega un valor a un enum, hay que borrar ese constraint (el seed ya lo hace para `oferta_estado_check`), o la base rechaza el valor nuevo.

### Proceso 1 en Bonita: instanciación y ventana de ofertas
- El modelo vigente es `Modelado/rescueSync-V1.3.bos` (Proceso 1 con variables, contratos, operaciones y dos timers; detalle en `.claude/docs/bonita-modelo.md`). Después de importarlo hay que **mapear los actores** en Studio (Municipio → Operador Municipal, Centro Coordinador Regional → Coordinador Regional, ONG → Representante de ONG), activar RescueSync y desplegar.
- Al registrar una emergencia, `EmergenciaService` usa la sesión de Bonita del usuario: busca el proceso por nombre (`bonita.process.name`, por defecto `Proceso 1`), inicia el caso sin contrato, espera la tarea "Registrar emergencia" y la ejecuta con `{emergenciaId, municipioId, nivelGravedad, zonaAfectada, descripcion, regionGroupPath}`. Si Bonita falla, se revierte el alta.
- La tarea "Desglosar" del coordinador se completa al publicar el lote con `{loteId, fechaAperturaOfertas, fechaCierreOfertas}` (`BonitaClient.ejecutarTarea`, todavía por integrar con `feature/publicacion-lotes`). **La apertura y el cierre los elige el usuario al publicar el lote**: el proceso ya lo soporta sin cambios. El timer "Apertura de convocatoria" espera hasta `fechaAperturaOfertas` (si ya pasó, abre enseguida) y el boundary timer vence en `fechaCierreOfertas`. Las fechas se mandan como `LocalDateTime` sin zona (se interpretan en `America/Argentina/Buenos_Aires`) y el backend debe validar cierre posterior a apertura.

### ONGs como subgrupos de `ONG`
Cada ONG es un **subgrupo de `/ONG`** en Bonita, y todos sus representantes tienen el rol `Representante de ONG`. Hay una ONG por subgrupo y puede tener varios usuarios:

| Subgrupo (path en Bonita) | ONG | Usuarios |
|---|---|---|
| `/ONG/CruzRojaLaPlata` | Cruz Roja Argentina – Filial La Plata | `ong.cruzroja`, `ong.cruzroja.2`, `ong.cruzroja.3` |
| `/ONG/CaritasBuenosAires` | Cáritas Arquidiócesis de Buenos Aires | `ong.caritas`, `ong.caritas.2`, `ong.caritas.3` |
| `/ONG/BomberosLaPlata` | Bomberos Voluntarios de La Plata | `ong.bomberos.1`, `ong.bomberos.2` |
| `/ONG/BancoAlimentosBA` | Fundación Banco de Alimentos Buenos Aires | `ong.bancoalimentos.1`, `ong.bancoalimentos.2` |
| `/ONG/TechoBuenosAires` | Techo Argentina – Regional Buenos Aires | `ong.techo.1` |

### Cómo se vincula una ONG de Bonita con la base local
- La tabla `ong` tiene la columna **`bonita_group_path`** (única, obligatoria) con el path del subgrupo, por ejemplo `/ONG/CaritasBuenosAires`.
- Se vincula por **path y no por el id numérico** del grupo de Bonita: el id lo asigna cada Bonita al desplegar la organización y es distinto en cada máquina; el path sale del XML y es igual para todo el equipo.
- En el login de un usuario con rol `ONG`, el backend arma el path del grupo (`parent_path` + `name`), busca la fila en `ong` (`OngService.obtenerPorGrupoBonita`) y devuelve `ongId` y `ongNombre` en la respuesta. Para los otros roles esos campos son `null`.
- Si el subgrupo existe en Bonita pero no hay fila en `ong` con ese path, el login falla (404) con un mensaje claro.

### Cómo agregar una ONG nueva
1. En Bonita Studio: crear el subgrupo bajo `ONG`, agregar los usuarios y sus memberships (rol `Representante de ONG`), y volver a desplegar la organización. Compartir el `.bos`/XML actualizado con el equipo.
2. En la base: insertar la fila con el mismo path, por ejemplo `INSERT INTO ong (razon_social, bonita_group_path) VALUES ('Nueva ONG', '/ONG/NuevaOng');`. Idealmente sumarla también a `db/seed/01-reset-y-seed.sql`.

### Datos de ejemplo (seed)
`db/seed/01-reset-y-seed.sql` **borra todos los datos** (municipios, recursos, ONGs, emergencias y lo que depende de ellos) y carga el juego de datos común: 4 regiones y 5 municipios (ver arriba), 10 recursos, las 5 ONGs con su inventario. **No carga emergencias ni lotes**: cada emergencia necesita su caso en Bonita, así que se crean desde la app (operador municipal registra, coordinador publica el lote). También adapta el esquema (tablas/columnas de región, municipio y lote, `oferta.fecha_modificacion` y el check de estados de oferta). Correrlo antes de levantar el backend (la entidad `Ong` exige `bonita_group_path`):

```bash
docker exec -i postgres_db psql -U postgres -d rescuesync < db/seed/01-reset-y-seed.sql
```

Con `docker compose up` el servicio `seed` lo corre solo, **una vez que el backend está healthy** y **solo si la base está vacía** (sin regiones), así que reiniciar el stack no borra tus emergencias. Para forzarlo (borra todo): `docker compose run --rm -e FORCE_SEED=1 seed`.

La UI usa **Bootstrap 5** (solo CSS, sin JS ni librerías de componentes). Se carga desde `angular.json`.

## Variables de entorno y puertos

La configuración sale de un `.env` en la raíz del repo (ignorado por git). Creá ese archivo a mano con las variables de la tabla (no hay plantilla versionada). Lo leen tanto Docker Compose como el backend cuando corre en local.

| Variable | Uso | Valor de ejemplo |
|---|---|---|
| `DB_HOST` | Host de PostgreSQL para el backend local (en Docker se fuerza `postgres`) | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Credenciales de la base | `rescuesync` / `postgres` / (completar) |
| `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` | Login de pgAdmin | (completar) |
| `SERVER_PORT` | Puerto del backend cuando corre en local (`./mvnw spring-boot:run`) | `8080` |
| `BACKEND_PORT` | Puerto del host donde Docker publica el backend | `8080` |
| `BONITA_BASE_URL` | URL base de Bonita para el backend local (opcional; por defecto `http://localhost:8080/bonita`) | `http://localhost:8080/bonita` |
| `BONITA_DOCKER_URL` | URL de Bonita para el backend en Docker (opcional; por defecto `http://host.docker.internal:8080/bonita`, o sea Bonita Studio en el host) | `http://host.docker.internal:8080/bonita` |

**Puertos y URLs**

| Servicio | Local (sin Docker) | Docker Compose | Red interna de Docker |
|---|---|---|---|
| Backend (Spring Boot) | http://localhost:`SERVER_PORT` (8080 por defecto) | http://localhost:`BACKEND_PORT` (8080 por defecto) | `backend:8080` |
| Frontend (Angular) | http://localhost:4200 (`npm start`) | http://localhost:80 | `frontend:80` |
| PostgreSQL | `localhost:5432` | `localhost:5432` | `postgres:5432` |
| pgAdmin | n/a | http://localhost:5050 | `pgadmin:80` |

> **Conflicto con Bonita Studio:** el Tomcat embebido de Bonita ocupa el puerto **8080**. Si lo tenés abierto, poné `SERVER_PORT=8081` y `BACKEND_PORT=8081` en tu `.env`; si no, el backend no arranca (o Docker falla con `ports are not available`).

## Base de datos

- PostgreSQL 15 en el contenedor `postgres_db`, puerto 5432.
- Base, usuario y contraseña definidos en el `.env` (`DB_NAME`, `DB_USER`, `DB_PASSWORD`).
- pgAdmin en http://localhost:5050 (credenciales en `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` del `.env`). Para conectarlo a la base desde pgAdmin, el host es `postgres` (red de Docker).

## Cómo correrlo

Requisitos: Docker, Java 21 y Node.js con npm. Antes de nada, creá el `.env` en la raíz (ver [Variables de entorno y puertos](#variables-de-entorno-y-puertos)).

Para desarrollo local se levanta solo la base y se corren backend y frontend a mano:

```bash
docker compose up -d postgres
```

Backend (desde `backend/`), en http://localhost:8080 (o el `SERVER_PORT` del `.env`):

```bash
./mvnw spring-boot:run     # ejecutar
./mvnw test                # tests
./mvnw clean package       # build
```

Frontend (desde `frontend/`), en http://localhost:4200:

```bash
npm install
npm start                  # servidor de desarrollo
npm test                   # tests
npm run build              # build
```

Stack completo en Docker (backend en :`BACKEND_PORT`, 8080 por defecto; frontend en :80):

```bash
docker compose up --build
```

Después de cambiar código hay que reconstruir la imagen del servicio afectado (`docker compose up -d --build backend`). Si solo cambia el `.env` o el `docker-compose.yml`, alcanza con `docker compose up -d`.
