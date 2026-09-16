import Link from 'next/link';
import { redirect } from 'next/navigation';
import { pageHasSession } from '@/lib/session';
import { serverFetchPrivate } from '@/lib/backend';
import { displayDate, serviceLabel, statusLabel, type ShipmentDetail } from '@/lib/types';
import ShipmentActions from './ShipmentActions';

export default async function ShipmentDetailsPage({ searchParams }: {
  searchParams: Promise<{ id?: string; accion?: string }>;
}) {
  const { id, accion } = await searchParams;
  if (!(await pageHasSession())) {
    const next = id && /^\d+$/.test(id) ? `/envios/detalles?id=${id}` : '/historial-operativo';
    redirect(`/operador/ingresar?next=${encodeURIComponent(next)}`);
  }
  if (!id || !/^\d+$/.test(id)) {
    return <p role="alert" className="rounded bg-red-50 p-4 text-red-700">Falta un ID de envío válido. <Link href="/historial-operativo" className="underline">Volver a la lista</Link></p>;
  }
  const result = await serverFetchPrivate<ShipmentDetail>(`envios/${id}`);
  const detail = result.data;
  if (!detail) {
    return <p role="alert" className="rounded bg-red-50 p-4 text-red-700">{result.status === 404 ? 'No se encontró el envío.' : 'No se pudo consultar la ficha. Comprueba que el backend esté activo y configurado.'} <Link href="/historial-operativo" className="underline">Volver a la lista</Link></p>;
  }
  const shipment = detail.shipment;

  return <main className="mx-auto max-w-5xl space-y-6 text-gray-900">
    <Link href="/historial-operativo" className="text-sm font-semibold text-purple-900 underline">← Volver a la lista</Link>
    <section className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-white p-6 shadow-sm">
      <div>
        <h1 className="text-2xl font-bold text-purple-900">Detalles del envío {detail.trackingCode}</h1>
        <p className="mt-1 text-sm text-gray-600">Registrado: {displayDate(detail.createdAt)} · ID interno: {detail.id}</p>
      </div>
      <span className="rounded-full bg-orange-100 px-4 py-2 font-semibold text-orange-800">{statusLabel(detail.status)}</span>
    </section>

    <div className="grid gap-5 md:grid-cols-2">
      <Card title="Remitente">
        <p>{shipment.senderName}</p><p>{shipment.senderPhone}</p><p>{shipment.senderEmail}</p>
      </Card>
      <Card title="Destinatario">
        <p>{shipment.recipientName}</p><p>{shipment.recipientPhone}</p><p>{shipment.recipientEmail}</p>
      </Card>
      <Card title="Dirección de destino">
        <p>{shipment.destinationStreet} {shipment.destinationNumber}, {shipment.destinationCity}</p>
      </Card>
      <Card title="Paquete y servicio">
        <p>Peso: {shipment.weightKg} kg</p>
        <p>Servicio: {serviceLabel(shipment.serviceType)}</p>
        {shipment.scheduledDeliveryDate && <p>Entrega programada: {shipment.scheduledDeliveryDate}</p>}
        <p>Fecha estimada: {detail.estimatedDeliveryDate}</p>
        <p>Costo informativo: {new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(detail.estimatedCostCop)}</p>
      </Card>
    </div>
    <Card title="Historial de movimientos">
      {detail.events.length ? <ol className="space-y-3">{detail.events.map((event, index) => <li key={`${event.occurredAt}-${index}`} className="border-l-2 border-purple-400 pl-3"><p className="font-medium">{event.description}</p><p className="text-sm text-gray-500">{displayDate(event.occurredAt)}</p></li>)}</ol> : <p>No hay movimientos registrados.</p>}
    </Card>
    <ShipmentActions key={`${detail.id}-${detail.version}`} detail={detail} initialAction={accion === 'editar' || accion === 'cancelar' ? accion : null} />
    <p className="text-xs text-gray-500">Sprint 1 no registra dimensiones ni dirección de origen; por eso no se muestran aquí.</p>
  </main>;
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm"><h2 className="mb-3 font-bold text-purple-900">{title}</h2><div className="space-y-1 text-sm text-gray-700">{children}</div></section>;
}
