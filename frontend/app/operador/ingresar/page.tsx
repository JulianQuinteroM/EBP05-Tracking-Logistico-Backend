'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function OperatorLoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('operador');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setPending(true);
    try {
      const response = await fetch('/api/session/login', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ username, password }),
        cache: 'no-store',
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'No se pudo ingresar');
      setPassword('');
      const next = new URLSearchParams(window.location.search).get('next');
      router.push(next?.startsWith('/') && !next.startsWith('//') ? next : '/historial-operativo');
      router.refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo ingresar');
    } finally {
      setPending(false);
    }
  }

  return <main className="app-panel mx-auto max-w-md p-8">
    <h1 className="mb-2 text-2xl font-bold text-white">Ingreso al panel</h1>
    <p className="mb-6 text-sm text-slate-400">Usa la cuenta configurada al iniciar el backend y el frontend. La contraseña no se guarda en el navegador.</p>
    <form onSubmit={submit} className="space-y-4">
      <label className="app-label">Usuario
        <select value={username} onChange={event => setUsername(event.target.value)} className="app-field mt-1"><option value="operador">operador</option><option value="administrador">administrador</option><option value="consulta">consulta</option></select>
      </label>
      <label className="app-label">Contraseña
        <input type="password" autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} required className="app-field mt-1" />
      </label>
      {error && <p role="alert" className="app-alert">{error}</p>}
      <button disabled={pending} className="app-btn app-btn-primary w-full disabled:opacity-50">{pending ? 'Ingresando...' : 'Ingresar'}</button>
    </form>
  </main>;
}
