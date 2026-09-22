import Link from 'next/link';
import { addressLabel, priorityLabel, statusLabel, trackingApi } from '@/lib/tracking-api';

type Props = { searchParams: Promise<{ tracking?: string; codigo?: string }> };

export default async function ShipmentInfoPage({ searchParams }: Props) {
  const params = await searchParams;
  const tracking = params.tracking ?? params.codigo ?? '';
  let shipment;
  let error = '';
  if (tracking) {
    try { shipment = await trackingApi.getByTracking(tracking); }
    catch (requestError) { error = requestError instanceof Error ? requestError.message : 'No se encontró ningún envío con el código ingresado. Verifica que sea correcto.'; }
  } else error = 'Ingresa un código de seguimiento para consultar el envío.';

  const movements = shipment?.historialMovimientos ?? shipment?.movimientos ?? [];
  const location = typeof shipment?.ubicacionActual === 'string' ? shipment.ubicacionActual : shipment?.ubicacionActual?.descripcion || shipment?.ubicacionActual?.ciudad;
  return <main className="mx-auto min-h-screen max-w-6xl bg-[#f7f9fc] px-6 py-10 text-slate-800">
    <Link href="/" className="text-sm font-semibold text-[#123b67]">← Volver a envíos</Link>
    <p className="mb-2 mt-8 text-sm font-semibold text-[#ef8b27]">Seguimiento de envío</p><h1 className="text-3xl font-bold text-[#123b67]">Detalle del envío</h1>
    {error ? <div role="alert" className="mt-8 rounded-xl border border-red-200 bg-red-50 p-6 text-red-700">{error}</div> : shipment && <>
      <section className="my-8 flex flex-col justify-between gap-4 rounded-2xl bg-[#123b67] p-6 text-white sm:flex-row sm:items-center"><div><p className="text-sm text-blue-100">Código de seguimiento</p><p className="mt-1 text-2xl font-bold">{shipment.trackingNumber}</p></div><b>{statusLabel(shipment.estadoOperativo)}</b></section>
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <section className="rounded-2xl bg-white p-6 shadow-sm"><h2 className="mb-6 text-lg font-bold text-[#123b67]">Historial de movimientos</h2>{movements.length ? <div className="space-y-5">{movements.map((movement, index) => <div className="flex gap-4" key={movement.id ?? movement.fechaHora}><div className="flex flex-col items-center"><span className={`mt-1 h-4 w-4 rounded-full border-4 ${index === 0 ? 'border-orange-200 bg-[#ef8b27]' : 'border-slate-200 bg-slate-400'}`} />{index < movements.length - 1 && <span className="h-full w-px bg-slate-200" />}</div><div><p className="text-xs text-slate-400">{new Date(movement.fechaHora).toLocaleString('es-CO')}</p><p className="font-semibold">{statusLabel(movement.estado)}</p><p className="text-sm text-slate-500">{movement.descripcion || movement.ubicacion || 'Sin descripción'}</p></div></div>)}</div> : <p className="text-sm text-slate-500">No hay movimientos registrados.</p>}</section>
        <div className="space-y-6"><InfoCard title="Remitente" lines={[shipment.nombreRemitente, shipment.telefonoRemitente, shipment.emailRemitente]} /><InfoCard title="Destinatario" lines={[shipment.nombreDestinatario, shipment.telefonoDestinatario, shipment.emailDestinatario]} /><InfoCard title="Direcciones" lines={[`Origen: ${addressLabel(shipment.direccionOrigen)}`, `Destino: ${addressLabel(shipment.direccionDestino)}`]} /><InfoCard title="Ubicación actual" lines={[location || 'No disponible']} /></div>
      </div>
      <section className="mt-6 rounded-2xl bg-[#123b67] p-6 text-white"><h2 className="mb-4 text-sm font-bold uppercase text-blue-100">Información operativa</h2><div className="grid grid-cols-2 gap-5 md:grid-cols-5"><Metric title="Peso" value={`${shipment.peso} kg`} /><Metric title="Dimensiones" value={shipment.dimensiones} /><Metric title="Prioridad" value={priorityLabel(shipment.prioridad)} /><Metric title="Costo" value={`$${shipment.costo}`} /><Metric title="Entrega estimada" value={new Date(shipment.fechaEstimadaEntrega).toLocaleDateString('es-CO')} /></div></section>
    </>}
  </main>;
}

function InfoCard({ title, lines }: { title: string; lines: (string | undefined)[] }) { return <section className="rounded-2xl bg-white p-6 shadow-sm"><h2 className="mb-3 text-lg font-bold text-[#123b67]">{title}</h2>{lines.filter(Boolean).map(line => <p className="text-sm text-slate-600" key={line}>{line}</p>)}</section>; }
function Metric({ title, value }: { title: string; value: string }) { return <div><p className="text-xs uppercase text-blue-200">{title}</p><p className="font-bold">{value}</p></div>; }
