export type ServiceType = 'ESTANDAR' | 'EXPRES' | 'PROGRAMADO';
export type ShipmentStatus = 'PENDIENTE_RECOGIDA' | 'CANCELADO';

export type ShipmentInput = {
  senderName: string;
  senderPhone: string;
  senderEmail: string;
  recipientName: string;
  recipientPhone: string;
  recipientEmail: string;
  destinationStreet: string;
  destinationNumber: string;
  destinationCity: string;
  weightKg: number;
  serviceType: ServiceType;
  scheduledDeliveryDate: string | null;
};

export type ShipmentEvent = {
  type: string;
  resultingStatus: ShipmentStatus;
  occurredAt: string;
  description: string;
};

export type ShipmentDetail = {
  id: number;
  trackingCode: string;
  status: ShipmentStatus;
  version: number;
  shipment: ShipmentInput;
  estimatedCostCop: number;
  estimatedDeliveryDate: string;
  createdAt: string;
  updatedAt: string;
  events: ShipmentEvent[];
};

export type ShipmentListItem = Pick<ShipmentDetail, 'id' | 'trackingCode' | 'status' | 'version' | 'createdAt'> & {
  senderName: string;
  recipientName: string;
  serviceType: ServiceType;
};

export type ShipmentListPage = {
  content: ShipmentListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type PublicTracking = {
  trackingCode: string;
  status: ShipmentStatus;
  serviceType: ServiceType;
  updatedAt: string;
  estimatedDeliveryDate: string;
  latestPublicEvent: ShipmentEvent | null;
};

export type Estimate = { costCop: number; deliveryDate: string; deliveryWindow: string };

export function statusLabel(status: ShipmentStatus) {
  return status === 'PENDIENTE_RECOGIDA' ? 'Pendiente de recogida' : 'Cancelado';
}

export function serviceLabel(service: ServiceType) {
  return { ESTANDAR: 'Estándar', EXPRES: 'Exprés', PROGRAMADO: 'Programado' }[service];
}

export function displayDate(value: string) {
  return new Date(value).toLocaleString('es-CO', { timeZone: 'America/Bogota' });
}
