'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import InputBase from '@/app/envios/nuevo/InputBase';
import { trackingApi, type Priority } from '@/lib/tracking-api';

type PersonPrefix = 'remitente' | 'destinatario';

export default function NuevoEnvio() {
  const router = useRouter();
  const [priority, setPriority] = useState<Priority>('ESTANDAR');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const estimate = { ESTANDAR: ['3-5 días', '$10.000'], EXPRES: ['24-48h', '$25.000'], PROGRAMADO: ['Según fecha', '$15.000'] }[priority];

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    const data = new FormData(event.currentTarget);
    const value = (name: string) => String(data.get(name) || '').trim();
    try {
      const payload = {
        remitente: { nombre: value('remitenteNombre'), telefono: value('remitenteTelefono'), email: value('remitenteEmail') },
        destinatario: { nombre: value('destinatarioNombre'), telefono: value('destinatarioTelefono'), email: value('destinatarioEmail') },
        direccionOrigen: { calle: value('origenCalle'), numero: value('origenNumero'), ciudad: value('origenCiudad') },
        direccionDestino: { calle: value('destinoCalle'), numero: value('destinoNumero'), ciudad: value('destinoCiudad') },
        prioridad: priority,
        peso: Number(data.get('peso')),
        dimensiones: value('dimensiones'),
        ...(priority === 'PROGRAMADO' ? { fechaEstimadaEntrega: value('fechaProgramada') } : {}),
      };
      const shipment = await trackingApi.createShipment(payload);
      router.push(`/envios/info?tracking=${encodeURIComponent(shipment.trackingNumber)}`);
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Error al crear el envío. Intente nuevamente.');
    } finally {
      setSubmitting(false);
    }
  }

  const personFields = (prefix: PersonPrefix, title: string) => (
    <section>
      <h3 className="mb-4 text-lg font-semibold text-purple-900">{title}</h3>
      <div className="grid gap-4 md:grid-cols-3">
        <label>Nombre<InputBase required name={`${prefix}Nombre`} placeholder="Nombre completo" /></label>
        <label>Teléfono<InputBase required type="tel" name={`${prefix}Telefono`} placeholder="Teléfono" /></label>
        <label>Correo<InputBase required type="email" name={`${prefix}Email`} placeholder="Correo electrónico" /></label>
      </div>
    </section>
  );

  const addressFields = (prefix: 'origen' | 'destino', title: string) => (
    <section>
      <h3 className="mb-4 text-lg font-semibold text-purple-900">{title}</h3>
      <div className="grid gap-4 md:grid-cols-3">
        <label>Calle<InputBase required name={`${prefix}Calle`} /></label>
        <label>Número<InputBase required name={`${prefix}Numero`} /></label>
        <label>Ciudad<InputBase required name={`${prefix}Ciudad`} /></label>
      </div>
    </section>
  );

  return <div className="mx-auto max-w-3xl rounded-xl border border-gray-100 bg-white p-8 shadow-sm">
    <h2 className="mb-2 border-b pb-2 text-2xl font-bold text-gray-800">Registrar Nuevo Envío</h2>
    <p className="mb-6 text-sm text-gray-500">Completa los datos del remitente, destinatario y ambas direcciones.</p>
    {error && <div role="alert" className="mb-6 rounded border-l-4 border-red-500 bg-red-50 p-4 text-red-700">{error}</div>}
    <form onSubmit={submit} className="space-y-8">
      {personFields('remitente', 'Datos del remitente')}
      {personFields('destinatario', 'Datos del destinatario')}
      {addressFields('origen', 'Dirección de origen')}
      {addressFields('destino', 'Dirección de destino')}
      <section className="rounded-lg border border-gray-200 bg-gray-50 p-6"><h3 className="mb-4 text-lg font-semibold text-purple-900">Detalles del paquete</h3><div className="grid gap-4 md:grid-cols-2">
        <label>Peso del paquete (kg)<InputBase required min="0.1" step="0.1" type="number" name="peso" /></label>
        <label>Dimensiones<InputBase required name="dimensiones" placeholder="20x20x20" /></label>
        <label>Prioridad<select value={priority} onChange={event => setPriority(event.target.value as Priority)} className="mt-1 w-full rounded border p-2 text-gray-900"><option value="ESTANDAR">Estándar</option><option value="EXPRES">Exprés</option><option value="PROGRAMADO">Programado</option></select></label>
        {priority === 'PROGRAMADO' && <label>Fecha programada<InputBase required type="date" name="fechaProgramada" /></label>}
      </div><div className="mt-6 flex justify-around rounded border border-purple-100 bg-white p-4 text-center"><div><span className="block text-sm text-gray-500">Tiempo estimado</span><b className="text-lg text-purple-900">{estimate[0]}</b></div><div className="border-l" /><div><span className="block text-sm text-gray-500">Costo informativo</span><b className="text-lg text-orange-600">{estimate[1]}</b></div></div></section>
      <div className="flex justify-end border-t pt-4"><button disabled={submitting} className="rounded-lg bg-orange-500 px-8 py-3 font-bold text-white disabled:bg-orange-300">{submitting ? 'Guardando...' : 'Guardar envío'}</button></div>
    </form>
  </div>;
}
