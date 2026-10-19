'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { BarChart3, ClipboardList, Home, LogIn, LogOut, PackagePlus, ShieldCheck } from 'lucide-react';

type Session = { authenticated: boolean; role: string | null; username: string | null };
const nav = [
  { href: '/', label: 'Inicio', icon: Home },
  { href: '/envios/nuevo', label: 'Nuevo envío', icon: PackagePlus },
  { href: '/historial-operativo', label: 'Listar envíos', icon: ClipboardList },
  { href: '/reportes', label: 'Reportes', icon: BarChart3 },
];

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [session, setSession] = useState<Session | null>(null);
  useEffect(() => {
    let active = true;
    fetch('/api/session', { cache: 'no-store' }).then(response => response.json())
      .then((data: Session) => { if (active) setSession(data); }).catch(() => {});
    return () => { active = false; };
  }, [pathname]);
  async function logout() {
    await fetch('/api/session/logout', { method: 'POST' });
    setSession({ authenticated: false, role: null, username: null });
    router.push('/'); router.refresh();
  }
  return <div className="min-h-screen md:flex">
    <aside className="border-b border-slate-700 bg-[#0f172a] p-4 md:sticky md:top-0 md:h-screen md:w-60 md:shrink-0 md:border-b-0 md:border-r md:p-5">
      <Link href="/" className="mb-8 flex items-center gap-3 text-lg font-extrabold text-white">
        <span className="rounded-xl bg-blue-600 p-2 text-white"><PackagePlus size={23} /></span>
        <span>LogiFlow<small className="block text-xs font-normal text-slate-400">Panel de control</small></span>
      </Link>
      <nav aria-label="Navegación principal" className="flex flex-wrap gap-1 md:block md:space-y-1">
        {nav.filter(item => session?.role !== 'viewer' || item.href !== '/envios/nuevo').map(item => <Link key={item.href} href={item.href}
          className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm ${pathname === item.href || (item.href === '/historial-operativo' && pathname.startsWith('/envios/detalles')) ? 'bg-blue-700/40 font-semibold text-blue-200' : 'text-slate-300 hover:bg-slate-700/50 hover:text-white'}`}>
          <item.icon size={18} />{item.label}
        </Link>)}
        {session?.role === 'admin' && <Link href="/admin/auditoria" className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm ${pathname.startsWith('/admin') ? 'bg-blue-700/40 text-blue-200' : 'text-slate-300 hover:bg-slate-700/50'}`}><ShieldCheck size={18} />Auditoría</Link>}
      </nav>
      <div className="mt-7 border-t border-slate-700 pt-4 text-xs text-slate-400 md:absolute md:bottom-5 md:left-5 md:right-5">
        {session?.authenticated ? <div className="flex items-center justify-between gap-2"><span><strong className="block text-slate-200">{session.username}</strong>{session.role}</span><button onClick={logout} aria-label="Cerrar sesión" className="rounded p-2 hover:bg-slate-700"><LogOut size={17} /></button></div>
          : <Link href="/operador/ingresar" className="flex items-center gap-2 text-slate-200 hover:text-white"><LogIn size={17} />Ingresar al panel</Link>}
      </div>
    </aside>
    <div className="min-w-0 flex-1"><header className="flex min-h-16 items-center justify-between border-b border-slate-700 px-5 text-sm font-semibold text-slate-300 md:px-9">Centro de operaciones {session?.role !== 'viewer' && <Link href="/envios/nuevo" className="app-btn app-btn-primary text-xs">+ Nuevo envío</Link>}</header><div className="mx-auto max-w-7xl p-5 md:p-9">{children}</div></div>
  </div>;
}
