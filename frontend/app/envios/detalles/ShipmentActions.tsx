'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { PACKAGING_LABELS, statusLabel, type PackagingType, type ShipmentDetail, type ShipmentInput, type ShipmentStatus } from '@/lib/types';

type Action = 'editar' | 'cancelar' | 'estado' | 'ubicacion';
const packageTypes = Object.keys(PACKAGING_LABELS) as PackagingType[];
const editFields: { key: keyof ShipmentInput; label: string; type?: string }[] = [
  { key: 'senderName', label: 'Remitente' }, { key: 'senderPhone', label: 'Teléfono remitente' },
  { key: 'senderEmail', label: 'Correo remitente', type: 'email' }, { key: 'recipientName', label: 'Destinatario' },
  { key: 'recipientPhone', label: 'Teléfono destinatario' }, { key: 'recipientEmail', label: 'Correo destinatario', type: 'email' },
  { key: 'destinationStreet', label: 'Calle destino' }, { key: 'destinationNumber', label: 'Número destino' },
  { key: 'destinationCity', label: 'Ciudad destino' },
];
function positiveDecimal(value: FormDataEntryValue | null, label: string, max: number) {
  const raw = String(value ?? '').trim();
  if (!/^\d+(?:[.,]\d{1,2})?$/.test(raw)) throw new Error(`${label}: usa máximo dos decimales.`);
  const parsed = Number(raw.replace(',', '.'));
  if (parsed < 0.01 || parsed > max) throw new Error(`${label}: debe estar entre 0,01 y ${max}.`);
  return parsed;
}

