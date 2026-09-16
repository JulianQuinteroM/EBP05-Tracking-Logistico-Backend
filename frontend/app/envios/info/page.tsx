import Link from 'next/link';
import { serverFetchPublic } from '@/lib/backend';
import { displayDate, serviceLabel, statusLabel, type PublicTracking } from '@/lib/types';

type Props = { searchParams: Promise<{ tracking?: string; codigo?: string }> };

export default async function ShipmentInfoPage({ searchParams }: Props) {
  const params = await searchParams;
  const code = (params.codigo || params.tracking || '').trim().toUpperCase();
  const result = code
    ? await serverFetchPublic<PublicTracking>(`seguimiento/${encodeURIComponent(code)}`)
    : { status: 400, data: null };
  const tracking = result.data;
  const error = !code ? 'Ingresa un código en la página inicial.'
    : result.status === 404 ? 'No existe un envío con ese código.'
      : result.status === 400 ? 'El código no tiene un formato o dígito verificador válido.'
        : 'No se pudo consultar el seguimiento. Verifica que el backend esté encendido.';

  return <main className="mx-auto max-w-4xl space-y-6 rounded-2xl bg-white p-8 text-slate-800 shadow-sm">
    <Link href="/" className="text-sm font-semibold text-purple-900 underline">← Volver a la búsqueda</Link>
    <div>
      <p className="text-sm font-semibold text-orange-600">Seguimiento público</p>
      <h1 className="mt-1 text-3xl font-bold text-purple-900">Consulta de envío</h1>
    </div>
    {!tracking ? <p role="alert" className="rounded-lg bg-red-50 p-4 text-red-700">{error}</p> : <>
      <section className="rounded-xl bg-purple-900 p-6 text-white">
        <p className="text-sm text-purple-200">Código de seguimiento</p>
        <p className="mt-1 text-2xl font-bold">{tracking.trackingCode}</p>
        <p className="mt-4 font-semibold">Estado: {statusLabel(tracking.status)}</p>
      </section>
      <div className="grid gap-5 md:grid-cols-2">
        <section className="rounded-xl border p-5">
          <h2 className="font-bold text-purple-900">Resumen</h2>
          <p className="mt-3">Servicio: {serviceLabel(tracking.serviceType)}</p>
          <p>Fecha estimada de entrega: {tracking.estimatedDeliveryDate}</p>
          <p>Última actualización: {displayDate(tracking.updatedAt)}</p>
        </section>
        <section className="rounded-xl border p-5">
          <h2 className="font-bold text-purple-900">Último movimiento</h2>
          {tracking.latestPublicEvent ? <>
            <p className="mt-3">{tracking.latestPublicEvent.description}</p>
            <p className="mt-2 text-sm text-slate-500">{displayDate(tracking.latestPublicEvent.occurredAt)}</p>
          </> : <p className="mt-3 text-slate-500">No hay movimientos públicos registrados.</p>}
        </section>
      </div>
      <p className="text-sm text-slate-500">Los datos personales y la dirección del destinatario solo se muestran al operador autorizado.</p>
    </>}
  </main>;
}
