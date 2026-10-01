# Funcionalidades por rol y reglas de negocio

Cada usuario tiene un solo rol. El front lo aplica con `rolGuard` en las rutas y el backend en cada endpoint (`AuthService`: `municipioDelUsuario`, `regionDelUsuario`, `ongDelUsuario`, `auditorDelUsuario`, `requerirRol`). Sin sesión la API responde 401; con un rol que no corresponde, 403.

## Funcionalidades

| Funcionalidad | Municipal | CCR | ONG | Auditor | Pantalla | Endpoint |
|---|:-:|:-:|:-:|:-:|---|---|
| Registrar emergencia (de su municipio) | ✔ | | | | `/municipal/emergencias/nueva` | `POST /api/emergencias` |
| Listar las emergencias de su municipio, con el estado de su lote | ✔ | | | | `/municipal/emergencias` | `GET /api/emergencias/mias` |
| Ver el detalle de una emergencia | ✔ (de su municipio) | ✔ (de su región) | | ✔ (solo API) | `/municipal/emergencias/:id` | `GET /api/emergencias/{id}` |
| Listar las emergencias de su región para desglosar | | ✔ | | | `/coordinador/emergencias` | `GET /api/emergencias/para-lotes` |
| Publicar un lote (solo emergencias de su región) | | ✔ | | | `/coordinador/emergencias/:id/lote/nuevo` | `POST /api/emergencias/{id}/lotes` |
| Listar los lotes de su región, en cualquier estado | | ✔ | | | `/coordinador/lotes` | `GET /api/lotes/mios` |
| Listar los lotes activos de todas las regiones | | | ✔ | ✔ (solo API) | `/ong/lotes` | `GET /api/lotes` |
| Ver el detalle de un lote con su emergencia | | ✔ (de su región) | ✔ | ✔ (solo API) | `/coordinador/lotes/:id`, `/ong/lotes/:id` | `GET /api/lotes/{id}` |
| Registrar, editar y eliminar ofertas de su ONG | | | ✔ | | `/ong/lotes/:id` | `POST`, `PUT`, `DELETE /api/ofertas` |
| Elegir las ONGs de una oferta conjunta y ver su stock (dentro del formulario de oferta) | | | ✔ | | `/ong/lotes/:id` | `GET /api/ongs`, `GET /api/ongs/inventario` |
| Ver el historial de versiones de una oferta | | | ✔ (si participa) | ✔ | detalle de la oferta | `GET /api/ofertas/{id}/versiones` |
| Ver todas las ofertas, incluidas las eliminadas | | | | ✔ | `/auditor/ofertas` | `GET /api/ofertas` |
| Catálogo de recursos | ✔ | ✔ | ✔ | ✔ | formulario de lote | `GET /api/recursos` |
| Ver y descartar sus notificaciones | ✔ | ✔ | ✔ | ✔ | campanita del navbar | `GET`, `DELETE /api/notificaciones` |

"Solo API" significa que el endpoint lo permite pero no hay pantalla para ese perfil.

## Reglas que valida el backend además del rol

- El municipio de una emergencia sale del usuario logueado, no del request.
- El coordinador solo ve y desglosa emergencias de los municipios de su región, y solo ve los lotes de esas emergencias.
- Al publicar un lote, la apertura de ofertas no puede ser anterior a la fecha y hora actual y el cierre tiene que ser posterior a la apertura. El formulario propone la apertura a la próxima hora en punto y el cierre dos días después.
- Un lote no puede tener el mismo recurso más de una vez. En el formulario, el recurso que se agrega aparece arriba de los ya cargados y los recursos ya elegidos no se pueden volver a elegir.

## Un lote por emergencia

Una emergencia tiene **un solo lote no cancelado**. Si el coordinador cancela el lote (por ejemplo, porque nadie ofertó) y arma otro, cada publicación conserva su propia **ventana de ofertas** (`lote.fecha_apertura_ofertas` / `lote.fecha_cierre_ofertas`); por eso las fechas viven en el lote y no en la emergencia.

La base lo garantiza con un índice único parcial (`lote(emergencia_id) WHERE estado <> 'CANCELADO'`). `OfertaService.registrar` rechaza ofertas antes de la apertura o después del cierre del lote.

## Edición y baja de ofertas

- Mientras la ventana del lote esté abierta y la oferta siga `PENDIENTE`, **cualquier ONG que participe** de la oferta puede editarla o eliminarla desde el detalle del lote (botones "Editar" y "Eliminar", este último con confirmación).
- **Editar** (`PUT /api/ofertas/{id}`) cambia solo las cantidades. Las ONGs participantes no se pueden cambiar: para eso hay que eliminar la oferta y registrar una nueva.
- **Eliminar** (`DELETE /api/ofertas/{id}`) es una **baja lógica**: la oferta pasa al estado `ELIMINADA`, conserva sus detalles y deja de aparecer en los listados.
- La oferta registra la fecha de su última modificación (`fecha_modificacion`) y su número de versión vigente (`numero_version`).
- Fuera de la ventana, con una oferta no pendiente o desde una ONG que no participa, el backend rechaza la operación (400 / 403).

## Trazabilidad de ofertas (versionado)

Cada alta, edición y baja guarda una versión inmutable (`oferta_version` + `detalle_oferta_version`) con el número, el tipo de cambio (`CREACION`, `EDICION`, `BAJA`), el estado, la fecha, el **username de Bonita** de quien lo hizo y su ONG, más una copia de todas las cantidades de ese momento.

`GET /api/ofertas/{id}/versiones` devuelve el historial (ONGs participantes y auditor). Las ONGs lo ven en la pestaña "Historial" del detalle de la oferta; el auditor ve todas las ofertas, incluidas las eliminadas, en `/auditor/ofertas` (`GET /api/ofertas`, solo auditor).
