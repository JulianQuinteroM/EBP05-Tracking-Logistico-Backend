import { redirect } from 'next/navigation';
import { pageSession } from '@/lib/session';
import Reports from './Reports';
export default async function ReportsPage() {
  const session = await pageSession();
  if (!session) redirect('/operador/ingresar?next=/reportes');
  if (!session.permissions.includes('REPORTES')) return <p className="app-alert">Tu cuenta no tiene permiso para consultar reportes.</p>;
  return <Reports />;
}
