export type Priority = "ESTANDAR" | "EXPRES" | "PROGRAMADO";
export type ShipmentStatus = "PENDIENTE_RECOGIDA" | "RECOGIDO" | "CANCELADO" | "EN_TRANSITO" | "ENTREGADO";

export type Address = {
  id?: number;
  calle?: string;
  numero?: string;
  ciudad?: string;
  departamentoProvincia?: string;
  codigoPostal?: string;
  pais?: string;
};

export type ShipmentAddressInput = {
  calle: string;
  numero: string;
  ciudad: string;
  departamentoProvincia?: string;
  codigoPostal?: string;
  pais?: string;
};

export type Movement = {
  id?: number;
  fechaHora: string;
  estado: ShipmentStatus;
  ubicacion?: string;
  descripcion?: string;
};

export type Shipment = {
  id: number;
  trackingNumber: string;
  prioridad: Priority;
  peso: number;
  dimensiones: string;
  costo: number;
  fechaRegistro: string;
  fechaEstimadaEntrega: string;
  estadoOperativo: ShipmentStatus;
  idRemitente: number;
  nombreRemitente: string;
  telefonoRemitente?: string;
  emailRemitente?: string;
  idDestinatario: number;
  nombreDestinatario: string;
  telefonoDestinatario?: string;
  emailDestinatario?: string;
  direccionOrigen?: Address;
  direccionDestino?: Address;
  historialMovimientos?: Movement[];
  movimientos?: Movement[];
  ubicacionActual?: string | { ciudad?: string; descripcion?: string };
};

export type PaginatedShipments = {
  contenido: Shipment[];
  numeroPagina: number;
  tamanoPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export type CreateShipment = {
  remitente: { nombre: string; telefono: string; email: string };
  destinatario: { nombre: string; telefono: string; email: string };
  direccionOrigen: ShipmentAddressInput;
  direccionDestino: ShipmentAddressInput;
  prioridad: Priority;
  peso: number;
  dimensiones: string;
  fechaEstimadaEntrega?: string;
};

export type UpdateShipment = {
  idRemitente?: number;
  idDestinatario?: number;
  idDireccionOrigen?: number;
  idDireccionDestino?: number;
  prioridad?: Priority;
  peso?: number;
  dimensiones?: string;
};

const backendUrl = (process.env.NEXT_PUBLIC_TRACKING_API_URL || "http://localhost:8080").replace(/\/$/, "");

function apiUrl(path: string) {
  return typeof window === "undefined"
    ? `${backendUrl}${path}`
    : path.replace(/^\/api(?=\/|$)/, "/tracking-api");
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(apiUrl(path), {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!response.ok) {
    let message = `La solicitud falló (${response.status})`;
    try {
      const body = await response.json() as { message?: string; error?: string };
      message = body.message || body.error || message;
    } catch { }
    throw new Error(message);
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export const trackingApi = {
  createShipment: (payload: CreateShipment) => request<Shipment>("/api/envios?operadorId=1", { method: "POST", body: JSON.stringify(payload) }),
  getByTracking: (tracking: string) => request<Shipment>(`/api/envios/tracking/${encodeURIComponent(tracking)}`),
  getById: (id: number) => request<Shipment>(`/api/envios/${id}`),
  getMovements: (id: number) => request<Movement[]>(`/api/envios/${id}/movimientos`),
  listShipments: (params: URLSearchParams) => request<PaginatedShipments>(`/api/envios?${params.toString()}`),
  updateShipment: (id: number, payload: UpdateShipment) => request<Shipment>(`/api/envios/${id}?usuario=Admin`, { method: "PUT", body: JSON.stringify(payload) }),
  cancelShipment: (id: number) => request<void>(`/api/envios/${id}?usuario=Admin`, { method: "DELETE" }),
  exportShipments: (params: URLSearchParams) => fetch(`${apiUrl("/api/envios/export")}?${params.toString()}`).then(async response => {
    if (!response.ok) throw new Error(`No se pudo exportar (${response.status})`);
    return response.blob();
  }),
};

export function priorityLabel(value: Priority) {
  return { ESTANDAR: "Estándar", EXPRES: "Exprés", PROGRAMADO: "Programado" }[value];
}

export function statusLabel(value: ShipmentStatus) {
  return { PENDIENTE_RECOGIDA: "Pendiente de recogida", RECOGIDO: "Recogido", CANCELADO: "Cancelado", EN_TRANSITO: "En tránsito", ENTREGADO: "Entregado" }[value];
}

export function addressLabel(address?: Address) {
  if (!address) return "No disponible";
  return [address.calle, address.numero, address.ciudad].filter(Boolean).join(" ") || "No disponible";
}
