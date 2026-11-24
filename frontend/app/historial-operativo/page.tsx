'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { displayDate, serviceLabel, statusLabel, statusTone, type ShipmentListPage } from '@/lib/types';
import { download } from '@/lib/client-api';
import { useAccess } from '@/lib/use-access';

type SortBy = 'createdAt' | 'trackingCode' | 'status' | 'serviceType';
export default function OperationalHistoryPage() {
  const router = useRouter();
  const can = useAccess();
  async function exportRows() { try { await download('envios/exportacion?q=' + encodeURIComponent(query.trim()), 'envios.csv'); } catch (e) { setError((e as Error).message); } }
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sortBy, setSortBy] = useState<SortBy>('createdAt');
  const [direction, setDirection] = useState<'asc' | 'desc'>('desc');
  const [data, setData] = useState<ShipmentListPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setLoading(true); setError('');
      const params = new URLSearchParams({ page: String(page), size: String(size), q: query.trim(), sortBy, direction });
      try {
        const response = await fetch(`/api/envios?${params}`, { cache: 'no-store', signal: controller.signal });
        const body = await response.json();
        if (response.status === 401) { router.push('/operador/ingresar?next=/historial-operativo'); return; }
        if (!response.ok) throw new Error(body.message || 'No se pudo cargar la lista.');
        setData(body as ShipmentListPage);
      } catch (cause) { if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'No se pudo cargar la lista.'); }
      finally { if (!controller.signal.aborted) setLoading(false); }
    }, query ? 300 : 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [query, page, size, sortBy, direction, router]);
  return <main className="space-y-6">
    <header className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-widest text-blue-300">Operaciones</p><h1 className="mt-1 text-3xl font-bold text-white">Historial operativo</h1><p className="mt-1 text-sm text-slate-400">Envíos almacenados en PostgreSQL.</p></div>{can('ENVIO_CREAR') && <Link href="/envios/nuevo" className="app-btn app-btn-primary">+ Nuevo envío</Link>}</header>
    <section className="app-panel grid gap-4 p-5 md:grid-cols-3"><label className="app-label">Código o nombre<input className="app-field mt-2" value={query} onChange={event => { setQuery(event.target.value); setPage(0); }} placeholder="Buscar envío" /></label><label className="app-label">Ordenar por<select className="app-field mt-2" value={sortBy} onChange={event => { setSortBy(event.target.value as SortBy); setPage(0); }}><option value="createdAt">Fecha de registro</option><option value="trackingCode">Código</option><option value="status">Estado</option><option value="serviceType">Servicio</option></select></label><label className="app-label">Dirección<select className="app-field mt-2" value={direction} onChange={event => { setDirection(event.target.value as 'asc' | 'desc'); setPage(0); }}><option value="desc">Descendente</option><option value="asc">Ascendente</option></select></label></section>
    {error && <p role="alert" className="app-alert">{error}</p>}
    <section className="app-panel overflow-x-auto"><table className="w-full min-w-[880px] text-left text-sm"><thead className="bg-[#21314c] text-xs uppercase text-slate-300"><tr><th className="px-4 py-3">Código</th><th className="px-4 py-3">Remitente</th><th className="px-4 py-3">Destinatario</th><th className="px-4 py-3">Estado</th><th className="px-4 py-3">Servicio</th><th className="px-4 py-3">Registro</th><th className="px-4 py-3">Acciones</th></tr></thead><tbody className="divide-y divide-slate-700 text-slate-300">{data?.content.map(item => <tr key={item.id} className="hover:bg-slate-700/20"><td className="px-4 py-4 font-bold text-blue-200">{item.trackingCode}</td><td className="px-4 py-4">{item.senderName}</td><td className="px-4 py-4">{item.recipientName}</td><td className="px-4 py-4"><span className={`status-pill status-${statusTone(item.status)}`}>{statusLabel(item.status)}</span></td><td className="px-4 py-4">{serviceLabel(item.serviceType)}</td><td className="px-4 py-4">{displayDate(item.createdAt)}</td><td className="px-4 py-4"><div className="flex gap-2"><Link className="rounded border border-blue-600 px-2 py-1 text-blue-200" href={`/envios/detalles?id=${item.id}`}>Ver detalles</Link>{can('ENVIO_EDITAR') && ['PENDIENTE_RECOGIDA', 'RECOGIDO'].includes(item.status) && <Link className="rounded border border-slate-500 px-2 py-1" href={`/envios/detalles?id=${item.id}&accion=editar`}>Editar</Link>}</div></td></tr>)}</tbody></table>
      {loading && <p className="p-6 text-center text-sm text-slate-400">Cargando envíos...</p>}
      {!loading && data?.content.length === 0 && <p className="p-6 text-center text-sm text-slate-400">No hay envíos para esta búsqueda.</p>}
      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-700 p-4 text-sm text-slate-300"><span>{data?.totalElements ?? 0} envíos · página {(data?.page ?? page) + 1} de {Math.max(data?.totalPages ?? 0, 1)}</span><div className="flex items-center gap-3"><label>Mostrar <select className="app-field ml-1 w-auto" value={size} onChange={event => { setSize(Number(event.target.value)); setPage(0); }}><option value="20">20</option><option value="50">50</option></select></label><button className="app-btn app-btn-secondary disabled:opacity-40" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Anterior</button><button className="app-btn app-btn-secondary disabled:opacity-40" disabled={loading || !data || page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Siguiente</button></div></div>
    </section>{can('REPORTES') && <button className="app-btn app-btn-secondary" onClick={() => void exportRows()}>Exportar todos los resultados de esta búsqueda (CSV)</button>}<p className="text-xs text-slate-500">Los envíos cancelados conservan su historial y auditoría.</p>
  </main>;
}
