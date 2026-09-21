'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { displayDate, serviceLabel, statusLabel, type ShipmentListPage } from '@/lib/types';

type SortBy = 'createdAt' | 'trackingCode' | 'status' | 'serviceType';

export default function OperationalHistoryPage() {
  const router = useRouter();
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
      setLoading(true);
      setError('');
      const params = new URLSearchParams({ page: String(page), size: String(size), q: query.trim(), sortBy, direction });
      try {
        const response = await fetch(`/api/envios?${params}`, { cache: 'no-store', signal: controller.signal });
        const body = await response.json();
        if (response.status === 401) {
          router.push('/operador/ingresar?next=/historial-operativo');
          return;
        }
        if (!response.ok) throw new Error(body.message || 'No se pudo cargar la lista');
        setData(body as ShipmentListPage);
      } catch (reason) {
        if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : 'No se pudo cargar la lista');
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }, query ? 300 : 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [query, page, size, sortBy, direction, router]);

  function order(value: SortBy) {
    if (sortBy === value) setDirection(direction === 'asc' ? 'desc' : 'asc');
    else { setSortBy(value); setDirection('asc'); }
    setPage(0);
  }

  async function logout() {
    await fetch('/api/session/logout', { method: 'POST' });
    router.push('/');
    router.refresh();
  }

  return <main className="mx-auto max-w-7xl space-y-6 rounded-xl bg-slate-50 p-6 text-slate-800">
    <header className="flex flex-wrap items-center justify-between gap-4">
      <div><p className="text-sm font-semibold text-purple-700">Operaciones</p><h1 className="text-2xl font-bold">Historial operativo</h1><p className="text-sm text-slate-600">Envíos reales almacenados en PostgreSQL.</p></div>
      <div className="flex gap-2"><Link href="/envios/nuevo" className="rounded bg-purple-900 px-4 py-2 font-semibold text-white">Nuevo envío</Link><button onClick={logout} className="rounded border px-4 py-2">Salir</button></div>
    </header>
    <section className="rounded-xl bg-white p-5 shadow-sm">
      <div className="grid gap-3 md:grid-cols-3">
        <label className="text-sm">Buscar por código o nombre
          <input value={query} onChange={event => { setQuery(event.target.value); setPage(0); }} placeholder="Código, remitente, destinatario" className="mt-1 w-full rounded border px-3 py-2 text-slate-900" />
        </label>
        <label className="text-sm">Ordenar por
          <select value={sortBy} onChange={event => order(event.target.value as SortBy)} className="mt-1 w-full rounded border px-3 py-2 text-slate-900">
            <option value="createdAt">Fecha de registro</option><option value="trackingCode">Código</option><option value="status">Estado</option><option value="serviceType">Servicio</option>
          </select>
        </label>
        <label className="text-sm">Dirección
          <select value={direction} onChange={event => { setDirection(event.target.value as 'asc' | 'desc'); setPage(0); }} className="mt-1 w-full rounded border px-3 py-2 text-slate-900"><option value="desc">Descendente</option><option value="asc">Ascendente</option></select>
        </label>
      </div>
      <p className="mt-3 text-xs text-slate-500">Filtros por estado/fecha, exportación y reportes quedan para HU futuras; esta lista no simula esas funciones.</p>
    </section>
    {error && <p role="alert" className="rounded bg-red-50 p-4 text-red-700">{error}</p>}
    <section className="overflow-x-auto rounded-xl bg-white shadow-sm">
      <table className="w-full min-w-[850px] text-left text-sm">
        <thead className="bg-slate-100 text-xs uppercase text-slate-600"><tr><th className="px-4 py-3">Código</th><th className="px-4 py-3">Remitente</th><th className="px-4 py-3">Destinatario</th><th className="px-4 py-3">Estado</th><th className="px-4 py-3">Servicio</th><th className="px-4 py-3">Registro</th><th className="px-4 py-3">Acciones</th></tr></thead>
        <tbody className="divide-y divide-slate-100">{data?.content.map(item => <tr key={item.id}>
          <td className="px-4 py-3 font-semibold text-purple-900">{item.trackingCode}</td><td className="px-4 py-3">{item.senderName}</td><td className="px-4 py-3">{item.recipientName}</td>
          <td className="px-4 py-3">{statusLabel(item.status)}</td><td className="px-4 py-3">{serviceLabel(item.serviceType)}</td><td className="px-4 py-3">{displayDate(item.createdAt)}</td>
          <td className="px-4 py-3"><div className="flex flex-wrap gap-2"><Link href={`/envios/detalles?id=${item.id}`} className="rounded border border-purple-300 px-2 py-1 text-purple-900">Ver detalles</Link>{item.status === 'PENDIENTE_RECOGIDA' && <><Link href={`/envios/detalles?id=${item.id}&accion=editar`} className="rounded border px-2 py-1">Editar</Link><Link href={`/envios/detalles?id=${item.id}&accion=cancelar`} className="rounded border border-red-300 px-2 py-1 text-red-700">Cancelar</Link></>}<Link href={`/envios/detalles?id=${item.id}&accion=eliminar`} className="rounded border border-red-800 px-2 py-1 font-medium text-red-900">Eliminar</Link></div></td>
        </tr>)}</tbody>
      </table>
      {loading && <p className="p-6 text-center text-sm text-slate-500">Cargando envíos...</p>}
      {!loading && data?.content.length === 0 && <p className="p-6 text-center text-sm text-slate-500">No hay envíos para esta búsqueda.</p>}
      <div className="flex flex-wrap items-center justify-between gap-3 border-t p-4 text-sm">
        <span>{data?.totalElements ?? 0} envíos · página {(data?.page ?? page) + 1} de {Math.max(data?.totalPages ?? 0, 1)}</span>
        <div className="flex items-center gap-3"><label>Mostrar <select value={size} onChange={event => { setSize(Number(event.target.value)); setPage(0); }} className="rounded border p-1"><option value="20">20</option><option value="50">50</option></select></label><button disabled={page === 0 || loading} onClick={() => setPage(page - 1)} className="rounded border px-3 py-1 disabled:opacity-40">Anterior</button><button disabled={loading || !data || page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="rounded border px-3 py-1 disabled:opacity-40">Siguiente</button></div>
      </div>
    </section>
  </main>;
}
