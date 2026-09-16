'use client';

import Link from 'next/link';
import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function Home() {
  const router = useRouter();
  const [trackingCode, setTrackingCode] = useState('');
  const [error, setError] = useState('');

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedCode = trackingCode.replace(/-/g, '').trim().toUpperCase();

    setError('');
    if (!/^LOG\d{9}$/.test(normalizedCode)) {
      setError('El código ingresado no es válido. Verifica el formato.');
      return;
    }

    // La pantalla de detalle se encarga de consultar el envío y mostrar
    // el mensaje correspondiente si el código no existe.
    router.push(`/envios/info?codigo=${encodeURIComponent(normalizedCode)}`);
  };

  return (
    <div className="space-y-12">
      {/* Hero Section */}
      <section className="bg-white rounded-2xl shadow-sm p-8 md:p-16 text-center border border-gray-100">
        <h1 className="text-4xl font-extrabold text-gray-900 mb-4">
          Gestiona y rastrea envíos al instante
        </h1>
        <p className="text-lg text-gray-600 mb-8 max-w-2xl mx-auto">
          Consulta un envío por código o ingresa como operador para administrarlo.
        </p>

        <form onSubmit={handleSearch} className="max-w-xl mx-auto flex gap-2">
          <input
            type="text"
            value={trackingCode}
            onChange={(event) => setTrackingCode(event.target.value)}
            aria-label="Ingresa tu código de seguimiento"
            placeholder="Ingresa tu código de seguimiento"
            className="placeholder-gray-400 text-black grow p-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-600 focus:border-transparent outline-none"
          />
          <button type="submit" className="bg-purple-900 hover:bg-purple-800 text-white font-bold py-4 px-8 rounded-lg transition-colors">
            Buscar
          </button>
        </form>
        {error && <p role="alert" className="mt-4 text-sm font-medium text-red-600">{error}</p>}
      </section>

      <p className="text-center text-sm text-gray-600">¿Trabajas en operaciones? <Link href="/operador/ingresar" className="font-semibold text-purple-900 underline">Ingresar como operador</Link></p>

      {/* Accesos Rápidos (Entidad para añadir links) */}
      <section className="grid md:grid-cols-3 gap-6">
        <Link href="/envios/nuevo" className="block group">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow h-full flex flex-col justify-center items-center text-center">
            <div className="bg-orange-100 p-4 rounded-full mb-4 text-orange-600 group-hover:bg-orange-500 group-hover:text-white transition-colors">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
            </div>
            <h3 className="text-xl font-bold text-gray-800 mb-2">Crear Nuevo Envío</h3>
            <p className="text-gray-500 text-sm">Registra un paquete, asigna prioridad y genera la guía.</p>
          </div>
        </Link>

        <Link href="/historial-operativo" className="block group">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow h-full flex flex-col justify-center items-center text-center">
            <div className="bg-gray-100 p-4 rounded-full mb-4 text-gray-600">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17l6-6-6-6"></path></svg>
            </div>
            <h3 className="text-xl font-bold text-gray-800 mb-2">Historial Operativo</h3>
            <p className="text-gray-500 text-sm">Consulta los envíos registrados en la base de datos.</p>
          </div>
        </Link>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 h-full flex flex-col justify-center items-center text-center opacity-70 cursor-not-allowed">
          <div className="bg-gray-100 p-4 rounded-full mb-4 text-gray-600">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
          </div>
          <h3 className="text-xl font-bold text-gray-800 mb-2">Gestión de Flota</h3>
          <p className="text-gray-500 text-sm">Módulo en construcción.</p>
        </div>
      </section>
    </div>
  );
}
