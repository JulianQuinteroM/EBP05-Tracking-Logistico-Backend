'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { displayDate, type AuditRecord } from '@/lib/types';

export default function AuditPage() {
  const router = useRouter();
  const [shipmentId, setShipmentId] = useState('');
  const [action, setAction] = useState('');
  const [actor, setActor] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [rows, setRows] = useState<AuditRecord[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [refresh, setRefresh] = useState(0);
  const [appliedQuery, setAppliedQuery] = useState('');
  const params = new URLSearchParams();
  if (shipmentId.trim()) params.set('shipmentId', shipmentId.trim());
  if (action) params.set('action', action);
  if (actor.trim()) params.set('actor', actor.trim());
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString();
  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/session', { cache: 'no-store' }).then(response => response.json()).then(session => {
      if (!session.authenticated) router.push('/operador/ingresar?next=/admin/auditoria');
      else if (session.role !== 'admin') { setError('Solo el administrador puede consultar la auditoría.'); setLoading(false); }
      else {
        setLoading(true);
        fetch(`/api/admin/auditoria?${appliedQuery}`, { signal: controller.signal, cache: 'no-store' })
          .then(async response => { const body = await response.json(); if (!response.ok) throw new Error(body.message || 'No se pudo cargar la auditoría.'); return body as AuditRecord[]; })
          .then(data => { if (!controller.signal.aborted) { setRows(data); setError(''); } })
          .catch(cause => { if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'No se pudo cargar.'); })
          .finally(() => { if (!controller.signal.aborted) setLoading(false); });
      }
    }).catch(() => setError('No se pudo comprobar la sesión.'));
    return () => controller.abort();
  }, [router, appliedQuery, refresh]);
  const alerts = rows.filter(row => row.action === 'TRANSICION_INUSUAL' || row.action === 'INTENTO_NO_AUTORIZADO');
  async function exportCsv() {
    setError('');
    try {
      const response = await fetch(`/api/admin/auditoria/exportacion?${query}`, { cache: 'no-store' });
      if (!response.ok) throw new Error('No se pudo exportar el historial.');
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement('a'); link.href = url; link.download = 'auditoria-envios.csv'; link.click();
      URL.revokeObjectURL(url);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo exportar.'); }
  }
  return <main className="space-y-6"><header className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-widest text-blue-300">Administrador</p><h1 className="mt-1 text-3xl font-bold text-white">Auditoría de envíos</h1><p className="mt-1 text-sm text-slate-400">Cambios, accesos y alertas conservados para revisión.</p></div><button onClick={exportCsv} className="app-btn app-btn-primary">Exportar CSV</button></header>
    <section className="app-panel p-5"><h2 className="font-bold text-white">Alertas operativas</h2><p className="mt-2 text-sm text-slate-400">{alerts.length} alertas en el resultado actual: transiciones inusuales e intentos no autorizados.</p>{alerts.slice(0, 5).map(row => <p key={row.id} className="mt-2 rounded border border-orange-800/70 bg-orange-900/20 p-3 text-sm text-orange-200">{displayDate(row.occurredAt)} · {row.action} · {row.actor} · {row.detail}</p>)}</section>
    <section className="app-panel grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-5"><label className="app-label">ID de envío<input className="app-field mt-1" type="number" min="1" value={shipmentId} onChange={event => setShipmentId(event.target.value)} /></label><label className="app-label">Acción<select className="app-field mt-1" value={action} onChange={event => setAction(event.target.value)}><option value="">Todas</option><option value="ESTADO">Estado</option><option value="UBICACION">Ubicación</option><option value="EDICION">Edición</option><option value="TRANSICION_INUSUAL">Transición inusual</option><option value="INTENTO_NO_AUTORIZADO">No autorizado</option></select></label><label className="app-label">Usuario<input className="app-field mt-1" value={actor} onChange={event => setActor(event.target.value)} /></label><label className="app-label">Desde<input className="app-field mt-1" type="date" value={from} onChange={event => setFrom(event.target.value)} /></label><label className="app-label">Hasta<input className="app-field mt-1" type="date" value={to} onChange={event => setTo(event.target.value)} /></label></section>
    <div className="flex justify-end"><button onClick={() => { setAppliedQuery(query); setRefresh(value => value + 1); }} className="app-btn app-btn-secondary">Aplicar filtros / actualizar</button></div>{error && <p role="alert" className="app-alert">{error}</p>}
    <section className="app-panel overflow-x-auto"><table className="w-full min-w-[1000px] text-left text-sm"><thead className="bg-[#21314c] text-xs uppercase text-slate-300"><tr>{['Fecha', 'Envío', 'Acción', 'Usuario', 'Campo', 'Antes', 'Después', 'IP / detalle'].map(name => <th key={name} className="px-3 py-3">{name}</th>)}</tr></thead><tbody className="divide-y divide-slate-700 text-slate-300">{rows.map(row => <tr key={row.id}><td className="px-3 py-3">{displayDate(row.occurredAt)}</td><td className="px-3 py-3">{row.shipmentId ?? '—'}</td><td className="px-3 py-3 text-blue-200">{row.action}</td><td className="px-3 py-3">{row.actor}</td><td className="px-3 py-3">{row.field ?? '—'}</td><td className="px-3 py-3">{row.beforeValue ?? '—'}</td><td className="px-3 py-3">{row.afterValue ?? '—'}</td><td className="px-3 py-3">{row.ipAddress ?? '—'}<small className="block text-slate-500">{row.detail}</small></td></tr>)}</tbody></table>{loading && <p className="p-5 text-center text-slate-400">Cargando auditoría...</p>}{!loading && rows.length === 0 && <p className="p-5 text-center text-slate-400">No hay registros para estos filtros.</p>}</section>
  </main>;
}
