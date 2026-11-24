'use client';
import { useEffect, useState } from 'react';
import type { Contact } from '@/app/contactos/page';
export default function ContactPicker({ onSelect }: { onSelect: (contact: Contact | null) => void }) {
  const [contacts, setContacts] = useState<Contact[]>([]);const [query, setQuery] = useState('');
  useEffect(() => { const controller = new AbortController();const timer = setTimeout(() => { fetch('/api/contactos?q=' + encodeURIComponent(query), { signal: controller.signal }).then(r => r.ok ? r.json() : []).then(data => { if (!controller.signal.aborted) setContacts(data); }).catch(() => {}); }, 250);return () => { clearTimeout(timer);controller.abort(); }; }, [query]);
  return <div className="space-y-2 rounded-lg border border-slate-700 p-3 sm:col-span-2 lg:col-span-3"><label className="app-label">Usar un contacto registrado<input className="app-field mt-1" value={query} onChange={e => setQuery(e.target.value)} placeholder="Buscar por nombre o documento" maxLength={120} /></label><label className="app-label">Seleccionar contacto<select className="app-field mt-1" defaultValue="" onChange={e => onSelect(contacts.find(c => c.id === Number(e.target.value)) ?? null)}><option value="">Ingresar datos manualmente</option>{contacts.map(c => <option key={c.id} value={c.id}>{c.nombre} · {c.documento || c.correo}</option>)}</select></label></div>;
}