export default function ShipmentActions({ detail, initialAction, canWrite, permissions }: { detail: ShipmentDetail; initialAction: Action | null; canWrite: boolean; permissions: string[] }) {
  const router = useRouter();
  const [action, setAction] = useState<Action | null>(initialAction);
  const [packaging, setPackaging] = useState<PackagingType>(detail.shipment.packagingType);
  const [status, setStatus] = useState<ShipmentStatus>(detail.allowedStatusTransitions[0] ?? detail.status);
  const [reason, setReason] = useState('');
  const [city, setCity] = useState('');
  const [cities, setCities] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  useEffect(() => {
    if (action !== 'ubicacion' || city.trim().length < 3) return;
    const controller = new AbortController();
    fetch(`/api/ciudades?q=${encodeURIComponent(city.trim())}`, { signal: controller.signal })
      .then(response => response.ok ? response.json() : []).then(data => { if (!controller.signal.aborted) setCities(data); }).catch(() => {});
    return () => controller.abort();
  }, [city, action]);
  async function send(path: string, body: object, success: string) {
    setError(''); setPending(true);
    try {
      const response = await fetch(path, { method: path.endsWith(String(detail.id)) ? 'PUT' : 'POST',
        headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
      const data = await response.json();
      if (!response.ok) throw new Error(response.status === 409 ? 'El envío cambió o la operación no está permitida. Recarga la ficha.' : data.message || 'No se pudo guardar.');
      setAction(null); setReason('');
      router.replace(`/envios/detalles?id=${detail.id}&aviso=${encodeURIComponent(success)}`);
      router.refresh();
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo guardar.'); }
    finally { setPending(false); }
  }
  function edit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const data = new FormData(event.currentTarget);
      const shipment: ShipmentInput = { ...detail.shipment,
        weightKg: positiveDecimal(data.get('weightKg'), 'Peso', 50), lengthCm: positiveDecimal(data.get('lengthCm'), 'Largo', 200),
        widthCm: positiveDecimal(data.get('widthCm'), 'Ancho', 200), heightCm: positiveDecimal(data.get('heightCm'), 'Alto', 200),
        packagingType: packaging, packagingDescription: packaging === 'OTRO' ? String(data.get('packagingDescription') ?? '').trim() : null };
      if (packaging === 'OTRO' && (shipment.packagingDescription?.length ?? 0) < 3) throw new Error('Describe el empaque en 3 a 100 caracteres.');
      if (detail.status === 'PENDIENTE_RECOGIDA') {
        for (const field of editFields) (shipment[field.key] as string) = String(data.get(field.key) ?? '').trim();
        shipment.serviceType = String(data.get('serviceType')) as ShipmentInput['serviceType'];
        shipment.scheduledDeliveryDate = shipment.serviceType === 'PROGRAMADO' ? String(data.get('scheduledDeliveryDate') ?? '') : null;
      }
      void send(`/api/envios/${detail.id}`, { version: detail.version, shipment }, 'Datos actualizados');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Datos inválidos.'); }
  }
  function changeStatus(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (status === 'INCIDENCIA' && (reason.trim().length < 10 || reason.trim().length > 250)) { setError('La incidencia requiere un motivo de 10 a 250 caracteres.'); return; }
    if (!window.confirm(`¿Confirmas cambiar ${detail.trackingCode} de ${statusLabel(detail.status)} a ${statusLabel(status)}?`)) return;
    void send(`/api/envios/${detail.id}/estado`, { version: detail.version, status, reason: status === 'INCIDENCIA' ? reason.trim() : null }, 'Estado actualizado');
  }
  function location(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const reference = String(data.get('reference') ?? '').trim();
    if (!city.trim()) { setError('Selecciona una ciudad.'); return; }
    if (reference.length > 100) { setError('La referencia no puede superar 100 caracteres.'); return; }
    void send(`/api/envios/${detail.id}/ubicaciones`, { version: detail.version, city: city.trim(), reference: reference || null }, 'Ubicación registrada');
  }
  function cancel(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!reason.trim()) { setError('Indica el motivo de cancelación.'); return; }
    if (!window.confirm(`¿Cancelar ${detail.trackingCode}? El envío permanecerá en el historial.`)) return;
    void send(`/api/envios/${detail.id}/cancelacion`, { version: detail.version, reason: reason.trim() }, 'Envío cancelado');
  }
  const editable = permissions.includes('ENVIO_EDITAR') && (detail.status === 'PENDIENTE_RECOGIDA' || detail.status === 'RECOGIDO');
  const locatable = permissions.includes('ENVIO_ESTADO') && !['PENDIENTE_RECOGIDA', 'ENTREGADO', 'CANCELADO'].includes(detail.status);
  return <section className="app-panel p-6">
    <h2 className="mb-4 text-lg font-bold text-white">Acciones del envío</h2>
    {!canWrite ? <p className="text-sm text-slate-400">La cuenta de consulta solo puede ver los datos.</p> : <div className="flex flex-wrap gap-2">
      {editable && <button onClick={() => { setAction('editar'); setError(''); }} className="app-btn app-btn-secondary">Editar datos</button>}
      {detail.allowedStatusTransitions.length > 0 && <button onClick={() => { setAction('estado'); setError(''); }} className="app-btn app-btn-primary">Actualizar estado</button>}
      {locatable && <button onClick={() => { setAction('ubicacion'); setError(''); }} className="app-btn app-btn-secondary">Registrar ubicación</button>}
      {permissions.includes('ENVIO_EDITAR') && detail.status === 'PENDIENTE_RECOGIDA' && <button onClick={() => { setAction('cancelar'); setError(''); }} className="app-btn app-btn-danger">Cancelar envío</button>}
      {!editable && !locatable && !detail.allowedStatusTransitions.length && <p className="text-sm text-slate-400">Este envío ya no admite modificaciones.</p>}
    </div>}
    {canWrite && action === 'editar' && editable && <form onSubmit={edit} className="mt-6 space-y-5 border-t border-slate-700 pt-5">
      <h3 className="font-bold text-blue-200">Editar {detail.status === 'RECOGIDO' ? 'datos físicos' : 'datos del envío'}</h3>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {detail.status === 'PENDIENTE_RECOGIDA' && editFields.map(field => <label key={field.key} className="app-label">{field.label}<input className="app-field mt-1" name={field.key} type={field.type ?? 'text'} defaultValue={String(detail.shipment[field.key] ?? '')} required /></label>)}
        {(['weightKg', 'lengthCm', 'widthCm', 'heightCm'] as const).map((key, index) => <label key={key} className="app-label">{['Peso (kg)', 'Largo (cm)', 'Ancho (cm)', 'Alto (cm)'][index]}<input className="app-field mt-1" name={key} inputMode="decimal" defaultValue={detail.shipment[key]} required /></label>)}
        <label className="app-label">Empaque<select className="app-field mt-1" value={packaging} onChange={event => setPackaging(event.target.value as PackagingType)}>{packageTypes.map(value => <option key={value} value={value}>{PACKAGING_LABELS[value]}</option>)}</select></label>
        {packaging === 'OTRO' && <label className="app-label">Descripción del empaque<input className="app-field mt-1" name="packagingDescription" defaultValue={detail.shipment.packagingDescription ?? ''} minLength={3} maxLength={100} required /></label>}
        {detail.status === 'PENDIENTE_RECOGIDA' && <><label className="app-label">Servicio<select className="app-field mt-1" name="serviceType" defaultValue={detail.shipment.serviceType}><option value="ESTANDAR">Estándar</option><option value="EXPRES">Exprés</option><option value="PROGRAMADO">Programado</option></select></label><label className="app-label">Fecha programada, si aplica<input className="app-field mt-1" name="scheduledDeliveryDate" type="date" defaultValue={detail.shipment.scheduledDeliveryDate ?? ''} /></label></>}
      </div><Actions pending={pending} onClose={() => setAction(null)} label="Guardar cambios" /></form>}
    {canWrite && action === 'estado' && detail.allowedStatusTransitions.length > 0 && <form onSubmit={changeStatus} className="mt-6 space-y-4 border-t border-slate-700 pt-5"><h3 className="font-bold text-blue-200">Cambiar estado</h3><label className="app-label">Nuevo estado<select className="app-field mt-1" value={status} onChange={event => setStatus(event.target.value as ShipmentStatus)}>{detail.allowedStatusTransitions.map(value => <option key={value} value={value}>{statusLabel(value)}</option>)}</select></label>{status === 'INCIDENCIA' && <label className="app-label">Motivo de incidencia (10–250 caracteres)<textarea className="app-field mt-1" value={reason} onChange={event => setReason(event.target.value)} minLength={10} maxLength={250} required /></label>}<Actions pending={pending} onClose={() => setAction(null)} label="Confirmar cambio" /></form>}
    {canWrite && action === 'ubicacion' && locatable && <form onSubmit={location} className="mt-6 space-y-4 border-t border-slate-700 pt-5"><h3 className="font-bold text-blue-200">Registrar ubicación actual</h3><label className="app-label">Ciudad colombiana<input className="app-field mt-1" list="ciudades-colombia" value={city} onChange={event => setCity(event.target.value)} placeholder="Escribe al menos 3 letras" required /><datalist id="ciudades-colombia">{cities.map(value => <option key={value} value={value} />)}</datalist></label><label className="app-label">Referencia (opcional, máximo 100 caracteres)<input className="app-field mt-1" name="reference" maxLength={100} /></label><Actions pending={pending} onClose={() => setAction(null)} label="Guardar ubicación" /></form>}
    {canWrite && action === 'cancelar' && detail.status === 'PENDIENTE_RECOGIDA' && <form onSubmit={cancel} className="mt-6 space-y-4 border-t border-slate-700 pt-5"><label className="app-label">Motivo de cancelación (privado)<textarea className="app-field mt-1" value={reason} onChange={event => setReason(event.target.value)} required /></label><Actions pending={pending} onClose={() => setAction(null)} label="Confirmar cancelación" /></form>}
    {error && <p role="alert" className="app-alert mt-4">{error}</p>}
    <p className="mt-5 text-xs text-slate-500">La eliminación permanente está desactivada para conservar la trazabilidad y auditoría exigidas en este sprint.</p>
  </section>;
}
function Actions({ pending, onClose, label }: { pending: boolean; onClose: () => void; label: string }) {
  return <div className="flex gap-2"><button disabled={pending} className="app-btn app-btn-primary disabled:opacity-50">{pending ? 'Guardando...' : label}</button><button type="button" onClick={onClose} className="app-btn app-btn-secondary">Volver</button></div>;
}
