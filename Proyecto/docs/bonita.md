# Integración con Bonita

Los pasos para dejar Bonita andando están en el [README](../README.md#configuración-del-login-con-bonita). Acá está el detalle de cómo se vincula la organización de Bonita con la base local y cómo se instancia el proceso.

## Cómo funciona el login

Por indicación de la cátedra, **los usuarios viven en la organización de Bonita**, no en una tabla propia. La app no guarda usuarios, contraseñas ni roles.

1. El front hace `POST /api/auth/login` con `{ username, password }`.
2. El backend (`BonitaClient`) llama a `POST {BONITA_BASE_URL}/loginservice` (form-urlencoded) y obtiene las cookies `JSESSIONID` y `X-Bonita-API-Token`.
3. Con esa sesión consulta a Bonita el usuario y su **membership** (rol + grupo). Cada usuario debe tener **exactamente una** membership.
4. El rol de Bonita se traduce al rol de la app: `Operador Municipal` → `MUNICIPAL`, `Coordinador Regional` → `COORDINADOR`, `Representante de ONG` → `ONG`, `Auditor` → `AUDITOR`.
5. La sesión de Bonita se guarda en la `HttpSession` del backend (cookie de sesión del backend). Endpoints: `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`.
6. Cada intento de login deja un log (`INFO` si funciona; `WARN`/`ERROR` con el motivo si falla). Las contraseñas no se loguean.

En el front, `Auth` (`core/services/auth.ts`) llama a esos endpoints con `withCredentials`, y al arrancar la app restaura la sesión con `GET /api/auth/me`. `authGuard` deja pasar solo a usuarios logueados y `rolGuard` restringe cada bloque de rutas a su rol. Los errores de login se muestran con un toast (`ToastService`).

## Organización `RescueSync`

Se define en `RescueSync.xml` (carpeta `Modelado/` del repo, también incluida en el `.bos`). Todos los usuarios de ejemplo tienen contraseña `bpm`.

### Municipios y regiones

Los municipios son **subgrupos de una región**, y las regiones son subgrupos de `/Municipio`. Un Coordinador Regional pertenece a una región y trabaja con las emergencias de los municipios de esa región.

| Región (path en Bonita) | Coordinador | Municipios (path) | Operador |
|---|---|---|---|
| `/Municipio/Region1` | `coord.norte` | `/Municipio/Region1/LaPlata` | `operador.laplata` |
| | | `/Municipio/Region1/CityBell` | `operador.citybell` |
| `/Municipio/Region2` | `coord.sur` | `/Municipio/Region2/Berisso` | `operador.berisso` |
| `/Municipio/Region3` | `coord.region3` | `/Municipio/Region3/Lobos` | `operador.lobos` |
| `/Municipio/Region4` | `coord.region4` | `/Municipio/Region4/Quilmes` | `operador.quilmes` |

### ONGs

Cada ONG es un **subgrupo de `/ONG`**, y todos sus representantes tienen el rol `Representante de ONG`. Hay una ONG por subgrupo y puede tener varios usuarios:

| Subgrupo (path en Bonita) | ONG | Usuarios |
|---|---|---|
| `/ONG/CruzRojaLaPlata` | Cruz Roja Argentina – Filial La Plata | `ong.cruzroja`, `ong.cruzroja.2`, `ong.cruzroja.3` |
| `/ONG/CaritasBuenosAires` | Cáritas Arquidiócesis de Buenos Aires | `ong.caritas`, `ong.caritas.2`, `ong.caritas.3` |
| `/ONG/BomberosLaPlata` | Bomberos Voluntarios de La Plata | `ong.bomberos.1`, `ong.bomberos.2` |
| `/ONG/BancoAlimentosBA` | Fundación Banco de Alimentos Buenos Aires | `ong.bancoalimentos.1`, `ong.bancoalimentos.2` |
| `/ONG/TechoBuenosAires` | Techo Argentina – Regional Buenos Aires | `ong.techo.1` |

### Auditores

`auditor.nacional` y `Usuario1`, en el grupo `Sistema Nacional`.

## Vínculo entre Bonita y la base local

El vínculo es por **path del grupo**, no por id: `ong.bonita_group_path`, `municipio.bonita_group_path` y `region.bonita_group_path` (únicos, obligatorios); `municipio.region_id` apunta a su región.

Se usa el path y no el id numérico porque el id lo asigna cada Bonita al desplegar la organización y es distinto en cada máquina; el path sale del XML y es igual para todo el equipo.

En el login, el backend arma el path del grupo (`parent_path` + `name`) y resuelve la fila local:

| Rol | Path de ejemplo | Campos en la respuesta del login |
|---|---|---|
| `MUNICIPAL` | `/Municipio/Region1/LaPlata` | `municipioId`, `municipioNombre`, `regionId`, `regionNombre` |
| `COORDINADOR` | `/Municipio/Region1` | `regionId`, `regionNombre` |
| `ONG` | `/ONG/CaritasBuenosAires` | `ongId`, `ongNombre` |
| `AUDITOR` | | ninguno |

Los campos que no corresponden al rol vienen en `null`. Si el grupo existe en Bonita pero no hay fila local con ese path, el login falla (404) con un mensaje claro.

## Cómo agregar una ONG nueva

1. En Bonita Studio: crear el subgrupo bajo `ONG`, agregar los usuarios y sus memberships (rol `Representante de ONG`), y volver a desplegar la organización. Subir el `.bos`/XML actualizado a `Modelado/`.
2. En la base: insertar la fila con el mismo path, por ejemplo `INSERT INTO ong (razon_social, bonita_group_path) VALUES ('Nueva ONG', '/ONG/NuevaOng');`.
3. Sumarla a `db/seed/01-reset-y-seed.sql` para que el resto del equipo la tenga.

## Proceso 1: instanciación y ventana de ofertas

El modelo vigente es `Modelado/rescueSync-V1.3.bos` (Proceso 1 con variables, contratos, operaciones y dos timers).

- **Alta de emergencia**: `EmergenciaService` usa la sesión de Bonita del usuario. Busca el proceso por nombre (`bonita.process.name` en `application.properties`, hoy `Sistema P1`), inicia el caso sin contrato, espera la tarea "Registrar emergencia" y la ejecuta con `{emergenciaId, municipioId, nivelGravedad, zonaAfectada, descripcion, regionGroupPath}`. El `regionGroupPath` permite filtrar las tareas del coordinador por región. Si Bonita falla, se revierte el alta y se elimina el caso si llegó a crearse.
- **Publicación del lote**: completa la tarea `Desglosar en "Lotes Necesidades"` del coordinador con `{loteId, fechaAperturaOfertas, fechaCierreOfertas}` (`LoteService.completarDesgloseEnBonita`). Si Bonita falla se revierte el lote.
- **Ventana de ofertas**: la apertura y el cierre los elige el usuario al publicar el lote. El timer "Apertura de convocatoria" espera hasta `fechaAperturaOfertas` (si ya pasó, abre enseguida) y el boundary timer vence en `fechaCierreOfertas`. Las fechas se mandan como `LocalDateTime` sin zona y se interpretan en `America/Argentina/Buenos_Aires`.
