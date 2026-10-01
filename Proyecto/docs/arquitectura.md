# Arquitectura y convenciones de código

Detalle de lo que el [README](../README.md) resume. Para las reglas de negocio ver [funcionalidades.md](funcionalidades.md); para la integración con Bonita, [bonita.md](bonita.md).

## Backend: Package-by-Layer

App chica que no va a escalar, así que se organiza por capa técnica y no por dominio. Paquete base: `com.proyecto.backend`.

```
com.proyecto.backend
  controller/    endpoints REST; solo reciben y devuelven DTOs, sin lógica de negocio
  service/       lógica de negocio y transacciones (@Transactional)
  repository/    interfaces Spring Data JPA
  model/         entidades JPA y enums del dominio (NivelGravedad, EstadoOferta, EstadoLote)
  dto/           records `<Concepto>Request` / `<Concepto>Response` (paquete plano, salvo `auth/`); nunca se exponen entidades en la API
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
- Endpoints bajo `/api/<recurso>` (sin versión en la ruta). Controllers con `@RequiredArgsConstructor`, que devuelven el DTO directo con `@ResponseStatus` (`ResponseEntity` solo si hace falta, ej. `Location`).

### Errores

`GlobalExceptionHandler` responde `ErrorResponse {timestamp, status, error, mensaje, detalles}`. El front muestra `mensaje`.

| Caso | Excepción | Status |
|---|---|---|
| Recurso no encontrado | `RecursoNoEncontradoException` (la única de "no encontrado") | 404 |
| Regla de negocio | `ReglaNegocioException` | 409 |
| Falla de Bonita | `BonitaIntegrationException` | 502, con mensaje genérico (sin ids de Bonita: el detalle va solo al log) |
| Sin sesión / rol que no corresponde | | 401 / 403 |

### Transacciones y Bonita

**Primero se escribe en la base (dentro de la transacción) y la llamada a Bonita es el último paso.** Si Bonita falla se hace rollback y no queda nada. `BonitaClient` traduce toda falla de Bonita a `BonitaIntegrationException`. Si ya se había creado el caso, `EmergenciaService` lo elimina (`BonitaClient.cancelarCaso`).

### Notificaciones

Son una ayuda, no algo esencial: se crean después del commit (`@TransactionalEventListener`) y si fallan solo se loguea; nunca se muestran como error.

| Cuándo | Quién la recibe | Código |
|---|---|---|
| Se registra una emergencia | Coordinadores de la región del municipio | `EmergenciaRegistradaEvent` → `NotificacionEmergenciaListener` |
| Cambia el lote de una emergencia (hoy: al publicarlo) | Operadores del municipio de la emergencia | `LoteCambiadoEvent` → `NotificacionLoteListener` |

`LoteCambiadoEvent` lleva el estado en que quedó el lote (`ACTIVO`, `CANCELADO`, `FINALIZADO`) y el listener arma el texto de los tres, aunque hoy solo se publica al activar un lote.

### Logs

`info` en cada escritura (ids relevantes), `warn` en rechazos de negocio, `error` en fallos inesperados o de integración. Nunca datos sensibles.

### Enums y base de datos

Hibernate crea un *check constraint* con los valores de cada enum persistido (por ejemplo `oferta_estado_check`) y `ddl-auto=update` no lo actualiza. Si se agrega un valor a un enum, hay que borrar ese constraint (el seed ya lo hace para `oferta_estado_check`), o la base rechaza el valor nuevo.

## Frontend: core / shared / layout / features

Componentes standalone, signals y control flow moderno (`@if`, `@for`). Estructura bajo `frontend/src/app/`:

```
core/       singletons de la app, se cargan una vez
  guards/     guards de rutas (auth-guard, rol-guard)
  services/   servicios globales (auth, cliente HTTP base)
  models/     interfaces y tipos TypeScript compartidos (espejo de los DTOs del backend)
layout/     estructura visual de la app (navbar, notifications)
shared/     piezas reutilizables sin lógica de negocio
  components/ directives/ pipes/
