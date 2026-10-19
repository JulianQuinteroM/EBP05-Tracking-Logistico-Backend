import Link from 'next/link';
import { redirect } from 'next/navigation';
import { pageSession } from '@/lib/session';
import { serverFetchPrivate } from '@/lib/backend';
import { displayDate, displayNumber, PACKAGING_LABELS, serviceLabel, statusLabel, statusTone, type ShipmentDetail } from '@/lib/types';
import ShipmentActions from './ShipmentActions';

export default async function ShipmentDetailsPage({ searchParams }: { searchParams: Promise<{ id?: string; accion?: string; aviso?: string }> }) {
  const { id, accion, aviso } = await searchParams;
  const session = await pageSession();
  if (!session) redirect(`/operador/ingresar?next=${encodeURIComponent(id && /^\d+$/.test(id) ? `/envios/detalles?id=${id}` : '/historial-operativo')}`);
  if (!id || !/^\d+$/.test(id)) return <p role="alert" className="app-alert">Falta un ID válido. <Link href="/historial-operativo" className="underline">Volver a la lista</Link></p>;
  const result = await serverFetchPrivate<ShipmentDetail>(`envios/${id}`);
  const detail = result.data;
  if (!detail) return <p role="alert" className="app-alert">{result.status === 404 ? 'No se encontró el envío.' : 'No se pudo consultar la ficha. Comprueba que el backend esté activo.'} <Link href="/historial-operativo" className="underline">Volver a la lista</Link></p>;
  const shipment = detail.shipment;
  const initialAction = ['editar', 'cancelar', 'estado', 'ubicacion'].includes(accion ?? '') ? accion as 'editar' | 'cancelar' | 'estado' | 'ubicacion' : null;
  return <main className="mx-auto max-w-6xl space-y-6">
    <Link href="/historial-operativo" className="text-sm text-blue-300 hover:underline">← Volver a la lista</Link>
    <section className="app-panel flex flex-wrap items-center justify-between gap-4 p-6"><div><p className="text-xs font-bold uppercase tracking-widest text-blue-300">Detalles del envío</p><h1 className="mt-1 text-2xl font-bold text-white">{detail.trackingCode}</h1><p className="mt-2 text-sm text-slate-400">Registrado: {displayDate(detail.createdAt)} · ID interno: {detail.id}</p></div><span className={`status-pill status-${statusTone(detail.status)}`}>{statusLabel(detail.status)}</span></section>
    {aviso && <p role="status" className="rounded-lg border border-green-800 bg-green-900/30 p-3 text-green-200">{aviso}</p>}
    <div className="grid gap-5 md:grid-cols-2">
      <Card title="Remitente"><p>{shipment.senderName}</p><p>{shipment.senderPhone}</p><p>{shipment.senderEmail}</p></Card>
      <Card title="Destinatario"><p>{shipment.recipientName}</p><p>{shipment.recipientPhone}</p><p>{shipment.recipientEmail}</p></Card>
      <Card title="Dirección y ubicación"><p>Destino: {shipment.destinationStreet} {shipment.destinationNumber}, {shipment.destinationCity}</p><p>Ubicación actual: <strong className="text-white">{detail.currentCity ?? 'Aún sin ubicación registrada'}</strong></p></Card>
      <Card title="Paquete y servicio"><p>Peso: {displayNumber(shipment.weightKg)} kg</p><p>Dimensiones: {displayNumber(shipment.lengthCm)} × {displayNumber(shipment.widthCm)} × {displayNumber(shipment.heightCm)} cm</p><p>Empaque: {PACKAGING_LABELS[shipment.packagingType]} {shipment.packagingDescription ? `— ${shipment.packagingDescription}` : ''}</p><p>Servicio: {serviceLabel(shipment.serviceType)}</p>{shipment.scheduledDeliveryDate && <p>Entrega programada: {shipment.scheduledDeliveryDate}</p>}<p>Entrega estimada: {detail.estimatedDeliveryDate}</p><p>Costo informativo: {new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(detail.estimatedCostCop)}</p></Card>
    </div>
    <Card title="Historial de movimientos">{detail.events.length ? <ol className="space-y-3">{detail.events.map((event, index) => <li key={`${event.occurredAt}-${index}`} className="border-l-2 border-blue-500 pl-4"><p className="font-semibold text-white">{event.description}</p><p>{event.previousStatus ? `${statusLabel(event.previousStatus)} → ` : ''}{statusLabel(event.resultingStatus)}{event.city ? ` · ${event.city}` : ''}{event.reference ? ` · ${event.reference}` : ''}</p><p className="text-xs text-slate-500">{displayDate(event.occurredAt)}{event.actor ? ` · ${event.actor}` : ''}</p></li>)}</ol> : <p>No hay movimientos.</p>}</Card>
    <ShipmentActions key={`${detail.id}-${detail.version}`} detail={detail} initialAction={initialAction} canWrite={session.role !== 'viewer'} />
  </main>;
}
function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="app-panel p-6"><h2 className="mb-4 border-b border-slate-700 pb-3 font-bold text-blue-200">{title}</h2><div className="space-y-2 text-sm text-slate-300">{children}</div></section>;
}
