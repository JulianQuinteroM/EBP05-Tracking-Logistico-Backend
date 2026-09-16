'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import InputBase from '@/app/envios/nuevo/InputBase';
import type { ServiceType, ShipmentDetail, ShipmentInput } from '@/lib/types';

const fields: { key: keyof ShipmentInput; label: string; type: string }[] = [
  { key: 'senderName', label: 'Nombre remitente', type: 'text' },
  { key: 'senderPhone', label: 'Teléfono remitente', type: 'tel' },
  { key: 'senderEmail', label: 'Correo remitente', type: 'email' },
  { key: 'recipientName', label: 'Nombre destinatario', type: 'text' },
  { key: 'recipientPhone', label: 'Teléfono destinatario', type: 'tel' },
  { key: 'recipientEmail', label: 'Correo destinatario', type: 'email' },
  { key: 'destinationStreet', label: 'Calle destino', type: 'text' },
  { key: 'destinationNumber', label: 'Número destino', type: 'text' },
  { key: 'destinationCity', label: 'Ciudad destino', type: 'text' },
];

export default function ShipmentActions({ detail, initialAction }: { detail: ShipmentDetail; initialAction: 'editar' | 'cancelar' | null }) {
  const router = useRouter();
  const [action, setAction] = useState<'editar' | 'cancelar' | null>(initialAction);
  const [form, setForm] = useState<ShipmentInput>(detail.shipment);
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [pending, setPending] = useState(false);

  function change<K extends keyof ShipmentInput>(key: K, value: ShipmentInput[K]) {
    setForm(current => ({ ...current, [key]: value }));
  }

  async function update(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(''); setMessage(''); setPending(true);
    try {
      const response = await fetch(`/api/envios/${detail.id}`, {
        method: 'PUT', headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ version: detail.version, shipment: form }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(response.status === 409 ? 'La ficha cambió. Recárgala antes de editar.' : data.message || 'No se pudo editar');
      setMessage('Envío actualizado correctamente.');
      setAction(null);
      router.replace(`/envios/detalles?id=${detail.id}`);
      router.refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo editar');
    } finally { setPending(false); }
  }

  async function cancel(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!reason.trim()) { setError('Indica el motivo de cancelación.'); return; }
    if (!window.confirm(`¿Confirmas la cancelación de ${detail.trackingCode}? El envío seguirá en el historial.`)) return;
    setError(''); setMessage(''); setPending(true);
    try {
      const response = await fetch(`/api/envios/${detail.id}/cancelacion`, {
        method: 'POST', headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ version: detail.version, reason: reason.trim() }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(response.status === 409 ? 'La ficha cambió o ya no puede cancelarse. Recárgala.' : data.message || 'No se pudo cancelar');
      setMessage('Envío cancelado. Permanece en el historial.');
      setAction(null);
      router.replace(`/envios/detalles?id=${detail.id}`);
      router.refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cancelar');
    } finally { setPending(false); }
  }

  return <section className="rounded-xl bg-white p-5 shadow-sm">
    <div className="flex flex-wrap items-center gap-3">
      {detail.status === 'PENDIENTE_RECOGIDA' && <>
        <button onClick={() => { setAction('editar'); setError(''); }} className="rounded bg-purple-900 px-4 py-2 font-semibold text-white">Editar</button>
        <button onClick={() => { setAction('cancelar'); setError(''); }} className="rounded bg-red-700 px-4 py-2 font-semibold text-white">Cancelar envío</button>
      </>}
      {detail.status !== 'PENDIENTE_RECOGIDA' && <p className="text-sm text-gray-600">Este envío ya no admite edición ni cancelación.</p>}
    </div>
    {action === 'editar' && detail.status === 'PENDIENTE_RECOGIDA' && <form onSubmit={update} className="mt-6 space-y-4">
      <h2 className="font-bold text-purple-900">Editar datos del envío</h2>
      <div className="grid gap-4 md:grid-cols-2">
        {fields.map(field => <label key={field.key} className="text-sm font-medium text-gray-700">{field.label}
          <InputBase type={field.type} required value={String(form[field.key] ?? '')} onChange={event => change(field.key, event.target.value)} className="mt-1" />
        </label>)}
        <label className="text-sm font-medium text-gray-700">Peso (kg)
          <InputBase type="number" min="0.001" step="0.001" required value={form.weightKg} onChange={event => change('weightKg', Number(event.target.value))} className="mt-1" />
        </label>
        <label className="text-sm font-medium text-gray-700">Servicio
          <select value={form.serviceType} onChange={event => { const value = event.target.value as ServiceType; setForm(current => ({ ...current, serviceType: value, scheduledDeliveryDate: value === 'PROGRAMADO' ? current.scheduledDeliveryDate : null })); }} className="mt-1 w-full rounded border p-2 text-gray-900">
            <option value="ESTANDAR">Estándar</option><option value="EXPRES">Exprés</option><option value="PROGRAMADO">Programado</option>
          </select>
        </label>
        {form.serviceType === 'PROGRAMADO' && <label className="text-sm font-medium text-gray-700">Fecha programada de entrega
          <InputBase type="date" required value={form.scheduledDeliveryDate ?? ''} onChange={event => change('scheduledDeliveryDate', event.target.value)} className="mt-1" />
        </label>}
      </div>
      <p className="text-xs text-gray-500">Se envían todos los campos editables y la versión actual. Código, ID y estado no se modifican.</p>
      <div className="flex gap-2"><button disabled={pending} className="rounded bg-purple-900 px-4 py-2 text-white disabled:opacity-50">{pending ? 'Guardando...' : 'Guardar cambios'}</button><button type="button" onClick={() => setAction(null)} className="rounded border px-4 py-2">Volver</button></div>
    </form>}
    {action === 'cancelar' && detail.status === 'PENDIENTE_RECOGIDA' && <form onSubmit={cancel} className="mt-6 space-y-3">
      <label className="block text-sm font-medium text-gray-700">Motivo de cancelación (privado)
        <textarea required value={reason} onChange={event => setReason(event.target.value)} className="mt-1 w-full rounded border p-3 text-gray-900" />
      </label>
      <div className="flex gap-2"><button disabled={pending} className="rounded bg-red-700 px-4 py-2 text-white disabled:opacity-50">{pending ? 'Cancelando...' : 'Confirmar cancelación'}</button><button type="button" onClick={() => setAction(null)} className="rounded border px-4 py-2">Volver</button></div>
    </form>}
    {error && <p role="alert" className="mt-4 text-sm text-red-700">{error}</p>}
    {message && <p role="status" className="mt-4 text-sm text-green-700">{message}</p>}
  </section>;
}
