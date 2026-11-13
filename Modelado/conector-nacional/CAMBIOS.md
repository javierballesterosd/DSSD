# Conector de Bonita `rescuesync-nacional` — cambios para recompilar

Archivo a recompilar: `IntegracionNacionalConnector.java` (paquete `com.rescuesync.bonita`).
`IntegracionNacionalConnector.java.orig` es el original, para comparar con `diff`.

## Versión 1.0.5 (`rescueSync-V1.16.bos`): reserva parcial y gateway de "sin recursos"

Requiere el SN con la rama `feature/reserva-temporal-parcial` (campo `parcial` en `POST /reservas/temporales`).

- **Reserva parcial:** en `VALIDAR` el conector manda `parcial: true`. El SN reserva lo que haya libre de cada recurso y solo responde 409 si no puede reservar nada; en ese caso la oferta queda rechazada como antes.
- **Salida nueva `hayOfertasHabilitadas` (Boolean):** `true` si al menos una oferta quedó con recursos reservados (o si no había ofertas pendientes). Para `ACEPTAR` y `RECHAZAR` queda en `true` y no se usa.
- **Proceso:** variable `hayOfertasHabilitadas` (default `true`), operación en el conector de `Validar en el Sistema Nacional (POST Cloud)` y gateway nuevo `Quedan ofertas con recursos?` antes de `Ver ofertas validadas`: `si` va a `Ver ofertas validadas`, `no` va a `Registrar emergencia`.
- **Versión:** definición, implementación, jar (`.store/.../1.0.5`), `app/pom.xml` y `.conf` pasan de 1.0.4 a 1.0.5.

Pendiente (depende del backend local, rama `feature/bloqueo-recursos`): en el camino `no` el backend todavía no cancela el lote, no marca `requiereActualizacion` ni notifica al municipio, así que `actualizar` no deja completar `Registrar emergencia` en ese caso. Ante un reintento del conector tras un fallo a mitad de la validación, `hayOfertasHabilitadas` solo cuenta las ofertas que seguían pendientes.

El `.bos` V1.16 se armó editando el XML del V1.15: al abrirlo en Studio conviene revisar el dibujo del gateway y sus flechas (las aristas nuevas no tienen puntos de quiebre) y volver a exportarlo.

## Qué cambió en 1.0.4 (5 cambios)

| Línea | Antes | Ahora | Por qué |
| --- | --- | --- | --- |
| 72 | `POST {SN}/reservas/temporal` | `POST {SN}/reservas/temporales` | El SN renombró la ruta (commit `fbd7734`). Con la vieja da 404 |
| 96 | `POST {SN}/reservas/definitiva` y `POST {SN}/reservas/liberacion` | `POST {SN}/reservas/definitivas` y `POST {SN}/reservas/liberaciones` | Ídem (commit `15e8570`); `liberacion` también se pasó a plural para seguir la misma convención |
| 148 | `item.get("ongId")` | `item.get("ongCodigo")` | Las ONGs ahora se identifican por su `codigo` (UUID) común a todos los sistemas, no por el id numérico de cada base |
| 157 | `"ongId", i.get("ongId")` | `"ongCodigo", i.get("ongCodigo")` | Ídem: con la clave vieja el valor es `null` y `Map.of` lanza NPE |

`/login`, `GET /reservas` y las rutas del backend local (`/api/emergencias/{id}/integracion-nacional`, `/validacion-sn`, `/decision-municipal/confirmacion`) **no cambian**.

## Contrato que espera ahora

- Plan local (`GET /api/emergencias/{id}/integracion-nacional`): cada ítem trae `ongCodigo` (String) en lugar de `ongId`.
- Respuestas del SN (`/reservas/...`): cada ítem de `items` trae `ongCodigo` en lugar de `ongId`.
- Callbacks al backend local (`/validacion-sn`, `/decision-municipal/confirmacion`): cada ítem se envía con `ongCodigo`.

Estos cambios están en las ramas `feature/id-comun-ong` de `Proyecto` y de `API-Nacional`. **Los tres componentes (conector, backend local, SN) tienen que desplegarse juntos**: con versiones mezcladas la validación falla.

## Pasos

1. Recompilar el conector con el flujo habitual del equipo.
2. Reemplazar el jar en el proyecto de Bonita (`app/.store/com/rescuesync/rescuesync-nacional/<versión>/`). Hoy el `.bos` trae la 1.0.3; si se sube la versión, actualizar también el `.def` y el `.impl`.
3. Reimportar o redeployar el conector en Studio y volver a exportar el `.bos`.
4. Probar un ciclo completo: validar ofertas, decisión municipal (aceptar y rechazar) y confirmar que el callback llega al backend local.
