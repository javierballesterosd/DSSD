export interface Ong {
  id: number;
  razonSocial: string;
}

export interface DisponibilidadRecurso {
  recursoId: number;
  recursoNombre: string;
  unidadMedida: string;
  cantidadDisponible: number;
}

export interface InventarioOng {
  ongId: number;
  razonSocial: string;
  recursos: DisponibilidadRecurso[];
}
