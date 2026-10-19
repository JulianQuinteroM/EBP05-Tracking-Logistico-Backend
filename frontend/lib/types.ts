export type ServiceType = 'ESTANDAR' | 'EXPRES' | 'PROGRAMADO';
export type PackagingType = 'SOBRE' | 'CAJA' | 'BOLSA' | 'TUBO' | 'ESTIBA' | 'OTRO';
export type ShipmentStatus = 'PENDIENTE_RECOGIDA' | 'RECOGIDO' | 'EN_TRANSITO' | 'CENTRO_DISTRIBUCION' | 'RUTA_ENTREGA' | 'ENTREGADO' | 'INCIDENCIA' | 'CANCELADO';

export type ShipmentInput = {
  senderName: string; senderPhone: string; senderEmail: string;
  recipientName: string; recipientPhone: string; recipientEmail: string;
  destinationStreet: string; destinationNumber: string; destinationCity: string;
  weightKg: number; lengthCm: number; widthCm: number; heightCm: number;
  packagingType: PackagingType; packagingDescription: string | null;
  serviceType: ServiceType; scheduledDeliveryDate: string | null;
};
export type ShipmentEvent = {
  type: string; previousStatus?: ShipmentStatus | null; resultingStatus: ShipmentStatus;
  city?: string | null; reference?: string | null; actor?: string | null;
  occurredAt: string; description: string;
};
export type ShipmentDetail = {
  id: number; trackingCode: string; status: ShipmentStatus; version: number;
  shipment: ShipmentInput; estimatedCostCop: number; estimatedDeliveryDate: string;
  createdAt: string; updatedAt: string; currentCity: string | null;
  allowedStatusTransitions: ShipmentStatus[]; events: ShipmentEvent[];
};
export type ShipmentListItem = Pick<ShipmentDetail, 'id' | 'trackingCode' | 'status' | 'version' | 'createdAt'> & {
  senderName: string; recipientName: string; serviceType: ServiceType;
};
export type ShipmentListPage = {
  content: ShipmentListItem[]; page: number; size: number; totalElements: number; totalPages: number;
};
export type PublicTracking = {
  trackingCode: string; status: ShipmentStatus; serviceType: ServiceType;
  updatedAt: string; estimatedDeliveryStart: string; estimatedDeliveryDate: string;
  currentCity: string | null; maskedSenderName: string; maskedRecipientName: string;
  journey: { status: ShipmentStatus; phase: 'COMPLETADO' | 'ACTUAL' | 'PENDIENTE' }[];
  latestPublicEvent: ShipmentEvent | null;
};
export type AuditRecord = {
  id: number; shipmentId: number | null; action: string; actor: string;
  field: string | null; beforeValue: string | null; afterValue: string | null;
  ipAddress: string | null; occurredAt: string; detail: string | null;
};
export type Estimate = { costCop: number; deliveryDate: string; deliveryWindow: string };

const STATUS_LABELS: Record<ShipmentStatus, string> = {
  PENDIENTE_RECOGIDA: 'Pendiente de recogida', RECOGIDO: 'Recogido',
  EN_TRANSITO: 'En tránsito', CENTRO_DISTRIBUCION: 'En centro de distribución',
  RUTA_ENTREGA: 'En ruta de entrega', ENTREGADO: 'Entregado',
  INCIDENCIA: 'Incidencia', CANCELADO: 'Cancelado',
};
export const PACKAGING_LABELS: Record<PackagingType, string> = {
  SOBRE: 'Sobre', CAJA: 'Caja', BOLSA: 'Bolsa', TUBO: 'Tubo',
  ESTIBA: 'Estiba / Pallet', OTRO: 'Otro',
};
export function statusLabel(status: ShipmentStatus) { return STATUS_LABELS[status] ?? status; }
export function statusTone(status: ShipmentStatus) {
  return ({ PENDIENTE_RECOGIDA: 'neutral', RECOGIDO: 'blue', EN_TRANSITO: 'yellow',
    CENTRO_DISTRIBUCION: 'orange', RUTA_ENTREGA: 'purple', ENTREGADO: 'green',
    INCIDENCIA: 'red', CANCELADO: 'neutral' } as const)[status];
}
export function serviceLabel(service: ServiceType) {
  return { ESTANDAR: 'Estándar', EXPRES: 'Exprés', PROGRAMADO: 'Programado' }[service];
}
export function displayDate(value: string) {
  return new Date(value).toLocaleString('es-CO', { timeZone: 'America/Bogota' });
}
export function displayNumber(value: number) {
  return new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(value);
}
