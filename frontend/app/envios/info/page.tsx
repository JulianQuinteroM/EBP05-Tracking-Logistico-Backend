import Link from 'next/link';
import { serverFetchPublic } from '@/lib/backend';
import { displayDate, serviceLabel, statusLabel, statusTone, type PublicTracking } from '@/lib/types';

type Props = { searchParams: Promise<{ tracking?: string; codigo?: string }> };
export default async function ShipmentInfoPage({ searchParams }: Props) {
  const params = await searchParams;
  const code = (params.codigo || params.tracking || '').trim().toUpperCase();
  const result = code ? await serverFetchPublic<PublicTracking>(`seguimiento/${encodeURIComponent(code)}`) : { status: 400, data: null };
  const tracking = result.data;
  const error = !code ? 'Ingresa un código en la página inicial.' : result.status === 404 ? 'No existe un envío con ese código.'
    : result.status === 429 ? 'Se alcanzó el límite de búsquedas fallidas. Intenta de nuevo en 15 minutos.'
      : result.status === 400 ? 'El código no tiene un formato o dígito verificador válido.'
        : 'No se pudo consultar el seguimiento. Comprueba que el backend esté encendido.';
  return <main className="mx-auto max-w-5xl space-y-6">
    <Link href="/" className="text-sm text-blue-300 hover:underline">← Volver a la búsqueda</Link>
    <div><p className="text-xs font-bold uppercase tracking-widest text-blue-300">Seguimiento público</p><h1 className="mt-2 text-3xl font-bold text-white">Seguimiento del envío</h1></div>
    {!tracking ? <p role="alert" className="app-alert">{error}</p> : <>
      <section className="app-panel flex flex-wrap items-center justify-between gap-4 bg-gradient-to-r from-[#1a2a49] to-[#123b6b] p-6"><div><p className="text-xs text-slate-400">Código de seguimiento</p><h2 className="mt-1 text-2xl font-bold text-white">{tracking.trackingCode}</h2></div><span className={`status-pill status-${statusTone(tracking.status)}`}>{statusLabel(tracking.status)}</span></section>
      <div className="grid gap-5 md:grid-cols-2">
        <Card title="Estado actual"><p className="text-lg font-semibold text-white">{statusLabel(tracking.status)}</p><p>Actualizado: {displayDate(tracking.updatedAt)}</p><p>Ubicación actual: {tracking.currentCity ?? 'Aún sin ubicación registrada'}</p></Card>
        <Card title="Resumen seguro"><p>Remitente: {tracking.maskedSenderName}</p><p>Destinatario: {tracking.maskedRecipientName}</p><p>Servicio: {serviceLabel(tracking.serviceType)}</p><p>Entrega estimada: {tracking.estimatedDeliveryStart === tracking.estimatedDeliveryDate ? tracking.estimatedDeliveryDate : `${tracking.estimatedDeliveryStart} a ${tracking.estimatedDeliveryDate}`}</p></Card>
      </div>
      {tracking.status === 'INCIDENCIA' && <p className="app-alert">El envío presenta una incidencia. La operación está revisando el caso; la fecha estimada puede cambiar.</p>}
      {tracking.status === 'ENTREGADO' && <p className="rounded-lg border border-green-700 bg-green-900/30 p-4 text-green-200">Entrega completada.</p>}
      {tracking.status === 'CANCELADO' && <p className="app-alert">Este envío fue cancelado.</p>}
      <Card title="Próximos pasos del envío">
        {tracking.journey.length ? <ol className="space-y-3">{tracking.journey.map((step, index) => <li key={`${step.status}-${index}`} className="flex items-center gap-3"><span className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${step.phase === 'COMPLETADO' ? 'bg-green-700 text-white' : step.phase === 'ACTUAL' ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300'}`}>{step.phase === 'COMPLETADO' ? '✓' : index + 1}</span><span className={step.phase === 'ACTUAL' ? 'font-bold text-white' : ''}>{statusLabel(step.status)} <small className="ml-2 text-xs text-slate-400">{step.phase.toLowerCase()}</small></span></li>)}</ol>
          : <p>La secuencia regular finalizó.</p>}
      </Card>
      <Card title="Último movimiento">{tracking.latestPublicEvent ? <><p className="text-white">{tracking.latestPublicEvent.description}</p><p>{tracking.latestPublicEvent.city ?? 'Ubicación no registrada'} · {displayDate(tracking.latestPublicEvent.occurredAt)}</p></> : <p>No hay movimientos públicos registrados.</p>}</Card>
      <p className="text-xs text-slate-500">La consulta pública oculta teléfonos, correos y dirección completa.</p>
    </>}
  </main>;
}
function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="app-panel p-6"><h2 className="mb-4 border-b border-slate-700 pb-3 font-bold text-blue-200">{title}</h2><div className="space-y-2 text-sm text-slate-300">{children}</div></section>;
}