features/   una carpeta por funcionalidad o pantalla (lazy-loaded desde app.routes.ts)
```

Reglas:
- Cada feature vive en `features/<nombre>/` con sus componentes, servicios y rutas propias. Puede haber una por perfil (municipal, coordinador, ong, auditor) o por proceso (emergencias, lotes, ofertas).
- `core` no depende de `features`. `features` puede usar `core` y `shared`. `shared` no depende del resto, salvo los tipos y constantes de `core/models` que usan sus componentes de presentación (`LoteFicha`, `LoteTarjeta`, `EmergenciaTarjeta`).
- Llamadas HTTP solo desde servicios, nunca desde componentes. Todos los servicios usan `@Service()`, nombre corto de clase (`Lotes`, `Emergencias`, `Recursos`, `Notificaciones`, `Ofertas`, `Ongs`) y `apiUrl` tomado de `environment` (nunca de `environment.development`: el build de producción usa `/api` y nginx lo proxea al backend). Si lo usa más de un perfil va en `core/services/<recurso>.ts`; si es de un solo feature, en `features/<x>/services/`.
- Modelos: un archivo por concepto en `core/models/` (`lote.ts`, `emergencia.ts`, `recurso.ts`...), espejo de los records del backend.
- Errores: se muestran con `ToastService` usando `mensajeDeError(err, fallback)` (`core/services/errores.ts`), que lee el `mensaje` del backend. Los de notificaciones no se muestran (solo `console.error`).
- Componentes: archivos y clases sin sufijo `Component` (`PublicacionLotes`, `AltaEmergencia`).
- Estado de UI con signals.
- Estilos: Bootstrap 5, solo CSS (sin JS ni librerías de componentes), cargado desde `angular.json`.

### Rutas

Cada perfil tiene su propio archivo de rutas, cargado con lazy loading desde `app.routes.ts`. Los perfiles (`MUNICIPAL`, `COORDINADOR`, `ONG`, `AUDITOR`, definidos en `core/models/rol.ts`) controlan el acceso: `rolGuard(rol)` (`core/guards/rol-guard.ts`) deja entrar a cada bloque de rutas solo al usuario con ese rol y manda al resto al inicio de su propio perfil. El backend valida lo mismo en cada endpoint.

| Ruta | Feature | Rol | Inicio (la raíz redirige acá) |
|---|---|---|---|
| `/login` | `features/auth/` (pública) | | |
| `/municipal/...` | `features/municipal/municipal.routes.ts` | `MUNICIPAL` | `/municipal/emergencias` |
| `/coordinador/...` | `features/coordinador/coordinador.routes.ts` | `COORDINADOR` | `/coordinador/emergencias` |
| `/ong/...` | `features/ong/ong.routes.ts` | `ONG` | `/ong/lotes` |
| `/auditor/...` | `features/auditor/auditor.routes.ts` | `AUDITOR` | `/auditor/ofertas` |

Todo lo que no es `/login` está dentro de `MainLayout` (navbar + contenido) y protegido por `authGuard`; `/` redirige al inicio del rol del usuario. Cualquier otra ruta muestra la página 404. No hay pantallas de inicio vacías: el inicio de cada perfil es su listado, con las acciones que necesita.

### Cómo agregar una pantalla

Para agregar una pantalla a un perfil (ej. `ong`):
1. Crear el componente en `features/ong/` (o en una subcarpeta si tiene varias piezas).
2. Agregar la ruta en `features/ong/ong.routes.ts`: `{ path: 'ofertas', component: Ofertas }`. No hace falta tocar `app.routes.ts`.
3. Agregar el link en `NAV_LINKS` de `layout/navbar/navbar.ts` para que aparezca en el menú.

## Guía de diseño de pantallas

Las pantallas de ONG (`features/ong/lotes/`) son la referencia; las de municipal y coordinador siguen el mismo armado.

- **Listado**: `<h1 class="h3">` (con la acción principal como `btn btn-primary` a la derecha) y una grilla `row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4` de tarjetas clickeables. Usar `LoteTarjeta` (`shared/components/lote-tarjeta/`) o `EmergenciaTarjeta` (`shared/components/emergencia-tarjeta/`): encabezado con el color de la gravedad, descripción recortada y pie con fecha y badge de estado.
- **Detalle**: sección `rounded-4` con el fondo de la gravedad, badges arriba, título `h4` y datos con rótulos `small text-uppercase text-body-secondary`; debajo, tarjetas `card border-0 shadow-sm`; al pie, `btn btn-outline-secondary` "Volver al listado". Para un lote usar `LoteFicha` (`shared/components/lote-ficha/`), que acepta badges (`ficha-badges`) y acciones (`ficha-acciones`) proyectados.
- **Formulario**: `h1.h3` + `card border-0 shadow-sm`, botón principal `btn btn-primary` y "Volver al listado" `btn btn-outline-secondary`. Al guardar se muestra un toast y se navega al detalle de lo creado.
- **Estados**: "Cargando…" y listas vacías en `text-body-secondary`; errores de carga en `alert alert-danger`; errores de una acción con `ToastService`.
- **Gravedad**: `NIVEL_GRAVEDAD_BADGE` y `NIVEL_GRAVEDAD_COLOR` (`core/models/emergencia.ts`): Baja gris, Media amarillo, Alta rojo, Crítica violeta (`text-bg-critica` / `bg-critica-subtle`, en `styles.scss`).
- **Fechas**: formatos de `shared/formatos-fecha.ts` con el `date` pipe: `FECHA_LISTADO` en listados, `FECHA_LARGA` en detalles y notificaciones, `FECHA_DIA` sin hora.
