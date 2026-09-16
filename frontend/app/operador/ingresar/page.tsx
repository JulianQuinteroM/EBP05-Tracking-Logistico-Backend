'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function OperatorLoginPage() {
  const router = useRouter();
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
        body: JSON.stringify({ username: 'operador', password }),
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

  return <main className="mx-auto max-w-md rounded-xl border border-gray-100 bg-white p-8 shadow-sm">
    <h1 className="mb-2 text-2xl font-bold text-purple-900">Ingreso de operador</h1>
    <p className="mb-6 text-sm text-gray-600">Las operaciones internas requieren la contraseña definida al iniciar el backend. No la compartas ni la guardes en el navegador.</p>
    <form onSubmit={submit} className="space-y-4">
      <label className="block text-sm font-medium text-gray-700">Usuario
        <input value="operador" readOnly className="mt-1 w-full rounded border p-3 text-gray-900" />
      </label>
      <label className="block text-sm font-medium text-gray-700">Contraseña
        <input type="password" autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} required className="mt-1 w-full rounded border p-3 text-gray-900" />
      </label>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <button disabled={pending} className="w-full rounded bg-purple-900 p-3 font-semibold text-white disabled:opacity-50">{pending ? 'Ingresando...' : 'Ingresar'}</button>
    </form>
  </main>;
}
