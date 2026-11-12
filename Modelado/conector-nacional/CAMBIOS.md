# Conector de Bonita `rescuesync-nacional` — cambios para recompilar

Archivo a recompilar: `IntegracionNacionalConnector.java` (paquete `com.rescuesync.bonita`).
`IntegracionNacionalConnector.java.orig` es el original, para comparar con `diff`.

## Qué cambió (5 cambios)

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
