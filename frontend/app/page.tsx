'use client';

import Link from 'next/link';
import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowRight, PackagePlus, Search, Truck } from 'lucide-react';

export default function Home() {
  const router = useRouter();
  const [trackingCode, setTrackingCode] = useState('');
  const [error, setError] = useState('');
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const code = trackingCode.replace(/-/g, '').trim().toUpperCase();
    if (!/^LOG\d{9}$/.test(code)) { setError('Verifica el formato del código de seguimiento.'); return; }
    setError('');
    router.push(`/envios/info?codigo=${encodeURIComponent(code)}`);
  }
  return <main className="space-y-7">
    <section className="relative overflow-hidden rounded-3xl border border-blue-900/60 bg-gradient-to-br from-[#19243c] via-[#151f36] to-[#122f59] px-6 py-14 text-center shadow-2xl md:px-16 md:py-20">
      <div className="absolute -right-20 -top-24 h-64 w-64 rounded-full bg-blue-600/15 blur-3xl" />
      <p className="relative mb-3 text-xs font-bold uppercase tracking-[.22em] text-blue-300">Centro de operaciones</p>
      <h1 className="relative mx-auto max-w-3xl text-4xl font-extrabold leading-tight text-white md:text-5xl">Gestiona y rastrea envíos al instante</h1>
      <p className="relative mx-auto mt-5 max-w-xl text-slate-300">Plataforma logística integral. Rastreo actualizado, creación de guías y administración operativa centralizada.</p>
      <form onSubmit={search} className="relative mx-auto mt-9 flex max-w-xl flex-col gap-2 sm:flex-row">
        <label className="sr-only" htmlFor="tracking">Código de seguimiento</label>
        <input id="tracking" value={trackingCode} onChange={event => setTrackingCode(event.target.value)} placeholder="Ingresa tu código de seguimiento" className="app-field flex-1" />
        <button className="app-btn app-btn-primary"><Search size={18} />Buscar</button>
      </form>
      {error && <p role="alert" className="relative mt-3 text-sm text-rose-300">{error}</p>}
    </section>
    <section className="grid gap-5 md:grid-cols-3" aria-label="Accesos rápidos">
      <Link href="/envios/nuevo" className="app-panel group p-7 transition hover:border-blue-500 hover:bg-[#1d2c46]"><span className="mb-5 inline-flex rounded-full bg-orange-500/15 p-4 text-orange-300"><PackagePlus /></span><h2 className="text-lg font-bold text-white">Crear nuevo envío</h2><p className="mt-2 text-sm text-slate-400">Registra un paquete, asigna prioridad y genera la guía.</p><ArrowRight className="mt-5 text-blue-300 transition group-hover:translate-x-1" size={18} /></Link>
      <Link href="/historial-operativo" className="app-panel group p-7 transition hover:border-blue-500 hover:bg-[#1d2c46]"><span className="mb-5 inline-flex rounded-full bg-slate-500/20 p-4 text-slate-300"><ArrowRight /></span><h2 className="text-lg font-bold text-white">Historial operativo</h2><p className="mt-2 text-sm text-slate-400">Consulta y gestiona los envíos almacenados.</p><ArrowRight className="mt-5 text-blue-300 transition group-hover:translate-x-1" size={18} /></Link>
      <div className="app-panel p-7 opacity-60"><span className="mb-5 inline-flex rounded-full bg-slate-500/20 p-4 text-slate-300"><Truck /></span><h2 className="text-lg font-bold text-white">Gestión de flota</h2><p className="mt-2 text-sm text-slate-400">Módulo previsto para un sprint posterior.</p></div>
    </section>
  </main>;
}
