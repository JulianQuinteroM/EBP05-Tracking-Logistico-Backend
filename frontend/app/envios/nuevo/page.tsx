'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import InputBase from './InputBase';
import { PACKAGING_LABELS, type Estimate, type PackagingType, type ServiceType, type ShipmentDetail, type ShipmentInput } from '@/lib/types';

function decimal(value: FormDataEntryValue | null, label: string, max: number) {
  const raw = String(value ?? '').trim();
  if (!/^\d+(?:[.,]\d{1,2})?$/.test(raw)) throw new Error(`${label}: usa un número con máximo dos decimales.`);
  const number = Number(raw.replace(',', '.'));
  if (number < 0.01 || number > max) throw new Error(`${label}: debe estar entre 0,01 y ${max}.`);
  return number;
}
const text = (data: FormData, key: string) => String(data.get(key) ?? '').trim();
const packageTypes = Object.keys(PACKAGING_LABELS) as PackagingType[];

export default function NuevoEnvio() {
  const router = useRouter();
  const [service, setService] = useState<ServiceType>('ESTANDAR');
  const [packaging, setPackaging] = useState<PackagingType>('CAJA');
  const [scheduledDate, setScheduledDate] = useState('');
  const [estimate, setEstimate] = useState<Estimate | null>(null);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const [ready, setReady] = useState(false);
  const [canCreate, setCanCreate] = useState(false);
  useEffect(() => {
    fetch('/api/session', { cache: 'no-store' }).then(response => response.json()).then(session => {
      if (!session.authenticated) router.push('/operador/ingresar?next=/envios/nuevo');
      else if (session.role === 'viewer') { setError('La cuenta de consulta no puede crear envíos.'); setReady(true); }
      else { setCanCreate(true); setReady(true); }
    }).catch(() => { setError('No se pudo comprobar la sesión.'); setReady(true); });
  }, [router]);
  useEffect(() => {
    if (service === 'PROGRAMADO' && !scheduledDate) return;
    const controller = new AbortController();
    fetch('/api/estimaciones', { method: 'POST', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ serviceType: service, scheduledDeliveryDate: service === 'PROGRAMADO' ? scheduledDate : null }), signal: controller.signal })
      .then(response => response.ok ? response.json() : null).then(data => { if (!controller.signal.aborted) setEstimate(data); }).catch(() => setEstimate(null));
    return () => controller.abort();
  }, [service, scheduledDate]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setPending(true);
    try {
      const data = new FormData(event.currentTarget);
      const input: ShipmentInput = {
        senderName: text(data, 'senderName'), senderPhone: text(data, 'senderPhone'), senderEmail: text(data, 'senderEmail'),
        recipientName: text(data, 'recipientName'), recipientPhone: text(data, 'recipientPhone'), recipientEmail: text(data, 'recipientEmail'),
        destinationStreet: text(data, 'destinationStreet'), destinationNumber: text(data, 'destinationNumber'), destinationCity: text(data, 'destinationCity'),
        weightKg: decimal(data.get('weightKg'), 'Peso', 50), lengthCm: decimal(data.get('lengthCm'), 'Largo', 200),
        widthCm: decimal(data.get('widthCm'), 'Ancho', 200), heightCm: decimal(data.get('heightCm'), 'Alto', 200),
        packagingType: packaging, packagingDescription: packaging === 'OTRO' ? text(data, 'packagingDescription') : null,
        serviceType: service, scheduledDeliveryDate: service === 'PROGRAMADO' ? scheduledDate : null,
      };
      if (packaging === 'OTRO' && (input.packagingDescription?.length ?? 0) < 3) throw new Error('Describe el empaque en 3 a 100 caracteres.');
      if (service === 'PROGRAMADO' && !scheduledDate) throw new Error('Selecciona una fecha de entrega programada.');
      const response = await fetch('/api/envios', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(input) });
      const body = await response.json();
      if (response.status === 401) { router.push('/operador/ingresar?next=/envios/nuevo'); throw new Error('Inicia sesión para continuar.'); }
      if (!response.ok) throw new Error(body.message || 'No se pudo crear el envío.');
      router.push(`/envios/detalles?id=${(body as ShipmentDetail).id}`);
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'No se pudo crear el envío.'); setPending(false); }
  }

  if (!ready) return <p className="app-panel p-6">Comprobando sesión...</p>;
  if (!canCreate) return <p role="alert" className="app-alert">{error || 'No tienes permiso para crear envíos.'}</p>;
  return <main className="mx-auto max-w-5xl space-y-6">
    <header><p className="text-xs font-bold uppercase tracking-widest text-blue-300">Operaciones / Nuevo envío</p><h1 className="mt-2 text-3xl font-bold text-white">Registrar nuevo envío</h1><p className="mt-2 text-slate-400">Completa los datos para generar una guía de seguimiento.</p></header>
    {error && <p role="alert" className="app-alert">{error}</p>}
    <form onSubmit={submit} className="space-y-6">
      <Section title="Datos del remitente"><Field label="Nombre completo" name="senderName" required /><Field label="Teléfono" name="senderPhone" type="tel" required /><Field label="Correo electrónico" name="senderEmail" type="email" required /></Section>
      <Section title="Datos del destinatario"><Field label="Nombre completo" name="recipientName" required /><Field label="Teléfono" name="recipientPhone" type="tel" required /><Field label="Correo electrónico" name="recipientEmail" type="email" required /><Field label="Calle" name="destinationStreet" required /><Field label="Número" name="destinationNumber" required /><Field label="Ciudad de destino" name="destinationCity" required /></Section>
      <Section title="Datos físicos y servicio">
        <Field label="Peso (kg)" name="weightKg" placeholder="Ej. 4,50" inputMode="decimal" required />
        <Field label="Largo (cm)" name="lengthCm" placeholder="Ej. 28" inputMode="decimal" required />
        <Field label="Ancho (cm)" name="widthCm" placeholder="Ej. 20" inputMode="decimal" required />
        <Field label="Alto (cm)" name="heightCm" placeholder="Ej. 12" inputMode="decimal" required />
        <label className="app-label">Tipo de empaque<select className="app-field mt-2" value={packaging} onChange={event => setPackaging(event.target.value as PackagingType)}>{packageTypes.map(value => <option value={value} key={value}>{PACKAGING_LABELS[value]}</option>)}</select></label>
        <label className="app-label">Tipo de servicio<select className="app-field mt-2" value={service} onChange={event => setService(event.target.value as ServiceType)}><option value="ESTANDAR">Estándar</option><option value="EXPRES">Exprés</option><option value="PROGRAMADO">Programado</option></select></label>
        {packaging === 'OTRO' && <Field label="Descripción del empaque (3–100 caracteres)" name="packagingDescription" minLength={3} maxLength={100} required />}
        {service === 'PROGRAMADO' && <label className="app-label">Fecha programada de entrega<InputBase type="date" className="mt-2" value={scheduledDate} onChange={event => setScheduledDate(event.target.value)} required /></label>}
      </Section>
      <div className="app-panel grid gap-4 p-5 text-sm sm:grid-cols-2"><p className="text-slate-300">Tiempo estimado <strong className="mt-1 block text-lg text-blue-200">{estimate?.deliveryWindow ?? '—'}</strong></p><p className="text-slate-300">Costo informativo <strong className="mt-1 block text-lg text-orange-300">{estimate ? new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(estimate.costCop) : '—'}</strong></p></div>
      <div className="flex justify-end"><button disabled={pending} className="app-btn app-btn-primary disabled:opacity-50">{pending ? 'Guardando...' : 'Guardar envío'}</button></div>
    </form>
  </main>;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="app-panel p-6"><h2 className="mb-5 border-b border-slate-700 pb-3 text-lg font-bold text-white">{title}</h2><div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{children}</div></section>;
}
function Field({ label, ...props }: { label: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return <label className="app-label">{label}<InputBase className="mt-2" {...props} /></label>;
}
