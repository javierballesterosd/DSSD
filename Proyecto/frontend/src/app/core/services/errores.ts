import { HttpErrorResponse } from '@angular/common/http';

/**
 * Mensaje para mostrarle al usuario ante un error HTTP. El backend responde `ErrorResponse`
 * (`mensaje` + `detalles`); si no hay respuesta del servidor (status 0) se avisa de la conexión.
 */
export function mensajeDeError(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verificá tu conexión e intentá de nuevo.';
  }

  const mensaje: string | undefined = error.error?.mensaje;
  const detalles: string[] = error.error?.detalles ?? [];

  if (mensaje && detalles.length > 0) {
    return `${mensaje}: ${detalles.join('; ')}`;
  }

  return mensaje ?? fallback;
}
