
export interface DescriptorAudiencia {
  grupoDestinatario: string;
}

export interface Notificacion {
  id: number,
  titulo: string,
  descripcion: string,
  fechaCreacion: string,
  remitenteUsername?: string | null,
  audiencia: DescriptorAudiencia,
}
